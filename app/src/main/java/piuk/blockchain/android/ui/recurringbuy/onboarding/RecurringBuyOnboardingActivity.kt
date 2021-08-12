package piuk.blockchain.android.ui.recurringbuy.onboarding

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.blockchain.notifications.analytics.Analytics
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityRecurringBuyOnBoardingBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.recurringbuy.RecurringBuyAnalytics
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class RecurringBuyOnboardingActivity : AppCompatActivity() {

    private val binding: ActivityRecurringBuyOnBoardingBinding by lazy {
        ActivityRecurringBuyOnBoardingBinding.inflate(layoutInflater)
    }

    private val analytics: Analytics by inject()
    private val assetCatalogue: AssetCatalogue by inject()

    private val fromCoinView: Boolean by unsafeLazy {
        intent?.getBooleanExtra(ORIGIN_ON_BOARDING_RBS, true) ?: true
    }

    private val asset: AssetInfo? by unsafeLazy {
        intent?.getStringExtra(ASSET)?.let {
            assetCatalogue.fromNetworkTicker(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showFullScreen()
        setContentView(binding.root)

        val recurringBuyOnBoardingPagerAdapter =
            RecurringBuyOnBoardingPagerAdapter(this, createListOfRecurringBuyInfo())

        with(binding) {
            viewpager.adapter = recurringBuyOnBoardingPagerAdapter
            indicator.setViewPager(viewpager)
            recurringBuyCta.apply {
                visibleIf { fromCoinView }
                recurringBuyCta.setOnClickListener {
                    goToRecurringSetUpScreen()
                    finish()
                }
            }
            closeBtn.setOnClickListener { finish() }
        }
        setupViewPagerListener()
    }

    private fun showFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    private fun showHeader(isShown: Boolean) {
        binding.apply {
            headerText.visibleIf { isShown }
        }
    }

    fun setupViewPagerListener() {
        binding.viewpager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                showHeader(position == 0)
                analytics.logEvent(RecurringBuyAnalytics.RecurringBuyInfoViewed(position))
                playLottieAnimationInterval(position)
            }
        })
    }

    private fun playLottieAnimationInterval(position: Int) {
        val minFrames = FRAMES_PER_SCREEN.times(position)
        val maxFrames = minFrames + FRAMES_PER_SCREEN
        binding.lottieAnimation.apply {
            setMinFrame(minFrames)
            setMaxFrame(maxFrames)
            playAnimation()
        }
    }

    private fun goToRecurringSetUpScreen() {
        startActivity(
            SimpleBuyActivity.newInstance(
                context = this,
                asset = asset
            )
        )
    }

    private fun createListOfRecurringBuyInfo(): List<RecurringBuyInfo> = listOf(
        RecurringBuyInfo(
            title1 = getString(R.string.recurring_buy_title_1_1),
            title2 = getString(R.string.recurring_buy_title_1_2)
        ),
        RecurringBuyInfo(
            title1 = getString(R.string.recurring_buy_title_2_1),
            title2 = getString(R.string.recurring_buy_title_2_2)
        ),
        RecurringBuyInfo(
            title1 = getString(R.string.recurring_buy_title_3_1),
            title2 = getString(R.string.recurring_buy_title_3_2)
        ),
        RecurringBuyInfo(
            title1 = getString(R.string.recurring_buy_title_4_1),
            title2 = getString(R.string.recurring_buy_title_4_2)
        ),
        RecurringBuyInfo(
            title1 = getString(R.string.recurring_buy_title_5_1),
            title2 = getString(R.string.recurring_buy_title_5_2),
            noteLink = R.string.recurring_buy_note
        )
    )

    override fun onBackPressed() {
        with(binding) {
            if (viewpager.currentItem == 0) {
                super.onBackPressed()
            } else {
                viewpager.currentItem = viewpager.currentItem - 1
            }
        }
    }

    companion object {
        private const val FRAMES_PER_SCREEN = 60
        private const val ORIGIN_ON_BOARDING_RBS = "FROM_COINVIEW"
        private const val ASSET = "ASSET"
        fun newInstance(
            context: Context,
            fromCoinView: Boolean,
            asset: AssetInfo? = null
        ): Intent = Intent(context, RecurringBuyOnboardingActivity::class.java).apply {
            putExtra(ORIGIN_ON_BOARDING_RBS, fromCoinView)
            putExtra(ASSET, asset?.ticker)
        }
    }
}