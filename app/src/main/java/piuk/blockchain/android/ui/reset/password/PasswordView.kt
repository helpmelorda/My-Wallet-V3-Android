package piuk.blockchain.android.ui.reset.password

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import info.blockchain.wallet.util.PasswordUtil
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewPasswordBinding
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.android.util.clearErrorState
import piuk.blockchain.android.util.setErrorState
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import kotlin.math.roundToInt

class PasswordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr), KoinComponent {

    private val environmentConfig: EnvironmentConfig by inject()

    private val binding: ViewPasswordBinding = ViewPasswordBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        with(binding) {
            passwordText.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable) {
                    passwordTextLayout.clearErrorState()
                    passwordStrength.visible()
                    passwordStrength.updatePassword(s.toString())
                }
            })

            confirmPasswordText.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable) {
                    confirmPasswordTextLayout.clearErrorState()
                }
            })
        }
    }

    fun isPasswordValid(): Boolean {
        with(binding) {
            val password = passwordText.text?.toString() ?: ""
            val confirmedPassword = confirmPasswordText.text?.toString() ?: ""
            val minPasswordStrength: Int = if (environmentConfig.isRunningInDebugMode()) {
                DEBUG_PASSWORD_STRENGTH
            } else {
                MIN_PASSWORD_STRENGTH
            }
            return when {
                password != confirmedPassword -> {
                    confirmPasswordTextLayout.setErrorState(
                        context.resources.getString(R.string.password_mismatch_error)
                    )
                    false
                }
                password.length < MIN_LENGTH -> {
                    passwordTextLayout.setErrorState(context.resources.getString(R.string.invalid_password_too_short))
                    false
                }
                PasswordUtil.getStrength(password).roundToInt() < minPasswordStrength -> {
                    passwordTextLayout.setErrorState(context.resources.getString(R.string.weak_password))
                    false
                }
                else -> true
            }
        }
    }

    fun getEnteredPassword() = binding.passwordText.text?.toString() ?: ""

    companion object {
        private const val MIN_LENGTH = 4
        private const val DEBUG_PASSWORD_STRENGTH = 1
        private const val MIN_PASSWORD_STRENGTH = 50
    }
}