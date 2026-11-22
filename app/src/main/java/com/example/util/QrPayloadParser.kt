package com.example.util

import java.net.URLDecoder
import java.util.Locale

enum class QrPayloadKind {
    WIFI,
    VCARD,
    URL,
    EMAIL,
    PHONE,
    SMS,
    GEO,
    CALENDAR,
    BARCODE,
    TEXT
}

data class QrDetailField(
    val label: String,
    val value: String,
    val isSensitive: Boolean = false,
    val copyValue: String = value
)

data class WifiDetails(
    val ssid: String,
    val password: String,
    val security: String,
    val securityDisplay: String,
    val isHidden: Boolean = false
)

data class ContactDetails(
    val name: String? = null,
    val organization: String? = null,
    val jobTitle: String? = null,
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val note: String? = null
)

data class CalendarDetails(
    val summary: String? = null,
    val location: String? = null,
    val start: String? = null,
    val end: String? = null,
    val description: String? = null
)

data class ParsedQrPayload(
    val kind: QrPayloadKind,
    val badgeTitle: String,
    val primaryTitle: String,
    val fields: List<QrDetailField>,
    val wifiInfo: WifiDetails? = null,
    val contactInfo: ContactDetails? = null,
    val calendarDetails: CalendarDetails? = null,
    val primaryPhone: String? = null,
    val primaryEmail: String? = null,
    val primaryUrl: String? = null,
    val emailSubject: String? = null,
    val emailBody: String? = null,
    val smsMessage: String? = null,
    val geoLat: String? = null,
    val geoLng: String? = null,
    val geoQuery: String? = null,
    val rawContent: String,
    val formattedCopyAll: String
)

object QrPayloadParser {

    private val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val urlRegex = Regex("(?i)\\b((?:https?://|www\\.)[^\\s,;\"'<>]+)")
    private val phoneRegex = Regex("(?:\\+?\\d[\\d\\s\\-().]{5,17}\\d)")

    fun detectPayloadTypeCode(rawContent: String, isBarcode: Boolean): String {
        if (isBarcode) return "BARCODE"
        return when (parse(rawContent, isBarcode = false).kind) {
            QrPayloadKind.WIFI -> "WIFI"
            QrPayloadKind.VCARD -> "VCARD"
            QrPayloadKind.URL -> "URL"
            QrPayloadKind.EMAIL -> "EMAIL"
            QrPayloadKind.PHONE -> "PHONE"
            QrPayloadKind.SMS -> "SMS"
            QrPayloadKind.GEO -> "GEO"
            QrPayloadKind.CALENDAR -> "CALENDAR"
            QrPayloadKind.BARCODE -> "BARCODE"
            QrPayloadKind.TEXT -> "TEXT"
        }
    }

    fun parse(rawContent: String, payloadHint: String? = null, isBarcode: Boolean = false): ParsedQrPayload {
        val trimmed = rawContent.trim()
        if (isBarcode || payloadHint == "BARCODE") {
            return buildBarcodePayload(trimmed)
        }

        return when {
            trimmed.startsWith("WIFI:", ignoreCase = true) -> parseWifi(trimmed)
            trimmed.contains("BEGIN:VCARD", ignoreCase = true) -> parseVCard(trimmed)
            trimmed.startsWith("MECARD:", ignoreCase = true) -> parseMeCard(trimmed)
            trimmed.contains("BEGIN:VEVENT", ignoreCase = true) -> parseCalendarEvent(trimmed)
            trimmed.startsWith("MATMSG:", ignoreCase = true) -> parseMatMsgEmail(trimmed)
            trimmed.startsWith("mailto:", ignoreCase = true) -> parseMailto(trimmed)
            trimmed.startsWith("SMSTO:", ignoreCase = true) || trimmed.startsWith("sms:", ignoreCase = true) -> parseSms(trimmed)
            trimmed.startsWith("tel:", ignoreCase = true) -> parseTel(trimmed)
            trimmed.startsWith("geo:", ignoreCase = true) -> parseGeo(trimmed)
            isSingleUrl(trimmed) -> parseUrl(trimmed)
            isSingleEmail(trimmed) -> parseMailto("mailto:$trimmed")
            isSinglePhone(trimmed) -> parseTel("tel:$trimmed")
            else -> parseSmartTextOrCard(trimmed)
        }
    }

    private fun parseWifi(raw: String): ParsedQrPayload {
        val body = raw.substring(5)
        val tokens = splitUnescaped(body, ';')
        var ssid = ""
        var password = ""
        var security = ""
        var isHidden = false

        for (token in tokens) {
            val t = token.trim()
            if (t.isEmpty()) continue
            when {
                t.startsWith("S:", ignoreCase = true) -> ssid = unescapeWifi(t.substring(2))
                t.startsWith("P:", ignoreCase = true) -> password = unescapeWifi(t.substring(2))
                t.startsWith("T:", ignoreCase = true) -> security = unescapeWifi(t.substring(2))
                t.startsWith("H:", ignoreCase = true) -> {
                    val flag = t.substring(2).trim()
                    isHidden = flag.equals("true", ignoreCase = true) || flag == "1"
                }
            }
        }

        val normalizedSec = when (security.trim().uppercase(Locale.US)) {
            "", "NOPASS", "NONE", "OPEN" -> if (password.isBlank()) "None (Open)" else "WPA/WPA2"
            "WPA", "WPA2", "WPA/WPA2" -> "WPA/WPA2"
            "WPA3", "SAE" -> "WPA3"
            "WEP" -> "WEP"
            else -> security.trim().uppercase(Locale.US)
        }

        val rawSecCode = when (security.trim().uppercase(Locale.US)) {
            "NOPASS", "NONE", "OPEN" -> "nopass"
            "" -> if (password.isBlank()) "nopass" else "WPA"
            else -> security.trim().uppercase(Locale.US)
        }

        val displaySsid = ssid.ifBlank { "Unnamed Network" }
        val fields = buildList {
            // Serial by serial: 1. Name, 2. Security, 3. Password (exact requested order)
            add(QrDetailField(label = "Wi-Fi Name", value = displaySsid))
            add(QrDetailField(label = "Security", value = normalizedSec))
            if (password.isNotEmpty()) {
                add(QrDetailField(label = "Password", value = password, isSensitive = true, copyValue = password))
            } else {
                add(QrDetailField(label = "Password", value = "No Password (Open)", isSensitive = false, copyValue = ""))
            }
            if (isHidden) {
                add(QrDetailField(label = "Hidden Network", value = "Yes"))
            }
        }

        val copyAll = buildString {
            append("Wi-Fi Name: ").append(displaySsid)
            append("\nSecurity: ").append(normalizedSec)
            if (password.isNotEmpty()) {
                append("\nPassword: ").append(password)
            }
        }

        val details = WifiDetails(
            ssid = displaySsid,
            password = password,
            security = rawSecCode,
            securityDisplay = normalizedSec,
            isHidden = isHidden
        )

        return ParsedQrPayload(
            kind = QrPayloadKind.WIFI,
            badgeTitle = "WI-FI NETWORK",
            primaryTitle = displaySsid,
            fields = fields,
            wifiInfo = details,
            rawContent = raw,
            formattedCopyAll = copyAll
        )
    }

    private fun parseVCard(raw: String): ParsedQrPayload {
        // Unfold lines per RFC 2425/6350
        val unfolded = raw.replace("\r\n ", "").replace("\n ", "")
            .replace("\r\n\t", "").replace("\n\t", "")
        val lines = unfolded.lines()

        var fullName: String? = null
        var structuredName: String? = null
        var org: String? = null
        var title: String? = null
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        val urls = mutableListOf<String>()
        val addresses = mutableListOf<String>()
        val notes = mutableListOf<String>()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty() ||
                trimmedLine.equals("BEGIN:VCARD", ignoreCase = true) ||
                trimmedLine.equals("END:VCARD", ignoreCase = true) ||
                trimmedLine.startsWith("VERSION:", ignoreCase = true)
            ) continue

            val colonIdx = trimmedLine.indexOf(':')
            if (colonIdx <= 0) continue

            val keyPart = trimmedLine.substring(0, colonIdx).uppercase(Locale.US)
            val baseKey = keyPart.substringBefore(';')
            val rawVal = trimmedLine.substring(colonIdx + 1).trim()
            if (rawVal.isEmpty()) continue
            val value = unescapeVCard(rawVal)

            when (baseKey) {
                "FN" -> if (fullName.isNullOrBlank()) fullName = value
                "N" -> {
                    val parts = rawVal.split(';').map { unescapeVCard(it).trim() }.filter { it.isNotEmpty() }
                    // N: Last;First;Middle;Prefix;Suffix
                    val reordered = when {
                        parts.size >= 2 -> {
                            val last = parts[0]
                            val first = parts[1]
                            val middle = parts.getOrNull(2).orEmpty()
                            listOf(first, middle, last).filter { it.isNotEmpty() }.joinToString(" ")
                        }
                        parts.size == 1 -> parts[0]
                        else -> ""
                    }
                    if (reordered.isNotBlank()) structuredName = reordered
                }
                "ORG" -> org = value.replace(';', ' ').trim()
                "TITLE", "ROLE" -> if (title.isNullOrBlank()) title = value
                "TEL" -> {
                    val cleanPhone = value.trim()
                    if (cleanPhone.isNotEmpty() && !phones.contains(cleanPhone)) {
                        phones.add(cleanPhone)
                    }
                }
                "EMAIL" -> {
                    val cleanEmail = value.trim()
                    if (cleanEmail.isNotEmpty() && !emails.contains(cleanEmail)) {
                        emails.add(cleanEmail)
                    }
                }
                "URL" -> {
                    val cleanUrl = value.trim()
                    if (cleanUrl.isNotEmpty() && !urls.contains(cleanUrl)) {
                        urls.add(cleanUrl)
                    }
                }
                "ADR" -> {
                    val addr = rawVal.split(';').map { unescapeVCard(it).trim() }
                        .filter { it.isNotEmpty() }.joinToString(", ")
                    if (addr.isNotEmpty() && !addresses.contains(addr)) {
                        addresses.add(addr)
                    }
                }
                "NOTE" -> notes.add(value)
            }
        }

        val name = fullName?.takeIf { it.isNotBlank() } ?: structuredName
        val contact = ContactDetails(
            name = name,
            organization = org,
            jobTitle = title,
            phones = phones,
            emails = emails,
            urls = urls,
            addresses = addresses,
            note = notes.joinToString("\n").takeIf { it.isNotBlank() }
        )

        return buildContactPayload(contact, raw)
    }

    private fun parseMeCard(raw: String): ParsedQrPayload {
        val body = raw.substring(7) // after MECARD:
        val tokens = splitUnescaped(body, ';')

        var name: String? = null
        var org: String? = null
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        val urls = mutableListOf<String>()
        val addresses = mutableListOf<String>()
        val notes = mutableListOf<String>()

        for (token in tokens) {
            val t = token.trim()
            if (t.isEmpty()) continue
            val colonIdx = t.indexOf(':')
            if (colonIdx <= 0) continue

            val key = t.substring(0, colonIdx).uppercase(Locale.US)
            val valStr = unescapeWifi(t.substring(colonIdx + 1)).trim()
            if (valStr.isEmpty()) continue

            when (key) {
                "N" -> {
                    // MeCard N can be Last,First
                    name = if (valStr.contains(',')) {
                        val parts = valStr.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                        if (parts.size >= 2) "${parts[1]} ${parts[0]}" else valStr
                    } else valStr
                }
                "ORG" -> org = valStr
                "TEL", "TEL-AV" -> if (!phones.contains(valStr)) phones.add(valStr)
                "EMAIL" -> if (!emails.contains(valStr)) emails.add(valStr)
                "URL" -> if (!urls.contains(valStr)) urls.add(valStr)
                "ADR" -> {
                    val addr = valStr.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
                    if (addr.isNotEmpty()) addresses.add(addr)
                }
                "NOTE" -> notes.add(valStr)
            }
        }

        val contact = ContactDetails(
            name = name,
            organization = org,
            phones = phones,
            emails = emails,
            urls = urls,
            addresses = addresses,
            note = notes.joinToString("\n").takeIf { it.isNotBlank() }
        )

        return buildContactPayload(contact, raw)
    }

    private fun buildContactPayload(contact: ContactDetails, raw: String): ParsedQrPayload {
        val fields = buildList {
            contact.name?.takeIf { it.isNotBlank() }?.let {
                add(QrDetailField("Full Name", it))
            }
            contact.jobTitle?.takeIf { it.isNotBlank() }?.let {
                add(QrDetailField("Designation / Title", it))
            }
            contact.organization?.takeIf { it.isNotBlank() }?.let {
                add(QrDetailField("Company / Organization", it))
            }
            contact.phones.forEachIndexed { index, phone ->
                val label = if (contact.phones.size > 1) "Phone ${index + 1}" else "Phone Number"
                add(QrDetailField(label, phone))
            }
            contact.emails.forEachIndexed { index, email ->
                val label = if (contact.emails.size > 1) "Email ${index + 1}" else "Email Address"
                add(QrDetailField(label, email))
            }
            contact.urls.forEachIndexed { index, url ->
                val label = if (contact.urls.size > 1) "Website ${index + 1}" else "Website"
                add(QrDetailField(label, url))
            }
            contact.addresses.forEach { addr ->
                add(QrDetailField("Address", addr))
            }
            contact.note?.takeIf { it.isNotBlank() }?.let {
                add(QrDetailField("Note", it))
            }
        }

        val finalFields = fields.ifEmpty { listOf(QrDetailField("Card Details", raw)) }
        val copyAll = finalFields.joinToString("\n") { "${it.label}: ${it.value}" }

        return ParsedQrPayload(
            kind = QrPayloadKind.VCARD,
            badgeTitle = "VISITING CARD / CONTACT",
            primaryTitle = contact.name ?: contact.organization ?: contact.phones.firstOrNull() ?: "Contact Card",
            fields = finalFields,
            contactInfo = contact,
            primaryPhone = contact.phones.firstOrNull(),
            primaryEmail = contact.emails.firstOrNull(),
            primaryUrl = contact.urls.firstOrNull(),
            rawContent = raw,
            formattedCopyAll = copyAll
        )
    }

    private fun parseSmartTextOrCard(raw: String): ParsedQrPayload {
        val emails = emailRegex.findAll(raw).map { it.value }.distinct().toList()
        val urls = urlRegex.findAll(raw).map { it.value }.distinct().toList()
        val phones = phoneRegex.findAll(raw)
            .map { it.value.trim() }
            .filter { candidate ->
                val digits = candidate.count { it.isDigit() }
                digits in 7..15
            }
            .distinct()
            .toList()

        // Check if multi-line structured card / ID card / visiting card
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val hasMultipleEntities = (phones.isNotEmpty() && emails.isNotEmpty()) ||
            (lines.size >= 2 && (phones.isNotEmpty() || emails.isNotEmpty()))

        if (hasMultipleEntities) {
            val fields = mutableListOf<QrDetailField>()
            var guessedName: String? = null
            var guessedCompany: String? = null
            var guessedTitle: String? = null
            val addresses = mutableListOf<String>()
            val notes = mutableListOf<String>()

            for (line in lines) {
                val colonIdx = line.indexOf(':')
                if (colonIdx in 1..22 && !line.startsWith("http", ignoreCase = true)) {
                    val keyRaw = line.substring(0, colonIdx).trim()
                    val valueRaw = line.substring(colonIdx + 1).trim()
                    if (valueRaw.isNotEmpty()) {
                        val keyLower = keyRaw.lowercase(Locale.US)
                        when {
                            keyLower in listOf("name", "full name", "holder", "contact", "person") && guessedName == null ->
                                guessedName = valueRaw
                            keyLower in listOf("company", "org", "organization", "office", "business", "institute") && guessedCompany == null ->
                                guessedCompany = valueRaw
                            keyLower in listOf("title", "designation", "role", "position", "post") && guessedTitle == null ->
                                guessedTitle = valueRaw
                            keyLower in listOf("address", "addr", "location", "city") ->
                                addresses.add(valueRaw)
                        }
                        fields.add(QrDetailField(keyRaw, valueRaw))
                        continue
                    }
                }

                // Unlabeled line: classify nicely
                when {
                    emailRegex.matches(line) -> fields.add(QrDetailField("Email Address", line))
                    urlRegex.matches(line) -> fields.add(QrDetailField("Website", line))
                    phones.contains(line) -> fields.add(QrDetailField("Phone Number", line))
                    guessedName == null && !line.contains('@') && !line.contains("www.", true) -> {
                        guessedName = line
                        fields.add(QrDetailField("Name", line))
                    }
                    else -> {
                        notes.add(line)
                        fields.add(QrDetailField("Info", line))
                    }
                }
            }

            val contact = ContactDetails(
                name = guessedName,
                organization = guessedCompany,
                jobTitle = guessedTitle,
                phones = phones,
                emails = emails,
                urls = urls,
                addresses = addresses,
                note = notes.joinToString("\n").takeIf { it.isNotBlank() }
            )

            val copyAll = fields.joinToString("\n") { "${it.label}: ${it.value}" }

            return ParsedQrPayload(
                kind = QrPayloadKind.VCARD,
                badgeTitle = "VISITING CARD / CONTACT",
                primaryTitle = guessedName ?: guessedCompany ?: phones.firstOrNull() ?: emails.firstOrNull() ?: "Contact Card",
                fields = fields,
                contactInfo = contact,
                primaryPhone = phones.firstOrNull(),
                primaryEmail = emails.firstOrNull(),
                primaryUrl = urls.firstOrNull(),
                rawContent = raw,
                formattedCopyAll = copyAll
            )
        }

        // Plain Text (still expose detected phone/email/url actions if present!)
        val fields = buildList {
            if (lines.size > 1) {
                lines.forEachIndexed { idx, line ->
                    val colonIdx = line.indexOf(':')
                    if (colonIdx in 1..20 && !line.startsWith("http", ignoreCase = true)) {
                        val k = line.substring(0, colonIdx).trim()
                        val v = line.substring(colonIdx + 1).trim()
                        if (v.isNotEmpty()) {
                            add(QrDetailField(k, v))
                            return@forEachIndexed
                        }
                    }
                    add(QrDetailField("Line ${idx + 1}", line))
                }
            } else {
                add(QrDetailField("Text Content", raw))
            }
        }

        return ParsedQrPayload(
            kind = QrPayloadKind.TEXT,
            badgeTitle = "TEXT CONTENT",
            primaryTitle = raw.take(48),
            fields = fields,
            primaryPhone = phones.firstOrNull(),
            primaryEmail = emails.firstOrNull(),
            primaryUrl = urls.firstOrNull(),
            rawContent = raw,
            formattedCopyAll = raw
        )
    }

    private fun parseUrl(raw: String): ParsedQrPayload {
        val fullUrl = if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
            raw
        } else {
            "https://$raw"
        }
        val host = runCatching { java.net.URI(fullUrl).host }.getOrNull()?.removePrefix("www.")
        val fields = buildList {
            if (!host.isNullOrBlank()) {
                add(QrDetailField("Domain", host))
            }
            add(QrDetailField("Website URL", fullUrl))
        }
        return ParsedQrPayload(
            kind = QrPayloadKind.URL,
            badgeTitle = "WEBSITE LINK",
            primaryTitle = host ?: fullUrl,
            fields = fields,
            primaryUrl = fullUrl,
            rawContent = raw,
            formattedCopyAll = fullUrl
        )
    }

    private fun parseMailto(raw: String): ParsedQrPayload {
        val withoutScheme = raw.removePrefix("mailto:").removePrefix("MAILTO:")
        val qIdx = withoutScheme.indexOf('?')
        val address = (if (qIdx >= 0) withoutScheme.substring(0, qIdx) else withoutScheme).trim()
        var subject: String? = null
        var body: String? = null

        if (qIdx >= 0) {
            val query = withoutScheme.substring(qIdx + 1)
            for (param in query.split('&')) {
                val eqIdx = param.indexOf('=')
                if (eqIdx > 0) {
                    val k = param.substring(0, eqIdx).lowercase(Locale.US)
                    val v = runCatching { URLDecoder.decode(param.substring(eqIdx + 1), "UTF-8") }
                        .getOrDefault(param.substring(eqIdx + 1))
                    when (k) {
                        "subject" -> subject = v
                        "body" -> body = v
                    }
                }
            }
        }

        val fields = buildList {
            add(QrDetailField("Email Address", address))
            subject?.takeIf { it.isNotBlank() }?.let { add(QrDetailField("Subject", it)) }
            body?.takeIf { it.isNotBlank() }?.let { add(QrDetailField("Message", it)) }
        }

        val copyAll = fields.joinToString("\n") { "${it.label}: ${it.value}" }

        return ParsedQrPayload(
            kind = QrPayloadKind.EMAIL,
            badgeTitle = "EMAIL ADDRESS",
            primaryTitle = address,
            fields = fields,
            primaryEmail = address,
            emailSubject = subject,
            emailBody = body,
            rawContent = raw,
            formattedCopyAll = copyAll
        )
    }

    private fun parseMatMsgEmail(raw: String): ParsedQrPayload {
        // MATMSG:TO:foo@bar.com;SUB:Hi;BODY:Hello;;
        val body = raw.substring(7)
        val tokens = splitUnescaped(body, ';')
        var to = ""
        var sub: String? = null
        var msg: String? = null

        for (t in tokens) {
            val trimmed = t.trim()
            when {
                trimmed.startsWith("TO:", ignoreCase = true) -> to = unescapeWifi(trimmed.substring(3))
                trimmed.startsWith("SUB:", ignoreCase = true) -> sub = unescapeWifi(trimmed.substring(4))
                trimmed.startsWith("BODY:", ignoreCase = true) -> msg = unescapeWifi(trimmed.substring(5))
            }
        }

        val fields = buildList {
            add(QrDetailField("Email Address", to))
            sub?.takeIf { it.isNotBlank() }?.let { add(QrDetailField("Subject", it)) }
            msg?.takeIf { it.isNotBlank() }?.let { add(QrDetailField("Message", it)) }
        }
        val copyAll = fields.joinToString("\n") { "${it.label}: ${it.value}" }

        return ParsedQrPayload(
            kind = QrPayloadKind.EMAIL,
            badgeTitle = "EMAIL MESSAGE",
            primaryTitle = to,
            fields = fields,
            primaryEmail = to,
            emailSubject = sub,
            emailBody = msg,
            rawContent = raw,
            formattedCopyAll = copyAll
        )
    }

    private fun parseTel(raw: String): ParsedQrPayload {
        val number = raw.replace(Regex("(?i)^tel:"), "").trim()
        return ParsedQrPayload(
            kind = QrPayloadKind.PHONE,
            badgeTitle = "PHONE NUMBER",
            primaryTitle = number,
            fields = listOf(QrDetailField("Phone Number", number)),
            primaryPhone = number,
            rawContent = raw,
            formattedCopyAll = number
        )
    }

    private fun parseSms(raw: String): ParsedQrPayload {
        val body = raw.replace(Regex("(?i)^smsto:|^sms:"), "")
        val parts = body.split(':', limit = 2)
        val phone = parts.getOrNull(0)?.trim().orEmpty()
        val msg = parts.getOrNull(1)?.trim().orEmpty()

        val fields = buildList {
            if (phone.isNotEmpty()) add(QrDetailField("Phone Number", phone))
            if (msg.isNotEmpty()) add(QrDetailField("Message", msg))
        }.ifEmpty { listOf(QrDetailField("SMS Content", raw)) }

        return ParsedQrPayload(
            kind = QrPayloadKind.SMS,
            badgeTitle = "SMS MESSAGE",
            primaryTitle = phone.ifEmpty { "SMS" },
            fields = fields,
            primaryPhone = phone.takeIf { it.isNotEmpty() },
            smsMessage = msg.takeIf { it.isNotEmpty() },
            rawContent = raw,
            formattedCopyAll = fields.joinToString("\n") { "${it.label}: ${it.value}" }
        )
    }

    private fun parseGeo(raw: String): ParsedQrPayload {
        val body = raw.replace(Regex("(?i)^geo:"), "")
        val qIdx = body.indexOf('?')
        val coordsPart = if (qIdx >= 0) body.substring(0, qIdx) else body
        val queryPart = if (qIdx >= 0) body.substring(qIdx + 1) else null
        val coords = coordsPart.split(',')
        val lat = coords.getOrNull(0)?.trim().orEmpty()
        val lng = coords.getOrNull(1)?.trim()?.substringBefore(',')?.trim().orEmpty()
        val label = queryPart?.removePrefix("q=")?.let {
            runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it)
        }

        val fields = buildList {
            if (lat.isNotEmpty()) add(QrDetailField("Latitude", lat))
            if (lng.isNotEmpty()) add(QrDetailField("Longitude", lng))
            label?.takeIf { it.isNotBlank() }?.let { add(QrDetailField("Location Label", it)) }
        }

        return ParsedQrPayload(
            kind = QrPayloadKind.GEO,
            badgeTitle = "MAP LOCATION",
            primaryTitle = "$lat, $lng",
            fields = fields,
            geoLat = lat,
            geoLng = lng,
            geoQuery = label,
            rawContent = raw,
            formattedCopyAll = fields.joinToString("\n") { "${it.label}: ${it.value}" }
        )
    }

    private fun parseCalendarEvent(raw: String): ParsedQrPayload {
        val lines = raw.lines()
        var summary: String? = null
        var location: String? = null
        var start: String? = null
        var end: String? = null
        var description: String? = null

        for (line in lines) {
            val t = line.trim()
            val idx = t.indexOf(':')
            if (idx <= 0) continue
            val k = t.substring(0, idx).substringBefore(';').uppercase(Locale.US)
            val v = unescapeVCard(t.substring(idx + 1).trim())
            if (v.isEmpty()) continue
            when (k) {
                "SUMMARY" -> summary = v
                "LOCATION" -> location = v
                "DTSTART" -> start = v
                "DTEND" -> end = v
                "DESCRIPTION" -> description = v
            }
        }

        val fields = buildList {
            summary?.let { add(QrDetailField("Event Title", it)) }
            start?.let { add(QrDetailField("Start Time", it)) }
            end?.let { add(QrDetailField("End Time", it)) }
            location?.let { add(QrDetailField("Location", it)) }
            description?.let { add(QrDetailField("Description", it)) }
        }.ifEmpty { listOf(QrDetailField("Calendar Event", raw)) }

        return ParsedQrPayload(
            kind = QrPayloadKind.CALENDAR,
            badgeTitle = "CALENDAR EVENT",
            primaryTitle = summary ?: "Calendar Event",
            fields = fields,
            calendarDetails = CalendarDetails(summary, location, start, end, description),
            rawContent = raw,
            formattedCopyAll = fields.joinToString("\n") { "${it.label}: ${it.value}" }
        )
    }

    private fun buildBarcodePayload(raw: String): ParsedQrPayload {
        val fields = listOf(
            QrDetailField("Barcode Value", raw)
        )
        return ParsedQrPayload(
            kind = QrPayloadKind.BARCODE,
            badgeTitle = "PRODUCT BARCODE",
            primaryTitle = raw,
            fields = fields,
            rawContent = raw,
            formattedCopyAll = raw
        )
    }

    private fun isSingleUrl(text: String): Boolean {
        if (text.contains('\n') || text.contains(' ')) return false
        return text.startsWith("http://", ignoreCase = true) ||
            text.startsWith("https://", ignoreCase = true) ||
            text.startsWith("www.", ignoreCase = true)
    }

    private fun isSingleEmail(text: String): Boolean {
        if (text.contains('\n') || text.contains(' ')) return false
        return emailRegex.matches(text)
    }

    private fun isSinglePhone(text: String): Boolean {
        if (text.contains('\n')) return false
        val cleaned = text.trim()
        if (!cleaned.all { it.isDigit() || it in "+-() " }) return false
        val digits = cleaned.count { it.isDigit() }
        return digits in 6..16
    }

    private fun splitUnescaped(input: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (ch in input) {
            if (escaped) {
                current.append('\\').append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == delimiter) {
                result.add(current.toString())
                current.clear()
            } else {
                current.append(ch)
            }
        }
        if (escaped) current.append('\\')
        result.add(current.toString())
        return result
    }

    private fun unescapeWifi(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length) {
                sb.append(text[i + 1])
                i += 2
            } else {
                sb.append(text[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun unescapeVCard(text: String): String {
        return text.replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }
}
