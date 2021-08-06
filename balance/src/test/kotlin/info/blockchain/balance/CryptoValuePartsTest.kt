package info.blockchain.balance

import org.amshove.kluent.`should be equal to`
import org.junit.Test
import java.util.Locale

class CryptoValuePartsTest {

    @Test
    fun `extract BTC parts in UK`() {
        Locale.setDefault(Locale.UK)

        1.2.bitcoin()
            .toStringParts().apply {
                symbol `should be equal to` "BTC"
                major `should be equal to` "1"
                minor `should be equal to` "2"
                majorAndMinor`should be equal to` "1.2"
            }
    }

    @Test
    fun `extract ETH parts in US`() {
        Locale.setDefault(Locale.US)

        9.89.ether()
            .toStringParts().apply {
                symbol `should be equal to` "ETH"
                major `should be equal to` "9"
                minor `should be equal to` "89"
                majorAndMinor`should be equal to` "9.89"
            }
    }

    @Test
    fun `extract max DP ETHER parts in UK`() {
        Locale.setDefault(Locale.UK)

        5.12345678.ether()
            .toStringParts().apply {
                symbol `should be equal to` "ETH"
                major `should be equal to` "5"
                minor `should be equal to` "12345678"
                majorAndMinor`should be equal to` "5.12345678"
            }
    }

    @Test
    fun `extract parts from large number in UK`() {
        Locale.setDefault(Locale.UK)

        5345678.ether()
            .toStringParts().apply {
                symbol `should be equal to` "ETH"
                major `should be equal to` "5,345,678"
                minor `should be equal to` "0"
                majorAndMinor`should be equal to` "5,345,678.0"
            }
    }

    @Test
    fun `extract parts from large number in France`() {
        Locale.setDefault(Locale.FRANCE)

        5345678.987.ether()
            .toStringParts().apply {
                symbol `should be equal to` "ETH"
                major `should be equal to` "5 345 678"
                minor `should be equal to` "987"
                majorAndMinor`should be equal to` "5 345 678,987"
            }
    }

    @Test
    fun `extract parts from large number in Italy`() {
        Locale.setDefault(Locale.ITALY)

        9345678.987.ether()
            .toStringParts().apply {
                symbol `should be equal to` "ETH"
                major `should be equal to` "9.345.678"
                minor `should be equal to` "987"
                majorAndMinor`should be equal to` "9.345.678,987"
            }
    }
}
