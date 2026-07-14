# Changelog

## v1.0.0 (in development)

First working build — the PoC Weekend-1 core loop plus MVP scaffolding.

### Added
- **UpiUriParser** — pure-Kotlin UPI QR parser (the security boundary), with a
  28-case unit suite covering real QR shapes and adversarial inputs (javascript:
  URIs, emoji VPAs, negative/oversized amounts, foreign currency, huge payloads).
- **Core flow**: Scan (ZXing, offline, torch) → Confirm (large verified screen,
  spoof warning, amount entry, first-seen cue) → Cheat card (copy VPA/amount,
  dual-SIM hint) → `ACTION_DIAL` `tel:*99%23`.
- **Device-only history** (TxnLog): UPI ID, name, amount, time — no PIN, no
  account number; excluded from cloud backup.
- **Onboarding**, **Help/troubleshooting** (per-carrier), **History** screens.
- Localization: **Tamil, English, Kannada, Hindi** (non-English need native review).
- Manifest: **CAMERA only**, `INTERNET` intentionally omitted.
- Docs: privacy policy, Play appeal, store listing, build runbook.

### Security posture
- No `INTERNET` permission → OS-enforced zero-network, verifiable in airplane mode.
- UPI PIN never entered in-app; strict VPA regex; ₹5,000 cap.

### Verified on this build
- **28/28 unit tests** pass (`UpiUriParser`).
- **5/5 instrumented Espresso tests** pass on an Android 14 emulator
  (confirm screen renders name/VPA/amount, amount-entry, spoof banner, error state).
- **Lint: 0 errors.** Fixed a real crash: `TelephonyManager.getPhoneCount()` is
  API 23+ but minSdk is 21 — it would have crashed on the exact old phones we
  target. Now guarded by SDK-version checks.
- Also fixed: `<queries>` for the DIAL intent (Android 11+ package visibility, so
  `canDial()` is correct), themed/monochrome launcher icon, overdraw (windowBackground).
- **Release APK: 1.54 MB** (budget 4 MB), R8 + resource shrinking.
- **Manifest: CAMERA only**; no network endpoint strings in the dex.
- Manually screenshotted: onboarding, main, help, history (empty), confirm
  (amount-less), confirm (fixed amount + anti-spoof warning). See `screenshots/`.
