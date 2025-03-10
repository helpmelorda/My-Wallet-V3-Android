package com.blockchain.utils

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.junit.Test
import java.math.BigDecimal
import java.text.ParseException
import java.util.Locale

class BigDecimalParsingTest {

    @Test
    fun `parse empty`() {
        { "".parseBigDecimal(Locale.US) } `should throw` ParseException::class
    }

    @Test
    fun `tryParse empty`() {
        "".tryParseBigDecimal(Locale.US) `should be equal to` null
    }

    @Test
    fun `tryParse garbage`() {
        "xyz".tryParseBigDecimal(Locale.US) `should be equal to` null
    }

    @Test
    fun `tryParse integer`() {
        "1".tryParseBigDecimal(Locale.US) `should be equal to` BigDecimal.ONE
    }

    @Test
    fun `tryParse fraction`() {
        "1.2".tryParseBigDecimal(Locale.US) `should be equal to` 1.2.toBigDecimal()
    }

    @Test
    fun `tryParse long with comma grouping separators`() {
        "1,000.23".tryParseBigDecimal(Locale.US) `should be equal to` 1000.23.toBigDecimal()
    }

    @Test
    fun `tryParse fraction with comma`() {
        "1,2".tryParseBigDecimal(Locale.FRANCE) `should be equal to` 1.2.toBigDecimal()
    }

    @Test
    fun `tryParse number with units`() {
        "$1.23".tryParseBigDecimal(Locale.US) `should be equal to` 1.23.toBigDecimal()
    }

    @Test
    fun `tryParse long with space grouping separators`() {
        "1 000,23".tryParseBigDecimal(Locale.FRANCE) `should be equal to` 1000.23.toBigDecimal()
    }

    @Test
    fun `tryParse long with period grouping separators`() {
        "1.000,23".tryParseBigDecimal(Locale.ITALIAN) `should be equal to` 1000.23.toBigDecimal()
    }

    @Test
    fun `parse long with period grouping separators`() {
        "1.000,23".parseBigDecimal(Locale.ITALIAN) `should be equal to` 1000.23.toBigDecimal()
    }

    @Test
    fun `parse garbage`() {
        { "xyz".parseBigDecimal(Locale.US) } `should throw` ParseException::class
    }

    @Test
    fun `UK input`() {
        "1,123".tryParseBigDecimal(Locale.UK) `should be equal to` 1123.toBigDecimal()
    }
}
