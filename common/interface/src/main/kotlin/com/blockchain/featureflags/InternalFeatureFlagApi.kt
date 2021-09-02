package com.blockchain.featureflags

interface InternalFeatureFlagApi {
    fun isFeatureEnabled(gatedFeature: GatedFeature): Boolean
    fun enable(gatedFeature: GatedFeature)
    fun disable(gatedFeature: GatedFeature)
    fun disableAll()
    fun getAll(): Map<GatedFeature, Boolean>
}

enum class GatedFeature(
    val readableName: String,
    val enabledForCompanyInternalBuild: Boolean = false
) {
    ADD_SUB_WALLET_ADDRESSES("Create BTC sub-wallets"),
    ACCOUNT_RECOVERY("Enable New Account Recovery Flow"),
    FULL_SCREEN_TXS("Enable full screen tx flow", true),
    NEW_ONBOARDING("New onboarding design"),
    NEW_SPLIT_DASHBOARD("Enable New Dashboard")
}