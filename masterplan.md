# 🚀 StellarPay – Masterplan / PRD (Hackathon APAC Stellar)

## 1. 📌 Overview

**StellarPay** adalah aplikasi pembayaran berbasis blockchain menggunakan jaringan Stellar yang memungkinkan user untuk:

* Deposit (simulasi fiat → crypto)
* Transfer antar user (stablecoin / token)
* Auto convert asset saat transfer (Path Payment)
* Withdraw (simulasi crypto → fiat)

Target utama:
💡 *“Kirim uang seperti e-wallet, tapi pakai blockchain (Stellar)”*

---

## 2. 🎯 Objectives

### Primary Goals

* Membuat sistem transfer berbasis Stellar Testnet
* Implementasi:

    * Wallet creation
    * Trustline
    * Token transfer
    * Path payment
* UX sederhana seperti fintech (GoPay / OVO style)

### Success Metrics

* User bisa kirim & terima token
* Transaksi sukses di testnet
* Path payment berjalan (auto convert)
* Demo end-to-end tanpa error

---

## 3. 👤 User Personas

### 1. Sender (User A)

* Kirim uang ke user lain
* Bisa kirim dalam bentuk token tertentu

### 2. Receiver (User B)

* Terima dana
* Bisa terima dalam asset berbeda (auto convert)

---

## 4. 🔁 Core Features

### 4.1 Wallet System

* Generate Stellar Keypair
* Store wallet (custodial untuk hackathon)

---

### 4.2 Account Funding (Testnet)

* Fund via Friendbot (XLM)
* Minimal balance untuk aktif

---

### 4.3 Asset System

* Custom token (contoh: USDC simulasi)
* Issuer account

---

### 4.4 Trustline

* User harus approve asset sebelum menerima

---

### 4.5 Transfer System

* Direct Payment:

    * Kirim asset yang sama
* Path Payment:

    * Kirim asset A → terima asset B

---

### 4.6 Deposit Simulation

* Backend mint token ke user
* Simulasi fiat → crypto

---

### 4.7 Withdraw Simulation

* User kirim token ke issuer
* Simulasi crypto → fiat

---

### 4.8 Realtime Transaction Listener

* Listen incoming payment via Horizon stream

---

## 5. 🧠 System Architecture

### High-Level Architecture

Frontend (React / Mobile)
↓
Backend API (Node.js / Express)
↓
Stellar SDK
↓
Horizon API (Testnet)
↓
Stellar Network

---

### Core Services

#### 1. Wallet Service

* Generate keypair
* Store public/secret (encrypted)

#### 2. Transaction Service

* Create payment
* Create trustline
* Create path payment

#### 3. Asset Service

* Manage issuer account
* Mint / burn token

#### 4. Listener Service

* Listen transaksi masuk
* Update database

---

## 6. 🔄 User Flow

### Flow 1 — Register & Setup

1. User register
2. Generate wallet
3. Fund via Friendbot
4. Setup trustline

---

### Flow 2 — Deposit

1. User klik deposit
2. Backend mint token ke wallet user

---

### Flow 3 — Transfer

1. Input address / username
2. Pilih asset
3. Kirim:

    * Direct payment atau
    * Path payment

---

### Flow 4 — Receive

1. Listener detect incoming tx
2. Update balance

---

### Flow 5 — Withdraw

1. Kirim ke issuer
2. Simulasi redeem fiat

---

## 7. ⚙️ Technical Stack

### Frontend

* React / Next.js
* Tailwind / UI Kit

### Backend

* Node.js + Express

### Blockchain

* Stellar Testnet
* js-stellar-sdk

### API

* Horizon API

---

## 8. 📦 Data Model (Simplified)

### User

* id
* email
* wallet_public
* wallet_secret (encrypted)

### Transaction

* id
* from
* to
* asset
* amount
* type (payment / path_payment)
* status

---

## 9. 🔐 Security (Hackathon Scope)

* Secret key encryption (basic)
* Server-side signing (custodial)
* No production-grade KYC

---

## 10. 🚧 Limitations (Hackathon)

* Tidak ada real fiat integration
* Anchor hanya simulasi
* Custodial wallet (bukan non-custodial)

---

## 11. 🧪 Testing Plan

* Test wallet creation
* Test trustline
* Test transfer
* Test path payment
* Test listener

---

## 12. 🚀 Future Scope (Post-Hackathon)

* Integrasi Anchor real (MoneyGram-like)
* Non-custodial wallet
* Mobile app
* Multi-chain support

---

## 13. 🏆 Differentiation (For Judges)

* Menggunakan Path Payment (auto convert)
* UX seperti e-wallet (bukan crypto app ribet)
* Real use case: remittance

---

## 14. 📚 References

* Stellar Developer Docs
* Horizon API
* js-stellar-sdk

---

## 15. 🎯 Demo Scenario

1. User A login
2. Deposit token
3. Kirim ke User B
4. User B terima asset berbeda (path payment)
5. Withdraw simulasi

---

## 🔥 Tagline

**“Send money across currencies instantly using Stellar.”**

---
