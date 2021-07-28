package com.blockchain.nabu.util.fakefactory.nabu

import com.blockchain.nabu.models.responses.nabu.Address

object FakeAddressFactory {
    val any = Address(
        line1 = "132 Seafield Street",
        line2 = null,
        city = "Llwynmawr",
        state = null,
        postCode = "LL20 3WR",
        countryCode = ""
    )
}