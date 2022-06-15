package io.bloco.template.ui.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.bloco.template.R
import io.bloco.template.databinding.ActivityWalletBinding
import io.bloco.template.ui.BaseActivity
import wallet.core.jni.*

@AndroidEntryPoint
class WalletActivity : BaseActivity() {

    private lateinit var binding: ActivityWalletBinding
    private var showRecovery = false

    private lateinit var sharedPreferences: EncryptedSharedPreferences
    private lateinit var clipboard: ClipboardManager

    init {
        System.loadLibrary("TrustWalletCore")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
        val masterKey = MasterKey(applicationContext, masterKeyAlias)

        sharedPreferences = EncryptedSharedPreferences.create(
            applicationContext,
            "wallet_pref",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences

        binding = ActivityWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGenerate.setOnClickListener{ onClickGenerate()}
        binding.btnRecovery.setOnClickListener{ onClickRecovery()}

        binding.txtPublicKey.setOnClickListener { onClickText(binding.txtPublicKey, "Public Key") }
        binding.txtPrivateKey.setOnClickListener { onClickText(binding.txtPrivateKey, "Private Key") }
        binding.txtRecoveryPhrase.setOnClickListener { onClickText(binding.txtRecoveryPhrase, "Recovery Phrase") }

        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        load()
    }

    override fun onDestroy() {
        save()
        super.onDestroy()
    }

    private fun load() {
        binding.txtPublicKey.text = sharedPreferences.getString(PUBLIC_KEY, "").toString()
        binding.txtPrivateKey.text = sharedPreferences.getString(PRIVATE_KEY, "").toString()
        binding.txtRecoveryPhrase.text = sharedPreferences.getString(RECOVERY_PHRASE, "").toString()

        updateUI()
    }

    private fun save() {
        sharedPreferences.edit()
            .putString(PUBLIC_KEY, binding.txtPublicKey.text.toString())
            .putString(PRIVATE_KEY, binding.txtPrivateKey.text.toString())
            .putString(RECOVERY_PHRASE, binding.txtRecoveryPhrase.text.toString())
            .apply()
    }


    private fun showSnackBar(message: String) {
        Snackbar.make(binding.coordinatorLayout, message, Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun onClickGenerate() {
        val wallet = HDWallet(128, "")
        println(wallet.getExtendedPublicKey(Purpose.BIP44, CoinType.ETHEREUM, HDVersion.XPUB))
        binding.txtPublicKey.text = wallet.getExtendedPublicKey(Purpose.BIP44, CoinType.ETHEREUM, HDVersion.XPUB)
        binding.txtPrivateKey.text = wallet.getExtendedPrivateKey(Purpose.BIP44, CoinType.ETHEREUM, HDVersion.XPRV)
        binding.txtRecoveryPhrase.text = wallet.mnemonic()
        updateUI()
        save()
    }

    private fun onClickRecovery() {
        showRecovery = !showRecovery
        updateUI()
    }

    private fun onClickText(view: TextView, name: String) {
        println(view.text.toString())
        val clip = ClipData.newPlainText("clipboard", view.text.toString())
//        clipboard.text = view.text.toString()
        clipboard.setPrimaryClip(clip)
        println("Pasted: " + clipboard.text)
//        showSnackBar("$name was copied to clipboard.")
    }

    private fun updateUI() {
        if (binding.txtPublicKey.text.isEmpty()) {
            binding.layoutPublicKey.visibility = View.GONE
            binding.btnRecovery.visibility = View.GONE
        } else {
            binding.layoutPublicKey.visibility = View.VISIBLE
            binding.btnRecovery.visibility = View.VISIBLE
        }

        if (showRecovery) {
            binding.layoutRecover.visibility = View.VISIBLE
        } else {
            binding.layoutRecover.visibility = View.GONE
        }
    }

    companion object {
        const val PUBLIC_KEY: String = "PUBLIC_KEY"
        const val PRIVATE_KEY: String = "PRIVATE_KEY"
        const val RECOVERY_PHRASE: String = "RECOVERY_PHRASE"
    }
}