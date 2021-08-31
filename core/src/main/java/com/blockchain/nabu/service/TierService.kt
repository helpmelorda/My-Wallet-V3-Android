package com.blockchain.nabu.service

import com.blockchain.nabu.models.responses.nabu.KycTiers
import io.reactivex.rxjava3.core.Single

interface TierService {

    fun tiers(): Single<KycTiers>
}
