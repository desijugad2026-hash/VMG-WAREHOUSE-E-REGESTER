package com.example.printing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.data.model.TransactionRecord
import com.example.ui.viewmodel.DayHistoryGroup
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EscPosReceiptBuilder {

  private const val CHARS_PER_LINE = 48
  private const val RASTER_WIDTH_DOTS = 576 // 80mm thermal paper @ 203 DPI (72 bytes * 8 bits = 576 dots)
  private const val RASTER_WIDTH_BYTES = 72
  private val CHARSET = Charset.forName("windows-1252")

  // ESC/POS Commands
  private val ESC_INIT = byteArrayOf(0x1B, 0x40)
  private val ESC_RESET_FONT = byteArrayOf(0x1B, 0x21, 0x00)
  private val ESC_DEFAULT_LINE_SPACING = byteArrayOf(0x1B, 0x32)
  // Balanced high-contrast heating: max dots = 11 (96 dots), heat time = 115 (1150us), interval = 2 (20us)
  private val ESC_HEAT_SETTINGS = byteArrayOf(0x1B, 0x37, 0x0B, 0x73.toByte(), 0x02)
  private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
  private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
  private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
  private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
  private val ESC_DOUBLE_STRIKE_ON = byteArrayOf(0x1B, 0x47, 0x01)
  private val ESC_DOUBLE_STRIKE_OFF = byteArrayOf(0x1B, 0x47, 0x00)
  private val ESC_DOUBLE_HEIGHT = byteArrayOf(0x1D, 0x21, 0x01)
  private val ESC_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00)
  private val ESC_FEED_MINIMAL = byteArrayOf(0x1B, 0x64, 0x03)
  private val ESC_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00)

  private val SOLID_LINE = "=".repeat(CHARS_PER_LINE)
  private val SINGLE_LINE = "-".repeat(CHARS_PER_LINE)

  fun generateTestPrintText(): String {
    val sb = StringBuilder()
    val now = SimpleDateFormat("dd/MM/yyyy  hh:mm a", Locale.getDefault()).format(Date())
    sb.appendLine(centerText("ESTIMATE"))
    sb.appendLine(SOLID_LINE)
    sb.appendLine(formatMetadataRow("NAME    : SAMPLE CUSTOMER", "DATE  : ${formatDateReceipt(System.currentTimeMillis())}"))
    sb.appendLine(formatMetadataRow("NUMBER  : 9876543210", "TIME  : ${formatTimeReceipt(System.currentTimeMillis())}"))
    sb.appendLine(formatMetadataRow("ADDRESS : MAIN MARKET", "S. NO.: 1"))
    sb.appendLine(formatMetadataRow("", "V. NO.: ___________"))
    sb.appendLine(SINGLE_LINE)
    sb.appendLine(formatItemHeader())
    sb.appendLine(SINGLE_LINE)
    formatItemRows("15 Box", "1100 GLOSS [12x24]", 260.0, 3900.0).forEach { sb.appendLine(it) }
    sb.appendLine("")
    formatItemRows("10 Box", "1200 WHITE [12x24]", 420.0, 4200.0).forEach { sb.appendLine(it) }
    sb.appendLine(SINGLE_LINE)
    sb.appendLine(formatPaymentAndTotals("CASH", 0.0, 8100.0))
    sb.appendLine(SINGLE_LINE)
    sb.appendLine(centerText("NO RETURN / NO EXCHANGE"))
    sb.appendLine("")
    sb.appendLine(centerText("THANK YOU"))
    return sb.toString()
  }

  fun generateTestPrintEscPos(): ByteArray {
    val raster = renderTestPrintRasterEscPos()
    if (raster != null) return raster

    val out = ByteArrayOutputStream()
    out.write(ESC_INIT)
    out.write(ESC_HEAT_SETTINGS)
    out.write(ESC_RESET_FONT)
    out.write(ESC_DEFAULT_LINE_SPACING)
    out.write(ESC_DOUBLE_STRIKE_ON)
    out.write(generateTestPrintText().toByteArray(CHARSET))
    out.write(ESC_DOUBLE_STRIKE_OFF)
    out.write(ESC_FEED_MINIMAL)
    out.write(ESC_CUT)
    return out.toByteArray()
  }

  fun generateOneInchBlackEscPos(): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(ESC_INIT)
    out.write(ESC_HEAT_SETTINGS)
    out.write(ESC_ALIGN_CENTER)

    val widthBytes = 72
    val heightDots = 200
    val gsV0Header = byteArrayOf(
      0x1D, 0x76, 0x30, 0x00,
      (widthBytes and 0xFF).toByte(),
      ((widthBytes shr 8) and 0xFF).toByte(),
      (heightDots and 0xFF).toByte(),
      ((heightDots shr 8) and 0xFF).toByte()
    )
    out.write(gsV0Header)

    val blackLine = ByteArray(widthBytes) { 0xFF.toByte() }
    for (i in 0 until heightDots) {
      out.write(blackLine)
    }

    out.write(ESC_FEED_MINIMAL)
    out.write(ESC_RESET_FONT)
    out.write(ESC_ALIGN_CENTER)
    out.write("--- 1 INCH DENSITY TEST ---\n".toByteArray(CHARSET))
    out.write(ESC_FEED_MINIMAL)
    return out.toByteArray()
  }

  /**
   * Generates clean, professional commercial receipt text for on-screen preview
   */
  fun generatePlainText(transaction: TransactionRecord, serialNumber: Long = 0L): String {
    val actualSerial = if (transaction.serialNumber > 0L) transaction.serialNumber else if (serialNumber > 0L) serialNumber else transaction.id.coerceAtLeast(1L)
    val sb = StringBuilder()
    when (transaction.type) {
      "CASH_SALE" -> buildSaleReceiptText(sb, transaction, actualSerial, "CASH")
      "ONLINE_SALE" -> {
        val mode = if (transaction.bankAccountName.isNotBlank()) "UPI (${transaction.bankAccountName})" else "UPI"
        buildSaleReceiptText(sb, transaction, actualSerial, mode)
      }
      "CREDIT_SALE" -> buildSaleReceiptText(sb, transaction, actualSerial, "CREDIT")
      "CASH_RECEIPT" -> buildCashReceiptText(sb, transaction, actualSerial)
      "CASH_GIVE" -> buildCashGiveText(sb, transaction, actualSerial)
      "GOODS_RETURN" -> buildGoodsReturnText(sb, transaction, actualSerial)
      else -> buildSaleReceiptText(sb, transaction, actualSerial, "CASH")
    }
    return sb.toString().trimEnd()
  }

  fun generatePlainText(transaction: TransactionRecord, serialNumber: Int): String {
    return generatePlainText(transaction, serialNumber.toLong())
  }

  fun generateEscPos(transaction: TransactionRecord, serialNumber: Long = 0L, copies: Int = 1): ByteArray {
    val out = ByteArrayOutputStream()
    for (i in 1..copies.coerceAtLeast(1)) {
      val singleCopy = generateSingleEscPos(transaction, serialNumber)
      out.write(singleCopy)
      if (i < copies) {
        out.write(ESC_FEED_MINIMAL)
      }
    }
    return out.toByteArray()
  }

  fun generateEscPos(transaction: TransactionRecord, serialNumber: Int, copies: Int = 1): ByteArray {
    return generateEscPos(transaction, serialNumber.toLong(), copies)
  }

  private fun generateSingleEscPos(transaction: TransactionRecord, serialNumber: Long = 0L): ByteArray {
    val actualSerial = if (transaction.serialNumber > 0L) transaction.serialNumber else if (serialNumber > 0L) serialNumber else transaction.id.coerceAtLeast(1L)

    val rasterBytes = renderTransactionRasterEscPos(transaction, actualSerial)
    if (rasterBytes != null) {
      return rasterBytes
    }

    val out = ByteArrayOutputStream()
    out.write(ESC_INIT)
    out.write(ESC_HEAT_SETTINGS)
    out.write(ESC_RESET_FONT)
    out.write(ESC_DEFAULT_LINE_SPACING)
    out.write(ESC_DOUBLE_STRIKE_ON)
    out.write((generatePlainText(transaction, actualSerial) + "\n").toByteArray(CHARSET))
    out.write(ESC_DOUBLE_STRIKE_OFF)
    out.write(ESC_FEED_MINIMAL)
    out.write(ESC_CUT)
    return out.toByteArray()
  }

  fun generateDaySummaryText(group: DayHistoryGroup): String {
    val sb = StringBuilder()
    sb.appendLine(centerText("WAREHOUSE REGISTER"))
    sb.appendLine(centerText("FULL DAY SUMMARY"))
    sb.appendLine(SOLID_LINE)
    sb.appendLine(formatRowLeftRight("Date: ${group.dateFormatted}", "Entries: ${group.transactions.size}"))
    sb.appendLine(SINGLE_LINE)
    sb.appendLine(formatRowLeftRight("CASH IN", "Rs. " + formatCompactAmount(group.totalCashReceived)))
    sb.appendLine(formatRowLeftRight("CASH OUT", "Rs. " + formatCompactAmount(group.totalCashGiven)))
    if (group.totalOnlineReceived > 0) {
      sb.appendLine(formatRowLeftRight("ONLINE IN", "Rs. " + formatCompactAmount(group.totalOnlineReceived)))
    }
    val net = group.totalCashReceived - group.totalCashGiven
    sb.appendLine(SINGLE_LINE)
    sb.appendLine(formatRowLeftRight("NET CASH", "Rs. " + formatCompactAmount(net)))
    sb.appendLine(SOLID_LINE)

    group.transactions.forEachIndexed { index, tx ->
      val serial = if (tx.serialNumber > 0L) tx.serialNumber else (index + 1).toLong()
      val typeLabel = when (tx.type) {
        "CASH_SALE" -> "SALE — CASH"
        "ONLINE_SALE" -> "SALE — UPI"
        "CREDIT_SALE" -> "SALE — CREDIT"
        "CASH_RECEIPT" -> if (tx.receiptMethod.equals("Online", ignoreCase = true)) "RECEIPT — UPI" else "RECEIPT — CASH"
        "CASH_GIVE" -> "PAYMENT — CASH"
        "GOODS_RETURN" -> "GOODS RETURN"
        else -> tx.type
      }
      val amtRight = if (tx.type == "CREDIT_SALE") typeLabel else "Rs. ${formatCompactAmount(tx.totalAmount)}"
      sb.appendLine(formatRowLeftRight("#$serial ${tx.personName.take(22)}", amtRight))
      val metaLeft = "${tx.formattedTime} • $typeLabel" +
        (if (tx.referenceNumber.isNotBlank()) " • Ref:${tx.referenceNumber}" else "")
      sb.appendLine("  $metaLeft")
      if (tx.shortItemsDescription.isNotBlank()) {
        sb.appendLine("  ${tx.shortItemsDescription.take(44)}")
      }
      if (index < group.transactions.size - 1) {
        sb.appendLine(SINGLE_LINE)
      }
    }
    sb.appendLine(SOLID_LINE)
    sb.appendLine(centerText("END OF DAY REPORT"))
    return sb.toString().trimEnd()
  }

  fun generateDaySummaryEscPos(group: DayHistoryGroup, copies: Int = 1): ByteArray {
    val out = ByteArrayOutputStream()
    val singleRaster = renderDaySummaryRasterEscPos(group)
    for (i in 1..copies.coerceAtLeast(1)) {
      if (singleRaster != null) {
        out.write(singleRaster)
      } else {
        val text = generateDaySummaryText(group)
        out.write(ESC_INIT)
        out.write(ESC_HEAT_SETTINGS)
        out.write(ESC_RESET_FONT)
        out.write(ESC_DEFAULT_LINE_SPACING)
        out.write(ESC_DOUBLE_STRIKE_ON)
        out.write((text + "\n").toByteArray(CHARSET))
        out.write(ESC_DOUBLE_STRIKE_OFF)
        out.write(ESC_FEED_MINIMAL)
        out.write(ESC_CUT)
      }
    }
    return out.toByteArray()
  }

  // --- Clean Plain-Text Builders (Matching the Specific Receipt Hierarchy) ---

  private fun buildSaleReceiptText(
    sb: StringBuilder,
    tx: TransactionRecord,
    serial: Long,
    paymentModeLabel: String
  ) {
    sb.appendLine(centerText("ESTIMATE"))
    sb.appendLine(SOLID_LINE)

    // Customer Information (Left) + Date/Time/Serial/Blank Voucher (Right)
    val name = tx.personName.ifBlank { "WALK-IN" }
    val phone = tx.phoneNumbersList.firstOrNull().orEmpty().ifBlank { "-" }
    val address = tx.address.ifBlank { "-" }
    val dateStr = formatDateReceipt(tx.timestamp)
    val timeStr = formatTimeReceipt(tx.timestamp)

    sb.appendLine(formatMetadataRow("NAME    : $name", "DATE  : $dateStr"))
    sb.appendLine(formatMetadataRow("NUMBER  : $phone", "TIME  : $timeStr"))
    sb.appendLine(formatMetadataRow("ADDRESS : $address", "S. NO.: $serial"))
    sb.appendLine(formatMetadataRow("", "V. NO.: ___________"))

    if (tx.referenceNumber.isNotBlank()) {
      sb.appendLine("REF NO  : ${tx.referenceNumber}")
    }

    sb.appendLine(SINGLE_LINE)
    sb.appendLine(formatItemHeader())
    sb.appendLine(SINGLE_LINE)

    val isCreditNoAmount = tx.type == "CREDIT_SALE" && tx.totalAmount <= 0.0

    tx.itemsList.forEachIndexed { index, item ->
      val nameStr = if (item.size.isNotBlank()) "${item.designName} [${item.size}]" else item.designName
      val qtyStr = "${formatQuantity(item.quantity)} ${item.unit}"
      if (isCreditNoAmount) {
        sb.appendLine(qtyStr)
        sb.appendLine(nameStr)
      } else {
        val lines = formatItemRows(
          qtyStr = qtyStr,
          desc = nameStr,
          rate = item.rate,
          amount = item.itemTotal
        )
        lines.forEach { sb.appendLine(it) }
      }
      if (index < tx.itemsList.size - 1) {
        sb.appendLine("")
      }
    }

    sb.appendLine(SINGLE_LINE)

    if (isCreditNoAmount) {
      sb.appendLine("PAYMENT MODE: CREDIT")
      sb.appendLine("")
      sb.appendLine("DELIVERY PERSON : __________________")
      sb.appendLine("SIGNATURE       : __________________")
    } else {
      sb.appendLine(formatPaymentAndTotals(paymentModeLabel, tx.cartage, tx.totalAmount))
    }

    sb.appendLine(SINGLE_LINE)
    sb.appendLine(centerText("NO RETURN / NO EXCHANGE"))
    sb.appendLine("")
    sb.appendLine(centerText("THANK YOU"))
  }

  private fun buildGoodsReturnText(sb: StringBuilder, tx: TransactionRecord, serial: Long) {
    sb.appendLine(centerText("GOODS RETURN"))
    sb.appendLine(SOLID_LINE)

    val name = tx.personName.ifBlank { "WALK-IN" }
    val phone = tx.phoneNumbersList.firstOrNull().orEmpty().ifBlank { "-" }
    val dateStr = formatDateReceipt(tx.timestamp)
    val timeStr = formatTimeReceipt(tx.timestamp)

    sb.appendLine(formatMetadataRow("NAME    : $name", "DATE  : $dateStr"))
    sb.appendLine(formatMetadataRow("NUMBER  : $phone", "TIME  : $timeStr"))
    sb.appendLine(formatMetadataRow("", "S. NO.: $serial"))
    sb.appendLine(formatMetadataRow("", "V. NO.: ___________"))

    if (tx.referenceNumber.isNotBlank()) {
      sb.appendLine("REF NO  : ${tx.referenceNumber}")
    }

    sb.appendLine(SINGLE_LINE)
    sb.appendLine("RETURNED ITEMS")
    sb.appendLine(SINGLE_LINE)

    tx.itemsList.forEachIndexed { index, item ->
      val nameStr = if (item.size.isNotBlank()) "${item.designName} [${item.size}]" else item.designName
      val qtyStr = "${formatQuantity(item.quantity)} ${item.unit}"
      sb.appendLine(qtyStr)
      sb.appendLine(nameStr)
      if (index < tx.itemsList.size - 1) {
        sb.appendLine("")
      }
    }

    sb.appendLine(SINGLE_LINE)
    if (tx.totalAmount > 0) {
      sb.appendLine(formatRowLeftRight("REFUND", "Rs. " + formatCompactAmount(tx.totalAmount)))
    } else {
      sb.appendLine(formatRowLeftRight("REFUND", "NIL"))
    }
    sb.appendLine(SINGLE_LINE)
    sb.appendLine(centerText("NO RETURN / NO EXCHANGE"))
    sb.appendLine("")
    sb.appendLine(centerText("THANK YOU"))
  }

  private fun buildCashReceiptText(sb: StringBuilder, tx: TransactionRecord, serial: Long) {
    sb.appendLine(centerText("RECEIPT"))
    sb.appendLine(SOLID_LINE)

    val name = tx.personName.ifBlank { "PARTY" }
    val phone = tx.phoneNumbersList.firstOrNull().orEmpty().ifBlank { "-" }
    val dateStr = formatDateReceipt(tx.timestamp)
    val timeStr = formatTimeReceipt(tx.timestamp)

    sb.appendLine(formatMetadataRow("RECEIVED: $name", "DATE  : $dateStr"))
    sb.appendLine(formatMetadataRow("NUMBER  : $phone", "TIME  : $timeStr"))
    sb.appendLine(formatMetadataRow("", "S. NO.: $serial"))
    sb.appendLine(formatMetadataRow("", "V. NO.: ___________"))

    if (tx.referenceNumber.isNotBlank()) {
      sb.appendLine("REF NO  : ${tx.referenceNumber}")
    }

    val mode = if (tx.receiptMethod.equals("Online", ignoreCase = true)) {
      if (tx.bankAccountName.isNotBlank()) "UPI (${tx.bankAccountName})" else "UPI"
    } else {
      "CASH"
    }

    sb.appendLine(SINGLE_LINE)
    sb.appendLine("PAYMENT MODE: $mode")
    sb.appendLine(SINGLE_LINE)
    sb.appendLine(formatRowLeftRight("AMOUNT RECEIVED", "Rs. " + formatCompactAmount(tx.totalAmount)))
    sb.appendLine(SOLID_LINE)
    sb.appendLine(centerText("THANK YOU"))
  }

  private fun buildCashGiveText(sb: StringBuilder, tx: TransactionRecord, serial: Long) {
    sb.appendLine(centerText("PAYMENT"))
    sb.appendLine(SOLID_LINE)

    val name = tx.personName.ifBlank { "PAYEE" }
    val phone = tx.phoneNumbersList.firstOrNull().orEmpty().ifBlank { "-" }
    val dateStr = formatDateReceipt(tx.timestamp)
    val timeStr = formatTimeReceipt(tx.timestamp)

    sb.appendLine(formatMetadataRow("PAID TO : $name", "DATE  : $dateStr"))
    sb.appendLine(formatMetadataRow("NUMBER  : $phone", "TIME  : $timeStr"))
    sb.appendLine(formatMetadataRow("", "S. NO.: $serial"))
    sb.appendLine(formatMetadataRow("", "V. NO.: ___________"))

    if (tx.referenceNumber.isNotBlank()) {
      sb.appendLine("REF NO  : ${tx.referenceNumber}")
    }

    sb.appendLine(SINGLE_LINE)
    sb.appendLine("PAYMENT MODE: CASH")
    sb.appendLine(SINGLE_LINE)
    sb.appendLine(formatRowLeftRight("AMOUNT PAID", "Rs. " + formatCompactAmount(tx.totalAmount)))
    sb.appendLine(SOLID_LINE)
    sb.appendLine(centerText("THANK YOU"))
  }

  private fun centerText(text: String): String {
    if (text.length >= CHARS_PER_LINE) return text
    val leftPad = (CHARS_PER_LINE - text.length) / 2
    return " ".repeat(leftPad) + text
  }

  private fun formatRowLeftRight(left: String, right: String): String {
    val totalLen = CHARS_PER_LINE
    if (left.length + right.length >= totalLen) {
      return "$left $right"
    }
    val spaces = totalLen - left.length - right.length
    return left + " ".repeat(spaces) + right
  }

  private fun formatMetadataRow(left: String, right: String): String {
    val leftColWidth = 26
    val rightColWidth = 22
    val truncatedLeft = if (left.length > leftColWidth) left.take(leftColWidth) else left
    val paddedLeft = truncatedLeft.padEnd(leftColWidth)
    val paddedRight = right.padStart(rightColWidth)
    return paddedLeft + paddedRight
  }

  private fun formatItemHeader(): String {
    val col1 = "QTY / DESCRIPTION".padEnd(26)
    val col2 = "RATE".padStart(10)
    val col3 = "AMOUNT".padStart(12)
    return col1 + col2 + col3
  }

  /**
   * Structure where QUANTITY and DESCRIPTION are visually connected:
   * Line 1: 15 Box                      Rs. 260     Rs. 3,900
   * Line 2: VMG 1100 [12x24]
   */
  private fun formatItemRows(
    qtyStr: String,
    desc: String,
    rate: Double,
    amount: Double
  ): List<String> {
    val lines = mutableListOf<String>()
    val rateFormatted = "Rs. " + formatCompactAmount(rate)
    val amountFormatted = "Rs. " + formatCompactAmount(amount)

    val line1Col1 = qtyStr.padEnd(26)
    val line1Col2 = rateFormatted.padStart(10)
    val line1Col3 = amountFormatted.padStart(12)
    lines.add(line1Col1 + line1Col2 + line1Col3)

    // Description lines wrapped to 46 chars
    val maxDescLen = 46
    var remaining = desc
    while (remaining.isNotEmpty()) {
      if (remaining.length <= maxDescLen) {
        lines.add("  $remaining")
        break
      } else {
        var splitIdx = remaining.lastIndexOf(' ', maxDescLen)
        if (splitIdx <= 0) splitIdx = maxDescLen
        lines.add("  " + remaining.substring(0, splitIdx))
        remaining = remaining.substring(splitIdx).trimStart()
      }
    }
    return lines
  }

  private fun formatPaymentAndTotals(
    paymentMode: String,
    cartage: Double,
    grandTotal: Double
  ): String {
    val sb = StringBuilder()
    val modeLine = "PAYMENT MODE: $paymentMode"
    val gtVal = "Rs. " + formatCompactAmount(grandTotal)

    if (cartage > 0) {
      val subTotal = (grandTotal - cartage).coerceAtLeast(0.0)
      sb.appendLine(formatRowLeftRight(modeLine, "SUBTOTAL: " + formatCompactAmount(subTotal).padStart(10)))
      sb.appendLine(formatRowLeftRight("", "CARTAGE: " + formatCompactAmount(cartage).padStart(10)))
      sb.appendLine(formatRowLeftRight("", "-".repeat(24)))
      sb.append(formatRowLeftRight("", "GRAND TOTAL: " + gtVal.padStart(10)))
    } else {
      sb.append(formatRowLeftRight(modeLine, "GRAND TOTAL: " + gtVal))
    }
    return sb.toString()
  }

  private fun formatDateReceipt(timeMillis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timeMillis))
  }

  private fun formatTimeReceipt(timeMillis: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timeMillis))
  }

  private fun formatCompactAmount(amount: Double): String {
    val isWhole = kotlin.math.abs(amount - kotlin.math.round(amount)) < 0.005
    return java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
      minimumFractionDigits = if (isWhole) 0 else 2
      maximumFractionDigits = if (isWhole) 0 else 2
    }.format(amount)
  }

  private fun formatQuantity(qty: Double): String {
    return if (qty % 1.0 == 0.0) qty.toInt().toString() else "%.1f".format(qty)
  }

  // =========================================================================
  // CLASSIC HIGH-DEFINITION VECTOR-TO-RASTER THERMAL ENGINE (576px @ 203 DPI)
  // =========================================================================

  private fun createVectorPaint(
    sizePx: Float,
    isBold: Boolean,
    extraStrokePx: Float = 0f,
    letterSpacingEm: Float = 0.04f
  ): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
      color = Color.BLACK
      typeface = Typeface.create(Typeface.SANS_SERIF, if (isBold) Typeface.BOLD else Typeface.NORMAL)
      textSize = sizePx
      hinting = Paint.HINTING_ON
      if (extraStrokePx > 0.01f) {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = extraStrokePx
      } else {
        style = Paint.Style.FILL
        strokeWidth = 0f
      }
      letterSpacing = letterSpacingEm
    }
  }

  private fun wrapTextByPixels(text: String, paint: Paint, maxWidthPx: Float): List<String> {
    if (text.isBlank()) return listOf("")
    val words = text.trim().split(Regex("\\s+"))
    val lines = mutableListOf<String>()
    var currentLine = ""

    for (word in words) {
      val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
      if (paint.measureText(testLine) <= maxWidthPx) {
        currentLine = testLine
      } else {
        if (currentLine.isNotEmpty()) {
          lines.add(currentLine)
        }
        if (paint.measureText(word) > maxWidthPx) {
          var chunk = ""
          for (ch in word) {
            if (paint.measureText(chunk + ch) <= maxWidthPx) {
              chunk += ch
            } else {
              if (chunk.isNotEmpty()) lines.add(chunk)
              chunk = ch.toString()
            }
          }
          currentLine = chunk
        } else {
          currentLine = word
        }
      }
    }
    if (currentLine.isNotEmpty()) {
      lines.add(currentLine)
    }
    return if (lines.isEmpty()) listOf(text) else lines
  }

  /**
   * Renders any TransactionRecord as a professional, crystal-clear 576px-wide
   * monochrome thermal receipt matching the exact visual specification:
   * - ESTIMATE title at top
   * - 2-column customer metadata on left, Date/Time/S.NO./V.NO. (blank) on right
   * - Items with QUANTITY and DESCRIPTION visually connected
   * - Payment mode on left, Cartage & Grand Total on right
   * - NO RETURN / NO EXCHANGE and THANK YOU footer
   */
  private fun renderTransactionRasterEscPos(
    transaction: TransactionRecord,
    actualSerial: Long
  ): ByteArray? {
    return try {
      val items = transaction.itemsList
      val estHeight = (1050 + (items.size * 140) + (transaction.address.length * 3)).coerceAtLeast(1100)
      val bitmap = Bitmap.createBitmap(RASTER_WIDTH_DOTS, estHeight, Bitmap.Config.ARGB_8888) ?: return null
      val canvas = Canvas(bitmap)
      canvas.drawColor(Color.WHITE)

      val margin = 16f
      val rightEdge = RASTER_WIDTH_DOTS - margin // 560f
      val contentWidth = rightEdge - margin // 544f

      val paintTitle = createVectorPaint(sizePx = 32f, isBold = true, letterSpacingEm = 0.12f)
      val paintTableHeader = createVectorPaint(sizePx = 22f, isBold = true, letterSpacingEm = 0.05f)
      val paintLabel = createVectorPaint(sizePx = 22f, isBold = false, extraStrokePx = 0.15f, letterSpacingEm = 0.03f)
      val paintValue = createVectorPaint(sizePx = 22f, isBold = false, extraStrokePx = 0.22f, letterSpacingEm = 0.04f)
      val paintValueBold = createVectorPaint(sizePx = 23f, isBold = true, letterSpacingEm = 0.05f)
      val paintGrandTotal = createVectorPaint(sizePx = 28f, isBold = true, letterSpacingEm = 0.05f)
      val paintFooter = createVectorPaint(sizePx = 22f, isBold = true, letterSpacingEm = 0.08f)

      val paintSolidLine = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
      }

      var y = 42f

      fun drawTitleHeader(title: String) {
        val tw = paintTitle.measureText(title)
        canvas.drawText(title, (RASTER_WIDTH_DOTS - tw) / 2f, y, paintTitle)
        y += 16f
        canvas.drawRect(margin, y, rightEdge, y + 2.5f, paintSolidLine)
        y += 32f
      }

      fun drawSolidRule(thickness: Float = 1.5f, spacingAfter: Float = 26f) {
        canvas.drawRect(margin, y, rightEdge, y + thickness, paintSolidLine)
        y += spacingAfter
      }

      /**
       * 2-Column Metadata Layout:
       * Left:  NAME, NUMBER, ADDRESS
       * Right: DATE, TIME, S. NO., V. NO. (Blank: ___________)
       */
      fun drawCustomerAndMeta(partyHeader: String = "NAME") {
        val leftColX = margin
        val leftValX = margin + 115f
        val rightColX = 320f
        val rightValX = 405f

        val dateStr = formatDateReceipt(transaction.timestamp)
        val timeStr = formatTimeReceipt(transaction.timestamp)
        val serialStr = actualSerial.toString()
        val voucherBlank = "___________"

        // Row 1: NAME & DATE
        canvas.drawText("$partyHeader:", leftColX, y, paintLabel)
        val nameLines = wrapTextByPixels(transaction.personName.ifBlank { "WALK-IN" }, paintValueBold, 185f)
        canvas.drawText(nameLines[0], leftValX, y, paintValueBold)

        canvas.drawText("DATE :", rightColX, y, paintLabel)
        canvas.drawText(dateStr, rightValX, y, paintValue)
        y += 30f

        // If name wrapped to second line
        if (nameLines.size > 1) {
          canvas.drawText(nameLines[1], leftValX, y, paintValueBold)
        }

        // Row 2: NUMBER & TIME
        val phones = transaction.phoneNumbersList
        val phoneStr = phones.firstOrNull().orEmpty().ifBlank { "-" }
        canvas.drawText("NUMBER:", leftColX, y, paintLabel)
        canvas.drawText(phoneStr, leftValX, y, paintValue)

        canvas.drawText("TIME :", rightColX, y, paintLabel)
        canvas.drawText(timeStr, rightValX, y, paintValue)
        y += 30f

        // Row 3: ADDRESS & S. NO.
        val addrStr = transaction.address.ifBlank { "-" }
        val addrLines = wrapTextByPixels(addrStr, paintValue, 185f)
        canvas.drawText("ADDRESS:", leftColX, y, paintLabel)
        canvas.drawText(addrLines[0], leftValX, y, paintValue)

        canvas.drawText("S. NO.:", rightColX, y, paintLabel)
        canvas.drawText(serialStr, rightValX, y, paintValueBold)
        y += 30f

        // Row 4: (Address overflow or REF NO) & V. NO. (Blank)
        if (addrLines.size > 1) {
          canvas.drawText(addrLines[1], leftValX, y, paintValue)
        } else if (transaction.referenceNumber.isNotBlank()) {
          canvas.drawText("REF NO :", leftColX, y, paintLabel)
          canvas.drawText(transaction.referenceNumber, leftValX, y, paintValue)
        }

        canvas.drawText("V. NO.:", rightColX, y, paintLabel)
        canvas.drawText(voucherBlank, rightValX, y, paintValue)
        y += 32f

        if (addrLines.size > 1 && transaction.referenceNumber.isNotBlank()) {
          canvas.drawText("REF NO :", leftColX, y, paintLabel)
          canvas.drawText(transaction.referenceNumber, leftValX, y, paintValue)
          y += 30f
        }
      }

      /**
       * Items Section where QUANTITY and DESCRIPTION are visually connected:
       * Row 1: 15 Box                    ₹260      ₹3,900
       * Row 2: VMG 1100
       * Row 3: 12x24
       */
      fun drawConnectedItemsTable(isChallanNoAmount: Boolean = false) {
        y -= 4f
        drawSolidRule(thickness = 1.5f, spacingAfter = 24f)

        val rateColRight = 440f
        val amountColRight = rightEdge

        // Table Header
        canvas.drawText("QTY / DESCRIPTION", margin, y, paintTableHeader)
        if (!isChallanNoAmount) {
          canvas.drawText("RATE", rateColRight - paintTableHeader.measureText("RATE"), y, paintTableHeader)
          canvas.drawText("AMOUNT", amountColRight - paintTableHeader.measureText("AMOUNT"), y, paintTableHeader)
        }
        y += 10f
        drawSolidRule(thickness = 1.5f, spacingAfter = 28f)

        val paintQty = createVectorPaint(sizePx = 23f, isBold = true, letterSpacingEm = 0.05f)
        val paintDesc = createVectorPaint(sizePx = 22f, isBold = false, extraStrokePx = 0.22f, letterSpacingEm = 0.04f)
        val paintRate = createVectorPaint(sizePx = 22f, isBold = false, extraStrokePx = 0.20f, letterSpacingEm = 0.04f)
        val paintAmount = createVectorPaint(sizePx = 23f, isBold = true, letterSpacingEm = 0.05f)
        val paintSize = createVectorPaint(sizePx = 20.5f, isBold = false, extraStrokePx = 0.18f, letterSpacingEm = 0.03f)

        items.forEachIndexed { index, item ->
          val qtyStr = "${formatQuantity(item.quantity)} ${item.unit}"
          val rateStr = "₹" + formatCompactAmount(item.rate)
          val amountStr = "₹" + formatCompactAmount(item.itemTotal)

          // Line 1: Quantity (Left) and Rate & Amount (Right)
          canvas.drawText(qtyStr, margin, y, paintQty)
          if (!isChallanNoAmount) {
            canvas.drawText(rateStr, rateColRight - paintRate.measureText(rateStr), y, paintRate)
            canvas.drawText(amountStr, amountColRight - paintAmount.measureText(amountStr), y, paintAmount)
          }
          y += 28f

          // Line 2: Product Name / Design Number
          val descLines = wrapTextByPixels(item.designName, paintDesc, contentWidth - 10f)
          canvas.drawText(descLines[0], margin + 8f, y, paintDesc)
          y += 26f
          for (i in 1 until descLines.size) {
            canvas.drawText(descLines[i], margin + 8f, y, paintDesc)
            y += 26f
          }

          // Line 3: Size (if present)
          if (item.size.isNotBlank()) {
            val sizeStr = if (item.size.contains("×") || item.size.contains("*")) item.size else "[${item.size}]"
            canvas.drawText(sizeStr, margin + 8f, y, paintSize)
            y += 24f
          }

          // Spacing between items
          if (index < items.size - 1) {
            y += 8f
          }
        }
      }

      /**
       * Payment Mode (Left) + Totals Block (Right)
       */
      fun drawPaymentModeAndTotals(paymentModeText: String) {
        y -= 4f
        drawSolidRule(thickness = 1.5f, spacingAfter = 26f)

        val totalsLeftX = 260f
        val modePaint = createVectorPaint(sizePx = 23f, isBold = true, letterSpacingEm = 0.05f)

        // Draw Payment mode on the left side
        canvas.drawText("PAYMENT MODE:", margin, y, paintLabel)
        canvas.drawText(paymentModeText, margin, y + 26f, modePaint)

        // Draw Totals on the right side
        if (transaction.cartage > 0) {
          val subTotal = (transaction.totalAmount - transaction.cartage).coerceAtLeast(0.0)
          val subStr = "₹" + formatCompactAmount(subTotal)
          canvas.drawText("SUBTOTAL", totalsLeftX, y, paintLabel)
          canvas.drawText(subStr, rightEdge - paintValue.measureText(subStr), y, paintValue)
          y += 28f

          val cartStr = "₹" + formatCompactAmount(transaction.cartage)
          canvas.drawText("CARTAGE", totalsLeftX, y, paintLabel)
          canvas.drawText(cartStr, rightEdge - paintValueBold.measureText(cartStr), y, paintValueBold)
          y += 28f

          // Small divider under subtotal & cartage
          canvas.drawRect(totalsLeftX, y, rightEdge, y + 1.2f, paintSolidLine)
          y += 22f
        }

        // Grand Total row
        val gtStr = "₹" + formatCompactAmount(transaction.totalAmount)
        canvas.drawText("GRAND TOTAL", totalsLeftX, y, paintGrandTotal)
        canvas.drawText(gtStr, rightEdge - paintGrandTotal.measureText(gtStr), y, paintGrandTotal)
        y += 14f

        drawSolidRule(thickness = 1.5f, spacingAfter = 28f)
      }

      fun drawFooter() {
        val s1 = "NO RETURN / NO EXCHANGE"
        val s2 = "THANK YOU"
        canvas.drawText(s1, (RASTER_WIDTH_DOTS - paintFooter.measureText(s1)) / 2f, y, paintFooter)
        y += 30f
        canvas.drawText(s2, (RASTER_WIDTH_DOTS - paintFooter.measureText(s2)) / 2f, y, paintFooter)
        y += 28f
      }

      when (transaction.type) {
        "CASH_SALE" -> {
          drawTitleHeader("ESTIMATE")
          drawCustomerAndMeta("NAME")
          drawConnectedItemsTable()
          drawPaymentModeAndTotals("CASH")
          drawFooter()
        }

        "ONLINE_SALE" -> {
          drawTitleHeader("ESTIMATE")
          drawCustomerAndMeta("NAME")
          drawConnectedItemsTable()
          val mode = if (transaction.bankAccountName.isNotBlank()) "UPI (${transaction.bankAccountName})" else "UPI"
          drawPaymentModeAndTotals(mode)
          drawFooter()
        }

        "CREDIT_SALE" -> {
          drawTitleHeader("ESTIMATE")
          drawCustomerAndMeta("NAME")
          val isChallanNoAmount = transaction.totalAmount <= 0.0
          drawConnectedItemsTable(isChallanNoAmount = isChallanNoAmount)

          if (isChallanNoAmount) {
            y -= 4f
            drawSolidRule(thickness = 1.5f, spacingAfter = 26f)
            canvas.drawText("PAYMENT MODE: CREDIT", margin, y, paintValueBold)
            y += 34f
            canvas.drawText("DELIVERY PERSON : __________________", margin, y, paintLabel)
            y += 32f
            canvas.drawText("SIGNATURE       : __________________", margin, y, paintLabel)
            y += 14f
            drawSolidRule(thickness = 1.5f, spacingAfter = 28f)
          } else {
            drawPaymentModeAndTotals("CREDIT")
          }
          drawFooter()
        }

        "GOODS_RETURN" -> {
          drawTitleHeader("GOODS RETURN")
          drawCustomerAndMeta("NAME")
          y -= 4f
          drawSolidRule(thickness = 1.5f, spacingAfter = 24f)
          canvas.drawText("RETURNED ITEMS", margin, y, paintTableHeader)
          y += 10f
          drawSolidRule(thickness = 1.5f, spacingAfter = 28f)

          val paintQty = createVectorPaint(sizePx = 23f, isBold = true, letterSpacingEm = 0.05f)
          val paintDesc = createVectorPaint(sizePx = 22f, isBold = false, extraStrokePx = 0.22f, letterSpacingEm = 0.04f)

          items.forEachIndexed { index, item ->
            val qtyStr = "${formatQuantity(item.quantity)} ${item.unit}"
            canvas.drawText(qtyStr, margin, y, paintQty)
            y += 28f
            val nameStr = if (item.size.isNotBlank()) "${item.designName} [${item.size}]" else item.designName
            canvas.drawText(nameStr, margin + 8f, y, paintDesc)
            y += 26f
            if (index < items.size - 1) y += 6f
          }

          y -= 4f
          drawSolidRule(thickness = 1.5f, spacingAfter = 26f)
          val refStr = if (transaction.totalAmount > 0) "₹" + formatCompactAmount(transaction.totalAmount) else "NIL"
          canvas.drawText("REFUND:", margin, y, paintGrandTotal)
          canvas.drawText(refStr, rightEdge - paintGrandTotal.measureText(refStr), y, paintGrandTotal)
          y += 14f
          drawSolidRule(thickness = 1.5f, spacingAfter = 28f)
          drawFooter()
        }

        "CASH_RECEIPT" -> {
          drawTitleHeader("RECEIPT")
          drawCustomerAndMeta("RECEIVED")
          y -= 4f
          drawSolidRule(thickness = 1.5f, spacingAfter = 26f)

          val mode = if (transaction.receiptMethod.equals("Online", ignoreCase = true)) {
            if (transaction.bankAccountName.isNotBlank()) "UPI (${transaction.bankAccountName})" else "UPI"
          } else {
            "CASH"
          }
          canvas.drawText("PAYMENT MODE: $mode", margin, y, paintValueBold)
          y += 32f

          val amtStr = "₹" + formatCompactAmount(transaction.totalAmount)
          canvas.drawText("AMOUNT RECEIVED", margin, y, paintGrandTotal)
          canvas.drawText(amtStr, rightEdge - paintGrandTotal.measureText(amtStr), y, paintGrandTotal)
          y += 14f
          drawSolidRule(thickness = 1.5f, spacingAfter = 28f)

          val s2 = "THANK YOU"
          canvas.drawText(s2, (RASTER_WIDTH_DOTS - paintFooter.measureText(s2)) / 2f, y, paintFooter)
          y += 28f
        }

        "CASH_GIVE" -> {
          drawTitleHeader("PAYMENT")
          drawCustomerAndMeta("PAID TO")
          y -= 4f
          drawSolidRule(thickness = 1.5f, spacingAfter = 26f)

          canvas.drawText("PAYMENT MODE: CASH", margin, y, paintValueBold)
          y += 32f

          val amtStr = "₹" + formatCompactAmount(transaction.totalAmount)
          canvas.drawText("AMOUNT PAID", margin, y, paintGrandTotal)
          canvas.drawText(amtStr, rightEdge - paintGrandTotal.measureText(amtStr), y, paintGrandTotal)
          y += 14f
          drawSolidRule(thickness = 1.5f, spacingAfter = 28f)

          val s2 = "THANK YOU"
          canvas.drawText(s2, (RASTER_WIDTH_DOTS - paintFooter.measureText(s2)) / 2f, y, paintFooter)
          y += 28f
        }
      }

      val finalHeight = (y + 12f).toInt().coerceIn(80, bitmap.height)
      val bytes = bitmapToEscPosRaster(bitmap, finalHeight)
      bitmap.recycle()
      bytes
    } catch (_: Throwable) {
      null
    }
  }

  private fun renderDaySummaryRasterEscPos(group: DayHistoryGroup): ByteArray? {
    return try {
      val estHeight = (750 + group.transactions.size * 155).coerceAtLeast(850)
      val bitmap = Bitmap.createBitmap(RASTER_WIDTH_DOTS, estHeight, Bitmap.Config.ARGB_8888) ?: return null
      val canvas = Canvas(bitmap)
      canvas.drawColor(Color.WHITE)

      val margin = 16f
      val rightEdge = RASTER_WIDTH_DOTS - margin
      val contentWidth = rightEdge - margin

      val paintTitle = createVectorPaint(sizePx = 28f, isBold = true, letterSpacingEm = 0.08f)
      val paintHeader = createVectorPaint(sizePx = 22f, isBold = true, letterSpacingEm = 0.06f)
      val paintLight = createVectorPaint(sizePx = 23f, isBold = false, extraStrokePx = 0.20f, letterSpacingEm = 0.055f)
      val paintDarkBold = createVectorPaint(sizePx = 23.5f, isBold = true, letterSpacingEm = 0.065f)
      val paintSolidLine = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
      }

      var y = 38f

      val t1 = "WAREHOUSE REGISTER"
      canvas.drawText(t1, (RASTER_WIDTH_DOTS - paintTitle.measureText(t1)) / 2f, y, paintTitle)
      y += 30f
      val t2 = "FULL DAY SUMMARY"
      canvas.drawText(t2, (RASTER_WIDTH_DOTS - paintHeader.measureText(t2)) / 2f, y, paintHeader)
      y += 14f
      canvas.drawRect(margin, y, rightEdge, y + 2.5f, paintSolidLine)
      y += 30f

      fun drawLR(left: String, right: String, boldLeft: Boolean = false) {
        val lp = if (boldLeft) paintDarkBold else paintLight
        canvas.drawText(left, margin, y, lp)
        canvas.drawText(right, rightEdge - paintDarkBold.measureText(right), y, paintDarkBold)
        y += 31f
      }

      fun drawDash() {
        var x = margin
        while (x < rightEdge) {
          canvas.drawRect(x, y, (x + 8f).coerceAtMost(rightEdge), y + 1.5f, paintSolidLine)
          x += 14f
        }
        y += 28f
      }

      drawLR("Date: ${group.dateFormatted}", "Entries: ${group.transactions.size}")
      y -= 6f
      drawDash()

      drawLR("CASH IN", "Rs. " + formatCompactAmount(group.totalCashReceived))
      drawLR("CASH OUT", "Rs. " + formatCompactAmount(group.totalCashGiven))
      if (group.totalOnlineReceived > 0) {
        drawLR("ONLINE IN", "Rs. " + formatCompactAmount(group.totalOnlineReceived))
      }
      val net = group.totalCashReceived - group.totalCashGiven
      y -= 6f
      drawDash()
      drawLR("NET CASH", "Rs. " + formatCompactAmount(net), boldLeft = true)

      y -= 6f
      canvas.drawRect(margin, y, rightEdge, y + 2.0f, paintSolidLine)
      y += 28f

      group.transactions.forEachIndexed { index, tx ->
        val serial = if (tx.serialNumber > 0L) tx.serialNumber else (index + 1).toLong()
        val typeLabel = when (tx.type) {
          "CASH_SALE" -> "SALE — CASH"
          "ONLINE_SALE" -> "SALE — UPI"
          "CREDIT_SALE" -> "SALE — CREDIT"
          "CASH_RECEIPT" -> if (tx.receiptMethod.equals("Online", ignoreCase = true)) "RECEIPT — UPI" else "RECEIPT — CASH"
          "CASH_GIVE" -> "PAYMENT — CASH"
          "GOODS_RETURN" -> "GOODS RETURN"
          else -> tx.type
        }
        val amtRight = if (tx.type == "CREDIT_SALE") typeLabel else "Rs. ${formatCompactAmount(tx.totalAmount)}"
        val leftHeader = "#$serial ${tx.personName.ifBlank { "-" }}"
        val availLeft = (contentWidth - paintDarkBold.measureText(amtRight) - 16f).coerceAtLeast(180f)
        val headerLines = wrapTextByPixels(leftHeader, paintDarkBold, availLeft)
        canvas.drawText(headerLines[0], margin, y, paintDarkBold)
        canvas.drawText(amtRight, rightEdge - paintDarkBold.measureText(amtRight), y, paintDarkBold)
        y += 28f

        val subMeta = "${tx.formattedTime} • $typeLabel" + (if (tx.referenceNumber.isNotBlank()) " • Ref:${tx.referenceNumber}" else "")
        canvas.drawText(subMeta, margin, y, paintLight)
        y += 27f

        if (tx.shortItemsDescription.isNotBlank()) {
          val itemLines = wrapTextByPixels(tx.shortItemsDescription, paintLight, contentWidth)
          canvas.drawText(itemLines[0], margin, y, paintLight)
          y += 27f
        }
        if (index < group.transactions.size - 1) {
          y -= 6f
          drawDash()
        }
      }

      y -= 4f
      canvas.drawRect(margin, y, rightEdge, y + 2.0f, paintSolidLine)
      y += 28f
      val endStr = "END OF DAY REPORT"
      canvas.drawText(endStr, (RASTER_WIDTH_DOTS - paintHeader.measureText(endStr)) / 2f, y, paintHeader)
      y += 16f

      val finalHeight = (y + 8f).toInt().coerceIn(80, bitmap.height)
      val bytes = bitmapToEscPosRaster(bitmap, finalHeight)
      bitmap.recycle()
      bytes
    } catch (_: Throwable) {
      null
    }
  }

  private fun renderTestPrintRasterEscPos(): ByteArray? {
    return try {
      val bitmap = Bitmap.createBitmap(RASTER_WIDTH_DOTS, 650, Bitmap.Config.ARGB_8888) ?: return null
      val canvas = Canvas(bitmap)
      canvas.drawColor(Color.WHITE)

      val margin = 16f
      val rightEdge = RASTER_WIDTH_DOTS - margin
      val paintTitle = createVectorPaint(sizePx = 32f, isBold = true, letterSpacingEm = 0.12f)
      val paintSub = createVectorPaint(sizePx = 22f, isBold = true, letterSpacingEm = 0.05f)
      val paintLight = createVectorPaint(sizePx = 22f, isBold = false, extraStrokePx = 0.18f, letterSpacingEm = 0.04f)
      val paintDark = createVectorPaint(sizePx = 23f, isBold = true, letterSpacingEm = 0.05f)
      val paintLine = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

      var y = 42f
      val t1 = "ESTIMATE"
      canvas.drawText(t1, (RASTER_WIDTH_DOTS - paintTitle.measureText(t1)) / 2f, y, paintTitle)
      y += 16f
      canvas.drawRect(margin, y, rightEdge, y + 2.5f, paintLine)
      y += 32f

      val leftColX = margin
      val leftValX = margin + 115f
      val rightColX = 320f
      val rightValX = 405f

      canvas.drawText("NAME:", leftColX, y, paintLight)
      canvas.drawText("SAMPLE CUSTOMER", leftValX, y, paintDark)
      canvas.drawText("DATE :", rightColX, y, paintLight)
      canvas.drawText(formatDateReceipt(System.currentTimeMillis()), rightValX, y, paintLight)
      y += 30f

      canvas.drawText("NUMBER:", leftColX, y, paintLight)
      canvas.drawText("9876543210", leftValX, y, paintLight)
      canvas.drawText("TIME :", rightColX, y, paintLight)
      canvas.drawText(formatTimeReceipt(System.currentTimeMillis()), rightValX, y, paintLight)
      y += 30f

      canvas.drawText("ADDRESS:", leftColX, y, paintLight)
      canvas.drawText("MAIN MARKET", leftValX, y, paintLight)
      canvas.drawText("S. NO.:", rightColX, y, paintLight)
      canvas.drawText("1", rightValX, y, paintDark)
      y += 30f

      canvas.drawText("V. NO.:", rightColX, y, paintLight)
      canvas.drawText("___________", rightValX, y, paintLight)
      y += 32f

      canvas.drawRect(margin, y, rightEdge, y + 1.5f, paintLine)
      y += 24f

      canvas.drawText("QTY / DESCRIPTION", margin, y, paintSub)
      canvas.drawText("RATE", 440f - paintSub.measureText("RATE"), y, paintSub)
      canvas.drawText("AMOUNT", rightEdge - paintSub.measureText("AMOUNT"), y, paintSub)
      y += 10f
      canvas.drawRect(margin, y, rightEdge, y + 1.5f, paintLine)
      y += 28f

      canvas.drawText("15 Box", margin, y, paintDark)
      canvas.drawText("₹260", 440f - paintLight.measureText("₹260"), y, paintLight)
      canvas.drawText("₹3,900", rightEdge - paintDark.measureText("₹3,900"), y, paintDark)
      y += 26f
      canvas.drawText("1100 GLOSS [12x24]", margin + 8f, y, paintLight)
      y += 32f

      canvas.drawRect(margin, y, rightEdge, y + 1.5f, paintLine)
      y += 26f

      canvas.drawText("PAYMENT MODE:", margin, y, paintLight)
      canvas.drawText("CASH", margin, y + 26f, paintDark)
      canvas.drawText("GRAND TOTAL", 260f, y + 14f, paintTitle)
      val gt = "₹3,900"
      canvas.drawText(gt, rightEdge - paintTitle.measureText(gt), y + 14f, paintTitle)
      y += 48f

      canvas.drawRect(margin, y, rightEdge, y + 1.5f, paintLine)
      y += 28f

      val s1 = "NO RETURN / NO EXCHANGE"
      val s2 = "THANK YOU"
      canvas.drawText(s1, (RASTER_WIDTH_DOTS - paintSub.measureText(s1)) / 2f, y, paintSub)
      y += 28f
      canvas.drawText(s2, (RASTER_WIDTH_DOTS - paintSub.measureText(s2)) / 2f, y, paintSub)
      y += 20f

      val finalHeight = (y + 10f).toInt().coerceIn(80, bitmap.height)
      val bytes = bitmapToEscPosRaster(bitmap, finalHeight)
      bitmap.recycle()
      bytes
    } catch (_: Throwable) {
      null
    }
  }

  private fun bitmapToEscPosRaster(bitmap: Bitmap, heightToUse: Int): ByteArray {
    val width = RASTER_WIDTH_DOTS
    val height = heightToUse.coerceIn(1, bitmap.height)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    var lastBlackRow = height - 1
    rowLoop@ while (lastBlackRow > 20) {
      val rowOffset = lastBlackRow * width
      for (x in 0 until width) {
        val c = pixels[rowOffset + x]
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        if (r < 220 || g < 220 || b < 220) {
          break@rowLoop
        }
      }
      lastBlackRow--
    }
    val croppedHeight = (lastBlackRow + 8).coerceAtMost(height)

    val out = ByteArrayOutputStream()
    out.write(ESC_INIT)
    out.write(ESC_HEAT_SETTINGS)
    out.write(ESC_ALIGN_LEFT)

    val stripRows = 24
    var yStart = 0
    while (yStart < croppedHeight) {
      val h = (croppedHeight - yStart).coerceAtMost(stripRows)
      val header = byteArrayOf(
        0x1D, 0x76, 0x30, 0x00,
        (RASTER_WIDTH_BYTES and 0xFF).toByte(),
        ((RASTER_WIDTH_BYTES shr 8) and 0xFF).toByte(),
        (h and 0xFF).toByte(),
        ((h shr 8) and 0xFF).toByte()
      )
      out.write(header)

      for (y in yStart until (yStart + h)) {
        val rowOffset = y * width
        for (bx in 0 until RASTER_WIDTH_BYTES) {
          var sliceByte = 0
          val baseX = bx * 8
          for (bit in 0 until 8) {
            val c = pixels[rowOffset + baseX + bit]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            if (lum < 138) {
              sliceByte = sliceByte or (1 shl (7 - bit))
            }
          }
          out.write(sliceByte)
        }
      }
      yStart += h
    }

    out.write(ESC_FEED_MINIMAL)
    out.write(ESC_CUT)
    return out.toByteArray()
  }
}
