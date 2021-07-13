package piuk.blockchain.android.util

import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.payload.data.WalletBody
import info.blockchain.wallet.payload.data.Wallet
import org.amshove.kluent.`should be equal to`
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class BackupWalletUtilTest {

    private lateinit var subject: BackupWalletUtil
    private val payloadDataManager: PayloadDataManager = mock()

    @Before
    fun setUp() {
        subject = BackupWalletUtil(payloadDataManager)
    }

    @Test
    fun getConfirmSequence() {
        // Arrange
        val expectedMnemonic = listOf("one", "two", "three", "four")
        val hdwallet: WalletBody = mock {
            on { mnemonic }.thenReturn(expectedMnemonic)
        }

        val wallet: Wallet = mock {
            on { walletBody }.thenReturn(hdwallet)
        }

        whenever(payloadDataManager.wallet).thenReturn(wallet)

        // Act
        val result = subject.getConfirmSequence(null)

        // Assert
        verify(payloadDataManager, atLeastOnce()).wallet
        verifyNoMoreInteractions(payloadDataManager)
        result.size `should be equal to` 3
    }

    @Test
    fun `getMnemonic success`() {
        // Arrange
        val expectedMnemonic = listOf("one", "two", "three", "four")
        val hdwallet: WalletBody = mock {
            on { mnemonic }.thenReturn(expectedMnemonic)
        }

        val wallet: Wallet = mock {
            on { walletBody }.thenReturn(hdwallet)
        }

        whenever(payloadDataManager.wallet).thenReturn(wallet)

        // Act
        val result = subject.getMnemonic(null)

        // Assert
        verify(payloadDataManager).wallet
        verify(wallet).decryptHDWallet(null)
        verify(wallet).walletBody

        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(wallet)

        result `should be equal to` expectedMnemonic
    }

    @Test
    fun `getMnemonic error`() {
        // Arrange
        val wallet: Wallet = mock {
            on { decryptHDWallet(null) }.thenThrow(NullPointerException())
        }
        whenever(payloadDataManager.wallet).thenReturn(wallet)
        // Act
        val result = subject.getMnemonic(null)
        // Assert
        verify(payloadDataManager).wallet
        verifyNoMoreInteractions(payloadDataManager)
        verify(wallet).decryptHDWallet(null)
        verifyNoMoreInteractions(wallet)
        result `should be equal to` null
    }
}