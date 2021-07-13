package com.blockchain.testutils

import com.nhaarman.mockitokotlin2.verify
import okhttp3.mockwebserver.MockWebServer
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test

class MockWebServerTestRuleTest {

    @Test
    fun `mock web server is both started and stopped`() {
        val mockWebServer: MockWebServer = mock()
        mockWebServerInit(mockWebServer).runRule()

        verify(mockWebServer).start()
        verify(mockWebServer).shutdown()
    }
}