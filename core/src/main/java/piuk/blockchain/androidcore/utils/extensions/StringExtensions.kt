package piuk.blockchain.androidcore.utils.extensions

private const val REGEX_UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
fun String.isValidGuid() = this.matches(REGEX_UUID.toRegex())

const val PIN_LENGTH = 4
fun String.isValidPin(): Boolean = (this != "0000" && this.length == PIN_LENGTH)