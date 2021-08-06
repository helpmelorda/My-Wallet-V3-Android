package info.blockchain.balance

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.`with message`
import org.junit.Test

class CryptoValueSubtractionTest {

    @Test
    fun `can subtract BTC`() {
        100.bitcoin() - 30.bitcoin() `should be equal to` 70.bitcoin()
    }

    @Test
    fun `can subtract BCH`() {
        200.123.bitcoinCash() - 100.003.bitcoinCash() `should be equal to` 100.12.bitcoinCash()
    }

    @Test
    fun `can subtract ETHER`() {
        1.23.ether() - 2.345.ether() `should be equal to` (-1.115).ether()
    }

    @Test
    fun `can't subtract ETHER and BTC`() {
        {
            1.23.ether() - 2.345.bitcoin()
        } `should throw` ValueTypeMismatchException::class `with message` "Can't subtract ETH and BTC"
    }

    @Test
    fun `can't subtract BTC and BCH`() {
        {
            1.bitcoin() - 1.bitcoinCash()
        } `should throw` ValueTypeMismatchException::class `with message` "Can't subtract BTC and BCH"
    }
}
