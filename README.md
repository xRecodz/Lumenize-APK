# 📱 Lumenize - Android App

**Lumenize** is a modern, minimalist fintech application built on the **Stellar Network**. It empowers users to manage digital assets (XLM & USDC) with a seamless, user-friendly experience similar to traditional banking apps but with the power of blockchain.

This repository contains the source code for the Android application. For the landing page and web implementation, visit [Lumenize-LP](https://github.com/xRecodz/Lumenize-LP).

---

## ✨ Key Features

- **Standardized Wallet Management**: Uses **BIP39 Mnemonic Phrases** (12 words) for secure wallet creation and restoration.
- **Fast Transactions**: Send XLM or USDC instantly to any Stellar address or using our **Username Resolver** (e.g., `@sauce`).
- **Path Payments**: Built-in support for auto-converting assets during transfer (e.g., Send XLM, recipient receives USDC).
- **Soroban Smart Contracts**: Implementation of platform fee splitting (2% fee) automated via Soroban logic.
- **Anchor Integration (SEP-24)**: Foundation for interactive deposits and withdrawals via regulated anchors.
- **Real-time Balance**: Pull-to-refresh functionality to fetch live data from the Stellar Horizon API.
- **Security First**: Hidden sensitive data (Secret Keys & Mnemonics) with "Tap to Reveal" and local persistence.

---

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/) (Modern Android Development)
- **UI Framework**: XML with Material Design 3 components.
- **Blockchain SDK**: [Stellar SDK (Java/Kotlin)](https://github.com/stellar/java-stellar-sdk)
- **Network**: Stellar Testnet (Horizon & RPC).
- **Libraries**:
    - `network.lightsail:stellar-sdk`: Core blockchain operations.
    - `androidx.swiperefreshlayout`: Smooth UI refresh interactions.
    - `com.journeyapps:zxing-android-embedded`: QR Code generation for deposits.
    - `okhttp3`: Network requests for Friendbot and external APIs.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana or newer.
- JDK 11 or 17.
- A Stellar Testnet account (can be generated within the app using the Faucet).

### Installation
1. Clone this repository:
   ```bash
   git clone https://github.com/xRecodz/Lumenize-APK.git
   ```
2. Open the project in **Android Studio**.
3. Let Gradle sync and download dependencies.
4. Build and Run on your emulator or physical device.

---

## 📸 Preview

- **Onboarding**: Minimalist artistic branding with font-spacing aesthetics.
- **Dashboard**: High-contrast Gray theme with "Fintech" card styles.
- **Transfer**: Dual-mode selector (Direct Send vs. Red Packet).

---

## 📝 License

Distributed under the MIT License. See `LICENSE` for more information.

---

**Developed for the Stellar Hackathon.**
*"Send crypto like sending a username."*
