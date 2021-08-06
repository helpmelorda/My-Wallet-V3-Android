package com.blockchain.network.websocket

import com.blockchain.serialization.JsonSerializable
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class ChannelAwareWebSocketTest {

    @Test
    fun `when open a channel, a subscribe message is sent down the underlying socket`() {
        val underlingSocket = mock<StringWebSocket>()
        underlingSocket.channelAware()
            .openChannel("ChannelName")
        verify(underlingSocket).send(
            "{\"action\":\"subscribe\"," +
                "\"channel\":\"ChannelName\"" +
                "}"
        )
        verifyNoMoreInteractions(underlingSocket)
    }

    @Test
    fun `when open a channel with parameters, a subscribe message is sent down the underlying socket`() {
        val underlingSocket = mock<StringWebSocket>()
        underlingSocket.channelAware()
            .openChannel("ChannelName", Params(param1 = "ABC"))
        verify(underlingSocket).send(
            "{\"action\":\"subscribe\"," +
                "\"channel\":\"ChannelName\"," +
                "\"params\":{" +
                "\"param1\":\"ABC\"}" +
                "}"
        )
        verifyNoMoreInteractions(underlingSocket)
    }

    @Test
    fun `channel is filtered`() {
        val underlingSocket = mock<StringWebSocket> {
            on { responses }.thenReturn(
                Observable.just(
                    "{\"channel\":\"ChannelName\"}",
                    "{\"x\":\"y\"}",
                    "{\"channel\":\"OtherChannel\"}",
                    "{\"channel\":\"ChannelName\",\"event\":\"subscribed\"}",
                    "{\"channel\":\"ChannelName\",\"event\":\"unsubscribed\"}",
                    "null"
                )
            )
        }
        underlingSocket.channelAware()
            .openChannel("ChannelName")
            .responses
            .test()
            .values() `should be equal to` listOf("{\"channel\":\"ChannelName\"}")
    }

    @Test
    fun `two channels on one webSocket`() {
        val underlingSocket = mock<StringWebSocket> {
            on { responses }.thenReturn(
                Observable.just(
                    "{\"channel\":\"ChannelName\"}",
                    "{\"channel\":\"OtherChannel\"}",
                    "{\"channel\":\"OtherChannel\",\"message\":\"message2\"}"
                )
            )
        }
        val channelAwareWebSocket = underlingSocket.channelAware()
        channelAwareWebSocket
            .openChannel("ChannelName")
            .responses
            .test()
            .values() `should be equal to` listOf("{\"channel\":\"ChannelName\"}")
        channelAwareWebSocket
            .openChannel("OtherChannel")
            .responses
            .test()
            .values() `should be equal to` listOf(
            "{\"channel\":\"OtherChannel\"}",
            "{\"channel\":\"OtherChannel\",\"message\":\"message2\"}"
        )
    }

    @Test
    fun `when close a channel, an subscribe message is sent down the underlying socket`() {
        val underlingSocket = mock<StringWebSocket>()
        val openChannel = underlingSocket.channelAware()
            .openChannel("ChannelName")
        reset(underlingSocket)
        openChannel.close()
        verify(underlingSocket).send(
            "{\"action\":\"unsubscribe\"," +
                "\"channel\":\"ChannelName\"" +
                "}"
        )
        verifyNoMoreInteractions(underlingSocket)
    }

    @Test
    fun `when close a channel with params, an subscribe message is sent down the underlying socket`() {
        val underlingSocket = mock<StringWebSocket>()
        val openChannel = underlingSocket.channelAware()
            .openChannel("ChannelName")
        reset(underlingSocket)
        openChannel.close(Params(param1 = "closing"))
        verify(underlingSocket).send(
            "{\"action\":\"unsubscribe\"," +
                "\"channel\":\"ChannelName\"," +
                "\"params\":{" +
                "\"param1\":\"closing\"}" +
                "}"
        )
        verifyNoMoreInteractions(underlingSocket)
    }

    @Test
    fun `when close a channel, the observable completes`() {
        val underlingSocket = mock<StringWebSocket> {
            on { responses }.thenReturn(Observable.never())
        }
        val channelAwareWebSocket = underlingSocket.channelAware()
        val openChannel = channelAwareWebSocket
            .openChannel("ChannelName")
        val test = openChannel
            .responses
            .test()
        test.assertNotComplete()
        openChannel.close()
        test.assertComplete()
    }

    @Test
    fun `errors are not filtered out`() {
        val underlingSocket = mock<StringWebSocket> {
            on { responses }.thenReturn(
                Observable.just(
                    "{\"channel\":\"ChannelName\",\"event\": \"error\"}"
                )
            )
        }

        val channelAwareWebSocket = underlingSocket.channelAware()
        channelAwareWebSocket
            .openChannel("ChannelName")
            .responses
            .test()
            .assertError(ErrorFromServer::class.java)
            .assertError { (it as ErrorFromServer).fullJson == "{\"channel\":\"ChannelName\",\"event\": \"error\"}" }
    }

    class Params(@Suppress("unused") val param1: String) : JsonSerializable
}
