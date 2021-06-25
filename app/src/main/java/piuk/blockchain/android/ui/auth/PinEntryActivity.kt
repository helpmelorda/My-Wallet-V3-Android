package piuk.blockchain.android.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.Window
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.ActivityPinEntryBinding
import piuk.blockchain.android.ui.customviews.dialogs.OverlayDetection
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class PinEntryActivity : BaseAuthActivity() {

    private val overlayDetection: OverlayDetection by inject()
    private val loginState: AccessState by inject()

    private val binding: ActivityPinEntryBinding by lazy {
        ActivityPinEntryBinding.inflate(layoutInflater)
    }

    private val pinEntryFragment: PinEntryFragment by lazy {
        PinEntryFragment.newInstance(isAfterCreateWallet)
    }

    private val isAfterCreateWallet: Boolean by unsafeLazy {
        intent.getBooleanExtra(EXTRA_IS_AFTER_WALLET_CREATION, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .add(binding.pinContainer.id, pinEntryFragment)
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        when {
            pinEntryFragment.isValidatingPinForResult -> {
                finishWithResultCanceled()
            }
            pinEntryFragment.allowExit() -> {
                loginState.logout()
            }
        }
    }

    private fun finishWithResultCanceled() {
        val intent = Intent()
        setResult(RESULT_CANCELED, intent)
        finish()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Test for screen overlays before user enters PIN
        return overlayDetection.detectObscuredWindow(this, event) ||
                super.dispatchTouchEvent(event)
    }

    override fun enforceFlagSecure(): Boolean = true

    companion object {

        const val REQUEST_CODE_UPDATE = 188
        private const val EXTRA_IS_AFTER_WALLET_CREATION = "piuk.blockchain.android.EXTRA_IS_AFTER_WALLET_CREATION"

        fun start(context: Context) {
            val intent = Intent(context, PinEntryActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        fun startAfterWalletCreation(context: Context) {
            val intent = Intent(context, PinEntryActivity::class.java)
            intent.putExtra(EXTRA_IS_AFTER_WALLET_CREATION, true)
            context.startActivity(intent)
        }
    }
}
