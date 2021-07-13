package info.blockchain.balance

import org.amshove.kluent.`should be equal to`
import org.junit.Test

class FiatValueValueMinorTests {

    @Test
    fun `value minor gbp`() {
        1.2.gbp().toBigInteger() `should be equal to` 120.toBigInteger()
    }

    @Test
    fun `value minor gbp 2 dp`() {
        2.21.gbp().toBigInteger() `should be equal to` 221.toBigInteger()
    }

    @Test
    fun `value minor yen`() {
        543.jpy().toBigInteger() `should be equal to` 543.toBigInteger()
    }
}
