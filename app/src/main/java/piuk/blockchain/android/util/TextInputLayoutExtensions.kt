package piuk.blockchain.android.util

import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.setErrorState(errorMessage: String) {
    isErrorEnabled = true
    error = errorMessage
}

fun TextInputLayout.clearErrorState() {
    error = ""
    isErrorEnabled = false
}