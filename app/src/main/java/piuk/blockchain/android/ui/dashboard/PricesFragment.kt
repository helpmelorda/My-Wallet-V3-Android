package piuk.blockchain.android.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.databinding.FragmentPricesBinding
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.dashboard.adapter.PricesDelegateAdapter
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsFlow
import piuk.blockchain.android.ui.dashboard.assetdetails.assetActionEvent
import piuk.blockchain.android.ui.dashboard.model.PricesIntent
import piuk.blockchain.android.ui.dashboard.model.PricesModel
import piuk.blockchain.android.ui.dashboard.model.PricesState
import piuk.blockchain.android.ui.home.HomeScreenMviFragment
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.android.ui.transactionflow.TransactionLauncher
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

data class PricesItem(
    val asset: AssetInfo,
    val priceWithDelta: Prices24HrWithDelta? = null
    // Etc
) {
    val assetTicker = asset.ticker
    val assetName = asset.name
}

internal class PricesFragment :
    HomeScreenMviFragment<PricesModel, PricesIntent, PricesState, FragmentPricesBinding>(),
    DialogFlow.FlowHost,
    AssetDetailsFlow.AssetDetailsHost {

    override val model: PricesModel by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val assetResources: AssetResources by inject()
    private val txLauncher: TransactionLauncher by inject()

    private val theAdapter: PricesDelegateAdapter by lazy {
        PricesDelegateAdapter(
            prefs = currencyPrefs,
            onPriceRequest = { onGetAssetPrice(it) },
            onCardClicked = { onAssetClicked(it) },
            assetResources = assetResources
        )
    }

    private val theLayoutManager: RecyclerView.LayoutManager by unsafeLazy {
        SafeLayoutManager(requireContext())
    }

    private val displayList = mutableListOf<PricesItem>()

    private val compositeDisposable = CompositeDisposable()

    // Hold the 'current' display state, to enable optimising of state updates
    private var state: PricesState? = null

    @UiThread
    override fun render(newState: PricesState) {
        try {
            doRender(newState)
        } catch (e: Throwable) {
            Timber.e("Error rendering: $e")
        }
    }

    @UiThread
    private fun doRender(newState: PricesState) {
        binding.swipe.isRefreshing = false

        updateDisplayList(newState)

        // Update/show dialog flow
        if (state?.activeFlow != newState.activeFlow) {
            state?.activeFlow?.let {
                clearBottomSheet()
            }

            newState.activeFlow?.let {
                if (it is TransactionFlow) {
                    txLauncher.startFlow(
                        activity = requireActivity(),
                        fragmentManager = childFragmentManager,
                        action = it.txAction,
                        flowHost = this@PricesFragment,
                        sourceAccount = it.txSource,
                        target = it.txTarget
                    )
                } else {
                    it.startFlow(childFragmentManager, this)
                }
            }
        }
        this.state = newState
    }

    private fun updateDisplayList(newState: PricesState) {
        val newList = newState.availablePrices.values.map {
            PricesItem(
                asset = it.assetInfo,
                priceWithDelta = it.prices
            )
        }

        with(displayList) {
            clear()
            addAll(newList.sortedBy { it.assetName })
        }
        theAdapter.notifyDataSetChanged()
    }

    override fun onBackPressed(): Boolean = false

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPricesBinding =
        FragmentPricesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSwipeRefresh()
        setupRecycler()
    }

    private fun setupRecycler() {
        binding.recyclerView.apply {
            layoutManager = theLayoutManager
            adapter = theAdapter

            addItemDecoration(BlockchainListDividerDecor(requireContext()))
        }
        theAdapter.items = displayList
    }

    private fun setupSwipeRefresh() {
        with(binding) {
            swipe.setOnRefreshListener { model.process(PricesIntent.GetAvailableAssets) }

            // Configure the refreshing colors
            swipe.setColorSchemeResources(
                R.color.blue_800,
                R.color.blue_600,
                R.color.blue_400,
                R.color.blue_200
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (isHidden) return

        initOrUpdateAssets()
    }

    private fun initOrUpdateAssets() {
        model.process(PricesIntent.GetAvailableAssets)
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private fun onGetAssetPrice(asset: AssetInfo) {
        model.process(PricesIntent.GetAssetPrice(asset))
    }

    private fun onAssetClicked(asset: AssetInfo) {
        analytics.logEvent(assetActionEvent(AssetDetailsAnalytics.WALLET_DETAILS, asset.ticker))
        model.process(PricesIntent.LaunchAssetDetailsFlow(asset))
    }

    override fun onFlowFinished() {
        model.process(PricesIntent.ClearBottomSheet)
    }

    override fun performAssetActionFor(action: AssetAction, account: BlockchainAccount) {
        clearBottomSheet()
        navigator().performAssetActionFor(action, account)
    }

    override fun goToBuy(asset: AssetInfo) {
        navigator().launchBuySell(BuySellFragment.BuySellViewType.TYPE_BUY, asset.ticker)
    }

    companion object {
        fun newInstance() = PricesFragment()
    }
}
