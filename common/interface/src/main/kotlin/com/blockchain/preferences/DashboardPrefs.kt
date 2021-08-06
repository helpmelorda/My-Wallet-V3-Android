package com.blockchain.preferences

interface DashboardPrefs {
    var swapIntroCompleted: Boolean
    var isOnboardingComplete: Boolean
    var isCustodialIntroSeen: Boolean
    var remainingSendsWithoutBackup: Int

    var dashboardAssetOrder: List<String>
}