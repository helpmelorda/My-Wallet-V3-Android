package piuk.blockchain.android.util

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.blockchain.koin.payloadScope
import org.koin.core.parameter.parametersOf

const val ACTIVITY_PARAMETER = "_param_activity"

inline fun <reified T : Any> AppCompatActivity.scopedInjectActivity(): Lazy<T> =
    payloadScope.inject(parameters = { parametersOf(toInjectionParameters()) })

inline fun <reified T : Any> Fragment.scopedInjectActivity(): Lazy<T> =
    payloadScope.inject(parameters = { parametersOf(this.requireActivity().toInjectionParameters()) })

fun AppCompatActivity.toInjectionParameters() = mapOf(ACTIVITY_PARAMETER to this)
fun FragmentActivity.toInjectionParameters() = mapOf(ACTIVITY_PARAMETER to this)
