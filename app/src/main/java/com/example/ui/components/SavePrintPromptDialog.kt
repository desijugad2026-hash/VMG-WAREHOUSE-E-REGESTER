package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TransactionRecord
import com.example.ui.theme.MintGreenIcon
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun SavePrintPromptDialog(
  transaction: TransactionRecord,
  serialNumber: Long,
  onCancel: () -> Unit,
  onPrint: (copies: Int) -> Unit
) {
  var copies by remember { mutableIntStateOf(1) }
  val typeLabel = when (transaction.type) {
    "CASH_SALE" -> "Cash Sale"
    "ONLINE_SALE" -> "Online Sale"
    "CREDIT_SALE" -> "Debtors (Credit)"
    "CASH_RECEIPT" -> if (transaction.receiptMethod.equals("Online", ignoreCase = true)) "Receipt (Online)" else "Receipt (Cash)"
    "CASH_GIVE" -> "Payment"
    "GOODS_RETURN" -> "Goods Return"
    else -> transaction.type
  }

  val formattedAmount = java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
  }.format(transaction.totalAmount)

  Dialog(
    onDismissRequest = onCancel,
    properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .widthIn(max = 440.dp),
      shape = RoundedCornerShape(20.dp),
      color = Color.White,
      shadowElevation = 10.dp
    ) {
      Column(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Success Header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = MintGreenIcon,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Transaction Saved",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900
          )
        }

        // Details Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = Slate100),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                color = Slate900,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "Serial #$serialNumber",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
              Text(
                text = typeLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue
              )
            }

            HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 4.dp))

            if (transaction.personName.isNotBlank()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Party / Name:", fontSize = 12.sp, color = Slate600)
                Text(transaction.personName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
              }
            }

            if (transaction.referenceNumber.isNotBlank()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Reference No:", fontSize = 12.sp, color = Slate600)
                Text(transaction.referenceNumber, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
              }
            }

            if (transaction.type != "CREDIT_SALE") {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(if (transaction.type == "GOODS_RETURN") "Refund Given:" else "Total Amount:", fontSize = 12.sp, color = Slate600)
                Text("₹$formattedAmount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
              }
            }
          }
        }

        // Copies Selector right inside Save & Print dialog!
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Slate100, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = "Print Copies",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900,
              maxLines = 1,
              softWrap = false
            )
            Text(
              text = "Select number of copies to print",
              fontSize = 11.sp,
              color = Slate600,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .testTag("btn_save_copies_minus"),
              color = Color.White,
              border = BorderStroke(1.dp, Slate200),
              onClick = { if (copies > 1) copies-- }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease Copies", tint = Slate700, modifier = Modifier.size(16.dp))
              }
            }

            Text(
              text = "$copies",
              fontSize = 16.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Slate900,
              modifier = Modifier.width(26.dp),
              textAlign = TextAlign.Center
            )

            Surface(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .testTag("btn_save_copies_plus"),
              color = Color.White,
              border = BorderStroke(1.dp, Slate200),
              onClick = { if (copies < 10) copies++ }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "Increase Copies", tint = Slate700, modifier = Modifier.size(16.dp))
              }
            }
          }
        }

        // Two explicit options requested by user: 1) Cancel, 2) Print
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Option 1: Cancel
          OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("btn_save_cancel"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate600)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Slate700, maxLines = 1, softWrap = false)
          }

          // Option 2: Print
          Button(
            onClick = { onPrint(copies) },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("btn_save_print"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (copies > 1) "Print ($copies)" else "Print",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }
  }
}
