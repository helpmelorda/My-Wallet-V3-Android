package com.blockchain.nabu

import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Single

class DisabledFeatureFlag(
    override val enabled: Single<Boolean> = Single.just(false)
) : FeatureFlag