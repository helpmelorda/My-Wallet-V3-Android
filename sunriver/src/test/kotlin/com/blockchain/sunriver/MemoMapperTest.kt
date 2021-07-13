package com.blockchain.sunriver

import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.`with message`
import org.junit.Test
import org.stellar.sdk.Memo
import org.stellar.sdk.MemoHash
import org.stellar.sdk.MemoId
import org.stellar.sdk.MemoReturnHash
import org.stellar.sdk.MemoText

class MemoMapperTest {

    @Test
    fun `null memo`() {
        MemoMapper().mapMemo(null) `should be equal to` Memo.none()
    }

    @Test
    fun `with specified type -text- should be a MemoText`() {
        val memo = createMemo("Hello, test memo", type = "text")
        memo `should not be` null
        memo `should be instance of` MemoText::class.java
        (memo as MemoText).text `should be equal to` "Hello, test memo"
    }

    @Test
    fun `with no specified type -null- should be a MemoText`() {
        val memo = createMemo("Hello, test memo, with null")
        memo `should not be` null
        memo `should be instance of` MemoText::class.java
        (memo as MemoText).text `should be equal to` "Hello, test memo, with null"
    }

    @Test
    fun `with specified type -id- should be a MemoId`() {
        val memo = createMemo("9871230892735", type = "id")
        memo `should not be` null
        memo `should be instance of` MemoId::class.java
        (memo as MemoId).id `should be equal to` 9871230892735L
    }

    @Test
    fun `with specified type -hash- should be a MemoHash`() {
        val memo = createMemo("0102030405060707020212351a8e0d9fffff0f8f7f6f5f5f24f5f67f2f2f63fa", type = "hash")
        memo `should not be` null
        memo `should be instance of` MemoHash::class.java
        (memo as MemoHash).hexValue `should be equal to`
            "0102030405060707020212351a8e0d9fffff0f8f7f6f5f5f24f5f67f2f2f63fa"
    }

    @Test
    fun `with specified type -return- should be a MemoReturnHash`() {
        val memo = createMemo("0102030405060707020212351a8e0d9fffff0f8f7f6f5f5f24f5f67f2f2f63fa", type = "return")
        memo `should not be` null
        memo `should be instance of` MemoReturnHash::class.java
        (memo as MemoReturnHash).hexValue `should be equal to`
            "0102030405060707020212351a8e0d9fffff0f8f7f6f5f5f24f5f67f2f2f63fa"
    }

    @Test
    fun `with unknown specified type should throw`() {
        {
            MemoMapper().mapMemo(
                Memo(
                    value = "Hello, test memo",
                    type = "unknown"
                )
            )
        } `should throw` IllegalArgumentException::class `with message`
            "Only null, text, id, hash and return are supported, not unknown"
    }

    @Test
    fun `Map none`() {
        MemoMapper().mapMemo(com.blockchain.sunriver.Memo.None) `should be equal to` Memo.none()
    }

    @Test
    fun `Map blank text`() {
        MemoMapper().mapMemo(Memo(value = "   ", type = "text")) `should be equal to` Memo.none()
    }

    @Test
    fun `Map blank id`() {
        MemoMapper().mapMemo(Memo(value = "   ", type = "id")) `should be equal to` Memo.none()
    }

    private fun createMemo(value: String, type: String? = null) = MemoMapper().mapMemo(
        Memo(
            value = value,
            type = type
        )
    )
}
