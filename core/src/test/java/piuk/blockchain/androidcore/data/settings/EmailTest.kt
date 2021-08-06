package piuk.blockchain.androidcore.data.settings

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be equal to`
import org.junit.Test

class EmailTest {

    @Test
    fun `assert equals`() {
        Email("abc@def.com", isVerified = false) `should be equal to` Email("abc@def.com", isVerified = false)
    }

    @Test
    fun `assert not equals by verified`() {
        Email("abc@def.com", isVerified = false) `should not be equal to` Email("abc@def.com", isVerified = true)
    }

    @Test
    fun `assert not equals by address`() {
        Email("abc@def.com", isVerified = false) `should not be equal to` Email("def@abc.com", isVerified = false)
    }
}
