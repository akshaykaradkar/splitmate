package com.splitmate.app.data

import java.util.Locale

/**
 * v2.3.5 (#1): generic place normalisation for trip-return detection.
 *
 * Maps Indian Railways station codes, IATA airport codes and common city spellings to one
 * canonical city key, so a trip that leaves from `LTT` (train) and comes back to `CSMT` or `BOM`
 * (flight) is recognised as "back in Mumbai". Pure (JVM-testable); no trip-specific logic.
 *
 * Unknown inputs fall back to their uppercased, trimmed code, so two identical unknown codes
 * still match each other.
 */
object TravelPlaceAliases {

    private val CITY_ALIASES: Map<String, List<String>> = mapOf(
        "MUMBAI" to listOf(
            "MUMBAI", "BOMBAY", "BOM", "CSMT", "CST", "CSTM", "LTT", "DR", "DADAR", "BCT", "MMCT",
            "BDTS", "BA", "ADH", "BVI", "BORIVALI", "TNA", "THANE", "PNVL", "PANVEL", "KYN", "KALYAN",
            "NAVI MUMBAI", "MUMBAI CENTRAL", "LOKMANYA TILAK", "BANDRA", "BANDRA TERMINUS"
        ),
        "DELHI" to listOf(
            "DELHI", "NEW DELHI", "NDLS", "NZM", "DLI", "ANVT", "DEE", "DSA", "DEL", "HAZRAT NIZAMUDDIN",
            "ANAND VIHAR", "OLD DELHI", "GURGAON", "GURUGRAM", "GGN", "NOIDA"
        ),
        "BENGALURU" to listOf(
            "BENGALURU", "BANGALORE", "BENGALURU CITY", "SBC", "YPR", "YESVANTPUR", "BNC", "KJM", "SMVB",
            "BLR", "KSR BENGALURU", "BANGALORE CANTT"
        ),
        "CHENNAI" to listOf(
            "CHENNAI", "MADRAS", "MAS", "MS", "MSB", "TBM", "TAMBARAM", "MAA", "CHENNAI CENTRAL",
            "CHENNAI EGMORE", "EGMORE"
        ),
        "KOLKATA" to listOf(
            "KOLKATA", "CALCUTTA", "HWH", "HOWRAH", "SDAH", "SEALDAH", "KOAA", "SHM", "SHALIMAR", "CCU"
        ),
        "PUNE" to listOf("PUNE", "POONA", "PNQ", "PUNE JN", "SVJR", "HDP", "HADAPSAR", "KK", "KHADKI"),
        "HYDERABAD" to listOf(
            "HYDERABAD", "SECUNDERABAD", "HYB", "SC", "KCG", "KACHEGUDA", "LPI", "LINGAMPALLI", "HYD",
            "HYDERABAD DECCAN"
        ),
        "GOA" to listOf(
            "GOA", "MADGAON", "MARGAO", "MAO", "MDGN", "KRMI", "KARMALI", "VSG", "VASCO", "VASCO DA GAMA",
            "THVM", "THIVIM", "PERNEM", "PERN", "GOI", "GOX", "DABOLIM", "MOPA", "PANAJI", "PANJIM"
        ),
        "AHMEDABAD" to listOf("AHMEDABAD", "ADI", "SBT", "SABARMATI", "MAN", "MANINAGAR", "AMD"),
        "JAIPUR" to listOf("JAIPUR", "JP", "GADJ", "GANDHINAGAR JAIPUR", "DPA", "JAI"),
        "LUCKNOW" to listOf("LUCKNOW", "LKO", "LJN", "GTNR", "GOMTI NAGAR"),
        "VARANASI" to listOf("VARANASI", "BANARAS", "BENARES", "BSB", "BSBS", "MUV", "DDU", "VNS"),
        "KOCHI" to listOf("KOCHI", "COCHIN", "ERS", "ERN", "ERNAKULAM", "AWY", "ALUVA", "COK"),
        "THIRUVANANTHAPURAM" to listOf("THIRUVANANTHAPURAM", "TRIVANDRUM", "TVC", "TVCN", "KCVL", "TRV"),
        "CHANDIGARH" to listOf("CHANDIGARH", "CDG", "IXC", "MOHALI", "PANCHKULA"),
        "BHOPAL" to listOf("BHOPAL", "BPL", "RKMP", "RANI KAMALAPATI", "HBJ", "BHO"),
        "INDORE" to listOf("INDORE", "INDB", "IDR"),
        "NAGPUR" to listOf("NAGPUR", "NGP", "AJNI"),
        "PATNA" to listOf("PATNA", "PNBE", "RJPB", "PPTA", "DNR", "PAT"),
        "BHUBANESWAR" to listOf("BHUBANESWAR", "BBS", "BHUBANESHWAR"),
        "GUWAHATI" to listOf("GUWAHATI", "GHY", "KYQ", "GAU"),
        "VISAKHAPATNAM" to listOf("VISAKHAPATNAM", "VIZAG", "VSKP", "VTZ"),
        "MYSURU" to listOf("MYSURU", "MYSORE", "MYS"),
        "MANGALURU" to listOf("MANGALURU", "MANGALORE", "MAQ", "MAJN", "IXE"),
        "COIMBATORE" to listOf("COIMBATORE", "CBE", "PTJ"),
        "MADURAI" to listOf("MADURAI", "MDU", "IXM"),
        "AMRITSAR" to listOf("AMRITSAR", "ASR", "ATQ"),
        "UDAIPUR" to listOf("UDAIPUR", "UDZ", "UDR"),
        "JODHPUR" to listOf("JODHPUR", "JU", "JDH"),
        "AGRA" to listOf("AGRA", "AGC", "AF", "IDH", "AGR"),
        "DEHRADUN" to listOf("DEHRADUN", "DDN", "DED"),
        "SURAT" to listOf("SURAT", "ST", "UDN", "STV"),
        "VADODARA" to listOf("VADODARA", "BARODA", "BRC", "BDQ"),
        "NASHIK" to listOf("NASHIK", "NASIK", "NK", "ISK"),
        "AURANGABAD" to listOf("AURANGABAD", "CHHATRAPATI SAMBHAJINAGAR", "AWB", "IXU"),
        "RATNAGIRI" to listOf("RATNAGIRI", "RN"),
        "KOLHAPUR" to listOf("KOLHAPUR", "KOP", "KLH"),
        "RAIPUR" to listOf("RAIPUR", "R", "RPR"),
        "RANCHI" to listOf("RANCHI", "RNC", "HTE", "IXR"),
        "JAMMU" to listOf("JAMMU", "JAT", "IXJ"),
        "SRINAGAR" to listOf("SRINAGAR", "SINA", "SXR"),
        "TIRUPATI" to listOf("TIRUPATI", "TPTY", "RU", "RENIGUNTA", "TIR"),
        "VIJAYAWADA" to listOf("VIJAYAWADA", "BZA"),
        "HAMPI" to listOf("HAMPI", "HOSAPETE", "HOSPET", "HPT")
    )

    private val ALIAS_TO_CITY: Map<String, String> = buildMap {
        CITY_ALIASES.forEach { (city, aliases) ->
            aliases.forEach { alias -> put(normaliseToken(alias), city) }
        }
    }

    private fun normaliseToken(raw: String): String =
        raw.trim()
            .uppercase(Locale.US)
            .replace(Regex("""[^A-Z0-9 ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    /**
     * Canonical city key for a station code / airport code / city name.
     * Handles decorated inputs such as `"CSMT (Mumbai CSMT)"`, `"Pune Jn"` or `"BOM T2"`.
     * Unknown inputs fall back to their uppercased, trimmed token. Blank input -> "".
     */
    fun canonicalCity(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val token = normaliseToken(raw)
        if (token.isEmpty()) return ""
        ALIAS_TO_CITY[token]?.let { return it }
        // "CSMT (Mumbai CSMT)" -> try the leading code, then the bracketed name. Only for decorated
        // inputs, so a plain name such as "DR AMBEDKAR NAGAR" never matches the "DR" (Dadar) code.
        val bracket = Regex("""\(([^)]*)\)""").find(raw)?.groupValues?.getOrNull(1)
        if (bracket != null) {
            val leading = normaliseToken(raw.substringBefore('(')).substringBefore(' ')
            ALIAS_TO_CITY[leading]?.let { return it }
        }
        if (!bracket.isNullOrBlank()) {
            val inner = normaliseToken(bracket)
            ALIAS_TO_CITY[inner]?.let { return it }
            inner.substringBefore(' ').takeIf { it.length >= 4 }?.let { word -> ALIAS_TO_CITY[word]?.let { return it } }
        }
        // Strip common suffixes ("PUNE JN", "AGRA CANTT", "JAIPUR JUNCTION").
        val stripped = token
            .replace(Regex("""\b(JN|JUNCTION|CANTT|CANTONMENT|TERMINUS|TERMINAL|STATION|AIRPORT|INTL|INTERNATIONAL|CITY|T[1-3])\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (stripped.isNotEmpty()) {
            ALIAS_TO_CITY[stripped]?.let { return it }
        }
        return raw.trim().uppercase(Locale.US)
    }

    /** True when both places are non-blank and normalise to the same canonical city. */
    fun isSameCity(a: String?, b: String?): Boolean {
        val ca = canonicalCity(a)
        val cb = canonicalCity(b)
        return ca.isNotEmpty() && ca == cb
    }
}
