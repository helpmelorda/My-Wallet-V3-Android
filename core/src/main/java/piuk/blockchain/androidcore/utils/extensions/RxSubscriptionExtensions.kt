package piuk.blockchain.androidcore.utils.extensions

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeSource
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.annotations.SchedulerSupport
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.internal.functions.Functions
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException

/**
 * Subscribes to a [Maybe] and silently consumes any emitted values. Any exceptions thrown won't
 * cascade into a [OnErrorNotImplementedException], but will be signalled to the RxJava plugin
 * error handler. Note that this means that [RxJavaPlugins.setErrorHandler()] MUST be set.
 *
 * @return A [Disposable] object.
 */
fun <T> Maybe<T>.emptySubscribe(): Disposable =
    subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER)

/**
 * Subscribes to a [Maybe] and silently consumes any emitted values. Any exceptions thrown won't
 * cascade into a [OnErrorNotImplementedException], but will be signalled to the RxJava plugin
 * error handler. Note that this means that [RxJavaPlugins.setErrorHandler()] MUST be set.
 *
 * @return A [Disposable] object.
 */
fun <T> Single<T>.emptySubscribe(): Disposable =
    subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER)

/**
 * Subscribes to a [Flowable] and silently consumes any emitted values. Any exceptions thrown won't
 * cascade into a [OnErrorNotImplementedException], but will be signalled to the RxJava plugin
 * error handler. Note that this means that [RxJavaPlugins.setErrorHandler()] MUST be set.
 *
 * @return A [Disposable] object.
 */
fun <T> Flowable<T>.emptySubscribe(): Disposable =
    subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER)

/**
 * Subscribes to a [Observable] and silently consumes any emitted values. Any exceptions thrown won't
 * cascade into a [OnErrorNotImplementedException], but will be signalled to the RxJava plugin
 * error handler. Note that this means that [RxJavaPlugins.setErrorHandler()] MUST be set.
 *
 * @return A [Disposable] object.
 */
fun <T> Observable<T>.emptySubscribe(): Disposable =
    subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER)

/**
 * Subscribes to a [Completable] and silently completes, if applicable. Any exceptions thrown won't
 * cascade into a [OnErrorNotImplementedException], but will be signalled to the RxJava plugin
 * error handler. Note that this means that [RxJavaPlugins.setErrorHandler()] MUST be set.
 *
 * @return A [Disposable] object.
 */
fun Completable.emptySubscribe(): Disposable =
    subscribe(Functions.EMPTY_ACTION, Functions.ERROR_CONSUMER)

@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
fun <T, R> Maybe<T>.flatMapBy(
    onSuccess: (T) -> MaybeSource<out R>?,
    onError: (Throwable?) -> MaybeSource<out R>?,
    onComplete: () -> MaybeSource<out R>?
): Maybe<R> = this.flatMap(
    onSuccess,
    onError,
    onComplete
)

fun <T> Completable.thenSingle(block: () -> Single<T>): Single<T> =
    andThen(Single.defer { block() })

fun Completable.then(block: () -> Completable): Completable =
    andThen(Completable.defer { block() })

fun <T> Completable.thenMaybe(block: () -> Maybe<T>): Maybe<T> =
    andThen(Maybe.defer { block() })

fun <T, R> Observable<List<T>>.mapList(func: (T) -> R): Single<List<R>> =
    flatMapIterable { list ->
        list.map { func(it) }
    }.toList()

fun <T, R> Single<List<T>>.mapList(func: (T) -> R): Single<List<R>> =
    flattenAsObservable { list ->
        list.map { func(it) }
    }.toList()
