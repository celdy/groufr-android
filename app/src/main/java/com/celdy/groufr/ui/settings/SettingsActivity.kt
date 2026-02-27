package com.celdy.groufr.ui.settings

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
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

    private val ringtonePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) {
            settingsStore.setNotificationSoundKey(SettingsStore.SOUND_DEVICE)
            settingsStore.setCustomSoundUri(uri.toString())
            updateSoundDisplay()
            playPreviewUri(uri)
        }
    }

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
            SettingsStore.SOUND_DEVICE -> {
                val uri = settingsStore.getCustomSoundUri()
                if (uri != null) {
                    val ringtone = RingtoneManager.getRingtone(this, Uri.parse(uri))
                    ringtone?.getTitle(this) ?: getString(R.string.settings_sound_device)
                } else {
                    getString(R.string.settings_sound_device)
                }
            }
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
                if (selectedKey == SettingsStore.SOUND_DEVICE) {
                    dialog.dismiss()
                    launchRingtonePicker()
                } else {
                    settingsStore.setNotificationSoundKey(selectedKey)
                    updateSoundDisplay()
                    playPreview(selectedKey)
                    dialog.dismiss()
                }
            }
            .show()
    }

    private fun launchRingtonePicker() {
        val currentUri = settingsStore.getCustomSoundUri()?.let { Uri.parse(it) }
        val intent = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).let {
            android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.settings_notification_sound))
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
            }
        }
        ringtonePicker.launch(intent)
    }

    private fun playPreview(key: String) {
        releaseMediaPlayer()
        val resId = SettingsStore.getResourceIdForKey(key) ?: return
        mediaPlayer = MediaPlayer.create(this, resId)?.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }

    private fun playPreviewUri(uri: Uri) {
        releaseMediaPlayer()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@SettingsActivity, uri)
                prepare()
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (_: Exception) {
            // Ignore playback errors
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
