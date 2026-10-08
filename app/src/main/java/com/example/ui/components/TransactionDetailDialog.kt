package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TransactionRecord
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun TransactionDetailDialog(
  transaction: TransactionRecord,
  serialNumber: Long,
  onDismiss: () -> Unit,
  onDeleteTransaction: (Long) -> Unit,
  onPrintTransaction: (() -> Unit)? = null
) {
  val actualSerial = if (transaction.serialNumber > 0L) transaction.serialNumber else if (serialNumber > 0L) serialNumber else transaction.id.coerceAtLeast(1L)
  var showConfirmDelete by remember { mutableStateOf(false) }

  if (showConfirmDelete) {
    ConfirmDialog(
      title = "Delete Transaction?",
      message = "Are you sure you want to delete this transaction (#$actualSerial)? This action cannot be undone.",
      onConfirm = {
        showConfirmDelete = false
        onDeleteTransaction(transaction.id)
        onDismiss()
      },
      onDismiss = { showConfirmDelete = false }
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .widthIn(max = 560.dp)
        .fillMaxHeight(0.88f),
      shape = RoundedCornerShape(20.dp),
      color = Color.White,
      shadowElevation = 8.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Surface(
                color = PrimaryBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "Sr #$actualSerial",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = PrimaryBlue,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
              Text(
                text = when (transaction.type) {
                  "CASH_RECEIPT" -> "Receipt (${transaction.receiptMethod})"
                  "CASH_GIVE" -> "Payment"
                  "CASH_SALE" -> "Cash Sale"
                  "ONLINE_SALE" -> "Online Sale"
                  "CREDIT_SALE" -> "Debtors (Credit Sale)"
                  "GOODS_RETURN" -> "Goods Return (G.R)"
                  else -> transaction.type
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = "${transaction.dateFormatted} • ${transaction.formattedTime}",
              fontSize = 12.sp,
              color = Slate500,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(top = 2.dp)
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate100)

        // Scrollable Body Content
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Party / Customer Name
          DetailRow(
            label = when (transaction.type) {
              "CREDIT_SALE" -> "Debtor / Shop Name:"
              "CASH_GIVE" -> "Paid To (Name):"
              "CASH_RECEIPT" -> "Received From:"
              else -> "Customer Name:"
            },
            value = transaction.personName.ifBlank { "—" },
            isBold = true
          )

          // Payment Mode
          val paymentMode = when (transaction.type) {
            "CASH_SALE" -> "Cash Sale"
            "ONLINE_SALE" -> if (transaction.bankAccountName.isNotBlank()) "Online (${transaction.bankAccountName})" else "Online Sale"
            "CASH_RECEIPT" -> if (transaction.receiptMethod.equals("Online", ignoreCase = true)) "Online (${transaction.bankAccountName})" else "Cash"
            "CASH_GIVE" -> "Cash Out"
            "CREDIT_SALE" -> "Credit (Udhar)"
            "GOODS_RETURN" -> "Return Refund"
            else -> transaction.type
          }
          DetailRow(
            label = "Payment Mode:",
            value = paymentMode
          )

          // Reference Number if present
          if (transaction.referenceNumber.isNotBlank()) {
            DetailRow(
              label = "Reference No:",
              value = transaction.referenceNumber,
              isBold = true
            )
          }

          // Mobile Numbers
          val phones = transaction.phoneNumbersList
          if (phones.isNotEmpty()) {
            DetailRow(
              label = if (phones.size > 1) "Mobile Numbers:" else "Mobile Number:",
              value = phones.joinToString(", ")
            )
          }

          // Address
          if (transaction.address.isNotBlank()) {
            DetailRow(
              label = "Address:",
              value = transaction.address
            )
          }

          // Items table (Orders & Goods Return)
          val items = transaction.itemsList
          if (items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (transaction.type == "GOODS_RETURN") "RETURNED ITEMS (${items.size})" else "ITEM DETAILS (${items.size})",
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              color = Slate700,
              letterSpacing = 0.4.sp
            )

            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
              color = Color.White,
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
              Column(modifier = Modifier.fillMaxWidth()) {
                // Table Header
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("ITEM / SIZE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Slate600)
                  Text(
                    if (transaction.type == "CREDIT_SALE") "QTY" else "QTY / RATE / TOTAL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Slate600
                  )
                }

                HorizontalDivider(color = Color(0xFFCBD5E1))

                // Table Rows (2-line structured layout — zero horizontal scroll required)
                items.forEachIndexed { index, item ->
                  val rowBg = if (index % 2 == 0) Color.White else Slate50
                  val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(rowBg)
                      .padding(vertical = 7.dp, horizontal = 10.dp)
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(
                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "${index + 1}.",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = Slate500,
                          modifier = Modifier.width(18.dp)
                        )
                        if (item.size.isNotBlank()) {
                          Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                          ) {
                            Text(
                              text = item.size,
                              fontSize = 10.sp,
                              fontWeight = FontWeight.Bold,
                              color = PrimaryBlue,
                              maxLines = 1,
                              softWrap = false,
                              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                          }
                          Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                          text = item.designName,
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          color = Slate900,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }

                      if (transaction.type == "CREDIT_SALE") {
                        Text(
                          text = "$qtyStr ${item.unit}",
                          fontSize = 13.sp,
                          fontWeight = FontWeight.ExtraBold,
                          color = PrimaryBlue,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }

                    if (transaction.type != "CREDIT_SALE") {
                      Spacer(modifier = Modifier.height(2.dp))
                      val formattedRate = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply {
                        minimumFractionDigits = 0
                        maximumFractionDigits = 2
                      }.format(item.rate)
                      val formattedTotal = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply {
                        minimumFractionDigits = 0
                        maximumFractionDigits = 2
                      }.format(item.itemTotal)
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "Qty: $qtyStr ${item.unit}  ×  ₹$formattedRate/${item.unit}",
                          fontSize = 11.sp,
                          color = Slate600,
                          maxLines = 1,
                          softWrap = false
                        )
                        Text(
                          text = "₹$formattedTotal",
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          color = Slate900,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }
                  }
                  if (index < items.size - 1) {
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                  }
                }
              }
            }
          }

          // Cartage (if applicable)
          if (transaction.cartage > 0) {
            val formattedCartage = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply {
              minimumFractionDigits = 2
              maximumFractionDigits = 2
            }.format(transaction.cartage)
            DetailRow(
              label = "Cartage / Transport:",
              value = "₹$formattedCartage"
            )
          }

          // Total Amount / Refund
          if (transaction.type != "CREDIT_SALE") {
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = when (transaction.type) {
                    "GOODS_RETURN" -> "Refund Given:"
                    "CASH_GIVE" -> "Payment Amount:"
                    "CASH_RECEIPT" -> "Received Amount:"
                    else -> "Grand Total Amount:"
                  },
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Slate800
                )
                val formattedTotalAmount = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply {
                  minimumFractionDigits = 2
                  maximumFractionDigits = 2
                }.format(transaction.totalAmount)
                Text(
                  text = "₹$formattedTotalAmount",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = PrimaryBlue
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons: Delete, Print, Close
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = { showConfirmDelete = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", fontSize = 13.sp, maxLines = 1, softWrap = false)
          }

          onPrintTransaction?.let { printAction ->
            Button(
              onClick = printAction,
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
              modifier = Modifier.weight(1.2f)
            ) {
              Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Print", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
          }

          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            modifier = Modifier.weight(0.9f)
          ) {
            Text("Close", fontSize = 13.sp, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

@Composable
private fun DetailRow(
  label: String,
  value: String,
  isBold: Boolean = false
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = label,
      fontSize = 13.sp,
      color = Slate600,
      modifier = Modifier.weight(0.45f)
    )
    Text(
      text = value,
      fontSize = 14.sp,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
      color = Slate900,
      modifier = Modifier.weight(0.55f)
    )
  }
}
