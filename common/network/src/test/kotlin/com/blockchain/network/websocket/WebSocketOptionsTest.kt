package com.blockchain.network.websocket

import org.amshove.kluent.`should be equal to`
import org.junit.Test

class WebSocketOptionsTest {

    @Test
    fun `the default origin is blockchain dot info`() {
        Options("wss://anyUrl").origin `should be equal to` "https://blockchain.info"
    }
}
