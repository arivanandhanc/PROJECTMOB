package `in`.arivanandhan.orubar.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import `in`.arivanandhan.orubar.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGetStarted.setOnClickListener {
            getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(MainActivity.KEY_ONBOARDED, true)
                .apply()
            finish()
        }
    }
}
