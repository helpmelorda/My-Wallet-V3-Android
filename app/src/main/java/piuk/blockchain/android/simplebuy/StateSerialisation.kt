package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import java.lang.reflect.Type

interface SimpleBuyPrefsSerializer {
    fun fetch(): SimpleBuyState?
    fun update(newState: SimpleBuyState)
    fun clear()
}

internal class SimpleBuyPrefsSerializerImpl(
    private val prefs: SimpleBuyPrefs,
    assetCatalogue: AssetCatalogue
) : SimpleBuyPrefsSerializer {

    private val gson = GsonBuilder()
        .registerTypeAdapter(AssetInfo::class.java, AssetTickerSerializer())
        .registerTypeAdapter(AssetInfo::class.java, AssetTickerDeserializer(assetCatalogue))
        .create()

    override fun fetch(): SimpleBuyState? =
        prefs.simpleBuyState()?.let {
            try {
                gson.fromJson(it, SimpleBuyState::class.java)
                    // When we de-serialise via gson the object fields get overwritten - including transients -
                    // so in order to have a valid object, we should re-init any non-nullable transient fields here
                    // or copy() operations will fail
                    .copy(
                        paymentOptions = PaymentOptions(),
                        isLoading = false,
                        shouldShowUnlockHigherFunds = false,
                        paymentPending = false,
                        confirmationActionRequested = false,
                        recurringBuyEligiblePaymentMethods = emptyList()
                    )
            } catch (t: Throwable) {
                prefs.clearBuyState()
                null
            }
        }

    override fun update(newState: SimpleBuyState) {
        val json = gson.toJson(newState)
        prefs.updateSimpleBuyState(json)
    }

    override fun clear() {
        prefs.clearBuyState()
    }
}

private class AssetTickerSerializer : JsonSerializer<AssetInfo> {
    override fun serialize(
        src: AssetInfo,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement = JsonPrimitive(src.ticker)
}

private class AssetTickerDeserializer(
    val assetCatalogue: AssetCatalogue
) : JsonDeserializer<AssetInfo> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): AssetInfo = assetCatalogue.fromNetworkTicker(
        json.asString
    ) ?: throw JsonParseException("Unknown Asset ticker")
}
