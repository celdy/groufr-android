package com.celdy.groufr.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.celdy.groufr.databinding.ActivityLoginBinding
import com.celdy.groufr.ui.main.MainActivity
import com.celdy.groufr.ui.common.loadSvg
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginLogo.loadSvg(com.celdy.groufr.R.raw.logo)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.loginCard.updatePadding(top = systemBars.top)
            binding.root.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        binding.loginButton.setOnClickListener {
            viewModel.login(
                email = binding.loginEmail.text?.toString().orEmpty(),
                password = binding.loginPassword.text?.toString().orEmpty()
            )
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                LoginState.Idle -> {
                    binding.loginButton.isEnabled = true
                    binding.loginError.isVisible = false
                }
                LoginState.Loading -> {
                    binding.loginButton.isEnabled = false
                    binding.loginError.isVisible = false
                }
                LoginState.Success -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is LoginState.Error -> {
                    binding.loginButton.isEnabled = true
                    binding.loginError.text = state.message
                    binding.loginError.isVisible = true
                }
            }
        }
    }
}
