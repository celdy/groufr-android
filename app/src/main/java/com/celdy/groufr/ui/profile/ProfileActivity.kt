package com.celdy.groufr.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.celdy.groufr.BuildConfig
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.data.local.UserDao
import com.celdy.groufr.data.storage.TokenStore
import com.celdy.groufr.databinding.ActivityProfileBinding
import com.celdy.groufr.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var userDao: UserDao
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.profileToolbar.setNavigationOnClickListener { finish() }

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

        val name = tokenStore.getUserName().orEmpty()
        binding.profileName.text = name
        binding.profileAvatar.text = name.firstOrNull()?.uppercase().orEmpty()

        lifecycleScope.launch {
            val user = userDao.getById(tokenStore.getUserId())
            user?.email?.let { binding.profileEmail.text = it }
        }

        binding.profileVersionValue.text = BuildConfig.VERSION_NAME

        binding.profileWebsiteValue.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://groufr.com")))
        }

        binding.profileDeveloperWebValue.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://celdy.com")))
        }

        binding.profileSignoutCard.setOnClickListener {
            authRepository.clearTokens()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
