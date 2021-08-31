package com.blockchain.nabu.util.fakefactory.nabu

import com.blockchain.nabu.models.responses.nabu.NabuCountryResponse

object FakeNabuCountryFactory {
    val germany = NabuCountryResponse(code = "DE", name = "Germany", scopes = emptyList(), regions = emptyList())
    val uk = NabuCountryResponse(code = "UK", name = "United Kingdom", scopes = emptyList(), regions = emptyList())
    val france = NabuCountryResponse(code = "FR", name = "France", scopes = emptyList(), regions = emptyList())

    val list = arrayListOf<NabuCountryResponse>(germany, uk, france)
}