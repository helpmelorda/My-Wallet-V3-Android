package piuk.blockchain.android.ui.kyc.countryselection.util

import org.amshove.kluent.`should be equal to`
import org.junit.Test

class AcronymTest {

    @Test
    fun `correctly abbreviates country`() {
        "United Kingdom".acronym() `should be equal to` "UK"
    }

    @Test
    fun `ignores trailing whitespace`() {
        "United Kingdom ".acronym() `should be equal to` "UK"
    }

    @Test
    fun `ignores leading whitespace`() {
        " United Kingdom".acronym() `should be equal to` "UK"
    }

    @Test
    fun `ignores extra middle whitespace`() {
        "United  Kingdom".acronym() `should be equal to` "UK"
    }

    @Test
    fun `already an acronym`() {
        "UK".acronym() `should be equal to` "UK"
    }
}