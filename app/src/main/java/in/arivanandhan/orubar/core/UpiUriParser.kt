package `in`.arivanandhan.orubar.core

import java.math.BigDecimal

/**
 * Pure-Kotlin parser for UPI QR payloads. Zero Android dependencies so it can be
 * unit-tested on the JVM and audited in isolation — this is the security boundary
 * of the whole app (blueprint §5.3, §7.3).
 *
 * The parser NEVER touches the network, PIN, or any payment API. It only reads a
 * `upi://pay?...` string and tells the UI what to display / dial.
 */
object UpiUriParser {

    /** The *99# per-transaction ceiling. Amounts above this are hard-blocked. */
    val MAX_AMOUNT: BigDecimal = BigDecimal("5000")

    /** VPA format per blueprint §5.3: local@handle, handle is letters only. */
    private val VPA_REGEX = Regex("^[\\w.\\-]{2,64}@[a-zA-Z]{2,64}$")

    /** Reasons a payload is rejected. Codes map to localized strings in the UI. */
    enum class Error {
        MALFORMED_URI,          // couldn't parse at all
        NOT_UPI_QR,             // scheme is not upi://
        MISSING_VPA,            // no pa= parameter
        INVALID_VPA,            // pa= fails the VPA regex
        INVALID_AMOUNT,         // am= is present but not a positive number
        AMOUNT_EXCEEDS_LIMIT,   // am= is above ₹5,000
        UNSUPPORTED_CURRENCY    // cu= is present and not INR
    }

    /** Non-fatal flags surfaced to the user on the confirm screen. */
    enum class Warning {
        NAME_HANDLE_MISMATCH    // pn does not visibly match the VPA (anti-spoof cue)
    }

    sealed class Result {
        data class Success(val payment: UpiPayment) : Result()
        data class Failure(val error: Error) : Result()
    }

    fun parse(raw: String?): Result {
        if (raw == null) return Result.Failure(Error.MALFORMED_URI)
        val input = raw.trim()
        if (input.isEmpty()) return Result.Failure(Error.MALFORMED_URI)

        // Scheme check — case-insensitive, must be exactly the upi scheme.
        val schemeSep = input.indexOf(':')
        if (schemeSep <= 0) return Result.Failure(Error.MALFORMED_URI)
        val scheme = input.substring(0, schemeSep).lowercase()
        if (scheme != "upi") return Result.Failure(Error.NOT_UPI_QR)

        val params = parseQuery(input)

        val pa = params["pa"]?.trim().orEmpty()
        if (pa.isEmpty()) return Result.Failure(Error.MISSING_VPA)
        if (!VPA_REGEX.matches(pa)) return Result.Failure(Error.INVALID_VPA)

        // Currency: default INR; reject anything else if explicitly present.
        val cuRaw = params["cu"]?.trim()
        val currency = if (cuRaw.isNullOrEmpty()) "INR" else cuRaw.uppercase()
        if (currency != "INR") return Result.Failure(Error.UNSUPPORTED_CURRENCY)

        // Amount: optional. Blank am= is treated as absent (user will enter it).
        val amRaw = params["am"]?.trim()
        var amount: BigDecimal? = null
        if (!amRaw.isNullOrEmpty()) {
            val parsed = try {
                BigDecimal(amRaw)
            } catch (e: NumberFormatException) {
                return Result.Failure(Error.INVALID_AMOUNT)
            }
            if (parsed.signum() <= 0) return Result.Failure(Error.INVALID_AMOUNT)
            if (parsed > MAX_AMOUNT) return Result.Failure(Error.AMOUNT_EXCEEDS_LIMIT)
            amount = normalizeAmount(parsed)
        }

        val name = params["pn"]?.trim()?.takeIf { it.isNotEmpty() }
        val note = params["tn"]?.trim()?.takeIf { it.isNotEmpty() }
        val merchantCode = params["mc"]?.trim()?.takeIf { it.isNotEmpty() }

        val warnings = buildSet {
            if (name != null && !nameMatchesHandle(name, pa)) {
                add(Warning.NAME_HANDLE_MISMATCH)
            }
        }

        return Result.Success(
            UpiPayment(
                payeeVpa = pa,
                payeeName = name,
                amount = amount,
                currency = currency,
                note = note,
                merchantCode = merchantCode,
                warnings = warnings,
                rawUri = input
            )
        )
    }

    /**
     * Validates an amount the user typed in for an amount-less QR. Same rules as
     * an inline `am=` value. Returns null on success, or the relevant [Error].
     */
    fun validateEnteredAmount(text: String?): Error? {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return Error.INVALID_AMOUNT
        val parsed = try {
            BigDecimal(t)
        } catch (e: NumberFormatException) {
            return Error.INVALID_AMOUNT
        }
        if (parsed.signum() <= 0) return Error.INVALID_AMOUNT
        if (parsed > MAX_AMOUNT) return Error.AMOUNT_EXCEEDS_LIMIT
        return null
    }

    /**
     * Canonicalizes an amount: drops trailing fractional zeros ("99.50" -> "99.5")
     * but keeps whole numbers as scale-0 integers ("250" -> "250", not "2.5E+2").
     * stripTrailingZeros alone yields a negative scale for round numbers, which
     * makes BigDecimal.equals surprising — this restores an intuitive value.
     */
    private fun normalizeAmount(bd: BigDecimal): BigDecimal {
        val stripped = bd.stripTrailingZeros()
        return if (stripped.scale() < 0) stripped.setScale(0) else stripped
    }

    // --- internals -------------------------------------------------------

    /**
     * Splits the query string after the first '?' into decoded key/value pairs.
     * Percent-decoding is done by our own [percentDecode] so that '+' is kept
     * literal (URLDecoder would turn it into a space and corrupt some names).
     * If a key repeats, the first occurrence wins.
     */
    private fun parseQuery(uri: String): Map<String, String> {
        val q = uri.indexOf('?')
        if (q < 0 || q == uri.length - 1) return emptyMap()
        val query = uri.substring(q + 1)
        val out = LinkedHashMap<String, String>()
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            val key: String
            val value: String
            if (eq < 0) {
                key = percentDecode(pair)
                value = ""
            } else {
                key = percentDecode(pair.substring(0, eq))
                value = percentDecode(pair.substring(eq + 1))
            }
            val normKey = key.trim().lowercase()
            if (normKey.isNotEmpty() && !out.containsKey(normKey)) {
                out[normKey] = value
            }
        }
        return out
    }

    /** Decodes %XX escapes as UTF-8; leaves malformed escapes and '+' untouched. */
    private fun percentDecode(s: String): String {
        if (s.indexOf('%') < 0) return s
        val bytes = ArrayList<Byte>(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val hi = hexValue(s[i + 1])
                val lo = hexValue(s[i + 2])
                if (hi >= 0 && lo >= 0) {
                    bytes.add(((hi shl 4) or lo).toByte())
                    i += 3
                    continue
                }
            }
            // Not a valid escape — emit the char's UTF-8 bytes as-is.
            for (b in c.toString().toByteArray(Charsets.UTF_8)) bytes.add(b)
            i++
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }

    private fun hexValue(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> -1
    }

    /**
     * Heuristic anti-spoof check: does the display name plausibly relate to the
     * VPA's local part? Conservative — only reports a mismatch when there is
     * clearly no shared substring, to avoid false alarms.
     */
    internal fun nameMatchesHandle(name: String, vpa: String): Boolean {
        val local = vpa.substringBefore('@')
        val a = name.lowercase().filter { it.isLetterOrDigit() }
        val b = local.lowercase().filter { it.isLetterOrDigit() }
        if (a.length < 3 || b.length < 3) return true // too short to judge
        val shorter = if (a.length <= b.length) a else b
        val longer = if (a.length <= b.length) b else a
        val window = minOf(4, shorter.length)
        var i = 0
        while (i + window <= shorter.length) {
            if (longer.contains(shorter.substring(i, i + window))) return true
            i++
        }
        return false
    }
}
