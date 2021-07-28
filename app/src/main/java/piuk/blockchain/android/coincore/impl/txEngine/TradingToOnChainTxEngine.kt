package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.android.ui.transactionflow.flow.FeeInfo

// Transfer from a custodial trading account to an onChain non-custodial account
class TradingToOnChainTxEngine(
    private val isNoteSupported: Boolean,
    private val walletManager: CustodialWalletManager
) : TxEngine() {

    override fun assertInputsValid() {
        check(txTarget is CryptoAddress)
        check(sourceAsset == (txTarget as CryptoAddress).asset)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.zip(
            sourceAccount.accountBalance.map { it as CryptoValue },
            sourceAccount.actionableBalance.map { it as CryptoValue },
            walletManager.fetchCryptoWithdrawFeeAndMinLimit(sourceAsset, Product.BUY),
            { total, available, cryptoFeeAndMin ->
                PendingTx(
                    amount = CryptoValue.zero(sourceAsset),
                    totalBalance = total,
                    availableBalance = available.minus(CryptoValue.fromMinor(sourceAsset, cryptoFeeAndMin.fee)),
                    feeForFullAvailable = CryptoValue.fromMinor(sourceAsset, cryptoFeeAndMin.fee),
                    feeAmount = CryptoValue.fromMinor(sourceAsset, cryptoFeeAndMin.fee),
                    feeSelection = FeeSelection(),
                    selectedFiat = userFiat,
                    minLimit = CryptoValue.fromMinor(sourceAsset, cryptoFeeAndMin.minLimit)
                )
            }
        )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == sourceAsset)

        return Single.zip(
            sourceAccount.accountBalance.map { it as CryptoValue },
            sourceAccount.actionableBalance.map { it as CryptoValue },
            walletManager.fetchCryptoWithdrawFeeAndMinLimit(sourceAsset, Product.BUY)
        ) { total, available, cryptoFeeAndMin ->
            pendingTx.copy(
                amount = amount,
                totalBalance = total,
                availableBalance = available.minus(CryptoValue.fromMinor(sourceAsset, cryptoFeeAndMin.fee))
            )
        }
    }

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        // This engine only supports FeeLevel.None, so
        return Single.just(pendingTx)
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                confirmations = listOfNotNull(
                    TxConfirmationValue.From(sourceAccount, sourceAsset),
                    TxConfirmationValue.To(
                        txTarget, AssetAction.Send, sourceAccount
                    ),
                    TxConfirmationValue.CompoundNetworkFee(
                        receivingFeeInfo = if (!pendingTx.feeAmount.isZero) {
                            FeeInfo(
                                pendingTx.feeAmount,
                                pendingTx.feeAmount.toFiat(exchangeRates, userFiat),
                                sourceAsset
                            )
                        } else null,
                        feeLevel = pendingTx.feeSelection.selectedLevel,
                        ignoreErc20LinkedNote = true
                    ),
                    TxConfirmationValue.Total(
                        totalWithFee = (pendingTx.amount as CryptoValue).plus(
                            pendingTx.feeAmount as CryptoValue
                        ),
                        exchange = pendingTx.amount.toFiat(exchangeRates, userFiat)
                            .plus(pendingTx.feeAmount.toFiat(exchangeRates, userFiat))
                    ),
                    if (isNoteSupported) {
                        TxConfirmationValue.Description()
                    } else null
                )
            )
        )

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx).updateTxValidity(pendingTx)

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.defer {
            val min = pendingTx.minLimit ?: CryptoValue.zero(sourceAsset)
            if (pendingTx.amount.isPositive &&
                pendingTx.availableBalance >= pendingTx.amount &&
                min <= pendingTx.amount
            ) {
                Completable.complete()
            } else {
                throw TxValidationFailure(
                    if (pendingTx.amount > pendingTx.availableBalance) {
                        ValidationState.INSUFFICIENT_FUNDS
                    } else {
                        ValidationState.INVALID_AMOUNT
                    }
                )
            }
        }

    // The custodial balance now returns an id, so it is possible to add a note via this
    // processor at some point. TODO
    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
        val targetAddress = txTarget as CryptoAddress
        return walletManager.transferFundsToWallet(pendingTx.amount as CryptoValue, targetAddress.address)
            .map {
                TxResult.UnHashedTxResult(pendingTx.amount)
            }
    }
}
