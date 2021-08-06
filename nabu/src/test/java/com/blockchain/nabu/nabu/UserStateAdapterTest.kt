package com.blockchain.nabu.nabu

import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.nabu.models.responses.nabu.UserStateAdapter
import com.squareup.moshi.JsonDataException
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.junit.Test

class UserStateAdapterTest {

    @Test
    fun `from none`() {
        UserStateAdapter().fromJson("NONE") `should be equal to` UserState.None
    }

    @Test
    fun `from created`() {
        UserStateAdapter().fromJson("CREATED") `should be equal to` UserState.Created
    }

    @Test
    fun `from active`() {
        UserStateAdapter().fromJson("ACTIVE") `should be equal to` UserState.Active
    }

    @Test
    fun `from blocked`() {
        UserStateAdapter().fromJson("BLOCKED") `should be equal to` UserState.Blocked
    }

    @Test
    fun `from unknown should throw exception`() {
        {
            UserStateAdapter().fromJson("malformed")
        } `should throw` JsonDataException::class
    }

    @Test
    fun `to none`() {
        UserStateAdapter().toJson(UserState.None) `should be equal to` "NONE"
    }

    @Test
    fun `to created`() {
        UserStateAdapter().toJson(UserState.Created) `should be equal to` "CREATED"
    }

    @Test
    fun `to active`() {
        UserStateAdapter().toJson(UserState.Active) `should be equal to` "ACTIVE"
    }

    @Test
    fun `to blocked`() {
        UserStateAdapter().toJson(UserState.Blocked) `should be equal to` "BLOCKED"
    }
}