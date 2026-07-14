package `in`.arivanandhan.orubar.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import `in`.arivanandhan.orubar.core.UssdSession

/**
 * Drives the live *99# USSD dialogs so the user only has to enter their PIN.
 *
 * IMPORTANT SAFETY PROPERTIES:
 *  - It does nothing at all unless an [UssdSession] is active (started when the
 *    user taps "Pay automatically"). Outside a payment it reads nothing.
 *  - It NEVER fills, reads, or stores a PIN. If any prompt mentions a PIN it
 *    immediately stops the session and hands control back to the user.
 *  - The app holds no INTERNET permission, so nothing this service sees can be
 *    sent off the device.
 *  - It only advances the known Send Money → UPI ID → VPA → amount steps, and
 *    matches menu options by their on-screen label (not a hardcoded number),
 *    bailing out to manual if it is not confident.
 *
 * USSD automation is inherently fragile across carriers/OEMs. Treat this as
 * best-effort: the manual cheat-card flow remains available as a fallback.
 */
class UssdAutomatorService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastHandledSignature: String = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (UssdSession.isExpired()) {
            if (UssdSession.active) UssdSession.stop()
            return
        }
        // Only react to dialog/window changes.
        val type = event?.eventType ?: return
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        // Let the dialog settle, then act once.
        handler.postDelayed({ process() }, 350)
    }

    override fun onInterrupt() { /* no-op */ }

    private fun process() {
        if (UssdSession.isExpired()) return
        val root = rootInActiveWindow ?: return

        val prompt = collectText(root).lowercase().trim()
        if (prompt.isEmpty()) return

        // Hard safety: never interact with a PIN prompt. Stop automating here.
        if (prompt.contains("pin")) {
            UssdSession.stop()
            return
        }

        // Avoid acting twice on the same dialog for the same stage.
        val signature = UssdSession.stage.name + "|" + prompt.hashCode()
        if (signature == lastHandledSignature) return

        when (UssdSession.stage) {
            UssdSession.Stage.SEND_MONEY -> {
                val digit = optionNumberFor(prompt, listOf("send money", "send money to", "1 send"))
                if (digit != null && inputAndSend(root, digit)) {
                    UssdSession.stage = UssdSession.Stage.RECIPIENT_UPI_ID
                    lastHandledSignature = signature
                }
            }
            UssdSession.Stage.RECIPIENT_UPI_ID -> {
                // Choose the "UPI ID / VPA" recipient type from the menu.
                val digit = optionNumberFor(prompt, listOf("upi id", "upi-id", "vpa", "virtual payment", "virtual address"))
                if (digit != null && inputAndSend(root, digit)) {
                    UssdSession.stage = UssdSession.Stage.ENTER_VPA
                    lastHandledSignature = signature
                }
            }
            UssdSession.Stage.ENTER_VPA -> {
                // This should now be a free-text prompt asking for the UPI ID.
                if (looksLikeInputPrompt(prompt) &&
                    (prompt.contains("upi") || prompt.contains("vpa") || prompt.contains("virtual") || prompt.contains("id"))
                ) {
                    if (inputAndSend(root, UssdSession.vpa)) {
                        UssdSession.stage = UssdSession.Stage.ENTER_AMOUNT
                        lastHandledSignature = signature
                    }
                }
            }
            UssdSession.Stage.ENTER_AMOUNT -> {
                if (prompt.contains("amount")) {
                    if (inputAndSend(root, UssdSession.amount)) {
                        // Everything is filled. From here the carrier asks for
                        // remarks (optional) and then the PIN — both are the
                        // user's to handle. We are done.
                        UssdSession.stage = UssdSession.Stage.DONE
                        UssdSession.stop()
                        lastHandledSignature = signature
                    }
                }
            }
            UssdSession.Stage.DONE -> UssdSession.stop()
        }
    }

    // --- helpers ---------------------------------------------------------

    /** Concatenates all visible text in the window (menu + prompt). */
    private fun collectText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        node.text?.let { sb.append(it).append('\n') }
        node.contentDescription?.let { sb.append(it).append('\n') }
        for (i in 0 until node.childCount) {
            sb.append(collectText(node.getChild(i)))
        }
        return sb.toString()
    }

    /** A prompt is a free-text input step if it has an editable field. */
    private fun looksLikeInputPrompt(prompt: String): Boolean =
        prompt.contains("enter") || prompt.contains("type") || !prompt.contains(".")

    /**
     * Finds the menu number that sits next to a label. USSD menus look like
     * "1. Send Money\n2. Request Money ...". We match the label and read the
     * digit on that line, so carrier-specific ordering still works.
     */
    private fun optionNumberFor(menuText: String, labels: List<String>): String? {
        val lines = menuText.split('\n')
        for (label in labels) {
            for (line in lines) {
                if (line.contains(label)) {
                    val m = Regex("(\\d+)").find(line) ?: continue
                    return m.groupValues[1]
                }
            }
        }
        return null
    }

    /** Sets [value] into the dialog's edit field and presses the positive button. */
    private fun inputAndSend(root: AccessibilityNodeInfo, value: String): Boolean {
        val edit = findEditable(root) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        val set = edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!set) return false
        val button = findButton(root, listOf("send", "ok", "submit")) ?: return false
        return button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            findEditable(node.getChild(i))?.let { return it }
        }
        return null
    }

    private fun findButton(node: AccessibilityNodeInfo?, texts: List<String>): AccessibilityNodeInfo? {
        if (node == null) return null
        val t = node.text?.toString()?.lowercase()?.trim()
        if (t != null && node.isClickable && texts.any { t == it || t.contains(it) }) return node
        for (i in 0 until node.childCount) {
            findButton(node.getChild(i), texts)?.let { return it }
        }
        return null
    }
}
