package piuk.blockchain.android.simplebuy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.BankLinkingPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.FragmentActivityBinding
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.base.addAnimationTransaction
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthFlowState
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.fromPreferencesValue
import piuk.blockchain.android.ui.linkbank.toPreferencesValue
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyFirstTimeBuyerFragment
import piuk.blockchain.android.ui.recurringbuy.RecurringBuySuccessfulFragment
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class SimpleBuyActivity : BlockchainActivity(), SimpleBuyNavigator {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val enableLogoutTimer: Boolean = false
    private val compositeDisposable = CompositeDisposable()
    private val simpleBuyFlowNavigator: SimpleBuyFlowNavigator by scopedInject()
    private val bankLinkingPrefs: BankLinkingPrefs by scopedInject()
    private val assetCatalogue: AssetCatalogue by inject()

    private val startedFromDashboard: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_NAVIGATION_KEY, false)
    }

    private val startedFromApprovalDeepLink: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_APPROVAL_KEY, false)
    }

    private val preselectedPaymentMethodId: String? by unsafeLazy {
        intent.getStringExtra(PRESELECTED_PAYMENT_METHOD)
    }

    private val startedFromKycResume: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_KYC_RESUME, false)
    }

    private val asset: AssetInfo? by unsafeLazy {
        intent.getStringExtra(ASSET_KEY)?.let {
            assetCatalogue.fromNetworkTicker(it)
        }
    }

    private val binding: FragmentActivityBinding by lazy {
        FragmentActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarGeneral.toolbarGeneral)
        setupToolbar("")
        if (savedInstanceState == null) {
            if (startedFromApprovalDeepLink) {
                val currentState = bankLinkingPrefs.getBankLinkingState().fromPreferencesValue()
                bankLinkingPrefs.setBankLinkingState(
                    currentState.copy(bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_COMPLETE).toPreferencesValue()
                )
            }
            analytics.logEvent(BuySellViewedEvent(BuySellType.BUY))
            subscribeForNavigation()
        }
    }

    override fun onSheetClosed() = subscribeForNavigation()

    private fun subscribeForNavigation() {
        compositeDisposable += simpleBuyFlowNavigator.navigateTo(
            startedFromKycResume,
            startedFromDashboard,
            startedFromApprovalDeepLink,
            asset
        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                when (it) {
                    is BuyNavigation.CurrencySelection -> launchCurrencySelector(it.currencies)
                    is BuyNavigation.FlowScreenWithCurrency -> startFlow(it)
                    BuyNavigation.PendingOrderScreen -> goToPendingOrderScreen()
                    BuyNavigation.OrderInProgressScreen -> goToPaymentScreen(false, startedFromApprovalDeepLink)
                }
            }
    }

    private fun launchCurrencySelector(currencies: List<String>) {
        compositeDisposable.clear()
        showBottomSheet(SimpleBuySelectCurrencyFragment.newInstance(currencies))
    }

    private fun startFlow(screenWithCurrency: BuyNavigation.FlowScreenWithCurrency) {
        when (screenWithCurrency.flowScreen) {
            FlowScreen.ENTER_AMOUNT -> goToBuyCryptoScreen(
                false, screenWithCurrency.cryptoCurrency, preselectedPaymentMethodId
            )
            FlowScreen.KYC -> startKyc()
            FlowScreen.KYC_VERIFICATION -> goToKycVerificationScreen(false)
            FlowScreen.CHECKOUT -> goToCheckOutScreen(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun exitSimpleBuyFlow() {
        if (!startedFromDashboard) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } else {
            finish()
        }
    }

    override fun goToBuyCryptoScreen(
        addToBackStack: Boolean,
        preselectedAsset: AssetInfo,
        preselectedPaymentMethodId: String?
    ) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(
                R.id.content_frame,
                SimpleBuyCryptoFragment.newInstance(preselectedAsset, preselectedPaymentMethodId),
                SimpleBuyCryptoFragment::class.simpleName
            )
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyCryptoFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToCheckOutScreen(addToBackStack: Boolean) {
        ViewUtils.hideKeyboard(this)
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(R.id.content_frame, SimpleBuyCheckoutFragment(), SimpleBuyCheckoutFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyCheckoutFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToKycVerificationScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(R.id.content_frame, SimpleBuyPendingKycFragment(), SimpleBuyPendingKycFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPendingKycFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToPendingOrderScreen() {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(
                R.id.content_frame,
                SimpleBuyCheckoutFragment.newInstance(true),
                SimpleBuyCheckoutFragment::class.simpleName
            )
            .commitAllowingStateLoss()
    }

    override fun startKyc() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, KYC_STARTED)
    }

    override fun pop() = onBackPressed()

    override fun hasMoreThanOneFragmentInTheStack(): Boolean =
        supportFragmentManager.backStackEntryCount > 1

    override fun goToPaymentScreen(addToBackStack: Boolean, isPaymentAuthorised: Boolean) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(
                R.id.content_frame, SimpleBuyPaymentFragment.newInstance(isPaymentAuthorised),
                SimpleBuyPaymentFragment::class.simpleName
            )
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPaymentFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressed() }

    override fun showLoading() = binding.progress.visible()

    override fun hideLoading() = binding.progress.gone()

    override fun launchBankAuthWithError(errorState: ErrorState) {
        startActivity(BankAuthActivity.newInstance(errorState, BankAuthSource.SIMPLE_BUY, this))
    }

    override fun goToSetupFirstRecurringBuy(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(R.id.content_frame, RecurringBuyFirstTimeBuyerFragment())
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPaymentFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToFirstRecurringBuyCreated(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .addAnimationTransaction()
            .replace(R.id.content_frame, RecurringBuySuccessfulFragment())
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPaymentFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    companion object {
        const val KYC_STARTED = 6788
        const val RESULT_KYC_SIMPLE_BUY_COMPLETE = 7854

        private const val STARTED_FROM_NAVIGATION_KEY = "started_from_navigation_key"
        private const val STARTED_FROM_APPROVAL_KEY = "STARTED_FROM_APPROVAL_KEY"
        private const val ASSET_KEY = "crypto_currency_key"
        private const val PRESELECTED_PAYMENT_METHOD = "preselected_payment_method_key"
        private const val STARTED_FROM_KYC_RESUME = "started_from_kyc_resume_key"

        fun newInstance(
            context: Context,
            asset: AssetInfo? = null,
            launchFromNavigationBar: Boolean = false,
            launchKycResume: Boolean = false,
            preselectedPaymentMethodId: String? = null,
            launchFromApprovalDeepLink: Boolean = false
        ) = Intent(context, SimpleBuyActivity::class.java).apply {
            putExtra(STARTED_FROM_NAVIGATION_KEY, launchFromNavigationBar)
            putExtra(ASSET_KEY, asset?.ticker)
            putExtra(STARTED_FROM_KYC_RESUME, launchKycResume)
            putExtra(PRESELECTED_PAYMENT_METHOD, preselectedPaymentMethodId)
            putExtra(STARTED_FROM_APPROVAL_KEY, launchFromApprovalDeepLink)
        }
    }
}