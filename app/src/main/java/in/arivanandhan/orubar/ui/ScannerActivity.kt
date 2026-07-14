package `in`.arivanandhan.orubar.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import `in`.arivanandhan.orubar.R
import `in`.arivanandhan.orubar.databinding.ActivityScannerBinding

/**
 * Offline QR scanner (ZXing embedded, no Play Services). On a successful decode
 * it hands the raw payload string to [ConfirmActivity] — it does NOT parse or
 * judge here; parsing is the confirm step's job so errors show one clear screen.
 */
class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private var torchOn = false
    private var handled = false

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startScanning()
            } else {
                showPermissionPanel()
                android.widget.Toast.makeText(
                    this, R.string.scan_permission_denied, android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide torch button on devices with no flash.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            binding.btnTorch.visibility = android.view.View.GONE
        }
        binding.btnTorch.setOnClickListener { toggleTorch() }
        binding.btnGrant.setOnClickListener { requestCamera.launch(Manifest.permission.CAMERA) }

        if (hasCamera()) startScanning() else requestCamera.launch(Manifest.permission.CAMERA)
    }

    private fun hasCamera() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun startScanning() {
        binding.permissionPanel.visibility = android.view.View.GONE
        binding.barcodeView.visibility = android.view.View.VISIBLE
        binding.barcodeView.decodeSingle(callback)
    }

    // BarcodeCallback has TWO abstract methods, so it is not a SAM interface —
    // it must be implemented as an explicit object.
    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (handled) return
            val text = result.text ?: return
            handled = true
            binding.barcodeView.pause()
            startActivity(
                Intent(this@ScannerActivity, ConfirmActivity::class.java)
                    .putExtra(ConfirmActivity.EXTRA_RAW_QR, text)
            )
            finish()
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>) {
            // no-op
        }
    }

    private fun toggleTorch() {
        torchOn = !torchOn
        if (torchOn) {
            binding.barcodeView.setTorchOn()
            binding.btnTorch.setText(R.string.scan_torch_off)
        } else {
            binding.barcodeView.setTorchOff()
            binding.btnTorch.setText(R.string.scan_torch_on)
        }
    }

    private fun showPermissionPanel() {
        binding.permissionPanel.visibility = android.view.View.VISIBLE
        binding.barcodeView.visibility = android.view.View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (hasCamera() && !handled) {
            binding.barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }
}
