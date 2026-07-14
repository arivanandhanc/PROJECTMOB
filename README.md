# OruBar Pay

**Scan any UPI QR → pay via `*99#` → zero internet.**

OruBar Pay is a tiny Android utility that reads a standard UPI QR code offline and
opens your phone's dialer on NPCI's `*99#` USSD service, so you can pay on a cheap
phone with one bar of signal and no data. It holds no money, calls no payment API,
stores no credentials, and **cannot go online** — it does not even request the
`INTERNET` permission.

> Working title. Verify the "OruBar Pay" trademark before public launch.

---

## 📥 Download & install

Grab the signed APK from [`dist/OruBar-Pay-v1.0.0.apk`](dist/OruBar-Pay-v1.0.0.apk)
(1.6 MB), copy it to your phone (WhatsApp / Drive / Bluetooth / cable), tap it,
allow "install from unknown sources", and install.

- **SHA-256:** `07c071a1fb3ad06d38f97fb4589a8a588b7ac0ed082338d4ca91829e616d6b4f`
  (also in `dist/OruBar-Pay-v1.0.0.apk.sha256` — verify before installing).
- A `-debug` APK is also included for development.

---

## Why it's different (security)

- **Nothing to steal.** No server, no accounts, no cloud database. The whole
  attack surface is a QR-string parser.
- **The UPI PIN never enters the app.** You type it inside your carrier's secure
  `*99#` session. There is no PIN field anywhere in OruBar Pay.
- **OS-enforced no-exfiltration.** With `INTERNET` omitted from the manifest,
  Android itself blocks any network call. Verifiable in airplane mode — this holds
  even in the optional auto-pay mode below.
- **Manual flow needs only `CAMERA`.** No SMS, no phone-state reads. The optional
  "Pay automatically" mode additionally uses `CALL_PHONE` + an accessibility
  service, both user-granted and off by default — see [docs/AUTOMATION.md](docs/AUTOMATION.md).
- **Malicious-QR defense.** Strict VPA validation, a large verified-name confirm
  screen, a name/handle mismatch warning, a ₹5,000 cap, and a first-seen cue.

## The ownership boundary (why no license is needed)

| The app owns | The carrier + NPCI own |
|---|---|
| Camera, QR decode, URI parsing, validation | The `*99#` USSD session and menu |
| Confirm UI, dialer launch (ACTION_DIAL) | PIN entry, authentication |
| Local, device-only history | Money movement, settlement, SMS receipt |

The app is functionally a QR reader plus a dialer shortcut. It uses
`Intent.ACTION_DIAL` (pre-fills the dialer; the user presses call) — never
`ACTION_CALL`. The transaction is user-initiated on NPCI's public service from the
user's own bank-registered SIM.

## Build

Requirements: **JDK 17**, Android SDK (platform 34, build-tools 34).

```bash
# Run the parser unit tests (the security boundary)
./gradlew :app:testDebugUnitTest

# Assemble a debug APK
./gradlew :app:assembleDebug

# Release build (R8 + resource shrinking, target < 4 MB)
./gradlew :app:assembleRelease
```

On Windows, use `gradlew.bat`. Set `JAVA_HOME` to a JDK 17 install.

## Module map

```
core/
  UpiUriParser.kt   Pure-Kotlin UPI QR parser (100% unit-tested) — the security boundary
  UpiPayment.kt     Immutable parsed-payment model
  UssdLauncher.kt   Builds tel:*99%23 ACTION_DIAL intent; dual-SIM detection
  UpiMessages.kt    Parser error → localized string mapping
data/
  TxnLog.kt         Device-only JSON history (no PIN, no account number)
ui/
  MainActivity, OnboardingActivity, ScannerActivity, ConfirmActivity,
  CheatCardActivity, HistoryActivity, HelpActivity
```

## Languages

Tamil, English, Kannada, Hindi. Non-English strings are machine-assisted and need
a native-speaker review before public launch (blueprint §7.2).

## Reproducible builds & trust

The release APK is meant to be built reproducibly and its SHA-256 published, so
anyone can verify the binary matches this source. See `docs/` for the privacy
policy and the Play Store appeal document.

## License

See [LICENSE](LICENSE). Free and open source — trust is the product.
