package info.blockchain.balance

import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

data class CryptoValue(
    val currency: AssetInfo,
    private val amount: BigInteger // Amount in the minor unit of the currency, Satoshi/Wei for example.
) : Money() {

    override val maxDecimalPlaces: Int = currency.precisionDp

    override val userDecimalPlaces: Int = DISPLAY_DP

    override val currencyCode = currency.ticker
    override val symbol = currency.ticker

    override fun toStringWithSymbol() = formatWithUnit(Locale.getDefault())

    override fun toStringWithoutSymbol() = format(Locale.getDefault())

    override fun toNetworkString(): String = format(Locale.US).removeComma()

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    override fun toBigDecimal(): BigDecimal = amount.toBigDecimal().movePointLeft(currency.precisionDp)

    override fun toBigInteger(): BigInteger = amount
    override fun toFloat(): Float = toBigDecimal().toFloat()

    override val isPositive: Boolean get() = amount.signum() == 1

    override val isZero: Boolean get() = amount.signum() == 0

    companion object {
        const val DISPLAY_DP = 8

        fun zero(asset: AssetInfo) =
            CryptoValue(asset, BigInteger.ZERO)

        fun fromMajor(
            currency: AssetInfo,
            major: BigDecimal
        ) = CryptoValue(currency, major.movePointRight(currency.precisionDp).toBigInteger())

        fun fromMinor(
            currency: AssetInfo,
            minor: BigDecimal
        ) = CryptoValue(currency, minor.toBigInteger())

        fun fromMinor(
            currency: AssetInfo,
            minor: BigInteger
        ) = CryptoValue(currency, minor)
    }

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    fun toMajorUnitDouble() = toBigDecimal().toDouble()

    override fun toZero(): CryptoValue = zero(currency)

    fun abs(): CryptoValue = CryptoValue(currency, amount.abs())

    override fun add(other: Money): CryptoValue {
        require(other is CryptoValue)
        return CryptoValue(currency, amount + other.amount)
    }

    override fun subtract(other: Money): CryptoValue {
        require(other is CryptoValue)
        return CryptoValue(currency, amount - other.amount)
    }

    override fun compare(other: Money): Int {
        require(other is CryptoValue)
        return amount.compareTo(other.amount)
    }

    override fun division(other: Money): Money {
        require(other is CryptoValue)
        return CryptoValue(currency, amount / other.amount)
    }

    override fun ensureComparable(operation: String, other: Money) {
        if (other is CryptoValue) {
            if (currency != other.currency) {
                throw ValueTypeMismatchException(operation, currencyCode, other.currencyCode)
            }
        } else {
            throw ValueTypeMismatchException(operation, currencyCode, other.currencyCode)
        }
    }
}
