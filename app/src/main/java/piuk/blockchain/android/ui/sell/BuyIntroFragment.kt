package piuk.blockchain.android.ui.sell

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.BuySellPairs
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.databinding.BuyIntroFragmentBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.base.ViewPagerFragment
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.android.ui.customviews.account.HeaderDecoration
import piuk.blockchain.android.ui.customviews.account.removeAllHeaderDecorations
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.trackProgress
import piuk.blockchain.android.util.visible

class BuyIntroFragment : ViewPagerFragment() {

    private var _binding: BuyIntroFragmentBinding? = null
    private val binding: BuyIntroFragmentBinding
        get() = _binding!!

    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val compositeDisposable = CompositeDisposable()
    private val coincore: Coincore by scopedInject()
    private val simpleBuyPrefs: SimpleBuyPrefs by scopedInject()
    private val assetResources: AssetResources by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BuyIntroFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadBuyDetails()
    }

    private fun loadBuyDetails() {
        compositeDisposable +=
            custodialWalletManager.getSupportedBuySellCryptoCurrencies(
                currencyPrefs.selectedFiatCurrency
            ).flatMap { pairs ->
                Single.zip(
                    pairs.pairs.map {
                        coincore[it.cryptoCurrency].getPricesWith24hDelta()
                            .map { priceDelta ->
                                PriceHistory(
                                    currentExchangeRate = priceDelta.currentRate as ExchangeRate.CryptoToFiat,
                                    priceDelta = priceDelta.delta24h
                                )
                            }
                    }
                ) { t: Array<Any> ->
                    t.map { it as PriceHistory } to pairs.copy(pairs = pairs.pairs)
                }
            }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                binding.buyEmpty.gone()
            }
            .trackProgress(activityIndicator)
            .subscribeBy(
                onSuccess = { (exchangeRates, buyPairs) ->
                    renderBuyIntro(buyPairs, exchangeRates)
                },
                onError = {
                    renderErrorState()
                }
            )
    }

    private fun renderBuyIntro(
        buyPairs: BuySellPairs,
        pricesHistory: List<PriceHistory>
    ) {
        with(binding) {
            rvCryptos.visible()
            buyEmpty.gone()

            val introHeaderView = IntroHeaderView(requireContext())
            introHeaderView.setDetails(
                icon = R.drawable.ic_cart,
                label = R.string.select_crypto_you_want,
                title = R.string.buy_with_cash
            )

            rvCryptos.removeAllHeaderDecorations()
            rvCryptos.addItemDecoration(
                HeaderDecoration.with(requireContext())
                    .parallax(0.5f)
                    .setView(introHeaderView)
                    .build()
            )

            rvCryptos.layoutManager = LinearLayoutManager(activity)
            rvCryptos.adapter = BuyCryptoCurrenciesAdapter(
                buyPairs.pairs.map { pair ->
                    BuyCryptoItem(
                        asset = pair.cryptoCurrency,
                        price = pricesHistory.first { it.cryptoCurrency == pair.cryptoCurrency }
                            .currentExchangeRate
                            .price(),
                        percentageDelta = pricesHistory.first {
                            it.cryptoCurrency == pair.cryptoCurrency
                        }.percentageDelta
                    ) {
                        simpleBuyPrefs.clearBuyState()
                        startActivity(
                            SimpleBuyActivity.newInstance(
                                activity as Context,
                                pair.cryptoCurrency.ticker,
                                launchFromNavigationBar = true,
                                launchKycResume = false
                            )
                        )
                    }
                },
                assetResources
            )
        }
    }

    private fun renderErrorState() {
        with(binding) {
            rvCryptos.gone()
            buyEmpty.setDetails {
                loadBuyDetails()
            }
            buyEmpty.visible()
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = BuyIntroFragment()
    }
}

data class PriceHistory(
    val currentExchangeRate: ExchangeRate.CryptoToFiat,
    val priceDelta: Double
) {
    val cryptoCurrency: AssetInfo
        get() = currentExchangeRate.from

    val percentageDelta: Double
        get() = priceDelta
}

data class BuyCryptoItem(
    val asset: AssetInfo,
    val price: Money,
    val percentageDelta: Double,
    val click: () -> Unit
)

data class ExchangePriceWithDelta(
    val price: Money,
    val delta: Double
)