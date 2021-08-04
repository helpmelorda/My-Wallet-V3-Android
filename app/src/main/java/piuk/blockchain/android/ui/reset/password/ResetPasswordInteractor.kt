package piuk.blockchain.android.ui.reset.password

import com.blockchain.metadata.MetadataRepository
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.preferences.WalletStatus
import io.reactivex.rxjava3.core.Completable
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcore.utils.extensions.then

class ResetPasswordInteractor(
    private val payloadDataManager: PayloadDataManager,
    private val accessState: AccessState,
    private val prefs: PersistentPrefs,
    private val nabuDataManager: NabuDataManager,
    private val metadataRepository: MetadataRepository,
    private val metadataManager: MetadataManager,
    private val prngFixer: PrngFixer,
    private val walletPrefs: WalletStatus
) {

    fun createWalletForAccount(email: String, password: String, walletName: String): Completable {
        prngFixer.applyPRNGFixes()
        return payloadDataManager.createHdWallet(password, walletName, email)
            .flatMapCompletable { wallet ->
                metadataManager.attemptMetadataSetup()
                    .then {
                        Completable.fromCallable {
                            accessState.isNewlyCreated = true
                            prefs.walletGuid = wallet.guid
                            prefs.sharedKey = wallet.sharedKey
                            walletPrefs.setNewUser()
                            prefs.setValue(PersistentPrefs.KEY_EMAIL, email)
                        }
                    }
            }
    }

    fun recoverAccount(recoveryToken: String): Completable =
        nabuDataManager.recoverAccount(recoveryToken).flatMapCompletable { nabuMetadata ->
            metadataRepository.saveMetadata(
                nabuMetadata,
                NabuCredentialsMetadata::class.java,
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        }

    fun restoreWallet(email: String, password: String, recoveryPhrase: String, walletName: String): Completable =
        payloadDataManager.restoreHdWallet(
            recoveryPhrase,
            walletName,
            email,
            password
        )
            .flatMapCompletable { wallet ->
                Completable.fromCallable {
                    accessState.isNewlyCreated = true
                    accessState.isRestored = true
                    prefs.walletGuid = wallet.guid
                    prefs.sharedKey = wallet.sharedKey
                    prefs.setValue(PersistentPrefs.KEY_EMAIL, email)
                    prefs.setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, true)
                }
            }

    fun resetUserKyc(): Completable = nabuDataManager.resetUserKyc()
}