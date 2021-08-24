package com.blockchain.preferences

interface DashboardPrefs {
    var swapIntroCompleted: Boolean
    var isOnBoardingComplete: Boolean
    var isCustodialIntroSeen: Boolean
    var remainingSendsWithoutBackup: Int

    var dashboardAssetOrder: List<String>
}