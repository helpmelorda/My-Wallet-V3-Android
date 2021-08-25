package piuk.blockchain.android.ui.transactionflow.fullscreen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.blockchain.core.price.ExchangeRate
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.databinding.FragmentTxFlowEnterAmountBinding
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.FiatCryptoInputView
import piuk.blockchain.android.ui.customviews.FiatCryptoViewConfiguration
import piuk.blockchain.android.ui.customviews.PrefixedOrSuffixedEditText
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.IssueType
import piuk.blockchain.android.ui.transactionflow.plugin.ExpandableTxFlowWidget
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget
import timber.log.Timber
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class EnterAmountFragment : TransactionFlowFragment<FragmentTxFlowEnterAmountBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTxFlowEnterAmountBinding =
        FragmentTxFlowEnterAmountBinding.inflate(inflater, container, false)

    private val customiser: EnterAmountCustomisations by inject()
    private val compositeDisposable = CompositeDisposable()
    private var state: TransactionState = TransactionState()

    private var lowerSlot: TxFlowWidget? = null
    private var upperSlot: TxFlowWidget? = null

    private var initialValueSet: Boolean = false

    private val imm: InputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        compositeDisposable += binding.amountSheetInput.amount
            .debounce(AMOUNT_DEBOUNCE_TIME_MS, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { amount ->
                state.fiatRate?.let { rate ->
                    check(state.pendingTx != null) { "Px is not initialised yet" }
                    model.process(
                        TransactionIntent.AmountChanged(
                            if (!state.allowFiatInput && amount is FiatValue) {
                                convertFiatToCrypto(amount, rate as ExchangeRate.CryptoToFiat, state).also {
                                    binding.amountSheetInput.fixExchange(it)
                                }
                            } else {
                                amount
                            }
                        )
                    )
                }
            }

        compositeDisposable += binding.amountSheetInput
            .onImeAction
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe {
                when (it) {
                    PrefixedOrSuffixedEditText.ImeOptions.NEXT -> {
                        if (state.nextEnabled) {
                            onCtaClick(state)
                        }
                    }
                    PrefixedOrSuffixedEditText.ImeOptions.BACK -> {
                        hideKeyboard()
                    }
                    else -> {
                        // do nothing
                    }
                }
            }

        compositeDisposable += binding.amountSheetInput.onInputToggle
            .subscribe {
                analyticsHooks.onCryptoToggle(it, state)
                model.process(TransactionIntent.DisplayModeChanged(it))
            }
    }

    @SuppressLint("SetTextI18n")
    override fun render(newState: TransactionState) {
        Timber.d("!TRANSACTION!> Rendering! EnterAmountFragment")

        with(binding) {
            amountSheetCtaButton.isEnabled = newState.nextEnabled
            amountSheetCtaButton.setOnClickListener {
                onCtaClick(newState)
            }

            if (!amountSheetInput.configured) {
                newState.pendingTx?.let {
                    amountSheetInput.configure(newState, customiser.defInputType(newState, it.selectedFiat))
                }
            }

            val availableBalance = newState.availableBalance
            if (availableBalance.isPositive || availableBalance.isZero) {
                // The maxLimit set here controls the number of digits that can be entered,
                // but doesn't restrict the input to be always under that value. Which might be
                // strange UX, but is currently by design.
                if (amountSheetInput.configured) {
                    if (customiser.shouldShowMaxLimit(newState)) {
                        amountSheetInput.maxLimit = newState.availableBalance
                    }
                    if (amountSheetInput.customInternalExchangeRate != newState.fiatRate) {
                        amountSheetInput.customInternalExchangeRate = newState.fiatRate
                    }
                }

                if (newState.setMax) {
                    amountSheetInput.updateValue(newState.maxSpendable)
                } else {
                    if (!initialValueSet) {
                        newState.initialAmountToSet()?.let {
                            amountSheetInput.updateValue(it)
                            initialValueSet = true
                        }
                    }
                }

                initialiseLowerSlotIfNeeded(newState)
                initialiseUpperSlotIfNeeded(newState)

                lowerSlot?.update(newState)
                upperSlot?.update(newState)

                showFlashMessageIfNeeded(newState)
            }

            newState.pendingTx?.let {
                if (it.feeSelection.selectedLevel == FeeLevel.None) {
                    frameLowerSlot.setOnClickListener(null)
                } else {
                    if (frameLowerSlot.getChildAt(0) is FullScreenBalanceAndFeeView) {
                        root.setOnClickListener {
                            FeeSelectionBottomSheet.newInstance().show(childFragmentManager, BOTTOM_SHEET)
                        }
                    }
                }
            }
        }

        state = newState
    }

    private fun FragmentTxFlowEnterAmountBinding.showFlashMessageIfNeeded(
        state: TransactionState
    ) {
        customiser.issueFlashMessage(state, amountSheetInput.configuration.inputCurrency)?.let {
            when (customiser.selectIssueType(state)) {
                IssueType.ERROR -> {
                    amountSheetInput.showError(it, customiser.shouldDisableInput(state.errorState))
                }
                IssueType.INFO -> {
                    amountSheetInput.showInfo(it) {
                        KycNavHostActivity.start(requireActivity(), CampaignType.Swap, true)
                    }
                }
            }
        } ?: showFeesTooHighMessageOrHide(state)
    }

    private fun FragmentTxFlowEnterAmountBinding.showFeesTooHighMessageOrHide(
        state: TransactionState
    ) {
        val feesTooHighMsg = customiser.issueFeesTooHighMessage(state)
        if (state.pendingTx != null && state.pendingTx.isLowOnBalance() && feesTooHighMsg != null) {
            amountSheetInput.showError(
                errorMessage = feesTooHighMsg
            )
        } else {
            amountSheetInput.hideLabels()
        }
    }

    private fun FragmentTxFlowEnterAmountBinding.initialiseUpperSlotIfNeeded(state: TransactionState) {
        if (upperSlot == null) {
            upperSlot = customiser.installEnterAmountUpperSlotView(
                requireContext(),
                frameUpperSlot,
                state
            ).apply {
                initControl(model, customiser, analyticsHooks)
            }
            (lowerSlot as? ExpandableTxFlowWidget)?.let {
                it.expanded.observeOn(AndroidSchedulers.mainThread()).subscribe {
                    configureCtaButton()
                }
            }
        }
    }

    private fun configureCtaButton() {
        val layoutParams: ViewGroup.MarginLayoutParams =
            binding.amountSheetCtaButton.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = resources.getDimension(R.dimen.standard_margin).toInt()
        binding.amountSheetCtaButton.layoutParams = layoutParams
    }

    private fun FragmentTxFlowEnterAmountBinding.initialiseLowerSlotIfNeeded(newState: TransactionState) {
        if (lowerSlot == null) {
            lowerSlot = customiser.installEnterAmountLowerSlotView(
                requireContext(),
                frameLowerSlot,
                newState
            ).apply {
                initControl(model, customiser, analyticsHooks)
            }
        }
    }

    // in this method we try to convert the fiat value coming out from
    // the view to a crypto which is withing the min and max limits allowed.
    // We use floor rounding for max and ceiling for min just to make sure that we wont have problem with rounding once
    // the amount reach the engine where the comparison with limits will happen.

    private fun convertFiatToCrypto(
        amount: FiatValue,
        rate: ExchangeRate.CryptoToFiat,
        state: TransactionState
    ): Money {
        val min = state.pendingTx?.minLimit ?: return rate.inverse().convert(amount)
        val max = state.maxSpendable
        val roundedUpAmount = rate.inverse(RoundingMode.CEILING, CryptoValue.DISPLAY_DP)
            .convert(amount)
        val roundedDownAmount = rate.inverse(RoundingMode.FLOOR, CryptoValue.DISPLAY_DP)
            .convert(amount)
        return if (roundedUpAmount >= min && roundedUpAmount <= max)
            roundedUpAmount
        else roundedDownAmount
    }

    private fun onCtaClick(state: TransactionState) {
        hideKeyboard()
        model.process(TransactionIntent.PrepareTransaction)
        analyticsHooks.onEnterAmountCtaClick(state)
    }

    private fun hideKeyboard() {
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun FiatCryptoInputView.configure(
        state: TransactionState,
        inputCurrency: CurrencyType
    ) {
        if (inputCurrency is CurrencyType.Crypto || state.amount.takeIf { it is CryptoValue }?.isPositive == true) {
            val selectedFiat = state.pendingTx?.selectedFiat ?: return
            configuration = FiatCryptoViewConfiguration(
                inputCurrency = CurrencyType.Crypto(state.sendingAsset),
                exchangeCurrency = CurrencyType.Fiat(selectedFiat),
                predefinedAmount = state.amount
            )
        } else {
            val selectedFiat = state.pendingTx?.selectedFiat ?: return
            val fiatRate = state.fiatRate ?: return
            val isCryptoWithFiatExchange = state.amount is CryptoValue && fiatRate is ExchangeRate.CryptoToFiat
            configuration = FiatCryptoViewConfiguration(
                inputCurrency = CurrencyType.Fiat(selectedFiat),
                outputCurrency = CurrencyType.Fiat(selectedFiat),
                exchangeCurrency = state.sendingAccount.currencyType(),
                predefinedAmount = if (isCryptoWithFiatExchange) {
                    fiatRate.convert(state.amount)
                } else {
                    state.amount
                }
            )
        }
        showKeyboard()
    }

    private fun showKeyboard() {
        val inputView = binding.amountSheetInput.findViewById<PrefixedOrSuffixedEditText>(
            R.id.enter_amount
        )

        inputView?.run {
            requestFocus()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    companion object {
        private const val AMOUNT_DEBOUNCE_TIME_MS = 300L
        private const val BOTTOM_SHEET = "BOTTOM_SHEET"

        fun newInstance(): EnterAmountFragment = EnterAmountFragment()
    }
}

private fun BlockchainAccount.currencyType(): CurrencyType =
    when (this) {
        is CryptoAccount -> CurrencyType.Crypto(this.asset)
        is FiatAccount -> CurrencyType.Fiat(this.fiatCurrency)
        else -> throw IllegalStateException("Account not supported")
    }

private fun PendingTx.isLowOnBalance() =
    feeSelection.selectedLevel != FeeLevel.None &&
        availableBalance.isZero && totalBalance.isPositive
