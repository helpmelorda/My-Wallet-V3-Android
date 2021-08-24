package piuk.blockchain.android.util.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleInterestedComponent

class AppLifecycleListener(
    private val lifecycleInterestedComponent: LifecycleInterestedComponent
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        lifecycleInterestedComponent.appStateUpdated.onNext(AppState.FOREGROUNDED)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        lifecycleInterestedComponent.appStateUpdated.onNext(AppState.BACKGROUNDED)
    }
}
