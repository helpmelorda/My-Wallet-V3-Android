package piuk.blockchain.androidcore.data.payload

import com.blockchain.android.testutils.rxInit
import com.blockchain.ui.password.SecondPasswordHandler
import com.blockchain.wallet.Seed
import com.blockchain.wallet.SeedAccess
import com.blockchain.wallet.SeedAccessWithoutPrompt
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.reactivex.rxjava3.core.Maybe

import com.nhaarman.mockitokotlin2.mock
import org.junit.Rule
import org.junit.Test

class PromptingSeedAccessAdapterTest {

    @get:Rule
    val initRx = rxInit {
        mainTrampoline()
    }

    @Test
    fun `seed prompt if required`() {
        val theSeed: Seed = mock()
        val seedAccessWithoutPrompt: SeedAccessWithoutPrompt = mock {
            on { seed }.thenReturn(Maybe.empty())
            on { seed(any()) }.thenReturn(Maybe.just(theSeed))
        }
        val secondPasswordHandler: SecondPasswordHandler = mock {
            on { hasSecondPasswordSet }.thenReturn(true)
            on { secondPassword() }.thenReturn(Maybe.just("ABCDEF"))
        }
        val seedAccess: SeedAccess = PromptingSeedAccessAdapter(seedAccessWithoutPrompt, secondPasswordHandler)

        seedAccess.seedPromptIfRequired.test()
            .assertValue(theSeed)
            .assertComplete()

        verify(seedAccessWithoutPrompt).seed("ABCDEF")
    }

    @Test
    fun `no seed prompt if not required`() {
        val theSeed: Seed = mock()
        val seedAccessWithoutPrompt: SeedAccessWithoutPrompt = mock {
            on { seed }.thenReturn(Maybe.just(theSeed))
        }
        val secondPasswordHandler: SecondPasswordHandler = mock {
            on { secondPassword() }.thenReturn(Maybe.empty())
        }
        val seedAccess: SeedAccess = PromptingSeedAccessAdapter(
            seedAccessWithoutPrompt,
            secondPasswordHandler
        )

        seedAccess.seedPromptIfRequired
            .test()
            .assertValue(theSeed)
            .assertComplete()

        verify(secondPasswordHandler).secondPassword()
        verifyNoMoreInteractions(secondPasswordHandler)
    }
}
