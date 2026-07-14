# Security Notes

OruBar Pay's security model is *architectural*: there is almost nothing to attack.

## Verifiable claims

| Claim | How to verify |
|---|---|
| Cannot access the network | Manifest has no `INTERNET` permission. App works identically in airplane mode. Dex scan finds no `http(s)://` endpoint strings (only `schemas.android.com` XML namespaces). **This holds even in auto-pay mode** — the accessibility service can read the screen but has nowhere to send anything. |
| Manual flow asks only for Camera | `aapt dump permissions` shows `CAMERA` (plus the AndroidX signature self-permission below). `CALL_PHONE` is declared for the optional auto-pay mode but only used after the user grants it. |
| Never sees the UPI PIN | There is no PIN input field anywhere in the code. PIN is entered inside the carrier's `*99#` session. |
| History never leaves the phone | `TxnLog` writes a private JSON file, excluded from cloud backup via `backup_rules.xml` / `data_extraction_rules.xml`. |

Reproduce the network audit:
```powershell
# permissions
aapt dump permissions app-release.apk         # expect: CAMERA (+ a signature self-permission, see below)
# no endpoints
# unzip the apk, scan classes.dex for  https?://  → only schemas.android.com
```

## About `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`

The merged manifest contains one extra entry:
`in.arivanandhan.orubar.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.

- It is **auto-added by AndroidX Core** (androidx.core 1.13+) to safely register
  non-exported broadcast receivers on Android 14, per platform guidance.
- Its `protectionLevel` is **signature**, and it is **defined by this app for this
  app**. It grants no access to the network, storage, contacts, SMS, or phone.
- Android does **not** show signature self-permissions in the user-facing "App
  permissions" screen, and it is not a data-collection signal. The "only Camera"
  statement to users remains accurate.

It is intentionally left in place: removing it could cause a `SecurityException`
if an AndroidX component registers a guarded receiver at runtime.

## Auto-pay mode (opt-in)

"Pay automatically" adds `CALL_PHONE` + an accessibility service so the app can
dial `*99#` and fill the menu up to the PIN. Safety is preserved by design:

- The service is **inert unless a payment is in progress** and self-cancels after
  90s or at the PIN step.
- It **never fills or reads a PIN** — it stops the instant a prompt mentions one.
- With **no `INTERNET` permission**, screen content it sees cannot be exfiltrated.
- The **manual, CAMERA-only flow remains the default** and Play-safe path.

See `docs/AUTOMATION.md` for the full trade-offs (incl. the Play accessibility
policy note and why it must be field-tested on a real SIM).

## Malicious-QR defense (the one real threat)

The parser (`UpiUriParser`) is the security boundary and is covered by a 28-case
unit suite. Defenses:

- Strict scheme check — only `upi://` is accepted; `javascript:`, `https:`, `tel:`
  etc. are rejected.
- Strict VPA regex; emoji / spaced / malformed VPAs rejected.
- Amount validation: non-numeric, zero, negative, and `> ₹5,000` all rejected.
- Foreign currency rejected.
- Name/handle mismatch surfaces an on-screen anti-spoof warning.
- First-seen VPA cue on the confirm screen.
- Oversized payloads (200 KB) parse without crashing.

## Supply-chain trust

- Only one third-party runtime dependency of note: ZXing (`zxing-android-embedded`).
- Open source; publish the release APK SHA-256; sign consistently.
- Aim for a reproducible build so the published binary can be matched to source.
