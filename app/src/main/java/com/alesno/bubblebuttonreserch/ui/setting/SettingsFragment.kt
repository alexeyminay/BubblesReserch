package com.alesno.bubblebuttonreserch.ui.setting

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.alesno.bubblebuttonreserch.BubbleNotificationManager
import com.alesno.bubblebuttonreserch.BubbleNotificationManager2
import com.alesno.bubblebuttonreserch.R
import com.alesno.bubblebuttonreserch.databinding.FragmentSettingsBinding
import com.alesno.bubblebuttonreserch.domain.FakeDataRepo
import com.alesno.bubblebuttonreserch.viewBindings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val mBinding by viewBindings(FragmentSettingsBinding::bind)
    private val mViewModel by viewModels<SettingsViewModel> { SettingsViewModelFactory.create() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFastChatSetting()
        subscribeToViewModel()
    }

    private fun initFastChatSetting() = with(mBinding) {
        fastChatSetting.setOnCheckedChangeListener { _, isChecked ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isChecked) {
                if (!canDisplayBubbles()) {
                    requestBubblePermissions()
                }
                BubbleNotificationManager.createNotificationChannel(requireContext())
                lifecycleScope.launch {
                    val id = "1"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        withContext(Dispatchers.IO) {
                            delay(2000)
                        }
                        BubbleNotificationManager2.createNotification(
                            context = requireContext(),
                            participant = FakeDataRepo.getParticipant(id)!!,
                            conversationId = id
                        )
                    } else {
                        BubbleNotificationManager.createLegacyNotification(
                            context = requireContext(),
                            participant = FakeDataRepo.getParticipant(id)!!,
                            conversationId = id
                        )
                    }
                }
            }
        }
        fastChatSetting.setOnClickListener { mViewModel.onFastChatClicked() }
    }

    private fun subscribeToViewModel() {
        mViewModel.settingsState
            .onEach(::render)
            .launchIn(lifecycleScope)
    }

    private fun render(settings: com.alesno.bubblebuttonreserch.domain.Settings) = with(mBinding) {
        fastChatSetting.isChecked = settings.isFastChatEnable
    }

    // TODO: 6/20/2021 Вынести в другое место
    private fun canDisplayBubbles(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }

        val bubblesEnabledGlobally = try {
            Settings.Global.getInt(requireActivity().contentResolver, "notification_bubbles") == 1
        } catch (e: Settings.SettingNotFoundException) {
            true
        }
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return bubblesEnabledGlobally && notificationManager?.areBubblesAllowed() == true
    }

    // TODO: 6/20/2021 Вынести в другое место
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBubblePermissions() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
        startActivityForResult(intent, REQUEST_CODE_BUBBLES_PERMISSION)
    }

    companion object {
        private const val REQUEST_CODE_BUBBLES_PERMISSION = 1212

        fun newInstance(): SettingsFragment = SettingsFragment()
    }

}