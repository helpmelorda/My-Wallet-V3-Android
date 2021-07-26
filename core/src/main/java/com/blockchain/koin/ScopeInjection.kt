package com.blockchain.koin

import android.content.ComponentCallbacks
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

inline fun <reified T : Any> ComponentCallbacks.scopedInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = payloadScope.inject<T>(qualifier, LazyThreadSafetyMode.SYNCHRONIZED, parameters)

inline fun <reified T> KoinComponent.scopedInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> =
    lazy(LazyThreadSafetyMode.SYNCHRONIZED) { payloadScope.get(qualifier, parameters) }