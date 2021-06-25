package piuk.blockchain.android.util

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable

private const val TINT_ALPHA = 0x26

fun ImageView.setImageDrawable(@DrawableRes res: Int) {
    setImageDrawable(context.getResolvedDrawable(res))
}

fun ImageView.setAssetIconColoursWithTint(asset: AssetInfo) {
    val main = tryParseColour(asset.colour)
    val tint = ColorUtils.setAlphaComponent(main, TINT_ALPHA)
    setAssetIconColours(tint, main)
}

fun ImageView.setAssetIconColoursNoTint(asset: AssetInfo) {
    val main = tryParseColour(asset.colour)
    val tint = context.getResolvedColor(R.color.white)
    setAssetIconColours(tint, main)
}

private fun ImageView.setAssetIconColours(@ColorInt tintColor: Int, @ColorInt filterColor: Int) {
    setBackgroundResource(R.drawable.bkgd_tx_circle)
    ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(tintColor))
    setColorFilter(filterColor)
}

fun ImageView.setTransactionHasFailed() =
    this.apply {
        setImageResource(R.drawable.ic_close)
        val tint = context.getResolvedColor(R.color.red_200)
        val main = context.getResolvedColor(R.color.red_600)
        setAssetIconColours(tint, main)
    }

fun ImageView.setTransactionIsConfirming() =
    this.apply {
        setImageResource(R.drawable.ic_tx_confirming)
        background = null
        setColorFilter(Color.TRANSPARENT)
    }

// We fetch the colour codes from the BE and, therefore, we shouldn't trust them
// to always parse correctly. If we cannot parse it, then fall back to black
private fun tryParseColour(colour: String): Int =
    try {
        Color.parseColor(colour)
    } catch (e: Throwable) {
        Color.parseColor("#000000")
    }
