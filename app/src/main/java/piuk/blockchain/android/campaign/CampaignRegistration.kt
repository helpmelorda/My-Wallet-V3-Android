package piuk.blockchain.android.campaign

import com.blockchain.nabu.models.responses.nabu.CampaignData
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface CampaignRegistration {
    fun registerCampaign(): Completable
    fun registerCampaign(campaignData: CampaignData): Completable
    fun userIsInCampaign(): Single<Boolean>
}