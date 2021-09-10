package piuk.blockchain.android.data.biometrics

import android.content.Context
import androidx.appcompat.app.AlertDialog
import piuk.blockchain.android.R

class BiometricPromptUtil {
    /**
     * Reusable static functions for displaying different Biometrics error states
     */
    companion object {
        fun showAuthLockoutDialog(context: Context) {
            AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(R.string.biometrics_disabled_lockout_title)
                .setMessage(R.string.biometrics_disabled_lockout_desc)
                .setCancelable(false)
                .setPositiveButton(R.string.common_ok) { di, _ ->
                    di.dismiss()
                }
                .show()
        }

        fun showPermanentAuthLockoutDialog(context: Context) {
            AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(R.string.biometrics_disabled_lockout_title)
                .setMessage(R.string.biometrics_disabled_lockout_perm_desc)
                .setCancelable(false)
                .setPositiveButton(R.string.common_ok) { di, _ ->
                    di.dismiss()
                }
                .show()
        }

        fun showActionableInvalidatedKeysDialog(
            context: Context,
            positiveActionCallback: () -> Unit,
            negativeActionCallback: () -> Unit
        ) {
            AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.biometrics_key_invalidated_settings_description)
                .setCancelable(false)
                .setPositiveButton(R.string.common_try_again) { _, _ ->
                    positiveActionCallback.invoke()
                }
                .setNegativeButton(R.string.biometrics_action_settings) { di, _ ->
                    di.dismiss()
                    negativeActionCallback.invoke()
                }
                .show()
        }

        fun showInfoInvalidatedKeysDialog(
            context: Context
        ) {
            AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(R.string.biometrics_key_invalidated_title)
                .setMessage(R.string.biometrics_key_invalidated_description)
                .setCancelable(false)
                .setPositiveButton(
                    R.string.fingerprint_use_pin
                ) { di, _ -> di.dismiss() }
                .show()
        }

        fun showBiometricsGenericError(
            context: Context,
            error: String = ""
        ) {
            AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(R.string.fingerprint_fatal_error_brief)
                .setMessage(context.getString(R.string.fingerprint_fatal_error_desc, error))
                .setCancelable(false)
                .setPositiveButton(
                    R.string.fingerprint_use_pin
                ) { di, _ -> di.dismiss() }
                .create()
                .show()
        }
    }
}