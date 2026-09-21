package com.splitmate.app.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class FrankfurterLatestResponse(
    val amount: Double = 1.0,
    @SerializedName(value = "base", alternate = ["base_code"])
    val base: String = "USD",
    @SerializedName(value = "date", alternate = ["time_last_update_utc"])
    val date: String = "",
    val rates: Map<String, Double> = emptyMap()
)

/**
 * Retrofit Service fetching 160+ live world currency conversion rates at runtime from
 * `https://open.er-api.com/v6/latest/USD`.
 */
interface FrankfurterApiService {

    @GET("latest/{baseCurrency}")
    suspend fun getLatestRates(
        @Path("baseCurrency") baseCurrency: String = "USD"
    ): FrankfurterLatestResponse

    suspend fun getSupportedCurrencies(): Map<String, String> = WorldCurrencyMetadata.namesMap
}

object FrankfurterNetwork {
    private const val BASE_URL = "https://open.er-api.com/v6/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    val api: FrankfurterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FrankfurterApiService::class.java)
    }
}

object WorldCurrencyMetadata {
    data class CurrencyInfo(val code: String, val name: String, val symbol: String)

    val allCurrencies: List<CurrencyInfo> = listOf(
        CurrencyInfo("INR", "Indian Rupee", "₹"),
        CurrencyInfo("USD", "United States Dollar", "$"),
        CurrencyInfo("EUR", "Euro", "€"),
        CurrencyInfo("GBP", "British Pound Sterling", "£"),
        CurrencyInfo("JPY", "Japanese Yen", "¥"),
        CurrencyInfo("AED", "UAE Dirham", "د.إ"),
        CurrencyInfo("AUD", "Australian Dollar", "A$"),
        CurrencyInfo("CAD", "Canadian Dollar", "C$"),
        CurrencyInfo("CHF", "Swiss Franc", "Fr"),
        CurrencyInfo("CNY", "Chinese Yuan", "¥"),
        CurrencyInfo("SGD", "Singapore Dollar", "S$"),
        CurrencyInfo("HKD", "Hong Kong Dollar", "HK$"),
        CurrencyInfo("NZD", "New Zealand Dollar", "NZ$"),
        CurrencyInfo("THB", "Thai Baht", "฿"),
        CurrencyInfo("MYR", "Malaysian Ringgit", "RM"),
        CurrencyInfo("IDR", "Indonesian Rupiah", "Rp"),
        CurrencyInfo("KRW", "South Korean Won", "₩"),
        CurrencyInfo("VND", "Vietnamese Dong", "₫"),
        CurrencyInfo("PHP", "Philippine Peso", "₱"),
        CurrencyInfo("BRL", "Brazilian Real", "R$"),
        CurrencyInfo("MXN", "Mexican Peso", "Mex$"),
        CurrencyInfo("ZAR", "South African Rand", "R"),
        CurrencyInfo("SAR", "Saudi Riyal", "﷼"),
        CurrencyInfo("QAR", "Qatari Riyal", "ر.ق"),
        CurrencyInfo("KWD", "Kuwaiti Dinar", "د.ك"),
        CurrencyInfo("OMR", "Omani Rial", "ر.ع."),
        CurrencyInfo("BHD", "Bahraini Dinar", ".د.ب"),
        CurrencyInfo("TRY", "Turkish Lira", "₺"),
        CurrencyInfo("SEK", "Swedish Krona", "kr"),
        CurrencyInfo("NOK", "Norwegian Krone", "kr"),
        CurrencyInfo("DKK", "Danish Krone", "kr"),
        CurrencyInfo("PLN", "Polish Zloty", "zł"),
        CurrencyInfo("CZK", "Czech Koruna", "Kč"),
        CurrencyInfo("HUF", "Hungarian Forint", "Ft"),
        CurrencyInfo("ILS", "Israeli New Shekel", "₪"),
        CurrencyInfo("EGP", "Egyptian Pound", "E£"),
        CurrencyInfo("NGN", "Nigerian Naira", "₦"),
        CurrencyInfo("KES", "Kenyan Shilling", "KSh"),
        CurrencyInfo("GHS", "Ghanaian Cedi", "₵"),
        CurrencyInfo("PKR", "Pakistani Rupee", "₨"),
        CurrencyInfo("BDT", "Bangladeshi Taka", "৳"),
        CurrencyInfo("LKR", "Sri Lankan Rupee", "Rs"),
        CurrencyInfo("NPR", "Nepalese Rupee", "रू"),
        CurrencyInfo("TWD", "New Taiwan Dollar", "NT$"),
        CurrencyInfo("ARS", "Argentine Peso", "$"),
        CurrencyInfo("CLP", "Chilean Peso", "$"),
        CurrencyInfo("COP", "Colombian Peso", "$"),
        CurrencyInfo("PEN", "Peruvian Sol", "S/"),
        CurrencyInfo("RUB", "Russian Ruble", "₽"),
        CurrencyInfo("UAH", "Ukrainian Hryvnia", "₴"),
        CurrencyInfo("RON", "Romanian Leu", "lei"),
        CurrencyInfo("BGN", "Bulgarian Lev", "лв"),
        CurrencyInfo("ISK", "Icelandic Króna", "kr"),
        CurrencyInfo("MAD", "Moroccan Dirham", "د.م."),
        CurrencyInfo("JOD", "Jordanian Dinar", "د.ا"),
        CurrencyInfo("KZT", "Kazakhstani Tenge", "₸"),
        CurrencyInfo("UZS", "Uzbekistani Som", "so'm"),
        CurrencyInfo("GEL", "Georgian Lari", "₾"),
        CurrencyInfo("AMD", "Armenian Dram", "֏"),
        CurrencyInfo("AZN", "Azerbaijani Manat", "₼")
    )

    val namesMap: Map<String, String> = allCurrencies.associate { it.code to it.name }
    val symbolsMap: Map<String, String> = allCurrencies.associate { it.code to it.symbol }

    fun symbolFor(code: String): String = symbolsMap[code.uppercase()] ?: code.take(2)
    fun nameFor(code: String): String = namesMap[code.uppercase()] ?: "$code Global Currency"
}
