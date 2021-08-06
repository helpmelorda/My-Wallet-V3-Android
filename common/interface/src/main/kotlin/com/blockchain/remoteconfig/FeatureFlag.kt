package com.blockchain.remoteconfig

import io.reactivex.rxjava3.core.Single

interface FeatureFlag {

    val enabled: Single<Boolean>
}
