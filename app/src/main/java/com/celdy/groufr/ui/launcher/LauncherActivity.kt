package com.celdy.groufr.ui.launcher

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.databinding.ActivityLauncherBinding
import com.celdy.groufr.ui.login.LoginActivity
import com.celdy.groufr.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityLauncherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            val isValid = authRepository.ensureValidSession()
            val destination = if (isValid) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(Intent(this@LauncherActivity, destination))
            finish()
        }
    }
}
