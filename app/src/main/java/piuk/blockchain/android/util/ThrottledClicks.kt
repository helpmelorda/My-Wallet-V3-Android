package piuk.blockchain.android.util

import android.view.View
import com.jakewharton.rxbinding4.view.clicks
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

fun View.throttledClicks(): Observable<Unit> =
    clicks().throttledClicks()

fun <U> Observable<U>.throttledClicks(): Observable<U> =
    throttleFirst(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
