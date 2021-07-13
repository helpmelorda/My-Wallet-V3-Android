package com.blockchain.sunriver

import com.blockchain.sunriver.derivation.deriveXlmAccountKeyPair
import com.blockchain.wallet.SeedAccess
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

internal class XlmSecretAccess(private val seedAccess: SeedAccess) {

    /**
     * Searches for an account that matches the public.
     */
    fun getPrivate(
        forPublic: HorizonKeyPair.Public,
        secondPassword: String?
    ): Single<HorizonKeyPair.Private> =
        seedAccess
            .seed(secondPassword)
            .observeOn(Schedulers.computation())
            .flatMap { seed ->
                for (i in 0..20) {
                    val keyPair = deriveXlmAccountKeyPair(seed.hdSeed, i)
                    if (keyPair.accountId == forPublic.accountId) {
                        return@flatMap Maybe.just(keyPair)
                    }
                }
                Maybe.empty()
            }.toSingle()
}
