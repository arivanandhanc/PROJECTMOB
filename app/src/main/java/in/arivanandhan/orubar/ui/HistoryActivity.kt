package `in`.arivanandhan.orubar.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import `in`.arivanandhan.orubar.R
import `in`.arivanandhan.orubar.data.TxnLog
import `in`.arivanandhan.orubar.databinding.ActivityHistoryBinding
import `in`.arivanandhan.orubar.databinding.ItemHistoryBinding
import java.text.DateFormat
import java.util.Date

/** Device-only payment history viewer (blueprint §5.2). */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var log: TxnLog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        log = TxnLog(this)

        binding.btnClear.setOnClickListener {
            log.clear()
            render()
            android.widget.Toast.makeText(this, R.string.history_cleared, android.widget.Toast.LENGTH_SHORT).show()
        }
        render()
    }

    private fun render() {
        val entries = log.all()
        binding.listContainer.removeAllViews()
        if (entries.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.btnClear.visibility = View.GONE
            return
        }
        binding.emptyText.visibility = View.GONE
        binding.btnClear.visibility = View.VISIBLE

        val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        val inflater = LayoutInflater.from(this)
        for (e in entries) {
            val row = ItemHistoryBinding.inflate(inflater, binding.listContainer, false)
            row.itemName.text = e.name ?: getString(R.string.confirm_no_name)
            row.itemAmount.text = e.amount?.let { getString(R.string.confirm_rupee, it) } ?: ""
            row.itemVpa.text = e.vpa
            row.itemTime.text = fmt.format(Date(e.timestamp))
            binding.listContainer.addView(row.root)
        }
    }
}
