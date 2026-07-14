package `in`.arivanandhan.orubar.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class UpiUriParserTest {

    private fun success(raw: String): UpiPayment {
        val r = UpiUriParser.parse(raw)
        assertTrue("expected success for [$raw] but was $r", r is UpiUriParser.Result.Success)
        return (r as UpiUriParser.Result.Success).payment
    }

    private fun failure(raw: String?): UpiUriParser.Error {
        val r = UpiUriParser.parse(raw)
        assertTrue("expected failure for [$raw] but was $r", r is UpiUriParser.Result.Failure)
        return (r as UpiUriParser.Result.Failure).error
    }

    // --- Happy paths: real-world QR shapes -------------------------------

    @Test fun parsesGpayMerchantSticker() {
        val p = success("upi://pay?pa=merchant.store@okhdfcbank&pn=Merchant%20Store&mc=5411&am=250&cu=INR")
        assertEquals("merchant.store@okhdfcbank", p.payeeVpa)
        assertEquals("Merchant Store", p.payeeName)
        assertEquals(BigDecimal("250"), p.amount)
        assertEquals("INR", p.currency)
        assertEquals("5411", p.merchantCode)
        assertFalse(p.requiresAmountEntry)
    }

    @Test fun parsesPhonePeP2p() {
        val p = success("upi://pay?pa=ravi123@ybl&pn=Ravi%20Kumar")
        assertEquals("ravi123@ybl", p.payeeVpa)
        assertEquals("Ravi Kumar", p.payeeName)
        assertNull(p.amount)
        assertTrue(p.requiresAmountEntry)
        assertEquals("ybl", p.handle)
    }

    @Test fun parsesAmountlessKiranaQr() {
        val p = success("upi://pay?pa=murugan.stores@ybl&pn=Murugan%20Stores")
        assertTrue(p.requiresAmountEntry)
        assertFalse(p.warnings.contains(UpiUriParser.Warning.NAME_HANDLE_MISMATCH))
    }

    @Test fun parsesPaytmSoundboxWithNote() {
        val p = success("upi://pay?pa=paytmqr123@paytm&pn=Kirana&tn=Order%2042&am=99.50")
        assertEquals("Order 42", p.note)
        assertEquals(BigDecimal("99.5"), p.amount)
        assertEquals("99.5", p.amountPlain())
    }

    @Test fun uppercaseSchemeAccepted() {
        val p = success("UPI://pay?pa=test.vpa@okaxis&pn=Test")
        assertEquals("test.vpa@okaxis", p.payeeVpa)
    }

    @Test fun handlesEncodedAtSign() {
        val p = success("upi://pay?pa=user%40okicici&pn=User")
        assertEquals("user@okicici", p.payeeVpa)
    }

    @Test fun plusSignKeptLiteralInName() {
        // URLDecoder would turn '+' into a space; we keep it literal.
        val p = success("upi://pay?pa=abc.def@ybl&pn=A+B%20Traders")
        assertEquals("A+B Traders", p.payeeName)
    }

    @Test fun amountAtExactCapAccepted() {
        val p = success("upi://pay?pa=shop@ybl&pn=Shop&am=5000")
        assertEquals(BigDecimal("5000"), p.amount)
    }

    @Test fun repeatedKeyFirstWins() {
        val p = success("upi://pay?pa=first@ybl&pa=second@ybl&pn=First")
        assertEquals("first@ybl", p.payeeVpa)
    }

    @Test fun missingCurrencyDefaultsToInr() {
        val p = success("upi://pay?pa=shop@ybl&pn=Shop")
        assertEquals("INR", p.currency)
    }

    @Test fun numericVpaLocalPartAccepted() {
        val p = success("upi://pay?pa=9876543210@ybl&pn=Phone%20Pe")
        assertEquals("9876543210@ybl", p.payeeVpa)
    }

    // --- Rejections ------------------------------------------------------

    @Test fun rejectsNonUpiScheme() {
        assertEquals(UpiUriParser.Error.NOT_UPI_QR, failure("https://pay?pa=shop@ybl"))
        assertEquals(UpiUriParser.Error.NOT_UPI_QR, failure("javascript:alert(1)"))
        assertEquals(UpiUriParser.Error.NOT_UPI_QR, failure("tel:*99%23"))
    }

    @Test fun rejectsMissingVpa() {
        assertEquals(UpiUriParser.Error.MISSING_VPA, failure("upi://pay?pn=NoVpa&am=100"))
    }

    @Test fun rejectsMalformedVpa() {
        assertEquals(UpiUriParser.Error.INVALID_VPA, failure("upi://pay?pa=notavpa&pn=X"))
        assertEquals(UpiUriParser.Error.INVALID_VPA, failure("upi://pay?pa=a@b&pn=X")) // local too short
        assertEquals(UpiUriParser.Error.INVALID_VPA, failure("upi://pay?pa=shop@ybl123&pn=X")) // digits in handle
        assertEquals(UpiUriParser.Error.INVALID_VPA, failure("upi://pay?pa=sh op@ybl&pn=X")) // space
    }

    @Test fun rejectsEmojiVpa() {
        assertEquals(UpiUriParser.Error.INVALID_VPA, failure("upi://pay?pa=😀shop@ybl&pn=X"))
    }

    @Test fun rejectsNegativeAmount() {
        assertEquals(UpiUriParser.Error.INVALID_AMOUNT, failure("upi://pay?pa=shop@ybl&am=-100"))
    }

    @Test fun rejectsZeroAmount() {
        assertEquals(UpiUriParser.Error.INVALID_AMOUNT, failure("upi://pay?pa=shop@ybl&am=0"))
    }

    @Test fun rejectsNonNumericAmount() {
        assertEquals(UpiUriParser.Error.INVALID_AMOUNT, failure("upi://pay?pa=shop@ybl&am=abc"))
    }

    @Test fun rejectsAmountOverCap() {
        assertEquals(UpiUriParser.Error.AMOUNT_EXCEEDS_LIMIT, failure("upi://pay?pa=shop@ybl&am=99999"))
        assertEquals(UpiUriParser.Error.AMOUNT_EXCEEDS_LIMIT, failure("upi://pay?pa=shop@ybl&am=5000.01"))
    }

    @Test fun rejectsForeignCurrency() {
        assertEquals(UpiUriParser.Error.UNSUPPORTED_CURRENCY, failure("upi://pay?pa=shop@ybl&am=100&cu=USD"))
    }

    @Test fun rejectsNullEmptyAndGarbage() {
        assertEquals(UpiUriParser.Error.MALFORMED_URI, failure(null))
        assertEquals(UpiUriParser.Error.MALFORMED_URI, failure(""))
        assertEquals(UpiUriParser.Error.MALFORMED_URI, failure("   "))
        assertEquals(UpiUriParser.Error.MALFORMED_URI, failure("no-scheme-here"))
    }

    @Test fun blankAmountTreatedAsAbsent() {
        val p = success("upi://pay?pa=shop@ybl&am=&pn=Shop")
        assertTrue(p.requiresAmountEntry)
    }

    @Test fun oversizedPayloadDoesNotCrash() {
        val huge = "upi://pay?pa=shop@ybl&pn=" + "A".repeat(200_000)
        val r = UpiUriParser.parse(huge)
        assertTrue(r is UpiUriParser.Result.Success)
    }

    @Test fun whitespacePaddedInputTrimmed() {
        val p = success("   upi://pay?pa=shop@ybl&pn=Shop   ")
        assertEquals("shop@ybl", p.payeeVpa)
    }

    // --- Anti-spoof warning ---------------------------------------------

    @Test fun flagsNameHandleMismatch() {
        val p = success("upi://pay?pa=randomxyz@ybl&pn=Reliance%20Fresh")
        assertTrue(p.warnings.contains(UpiUriParser.Warning.NAME_HANDLE_MISMATCH))
    }

    @Test fun noMismatchWhenNamesAlign() {
        val p = success("upi://pay?pa=muruganstores@ybl&pn=Murugan%20Stores")
        assertFalse(p.warnings.contains(UpiUriParser.Warning.NAME_HANDLE_MISMATCH))
    }

    @Test fun noMismatchWhenNameAbsent() {
        val p = success("upi://pay?pa=randomxyz@ybl")
        assertFalse(p.warnings.contains(UpiUriParser.Warning.NAME_HANDLE_MISMATCH))
    }

    // --- Entered-amount validation --------------------------------------

    @Test fun validatesEnteredAmount() {
        assertNull(UpiUriParser.validateEnteredAmount("250"))
        assertNull(UpiUriParser.validateEnteredAmount("5000"))
        assertEquals(UpiUriParser.Error.INVALID_AMOUNT, UpiUriParser.validateEnteredAmount(""))
        assertEquals(UpiUriParser.Error.INVALID_AMOUNT, UpiUriParser.validateEnteredAmount("0"))
        assertEquals(UpiUriParser.Error.INVALID_AMOUNT, UpiUriParser.validateEnteredAmount("-5"))
        assertEquals(UpiUriParser.Error.INVALID_AMOUNT, UpiUriParser.validateEnteredAmount("abc"))
        assertEquals(UpiUriParser.Error.AMOUNT_EXCEEDS_LIMIT, UpiUriParser.validateEnteredAmount("5001"))
    }
}
