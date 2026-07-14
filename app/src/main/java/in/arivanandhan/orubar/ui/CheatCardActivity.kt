package `in`.arivanandhan.orubar.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import `in`.arivanandhan.orubar.R
import `in`.arivanandhan.orubar.core.UssdLauncher
import `in`.arivanandhan.orubar.data.TxnLog
import `in`.arivanandhan.orubar.databinding.ActivityCheatcardBinding

/**
 * The guided *99# cheat card (blueprint §5.5). Shows the exact menu steps, offers
 * copy buttons for the VPA and amount (USSD dialogs on most OEMs allow paste),
 * and launches the dialer via ACTION_DIAL. The app hands over here — the user
 * enters their UPI PIN inside the carrier's secure USSD session, never in-app.
 *
 * Implemented as an in-app screen (not a system overlay) so the app keeps its
 * CAMERA-only permission set — the core trust signal.
 */
class CheatCardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheatcardBinding
    private lateinit var vpa: String
    private var name: String? = null
    private lateinit var amount: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheatcardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vpa = intent.getStringExtra(EXTRA_VPA).orEmpty()
        name = intent.getStringExtra(EXTRA_NAME)
        amount = intent.getStringExtra(EXTRA_AMOUNT).orEmpty()

        binding.vpaValue.text = vpa
        binding.amountValue.text = getString(R.string.confirm_rupee, amount)

        if (UssdLauncher.isLikelyDualSim(this)) {
            binding.dualSimHint.visibility = android.view.View.VISIBLE
        }

        binding.btnCopyVpa.setOnClickListener { copy(vpa) }
        binding.btnCopyAmount.setOnClickListener { copy(amount) }
        binding.btnOpenDialer.setOnClickListener { openDialer() }
        binding.btnDone.setOnClickListener { goHome() }
    }

    private fun copy(text: String) {
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("OruBar", text))
        Toast.makeText(this, R.string.cheat_copied, Toast.LENGTH_SHORT).show()
    }

    private fun openDialer() {
        if (!UssdLauncher.canDial(this)) {
            Toast.makeText(this, R.string.cheat_no_dialer, Toast.LENGTH_LONG).show()
            return
        }
        // Pre-copy the VPA so it is ready to paste into the *99# prompt.
        copy(vpa)
        logTxn(dialerOpened = true)
        startActivity(UssdLauncher.dialIntent())
    }

    private fun logTxn(dialerOpened: Boolean) {
        TxnLog(this).add(
            TxnLog.Entry(
                vpa = vpa,
                name = name,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                dialerOpened = dialerOpened
            )
        )
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        const val EXTRA_VPA = "vpa"
        const val EXTRA_NAME = "name"
        const val EXTRA_AMOUNT = "amount"
    }
}
