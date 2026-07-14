package `in`.arivanandhan.orubar.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.arivanandhan.orubar.R
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented checks for the trust screen — verifies the parser output is
 * rendered correctly and that bad QRs land on the error state. Runs on device
 * / emulator (blueprint §7.3 instrumented smoke test).
 */
@RunWith(AndroidJUnit4::class)
class ConfirmActivityTest {

    private fun launchWith(raw: String): ActivityScenario<ConfirmActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ConfirmActivity::class.java)
            .putExtra(ConfirmActivity.EXTRA_RAW_QR, raw)
        return ActivityScenario.launch(intent)
    }

    @Test fun fixedAmountQrShowsNameAndAmountAndPayButton() {
        launchWith("upi://pay?pa=merchant.store@okhdfcbank&pn=Merchant%20Store&am=250").use {
            onView(withId(R.id.payeeName)).check(matches(allOf(isDisplayed(), withText("Merchant Store"))))
            onView(withId(R.id.payeeVpa)).check(matches(withText("merchant.store@okhdfcbank")))
            onView(withId(R.id.amountFixed)).check(matches(isDisplayed()))
            onView(withId(R.id.btnPay)).check(matches(isDisplayed()))
        }
    }

    @Test fun amountlessQrShowsAmountInput() {
        launchWith("upi://pay?pa=murugan.stores@ybl&pn=Murugan%20Stores").use {
            onView(withId(R.id.amountInputLayout)).check(matches(isDisplayed()))
        }
    }

    @Test fun spoofQrShowsWarningBanner() {
        launchWith("upi://pay?pa=randomxyz@ybl&pn=Reliance%20Fresh&am=100").use {
            onView(withId(R.id.spoofWarning)).check(matches(isDisplayed()))
        }
    }

    @Test fun invalidQrShowsErrorPanel() {
        launchWith("javascript:alert(1)").use {
            onView(withId(R.id.errorText)).check(matches(isDisplayed()))
            onView(withId(R.id.btnScanAgainError)).check(matches(isDisplayed()))
        }
    }

    @Test fun overCapAmountShowsError() {
        launchWith("upi://pay?pa=shop@ybl&pn=Shop&am=99999").use {
            onView(withId(R.id.errorText)).check(matches(isDisplayed()))
        }
    }
}
