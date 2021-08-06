package piuk.blockchain.android.sunriver

import com.blockchain.fees.FeeType
import com.blockchain.sunriver.XlmFeesFetcher
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.fees.FeeDataManager

internal class XlmFeesFetcherAdapter(
    private val feesDataManager: FeeDataManager
) : XlmFeesFetcher {
    override fun operationFee(feeType: FeeType): Single<CryptoValue> =
        feesDataManager.xlmFeeOptions
            .map {
                CryptoValue.fromMinor(
                    CryptoCurrency.XLM,
                    when (feeType) {
                        FeeType.Regular -> it.regularFee.toBigInteger()
                        FeeType.Priority -> it.priorityFee.toBigInteger()
                    }
                )
            }.singleOrError()
}
