# Play Store Review — Pre-written Appeal (Risk R3)

Use this if the app is misclassified as a financial / payments app and rejected.
Keep it factual and short. Attach a screen recording of the app running in
**airplane mode** and a screenshot of the permissions list (CAMERA only).

---

**Subject:** Appeal — OruBar Pay (package `in.arivanandhan.orubar`) is a QR-scanner
and dialer utility, not a payment app

Dear Google Play Review Team,

OruBar Pay was reviewed as a financial/payments application. We respectfully
request re-review as a **Tools** utility, for the following factual reasons:

1. **The app processes no payments.** It does not hold, move, or settle funds. It
   does not call any payment, UPI, or banking API. It has no wallet and no account
   system.

2. **The app cannot access the network.** It does not request the `INTERNET`
   permission. This is verifiable in the manifest and enforced by Android — the
   app functions identically in airplane mode (recording attached).

3. **The only permission requested is `CAMERA`**, used solely to scan a QR code
   on-device. There is no `SMS`, no `CALL_PHONE`, and no phone-state permission.

4. **No payment credential is ever entered in the app.** There is no UPI PIN field
   of any kind. Payment is completed by the user inside their carrier's `*99#`
   (USSD) session, which is operated by the mobile network operator and NPCI, not
   by this app.

5. **What the app actually does:** it reads a standard `upi://pay` QR string,
   displays the payee name and amount for the user to verify, and — on an explicit
   user tap — opens the phone dialer pre-filled with the public `*99#` code using
   `Intent.ACTION_DIAL`. This is the same mechanism a contacts app uses to pre-fill
   a phone number. The app never auto-dials (`ACTION_CALL` is not used).

The transaction is initiated by the user, on NPCI's public `*99#` service, from
the user's own bank-registered SIM. OruBar Pay is a convenience layer (QR reader +
dialer shortcut) and is correctly categorized under **Tools**.

We are happy to provide any additional information.

Regards,
Arivanandhan Chitheshwaran — orubar@arivanandhan.in

---

## Evidence checklist to attach

- [ ] Screen recording: full scan → confirm → dialer flow in airplane mode.
- [ ] Screenshot: app permissions page showing CAMERA only.
- [ ] Link to the open-source repository and the published APK SHA-256.
- [ ] Data-safety form: "No data collected / No data shared."
