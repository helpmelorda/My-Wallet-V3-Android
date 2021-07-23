package com.blockchain.nabu.util.fakefactory

import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.UserState

object FakeNabuUserMotherFactory {
    val satoshi = NabuUser(
        firstName = "Satoshi",
        lastName = "Nakamoto",
        email = "satoshi@btc.com",
        emailVerified = false,
        dob = null,
        mobile = "",
        mobileVerified = false,
        address = FakeAddressFactory.any,
        state = UserState.None,
        kycState = KycState.None,
        insertedAt = "",
        updatedAt = ""
    )
}