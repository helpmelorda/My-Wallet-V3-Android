package piuk.blockchain.android.ui.dashboard.sheets

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyState
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.utils.toFormattedDate
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetRecurringBuyInfoBinding
import piuk.blockchain.android.simplebuy.CheckoutAdapterDelegate
import piuk.blockchain.android.simplebuy.SimpleBuyCheckoutItem
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringDate
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsError
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsIntent
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsModel
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsState
import piuk.blockchain.android.ui.dashboard.assetdetails.ClearSelectedRecurringBuy
import piuk.blockchain.android.ui.dashboard.assetdetails.DeleteRecurringBuy
import piuk.blockchain.android.ui.dashboard.assetdetails.GetPaymentDetails
import piuk.blockchain.android.ui.dashboard.assetdetails.ReturnToPreviousStep
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyAnalytics

class RecurringBuyDetailsSheet : MviBottomSheet<AssetDetailsModel,
    AssetDetailsIntent, AssetDetailsState, DialogSheetRecurringBuyInfoBinding>() {

    private val listAdapter: CheckoutAdapterDelegate by lazy {
        CheckoutAdapterDelegate()
    }

    override val model: AssetDetailsModel by scopedInject()

    lateinit var cacheState: AssetDetailsState

    override fun initControls(binding: DialogSheetRecurringBuyInfoBinding) {
        with(binding) {
            with(rbSheetItems) {
                adapter = listAdapter
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(BlockchainListDividerDecor(requireContext()))
            }

            rbSheetBack.setOnClickListener {
                returnToPreviousSheet()
            }

            rbSheetCancel.setOnClickListener {
                sendAnalyticsForRecurringBuyCancelClicked(cacheState)

                // TODO stopgap check while design make their mind up
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.settings_bank_remove_check_title)
                    .setMessage(R.string.recurring_buy_cancel_dialog_desc)
                    .setPositiveButton(R.string.common_ok) { di, _ ->
                        di.dismiss()
                        model.process(DeleteRecurringBuy)
                    }
                    .setNegativeButton(R.string.common_cancel) { di, _ ->
                        di.dismiss()
                    }.show()
            }
        }
    }

    private fun sendAnalyticsForRecurringBuyCancelClicked(state: AssetDetailsState) {
        state.selectedRecurringBuy?.let {
            val paymentMethodType = it.paymentDetails?.let { paymentType -> paymentType.paymentDetails }
                ?: throw IllegalStateException("Missing Payment Method on RecurringBuy")
            analytics.logEvent(
                RecurringBuyAnalytics
                    .RecurringBuyCancelClicked(
                        LaunchOrigin.RECURRING_BUY_DETAILS,
                        it.recurringBuyFrequency,
                        it.amount,
                        it.asset,
                        paymentMethodType
                    )
            )
        }
    }

    override fun render(newState: AssetDetailsState) {
        cacheState = newState
        if (newState.selectedRecurringBuy?.paymentDetails == null
        ) {
            model.process(GetPaymentDetails)
        }
        newState.selectedRecurringBuy?.let {
            when {
                it.state == RecurringBuyState.INACTIVE -> {
                    ToastCustom.makeText(
                        requireContext(), getString(R.string.recurring_buy_cancelled_toast), Toast.LENGTH_LONG,
                        ToastCustom.TYPE_OK
                    )
                    returnToPreviousSheet()
                }
                newState.errorState == AssetDetailsError.RECURRING_BUY_DELETE -> {
                    ToastCustom.makeText(
                        requireContext(), getString(R.string.recurring_buy_cancelled_error_toast), Toast.LENGTH_LONG,
                        ToastCustom.TYPE_ERROR
                    )
                }
                else ->
                    with(binding) {
                        rbSheetTitle.text = getString(R.string.recurring_buy_sheet_title, it.asset.ticker)
                        rbSheetHeader.setDetails(
                            it.amount.toStringWithSymbol(),
                            ""
                        )
                        it.renderListItems()
                    }
            }
        }
    }

    private fun returnToPreviousSheet() {
        model.process(ClearSelectedRecurringBuy)
        model.process(ReturnToPreviousStep)
    }

    private fun RecurringBuy.renderListItems() {
        listAdapter.items = listOfNotNull(
            if (paymentMethodType == PaymentMethodType.FUNDS) {
                SimpleBuyCheckoutItem.SimpleCheckoutItem(
                    getString(R.string.payment_method),
                    getString(R.string.recurring_buy_funds_label, amount.currencyCode)
                )
            } else {
                if (paymentMethodType == PaymentMethodType.PAYMENT_CARD) {
                    val paymentDetails = (paymentDetails as PaymentMethod.Card)
                    SimpleBuyCheckoutItem.ComplexCheckoutItem(
                        getString(R.string.payment_method),
                        paymentDetails.uiLabel(),
                        paymentDetails.endDigits
                    )
                } else {
                    val paymentDetails = (paymentDetails as PaymentMethod.Bank)
                    SimpleBuyCheckoutItem.ComplexCheckoutItem(
                        getString(R.string.payment_method),
                        paymentDetails.bankName,
                        paymentDetails.accountEnding
                    )
                }
            },
            SimpleBuyCheckoutItem.ComplexCheckoutItem(
                getString(R.string.recurring_buy_frequency_label),
                recurringBuyFrequency.toHumanReadableRecurringBuy(requireContext()),
                recurringBuyFrequency.toHumanReadableRecurringDate(requireContext())
            ),
            SimpleBuyCheckoutItem.SimpleCheckoutItem(
                getString(R.string.recurring_buy_info_purchase_label),
                nextPaymentDate.toFormattedDate()
            ),
            SimpleBuyCheckoutItem.SimpleCheckoutItem(
                getString(R.string.common_total),
                amount.toStringWithSymbol(),
                true
            )
        )
        listAdapter.notifyDataSetChanged()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetRecurringBuyInfoBinding =
        DialogSheetRecurringBuyInfoBinding.inflate(inflater, container, false)

    companion object {
        fun newInstance(): RecurringBuyDetailsSheet = RecurringBuyDetailsSheet()
    }
}