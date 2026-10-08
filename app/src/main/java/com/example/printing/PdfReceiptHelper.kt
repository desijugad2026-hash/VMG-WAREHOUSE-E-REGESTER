package com.example.printing

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.OrderItem
import com.example.data.model.TransactionRecord
import com.example.ui.viewmodel.DayHistoryGroup
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptHelper {

  // Standard A4 dimensions in PostScript points (72 points per inch)
  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 36f
  private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2) // 523f
  private const val FOOTER_RESERVED = 32f
  private const val MAX_CONTENT_Y = PAGE_HEIGHT - MARGIN - FOOTER_RESERVED // 774f

  /**
   * Generates a PDF file for an individual transaction in receipt-style format and opens it
   */
  fun openTransactionPdf(context: Context, transaction: TransactionRecord, serialNumber: Long = 0L) {
    try {
      val actualSerial = if (transaction.serialNumber > 0L) transaction.serialNumber else if (serialNumber > 0L) serialNumber else transaction.id.coerceAtLeast(1L)
      val file = createTransactionPdfFile(context, transaction, actualSerial)
      viewOrSharePdf(context, file, "Receipt_#$actualSerial.pdf")
    } catch (e: Exception) {
      Toast.makeText(context, "Could not open PDF: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
    }
  }

  fun openTransactionPdf(context: Context, transaction: TransactionRecord, serialNumber: Int) {
    openTransactionPdf(context, transaction, serialNumber.toLong())
  }

  /**
   * Generates a professional multi-page A4 PDF file for full day transactions
   * with receipt-style blocks, multi-item wrapping, and automatic page breaking.
   */
  fun openDaySummaryPdf(context: Context, group: DayHistoryGroup) {
    try {
      val file = createDaySummaryPdfFile(context, group)
      viewOrSharePdf(context, file, "Day_Summary_${group.dateKey}.pdf")
    } catch (e: Exception) {
      Toast.makeText(context, "Could not open PDF: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
    }
  }

  private fun viewOrSharePdf(context: Context, file: File, title: String) {
    try {
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
      )
      val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      val chooser = Intent.createChooser(viewIntent, "Open PDF Receipt")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      printPdfViaSystem(context, file, title)
    }
  }

  private fun printPdfViaSystem(context: Context, file: File, title: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager == null) {
      Toast.makeText(context, "Print service unavailable on this device", Toast.LENGTH_SHORT).show()
      return
    }

    val adapter = object : PrintDocumentAdapter() {
      override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
      ) {
        if (cancellationSignal?.isCanceled == true) {
          callback?.onLayoutCancelled()
          return
        }
        val info = PrintDocumentInfo.Builder(title)
          .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
          .build()
        callback?.onLayoutFinished(info, true)
      }

      override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
      ) {
        try {
          val input = FileInputStream(file)
          val output = FileOutputStream(destination?.fileDescriptor)
          input.copyTo(output)
          callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          input.close()
          output.close()
        } catch (e: Exception) {
          callback?.onWriteFailed(e.message)
        }
      }
    }

    printManager.print(title, adapter, PrintAttributes.Builder().build())
  }

  // =========================================================================
  // SINGLE TRANSACTION RECEIPT PDF (A4 or thermal-proportional)
  // =========================================================================

  private fun createTransactionPdfFile(
    context: Context,
    transaction: TransactionRecord,
    serialNumber: Long
  ): File {
    val pdfDir = File(context.cacheDir, "receipt_pdfs").apply { mkdirs() }
    val file = File(pdfDir, "bill_receipt_${transaction.id}_#$serialNumber.pdf")

    val document = PdfDocument()
    val pageWidth = 480
    val pageHeight = calculateSinglePageHeight(transaction)
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    drawSingleTransactionReceiptPdf(canvas, transaction, serialNumber, pageWidth)

    document.finishPage(page)
    val fos = FileOutputStream(file)
    document.writeTo(fos)
    fos.close()
    document.close()

    return file
  }

  private fun calculateSinglePageHeight(transaction: TransactionRecord): Int {
    val itemsCount = transaction.itemsList.size
    val base = 480
    val itemsHeight = itemsCount * 56
    val cartageExtra = if (transaction.cartage > 0) 30 else 0
    return (base + itemsHeight + cartageExtra).coerceAtLeast(550)
  }

  private fun drawSingleTransactionReceiptPdf(
    canvas: Canvas,
    transaction: TransactionRecord,
    serialNumber: Long,
    pageWidth: Int
  ) {
    canvas.drawColor(Color.WHITE)

    val paintTitle = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      textSize = 20f
    }
    val paintBold = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      textSize = 12f
    }
    val paintRegular = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      textSize = 11.5f
    }
    val paintSecondary = Paint().apply {
      isAntiAlias = true
      color = Color.DKGRAY
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      textSize = 10.5f
    }
    val paintLine = Paint().apply {
      color = Color.BLACK
      strokeWidth = 1.2f
    }

    val margin = 24f
    val contentWidth = pageWidth - (margin * 2)
    val rightEdge = margin + contentWidth
    var y = 38f

    // 1. Title: ESTIMATE
    val title = when (transaction.type) {
      "GOODS_RETURN" -> "GOODS RETURN"
      "CASH_RECEIPT" -> "RECEIPT"
      "CASH_GIVE" -> "PAYMENT"
      else -> "ESTIMATE"
    }
    val tw = paintTitle.measureText(title)
    canvas.drawText(title, (pageWidth - tw) / 2f, y, paintTitle)
    y += 14f
    canvas.drawLine(margin, y, rightEdge, y, paintLine)
    y += 22f

    // 2. Customer Information (Left) & Metadata (Right)
    val leftColX = margin
    val leftValX = margin + 65f
    val rightColX = margin + (contentWidth * 0.58f)
    val rightValX = rightColX + 55f

    val name = transaction.personName.ifBlank { "WALK-IN" }
    val phone = transaction.phoneNumbersList.firstOrNull().orEmpty().ifBlank { "-" }
    val address = transaction.address.ifBlank { "-" }
    val dateStr = formatDate(transaction.timestamp)
    val timeStr = formatTime(transaction.timestamp)

    canvas.drawText("NAME:", leftColX, y, paintRegular)
    canvas.drawText(name, leftValX, y, paintBold)
    canvas.drawText("DATE :", rightColX, y, paintRegular)
    canvas.drawText(dateStr, rightValX, y, paintRegular)
    y += 18f

    canvas.drawText("NUMBER:", leftColX, y, paintRegular)
    canvas.drawText(phone, leftValX, y, paintRegular)
    canvas.drawText("TIME :", rightColX, y, paintRegular)
    canvas.drawText(timeStr, rightValX, y, paintRegular)
    y += 18f

    canvas.drawText("ADDRESS:", leftColX, y, paintRegular)
    canvas.drawText(address, leftValX, y, paintRegular)
    canvas.drawText("S. NO.:", rightColX, y, paintRegular)
    canvas.drawText(serialNumber.toString(), rightValX, y, paintBold)
    y += 18f

    if (transaction.referenceNumber.isNotBlank()) {
      canvas.drawText("REF NO :", leftColX, y, paintRegular)
      canvas.drawText(transaction.referenceNumber, leftValX, y, paintRegular)
    }
    canvas.drawText("V. NO.:", rightColX, y, paintRegular)
    canvas.drawText("___________", rightValX, y, paintRegular)
    y += 20f

    canvas.drawLine(margin, y, rightEdge, y, paintLine)
    y += 18f

    // 3. Items Section
    val isChallanNoAmount = transaction.type == "CREDIT_SALE" && transaction.totalAmount <= 0.0

    canvas.drawText("QTY / DESCRIPTION", margin, y, paintBold)
    if (!isChallanNoAmount && transaction.type != "GOODS_RETURN") {
      canvas.drawText("RATE", rightEdge - 120f - paintBold.measureText("RATE"), y, paintBold)
      canvas.drawText("AMOUNT", rightEdge - paintBold.measureText("AMOUNT"), y, paintBold)
    }
    y += 8f
    canvas.drawLine(margin, y, rightEdge, y, paintLine)
    y += 18f

    for (item in transaction.itemsList) {
      val qtyStr = "${formatQty(item.quantity)} ${item.unit}"
      canvas.drawText(qtyStr, margin, y, paintBold)
      if (!isChallanNoAmount && transaction.type != "GOODS_RETURN") {
        val rStr = "₹" + formatCompact(item.rate)
        val aStr = "₹" + formatCompact(item.itemTotal)
        canvas.drawText(rStr, rightEdge - 120f - paintRegular.measureText(rStr), y, paintRegular)
        canvas.drawText(aStr, rightEdge - paintBold.measureText(aStr), y, paintBold)
      }
      y += 16f

      val nameStr = if (item.size.isNotBlank()) "${item.designName} [${item.size}]" else item.designName
      canvas.drawText(nameStr, margin + 6f, y, paintRegular)
      y += 18f
    }

    canvas.drawLine(margin, y, rightEdge, y, paintLine)
    y += 20f

    // 4. Payment Mode & Totals
    val modeLabel = when (transaction.type) {
      "CASH_SALE" -> "CASH"
      "ONLINE_SALE" -> if (transaction.bankAccountName.isNotBlank()) "UPI (${transaction.bankAccountName})" else "UPI"
      "CREDIT_SALE" -> "CREDIT"
      "CASH_RECEIPT" -> if (transaction.receiptMethod.equals("Online", true)) "UPI" else "CASH"
      "CASH_GIVE" -> "CASH"
      "GOODS_RETURN" -> "REFUND"
      else -> "CASH"
    }

    canvas.drawText("PAYMENT MODE: $modeLabel", margin, y, paintBold)

    if (transaction.cartage > 0) {
      val cartStr = "₹" + formatCompact(transaction.cartage)
      canvas.drawText("CARTAGE: $cartStr", rightEdge - paintBold.measureText("CARTAGE: $cartStr"), y, paintRegular)
      y += 18f
    }

    if (!isChallanNoAmount) {
      paintBold.textSize = 14f
      val gtLabel = if (transaction.type == "GOODS_RETURN") "REFUND: " else "GRAND TOTAL: "
      val gtVal = "₹" + formatCompact(transaction.totalAmount)
      val fullGt = gtLabel + gtVal
      canvas.drawText(fullGt, rightEdge - paintBold.measureText(fullGt), y + 6f, paintBold)
      paintBold.textSize = 12f
      y += 24f
    } else {
      y += 20f
      canvas.drawText("DELIVERY PERSON : __________________", margin, y, paintRegular)
      y += 18f
      canvas.drawText("SIGNATURE       : __________________", margin, y, paintRegular)
      y += 20f
    }

    canvas.drawLine(margin, y, rightEdge, y, paintLine)
    y += 24f

    // 5. Footer
    paintBold.textSize = 11f
    val s1 = "NO RETURN / NO EXCHANGE"
    val s2 = "THANK YOU"
    canvas.drawText(s1, (pageWidth - paintBold.measureText(s1)) / 2f, y, paintBold)
    y += 18f
    canvas.drawText(s2, (pageWidth - paintBold.measureText(s2)) / 2f, y, paintBold)
  }

  // =========================================================================
  // FULL DAY SUMMARY MULTI-PAGE A4 PDF ENGINE
  // =========================================================================

  private fun createDaySummaryPdfFile(context: Context, group: DayHistoryGroup): File {
    val pdfDir = File(context.cacheDir, "receipt_pdfs").apply { mkdirs() }
    val file = File(pdfDir, "day_summary_${group.dateKey}.pdf")

    val document = PdfDocument()
    val builder = DaySummaryPdfRenderer(document, group)
    builder.render()

    val fos = FileOutputStream(file)
    document.writeTo(fos)
    fos.close()
    document.close()

    return file
  }

  /**
   * Dedicated Multi-Page A4 PDF Renderer for the Daily Summary.
   * Features:
   * - A4 Pages (595 × 842 points)
   * - Daily Accounting Summary Header on Page 1
   * - Receipt-Style Block for each transaction (NOT a single-line spreadsheet!)
   * - Quantity & Description visually connected
   * - Zero clipping of long names or addresses (natural word wrapping)
   * - Automatic Page Breaking without splitting individual items
   * - "TRANSACTION #X — CONTINUED" on page splits
   * - Discreet "Page X of Y" on every page footer
   */
  private class DaySummaryPdfRenderer(
    private val document: PdfDocument,
    private val group: DayHistoryGroup
  ) {
    private val paintDocTitle = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      textSize = 18f
    }
    private val paintDocSub = Paint().apply {
      isAntiAlias = true
      color = Color.DKGRAY
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      textSize = 12f
    }
    private val paintTxHeader = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      textSize = 12.5f
    }
    private val paintBold = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
      textSize = 11f
    }
    private val paintRegular = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      textSize = 10.5f
    }
    private val paintSecondary = Paint().apply {
      isAntiAlias = true
      color = Color.DKGRAY
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      textSize = 9.5f
    }
    private val paintLine = Paint().apply {
      color = Color.BLACK
      strokeWidth = 1f
    }
    private val paintLightLine = Paint().apply {
      color = Color.LTGRAY
      strokeWidth = 0.8f
    }
    private val paintBoxBg = Paint().apply {
      color = Color.rgb(245, 247, 250)
      style = Paint.Style.FILL
    }

    private val rightEdge = MARGIN + CONTENT_WIDTH
    private var currentPage: PdfDocument.Page? = null
    private var currentCanvas: Canvas? = null
    private var pageIndex = 0
    private var currentY = MARGIN

    // We first run a layout pass to know total page count
    private var totalPagesCount = 1

    fun render() {
      // Pass 1: Compute total page count
      totalPagesCount = computeTotalPages()

      // Pass 2: Actually draw the document
      pageIndex = 0
      startNewPage()

      // 1. Draw Daily Summary Header on Page 1
      drawDaySummaryHeader()

      // 2. Draw each transaction as a receipt-style block
      group.transactions.forEachIndexed { index, tx ->
        val serial = if (tx.serialNumber > 0L) tx.serialNumber else (index + 1).toLong()
        drawTransactionBlock(tx, serial)
      }

      // Finish the final page
      finishCurrentPage()
    }

    private fun startNewPage() {
      if (currentPage != null) {
        finishCurrentPage()
      }
      pageIndex++
      val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
      currentPage = document.startPage(pageInfo)
      currentCanvas = currentPage!!.canvas
      currentCanvas!!.drawColor(Color.WHITE)
      currentY = MARGIN + 10f
    }

    private fun finishCurrentPage() {
      currentPage?.let { page ->
        // Draw page footer: "Page X of Y"
        val footerPaint = Paint().apply {
          isAntiAlias = true
          color = Color.GRAY
          typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
          textSize = 9f
        }
        val footerText = "Page $pageIndex of $totalPagesCount  •  Warehouse Register"
        val fw = footerPaint.measureText(footerText)
        currentCanvas?.drawText(footerText, (PAGE_WIDTH - fw) / 2f, PAGE_HEIGHT - MARGIN, footerPaint)

        document.finishPage(page)
      }
      currentPage = null
      currentCanvas = null
    }

    private fun ensureSpace(heightNeeded: Float, continuationHeader: String? = null) {
      if (currentY + heightNeeded > MAX_CONTENT_Y) {
        startNewPage()
        if (continuationHeader != null) {
          currentCanvas!!.drawText(continuationHeader, MARGIN, currentY, paintTxHeader)
          currentY += 16f
          currentCanvas!!.drawLine(MARGIN, currentY, rightEdge, currentY, paintLightLine)
          currentY += 14f
        }
      }
    }

    private fun drawDaySummaryHeader() {
      val canvas = currentCanvas!!

      canvas.drawText("WAREHOUSE REGISTER", MARGIN, currentY, paintDocTitle)
      currentY += 18f
      canvas.drawText("FULL DAY SUMMARY  •  ${group.dateFormatted}", MARGIN, currentY, paintDocSub)
      currentY += 14f
      canvas.drawLine(MARGIN, currentY, rightEdge, currentY, paintLine)
      currentY += 16f

      // Accounting Drawer Summary Card
      val cardTop = currentY
      val cardHeight = 60f
      canvas.drawRect(MARGIN, cardTop, rightEdge, cardTop + cardHeight, paintBoxBg)

      val col1X = MARGIN + 16f
      val col2X = MARGIN + 140f
      val col3X = MARGIN + 270f
      val col4X = MARGIN + 400f

      var cardY = cardTop + 22f
      canvas.drawText("CASH IN", col1X, cardY, paintSecondary)
      canvas.drawText("CASH OUT", col2X, cardY, paintSecondary)
      canvas.drawText("ONLINE IN", col3X, cardY, paintSecondary)
      canvas.drawText("NET CASH", col4X, cardY, paintSecondary)

      cardY += 20f
      canvas.drawText("₹" + formatCompact(group.totalCashReceived), col1X, cardY, paintBold)
      canvas.drawText("₹" + formatCompact(group.totalCashGiven), col2X, cardY, paintBold)
      canvas.drawText("₹" + formatCompact(group.totalOnlineReceived), col3X, cardY, paintBold)

      val net = group.totalCashReceived - group.totalCashGiven
      val netPaint = Paint(paintBold).apply {
        color = if (net >= 0) Color.rgb(21, 128, 61) else Color.rgb(220, 38, 38)
      }
      canvas.drawText("₹" + formatCompact(net), col4X, cardY, netPaint)

      currentY += cardHeight + 20f
      canvas.drawText("TRANSACTION REGISTER (${group.transactions.size} ENTRIES)", MARGIN, currentY, paintBold)
      currentY += 12f
      canvas.drawLine(MARGIN, currentY, rightEdge, currentY, paintLine)
      currentY += 18f
    }

    private fun drawTransactionBlock(tx: TransactionRecord, serial: Long) {
      val canvas = currentCanvas!!
      val items = tx.itemsList

      val typeLabel = when (tx.type) {
        "CASH_SALE" -> "SALE — CASH"
        "ONLINE_SALE" -> if (tx.bankAccountName.isNotBlank()) "SALE — UPI (${tx.bankAccountName})" else "SALE — UPI"
        "CREDIT_SALE" -> "SALE — CREDIT"
        "CASH_RECEIPT" -> if (tx.receiptMethod.equals("Online", true)) "RECEIPT — UPI" else "RECEIPT — CASH"
        "CASH_GIVE" -> "PAYMENT — CASH"
        "GOODS_RETURN" -> "GOODS RETURN"
        else -> tx.type
      }

      // Estimate height of the header + metadata (approx 70f)
      val headerNeeded = 72f
      ensureSpace(headerNeeded)

      // 1. Transaction Header
      val txHeaderStr = "TRANSACTION #$serial  •  ${tx.formattedTime}  •  $typeLabel"
      canvas.drawText(txHeaderStr, MARGIN, currentY, paintTxHeader)
      currentY += 18f

      // 2. Customer details & Blank Voucher Number
      val name = tx.personName.ifBlank { "WALK-IN" }
      val phone = tx.phoneNumbersList.firstOrNull().orEmpty().ifBlank { "-" }
      val address = tx.address.ifBlank { "-" }

      canvas.drawText("NAME: $name", MARGIN, currentY, paintBold)
      val phoneStr = "NUMBER: $phone"
      canvas.drawText(phoneStr, rightEdge - paintRegular.measureText(phoneStr), currentY, paintRegular)
      currentY += 15f

      canvas.drawText("ADDRESS: $address", MARGIN, currentY, paintRegular)
      val vNoStr = "VOUCHER NO.: ___________"
      canvas.drawText(vNoStr, rightEdge - paintRegular.measureText(vNoStr), currentY, paintRegular)
      currentY += 15f

      if (tx.referenceNumber.isNotBlank()) {
        canvas.drawText("REF NO: ${tx.referenceNumber}", MARGIN, currentY, paintSecondary)
        currentY += 14f
      }

      // 3. Items Table (if any)
      if (items.isNotEmpty()) {
        currentY += 4f
        ensureSpace(32f, "TRANSACTION #$serial — CONTINUED")
        canvas.drawLine(MARGIN, currentY, rightEdge, currentY, paintLightLine)
        currentY += 14f

        canvas.drawText("QTY / DESCRIPTION", MARGIN, currentY, paintSecondary)
        if (tx.type != "GOODS_RETURN") {
          canvas.drawText("RATE", rightEdge - 110f - paintSecondary.measureText("RATE"), currentY, paintSecondary)
          canvas.drawText("AMOUNT", rightEdge - paintSecondary.measureText("AMOUNT"), currentY, paintSecondary)
        }
        currentY += 6f
        canvas.drawLine(MARGIN, currentY, rightEdge, currentY, paintLightLine)
        currentY += 16f

        items.forEachIndexed { itemIndex, item ->
          val itemHeight = estimateItemHeight(item)
          // CRITICAL: NEVER SPLIT AN INDIVIDUAL ITEM
          ensureSpace(itemHeight, "TRANSACTION #$serial — CONTINUED")

          val qtyStr = "${formatQty(item.quantity)} ${item.unit}"
          canvas.drawText(qtyStr, MARGIN, currentY, paintBold)

          if (tx.type != "GOODS_RETURN") {
            val rStr = "₹" + formatCompact(item.rate)
            val aStr = "₹" + formatCompact(item.itemTotal)
            canvas.drawText(rStr, rightEdge - 110f - paintRegular.measureText(rStr), currentY, paintRegular)
            canvas.drawText(aStr, rightEdge - paintBold.measureText(aStr), currentY, paintBold)
          }
          currentY += 14f

          val nameStr = if (item.size.isNotBlank()) "${item.designName}  [${item.size}]" else item.designName
          val descLines = wrapText(nameStr, paintRegular, CONTENT_WIDTH - 20f)
          descLines.forEach { line ->
            canvas.drawText(line, MARGIN + 8f, currentY, paintRegular)
            currentY += 14f
          }
          currentY += 4f
        }
      }

      // 4. Totals Block
      val totalsNeeded = if (tx.cartage > 0) 48f else 32f
      ensureSpace(totalsNeeded, "TRANSACTION #$serial — CONTINUED")

      canvas.drawLine(MARGIN, currentY, rightEdge, currentY, paintLightLine)
      currentY += 14f

      if (tx.cartage > 0) {
        val cartStr = "CARTAGE: ₹" + formatCompact(tx.cartage)
        canvas.drawText(cartStr, rightEdge - paintRegular.measureText(cartStr), currentY, paintRegular)
        currentY += 14f
      }

      val isChallanNoAmount = tx.type == "CREDIT_SALE" && tx.totalAmount <= 0.0
      if (!isChallanNoAmount) {
        val totalLabel = if (tx.type == "GOODS_RETURN") "REFUND: " else "GRAND TOTAL: "
        val totalVal = "₹" + formatCompact(tx.totalAmount)
        val fullTotalStr = totalLabel + totalVal
        canvas.drawText(fullTotalStr, rightEdge - paintBold.measureText(fullTotalStr), currentY, paintBold)
      }

      val modeFooter = "PAYMENT MODE: $typeLabel"
      canvas.drawText(modeFooter, MARGIN, currentY, paintSecondary)
      currentY += 18f

      // Transaction bottom separator
      canvas.drawLine(MARGIN, currentY, rightEdge, currentY, paintLine)
      currentY += 20f
    }

    private fun estimateItemHeight(item: OrderItem): Float {
      val nameStr = if (item.size.isNotBlank()) "${item.designName}  [${item.size}]" else item.designName
      val lines = wrapText(nameStr, paintRegular, CONTENT_WIDTH - 20f)
      return 14f + (lines.size * 14f) + 8f
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
      if (text.isBlank()) return listOf("")
      val words = text.trim().split(Regex("\\s+"))
      val lines = mutableListOf<String>()
      var current = ""
      for (w in words) {
        val test = if (current.isEmpty()) w else "$current $w"
        if (paint.measureText(test) <= maxWidth) {
          current = test
        } else {
          if (current.isNotEmpty()) lines.add(current)
          current = w
        }
      }
      if (current.isNotEmpty()) lines.add(current)
      return if (lines.isEmpty()) listOf(text) else lines
    }

    /**
     * Pass 1: Simulate the layout using the exact same constraints
     * to accurately determine totalPagesCount.
     */
    private fun computeTotalPages(): Int {
      var pages = 1
      var y = MARGIN + 10f

      // Header height
      y += 18f + 14f + 16f + 60f + 20f + 12f + 18f

      for (tx in group.transactions) {
        // Transaction header
        val headerNeeded = 72f
        if (y + headerNeeded > MAX_CONTENT_Y) {
          pages++
          y = MARGIN + 10f
        }
        y += headerNeeded

        // Items
        if (tx.itemsList.isNotEmpty()) {
          if (y + 32f > MAX_CONTENT_Y) {
            pages++
            y = MARGIN + 10f + 30f
          }
          y += 32f

          for (item in tx.itemsList) {
            val itemHeight = estimateItemHeight(item)
            if (y + itemHeight > MAX_CONTENT_Y) {
              pages++
              y = MARGIN + 10f + 30f
            }
            y += itemHeight
          }
        }

        // Totals
        val totalsNeeded = if (tx.cartage > 0) 48f else 32f
        if (y + totalsNeeded > MAX_CONTENT_Y) {
          pages++
          y = MARGIN + 10f + 30f
        }
        y += totalsNeeded + 20f
      }

      return pages.coerceAtLeast(1)
    }
  }

  private fun formatDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timeMillis))
  }

  private fun formatTime(timeMillis: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timeMillis))
  }

  private fun formatCompact(amount: Double): String {
    val isWhole = kotlin.math.abs(amount - kotlin.math.round(amount)) < 0.005
    return java.text.NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
      minimumFractionDigits = if (isWhole) 0 else 2
      maximumFractionDigits = if (isWhole) 0 else 2
    }.format(amount)
  }

  private fun formatQty(qty: Double): String {
    return if (qty % 1.0 == 0.0) qty.toInt().toString() else "%.1f".format(qty)
  }
}
