package info.blockchain.balance

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be equal to`
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FiatValueFromStringTest {

    @Before
    fun setUs() {
        Locale.setDefault(Locale.US)
    }

    @Before
    fun clearOther() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `empty string`() {
        FiatValue.fromMajorOrZero("USD", "") `should be equal to` 0.usd()
    }

    @Test
    fun `bad string`() {
        FiatValue.fromMajorOrZero("GBP", "a") `should be equal to` 0.gbp()
    }

    @Test
    fun `one dollar`() {
        FiatValue.fromMajorOrZero("USD", "1") `should be equal to` 1.usd()
    }

    @Test
    fun `2 dp dollars`() {
        FiatValue.fromMajorOrZero("USD", "1.23") `should be equal to` 1.23.usd()
    }

    @Test
    fun `French input`() {
        Locale.setDefault(Locale.FRANCE)
        FiatValue.fromMajorOrZero("EUR", "1,12") `should be equal to` 1.12.eur()
    }

    @Test
    fun `UK input`() {
        Locale.setDefault(Locale.UK)
        FiatValue.fromMajorOrZero("EUR", "1,12") `should be equal to` 112.eur()
    }

    @Test
    fun `Override locale input`() {
        Locale.setDefault(Locale.UK)
        FiatValue.fromMajorOrZero("EUR", "1,12", Locale.FRANCE) `should be equal to` 1.12.eur()
    }

    @Test
    fun `rounds down below midway`() {
        FiatValue.fromMajorOrZero("GBP", "1.2349") `should be equal to` 1.23.gbp()
    }

    @Test
    fun `rounds down below midway - JPY`() {
        FiatValue.fromMajorOrZero("JPY", "123.49") `should be equal to` 123.jpy()
    }

    @Test
    fun `rounds down at midway - JPY`() {
        FiatValue.fromMajorOrZero("JPY", "123.5") `should be equal to` 123.jpy()
    }

    @Test
    fun `very large number test - ensures does not go via double`() {
        val major = "1234567890123456.78"
        val largeNumberBigDecimal = major.toBigDecimal()
        FiatValue.fromMajorOrZero("USD", major) `should be equal to`
            largeNumberBigDecimal.usd()
        FiatValue.fromMajorOrZero("USD", major) `should not be equal to`
            largeNumberBigDecimal.toDouble().usd()
    }
}
