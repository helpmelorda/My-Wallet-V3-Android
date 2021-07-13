package piuk.blockchain.android.simplebuy

import android.annotation.SuppressLint
import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.test.assertEquals

@Suppress("UnnecessaryVariable")
class SimpleBuySyncFactoryTest {

    private val remoteState: CustodialWalletManager = mock()
    private val serializer: SimpleBuyPrefsSerializer = mock()

    private val subject = SimpleBuySyncFactory(
        custodialWallet = remoteState,
        serializer = serializer
    )

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Test
    fun `There are no buys in progress anywhere`() {

        whenever(serializer.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(Single.just(emptyList()))

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(null)
    }

    @Test
    fun `there is a local buy, in a pre-confirmed state, and no other buys in progress`() {

        val localInput = SimpleBuyState(
            amount = FiatValue.fromMinor("EUR", 1000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.INITIALISED,
            expirationDate = Date(),
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        whenever(serializer.fetch()).thenReturn(localInput)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(Single.just(emptyList()))

        val expectedResult = localInput

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    @Ignore("Temp not clearing state to facilitate happy path testing")
    fun `user is not eligible, clear any local state`() {

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        // Local state is cleared
        verify(serializer, atLeastOnce()).clear()

        // Local and remote state is not queried
        verifyNoMoreInteractions(serializer)
        verifyNoMoreInteractions(remoteState)
    }

    @Test
    fun `there is a remote buy, in an awaiting funds state, and no other buys in progress`() {

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = Date(),
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(remoteInput)
            )
        )

        val expectedResult = remoteInput.toSimpleBuyState() // Minor hack - should prob HC this

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    fun `there is a remote buy, in a pending state, and no other buys in progress`() {

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.PENDING_EXECUTION,
            expires = Date(),
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(remoteInput)
            )
        )

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    fun `there are several remote buys, all in awaiting funds state, no local buy in progress`() {
        // Which shouldn't ever happen, but it does.

        val remoteInput1 = BuySellOrder(
            id = ORDER_ID_2,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            state = OrderState.AWAITING_FUNDS,
            expires = MIDDLE_ORDER_DATE,
            fee = FiatValue.zero("EUR"),
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        val remoteInput2 = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.AWAITING_FUNDS,
            expires = LAST_ORDER_DATE,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        val remoteInput3 = BuySellOrder(
            id = ORDER_ID_3,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = EARLY_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput1,
                    remoteInput2,
                    remoteInput3
                )
            )
        )

        // If and when we encounter this situation, we will take the one that was submitted last
        val expectedResult = remoteInput2.toSimpleBuyState() // Minor hack - should prob HC this

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    fun `there are several remote buys, all in various completed states, no local buy in progress`() {
        // Which shouldn't ever happen, but it does.

        val remoteInput1 = BuySellOrder(
            id = ORDER_ID_1,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.CANCELED,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = MIDDLE_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        val remoteInput2 = BuySellOrder(
            id = ORDER_ID_2,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.PENDING_EXECUTION,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        val remoteInput3 = BuySellOrder(
            id = ORDER_ID_3,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.FINISHED,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = EARLY_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        val remoteInput4 = BuySellOrder(
            id = ORDER_ID_4,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            paymentMethodId = "123-123",
            state = OrderState.FAILED,
            expires = EARLY_ORDER_DATE,
            fee = FiatValue.zero("EUR"),
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput1,
                    remoteInput2,
                    remoteInput3,
                    remoteInput4
                )
            )
        )

        val expectedResult = remoteInput2.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    fun `there are several remote buys, some in awaiting funds some in pending state, no local buy in progress`() {

        val remoteInput1 = BuySellOrder(
            id = ORDER_ID_2,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.PENDING_EXECUTION,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = MIDDLE_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        val remoteInput2 = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.PENDING_EXECUTION,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = EARLY_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        val remoteInput3 = BuySellOrder(
            id = ORDER_ID_2,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = EARLY_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        val remoteInput4 = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.PENDING_CONFIRMATION,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput1,
                    remoteInput2,
                    remoteInput3,
                    remoteInput4
                )
            )
        )

        // If and when we encounter this situation, we will take the one that was submitted last
        val expectedResult = remoteInput4.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    fun `remote overrides local`() {
        // We have a local confirmed buy, but it has been completed on another device
        // We should have no local state

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.FINISHED,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput
                )
            )
        )

        val expectedResult = null

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    fun `remote pending overrides local`() {
        // We have a local confirmed buy, but it has been completed on another device
        // We should have no local state

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.PENDING_EXECUTION,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput
                )
            )
        )

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    fun `remote awaiting funds overrides local pending confirmation`() {
        // We have a local confirmed buy, but it has been completed on another device
        // We should have no local state

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.PENDING_CONFIRMATION,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput
                )
            )
        )

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    fun `remote awaiting funds overrides local initialised`() {
        // We have an unconfirmed local buy, but another has been set up on another
        // device and is awaiting funds
        // We should use the remote

        val localInput = SimpleBuyState(
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.INITIALISED,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = MIDDLE_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(localInput)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput
                )
            )
        )

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .await()

        verify(remoteState, never()).getBuyOrder(EXPECTED_ORDER_ID)
        validateFinalState(expectedResult)
    }

    // Test lightweight sync
    @Test
    fun `lightweight, no local state`() {

        whenever(serializer.fetch()).thenReturn(null)

        val expectedResult = null

        subject.lightweightSync()
            .test()
            .assertComplete()
            .await()

        validateFinalStateLightweight(expectedResult)
    }

    @Test
    fun `lightweight, local state initialised`() {

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.INITIALISED,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        whenever(serializer.fetch()).thenReturn(localInput)

        val expectedResult = localInput

        subject.lightweightSync()
            .test()
            .assertComplete()
            .await()

        validateFinalStateLightweight(expectedResult)
    }

    @Test
    fun `lightweight, local state awaiting funds, remote awaiting funds`() {

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            selectedPaymentMethod = SelectedPaymentMethod(
                PaymentMethod.FUNDS_PAYMENT_ID,
                paymentMethodType = PaymentMethodType.FUNDS, isEligible = true
            ),
            currentScreen = FlowScreen.CHECKOUT
        )

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.FUNDS_PAYMENT_ID,
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.lightweightSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    fun `lightweight, local state awaiting funds, remote pending`() {

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE,
            currentScreen = FlowScreen.CHECKOUT
        )

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.PENDING_EXECUTION,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.lightweightSync()
            .test()
            .assertComplete()
            .await()

        validateFinalState(expectedResult)
    }

    @Test
    fun `lightweight, local state awaiting funds, remote finished`() {

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE,
            currentScreen = FlowScreen.CHECKOUT
        )

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.FINISHED,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))

        val expectedResult = null

        subject.lightweightSync()
            .test()
            .assertComplete()
            .await()

        validateFinalStateLightweight(expectedResult)
    }

    @Test
    fun `lightweight, local state awaiting funds, remote canceled`() {

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE,
            currentScreen = FlowScreen.CHECKOUT
        )

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.CANCELED,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))

        val expectedResult = null

        subject.lightweightSync()
            .test()
            .assertComplete()
            .await()

        validateFinalStateLightweight(expectedResult)
    }

    @Test
    fun `lightweight, local state awaiting funds, remote failed`() {

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoAsset = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE,
            currentScreen = FlowScreen.CHECKOUT
        )

        val remoteInput = BuySellOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.zero(CryptoCurrency.BTC),
            state = OrderState.FAILED,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.FUNDS,
            type = OrderType.BUY,
            depositPaymentId = ""
        )

        whenever(serializer.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))

        val expectedResult = null

        subject.lightweightSync()
            .test()
            .assertComplete()
            .await()

        validateFinalStateLightweight(expectedResult)
    }

    private fun validateFinalState(expected: SimpleBuyState?) {
        if (expected != null) {
            argumentCaptor<SimpleBuyState>().apply {
                verify(serializer).update(capture())
                assertEquals(expected, firstValue)
            }
        } else {
            verify(serializer, atLeastOnce()).clear()
        }

        verify(serializer, atLeastOnce()).fetch()
        verifyNoMoreInteractions(serializer)
    }

    private fun validateFinalStateLightweight(expected: SimpleBuyState?) {
        // Lightweight sync only clears state, if remote is complete, so never updates
        if (expected == null) {
            verify(serializer, atLeastOnce()).clear()
        }

        verify(serializer, atLeastOnce()).fetch()
        verifyNoMoreInteractions(serializer)
    }

    companion object {
        private const val EXPECTED_ORDER_ID = "12345-12345-1234-1234567890"
        private const val ORDER_ID_1 = "11111-11111-1111-1111111111"
        private const val ORDER_ID_2 = "22222-22222-2222-2222222222"
        private const val ORDER_ID_3 = "33333-33333-3333-3333333333"
        private const val ORDER_ID_4 = "44444-44444-4444-4444444444"

        @SuppressLint("SimpleDateFormat")
        private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        private val EARLY_ORDER_DATE = format.parse("2020-02-21T10:36:40.245+0000")
        private val MIDDLE_ORDER_DATE = format.parse("2020-02-21T10:46:40.245+0000")
        private val LAST_ORDER_DATE = format.parse("2020-02-21T10:56:40.245+0000")
    }
}
