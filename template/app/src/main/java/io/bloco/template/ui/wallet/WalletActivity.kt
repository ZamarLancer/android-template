package io.bloco.template.ui.wallet

import android.os.Bundle
import android.view.View
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

    init {
        System.loadLibrary("TrustWalletCore")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val masterKey = MasterKey.Builder(applicationContext).build()

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


    private fun showErrorSnackBar() {
        Snackbar.make(binding.coordinatorLayout, R.string.error_message, Snackbar.LENGTH_SHORT)
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