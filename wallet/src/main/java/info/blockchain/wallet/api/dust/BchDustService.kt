package info.blockchain.wallet.api.dust

import com.blockchain.api.NonCustodialBitcoinService
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.api.dust.data.DustInput
import io.reactivex.rxjava3.core.Single

interface DustService {
    fun getDust(): Single<DustInput>
}

internal class BchDustService(
    private val api: DustApi,
    private val apiCode: ApiCode
) : DustService {
    override fun getDust(): Single<DustInput> =
        api.getDust(NonCustodialBitcoinService.BITCOIN_CASH, apiCode.apiCode)
}