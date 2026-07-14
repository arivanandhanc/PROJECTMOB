package `in`.arivanandhan.orubar.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import `in`.arivanandhan.orubar.R
import `in`.arivanandhan.orubar.core.UpiMessages
import `in`.arivanandhan.orubar.core.UpiPayment
import `in`.arivanandhan.orubar.core.UpiUriParser
import `in`.arivanandhan.orubar.data.TxnLog
import `in`.arivanandhan.orubar.databinding.ActivityConfirmBinding

/**
 * The trust screen (blueprint §3.1, §5.6). Parses the scanned payload, shows a
 * large verified confirmation, surfaces anti-spoof cues, and — only on an
 * explicit tap — moves to the cheat-card / dialer step. No money, no PIN here.
 */
class ConfirmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmBinding
    private var payment: UpiPayment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val raw = intent.getStringExtra(EXTRA_RAW_QR)
        when (val result = UpiUriParser.parse(raw)) {
            is UpiUriParser.Result.Failure -> showError(result.error)
            is UpiUriParser.Result.Success -> showSuccess(result.payment)
        }

        binding.btnScanAgainError.setOnClickListener { rescan() }
    }

    private fun showError(error: UpiUriParser.Error) {
        binding.errorPanel.visibility = View.VISIBLE
        binding.successPanel.visibility = View.GONE
        binding.errorText.setText(UpiMessages.stringResFor(error))
    }

    private fun showSuccess(p: UpiPayment) {
        payment = p
        binding.errorPanel.visibility = View.GONE
        binding.successPanel.visibility = View.VISIBLE

        binding.payeeName.text = p.payeeName ?: getString(R.string.confirm_no_name)
        binding.payeeVpa.text = p.payeeVpa

        // Anti-spoof: name/handle mismatch banner.
        binding.spoofWarning.visibility =
            if (p.warnings.contains(UpiUriParser.Warning.NAME_HANDLE_MISMATCH))
                View.VISIBLE else View.GONE

        // First-seen vs known VPA cue.
        val seen = TxnLog(this).hasSeen(p.payeeVpa)
        binding.seenNote.setText(
            if (seen) R.string.confirm_seen_before else R.string.confirm_first_seen
        )

        if (p.requiresAmountEntry) {
            binding.amountInputLayout.visibility = View.VISIBLE
            binding.amountFixed.visibility = View.GONE
        } else {
            binding.amountFixed.visibility = View.VISIBLE
            binding.amountInputLayout.visibility = View.GONE
            binding.amountFixed.text = getString(R.string.confirm_rupee, p.amountPlain())
        }

        binding.btnPay.setOnClickListener { onPay(p) }
    }

    private fun onPay(p: UpiPayment) {
        val amount: String = if (p.requiresAmountEntry) {
            val entered = binding.amountInput.text?.toString()
            val err = UpiUriParser.validateEnteredAmount(entered)
            if (err != null) {
                binding.amountInputLayout.error = getString(R.string.err_enter_valid_amount)
                return
            }
            binding.amountInputLayout.error = null
            entered!!.trim()
        } else {
            p.amountPlain()!!
        }

        startActivity(
            Intent(this, CheatCardActivity::class.java)
                .putExtra(CheatCardActivity.EXTRA_VPA, p.payeeVpa)
                .putExtra(CheatCardActivity.EXTRA_NAME, p.payeeName)
                .putExtra(CheatCardActivity.EXTRA_AMOUNT, amount)
        )
        finish()
    }

    private fun rescan() {
        startActivity(Intent(this, ScannerActivity::class.java))
        finish()
    }

    companion object {
        const val EXTRA_RAW_QR = "raw_qr"
    }
}
