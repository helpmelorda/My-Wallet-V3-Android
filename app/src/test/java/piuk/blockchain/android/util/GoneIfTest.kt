package piuk.blockchain.android.util

import android.view.View
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify

import org.junit.Test

class GoneIfTest {

    @Test
    fun `when function evaluates to true, the view is set to gone`() {
        mock<View>()
            .apply {
                goneIf { true }
                verify(this).visibility = View.GONE
            }
    }

    @Test
    fun `when function evaluates to false, the view is set to visible`() {
        mock<View>()
            .apply {
                goneIf { false }
                verify(this).visibility = View.VISIBLE
            }
    }

    @Test
    fun `when view is null, the function does not evaluate`() {
        mock<() -> Boolean> {
            onGeneric { invoke() }.thenReturn(true)
        }.apply {
            (null as View?).goneIf(this)
            verify(this, never()).invoke()
        }
    }

    @Test
    fun `when supplied true, the view is set to gone`() {
        mock<View>()
            .apply {
                goneIf(true)
                verify(this).visibility = View.GONE
            }
    }

    @Test
    fun `when supplied false, the view is set to visible`() {
        mock<View>()
            .apply {
                goneIf(false)
                verify(this).visibility = View.VISIBLE
            }
    }
}
