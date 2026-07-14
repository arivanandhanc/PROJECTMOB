# *99# Field Notes (PoC Weekend 2) — Carrier Matrix

Fill one block per carrier during on-device testing (blueprint §6 W2, Appendix B).
The cheat-card content in `strings.xml` (`help_carrier_*`, `cheat_step_*`) should be
updated from what you actually observe here — it is data, not code.

Baseline menu (Jul 2026): `1 Send Money → recipient type → details → amount → PIN`.

---

## Carrier: __________  (Jio / Airtel / Vi / BSNL)

- **Circle / SIM:** __________
- **Menu tree as seen:**
  1.
  2.
  3.
- **Deep-code strings tested:**
  - `*99#` → result: __________
  - `*99*1#` → result: __________
  - `*99*1*3#` → result: __________
- **1-bar signal behavior:** __________
- **VoLTE quirk (esp. Jio):** __________
- **Dual-SIM behavior (which slot worked):** __________
- **Paste-into-USSD-dialog supported?** yes / no (OEM: ______)
- **Error messages seen (verbatim) + plain-Tamil translation for help screen:**
  -

---

## Go/No-Go summary

| Carrier | Works at 1 bar? | Deep code works? | Notes |
|---|---|---|---|
| Jio |  |  |  |
| Airtel |  |  |  |
| Vi |  |  |  |
| BSNL |  |  |  |

GO requires ≥3 of 4 carriers working reliably at 1 bar and Jio not fundamentally broken.
