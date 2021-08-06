package piuk.blockchain.android.ui.auth

import io.reactivex.rxjava3.core.Single

interface MobileNoticeRemoteConfig {
    fun mobileNoticeDialog(): Single<MobileNoticeDialog>
}