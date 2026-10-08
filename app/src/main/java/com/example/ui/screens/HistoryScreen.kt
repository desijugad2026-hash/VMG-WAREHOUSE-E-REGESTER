package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionRecord
import com.example.printing.EscPosReceiptBuilder
import com.example.printing.PdfReceiptHelper
import com.example.printing.PrintJob
import com.example.printing.PrintPreviewDialog
import com.example.printing.PrinterSelectionDialog
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.WarehouseTopBar
import com.example.ui.theme.MintGreenBg
import com.example.ui.theme.MintGreenIcon
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoftBlueBg
import com.example.ui.theme.SoftBlueIcon
import com.example.ui.theme.SoftPurpleBg
import com.example.ui.theme.SoftPurpleIcon
import com.example.ui.theme.SoftRoseBg
import com.example.ui.theme.SoftRoseIcon
import com.example.ui.theme.SoftTealBg
import com.example.ui.theme.SoftTealIcon
import com.example.ui.viewmodel.DayHistoryGroup
import com.example.ui.viewmodel.WarehouseViewModel

@Composable
fun HistoryScreen(
  viewModel: WarehouseViewModel,
  onBack: () -> Unit
) {
  val dayGroups by viewModel.dayHistoryGroups.collectAsStateWithLifecycle()

  // Track expanded state for each dateKey (most recent day expanded by default)
  val expandedDateKeys = remember { mutableStateMapOf<String, Boolean>() }
  var searchQuery by remember { mutableStateOf("") }
  var selectedTypeFilter by remember { mutableStateOf("ALL") }

  val filteredDayGroups = remember(dayGroups, searchQuery, selectedTypeFilter) {
    val q = searchQuery.trim()
    if (q.isEmpty() && selectedTypeFilter == "ALL") {
      dayGroups
    } else {
      dayGroups.mapNotNull { group ->
        val matchingTx = group.transactions.filter { tx ->
          val typeMatch = when (selectedTypeFilter) {
            "SALES" -> tx.type == "CASH_SALE" || tx.type == "ONLINE_SALE"
            "DEBTORS" -> tx.type == "CREDIT_SALE"
            "RECEIPTS" -> tx.type == "CASH_RECEIPT"
            "PAYMENTS" -> tx.type == "CASH_GIVE"
            "RETURNS" -> tx.type == "GOODS_RETURN"
            else -> true
          }
          val serialStr = (if (tx.serialNumber > 0L) tx.serialNumber else tx.id).toString()
          val queryMatch = q.isEmpty() ||
            tx.personName.contains(q, ignoreCase = true) ||
            tx.phoneNumbersList.any { it.contains(q, ignoreCase = true) } ||
            tx.referenceNumber.contains(q, ignoreCase = true) ||
            tx.shortItemsDescription.contains(q, ignoreCase = true) ||
            serialStr.contains(q, ignoreCase = true) ||
            group.dateFormatted.contains(q, ignoreCase = true)
          typeMatch && queryMatch
        }
        if (matchingTx.isEmpty()) null else group.copy(transactions = matchingTx)
      }
    }
  }

  // State for transaction detail dialog
  var selectedTransactionWithSerial by remember { mutableStateOf<Pair<TransactionRecord, Long>?>(null) }

  // State for day deletion confirmation dialog
  var dayToDelete by remember { mutableStateOf<DayHistoryGroup?>(null) }

  // Printing & PDF states
  val context = LocalContext.current
  var activePrintPreviewJob by remember { mutableStateOf<PrintJob?>(null) }
  var activePdfAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var activeEscPosData by remember { mutableStateOf<ByteArray?>(null) }
  var showPrinterDialog by remember { mutableStateOf(false) }
  var activeCopies by remember { mutableStateOf(1) }

  // Confirmation dialog for day deletion
  dayToDelete?.let { group ->
    ConfirmDialog(
      title = "Delete Day's Transactions?",
      message = "Are you sure you want to delete all ${group.transactions.size} transactions for ${group.dateFormatted}? This will remove all entries for this day.",
      confirmButtonText = "Delete Full Day",
      onConfirm = {
        viewModel.deleteDayTransactions(group.dateKey)
        dayToDelete = null
      },
      onDismiss = { dayToDelete = null }
    )
  }

  // Transaction Detail Dialog
  selectedTransactionWithSerial?.let { (tx, serial) ->
    TransactionDetailDialog(
      transaction = tx,
      serialNumber = serial,
      onDismiss = { selectedTransactionWithSerial = null },
      onDeleteTransaction = { txId ->
        viewModel.deleteTransaction(txId)
      },
      onPrintTransaction = {
        val previewText = EscPosReceiptBuilder.generatePlainText(tx, serial)
        val escPos = EscPosReceiptBuilder.generateEscPos(tx, serial, 1)
        activePrintPreviewJob = PrintJob(
          title = "Print Receipt #$serial",
          plainTextReceipt = previewText,
          escPosData = escPos
        )
        activePdfAction = {
          PdfReceiptHelper.openTransactionPdf(context, tx, serial)
        }
        selectedTransactionWithSerial = null
      }
    )
  }

  // Print Preview Dialog
  activePrintPreviewJob?.let { job ->
    PrintPreviewDialog(
      title = job.title,
      previewText = job.plainTextReceipt,
      onDismiss = {
        activePrintPreviewJob = null
        activePdfAction = null
      },
      onViewPdf = {
        activePdfAction?.invoke()
      },
      onProceedToPrint = { copies ->
        activeCopies = copies
        activeEscPosData = job.escPosData
        showPrinterDialog = true
      }
    )
  }

  // Printer Selection Dialog
  if (showPrinterDialog && activeEscPosData != null) {
    PrinterSelectionDialog(
      escPosData = activeEscPosData!!,
      copies = activeCopies,
      onDismiss = {
        showPrinterDialog = false
        activePrintPreviewJob = null
        activePdfAction = null
      },
      onPrintSuccess = {
        showPrinterDialog = false
        activePrintPreviewJob = null
        activePdfAction = null
      }
    )
  }

  Scaffold(
    topBar = {
      WarehouseTopBar(
        title = "Transaction History",
        subtitle = "Daily register logs & receipts",
        onBackClick = onBack
      )
    },
    containerColor = Color(0xFFF8FAFC)
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentAlignment = Alignment.TopCenter
    ) {
      if (dayGroups.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Slate600, modifier = Modifier.size(28.dp))
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text("No Transactions Recorded", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate900)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            "Vouchers saved in Sales, Receipt, Payment, or Goods Return will appear here grouped by date.",
            fontSize = 12.sp,
            color = Slate600,
            lineHeight = 17.sp
          )
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          // Search Bar
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search party, voucher #, phone, item or ref...", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate500, modifier = Modifier.size(16.dp))
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Type Filter Chips
          val filterOptions = listOf(
            "ALL" to "All",
            "SALES" to "Cash/Online Sales",
            "DEBTORS" to "Debtors (Credit)",
            "RECEIPTS" to "Receipts",
            "PAYMENTS" to "Payments",
            "RETURNS" to "Returns"
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            filterOptions.forEach { (key, label) ->
              val isSelected = selectedTypeFilter == key
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .clickable { selectedTypeFilter = key },
                color = if (isSelected) PrimaryBlue else Color.White,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else Color(0xFFCBD5E1))
              ) {
                Text(
                  text = label,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) Color.White else Slate700,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (filteredDayGroups.isEmpty()) {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
              shape = RoundedCornerShape(8.dp),
              color = Color.White,
              border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text("No matching transactions found", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate900)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Try clearing the search or selecting 'All' filters.", fontSize = 12.sp, color = Slate500)
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              itemsIndexed(filteredDayGroups, key = { _, g -> g.dateKey }) { index, group ->
                val isFiltering = searchQuery.isNotBlank() || selectedTypeFilter != "ALL"
                val isExpanded = if (isFiltering) {
                  expandedDateKeys[group.dateKey] ?: true
                } else {
                  expandedDateKeys[group.dateKey] ?: (index == 0)
                }

                DayTransactionCard(
                  group = group,
                  isExpanded = isExpanded,
                  onToggleExpand = {
                    expandedDateKeys[group.dateKey] = !isExpanded
                  },
                  onDeleteDay = {
                    dayToDelete = group
                  },
                  onPrintDay = {
                    val previewText = EscPosReceiptBuilder.generateDaySummaryText(group)
                    val escPos = EscPosReceiptBuilder.generateDaySummaryEscPos(group, 1)
                    activePrintPreviewJob = PrintJob(
                      title = "Print Day: ${group.dateFormatted}",
                      plainTextReceipt = previewText,
                      escPosData = escPos
                    )
                    activePdfAction = {
                      PdfReceiptHelper.openDaySummaryPdf(context, group)
                    }
                  },
                  onViewDayPdf = {
                    PdfReceiptHelper.openDaySummaryPdf(context, group)
                  },
                  onSelectTransaction = { tx, serial ->
                    selectedTransactionWithSerial = Pair(tx, serial)
                  },
                  onPrintTransactionDirect = { tx, serial ->
                    val previewText = EscPosReceiptBuilder.generatePlainText(tx, serial)
                    val escPos = EscPosReceiptBuilder.generateEscPos(tx, serial, 1)
                    activePrintPreviewJob = PrintJob(
                      title = "Print Receipt #$serial",
                      plainTextReceipt = previewText,
                      escPosData = escPos
                    )
                    activePdfAction = {
                      PdfReceiptHelper.openTransactionPdf(context, tx, serial)
                    }
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DayTransactionCard(
  group: DayHistoryGroup,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onDeleteDay: () -> Unit,
  onPrintDay: () -> Unit,
  onViewDayPdf: () -> Unit,
  onSelectTransaction: (TransactionRecord, Long) -> Unit,
  onPrintTransactionDirect: (TransactionRecord, Long) -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = Color.White,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Date Header Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF1F5F9))
          .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Date info (clickable to expand/collapse)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .weight(1f)
            .clickable { onToggleExpand() }
        ) {
          Icon(
            Icons.Default.CalendarToday,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = group.dateFormatted,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Slate900,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            color = Color.White,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
          ) {
            Text(
              text = "${group.transactions.size}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Slate700,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
          }
        }

        // Action Buttons: PDF, Print, Delete, Toggle
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          IconButton(
            onClick = onViewDayPdf,
            modifier = Modifier.size(30.dp)
          ) {
            Icon(
              Icons.Default.PictureAsPdf,
              contentDescription = "PDF Report",
              tint = Color(0xFFDC2626),
              modifier = Modifier.size(17.dp)
            )
          }

          IconButton(
            onClick = onPrintDay,
            modifier = Modifier.size(30.dp)
          ) {
            Icon(
              Icons.Default.Print,
              contentDescription = "Print Day Summary",
              tint = PrimaryBlue,
              modifier = Modifier.size(17.dp)
            )
          }

          IconButton(
            onClick = onDeleteDay,
            modifier = Modifier.size(30.dp)
          ) {
            Icon(
              Icons.Default.Delete,
              contentDescription = "Delete Day",
              tint = Color(0xFF64748B),
              modifier = Modifier.size(17.dp)
            )
          }

          Surface(
            modifier = Modifier
              .size(28.dp)
              .clip(RoundedCornerShape(6.dp))
              .clickable { onToggleExpand() },
            color = Color.White,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Slate700,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      // Expanded Content
      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(),
        exit = shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Compact Day Summary Bar
          Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              SummaryStatPill(
                label = "CASH IN",
                amount = group.totalCashReceived,
                color = Color(0xFF15803D)
              )

              SummaryStatPill(
                label = "CASH OUT",
                amount = group.totalCashGiven,
                color = Color(0xFFB91C1C)
              )

              if (group.totalOnlineReceived > 0) {
                SummaryStatPill(
                  label = "ONLINE IN",
                  amount = group.totalOnlineReceived,
                  color = PrimaryBlue
                )
              }

              SummaryStatPill(
                label = "NET CASH",
                amount = group.netCash,
                color = if (group.netCash >= 0) Slate900 else Color(0xFFDC2626)
              )
            }
          }

          if (group.transactions.isEmpty()) {
            Text(
              text = "No transactions on this date",
              fontSize = 12.sp,
              color = Slate500,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          } else {
            Surface(
              color = Color.White,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.fillMaxWidth()) {
                group.transactions.forEachIndexed { idx, tx ->
                  val serial = if (tx.serialNumber > 0L) tx.serialNumber else tx.id.coerceAtLeast(1L)
                  TransactionItemRow(
                    transaction = tx,
                    serialNumber = serial,
                    onClick = { onSelectTransaction(tx, serial) },
                    onPrint = { onPrintTransactionDirect(tx, serial) }
                  )
                  if (idx < group.transactions.size - 1) {
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SummaryStatPill(
  label: String,
  amount: Double,
  color: Color
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = label,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = Slate500,
      letterSpacing = 0.3.sp,
      maxLines = 1,
      softWrap = false
    )
    Spacer(modifier = Modifier.height(1.dp))
    val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply {
      minimumFractionDigits = 0
      maximumFractionDigits = 2
    }.format(amount)
    Text(
      text = "₹$formatted",
      fontSize = 12.sp,
      fontWeight = FontWeight.ExtraBold,
      color = color,
      maxLines = 1,
      softWrap = false
    )
  }
}

@Composable
fun TransactionItemRow(
  transaction: TransactionRecord,
  serialNumber: Long,
  onClick: () -> Unit,
  onPrint: () -> Unit
) {
  val (badgeBg, badgeColor, typeLabel, _) = when (transaction.type) {
    "CASH_RECEIPT" -> Quad(
      MintGreenBg,
      MintGreenIcon,
      if (transaction.receiptMethod.equals("Online", ignoreCase = true)) "Receipt (Online)" else "Receipt (Cash)",
      if (transaction.receiptMethod.equals("Online", ignoreCase = true)) Icons.Default.AccountBalance else Icons.Default.ArrowDownward
    )
    "CASH_GIVE" -> Quad(SoftRoseBg, SoftRoseIcon, "Payment", Icons.Default.ArrowUpward)
    "CASH_SALE" -> Quad(SoftBlueBg, SoftBlueIcon, "Cash Sale", Icons.Default.ReceiptLong)
    "ONLINE_SALE" -> Quad(Color(0xFFEDE9FE), Color(0xFF4F46E5), "Online Sale", Icons.Default.CreditCard)
    "CREDIT_SALE" -> Quad(Color(0xFFF3E8FF), Color(0xFF7C3AED), "Debtors", Icons.Default.ReceiptLong)
    "GOODS_RETURN" -> Quad(SoftTealBg, SoftTealIcon, "G.R Return", Icons.Default.Replay)
    else -> Quad(Slate100, Slate700, transaction.type, Icons.Default.ReceiptLong)
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Serial Number Badge
    Surface(
      color = Color(0xFFF1F5F9),
      shape = RoundedCornerShape(4.dp),
      border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
      Text(
        text = "#$serialNumber",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Slate700,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Main Details
    Column(modifier = Modifier.weight(1f)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Surface(
          color = badgeBg,
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = typeLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
          )
        }

        Text(
          text = transaction.formattedTime,
          fontSize = 11.sp,
          color = Slate500,
          maxLines = 1,
          softWrap = false
        )

        if (transaction.referenceNumber.isNotBlank()) {
          Text(
            text = "• Ref: ${transaction.referenceNumber}",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate600,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      // Party Name
      Text(
        text = transaction.personName.ifBlank { "Unnamed Party" },
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = Slate900,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      // Details for Sales & Goods Return
      val desc = transaction.shortItemsDescription
      if (desc.isNotBlank()) {
        Text(
          text = desc,
          fontSize = 11.sp,
          color = Slate600,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Spacer(modifier = Modifier.width(6.dp))

    // Amount Display (except for credit sale where rate is not applicable)
    if (transaction.type != "CREDIT_SALE") {
      val formattedAmount = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
      }.format(transaction.totalAmount)
      Text(
        text = "₹$formattedAmount",
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        softWrap = false,
        color = when (transaction.type) {
          "CASH_RECEIPT", "CASH_SALE" -> Color(0xFF15803D)
          "ONLINE_SALE" -> Color(0xFF4F46E5)
          "CASH_GIVE", "GOODS_RETURN" -> Color(0xFFB91C1C)
          else -> PrimaryBlue
        }
      )
      Spacer(modifier = Modifier.width(4.dp))
    }

    // Quick Print Icon Button
    IconButton(
      onClick = onPrint,
      modifier = Modifier.size(30.dp)
    ) {
      Icon(
        Icons.Default.Print,
        contentDescription = "Print Receipt",
        tint = PrimaryBlue,
        modifier = Modifier.size(17.dp)
      )
    }

    Icon(
      Icons.AutoMirrored.Filled.ArrowForwardIos,
      contentDescription = "Details",
      tint = Slate400,
      modifier = Modifier.size(12.dp)
    )
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
