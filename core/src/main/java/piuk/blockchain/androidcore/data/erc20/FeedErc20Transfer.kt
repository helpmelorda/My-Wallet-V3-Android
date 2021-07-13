package piuk.blockchain.androidcore.data.erc20

import io.reactivex.rxjava3.core.Observable
import java.math.BigInteger

data class FeedErc20Transfer(val transfer: Erc20Transfer, val feeObservable: Observable<BigInteger>)
