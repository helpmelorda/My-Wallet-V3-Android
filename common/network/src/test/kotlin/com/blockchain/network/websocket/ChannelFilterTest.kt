package com.blockchain.network.websocket

import com.nhaarman.mockitokotlin2.mock
import io.reactivex.rxjava3.core.Observable

import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class ChannelFilterTest {

    @Test
    fun `channel is filtered`() {
        mock<WebSocketReceive<String>> {
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
        }.channelMessageFilter("ChannelName")
            .responses
            .test()
            .values() `should be equal to` listOf("{\"channel\":\"ChannelName\"}")
    }

    @Test
    fun `errors can be ignored`() {
        mock<WebSocketReceive<String>> {
            on { responses }.thenReturn(
                Observable.just(
                    "{\"channel\":\"ChannelName\",\"event\":\"error\"}",
                    "{\"channel\":\"ChannelName\"}"
                )
            )
        }.channelMessageFilter("ChannelName", throwErrors = false)
            .responses
            .test()
            .apply {
                assertNoErrors()
                values() `should be equal to` listOf("{\"channel\":\"ChannelName\"}")
            }
    }

    @Test
    fun `errors can be thrown - by default`() {
        mock<WebSocketReceive<String>> {
            on { responses }.thenReturn(
                Observable.just(
                    "{\"channel\":\"ChannelName\",\"event\":\"error\"}",
                    "{\"channel\":\"ChannelName\"}"
                )
            )
        }.channelMessageFilter("ChannelName")
            .responses
            .test()
            .apply {
                assertError {
                    it `should be instance of` ErrorFromServer::class
                    it.message `should be equal to` "Server returned error"
                    (it as ErrorFromServer).fullJson `should be equal to`
                        "{\"channel\":\"ChannelName\",\"event\":\"error\"}"
                    true
                }
            }
    }
}
