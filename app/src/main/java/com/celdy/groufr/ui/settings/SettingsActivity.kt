package com.celdy.groufr.ui.settings

import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.celdy.groufr.R
import com.celdy.groufr.data.storage.SettingsStore
import com.celdy.groufr.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    @Inject lateinit var settingsStore: SettingsStore
    private lateinit var binding: ActivitySettingsBinding
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.settingsToolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.settingsToolbar.updatePadding(top = systemBars.top)
            binding.root.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        updateSoundDisplay()

        binding.settingsSoundCard.setOnClickListener {
            showSoundPicker()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    private fun updateSoundDisplay() {
        binding.settingsSoundValue.text = getDisplayName(settingsStore.getNotificationSoundKey())
    }

    private fun getDisplayName(key: String): String {
        return when (key) {
            SettingsStore.SOUND_NONE -> getString(R.string.settings_sound_none)
            "notify_soft_double" -> getString(R.string.settings_sound_soft_double)
            "notify_soft_triple" -> getString(R.string.settings_sound_soft_triple)
            "notify_bright_short" -> getString(R.string.settings_sound_bright_short)
            "notify_low_soft" -> getString(R.string.settings_sound_low_soft)
            else -> key
        }
    }

    private fun showSoundPicker() {
        val keys = SettingsStore.SOUND_KEYS
        val labels = keys.map { getDisplayName(it) }.toTypedArray()
        val currentKey = settingsStore.getNotificationSoundKey()
        val checkedIndex = keys.indexOf(currentKey).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_notification_sound)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val selectedKey = keys[which]
                settingsStore.setNotificationSoundKey(selectedKey)
                updateSoundDisplay()
                playPreview(selectedKey)
                dialog.dismiss()
            }
            .show()
    }

    private fun playPreview(key: String) {
        releaseMediaPlayer()
        val resId = SettingsStore.getResourceIdForKey(key) ?: return
        mediaPlayer = MediaPlayer.create(this, resId)?.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
