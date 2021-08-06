package com.blockchain.nabu.service

import io.reactivex.rxjava3.core.Completable

interface TierUpdater {

    /**
     * Set the tier the user wants to apply for.
     */
    fun setUserTier(tier: Int): Completable
}
