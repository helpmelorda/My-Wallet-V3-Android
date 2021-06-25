package info.blockchain.wallet.prices

import info.blockchain.balance.AssetInfo
import io.reactivex.Single
import java.math.BigDecimal

interface CurrentPriceApi {

    fun currentPrice(base: AssetInfo, quoteFiatCode: String): Single<BigDecimal>
}