package com.blockchain.preferences

interface SimpleBuyPrefs {
    fun simpleBuyState(): String?
    fun updateSimpleBuyState(simpleBuyState: String)
    fun clearBuyState()
    fun cardState(): String?
    fun updateCardState(cardState: String)
    fun clearCardState()
    fun updateSupportedCards(cardTypes: String)
    fun getSupportedCardTypes(): String?
    var hasCompletedAtLeastOneBuy: Boolean

    var addCardInfoDismissed: Boolean
}