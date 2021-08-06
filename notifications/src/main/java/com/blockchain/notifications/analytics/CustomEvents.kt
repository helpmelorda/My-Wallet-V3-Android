package com.blockchain.notifications.analytics

@Deprecated("Use com.blockchain.notifications.analytics.Analytics instead")
fun pairingEvent(pairingMethod: PairingMethod) =
    LoggingEvent("Wallet Pairing", mapOf("Pairing method" to pairingMethod.name))

@Suppress("UNUSED_PARAMETER")
@Deprecated("Use com.blockchain.notifications.analytics.Analytics instead")
enum class PairingMethod(name: String) {
    MANUAL("Manual"),
    QR_CODE("Qr code"),
    REVERSE("Reverse")
}

@Suppress("UNUSED_PARAMETER")
@Deprecated("Use com.blockchain.notifications.analytics.Analytics instead")
enum class AddressType(name: String) {
    PRIVATE_KEY("Private key")
}

@Deprecated("Use com.blockchain.notifications.analytics.Analytics instead")
fun appLaunchEvent(playServicesFound: Boolean) =
    LoggingEvent("App Launched",
        mapOf("Play Services found" to playServicesFound))

@Deprecated("Use com.blockchain.notifications.analytics.Analytics instead")
fun secondPasswordEvent(secondPasswordEnabled: Boolean) =
    LoggingEvent("Second password event",
        mapOf("Second password enabled" to secondPasswordEnabled))

@Deprecated("Use com.blockchain.notifications.analytics.Analytics instead")
fun launcherShortcutEvent(type: String) =
    LoggingEvent("Launcher Shortcut", mapOf("Launcher Shortcut used" to type))

@Deprecated("Use com.blockchain.notifications.analytics.Analytics instead")
fun walletUpgradeEvent(successful: Boolean) =
    LoggingEvent("Wallet Upgraded", mapOf("Successful" to successful))