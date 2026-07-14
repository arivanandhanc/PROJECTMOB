package `in`.arivanandhan.orubar.core

import `in`.arivanandhan.orubar.R

/** Maps parser [UpiUriParser.Error] codes to localized string resources. */
object UpiMessages {
    fun stringResFor(error: UpiUriParser.Error): Int = when (error) {
        UpiUriParser.Error.MALFORMED_URI -> R.string.err_malformed
        UpiUriParser.Error.NOT_UPI_QR -> R.string.err_not_upi
        UpiUriParser.Error.MISSING_VPA -> R.string.err_missing_vpa
        UpiUriParser.Error.INVALID_VPA -> R.string.err_invalid_vpa
        UpiUriParser.Error.INVALID_AMOUNT -> R.string.err_invalid_amount
        UpiUriParser.Error.AMOUNT_EXCEEDS_LIMIT -> R.string.err_amount_over_cap
        UpiUriParser.Error.UNSUPPORTED_CURRENCY -> R.string.err_unsupported_currency
    }
}
