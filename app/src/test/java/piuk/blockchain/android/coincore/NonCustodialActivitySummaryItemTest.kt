package piuk.blockchain.android.coincore

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be equal to`
import org.junit.Test

class NonCustodialActivitySummaryItemTest {

    @Test
    fun `ensure not equal when compared to different type`() {

        val activityItem = TestNonCustodialSummaryItem()
        val objectToCompare = Any()

        activityItem.toString() `should not be equal to` objectToCompare.toString()
        activityItem.hashCode() `should not be equal to` objectToCompare.hashCode()
        activityItem `should not be equal to` objectToCompare
    }

    @Test
    fun `ensure equals, hashCode and toString work correctly with subtly different objects`() {

        val itemOne = TestNonCustodialSummaryItem()
            .apply { note = "note 1" }

        val itemTwo = TestNonCustodialSummaryItem()
            .apply { note = "note 2" }

        itemOne.toString() `should not be equal to` itemTwo.toString()
        itemOne.hashCode() `should not be equal to` itemTwo.hashCode()
        itemOne `should not be equal to` itemTwo
    }

    @Test
    fun `ensure equals, hashCode and toString work correctly with identical objects`() {

        val itemOne = TestNonCustodialSummaryItem()
            .apply { note = "note" }

        val itemTwo = TestNonCustodialSummaryItem()
            .apply { note = "note" }

        itemOne.toString() `should be equal to` itemTwo.toString()
        itemOne.hashCode() `should be equal to` itemTwo.hashCode()
        itemOne `should be equal to` itemTwo
    }
}
