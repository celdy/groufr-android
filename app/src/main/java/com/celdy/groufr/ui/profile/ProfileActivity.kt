package com.celdy.groufr.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.data.storage.TokenStore
import com.celdy.groufr.databinding.ActivityProfileBinding
import com.celdy.groufr.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var tokenStore: TokenStore
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.profileToolbar.setNavigationOnClickListener { finish() }
        binding.profileName.text = tokenStore.getUserName().orEmpty()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.profileToolbar.updatePadding(top = systemBars.top)
            binding.root.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        binding.profileLogout.setOnClickListener {
            authRepository.clearTokens()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
