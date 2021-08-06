package com.blockchain.network.websocket

import com.blockchain.logging.Logger
import com.blockchain.logging.NullLogger
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.reactivex.rxjava3.core.Observable

import org.amshove.kluent.`should be`
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest

class LoggingWebSocketTest : AutoCloseKoinTest() {

    private val logger = mock<Logger>()

    @Before
    fun setup() {
        startKoin {
            // load Koin modules
            modules(listOf(
                module {
                    single {
                        logger
                    }
                }
            ))
        }
    }

    @Test
    fun `logs sends`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        val socket = underlyingSocket
            .debugLog("X")

        socket.send("Hello")

        verify(logger).v("WebSocket X send Hello")
        verifyNoMoreInteractions(logger)
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

        verify(logger).v("WebSocket Y receive A response")
        verifyNoMoreInteractions(logger)
    }

    @Test
    fun `logs connection events`() {
        mock<WebSocket<String, String>> {
            on { connectionEvents }.thenReturn(Observable.just(ConnectionEvent.Connected))
        }
            .debugLog("Z")
            .connectionEvents
            .test()

        verify(logger).d("WebSocket Z Connected")
        verifyNoMoreInteractions(logger)
    }

    @Test
    fun `open calls`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        underlyingSocket
            .debugLog("Z")
            .open()

        verify(logger).d("WebSocket Z Open called")
        verifyNoMoreInteractions(logger)
        verify(underlyingSocket).open()
    }

    @Test
    fun `close calls`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        underlyingSocket
            .debugLog("Z")
            .close()

        verify(logger).d("WebSocket Z Close called")
        verifyNoMoreInteractions(logger)
        verify(underlyingSocket).close()
    }
}

class LoggingWebSocketWithNullLoggerTest : AutoCloseKoinTest() {

    @Before
    fun setup() {
        startKoin {
            modules(listOf(
                module {
                    single {
                        NullLogger as Logger
                    }
                }
            ))
        }
    }

    @Test
    fun `no wrapper socket is provided`() {
        val underlyingSocket = mock<WebSocket<String, String>>()
        underlyingSocket.debugLog("X") `should be` underlyingSocket
    }
}
