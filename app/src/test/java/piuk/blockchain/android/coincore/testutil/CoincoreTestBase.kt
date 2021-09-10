package piuk.blockchain.android.coincore.testutil

import com.blockchain.core.price.ExchangeRate
import com.blockchain.android.testutils.rxInit
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import org.junit.After
import org.junit.Rule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module

private fun injectMocks(module: Module) {
    startKoin {
        modules(
            listOf(
                module
            )
        )
    }
}

open class CoincoreTestBase {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    protected open val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency }.thenReturn(TEST_USER_FIAT)
    }

    private val userFiatToUserFiat = ExchangeRate.FiatToFiat(
        from = TEST_USER_FIAT,
        to = TEST_USER_FIAT,
        rate = 1.0.toBigDecimal()
    )

    private val apiFiatToUserFiat = ExchangeRate.FiatToFiat(
        from = TEST_API_FIAT,
        to = TEST_USER_FIAT,
        rate = 2.0.toBigDecimal()
    )

    protected val exchangeRates: ExchangeRatesDataManager = mock {
        on { getLastFiatToUserFiatRate(TEST_USER_FIAT) }.thenReturn(userFiatToUserFiat)
        on { getLastFiatToUserFiatRate(TEST_API_FIAT) }.thenReturn(apiFiatToUserFiat)
    }

    protected open fun initMocks() {
        injectMocks(
            module {
                scope(payloadScopeQualifier) {
                    factory {
                        currencyPrefs
                    }
                }
            }
        )
    }

    @After
    open fun teardown() {
        stopKoin()
    }

    companion object {
        @JvmStatic
        protected val TEST_USER_FIAT = "EUR"
        @JvmStatic
        protected val TEST_API_FIAT = "USD"

        // do not modify these objects, as they're shared between tests
        val TEST_ASSET = object : CryptoCurrency(
            ticker = "NOPE",
            name = "Not a real thing",
            categories = setOf(AssetCategory.CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 3,
            colour = "000000"
        ) {}

        val SECONDARY_TEST_ASSET = object : CryptoCurrency(
            ticker = "NOPE2",
            name = "Not a real thing",
            categories = setOf(AssetCategory.CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 3,
            colour = "000000"
        ) {}
    }
}