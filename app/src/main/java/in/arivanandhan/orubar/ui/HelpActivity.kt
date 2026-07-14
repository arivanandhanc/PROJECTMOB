package `in`.arivanandhan.orubar.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import `in`.arivanandhan.orubar.databinding.ActivityHelpBinding

/** Fully offline help & troubleshooting (blueprint §7.1). Static content only. */
class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
