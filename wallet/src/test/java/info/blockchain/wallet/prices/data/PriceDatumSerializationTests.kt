package info.blockchain.wallet.prices.data

import com.fasterxml.jackson.databind.ObjectMapper
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class PriceDatumSerializationTests {

    @Test
    fun `can deserialize`() {
        val priceData = priceDatum("{\"timestamp\" : 1533555180, \"price\" : 6962.21, \"volume24h\" : 46215.61}")
        priceData.apply {
            timestamp `should be equal to` 1533555180L
            price `should be equal to` 6962.21
            volume24h `should be equal to` 46215.61
        }
    }
}

private fun priceDatum(json: String) = ObjectMapper().readValue(
    json, PriceDatum::class.java
)
