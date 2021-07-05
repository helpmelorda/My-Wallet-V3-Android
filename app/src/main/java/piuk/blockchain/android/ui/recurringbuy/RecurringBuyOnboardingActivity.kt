package piuk.blockchain.android.ui.recurringbuy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityRecurringBuyOnBoardingBinding
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.util.visibleIf

class RecurringBuyOnboardingActivity : AppCompatActivity() {

    private val binding: ActivityRecurringBuyOnBoardingBinding by lazy {
        ActivityRecurringBuyOnBoardingBinding.inflate(layoutInflater)
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
            recurringBuyCta.setOnClickListener { goToRecurringSetUpScreen() }
            closeBtn.setOnClickListener { finish() }
        }
        setupViewPagerListener()
    }

    private fun showFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    private fun showHeader(isShown: Boolean) {
        binding.apply {
            title.visibleIf { isShown }
            icon.visibleIf { isShown }
            headerBkgd.visibleIf { isShown }
            headerText.visibleIf { isShown }
        }
    }

    fun setupViewPagerListener() {
        binding.viewpager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                showHeader(position == 0)
            }
        })
    }

    private fun goToRecurringSetUpScreen() {
        startActivity(Intent(this, SimpleBuyActivity::class.java))
        finish()
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
}