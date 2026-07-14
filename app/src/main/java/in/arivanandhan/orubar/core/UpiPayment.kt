package `in`.arivanandhan.orubar.core

import java.math.BigDecimal

/**
 * A validated UPI payment target, produced by [UpiUriParser]. Immutable and free
 * of Android types so it can cross the parser/UI boundary cleanly.
 *
 * [amount] is null when the QR carried no `am=` value — the UI must prompt the
 * user for an amount before dialing (kirana QRs are usually amount-less).
 */
data class UpiPayment(
    val payeeVpa: String,
    val payeeName: String?,
    val amount: BigDecimal?,
    val currency: String,
    val note: String?,
    val merchantCode: String?,
    val warnings: Set<UpiUriParser.Warning>,
    val rawUri: String
) {
    /** True when the user still needs to enter an amount before paying. */
    val requiresAmountEntry: Boolean get() = amount == null

    /** The payee handle after '@' — e.g. "ybl", "okhdfcbank". */
    val handle: String get() = payeeVpa.substringAfter('@', "")

    /** A plain amount string for display / dialing, e.g. "250" or "99.50". */
    fun amountPlain(): String? = amount?.toPlainString()
}
