package piuk.blockchain.android.ui.backup.start

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class BackupWalletStartingModelTest {
    private lateinit var model: BackupWalletStartingModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: BackupWalletStartingInteractor = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = BackupWalletStartingModel(
            initialState = BackupWalletStartingState(),
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `trigger alert successfully`() {
        whenever(interactor.triggerSeedPhraseAlert()).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(BackupWalletStartingIntents.TriggerEmailAlert)

        testState.assertValues(
            BackupWalletStartingState(),
            BackupWalletStartingState(status = BackupWalletStartingStatus.SENDING_ALERT),
            BackupWalletStartingState(status = BackupWalletStartingStatus.COMPLETE)
        )
    }
}