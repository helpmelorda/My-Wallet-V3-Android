package info.blockchain.balance

import org.amshove.kluent.`should be equal to`
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class XlmCryptoValueTest {

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `format zero`() {
        CryptoValue.zero(CryptoCurrency.XLM)
            .toStringWithSymbol() `should be equal to` "0 XLM"
    }

    @Test
    fun `format 1`() {
        CryptoValue.fromMajor(CryptoCurrency.XLM, BigDecimal.ONE)
            .toStringWithSymbol() `should be equal to` "1.0 XLM"
    }

    @Test
    fun `create via constructor`() {
        CryptoValue(CryptoCurrency.XLM, 98765432.toBigInteger()) `should be equal to` 9.8765432.lumens()
    }

    @Test
    fun `format fractions`() {
        0.1.lumens().toStringWithSymbol() `should be equal to` "0.1 XLM"
        0.01.lumens().toStringWithSymbol() `should be equal to` "0.01 XLM"
        0.001.lumens().toStringWithSymbol() `should be equal to` "0.001 XLM"
        0.0001.lumens().toStringWithSymbol() `should be equal to` "0.0001 XLM"
        0.00001.lumens().toStringWithSymbol() `should be equal to` "0.00001 XLM"
        0.000001.lumens().toStringWithSymbol() `should be equal to` "0.000001 XLM"
        0.0000001.lumens().toStringWithSymbol() `should be equal to` "0.0000001 XLM"
    }

    @Test
    fun `format in French locale`() {
        Locale.setDefault(Locale.FRANCE)
        1234.56789.lumens().toStringWithSymbol() `should be equal to` "1 234,56789 XLM"
    }
}
