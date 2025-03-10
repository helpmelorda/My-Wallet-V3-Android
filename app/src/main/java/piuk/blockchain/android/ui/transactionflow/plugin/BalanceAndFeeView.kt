package piuk.blockchain.android.ui.transactionflow.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.core.price.ExchangeRate
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.databinding.ViewTxFlowFeeAndBalanceBinding
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.isVisible
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

class BalanceAndFeeView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), ExpandableTxFlowWidget {

    private lateinit var model: TransactionModel
    private lateinit var customiser: EnterAmountCustomisations
    private lateinit var analytics: TxFlowAnalytics
    private val imm: InputMethodManager by lazy {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private val binding: ViewTxFlowFeeAndBalanceBinding =
        ViewTxFlowFeeAndBalanceBinding.inflate(LayoutInflater.from(context), this, true)

    private val expandableSubject = PublishSubject.create<Boolean>()

    override val expanded: Observable<Boolean>
        get() = expandableSubject

    override fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    ) {
        check(this::model.isInitialized.not()) { "Control already initialised" }

        this.model = model
        this.customiser = customiser
        this.analytics = analytics

        binding.useMax.gone()

        binding.root.setOnClickListener {
            toggleDropdown()
        }
    }

    override fun update(state: TransactionState) {
        check(::model.isInitialized) { "Control not initialised" }

        updateMaxGroup(state)
        updateBalance(state)

        state.pendingTx?.let {
            if (it.feeSelection.selectedLevel == FeeLevel.None) {
                binding.feeEdit.gone()
            } else {
                binding.feeEdit.update(it.feeSelection, model)
                binding.feeEdit.visible()
            }
        }
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }

    private fun updateBalance(state: TransactionState) {
        with(binding) {
            val availableBalance = state.availableBalance
            maxAvailableValue.text = makeAmountString(availableBalance, state)
            feeForFullAvailableLabel.text = customiser.enterAmountMaxNetworkFeeLabel(state)

            state.pendingTx?.totalBalance?.let {
                totalAvailableValue.text = makeAmountString(it, state)
            }

            if (customiser.shouldNotDisplayNetworkFee(state)) {
                networkFeeValue.text = context.getString(R.string.fee_calculated_at_checkout)
                feeForFullAvailableValue.text = context.getString(R.string.fee_calculated_at_checkout)
            } else {
                state.pendingTx?.feeAmount?.let {
                    networkFeeValue.text = makeAmountString(it, state)
                }

                state.pendingTx?.feeForFullAvailable?.let {
                    feeForFullAvailableValue.text = makeAmountString(it, state)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun makeAmountString(value: Money, state: TransactionState): String =
        if ((value.isPositive || value.isZero) && state.fiatRate != null) {
            showFiatOrCryptoValues(
                currencyType = state.currencyType ?: (state.pendingTx?.selectedFiat?.let {
                    val defaultMode = customiser.defInputType(state, it)
                    model.process(TransactionIntent.DisplayModeChanged(defaultMode))
                    defaultMode
                } ?: CurrencyType.Crypto(state.sendingAsset)),
                state.fiatRate,
                value
            )
        } else {
            customiser.enterAmountGetNoBalanceMessage(state)
        }

    private fun showFiatOrCryptoValues(currencyType: CurrencyType, rate: ExchangeRate, value: Money) =
        when (currencyType) {
            is CurrencyType.Fiat -> {
                if (rate.canConvert(value))
                    rate.convert(value).toStringWithSymbol()
                else value.toStringWithSymbol()
            }
            is CurrencyType.Crypto -> value.toStringWithSymbol()
        }

    private fun updateMaxGroup(state: TransactionState) =
        with(binding) {
            networkFeeLabel.visibleIf { state.amount.isPositive }
            networkFeeValue.visibleIf { state.amount.isPositive }
            networkFeeArrow.visibleIf { state.amount.isPositive }
            with(useMax) {
                visibleIf { !state.amount.isPositive && !customiser.shouldDisableInput(state.errorState) }
                text = customiser.enterAmountMaxButton(state)
                setOnClickListener {
                    analytics.onMaxClicked(state)
                    model.process(TransactionIntent.UseMaxSpendable)
                }
            }
        }

    private var externalFocus: View? = null
    private fun toggleDropdown() {
        val revealDropdown = !binding.dropdown.isVisible()
        expandableSubject.onNext(revealDropdown)

        // Clear focus - and keyboard - remember it, so we can set it back when we close
        if (revealDropdown) {
            val viewGroup = findRootView()
            externalFocus = viewGroup?.findFocus()
            externalFocus?.clearFocus()
            hideKeyboard()
        } else {
            externalFocus?.let {
                it.requestFocus()
                showKeyboard(it)
            }
            externalFocus = null
        }

        with(binding.dropdown) {
            if (revealDropdown) {
                visible()
            } else {
                gone()
            }
        }
        // And flip the toggle indicator
        binding.networkFeeArrow.rotation += 180f
    }

    private fun findRootView(): ViewGroup? {
        var v = binding.root.parent as? ViewGroup
        while (v?.parent is ViewGroup) {
            v = v.parent as? ViewGroup
        }
        return v
    }

    private fun showKeyboard(inputView: View) {
        imm.showSoftInput(inputView, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}

private fun ExchangeRate.canConvert(value: Money): Boolean =
    when (this) {
        is ExchangeRate.FiatToCrypto -> value.currencyCode == this.from
        is ExchangeRate.CryptoToFiat -> (value is CryptoValue && value.currency == this.from)
        is ExchangeRate.FiatToFiat -> (value is FiatValue && value.currencyCode == this.from)
        is ExchangeRate.CryptoToCrypto -> (value is CryptoValue && value.currency == this.from)
    }
