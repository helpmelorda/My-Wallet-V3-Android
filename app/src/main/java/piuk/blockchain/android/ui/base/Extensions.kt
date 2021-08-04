package piuk.blockchain.android.ui.base

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import piuk.blockchain.android.R

fun FragmentActivity.setupToolbar(resource: Int, homeAsUpEnabled: Boolean = true) {
    (this as? BlockchainActivity)?.let {
        it.setupToolbar(
            it.supportActionBar ?: return,
            resource
        )
        it.supportActionBar?.setDisplayHomeAsUpEnabled(homeAsUpEnabled)
    }
}

fun FragmentActivity.setupToolbar(resource: String, homeAsUpEnabled: Boolean = true) {
    (this as? BlockchainActivity)?.let {
        it.setupToolbar(
            it.supportActionBar ?: return,
            resource
        )
        it.supportActionBar?.setDisplayHomeAsUpEnabled(homeAsUpEnabled)
    }
}

fun FragmentTransaction.addAnimationTransaction(): FragmentTransaction =
    this.setCustomAnimations(
        R.anim.fragment_slide_left_enter,
        R.anim.fragment_slide_left_exit,
        R.anim.fragment_slide_right_enter,
        R.anim.fragment_slide_right_exit
    )