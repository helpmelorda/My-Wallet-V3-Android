package piuk.blockchain.android.ui.base

import android.content.Context
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.util.ActivityIndicator
import piuk.blockchain.android.util.AppUtil

open class ViewPagerFragment: Fragment() {

    protected var activityIndicator = ActivityIndicator()

    private val appUtil: AppUtil by inject()
    private val compositeDisposable = CompositeDisposable()
    private var mActivity: BlockchainActivity? = null

    override fun onResume() {
        super.onResume()
        if (mActivity == null) return
        compositeDisposable += activityIndicator.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                if (it == true) {
                    mActivity?.showLoading()
                } else {
                    mActivity?.hideLoading()
                }
            }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as? BlockchainActivity
    }

    override fun onPause() {
        super.onPause()
        mActivity?.hideLoading()
        compositeDisposable.clear()
    }

}