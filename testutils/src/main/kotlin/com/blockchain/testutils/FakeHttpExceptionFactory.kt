package com.blockchain.testutils

import com.nhaarman.mockitokotlin2.mock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response

object FakeHttpExceptionFactory {
    fun httpExceptionWith(code: Int): HttpException {
        return HttpException(
            Response.error<Unit>(
                code,
                "{}".toResponseBody("application/json".toMediaTypeOrNull())
            )
        )
    }

    fun <T> mockApiCall(responseBody: T): Call<T> {
        val response: Response<T> = mockResponse(responseBody)
        return mock {
            on { execute() }.thenReturn(response)
        }
    }

    fun <T> mockResponse(responseBody: T, successful: Boolean = true): Response<T> {
        return mock {
            on { body() }.thenReturn(responseBody)
            on { isSuccessful }.thenReturn(successful)
        }
    }
}