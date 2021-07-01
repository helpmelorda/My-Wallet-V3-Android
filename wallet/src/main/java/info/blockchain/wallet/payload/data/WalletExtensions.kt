package info.blockchain.wallet.payload.data

fun Wallet.nonArchivedImportedAddressStrings() =
    nonArchivedImportedAddresses().distinct()

private fun Wallet.nonArchivedImportedAddresses() =
    importedAddressList
        .filterNot { it.isArchived }.map { it.address }

fun Wallet.activeXpubs() =
    walletBody?.activeXpubs ?: emptyList()
