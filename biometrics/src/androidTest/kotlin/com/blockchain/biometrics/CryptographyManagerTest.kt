package com.blockchain.biometrics

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.Charset

// NOTE: roboelectric runner needed for Base64 Android implementations
@Suppress("PrivatePropertyName")
@Config(sdk = [24])
@RunWith(RobolectricTestRunner::class)
class CryptographyManagerTest {

    private val subject = CryptographyManagerImpl()

    @Before
    fun setup() {
    }

    @Test
    fun generateCompositeKey() {
        val byteArrayData = "data".toByteArray()
        val base64EncodedData = "ZGF0YQ==\n"
        val expectedValue = "$base64EncodedData-_-$base64EncodedData"

        val result = subject.generateCompositeKey(byteArrayData, separator, byteArrayData)
        Assert.assertEquals(result, expectedValue)
    }

    @Test
    fun getIV() {
        // data-_-data
        val base64EncodedData = "ZGF0YQ==\n"
        val dataAndIV = "$base64EncodedData-_-$base64EncodedData"

        val result = subject.getDataAndIV(dataAndIV, separator)
        val data = String(subject.decodeFromBase64ToArray(result.first), Charset.forName("UTF-8"))
        Assert.assertEquals(data, "data")
        val iv = String(subject.decodeFromBase64ToArray(result.second), Charset.forName("UTF-8"))
        Assert.assertEquals(iv, "data")
    }

    @Test
    fun getDataAndIV_success() {
        val encodedValue = "ZGF0YQ==\n"
        val data = "$encodedValue-_-$encodedValue"
        val result = subject.getDataAndIV(data, separator)
        Assert.assertEquals(result.first, encodedValue)
        Assert.assertEquals(result.second, encodedValue)
    }

    @Test(expected = IllegalStateException::class)
    fun getDataAndIV_noSeparator() {
        val encodedValue = "ZGF0YQ==\n"
        val data = "$encodedValue$encodedValue"
        subject.getDataAndIV(data, separator)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getDataAndIV_noIV() {
        val encodedValue = "ZGF0YQ==\n"
        val data = "$encodedValue-_-"
        subject.getDataAndIV(data, separator)
    }

    @Test(expected = IllegalStateException::class)
    fun ensureDecryptFailsOnEmptyData() {
        subject.decodeAndDecryptData("anyKey", separator, "")
    }

    companion object {
        const val separator = "-_-"
    }
}