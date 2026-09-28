# Changelog

All notable changes to the **Astilla POS** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.0] - 2026-09-29

### 🚀 Added
- **Customer List & Utang Ledger Module**:
  - Registered customer profiles with names, phone numbers, and Facebook Messenger usernames.
  - Interactive inventory product picker for credit transactions (disallows arbitrary money lending; links strictly to stock items).
  - Itemized borrowing logs and payment tracking ("Bayad Utang") with real-time balance calculations.
- **Automated Dual-Channel Credit Notifications**:
  - Automated native cellular SIM SMS dispatch via Android `SmsManager` displaying item breakdown, transaction date/time, and updated balance.
  - Facebook Messenger integration launching deep-link chats (`m.me`) with pre-filled itemized credit receipts.
- **Accrual Analytics & Financial Reports**:
  - Revenue tracking separating **Gross Sales** (Cash + Credit) from **Actual Cash Flow** (Cash + Payments Received) to eliminate double-counting.
  - Real-time **Outstanding Utang** metric card on the main Analytics dashboard.
- **Expanded Google Apps Script Cloud Sync**:
  - Added support for sync events to automatic `Customer_Credits` and `Customer_Payments` Google Sheet tabs in `Code.gs`.

### 🔄 Changed
- **POS Checkout Modal**: Updated payment selection toggle between Cash and Customer Credit.
- **Inventory Integration**: Automatically deducts product stock levels when items are issued on credit.
- **Borrow Receipt Dialog**: Added explicit formatted date and timestamp (`MMM dd, yyyy - hh:mm a`) to the receipt header.

### 🛠️ Fixed
- Prevented cash-only credit entries by strictly enforcing inventory item selection for all borrow transactions.
- Resolved line ending syntax anomalies in `Code.gs`.

---

## [1.1.0] - 2026-05-15
- Initial offline-first release featuring CameraX scanner, Room Database, and Google Sheets sync webhook for Sales and Inventory.
