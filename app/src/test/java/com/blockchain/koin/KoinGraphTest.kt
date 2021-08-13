package com.blockchain.koin

import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication

@Config(sdk = [24], application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class KoinGraphTest : KoinTest {

    @After
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `test module configuration`() {
        getKoin().checkModules()
    }
}
