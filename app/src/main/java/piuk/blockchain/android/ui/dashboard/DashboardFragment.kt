package piuk.blockchain.android.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.databinding.FragmentDashboardBinding
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class DashboardFragment : Fragment() {

    private val startingView: DashboardViewType by unsafeLazy {
        arguments?.getSerializable(PARAM_START_VIEW) as? DashboardViewType ?: DashboardViewType.TYPE_PORTFOLIO
    }

    private var _binding: FragmentDashboardBinding? = null

    private val binding: FragmentDashboardBinding
        get() = _binding!!

    private val flowToLaunch: AssetAction? by unsafeLazy {
        arguments?.getSerializable(PortfolioFragment.FLOW_TO_LAUNCH) as? AssetAction
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val flowCurrency: String? by unsafeLazy {
        arguments?.getString(PortfolioFragment.FLOW_FIAT_CURRENCY)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            dashboardTabs.setupWithViewPager(dashboardPager)
            dashboardPager.adapter = DashboardPagerAdapter(
                listOf(getString(R.string.portfolio), getString(R.string.prices)),
                childFragmentManager,
                flowToLaunch,
                flowCurrency
            )

            dashboardPager.setCurrentItem(
                when (startingView) {
                    DashboardViewType.TYPE_PORTFOLIO -> DashboardViewType.TYPE_PORTFOLIO.ordinal
                    DashboardViewType.TYPE_PRICES -> DashboardViewType.TYPE_PRICES.ordinal
                }, true
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val PARAM_START_VIEW = "show_view"

        fun newInstance(
            dashboardViewType: DashboardViewType = DashboardViewType.TYPE_PORTFOLIO
        ): DashboardFragment =
            DashboardFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(PARAM_START_VIEW, dashboardViewType)
                }
            }

        fun newInstance(flowToLaunch: AssetAction?, fiatCurrency: String?) =
            DashboardFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(PARAM_START_VIEW, DashboardViewType.TYPE_PORTFOLIO)
                    if (flowToLaunch != null && fiatCurrency != null) {
                        putSerializable(PortfolioFragment.FLOW_TO_LAUNCH, flowToLaunch)
                        putString(PortfolioFragment.FLOW_FIAT_CURRENCY, fiatCurrency)
                    }
                }
            }
    }

    enum class DashboardViewType {
        TYPE_PORTFOLIO,
        TYPE_PRICES
    }
}

class DashboardPagerAdapter(
    private val titlesList: List<String>,
    fragmentManager: FragmentManager,
    private val flowToLaunch: AssetAction? = null,
    private val flowCurrency: String? = null
) : FragmentPagerAdapter(fragmentManager) {

    private val dashboardFragment: Fragment by unsafeLazy {
        if (flowToLaunch != null && flowCurrency != null) {
            PortfolioFragment.newInstance(flowToLaunch, flowCurrency)
        } else {
            PortfolioFragment.newInstance()
        }
    }

    private val pricesFragment: Fragment by unsafeLazy {
        PricesFragment.newInstance()
    }

    override fun getCount(): Int = titlesList.size
    override fun getPageTitle(position: Int): CharSequence =
        titlesList[position]

    override fun getItem(position: Int): Fragment =
        when (position) {
            0 -> dashboardFragment
            else -> pricesFragment
        }
}
