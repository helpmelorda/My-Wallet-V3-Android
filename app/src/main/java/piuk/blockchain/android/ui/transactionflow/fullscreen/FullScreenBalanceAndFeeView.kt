package piuk.blockchain.android.ui.transactionflow.fullscreen

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.canConvert
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewTxFullscreenFeeAndBalanceBinding
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.ui.transactionflow.plugin.EnterAmountWidget
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visibleIf

class FullScreenBalanceAndFeeView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), EnterAmountWidget {

    private lateinit var model: TransactionModel
    private lateinit var customiser: EnterAmountCustomisations
    private lateinit var analytics: TxFlowAnalytics

    private val binding: ViewTxFullscreenFeeAndBalanceBinding =
        ViewTxFullscreenFeeAndBalanceBinding.inflate(LayoutInflater.from(context), this, true)

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
    }

    override fun update(state: TransactionState) {
        check(::model.isInitialized) { "Control not initialised" }

        updateMaxGroup(state)
        updateBalance(state)
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
            val isPositiveAmount = state.amount.isPositive

            networkFeeLabel.visibleIf { isPositiveAmount }
            networkFeeValue.visibleIf { isPositiveAmount }
            networkFeeArrow.visibleIf { isPositiveAmount }
            feeForFullAvailableLabel.visibleIf { isPositiveAmount }
            feeForFullAvailableValue.visibleIf { isPositiveAmount }
            totalAvailableLabel.visibleIf { isPositiveAmount }
            totalAvailableValue.visibleIf { isPositiveAmount }

            with(useMax) {
                visibleIf { !isPositiveAmount && !customiser.shouldDisableInput(state.errorState) }
                text = customiser.enterAmountMaxButton(state)
                setOnClickListener {
                    analytics.onMaxClicked(state)
                    model.process(TransactionIntent.UseMaxSpendable)
                }
            }
        }
}
