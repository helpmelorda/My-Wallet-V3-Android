package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.isCustodialOnly
import info.blockchain.balance.isErc20
import io.reactivex.rxjava3.core.Completable
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.bch.BchAddress
import piuk.blockchain.android.coincore.btc.BtcAddress
import piuk.blockchain.android.coincore.custodialonly.DynamicCustodialAddress
import piuk.blockchain.android.coincore.erc20.Erc20Address
import piuk.blockchain.android.coincore.eth.EthAddress
import piuk.blockchain.android.coincore.xlm.XlmAddress

internal fun makeExternalAssetAddress(
    asset: AssetInfo,
    address: String,
    label: String = address,
    postTransactions: (TxResult) -> Completable = { Completable.complete() }
): CryptoAddress =
    when {
        asset == CryptoCurrency.ETHER -> {
            EthAddress(
                address = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.BTC -> {
            BtcAddress(
                address = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.BCH -> {
            BchAddress(
                address_ = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset == CryptoCurrency.XLM -> {
            XlmAddress(
                _address = address,
                _label = label,
                onTxCompleted = postTransactions
            )
        }
        asset.isErc20() -> {
            Erc20Address(
                asset = asset,
                address = address,
                label = label,
                onTxCompleted = postTransactions
            )
        }
        asset.isCustodialOnly -> {
            DynamicCustodialAddress(
                address = address,
                asset = asset,
                label = label
            )
        }
        else -> throw IllegalArgumentException("External Address not not supported for asset: $asset")
    }
