package com.example.printing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun PrintPreviewDialog(
  title: String = "Print Receipt Preview",
  previewText: String,
  onDismiss: () -> Unit,
  onViewPdf: () -> Unit,
  onProceedToPrint: (copies: Int) -> Unit
) {
  var copies by remember { mutableIntStateOf(1) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 520.dp)
        .fillMaxHeight(0.85f),
      shape = RoundedCornerShape(20.dp),
      color = Color.White,
      shadowElevation = 8.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .padding(18.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFEEF2FF)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Print,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Exact receipt layout",
                fontSize = 11.sp,
                color = Slate600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Receipt Paper Preview Container
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Slate50),
          border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(14.dp)
          ) {
            Text(
              text = previewText,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              lineHeight = 16.sp,
              color = Slate900,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Copies Selector (Matching prompt: "when click on print then it also ask how meny copies needed default 1")
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Slate100, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = "Print Copies",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = Slate800,
              maxLines = 1,
              softWrap = false
            )
            Text(
              text = "Number of receipts to print",
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
                .size(32.dp)
                .clip(CircleShape),
              color = Color.White,
              border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
              onClick = { if (copies > 1) copies-- }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Slate700, modifier = Modifier.size(16.dp))
              }
            }

            Text(
              text = "$copies",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900,
              modifier = Modifier.width(24.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Surface(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
              color = Color.White,
              border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
              onClick = { if (copies < 10) copies++ }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Slate700, modifier = Modifier.size(16.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // PDF View Button
        OutlinedButton(
          onClick = onViewPdf,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_view_pdf"),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E293B)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
          Icon(
            Icons.Default.PictureAsPdf,
            contentDescription = null,
            tint = Color(0xFFDC2626),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            "View / Save as PDF",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: Cancel and Print
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .testTag("btn_cancel_print"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
          ) {
            Text("Cancel", fontSize = 14.sp, maxLines = 1, softWrap = false)
          }

          Button(
            onClick = { onProceedToPrint(copies) },
            modifier = Modifier
              .weight(1.2f)
              .testTag("btn_confirm_print"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
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
