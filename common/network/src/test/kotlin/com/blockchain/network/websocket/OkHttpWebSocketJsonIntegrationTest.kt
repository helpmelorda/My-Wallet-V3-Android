package com.blockchain.network.websocket

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import org.junit.Test

class OkHttpWebSocketJsonIntegrationTest {

    private val options = Options(url = "https://blockchain.info/service")
    private val client: OkHttpClient = mock()
    private val socket: WebSocket = mock()

    private val subject = OkHttpWebSocket(client, options, null)

    @Suppress("unused")
    class ClientMessage(val data1: String, val data2: Int)

    data class ServerMessage(val data3: String, val data4: Int)

    private val moshi = Moshi.Builder().build()

    @Test
    fun `can send one message`() {
        val message = ClientMessage(data1 = "Subscribe", data2 = 1)
        val messageAsJson = moshi.adapter(ClientMessage::class.java).toJson(message)

        whenever(client.newWebSocket(any(), any())).thenReturn(socket)

        subject.toJsonSocket<ClientMessage, ServerMessage>(moshi)
            .apply {
                open()
                send(message)
            }

        verify(socket).send(messageAsJson)
        verifyNoMoreInteractions(socket)
    }
}
