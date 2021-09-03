package com.blockchain.extensions

import com.nhaarman.mockitokotlin2.mock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import org.junit.Test
import piuk.blockchain.androidcore.utils.extensions.AccountLockedException
import piuk.blockchain.androidcore.utils.extensions.AuthRequiredException
import piuk.blockchain.androidcore.utils.extensions.InitialErrorException
import piuk.blockchain.androidcore.utils.extensions.UnknownErrorException
import piuk.blockchain.androidcore.utils.extensions.handleResponse
import retrofit2.Response

class ResponseExtensionsTest {

    @Test
    fun successfulResponse() {
        val responseMock: ResponseBody = mock {
            on { string() }.thenReturn("{\"test\":\"test\"}")
        }
        val response: Response<ResponseBody> = mock {
            on { isSuccessful }.thenReturn(true)
            on { body() }.thenReturn(responseMock)
        }

        val observer = response.handleResponse().test()
        val expectedObject = Json.parseToJsonElement(responseMock.string()) as JsonObject

        observer
            .assertNoErrors()
            .assertValueAt(0) {
                it == expectedObject
            }
    }

    @Test
    fun successfulResponseWithoutBody() {
        val response: Response<ResponseBody> = mock {
            on { isSuccessful }.thenReturn(true)
            on { body() }.thenReturn(null)
        }

        val observer = response.handleResponse().test()
        observer.assertError {
            it is UnknownErrorException
        }
    }

    @Test
    fun errorResponseAccountLocked() {
        val errorMock: ResponseBody = mock {
            on { string() }.thenReturn("Account locked")
        }
        val response: Response<ResponseBody> = mock {
            on { isSuccessful }.thenReturn(false)
            on { errorBody() }.thenReturn(errorMock)
        }

        val observer = response.handleResponse().test()
        observer.assertError {
            it is AccountLockedException
        }
    }

    @Test
    fun errorResponseNoBody() {
        val errorMock: ResponseBody = mock {
            on { string() }.thenReturn(null)
        }
        val response: Response<ResponseBody> = mock {
            on { isSuccessful }.thenReturn(false)
            on { errorBody() }.thenReturn(errorMock)
        }

        val observer = response.handleResponse().test()
        observer.assertError {
            it is UnknownErrorException
        }
    }

    @Test
    fun errorResponseInitialError() {
        val errorMock: ResponseBody = mock {
            on { string() }.thenReturn("{\"initial_error\":\"test\"}")
        }
        val response: Response<ResponseBody> = mock {
            on { isSuccessful }.thenReturn(false)
            on { errorBody() }.thenReturn(errorMock)
        }

        val observer = response.handleResponse().test()
        observer.assertError {
            it is InitialErrorException
        }
    }

    @Test
    fun errorResponseAuthRequired() {
        val errorMock: ResponseBody = mock {
            on { string() }.thenReturn("{\"authorization_required\":\"test\"}")
        }
        val response: Response<ResponseBody> = mock {
            on { isSuccessful }.thenReturn(false)
            on { errorBody() }.thenReturn(errorMock)
        }

        val observer = response.handleResponse().test()
        observer.assertError {
            it is AuthRequiredException
        }
    }

    @Test
    fun errorResponseOther() {
        val errorMock: ResponseBody = mock {
            on { string() }.thenReturn("{\"another_value\":\"test\"}")
        }
        val response: Response<ResponseBody> = mock {
            on { isSuccessful }.thenReturn(false)
            on { errorBody() }.thenReturn(errorMock)
        }

        val observer = response.handleResponse().test()
        observer.assertError {
            it is UnknownErrorException
        }
    }
}