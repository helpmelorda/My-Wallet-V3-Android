package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class FiatValueFromMinorTests {

    @Test
    fun `from minor GBP`() {
        FiatValue.fromMinor(
            "GBP",
            123
        ) `should be equal to` 1.23.gbp()
    }

    @Test
    fun `from minor GBP 0 end`() {
        FiatValue.fromMinor(
            "GBP",
            200
        ) `should be equal to` 2.gbp()
    }

    @Test
    fun `from minor GBP scale is set to 2`() {
        FiatValue.fromMinor(
            "GBP",
            200
        ).toBigDecimal().scale() `should be` 2
    }

    @Test
    fun `from minor USD`() {
        FiatValue.fromMinor(
            "USD",
            456
        ) `should be equal to` 4.56.usd()
    }

    @Test
    fun `from minor JPY`() {
        FiatValue.fromMinor(
            "JPY",
            456
        ) `should be equal to` 456.jpy()
    }

    @Test
    fun `from minor JPY scale is set to 0`() {
        FiatValue.fromMinor(
            "JPY",
            200
        ).toBigDecimal().scale() `should be` 0
    }
}
