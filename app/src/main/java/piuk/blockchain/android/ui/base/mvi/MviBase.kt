package piuk.blockchain.android.ui.base.mvi

import androidx.annotation.CallSuper
import com.blockchain.logging.CrashLogger
import com.jakewharton.rxrelay3.BehaviorRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.ReplaySubject

import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

interface MviState

interface MviIntent<S : MviState> {
    fun reduce(oldState: S): S
    fun isValidFor(oldState: S): Boolean = true
}

abstract class MviModel<S : MviState, I : MviIntent<S>>(
    initialState: S,
    uiScheduler: Scheduler,
    private val environmentConfig: EnvironmentConfig,
    private val crashLogger: CrashLogger
) {

    private val _state: BehaviorRelay<S> = BehaviorRelay.createDefault(initialState)
    val state: Observable<S> = _state.distinctUntilChanged()
        .doOnNext {
            onStateUpdate(it)
        }.observeOn(uiScheduler)

    protected val disposables = CompositeDisposable()

    // In principle, "intents" could - and should - be a PublishSubject.
    // However, we use Koin for DI, which is lazy and doesn't create injected objects until they are first
    // accessed. So, if the UI code issues Intents early in it's lifecycle (ie in, say, onCreate()) then the model may
    // only be created at the point - as an Intent is issued via process() - and we can end up in a race between
    // the arrival of the first intent and the set up of the main processing rx chain.
    // When that happens if the intent chain loses, the initial Intent gets dropped and that's that.
    // To mitigate that, we use a ReplaySubject with a small buffer, so that if the init()
    // happens after the process() from the UI, we still see those initial Intents.
    private val intents = ReplaySubject.create<I>(5)

    // A consequence of using a ReplaySubject, is that sometimes, when we are processing a lot of Intents rapidly from
    // from multiple threads, it can get confused and miss some. We see this as an IndexOutOfRange exception which can
    // stall, or stop, the intent flow. Leading to a jammed or glitching UI/UX
    // To mitigate this, we use another subject to marshal all the inbound Intents on to the same, known, thread.
    // See: https://github.com/ReactiveX/RxJava/issues/1029 for an (older) explanation.
    private val threadProxy = PublishSubject.create<I>()
        .apply {
            disposables += observeOn(uiScheduler)
                .subscribe {
                    intents.onNext(it)
                }
        }

    init {
        disposables +=
            intents.distinctUntilChanged(::distinctIntentFilter)
                .observeOn(Schedulers.io())
                .scan(initialState) { previousState, intent ->
                    Timber.d("***> Model: ProcessIntent: ${intent.javaClass.simpleName}")
                    if (intent.isValidFor(previousState)) {
                        performAction(previousState, intent)?.let { disposables += it }
                        intent.reduce(previousState)
                    } else {
                        Timber.d("***> Model: Dropping invalid Intent: ${intent.javaClass.simpleName}")
                        previousState
                    }
                }
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onNext = { newState ->
                        _state.accept(newState)
                    },
                    onError = ::onScanLoopError
                )
    }

    fun process(intent: I) = threadProxy.onNext(intent)

    fun destroy() {
        disposables.clear()
    }

    protected open fun distinctIntentFilter(previousIntent: I, nextIntent: I): Boolean {
        return previousIntent == nextIntent
    }

    @CallSuper
    protected open fun onScanLoopError(t: Throwable) {
        Timber.e("***> Scan loop failed: $t")
        crashLogger.logException(t)
        if (environmentConfig.isRunningInDebugMode()) {
            throw t
        }
    }

    protected open fun onStateUpdate(s: S) {}

    protected abstract fun performAction(previousState: S, intent: I): Disposable?
}
