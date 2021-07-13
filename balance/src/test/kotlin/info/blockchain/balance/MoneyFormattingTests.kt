package info.blockchain.balance

import org.amshove.kluent.`should be equal to`
import org.junit.Test
import java.util.Locale

class MoneyFormattingTests {

    @Test
    fun `FiatValue formatted as Money`() {
        Locale.setDefault(Locale.CANADA)

        val money: Money = 1.cad()
        money.symbol `should be equal to` "$"
        money.toStringWithSymbol() `should be equal to` "$1.00"
        money.toStringWithoutSymbol() `should be equal to` "1.00"
    }

    @Test
    fun `FiatValue formatted as Money with rounding`() {
        Locale.setDefault(Locale.CANADA)
        val money: Money = 1.695.cad()

        money.toStringWithSymbol() `should be equal to` "$1.69"
        money.toStringWithoutSymbol() `should be equal to` "1.69"
    }

    @Test
    fun `FiatValue JPY formatted as Money`() {
        Locale.setDefault(Locale.US)

        val money: Money = 123.jpy()
        money.symbol `should be equal to` "JPY"
        money.toStringWithSymbol() `should be equal to` "JPY123"
        money.toStringWithoutSymbol() `should be equal to` "123"
    }

    @Test
    fun `CryptoValue formatted as Money`() {
        Locale.setDefault(Locale.US)

        val money: Money = 1.23.bitcoin()
        money.symbol `should be equal to` "BTC"
        money.toStringWithSymbol() `should be equal to` "1.23 BTC"
        money.toStringWithoutSymbol() `should be equal to` "1.23"
    }

    @Test
    fun `CryptoValue Ether formatted as Money`() {
        Locale.setDefault(Locale.FRANCE)

        val money: Money = 1.23.ether()
        money.symbol `should be equal to` "ETH"
        money.toStringWithSymbol() `should be equal to` "1,23 ETH"
        money.toStringWithoutSymbol() `should be equal to` "1,23"
    }
}
