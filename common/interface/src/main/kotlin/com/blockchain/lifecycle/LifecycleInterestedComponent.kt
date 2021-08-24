package com.blockchain.lifecycle

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class LifecycleInterestedComponent : LifecycleObservable {
    val appStateUpdated: PublishSubject<AppState> = PublishSubject.create()

    override val onStateUpdated: Observable<AppState>
        get() = appStateUpdated
}

interface LifecycleObservable {
    val onStateUpdated: Observable<AppState>
}

enum class AppState {
    FOREGROUNDED, BACKGROUNDED
}