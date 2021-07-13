@file:JvmName("RxSchedulingExtensions")

package piuk.blockchain.androidcore.utils.extensions

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

/**
 * Applies standard Schedulers to an [Observable], ie IO for subscription, Main Thread for
 * onNext/onComplete/onError
 */
fun <T> Observable<T>.applySchedulers(): Observable<T> =
    this.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError(Timber::e)

/**
 * Applies standard Schedulers to a [Single], ie IO for subscription, Main Thread for
 * onNext/onComplete/onError
 */
fun <T> Single<T>.applySchedulers(): Single<T> =
    this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnError(Timber::e)

/**
 * Applies standard Schedulers to a [Completable], ie IO for subscription,
 * Main Thread for onNext/onComplete/onError
 */
fun Completable.applySchedulers(): Completable =
    this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnError(Timber::e)

/**
 * Applies standard Schedulers to an [Observable], ie IO for subscription, Main Thread for
 * onNext/onComplete/onError
 */
fun <T> Maybe<T>.applySchedulers(): Maybe<T> =
    this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnError(Timber::e)