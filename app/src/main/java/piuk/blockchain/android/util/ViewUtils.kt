package piuk.blockchain.android.util

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.annotation.IntDef

object ViewUtils {
    /**
     * Converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp to convert to pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    private fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    /**
     * Returns a properly padded FrameLayout which wraps a [View]. Once wrapped,
     * the view will conform to the Material Design guidelines for spacing within a Dialog.
     *
     * @param context The current Activity or Fragment context
     * @param view A [View] that you wish to wrap
     * @return A correctly padded FrameLayout containing the AppCompatEditText
     */
    fun getAlertDialogPaddedView(context: Context, view: View?): FrameLayout {
        val frameLayout = FrameLayout(context)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val marginInPixels = convertDpToPixel(20f, context).toInt()
        params.setMargins(marginInPixels, 0, marginInPixels, 0)
        frameLayout.addView(view, params)
        return frameLayout
    }

    /**
     * Hides the keyboard in a specified [AppCompatActivity]
     *
     * @param activity The Activity in which you want to hide the keyboard
     */
    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * These annotations are hidden in the Android Jar for some reason. Defining them here instead
     * for use in [piuk.blockchain.androidcoreui.ui.base.View] interfaces etc.
     */
    @IntDef(View.VISIBLE, View.INVISIBLE, View.GONE)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class Visibility
}

val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()