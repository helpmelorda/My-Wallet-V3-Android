package com.blockchain.core.price

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.ValueTypeMismatchException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

sealed class ExchangeRate {
    abstract val rate: BigDecimal

    abstract fun convert(value: Money, round: Boolean = true): Money
    abstract fun price(): Money
    abstract fun inverse(roundingMode: RoundingMode = RoundingMode.HALF_UP, scale: Int = -1): ExchangeRate

    class CryptoToCrypto(
        val from: AssetInfo,
        val to: AssetInfo,
        override val rate: BigDecimal
    ) : ExchangeRate() {
        fun applyRate(cryptoValue: CryptoValue): CryptoValue {
            validateCurrency(from, cryptoValue.currency)
            return CryptoValue.fromMajor(
                to,
                rate.multiply(cryptoValue.toBigDecimal())
            )
        }

        override fun convert(value: Money, round: Boolean): Money =
            applyRate(value as CryptoValue)

        override fun price(): Money =
            CryptoValue.fromMajor(to, rate)

        override fun inverse(roundingMode: RoundingMode, scale: Int) =
            CryptoToCrypto(
                to,
                from,
                BigDecimal.ONE.divide(
                    rate,
                    if (scale == -1) from.precisionDp else scale,
                    roundingMode
                ).stripTrailingZeros()
            )
    }

    data class CryptoToFiat(
        val from: AssetInfo,
        val to: String,
        override val rate: BigDecimal
    ) : ExchangeRate() {
        fun applyRate(cryptoValue: CryptoValue, round: Boolean = false): FiatValue {
            validateCurrency(from, cryptoValue.currency)
            return FiatValue.fromMajor(
                currencyCode = to,
                major = rate.multiply(cryptoValue.toBigDecimal()),
                round = round
            )
        }

        override fun convert(value: Money, round: Boolean): Money =
            applyRate(value as CryptoValue, round)

        override fun price(): Money =
            FiatValue.fromMajor(to, rate)

        override fun inverse(roundingMode: RoundingMode, scale: Int) =
            FiatToCrypto(
                to,
                from,
                BigDecimal.ONE.divide(
                    rate,
                    if (scale == -1) from.precisionDp else scale,
                    roundingMode
                ).stripTrailingZeros()
            )
    }

    class FiatToCrypto(
        val from: String,
        val to: AssetInfo,
        override val rate: BigDecimal
    ) : ExchangeRate() {
        fun applyRate(fiatValue: FiatValue): CryptoValue {
            validateCurrency(from, fiatValue.currencyCode)
            return CryptoValue.fromMajor(
                to,
                rate.multiply(fiatValue.toBigDecimal())
            )
        }

        override fun convert(value: Money, round: Boolean): Money =
            applyRate(value as FiatValue)

        override fun price(): Money =
            CryptoValue.fromMajor(to, rate)

        override fun inverse(roundingMode: RoundingMode, scale: Int) =
            CryptoToFiat(
                to,
                from,
                BigDecimal.ONE.divide(
                    rate,
                    if (scale == -1) Currency.getInstance(from).defaultFractionDigits else scale,
                    roundingMode
                ).stripTrailingZeros()
            )
    }

    class FiatToFiat(
        val from: String,
        val to: String,
        override val rate: BigDecimal
    ) : ExchangeRate() {
        fun applyRate(fiatValue: FiatValue): FiatValue {
            validateCurrency(from, fiatValue.currencyCode)
            return FiatValue.fromMajor(
                to,
                rate.multiply(fiatValue.toBigDecimal())
            )
        }

        override fun convert(value: Money, round: Boolean): Money =
            applyRate(value as FiatValue)

        override fun price(): Money =
            FiatValue.fromMajor(to, rate)

        override fun inverse(roundingMode: RoundingMode, scale: Int) =
            FiatToFiat(
                to,
                from,
                BigDecimal.ONE.divide(
                    rate,
                    if (scale == -1) Currency.getInstance(from).defaultFractionDigits else scale,
                    roundingMode
                ).stripTrailingZeros()
            )
    }

    companion object {
        private fun validateCurrency(expected: AssetInfo, got: AssetInfo) {
            if (expected != got)
                throw ValueTypeMismatchException("exchange", expected.ticker, got.ticker)
        }

        private fun validateCurrency(expected: String, got: String) {
            if (expected != got)
                throw ValueTypeMismatchException("exchange", expected, got)
        }
    }
}

operator fun CryptoValue?.times(rate: ExchangeRate.CryptoToCrypto?) =
    this?.let { rate?.applyRate(it) }

operator fun CryptoValue?.div(rate: ExchangeRate.CryptoToCrypto?) =
    this?.let { rate?.inverse()?.applyRate(it) }

operator fun FiatValue?.times(rate: ExchangeRate.FiatToCrypto?) =
    this?.let { rate?.applyRate(it) }

operator fun CryptoValue?.times(exchangeRate: ExchangeRate.CryptoToFiat?) =
    this?.let { exchangeRate?.applyRate(it) }

operator fun CryptoValue?.div(exchangeRate: ExchangeRate.FiatToCrypto?) =
    this?.let { exchangeRate?.inverse()?.applyRate(it) }

operator fun FiatValue?.div(exchangeRate: ExchangeRate.CryptoToFiat?) =
    this?.let { exchangeRate?.inverse()?.applyRate(it) }

fun ExchangeRate.hasSameSourceAndTarget(other: ExchangeRate): Boolean =
    when (this) {
        is ExchangeRate.CryptoToFiat -> (other as? ExchangeRate.CryptoToFiat)?.from == from && other.to == to
        is ExchangeRate.FiatToCrypto -> (other as? ExchangeRate.FiatToCrypto)?.from == from && other.to == to
        is ExchangeRate.FiatToFiat -> (other as? ExchangeRate.FiatToFiat)?.from == from && other.to == to
        is ExchangeRate.CryptoToCrypto -> (other as? ExchangeRate.CryptoToCrypto)?.from == from && other.to == to
    }

fun ExchangeRate.hasOppositeSourceAndTarget(other: ExchangeRate): Boolean =
    this.hasSameSourceAndTarget(other.inverse())
