package piuk.blockchain.android.ui.backup.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.util.scopedInjectActivity
import com.blockchain.ui.password.SecondPasswordHandler
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentBackupStartBinding
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment
import piuk.blockchain.android.ui.base.mvi.MviFragment

class BackupWalletStartingFragment :
    MviFragment<BackupWalletStartingModel,
                BackupWalletStartingIntents,
                BackupWalletStartingState,
                FragmentBackupStartBinding>() {

    private val secondPasswordHandler: SecondPasswordHandler by scopedInjectActivity()

    override val model: BackupWalletStartingModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBackupStartBinding =
        FragmentBackupStartBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStart.setOnClickListener {
            model.process(BackupWalletStartingIntents.TriggerEmailAlert)
            secondPasswordHandler.validate(
                requireContext(),
                object : SecondPasswordHandler.ResultListener {
                    override fun onNoSecondPassword() {
                        loadFragmentWordListFragment()
                    }

                    override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                        loadFragmentWordListFragment(validatedSecondPassword)
                    }
                }
            )
        }
    }

    override fun render(newState: BackupWalletStartingState) {}

    private fun loadFragmentWordListFragment(secondPassword: String? = null) {
        val fragment = BackupWalletWordListFragment().apply {
            secondPassword?.let {
                arguments = Bundle().apply {
                    putString(
                        BackupWalletWordListFragment.ARGUMENT_SECOND_PASSWORD,
                        it
                    )
                }
            }
        }
        activity.run {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        const val TAG = "BackupWalletStartingFragment"
    }
}