# "Pay automatically" mode — how it works & its trade-offs

By default OruBar Pay hands you a pre-loaded `*99#` dialer and you tap through the
menu. **Pay automatically** is an *opt-in* mode that does more of that for you.

## What it does

```
You tap "Pay automatically"
  → app dials *99# itself            (needs Phone permission)
  → accessibility service fills:
        Send Money → UPI ID → <VPA> → <amount>   (needs Accessibility permission)
  → it STOPS at the PIN screen
  → you enter only your UPI PIN
```

## Why two permissions are needed

- **Phone (`CALL_PHONE`)** — to place the `*99#` call itself instead of you
  pressing the green button.
- **Accessibility service** — a UPI ID like `name@bank` contains letters and `@`,
  which **cannot** be put in a dial string (dialers only accept digits, `*`, `#`).
  So the only way for the app to type the VPA and amount into the live USSD menu
  is to fill the on-screen dialogs — which requires the accessibility API.

Both are **opt-in**: the app asks for them only when you choose auto-pay, and the
default manual flow needs neither.

## Safety properties (kept even in auto mode)

- **The PIN is never touched.** The service stops the moment a prompt mentions a
  PIN, and never fills a PIN field. You always enter the PIN yourself.
- **Still no internet.** The app has no `INTERNET` permission. Even though the
  accessibility service can read the screen while you pay, there is nowhere for
  that data to go — it cannot leave the phone.
- **Only active during a payment.** The service ignores everything unless you
  just tapped "Pay automatically"; it self-cancels after 90 seconds or on the PIN
  step.
- **Matches menu items by label, not a fixed number**, and bails out to the manual
  flow if it is not confident, so it degrades safely.

## Honest limitations (read before trusting it with money)

- **It cannot be tested on an emulator** — USSD menus only exist on a real SIM.
  Validate on a real phone with a **₹1** payment before relying on it.
- **USSD menus vary by carrier, circle, language and phone brand.** Auto-fill is
  best-effort; on some networks it may mis-step, in which case use the manual
  cheat-card flow (always available on the same screen).
- **Play Store note:** Google restricts accessibility-service use to genuine
  accessibility purposes. This mode may need justification during review, or may
  be better shipped only via the **direct APK / F-Droid** channels. The manual
  flow (CAMERA-only) is the Play-safe default.

## If you don't want it

Do nothing. Use **"Or do it step by step"** on the same screen — that path never
requests Phone or Accessibility permissions and behaves exactly as before.
