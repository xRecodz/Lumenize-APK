package com.example.lumenize

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.stellar.sdk.KeyPair

class MainActivity : AppCompatActivity() {

    private val stellarManager = StellarManager()
    private var currentKeyPair: KeyPair? = null
    private var currentMnemonic: String? = null

    // Mock Username Resolver Mapping
    private val usernameRegistry = mapOf(
        "@sauce" to "GDS6WHXU2I7RDLTCBJRQOIBLCOZ6YJUXF6236Z2X7L7I7N6AOF7LCC2B",
        "@dev" to "GBR3HT4V5A6B7C8D9E0F1G2H3I4J5K6L7M8N9O0P1Q2R3S4T5U6V7W8X",
        "@lumenize" to "GAYO2B... (Mock Address)"
    )

    // UI Layouts (Full Overlays)
    private lateinit var layoutOnboarding: LinearLayout
    private lateinit var layoutImport: LinearLayout
    private lateinit var layoutBackup: LinearLayout
    private lateinit var layoutMain: LinearLayout
    private lateinit var layoutSendForm: LinearLayout
    private lateinit var layoutDepositQR: LinearLayout
    private lateinit var layoutWithdrawForm: LinearLayout

    // Screen Layouts (Inside FrameLayout)
    private lateinit var layoutHome: View
    private lateinit var layoutTransfer: View
    private lateinit var layoutMiniApps: View
    private lateinit var layoutProfile: View

    // Global Views
    private lateinit var tvMainBalance: TextView
    private lateinit var tvMainBalanceUsdc: TextView
    private lateinit var tvAccountAddress: TextView
    private lateinit var bottomNav: BottomNavigationView

    // Swipe Refresh
    private lateinit var swipeHome: SwipeRefreshLayout
    private lateinit var swipeTransfer: SwipeRefreshLayout
    private lateinit var swipeProfile: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupBottomNav()
        setupUsernameResolver()

        // Persistent State Check
        loadWallet()
    }

    private fun loadWallet() {
        val prefs = getSharedPreferences("lumenize_wallet", Context.MODE_PRIVATE)
        val secretKey = prefs.getString("secret_key", null)
        val mnemonic = prefs.getString("mnemonic", null)
        
        if (secretKey != null) {
            try {
                currentKeyPair = KeyPair.fromSecretSeed(secretKey)
                currentMnemonic = mnemonic
                showMainContent()
            } catch (e: Exception) {
                showLayout(layoutOnboarding)
            }
        } else {
            showLayout(layoutOnboarding)
        }
    }

    private fun saveWallet(secretKey: String, mnemonic: String?) {
        val prefs = getSharedPreferences("lumenize_wallet", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("secret_key", secretKey)
            if (mnemonic != null) putString("mnemonic", mnemonic)
        }.apply()
    }

    private fun clearWallet() {
        val prefs = getSharedPreferences("lumenize_wallet", Context.MODE_PRIVATE)
        prefs.edit().remove("secret_key").remove("mnemonic").apply()
        currentKeyPair = null
        currentMnemonic = null
        showLayout(layoutOnboarding)
    }

    private fun initViews() {
        layoutOnboarding = findViewById(R.id.layoutOnboarding)
        layoutImport = findViewById(R.id.layoutImport)
        layoutBackup = findViewById(R.id.layoutBackup)
        layoutMain = findViewById(R.id.layoutMain)
        layoutSendForm = findViewById(R.id.layoutSendForm)
        layoutDepositQR = findViewById(R.id.layoutDepositQR)
        layoutWithdrawForm = findViewById(R.id.layoutWithdrawForm)

        swipeHome = findViewById(R.id.swipeHome)
        swipeTransfer = findViewById(R.id.swipeTransfer)
        swipeProfile = findViewById(R.id.swipeProfile)

        layoutHome = swipeHome
        layoutTransfer = swipeTransfer
        layoutProfile = swipeProfile
        layoutMiniApps = findViewById(R.id.layoutMiniApps)

        tvMainBalance = findViewById(R.id.tvMainBalance)
        tvMainBalanceUsdc = findViewById(R.id.tvMainBalanceUsdc)
        tvAccountAddress = findViewById(R.id.tvAccountAddress)
        bottomNav = findViewById(R.id.bottomNavigation)
    }

    private fun setupListeners() {
        // 1. Onboarding
        findViewById<Button>(R.id.btnStartCreateWallet).setOnClickListener {
            val mnemonic = stellarManager.generateMnemonic()
            currentMnemonic = mnemonic
            currentKeyPair = stellarManager.getKeyPairFromMnemonic(mnemonic)
            
            findViewById<TextView>(R.id.tvSecretKeyDisplay).text = "Tap to reveal"
            findViewById<View>(R.id.btnCopySecretKey).visibility = View.GONE
            showLayout(layoutBackup)
        }

        findViewById<Button>(R.id.btnStartImportWallet).setOnClickListener {
            showLayout(layoutImport)
        }

        // 2. Import
        findViewById<Button>(R.id.btnSubmitImport).setOnClickListener {
            val secret = findViewById<EditText>(R.id.etImportSecretKey).text.toString().trim()
            if (secret.startsWith("S") && secret.length == 56) {
                try {
                    currentKeyPair = KeyPair.fromSecretSeed(secret)
                    currentMnemonic = null // Imported from SK directly
                    saveWallet(secret, null)
                    showMainContent()
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid Secret Key", Toast.LENGTH_SHORT).show()
                }
            } else if (secret.split(" ").size >= 12) {
                // Mnemonic Import
                try {
                    val keyPair = stellarManager.getKeyPairFromMnemonic(secret)
                    currentKeyPair = keyPair
                    currentMnemonic = secret
                    saveWallet(String(keyPair.secretSeed), secret)
                    showMainContent()
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid Mnemonic", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a valid Secret Key or 12-word Mnemonic", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnCancelImport).setOnClickListener {
            showLayout(layoutOnboarding)
        }

        // 3. Backup
        val tvSecretDisplay = findViewById<TextView>(R.id.tvSecretKeyDisplay)
        val btnCopySecret = findViewById<Button>(R.id.btnCopySecretKey)
        
        tvSecretDisplay.setOnClickListener {
            currentMnemonic?.let {
                if (tvSecretDisplay.text.toString().contains("reveal")) {
                    tvSecretDisplay.text = it
                    btnCopySecret.visibility = View.VISIBLE
                } else {
                    tvSecretDisplay.text = "Tap to reveal"
                    btnCopySecret.visibility = View.GONE
                }
            }
        }

        btnCopySecret.setOnClickListener {
            currentMnemonic?.let { copyToClipboard("Mnemonic Phrase", it) }
        }

        val cbConfirm = findViewById<CheckBox>(R.id.cbConfirmBackup)
        val btnFinishBackup = findViewById<Button>(R.id.btnFinishBackup)
        cbConfirm.setOnCheckedChangeListener { _, isChecked ->
            btnFinishBackup.isEnabled = isChecked
        }

        btnFinishBackup.setOnClickListener {
            currentKeyPair?.let {
                saveWallet(String(it.secretSeed), currentMnemonic)
                showMainContent()
            }
        }

        // 4. Dashboard Quick Actions
        findViewById<View>(R.id.btnQuickDeposit).setOnClickListener {
            showDepositOptions()
        }

        findViewById<View>(R.id.btnQuickSend).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_transfer
        }

        findViewById<View>(R.id.btnQuickWithdraw).setOnClickListener {
            showWithdrawalOptions()
        }

        findViewById<Button>(R.id.btnDashboardTrustUsdc).setOnClickListener {
            enableUsdc()
        }

        // 5. Transfer Screen Modes
        findViewById<View>(R.id.btnModeSendDirect).setOnClickListener {
            showLayout(layoutSendForm)
        }
        findViewById<View>(R.id.btnModeSendLink).setOnClickListener {
            val input = EditText(this)
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.hint = "0.00"
            
            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(64, 24, 64, 24)
            input.layoutParams = lp
            container.addView(input)

            MaterialAlertDialogBuilder(this)
                .setTitle("Create Red Packet Link")
                .setMessage("Enter the amount of XLM you want to share.")
                .setView(container)
                .setPositiveButton("Create Link") { _, _ ->
                    val amount = input.text.toString()
                    if (amount.isNotEmpty()) {
                        lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                currentKeyPair?.let { stellarManager.createRedPacket(it, amount) } ?: "Error"
                            }
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle("Red Packet Created")
                                .setMessage(result + "\n\nShare this ID with your friends to let them claim the balance.")
                                .setPositiveButton("Share Link") { _, _ ->
                                    copyToClipboard("Red Packet Link", "https://lumenize.pay/claim?id=mock-id-123")
                                }
                                .show()
                        }
                    } else {
                        Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarSendForm).setNavigationOnClickListener {
            showLayout(layoutMain)
            switchScreen(layoutTransfer)
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarDepositQR).setNavigationOnClickListener {
            showLayout(layoutMain)
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarWithdrawForm).setNavigationOnClickListener {
            showLayout(layoutMain)
        }

        findViewById<Button>(R.id.btnSubmitSend).setOnClickListener {
            handleSendPayment()
        }

        findViewById<Button>(R.id.btnSubmitWithdraw).setOnClickListener {
            Toast.makeText(this, "Simulating Withdrawal...", Toast.LENGTH_SHORT).show()
            // Here you'd normally build a tx with a memo
            showLayout(layoutMain)
        }

        findViewById<View>(R.id.btnCopyDepositAddress).setOnClickListener {
            currentKeyPair?.accountId?.let { copyToClipboard("Wallet Address", it) }
        }

        findViewById<TextView>(R.id.tvDepositAddress).setOnClickListener {
            currentKeyPair?.accountId?.let { copyToClipboard("Wallet Address", it) }
        }

        // 6. MiniApps
        findViewById<Button>(R.id.btnContactUs).setOnClickListener {
            Toast.makeText(this, "Redirecting to Support...", Toast.LENGTH_SHORT).show()
        }

        // 7. Profile Screen Actions
        findViewById<Button>(R.id.btnCopyAddress).setOnClickListener {
            currentKeyPair?.accountId?.let { copyToClipboard("Wallet Address", it) }
        }

        val tvProfileMnemonic = findViewById<TextView>(R.id.tvProfileMnemonic)
        findViewById<View>(R.id.btnRevealMnemonic).setOnClickListener {
            val current = tvProfileMnemonic.text.toString()
            if (current.contains("•")) {
                tvProfileMnemonic.text = currentMnemonic ?: "No Mnemonic (Imported via SK)"
            } else {
                tvProfileMnemonic.text = "•••• •••• •••• ••••"
            }
        }
        findViewById<View>(R.id.btnCopyMnemonic).setOnClickListener {
            currentMnemonic?.let { copyToClipboard("Mnemonic Phrase", it) }
        }

        val tvProfilePrivateKey = findViewById<TextView>(R.id.tvProfilePrivateKey)
        findViewById<View>(R.id.btnRevealPrivateKey).setOnClickListener {
            val current = tvProfilePrivateKey.text.toString()
            if (current.contains("•")) {
                currentKeyPair?.let { tvProfilePrivateKey.text = String(it.secretSeed) }
            } else {
                tvProfilePrivateKey.text = "••••••••••••••••••••••••••••"
            }
        }
        findViewById<View>(R.id.btnCopyPrivateKey).setOnClickListener {
            currentKeyPair?.let { copyToClipboard("Private Key", String(it.secretSeed)) }
        }

        findViewById<Button>(R.id.btnDisconnect).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Disconnect Wallet")
                .setMessage("Are you sure you want to disconnect? Make sure you have backed up your mnemonic phrase or secret key.")
                .setPositiveButton("Disconnect") { _, _ -> clearWallet() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 8. Swipe Refresh Handlers
        swipeHome.setOnRefreshListener { 
            currentKeyPair?.accountId?.let { refreshBalance(it) { swipeHome.isRefreshing = false } } ?: run { swipeHome.isRefreshing = false }
        }
        swipeTransfer.setOnRefreshListener { 
            swipeTransfer.isRefreshing = false 
        }
        swipeProfile.setOnRefreshListener { 
            currentKeyPair?.accountId?.let { refreshBalance(it) { swipeProfile.isRefreshing = false } } ?: run { swipeProfile.isRefreshing = false }
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchScreen(layoutHome)
                R.id.nav_transfer -> switchScreen(layoutTransfer)
                R.id.nav_miniapps -> switchScreen(layoutMiniApps)
                R.id.nav_profile -> switchScreen(layoutProfile)
            }
            true
        }
    }

    private fun showMainContent() {
        showLayout(layoutMain)
        switchScreen(layoutHome)
        bottomNav.selectedItemId = R.id.nav_home
        currentKeyPair?.accountId?.let {
            tvAccountAddress.text = it
            refreshBalance(it)
        }
    }

    private fun showLayout(layout: View) {
        layoutOnboarding.visibility = View.GONE
        layoutImport.visibility = View.GONE
        layoutBackup.visibility = View.GONE
        layoutMain.visibility = View.GONE
        layoutSendForm.visibility = View.GONE
        layoutDepositQR.visibility = View.GONE
        layoutWithdrawForm.visibility = View.GONE
        
        layout.alpha = 0f
        layout.visibility = View.VISIBLE
        layout.animate().alpha(1f).setDuration(400).start()
    }

    private fun showDepositQR() {
        val address = currentKeyPair?.accountId ?: return
        findViewById<TextView>(R.id.tvDepositAddress).text = address
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(address, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
            findViewById<android.widget.ImageView>(R.id.ivDepositQR).setImageBitmap(bitmap)
        } catch (e: Exception) { e.printStackTrace() }
        showLayout(layoutDepositQR)
    }

    private fun switchScreen(screen: View) {
        layoutHome.visibility = View.GONE
        layoutTransfer.visibility = View.GONE
        layoutMiniApps.visibility = View.GONE
        layoutProfile.visibility = View.GONE
        
        screen.alpha = 0f
        screen.visibility = View.VISIBLE
        screen.animate().alpha(1f).setDuration(300).start()
        
        // Refresh balance when going home
        if (screen == layoutHome) {
            currentKeyPair?.accountId?.let { refreshBalance(it) }
        }
    }

    private fun setupUsernameResolver() {
        val etDest = findViewById<EditText>(R.id.etSendDestination)
        val tvResolved = findViewById<TextView>(R.id.tvResolvedAddress)
        
        etDest.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString().trim().lowercase()
                if (input.startsWith("@")) {
                    val address = usernameRegistry[input]
                    if (address != null) {
                        tvResolved.text = "Resolved to: $address"
                        tvResolved.visibility = View.VISIBLE
                    } else {
                        tvResolved.text = "Username not found"
                        tvResolved.visibility = View.VISIBLE
                    }
                } else {
                    tvResolved.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showDepositOptions() {
        val publicKey = currentKeyPair?.accountId ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_action_picker, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        dialogView.findViewById<TextView>(R.id.tvPickerTitle).text = "Deposit Funds"
        dialogView.findViewById<TextView>(R.id.tvTitle1).text = "Faucet"
        dialogView.findViewById<TextView>(R.id.tvDesc1).text = "Get free testnet XLM instantly"
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon1).setImageResource(android.R.drawable.ic_menu_add)
        dialogView.findViewById<View>(R.id.btnOption1).setOnClickListener {
            dialog.dismiss()
            fundXlm(publicKey)
        }

        dialogView.findViewById<TextView>(R.id.tvTitle2).text = "Deposit Wallet / Exchange"
        dialogView.findViewById<TextView>(R.id.tvDesc2).text = "Show QR and Wallet Address to receive"
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon2).setImageResource(android.R.drawable.ic_dialog_info)
        dialogView.findViewById<View>(R.id.btnOption2).setOnClickListener {
            dialog.dismiss()
            showDepositQR()
        }

        dialogView.findViewById<TextView>(R.id.tvTitle3).text = "Virtual Bank Account"
        dialogView.findViewById<TextView>(R.id.tvDesc3).text = "Simulated Local Bank Transfer"
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon3).setImageResource(android.R.drawable.ic_dialog_map)
        dialogView.findViewById<View>(R.id.btnOption3).setOnClickListener {
            dialog.dismiss()
            mintUsdc(publicKey)
        }
        dialog.show()
    }

    private fun showWithdrawalOptions() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_action_picker, null)
        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        dialogView.findViewById<TextView>(R.id.tvPickerTitle).text = "Withdraw Funds"
        
        // Option 1: Withdrawal Exchange
        dialogView.findViewById<TextView>(R.id.tvTitle1).text = "Withdrawal Exchange"
        dialogView.findViewById<TextView>(R.id.tvDesc1).text = "Send your funds to external address"
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon1).setImageResource(android.R.drawable.ic_menu_send)
        dialogView.findViewById<View>(R.id.btnOption1).setOnClickListener {
            dialog.dismiss()
            showLayout(layoutWithdrawForm)
        }

        // Option 2: P2P Withdrawal
        dialogView.findViewById<TextView>(R.id.tvTitle2).text = "P2P Withdrawal"
        dialogView.findViewById<TextView>(R.id.tvDesc2).text = "Cash out via trusted P2P merchants"
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon2).setImageResource(android.R.drawable.ic_menu_share)
        dialogView.findViewById<View>(R.id.btnOption2).setOnClickListener {
            dialog.dismiss()
            showLayout(layoutWithdrawForm)
        }

        // Option 3: Withdrawal Local IDR
        dialogView.findViewById<TextView>(R.id.tvTitle3).text = "Withdrawal Local IDR"
        dialogView.findViewById<TextView>(R.id.tvDesc3).text = "Withdraw directly to your bank account"
        dialogView.findViewById<android.widget.ImageView>(R.id.ivIcon3).setImageResource(android.R.drawable.ic_dialog_alert)
        dialogView.findViewById<View>(R.id.btnOption3).setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun fundXlm(publicKey: String) {
        Toast.makeText(this, "Funding XLM...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) { stellarManager.fundAccount(publicKey) }
            if (success) {
                Toast.makeText(this@MainActivity, "XLM Funded", Toast.LENGTH_SHORT).show()
                refreshBalance(publicKey)
            }
        }
    }

    private fun mintUsdc(publicKey: String) {
        Toast.makeText(this, "Validating USDC Trustline...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { stellarManager.simulateUsdcDeposit(publicKey, "100") }
            if (result.contains("Success")) {
                Toast.makeText(this@MainActivity, "100 USDC Deposited (Simulated)", Toast.LENGTH_SHORT).show()
                refreshBalance(publicKey)
            } else {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Deposit Failed")
                    .setMessage(result)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun enableUsdc() {
        val keyPair = currentKeyPair ?: return
        val usdcAsset = stellarManager.getUsdcAsset() ?: return
        Toast.makeText(this, "Enabling USDC... (Requires XLM)", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) { stellarManager.createTrustline(keyPair, usdcAsset) }
            if (success) {
                Toast.makeText(this@MainActivity, "USDC Enabled Successfully", Toast.LENGTH_SHORT).show()
                refreshBalance(keyPair.accountId)
            } else {
                Toast.makeText(this@MainActivity, "Failed. Make sure you have XLM first.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSendPayment() {
        val source = currentKeyPair ?: return
        var destination = findViewById<EditText>(R.id.etSendDestination).text.toString().trim()
        val amount = findViewById<EditText>(R.id.etSendAmount).text.toString()
        val isPath = findViewById<CheckBox>(R.id.cbSendPathPayment).isChecked

        if (destination.startsWith("@")) {
            val resolved = usernameRegistry[destination.lowercase()]
            if (resolved != null) {
                destination = resolved
            } else {
                Toast.makeText(this, "Recipient username not found", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (destination.isNotEmpty() && amount.isNotEmpty()) {
            Toast.makeText(this, "Initiating Soroban Split Payment...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    val usdcAsset = stellarManager.getUsdcAsset()
                    if (isPath && usdcAsset != null) {
                        // For Path payment, we stick to standard SDK
                        val success = stellarManager.sendPathPayment(source, destination, org.stellar.sdk.AssetTypeNative(), "50", usdcAsset, amount)
                        if (success) "Success: Path Payment Sent" else "Failed"
                    } else {
                        // Use Soroban Split for Direct Payment
                        stellarManager.invokePaymentSplit(source, destination, amount)
                    }
                }
                
                if (result.contains("Success")) {
                    Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
                    showLayout(layoutMain)
                    switchScreen(layoutHome)
                    bottomNav.selectedItemId = R.id.nav_home
                } else {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Transaction Failed")
                        .setMessage(result + "\n\nMake sure recipient is active and you have enough XLM.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun refreshBalance(publicKey: String, onComplete: (() -> Unit)? = null) {
        lifecycleScope.launch {
            val account = withContext(Dispatchers.IO) { stellarManager.getAccount(publicKey) }
            if (account != null) {
                val xlmRaw = account.balances.find { it.assetType == "native" }?.balance ?: "0.00"
                val usdcRaw = account.balances.find { it.assetCode == "USDC" }?.balance ?: "0.00"
                
                val formatter = java.text.DecimalFormat("#,##0.00")
                val xlmFormatted = formatter.format(xlmRaw.toDouble())
                val usdcFormatted = formatter.format(usdcRaw.toDouble())
                
                tvMainBalance.text = "$xlmFormatted XLM"
                tvMainBalanceUsdc.text = "$usdcFormatted USDC"
            }
            onComplete?.invoke()
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
