package `in`.arivanandhan.orubar.core

/**
 * Shared, in-memory hand-off between [`in`.arivanandhan.orubar.ui.CheatCardActivity]
 * and the accessibility service that fills the live *99# menu. Holds ONLY the
 * VPA and amount for the current payment plus a stage cursor.
 *
 * It never holds a PIN and is cleared as soon as the flow reaches the PIN step
 * or times out. Because the app has no INTERNET permission, nothing here can be
 * exfiltrated even in principle.
 */
object UssdSession {

    /** The interactive steps the service drives, in order. PIN is NOT here — the
     *  user always enters the PIN themselves. */
    enum class Stage { SEND_MONEY, RECIPIENT_UPI_ID, ENTER_VPA, ENTER_AMOUNT, DONE }

    @Volatile var active: Boolean = false
        private set

    @Volatile var vpa: String = ""
        private set

    @Volatile var amount: String = ""
        private set

    /** Wall-clock ms when the session started; the service self-cancels if the
     *  flow is stale (user backed out, wrong SIM, carrier error). */
    @Volatile var startedAt: Long = 0L
        private set

    @Volatile var stage: Stage = Stage.SEND_MONEY

    private const val TIMEOUT_MS = 90_000L

    @Synchronized
    fun start(vpa: String, amount: String) {
        this.vpa = vpa
        this.amount = amount
        this.stage = Stage.SEND_MONEY
        this.startedAt = System.currentTimeMillis()
        this.active = true
    }

    @Synchronized
    fun stop() {
        active = false
        vpa = ""
        amount = ""
        stage = Stage.DONE
    }

    fun isExpired(): Boolean =
        !active || System.currentTimeMillis() - startedAt > TIMEOUT_MS
}
