# 🚀 StellarPay Advanced Implementation

---

# 🧠 1. Soroban Smart Contract (Rust)

## 🎯 Use Case

* Fee splitting
* Escrow
* Payment routing

---

## 📦 Contract: `payment_split.rs`

```rust
#![no_std]
use soroban_sdk::{contractimpl, Env, Address, Symbol};

pub struct PaymentSplit;

#[contractimpl]
impl PaymentSplit {
    pub fn send_with_fee(
        env: Env,
        from: Address,
        to: Address,
        platform: Address,
        amount: i128,
    ) {
        // 2% fee
        let fee = amount * 2 / 100;
        let net = amount - fee;

        // transfer to receiver
        env.invoke_contract::<()>(
            &env.current_contract_address(),
            &Symbol::short("transfer"),
            (from.clone(), to, net),
        );

        // transfer fee to platform
        env.invoke_contract::<()>(
            &env.current_contract_address(),
            &Symbol::short("transfer"),
            (from, platform, fee),
        );
    }
}
```

---

## ⚙️ Build & Deploy

### 1. Install

```bash
cargo install soroban-cli
```

---

### 2. Build

```bash
cargo build --target wasm32-unknown-unknown --release
```

---

### 3. Deploy (Testnet)

```bash
soroban contract deploy \
  --wasm target/wasm32-unknown-unknown/release/payment_split.wasm \
  --source-account YOUR_KEY \
  --network testnet
```

---

### 4. Invoke

```bash
soroban contract invoke \
  --id CONTRACT_ID \
  --fn send_with_fee \
  --arg from=... \
  --arg to=... \
  --arg platform=... \
  --arg amount=1000
```

---

# ⚙️ 2. Backend API (Express + Stellar SDK)

## 📦 Setup

```bash
npm init -y
npm install express stellar-sdk cors dotenv
```

---

## 📁 `server.js`

```js
require("dotenv").config();
const express = require("express");
const StellarSdk = require("stellar-sdk");

const app = express();
app.use(express.json());

const server = new StellarSdk.Server("https://horizon-testnet.stellar.org");
const network = StellarSdk.Networks.TESTNET;

// ======================
// 🔐 WALLET CREATE
// ======================
app.post("/wallet/create", (req, res) => {
  const pair = StellarSdk.Keypair.random();

  res.json({
    publicKey: pair.publicKey(),
    secret: pair.secret()
  });
});

// ======================
// 💰 FUND (TESTNET)
// ======================
app.post("/wallet/fund", async (req, res) => {
  const { publicKey } = req.body;

  const response = await fetch(`https://friendbot.stellar.org/?addr=${publicKey}`);
  const data = await response.json();

  res.json(data);
});

// ======================
// 🔗 TRUSTLINE
// ======================
app.post("/trustline", async (req, res) => {
  const { secret, issuer, assetCode } = req.body;

  const keypair = StellarSdk.Keypair.fromSecret(secret);
  const account = await server.loadAccount(keypair.publicKey());

  const asset = new StellarSdk.Asset(assetCode, issuer);

  const tx = new StellarSdk.TransactionBuilder(account, {
    fee: StellarSdk.BASE_FEE,
    networkPassphrase: network
  })
    .addOperation(StellarSdk.Operation.changeTrust({ asset }))
    .setTimeout(30)
    .build();

  tx.sign(keypair);
  const result = await server.submitTransaction(tx);

  res.json(result);
});

// ======================
// 💸 SEND PAYMENT
// ======================
app.post("/send", async (req, res) => {
  const { secret, to, amount } = req.body;

  const keypair = StellarSdk.Keypair.fromSecret(secret);
  const account = await server.loadAccount(keypair.publicKey());

  const tx = new StellarSdk.TransactionBuilder(account, {
    fee: StellarSdk.BASE_FEE,
    networkPassphrase: network
  })
    .addOperation(
      StellarSdk.Operation.payment({
        destination: to,
        asset: StellarSdk.Asset.native(),
        amount
      })
    )
    .setTimeout(30)
    .build();

  tx.sign(keypair);
  const result = await server.submitTransaction(tx);

  res.json(result);
});

// ======================
// 🔁 PATH PAYMENT
// ======================
app.post("/send-path", async (req, res) => {
  const { secret, to, sendAmount, destAssetCode, issuer } = req.body;

  const keypair = StellarSdk.Keypair.fromSecret(secret);
  const account = await server.loadAccount(keypair.publicKey());

  const destAsset = new StellarSdk.Asset(destAssetCode, issuer);

  const tx = new StellarSdk.TransactionBuilder(account, {
    fee: StellarSdk.BASE_FEE,
    networkPassphrase: network
  })
    .addOperation(
      StellarSdk.Operation.pathPaymentStrictSend({
        sendAsset: StellarSdk.Asset.native(),
        sendAmount,
        destination: to,
        destAsset,
        destMin: "1"
      })
    )
    .setTimeout(30)
    .build();

  tx.sign(keypair);
  const result = await server.submitTransaction(tx);

  res.json(result);
});

// ======================
// 📡 LISTEN TRANSACTION
// ======================
app.get("/listen/:publicKey", (req, res) => {
  const { publicKey } = req.params;

  server.payments()
    .forAccount(publicKey)
    .cursor("now")
    .stream({
      onmessage: (payment) => {
        console.log("Incoming:", payment);
      }
    });

  res.send("Listening...");
});

app.listen(3000, () => {
  console.log("🚀 Server running on port 3000");
});
```

---

# 🧨 3. USERNAME → ADDRESS RESOLVER (KILLER FEATURE)

## 🎯 Konsep

User tidak perlu kirim ke:

```
GCFX...XYZ
```

Cukup:

```
@budi
```

---

## 📦 Database Schema

```sql
users:
- id
- username (unique)
- public_key
```

---

## 🔗 API

### Register Username

```js
app.post("/username/register", async (req, res) => {
  const { username, publicKey } = req.body;

  // save ke DB
});
```

---

### Resolve Username

```js
app.get("/resolve/:username", async (req, res) => {
  const { username } = req.params;

  // query DB
  const user = await db.find(username);

  res.json({
    address: user.public_key
  });
});
```

---

## 🔥 Advanced (Soroban Version)

Bisa kamu simpan mapping di contract:

```rust
pub fn register(env: Env, username: Symbol, address: Address) {
    env.storage().instance().set(&username, &address);
}

pub fn resolve(env: Env, username: Symbol) -> Address {
    env.storage().instance().get(&username).unwrap()
}
```

---

# 🏆 FINAL FLOW (SUPER POWERFUL)

```text
User A → @budi
↓
Backend resolve → GXXXX
↓
Call Soroban contract (fee split)
↓
Send via Stellar
↓
Receiver dapat dana
↓
Listener detect → update UI
```

---

# 🔥 BONUS IDE (BIAR MENANG)

* QR send
* Username + domain (budi.pay)
* Multi currency send (XLM → USDC)
* Fee auto split via Soroban

---

# 🎯 TAGLINE

**“Send crypto like sending a username.”**

---
