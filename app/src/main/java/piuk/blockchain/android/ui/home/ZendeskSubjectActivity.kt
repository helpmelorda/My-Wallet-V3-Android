package piuk.blockchain.android.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.nabu.BasicProfileInfo
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityZendeskSubjectBinding
import zendesk.chat.Chat
import zendesk.chat.ChatConfiguration
import zendesk.chat.ChatEngine
import zendesk.chat.ChatProvidersConfiguration
import zendesk.chat.PreChatFormFieldStatus
import zendesk.chat.VisitorInfo
import zendesk.messaging.MessagingActivity

class ZendeskSubjectActivity : AppCompatActivity() {

    private val binding: ActivityZendeskSubjectBinding by lazy {
        ActivityZendeskSubjectBinding.inflate(layoutInflater)
    }

    private val userInformation by lazy {
        intent.getSerializableExtra(USER_INFO) as BasicProfileInfo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setTitle(R.string.contact_support)
        setSupportActionBar(binding.toolbarGeneral.toolbarGeneral)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
        }

        Chat.INSTANCE.init(applicationContext, BuildConfig.ZENDESK_API_KEY)

        setChatVisitorInfo()

        with(binding) {
            zendeskOptions.setOnCheckedChangeListener { _, _ ->
                zendeskContinue.isEnabled = true
            }

            zendeskContinue.setOnClickListener {
                val checkedButton = findViewById<RadioButton>(zendeskOptions.checkedRadioButtonId)

                val provider = Chat.INSTANCE.providers()?.profileProvider()
                provider?.setVisitorNote(checkedButton.text.toString())

                startChat()
                finish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startChat() {
        MessagingActivity.builder()
            .withMultilineResponseOptionsEnabled(true)
            .withEngines(ChatEngine.engine())
            .withToolbarTitle(getString(R.string.zendesk_window_title))
            .show(this, getChatConfiguration())
    }

    private fun setChatVisitorInfo() {
        val visitorInfo = VisitorInfo.builder()
            .withName(userInformation.firstName)
            .withEmail(userInformation.email)
            .build()

        val chatProvidersConfiguration = ChatProvidersConfiguration.builder()
            .withVisitorInfo(visitorInfo)
            .withDepartment(ZENDESK_CHANNEL)
            .build()

        Chat.INSTANCE.chatProvidersConfiguration = chatProvidersConfiguration
    }

    private fun getChatConfiguration() = ChatConfiguration.builder()
        .withAgentAvailabilityEnabled(true)
        .withPreChatFormEnabled(false)
        .withDepartmentFieldStatus(PreChatFormFieldStatus.HIDDEN)
        .withOfflineFormEnabled(true)
        .withPhoneFieldStatus(PreChatFormFieldStatus.HIDDEN)
        .build()

    companion object {
        private const val USER_INFO = "USER_INFO"
        private const val ZENDESK_CHANNEL = "wallet_sb_department"

        fun newInstance(context: Context, userInfo: BasicProfileInfo): Intent =
            Intent(context, ZendeskSubjectActivity::class.java).apply {
                putExtra(USER_INFO, userInfo)
            }
    }
}