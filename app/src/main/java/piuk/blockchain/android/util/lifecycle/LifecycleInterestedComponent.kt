package piuk.blockchain.android.util.lifecycle

import io.reactivex.rxjava3.subjects.PublishSubject

class LifecycleInterestedComponent {
    val appStateUpdated: PublishSubject<AppState> = PublishSubject.create()
}