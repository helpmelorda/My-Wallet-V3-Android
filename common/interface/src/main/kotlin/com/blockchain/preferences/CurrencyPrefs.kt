package com.blockchain.preferences

interface CurrencyPrefs {
    var selectedFiatCurrency: String
    val defaultFiatCurrency: String
}
