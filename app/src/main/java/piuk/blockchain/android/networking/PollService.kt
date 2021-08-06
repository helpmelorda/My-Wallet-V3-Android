package piuk.blockchain.android.networking

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.withLatestFrom
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit

sealed class PollResult<T>(val value: T) {
    class FinalResult<T>(value: T) : PollResult<T>(value)
    class TimeOut<T>(value: T) : PollResult<T>(value)
    class Cancel<T>(value: T) : PollResult<T>(value)
}

class PollService<T : Any>(
    private val fetcher: Single<T>,
    private val matcher: (T) -> Boolean
) {
    val cancel = PublishSubject.create<Boolean>()

    fun start(timerInSec: Long = 5, retries: Int = 20) =
        fetcher.repeatWhen { it.delay(timerInSec, TimeUnit.SECONDS).zipWith(Flowable.range(0, retries)) }
            .toObservable()
            .withLatestFrom(cancel.startWithItem(false))
            .takeUntil { (value, canceled) ->
                matcher(value) || canceled
            }
            .lastOrError()
            .map { (value, canceled) ->
                if (canceled)
                    PollResult.Cancel(value)
                if (matcher(value))
                    PollResult.FinalResult(value)
                else
                    PollResult.TimeOut(value)
            }
}