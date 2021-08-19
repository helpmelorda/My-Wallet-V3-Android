package piuk.blockchain.android.coincore

import com.blockchain.core.custodial.TradingAccountBalance
import com.blockchain.core.interest.InterestAccountBalance
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRates
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount

data class AccountBalance(
    val total: Money,
    val actionable: Money,
    val pending: Money,
    val exchangeRate: ExchangeRate?
) {
    val totalFiat: Money? // TEMP nullable TODO - fix it!
        get() = exchangeRate?.convert(total)

    internal companion object {
        fun from(balance: TradingAccountBalance, rate: ExchangeRate): AccountBalance {
            require(rate is ExchangeRate.CryptoToFiat || rate is ExchangeRate.FiatToFiat)

            return AccountBalance(
                total = balance.total,
                actionable = balance.actionable,
                pending = balance.pending,
                exchangeRate = rate
            )
        }

        fun from(balance: InterestAccountBalance, rate: ExchangeRate): AccountBalance {
            require(rate is ExchangeRate.CryptoToFiat)

            return AccountBalance(
                total = balance.totalBalance,
                actionable = balance.actionableBalance,
                pending = balance.pendingDeposit,
                exchangeRate = rate
            )
        }

        fun zero(assetInfo: AssetInfo) =
            AccountBalance(
                total = CryptoValue.zero(assetInfo),
                actionable = CryptoValue.zero(assetInfo),
                pending = CryptoValue.zero(assetInfo),
                exchangeRate = null
            )
    }
}

interface BlockchainAccount {

    val label: String

    val balance: Observable<AccountBalance>

    @Deprecated("Use balance")
    val accountBalance: Single<Money> // Total balance, including uncleared and locked

    @Deprecated("Use balance")
    // Available balance, not including uncleared and locked, that may be used for transactions
    val actionableBalance: Single<Money>

    @Deprecated("Use balance")
    val pendingBalance: Single<Money>

    val activity: Single<ActivitySummaryList>

    val actions: Single<AvailableActions>

    val isFunded: Boolean

    val hasTransactions: Boolean

    val isEnabled: Single<Boolean>

    val disabledReason: Single<IneligibilityReason>

    @Deprecated("Use balance")
    fun fiatBalance(fiatCurrency: String, exchangeRates: ExchangeRates): Single<Money>

    val receiveAddress: Single<ReceiveAddress>

    fun requireSecondPassword(): Single<Boolean> = Single.just(false)
}

interface SingleAccount : BlockchainAccount, TransactionTarget {
    val isDefault: Boolean

    // Is this account currently able to operate as a transaction source
    val sourceState: Single<TxSourceState>

    fun doesAddressBelongToWallet(address: String): Boolean = false
}

enum class TxSourceState {
    CAN_TRANSACT,
    NO_FUNDS,
    FUNDS_LOCKED,
    NOT_ENOUGH_GAS,
    TRANSACTION_IN_FLIGHT,
    NOT_SUPPORTED
}

interface InterestAccount
interface TradingAccount
interface NonCustodialAccount
interface BankAccount
interface ExchangeAccount

typealias SingleAccountList = List<SingleAccount>

interface CryptoAccount : SingleAccount {
    val asset: AssetInfo

    val isArchived: Boolean
        get() = false

    fun matches(other: CryptoAccount): Boolean

    val hasStaticAddress: Boolean
        get() = true
}

interface FiatAccount : SingleAccount {
    val fiatCurrency: String
    override val pendingBalance: Single<Money>
        get() = Single.just(FiatValue.zero(fiatCurrency))

    fun canWithdrawFunds(): Single<Boolean>
}

interface AccountGroup : BlockchainAccount {
    val accounts: SingleAccountList

    override val isEnabled: Single<Boolean>
        get() = Single.just(true)

    override val disabledReason: Single<IneligibilityReason>
        get() = Single.just(IneligibilityReason.NONE)

    fun includes(account: BlockchainAccount): Boolean
}

internal fun BlockchainAccount.isCustodial(): Boolean =
    this is CustodialTradingAccount
