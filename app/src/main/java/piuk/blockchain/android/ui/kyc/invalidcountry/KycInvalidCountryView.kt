package piuk.blockchain.android.ui.kyc.invalidcountry

import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.kyc.countryselection.util.CountryDisplayModel

interface KycInvalidCountryView : View {

    val displayModel: CountryDisplayModel

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun finishPage()
}