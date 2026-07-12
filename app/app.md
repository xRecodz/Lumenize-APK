# 📱 Lumenize App Flow (app.md)

## 🧭 Overview

Dokumen ini menjelaskan alur lengkap aplikasi Lumenize dari:

* User membuka aplikasi
* Membuat wallet
* Deposit
* Transfer
* Hingga receiver menerima dana

Menggunakan:

* Stellar Testnet
* Stellar SDK
* Soroban (optional smart contract)

---

# 🏁 1. ENTRY FLOW (OPEN APP)

## 1.1 Splash Screen

* Logo Lumenize
* Loading config (API, Horizon, network)

---

## 1.2 Auth Screen

### Option:

* Login
* Register
* Continue as Guest (optional hackathon)

---

# 👤 2. USER ONBOARDING

## 2.1 Create Wallet

Saat user register:

```js
const pair = StellarSdk.Keypair.random();
```

### Output:

* Public Key → Address user
* Secret Key → Private key

---

## 2.2 Backup Phase (WAJIB 🔥)

User ditampilkan:

* Secret Key / Seed Phrase

### UX:

* “Save this key”
* Checkbox: “I have saved my key”

---

## 2.3 Wallet Storage

### Hackathon mode:

* Simpan di backend (encrypted)

### Production:

* Non-custodial (user simpan sendiri)

---

## 2.4 Fund Account (Testnet)

Call:
https://friendbot.stellar.org/?addr=PUBLIC_KEY

### Result:

* Wallet dapat XLM

---

# 🏠 3. HOME DASHBOARD

## Menampilkan:

* Balance:

    * XLM
    * USDC (custom token)
* Recent transactions
* Button:

    * Deposit
    * Send
    * Receive

---

# 💰 4. DEPOSIT FLOW (FIAT → CRYPTO SIMULATION)

## 4.1 User Input

* Amount (ex: $100)

---

## 4.2 Conversion Rate

Example:

```text
1 USDC = 1 USD
1 XLM = $0.10
```

---

## 4.3 Backend Process

1. Issuer account mint token:

```js
Operation.payment({
  destination: userPublicKey,
  asset: USDC,
  amount: "100"
});
```

---

## 4.4 Result

* User balance:
  +100 USDC

---

# 🔐 5. TRUSTLINE FLOW

Jika user belum trust asset:

```js
Operation.changeTrust({
  asset: USDC
});
```

---

# 💸 6. SEND FLOW (TRANSFER)

## 6.1 Input

* Receiver address / username
* Amount
* Asset

---

## 6.2 Option Mode

### A. Direct Payment

* Kirim USDC → terima USDC

---

### B. Path Payment (AUTO CONVERT 🔥)

* Kirim XLM
* Receiver dapat USDC

---

## 6.3 Build Transaction

```js
const tx = new TransactionBuilder(account, {
  fee: BASE_FEE,
  networkPassphrase: Networks.TESTNET,
})
.addOperation(Operation.payment({
  destination: receiver,
  asset: asset,
  amount: "10"
}))
.setTimeout(30)
.build();
```

---

## 6.4 Sign

```js
tx.sign(senderKeypair);
```

---

## 6.5 Submit

```js
await server.submitTransaction(tx);
```

---

# 📦 7. SAMPLE TRANSACTION (TX)

## Example TX Response:

```json
{
  "hash": "abc123xyz",
  "ledger": 123456,
  "source_account": "GXXXX",
  "successful": true,
  "created_at": "2026-01-01T00:00:00Z",
  "operation_count": 1
}
```

---

# 🔁 8. PATH PAYMENT FLOW (ADVANCED)

## Example:

```js
Operation.pathPaymentStrictSend({
  sendAsset: Asset.native(),
  sendAmount: "10",
  destination: receiver,
  destAsset: USDC,
  destMin: "9"
});
```

---

## Result:

* Sender kirim XLM
* Receiver terima USDC

---

# 📡 9. RECEIVE FLOW (REALTIME)

Backend listen:

```js
server.payments()
  .forAccount(receiverPublicKey)
  .cursor("now")
  .stream({
    onmessage: (payment) => {
      // update DB
    }
  });
```

---

# 💵 10. WITHDRAW FLOW (SIMULATION)

## Flow:

1. User kirim ke issuer
2. Backend mark sebagai withdraw
3. Simulasi fiat transfer

---

# 🧠 11. SOROBAN INTEGRATION (SMART CONTRACT)

## Use Case:

* Escrow
* Fee system
* Payment split

---

## Example Logic:

* User kirim ke contract
* Contract:

    * hold dana
    * release ke receiver

---

## Example Concept:

```rust
fn transfer_with_fee(amount: i128) {
    let fee = amount * 2 / 100;
    let net = amount - fee;

    send(receiver, net);
    send(platform, fee);
}
```

---

# 🔐 12. SECURITY FLOW

* Encrypt private key
* Validate address
* Check trustline before send
* Prevent double send

---

# 📊 13. EXCHANGE RATE SYSTEM

## Source:

* API (CoinGecko / custom mock)

## Example:

```text
1 XLM = $0.10
1 USDC = $1
```

---

# 🎯 14. COMPLETE FLOW SUMMARY

```text
User open app
→ register
→ create wallet
→ backup key
→ fund via friendbot
→ trustline

→ deposit (mint token)
→ send (payment / path payment)
→ blockchain confirm
→ receiver detect via listener
→ update balance

→ withdraw (simulate)
```

---

# 🏆 15. HACKATHON HIGHLIGHT

* Path Payment (auto convert)
* Stellar fast + low fee
* UX seperti e-wallet
* Soroban smart logic

---

# 🔥 TAGLINE

“Send money instantly across currencies using Stellar.”

---
