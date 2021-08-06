package com.blockchain.notifications.links

import android.content.Intent
import android.net.Uri
import io.reactivex.rxjava3.core.Maybe

interface PendingLink {

    fun getPendingLinks(intent: Intent): Maybe<Uri>
}