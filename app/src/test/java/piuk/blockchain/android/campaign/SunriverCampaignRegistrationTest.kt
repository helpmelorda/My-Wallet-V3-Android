package piuk.blockchain.android.campaign

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.nabu.CampaignData
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.RegisterCampaignRequest
import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.sunriver.XlmAccountReference
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be`
import org.junit.Rule
import org.junit.Test

class SunriverCampaignRegistrationTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Test
    fun `get card type complete`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        SunriverCampaignRegistration(

            mock {
                on { getCampaignList(offlineToken) }.thenReturn(Single.just(listOf("SUNRIVER")))
            },
            givenToken(offlineToken),
            mock {
                on { getUserState() }.thenReturn(Single.just(UserState.Active))
                on { getKycStatus() }.thenReturn(Single.just(KycState.Verified))
            },
            mock()
        ).getCampaignCardType()
            .test()
            .values()
            .first()
            .apply {
                this `should be equal to` SunriverCardType.Complete
            }
    }

    @Test
    fun `get card type join wait list`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        SunriverCampaignRegistration(
            mock {
                on { getCampaignList(offlineToken) }.thenReturn(Single.just(emptyList()))
            },
            givenToken(offlineToken),
            mock {
                on { getUserState() }.thenReturn(Single.just(UserState.Active))
                on { getKycStatus() }.thenReturn(Single.just(KycState.None))
            },
            mock()
        ).getCampaignCardType()
            .test()
            .values()
            .first()
            .apply {
                this `should be equal to` SunriverCardType.JoinWaitList
            }
    }

    @Test
    fun `get card type finish sign up`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        SunriverCampaignRegistration(
            mock {
                on { getCampaignList(offlineToken) }.thenReturn(Single.just(listOf("SUNRIVER")))
            },
            givenToken(offlineToken),
            mock {
                on { getUserState() }.thenReturn(Single.just(UserState.Created))
                on { getKycStatus() }.thenReturn(Single.just(KycState.None))
            },
            mock()
        ).getCampaignCardType()
            .test()
            .values()
            .first()
            .apply {
                this `should be equal to` SunriverCardType.FinishSignUp
            }
    }

    @Test
    fun `register as user already has an account`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        val accountRef = XlmAccountReference("", "GABCDEFHI")
        val campaignData = CampaignData("name", false)
        val nabuDataManager = mock<NabuDataManager> {
            on { registerCampaign(any(), any(), any()) }.thenReturn(Completable.complete())
        }
        val xlmDataManager: XlmDataManager = mock()
        whenever(xlmDataManager.defaultAccount()).thenReturn(Single.just(accountRef))

        SunriverCampaignRegistration(
            nabuDataManager = nabuDataManager,
            nabuToken = givenToken(offlineToken),
            kycStatusHelper = mock(),
            xlmDataManager = xlmDataManager
        ).registerCampaign(campaignData)
            .test()
            .assertNoErrors()
            .assertComplete()
        verify(nabuDataManager).registerCampaign(
            offlineToken,
            RegisterCampaignRequest.registerSunriver(
                accountRef.accountId,
                campaignData.newUser
            ),
            campaignData.campaignName
        )
        verifyNoMoreInteractions(nabuDataManager)
    }

    @Test
    fun `register as user has no account`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        val accountRef = XlmAccountReference("", "GABCDEFHIJ")
        val campaignData = CampaignData("name", false)
        val nabuDataManager = mock<NabuDataManager> {
            on { registerCampaign(any(), any(), any()) }.thenReturn(Completable.complete())
            on { requestJwt() }.thenReturn(Single.just("jwt"))
            on { getAuthToken("jwt") }.thenReturn(Single.just(offlineToken))
        }
        val xlmDataManager: XlmDataManager = mock()
        whenever(xlmDataManager.defaultAccount()).thenReturn(Single.just(accountRef))

        SunriverCampaignRegistration(
            nabuDataManager = nabuDataManager,
            nabuToken = givenToken(offlineToken),
            kycStatusHelper = mock(),
            xlmDataManager = xlmDataManager
        ).registerCampaign(campaignData)
            .test()
            .assertNoErrors()
            .assertComplete()
        verify(nabuDataManager).registerCampaign(
            offlineToken,
            RegisterCampaignRequest.registerSunriver(
                accountRef.accountId,
                campaignData.newUser
            ),
            campaignData.campaignName
        )
        verifyNoMoreInteractions(nabuDataManager)
    }

//    @Test
//    fun `register as user already has an account`() {
//        val offlineToken = NabuOfflineTokenResponse("userId", "token")
//        val accountRef = AccountReference.Xlm("", "GABCDEFHI")
//        val campaignData = CampaignData("name", false)
//        val nabuDataManager = mock<NabuDataManager> {
//            on { registerCampaign(any(), any(), any()) }.thenReturn(Completable.complete()
//        }
//        SunriverCampaignRegistration(
//            mock(),
//            nabuDataManager,
//            givenToken(offlineToken),
//            mock(),
//            mock()
//        ).registerCampaignAndSignUpIfNeeded(campaignData)
//            .test()
//            .assertNoErrors()
//            .assertComplete()
//        verify(nabuDataManager).registerCampaign(
//            offlineToken,
//            RegisterCampaignRequest.registerSunriver(
//                accountRef.accountId,
//                campaignData.newUser
//            ),
//            campaignData.campaignName
//        )
//        verifyNoMoreInteractions(nabuDataManager)
//    }
//
//    @Test
//    fun `register as user has no account`() {
//        val offlineToken = NabuOfflineTokenResponse("userId", "token")
//        val accountRef = AccountReference.Xlm("", "GABCDEFHIJ")
//        val campaignData = CampaignData("name", false)
//        val nabuDataManager = mock<NabuDataManager> {
//            on { registerCampaign(any(), any(), any()) }.thenReturn(Completable.complete()
//            on { requestJwt() }.thenReturn(Single.just("jwt")
//            on { getAuthToken("jwt") }.thenReturn(Single.just(offlineToken)
//        }
//        SunriverCampaignRegistration(
//            mock(),
//            nabuDataManager,
//            givenToken(offlineToken),
//            mock(),
//            mock()
//        ).registerCampaignAndSignUpIfNeeded(accountRef, campaignData)
//            .test()
//            .assertNoErrors()
//            .assertComplete()
//        verify(nabuDataManager).registerCampaign(
//            offlineToken,
//            RegisterCampaignRequest.registerSunriver(
//                accountRef.accountId,
//                campaignData.newUser
//            ),
//            campaignData.campaignName
//        )
//        verifyNoMoreInteractions(nabuDataManager)
//    }

    @Test
    fun `register sunriver campaign`() {
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        val accountRef = XlmAccountReference("", "GABCDEFHJIK")
        val nabuDataManager = mock<NabuDataManager> {
            on { registerCampaign(any(), any(), any()) }.thenReturn(Completable.complete())
        }
        SunriverCampaignRegistration(
            nabuDataManager,
            givenToken(offlineToken),
            mock(),
            mock {
                on { defaultAccount() }.thenReturn(Single.just(accountRef))
            }
        ).registerCampaign()
            .test()
            .assertNoErrors()
            .assertComplete()
        verify(nabuDataManager).registerCampaign(
            offlineToken,
            RegisterCampaignRequest.registerSunriver(
                "GABCDEFHJIK",
                false
            ),
            "SUNRIVER"
        )
        verifyNoMoreInteractions(nabuDataManager)
    }

    @Test
    fun `user is in sunriver campaign`() {
        givenUserInCampaigns(listOf("SUNRIVER"))
            .userIsInCampaign()
            .test()
            .values()
            .single() `should be` true
    }

    @Test
    fun `user is not in any campaign`() {
        givenUserInCampaigns(emptyList())
            .userIsInCampaign()
            .test()
            .values()
            .single() `should be` false
    }

    @Test
    fun `user is in other campaign`() {
        givenUserInCampaigns(listOf("CAMPAIGN2"))
            .userIsInCampaign()
            .test()
            .values()
            .single() `should be` false
    }

    @Test
    fun `user is in multiple campaigns`() {
        givenUserInCampaigns(listOf("CAMPAIGN2", "SUNRIVER"))
            .userIsInCampaign()
            .test()
            .values()
            .single() `should be` true
    }
}

private fun givenUserInCampaigns(campaigns: List<String>): SunriverCampaignRegistration {
    val offlineToken = NabuOfflineTokenResponse("userId", "token")
    return SunriverCampaignRegistration(
        mock {
            on { getCampaignList(offlineToken) }.thenReturn(Single.just(campaigns))
        },
        givenToken(offlineToken),
        mock(),
        mock()
    )
}

private fun givenToken(offlineToken: NabuOfflineTokenResponse): NabuToken =
    mock {
        on { fetchNabuToken() }.thenReturn(Single.just(offlineToken))
    }
