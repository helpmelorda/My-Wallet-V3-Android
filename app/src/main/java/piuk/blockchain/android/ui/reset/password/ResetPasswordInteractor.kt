package piuk.blockchain.android.ui.reset.password

import com.blockchain.metadata.MetadataRepository
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.preferences.WalletStatus
import io.reactivex.rxjava3.core.Completable
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcore.utils.extensions.then

class ResetPasswordInteractor(
    private val authDataManager: AuthDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val accessState: AccessState,
    private val prefs: PersistentPrefs,
    private val nabuDataManager: NabuDataManager,
    private val metadataManager: MetadataManager,
    private val metadataRepository: MetadataRepository,
    private val prngFixer: PrngFixer,
    private val walletPrefs: WalletStatus
) {

    fun createWalletForAccount(email: String, password: String, walletName: String): Completable {
        prngFixer.applyPRNGFixes()
        return payloadDataManager.createHdWallet(password, walletName, email)
            .flatMapCompletable { wallet ->
                Completable.fromCallable {
                    accessState.isNewlyCreated = true
                    prefs.walletGuid = wallet.guid
                    prefs.sharedKey = wallet.sharedKey
                    prefs.email = email
                    walletPrefs.setNewUser()
                }
            }
    }

    fun recoverAccount(userId: String, recoveryToken: String): Completable =
        nabuDataManager.recoverAccount(userId, recoveryToken).flatMapCompletable { nabuMetadata ->
            metadataManager.attemptMetadataSetup()
                .then {
                    metadataRepository.saveMetadata(
                        nabuMetadata,
                        NabuCredentialsMetadata::class.java,
                        NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
                    )
                }
        }

    fun setNewPassword(password: String): Completable {
        val fallbackPassword = payloadDataManager.tempPassword
        payloadDataManager.tempPassword = password
        accessState.isRestored = true
        return authDataManager.verifyCloudBackup()
            .then { payloadDataManager.syncPayloadWithServer() }
            .doOnError {
                payloadDataManager.tempPassword = fallbackPassword
            }
    }

    fun resetUserKyc(): Completable = nabuDataManager.resetUserKyc()
}