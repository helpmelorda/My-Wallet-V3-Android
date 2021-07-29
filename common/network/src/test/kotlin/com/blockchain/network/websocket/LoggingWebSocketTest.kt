package com.blockchain.network.websocket

import com.blockchain.logging.Logger
import com.blockchain.logging.NullLogger
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.`should be`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest

class LoggingWebSocketTest : KoinTest {

    private val logging = mock<Logger>()

    @Before
    fun setup() {
        startKoin {
            allowOverride(override = true)
            modules(
                module {
                    single {
                        logging
                    }.bind(Logger::class)
                }
            )
        }
    }

    @After
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `logs sends`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        val socket = underlyingSocket
            .debugLog("X")

        socket.send("Hello")

        verify(logging).v("WebSocket X send Hello")
        verifyNoMoreInteractions(logging)
        verify(underlyingSocket).send("Hello")
    }

    @Test
    fun `logs receives`() {
        mock<WebSocket<String, String>> {
            on { responses }.thenReturn(Observable.just("A response"))
        }
            .debugLog("Y")
            .responses
            .test()

        verify(logging).v("WebSocket Y receive A response")
        verifyNoMoreInteractions(logging)
    }

    @Test
    fun `logs connection events`() {
        mock<WebSocket<String, String>> {
            on { connectionEvents }.thenReturn(Observable.just(ConnectionEvent.Connected))
        }
            .debugLog("Z")
            .connectionEvents
            .test()

        verify(logging).d("WebSocket Z Connected")
        verifyNoMoreInteractions(logging)
    }

    @Test
    fun `open calls`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        underlyingSocket
            .debugLog("Z")
            .open()

        verify(logging).d("WebSocket Z Open called")
        verifyNoMoreInteractions(logging)
        verify(underlyingSocket).open()
    }

    @Test
    fun `close calls`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        underlyingSocket
            .debugLog("Z")
            .close()

        verify(logging).d("WebSocket Z Close called")
        verifyNoMoreInteractions(logging)
        verify(underlyingSocket).close()
    }
}

class LoggingWebSocketWithNullLoggerTest : KoinTest {

    @Before
    fun setup() {
        startKoin {
            modules(listOf(
                module {
                    single {
                        NullLogger
                    }.bind(Logger::class)
                }
            ))
        }
    }

    @After
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `no wrapper socket is provided`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        underlyingSocket.debugLog("X") `should be` underlyingSocket
    }
}
