package piuk.blockchain.android.ui.dashboard.assetdetails

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.isCustodialOnly
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.databinding.DialogSheetDashboardAssetDetailsBinding
import piuk.blockchain.android.simplebuy.CustodialBalanceClicked
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.account.PendingBalanceAccountDecorator
import piuk.blockchain.android.ui.dashboard.assetdetails.delegates.AssetDetailAdapterDelegate
import piuk.blockchain.android.ui.dashboard.setDeltaColour
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyAnalytics
import piuk.blockchain.android.ui.recurringbuy.onboarding.RecurringBuyOnboardingActivity
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.invisible
import piuk.blockchain.android.util.loadInterMedium
import piuk.blockchain.android.util.setOnTabSelectedListener
import piuk.blockchain.android.util.visible
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class AssetDetailSheet : MviBottomSheet<AssetDetailsModel,
    AssetDetailsIntent, AssetDetailsState, DialogSheetDashboardAssetDetailsBinding>() {

    private val currencyPrefs: CurrencyPrefs by inject()
    private val labels: DefaultLabels by inject()
    private val assetCatalogue: AssetCatalogue by inject()
    private val locale = Locale.getDefault()

    private val asset: AssetInfo by lazy {
        arguments?.getString(ARG_CRYPTO_ASSET)?.let {
            assetCatalogue.fromNetworkTicker(it)
        } ?: throw IllegalArgumentException("No cryptoCurrency specified")
    }

    private val assetSelect: Coincore by scopedInject()

    private val token: CryptoAsset by lazy {
        assetSelect[asset]
    }

    private val listItems = mutableListOf<AssetDetailsItem>()

    private val detailsAdapter by lazy {
        AssetDetailAdapter(
            ::onAccountSelected,
            labels
        ) {
            PendingBalanceAccountDecorator(it.account)
        }
    }
    
    private val adapterDelegate by lazy {
        AssetDetailAdapterDelegate(
            ::onAccountSelected,
            token,
            labels,
            ::openOnboardingForRecurringBuy,
            ::onRecurringBuyClicked
        ) {
            PendingBalanceAccountDecorator(it.account)
        }
    }

    private var state = AssetDetailsState()

    override val model: AssetDetailsModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetDashboardAssetDetailsBinding =
        DialogSheetDashboardAssetDetailsBinding.inflate(inflater, container, false)

    @UiThread
    override fun render(newState: AssetDetailsState) {
        clearList()

        if (newState.errorState != AssetDetailsError.NONE) {
            handleErrorState(newState.errorState)
        }

        newState.assetDisplayMap?.let { assetDisplayMap ->
            onGotAssetDetails(assetDisplayMap)
        }

        newState.recurringBuys?.let {
            renderRecurringBuys(it)
        }

        binding.currentPrice.text = newState.assetFiatPrice

        configureTimespanSelectionUI(binding, newState.timeSpan)

        if (newState.chartLoading) {
            chartToLoadingState()
        } else {
            chartToDataState()
        }

        binding.chart.apply {
            if (newState.chartData != state.chartData) {
                updateChart(this, newState.chartData)
            }
        }

        updatePriceChange(binding.priceChange, newState.chartData)

        state = newState
    }

    override fun initControls(binding: DialogSheetDashboardAssetDetailsBinding) {
        model.process(LoadAsset(token))

        with(binding) {
            configureChart(
                chart,
                getFiatSymbol(currencyPrefs.selectedFiatCurrency),
                numOfDecimalsForChart(asset)
            )

            configureTabs(chartPricePeriods)

            currentPriceTitle.text =
                getString(R.string.dashboard_price_for_asset, asset.ticker)

            assetList.apply {
                adapter = adapterDelegate
                addItemDecoration(BlockchainListDividerDecor(requireContext()))
            }
        }
    }

    override fun dismiss() {
        super.dismiss()
        model.process(ClearSheetDataIntent)
    }

    private fun openOnboardingForRecurringBuy() {
        analytics.logEvent(RecurringBuyAnalytics.RecurringBuyLearnMoreClicked(LaunchOrigin.CURRENCY_PAGE))
        startActivity(
            RecurringBuyOnboardingActivity.newInstance(
                context = requireContext(),
                fromCoinView = true,
                asset = asset
            )
        )
        dismiss()
    }

    private fun onRecurringBuyClicked(recurringBuy: RecurringBuy) {
        clearList()
        analytics.logEvent(
            RecurringBuyAnalytics.RecurringBuyDetailsClicked(
                LaunchOrigin.RECURRING_BUY_DETAILS,
                recurringBuy.asset.ticker
            )
        )
        model.process(ShowRecurringBuySheet(recurringBuy))
    }

    private fun clearList() {
        listItems.clear()
        updateList()
    }

    private fun updateList() {
        adapterDelegate.items = listItems
        adapterDelegate.notifyDataSetChanged()
    }

    private fun renderRecurringBuys(recurringBuys: Map<String, RecurringBuy>) {
        if (recurringBuys.keys.isNotEmpty()) {
            val recurringBuysItems = recurringBuys.values.map {
                AssetDetailsItem.RecurringBuyInfo(
                    it
                )
            }
            listItems.addAll(recurringBuysItems)
        } else {
            listItems.add(AssetDetailsItem.RecurringBuyBanner)
        }

        updateList()
    }

    private fun onGotAssetDetails(assetDetails: AssetDisplayMap) {
        val itemList = mutableListOf<AssetDetailsItem>()

        assetDetails[AssetFilter.NonCustodial]?.let {
            itemList.add(
                AssetDetailsItem.CryptoDetailsInfo(
                    assetFilter = AssetFilter.NonCustodial,
                    account = it.account,
                    balance = it.amount,
                    fiatBalance = it.fiatValue,
                    actions = it.actions,
                    interestRate = it.interestRate
                )
            )
        }

        assetDetails[AssetFilter.Custodial]?.let {
            itemList.add(
                AssetDetailsItem.CryptoDetailsInfo(
                    assetFilter = AssetFilter.Custodial,
                    account = it.account,
                    balance = it.amount,
                    fiatBalance = it.fiatValue,
                    actions = it.actions,
                    interestRate = it.interestRate
                )
            )
        }

        assetDetails[AssetFilter.Interest]?.let {
            itemList.add(
                AssetDetailsItem.CryptoDetailsInfo(
                    assetFilter = AssetFilter.Interest,
                    account = it.account,
                    balance = it.amount,
                    fiatBalance = it.fiatValue,
                    actions = it.actions,
                    interestRate = it.interestRate
                )
            )
        }

        if (asset.isCustodialOnly) {
            listItems.add(0, AssetDetailsItem.AssetLabel)
        }

        listItems.addAll(0, itemList)
        updateList()
    }

    private fun onAccountSelected(account: BlockchainAccount, assetFilter: AssetFilter) {
        clearList()

        if (account is CryptoAccount && assetFilter == AssetFilter.Custodial) {
            analytics.logEvent(CustodialBalanceClicked(account.asset))
        }

        state.assetDisplayMap?.get(assetFilter)?.let {
            model.process(
                ShowAssetActionsIntent(account)
            )
        }
    }

    private fun updateChart(chart: LineChart, data: HistoricalRateList) {
        chart.apply {
            visible()
            clear()
            if (data.isEmpty()) {
                binding.priceChange.text = "--"
                return
            }
            val entries = data
                .map {
                    Entry(
                        it.timestamp.toFloat(),
                        it.rate.toFloat()
                    )
                }

            this.data = LineData(LineDataSet(entries, null).apply {
                color = ContextCompat.getColor(context, getDataRepresentationColor(data))
                lineWidth = 2f
                mode = LineDataSet.Mode.LINEAR
                setDrawValues(false)
                setDrawCircles(false)
                isHighlightEnabled = true
                setDrawHighlightIndicators(false)
                marker = ValueMarker(
                    context,
                    R.layout.price_chart_marker,
                    getFiatSymbol(currencyPrefs.selectedFiatCurrency),
                    numOfDecimalsForChart(asset)
                )
            })
            animateX(500)
        }
    }

    private fun handleErrorState(error: AssetDetailsError) {
        val errorString = when (error) {
            AssetDetailsError.NO_CHART_DATA ->
                getString(R.string.asset_details_chart_load_failed_toast)
            AssetDetailsError.NO_ASSET_DETAILS ->
                getString(R.string.asset_details_load_failed_toast)
            AssetDetailsError.NO_EXCHANGE_RATE ->
                getString(R.string.asset_details_exchange_load_failed_toast)
            else -> "" // this never triggers
        }
        ToastCustom.makeText(requireContext(), errorString, Toast.LENGTH_SHORT, ToastCustom.TYPE_ERROR)
    }

    private fun chartToLoadingState() {
        with(binding) {
            pricesLoading.visible()
            chart.invisible()
            priceChange.apply {
                text = "--"
                setTextColor(ContextCompat.getColor(context, R.color.dashboard_chart_unknown))
            }
        }
    }

    private fun chartToDataState() {
        binding.pricesLoading.gone()
        binding.chart.visible()
    }

    private fun configureTabs(chartPricePeriods: TabLayout) {
        HistoricalTimeSpan.values().forEachIndexed { index, timeSpan ->
            chartPricePeriods.getTabAt(index)?.text = timeSpan.tabName()
        }
        chartPricePeriods.setOnTabSelectedListener {
            model.process(UpdateTimeSpan(HistoricalTimeSpan.values()[it]))
        }
    }

    private fun HistoricalTimeSpan.tabName() =
        when (this) {
            HistoricalTimeSpan.ALL_TIME -> "ALL"
            HistoricalTimeSpan.YEAR -> "1Y"
            HistoricalTimeSpan.MONTH -> "1M"
            HistoricalTimeSpan.WEEK -> "1W"
            HistoricalTimeSpan.DAY -> "1D"
        }

    private fun getDataRepresentationColor(data: HistoricalRateList): Int {
        // We have filtered out nulls by here, so we can 'safely' default to zeros for the price
        val firstPrice: Double = data.first().rate
        val lastPrice: Double = data.last().rate

        val diff = lastPrice - firstPrice
        return if (diff < 0) R.color.dashboard_chart_negative else R.color.dashboard_chart_positive
    }

    @SuppressLint("SetTextI18n")
    private fun updatePriceChange(percentageView: AppCompatTextView, data: HistoricalRateList) {
        // We have filtered out nulls by here, so we can 'safely' default to zeros for the price
        val firstPrice: Double = data.firstOrNull()?.rate ?: 0.0
        val lastPrice: Double = data.lastOrNull()?.rate ?: 0.0
        val difference = lastPrice - firstPrice

        val percentChange = (difference / firstPrice) * 100
        val percentChangeTxt = if (percentChange.isNaN()) {
            "--"
        } else {
            String.format("%.1f", percentChange)
        }

        percentageView.text =
            FiatValue.fromMajor(
                currencyPrefs.selectedFiatCurrency,
                difference.toBigDecimal()
            ).toStringWithSymbol() + " ($percentChangeTxt%)"

        percentageView.setDeltaColour(
            delta = difference,
            negativeColor = R.color.dashboard_chart_negative,
            positiveColor = R.color.dashboard_chart_positive
        )
    }

    private fun configureChart(chart: LineChart, fiatSymbol: String, decimalPlaces: Int) {
        chart.apply {
            setDrawGridBackground(false)
            setDrawBorders(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            axisLeft.setDrawGridLines(false)

            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return fiatSymbol + NumberFormat.getNumberInstance(Locale.getDefault())
                        .apply {
                            maximumFractionDigits = decimalPlaces
                            minimumFractionDigits = decimalPlaces
                            roundingMode = RoundingMode.HALF_UP
                        }.format(value)
                }
            }

            axisLeft.granularity = 0.005f
            axisLeft.isGranularityEnabled = true
            axisLeft.textColor = ContextCompat.getColor(context, R.color.primary_grey_medium)
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            xAxis.textColor = ContextCompat.getColor(context, R.color.primary_grey_medium)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.isGranularityEnabled = true
            setExtraOffsets(8f, 0f, 0f, 10f)
            setNoDataTextColor(ContextCompat.getColor(context, R.color.primary_grey_medium))
            val typeFace = context.loadInterMedium()
            xAxis.typeface = typeFace
            axisLeft.typeface = typeFace
        }
    }

    private fun configureTimespanSelectionUI(
        binding: DialogSheetDashboardAssetDetailsBinding,
        selection: HistoricalTimeSpan
    ) {
        val dateFormat = when (selection) {
            HistoricalTimeSpan.ALL_TIME -> SimpleDateFormat("yyyy", locale)
            HistoricalTimeSpan.YEAR -> SimpleDateFormat("MMM ''yy", locale)
            HistoricalTimeSpan.MONTH,
            HistoricalTimeSpan.WEEK -> SimpleDateFormat("dd. MMM", locale)
            HistoricalTimeSpan.DAY -> SimpleDateFormat("H:00", locale)
        }

        val granularity = when (selection) {
            HistoricalTimeSpan.ALL_TIME -> 60 * 60 * 24 * 365F
            HistoricalTimeSpan.YEAR -> 60 * 60 * 24 * 30F
            HistoricalTimeSpan.MONTH,
            HistoricalTimeSpan.WEEK -> 60 * 60 * 24 * 2F
            HistoricalTimeSpan.DAY -> 60 * 60 * 4F
        }

        with(binding) {
            chart.xAxis.apply {
                this.granularity = granularity
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return dateFormat.format(Date(value.toLong() * 1000))
                    }
                }
            }

            priceChangePeriod.text = resources.getString(
                when (selection) {
                    HistoricalTimeSpan.YEAR -> R.string.dashboard_time_span_last_year
                    HistoricalTimeSpan.MONTH -> R.string.dashboard_time_span_last_month
                    HistoricalTimeSpan.WEEK -> R.string.dashboard_time_span_last_week
                    HistoricalTimeSpan.DAY -> R.string.dashboard_time_span_last_day
                    HistoricalTimeSpan.ALL_TIME -> R.string.dashboard_time_span_all_time
                }
            )

            chartPricePeriods.getTabAt(selection.ordinal)?.select()
        }
    }

    companion object {
        private const val ARG_CRYPTO_ASSET = "crypto"

        fun newInstance(asset: AssetInfo): AssetDetailSheet {
            return AssetDetailSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CRYPTO_ASSET, asset.ticker)
                }
            }
        }

        private fun getFiatSymbol(currencyCode: String, locale: Locale = Locale.getDefault()) =
            Currency.getInstance(currencyCode).getSymbol(locale)

        private fun numOfDecimalsForChart(asset: AssetInfo): Int =
            when (asset.ticker) {
                CryptoCurrency.XLM.ticker -> 4
                else -> 2
            }
    }
}
