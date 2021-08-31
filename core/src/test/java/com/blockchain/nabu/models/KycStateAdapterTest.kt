package com.blockchain.nabu.models

import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.KycStateAdapter
import com.squareup.moshi.JsonDataException
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.junit.Test

class KycStateAdapterTest {

    @Test
    fun `from none`() {
        KycStateAdapter().fromJson("NONE") `should be equal to` KycState.None
    }

    @Test
    fun `from pending`() {
        KycStateAdapter().fromJson("PENDING") `should be equal to` KycState.Pending
    }

    @Test
    fun `from under review`() {
        KycStateAdapter().fromJson("UNDER_REVIEW") `should be equal to` KycState.UnderReview
    }

    @Test
    fun `from rejected`() {
        KycStateAdapter().fromJson("REJECTED") `should be equal to` KycState.Rejected
    }

    @Test
    fun `from expired`() {
        KycStateAdapter().fromJson("EXPIRED") `should be equal to` KycState.Expired
    }

    @Test
    fun `from verified`() {
        KycStateAdapter().fromJson("VERIFIED") `should be equal to` KycState.Verified
    }

    @Test
    fun `from unknown should throw exception`() {
        {
            KycStateAdapter().fromJson("malformed")
        } `should throw` JsonDataException::class
    }

    @Test
    fun `to none`() {
        KycStateAdapter().toJson(KycState.None) `should be equal to` "NONE"
    }

    @Test
    fun `to pending`() {
        KycStateAdapter().toJson(KycState.Pending) `should be equal to` "PENDING"
    }

    @Test
    fun `to under review`() {
        KycStateAdapter().toJson(KycState.UnderReview) `should be equal to` "UNDER_REVIEW"
    }

    @Test
    fun `to rejected`() {
        KycStateAdapter().toJson(KycState.Rejected) `should be equal to` "REJECTED"
    }

    @Test
    fun `to expired`() {
        KycStateAdapter().toJson(KycState.Expired) `should be equal to` "EXPIRED"
    }

    @Test
    fun `to verified`() {
        KycStateAdapter().toJson(KycState.Verified) `should be equal to` "VERIFIED"
    }
}