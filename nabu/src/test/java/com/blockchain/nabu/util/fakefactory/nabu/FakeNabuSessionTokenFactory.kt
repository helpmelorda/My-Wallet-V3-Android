package com.blockchain.nabu.util.fakefactory.nabu

import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse

object FakeNabuSessionTokenFactory {
    val any = NabuSessionTokenResponse(
        "ID",
        "USER_ID",
        "TOKEN",
        true,
        "EXPIRES_AT",
        "INSERTED_AT",
        "UPDATED_AT"
    )
}