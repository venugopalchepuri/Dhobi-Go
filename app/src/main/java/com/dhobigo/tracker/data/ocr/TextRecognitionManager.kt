package com.dhobigo.tracker.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class ExtractedChitData(
    val slipNumber: String? = null,
    val dateText: String? = null,
    val dateMillis: Long? = null,
    val recognizedItems: Map<String, Int> = emptyMap(),
    val detectedTotal: Int? = null,
    val rawText: String = "",
    val mismatchWarning: String? = null
)

class TextRecognitionManager(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeChit(
        imageFile: File,
        knownCategories: List<String>
    ): ExtractedChitData {
        val inputImage = try {
            InputImage.fromFilePath(context, Uri.fromFile(imageFile))
        } catch (e: Exception) {
            return ExtractedChitData(rawText = "Failed to load image: ${e.message}")
        }

        val visionText = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text?> { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    continuation.resume(text)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } ?: return ExtractedChitData(rawText = "OCR process failed or produced no text.")

        val rawText = visionText.text
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var detectedSlipNumber: String? = null
        var detectedDateText: String? = null
        var detectedDateMillis: Long? = null
        var detectedTotal: Int? = null
        val recognizedItems = mutableMapOf<String, Int>()

        // 1. Regex patterns for slip numbers
        val slipRegex = Pattern.compile(
            """(?:slip|no|chit|bill|rec|s\.no|token)[\s.:#-]*([0-9A-Za-z]{2,8})""",
            Pattern.CASE_INSENSITIVE
        )

        // 2. Regex patterns for total
        val totalRegex = Pattern.compile(
            """(?:total|tot|grand total|items)[\s.:=-]*(\d{1,3})""",
            Pattern.CASE_INSENSITIVE
        )

        // 3. Regex for dates (e.g. 20/10/2026, 20-10-26, 20 Oct 2026)
        val dateRegex = Pattern.compile(
            """\b(\d{1,2}[-/.](?:\d{1,2}|[A-Za-z]{3})[-/.](?:\d{2,4}))\b"""
        )

        for (line in lines) {
            // Check slip number
            if (detectedSlipNumber == null) {
                val slipMatcher = slipRegex.matcher(line)
                if (slipMatcher.find()) {
                    detectedSlipNumber = slipMatcher.group(1)
                }
            }

            // Check total
            if (detectedTotal == null) {
                val totalMatcher = totalRegex.matcher(line)
                if (totalMatcher.find()) {
                    detectedTotal = totalMatcher.group(1)?.toIntOrNull()
                }
            }

            // Check date
            if (detectedDateText == null) {
                val dateMatcher = dateRegex.matcher(line)
                if (dateMatcher.find()) {
                    val rawDate = dateMatcher.group(1)
                    detectedDateText = rawDate
                    detectedDateMillis = parseDateToMillis(rawDate)
                }
            }

            // Match categories and quantities
            for (category in knownCategories) {
                val normalizedCat = category.lowercase(Locale.getDefault())
                val normalizedLine = line.lowercase(Locale.getDefault())

                if (normalizedLine.contains(normalizedCat)) {
                    // Extract number from the same line
                    val numberMatch = Pattern.compile("""\b(\d{1,2})\b""").matcher(line)
                    var foundQuantity: Int? = null
                    while (numberMatch.find()) {
                        val num = numberMatch.group(1)?.toIntOrNull()
                        if (num != null && num in 1..99) {
                            foundQuantity = num
                            break
                        }
                    }

                    if (foundQuantity != null && foundQuantity > 0) {
                        recognizedItems[category] = foundQuantity
                    }
                }
            }
        }

        // Validate items sum against total if both found
        val itemsSum = recognizedItems.values.sum()
        var warning: String? = null
        if (detectedTotal != null && itemsSum > 0 && detectedTotal != itemsSum) {
            warning = "Recognized item count ($itemsSum) differs from detected chit total ($detectedTotal). Please verify quantities."
        }

        return ExtractedChitData(
            slipNumber = detectedSlipNumber,
            dateText = detectedDateText,
            dateMillis = detectedDateMillis,
            recognizedItems = recognizedItems,
            detectedTotal = detectedTotal,
            rawText = rawText,
            mismatchWarning = warning
        )
    }

    private fun parseDateToMillis(dateStr: String?): Long? {
        if (dateStr == null) return null
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy",
            "dd MMM yyyy", "dd MMMM yyyy", "yyyy-MM-dd"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(dateStr)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return null
    }
}
