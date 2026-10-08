package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseTopBar(
  title: String,
  subtitle: String? = null,
  onBackClick: (() -> Unit)? = null,
  actions: @Composable () -> Unit = {}
) {
  Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
    TopAppBar(
      title = {
        Column {
          Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Slate900,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
          if (!subtitle.isNullOrBlank()) {
            Text(
              text = subtitle,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = Slate600,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      },
      navigationIcon = {
        if (onBackClick != null) {
          IconButton(onClick = onBackClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Slate900
            )
          }
        }
      },
      actions = { actions() },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.White,
        titleContentColor = Slate900
      )
    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(Slate200)
    )
  }
}

@Composable
fun ConfirmDialog(
  title: String,
  message: String,
  confirmButtonText: String = "Delete",
  isDestructive: Boolean = true,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.widthIn(max = 440.dp),
    title = {
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Slate900
      )
    },
    text = {
      Text(
        text = message,
        fontSize = 13.sp,
        color = Slate700
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isDestructive) Color(0xFFDC2626) else PrimaryBlue
        )
      ) {
        Text(confirmButtonText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Cancel", color = Slate700, fontSize = 13.sp, maxLines = 1, softWrap = false)
      }
    },
    containerColor = Color.White,
    shape = RoundedCornerShape(12.dp)
  )
}

@Composable
fun UnitSelectorPills(
  selectedUnit: String,
  onUnitSelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val units = listOf("Box", "Kg", "Pcs", "Bag")
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    units.forEach { unit ->
      val isSelected = unit.equals(selectedUnit, ignoreCase = true)
      Surface(
        modifier = Modifier
          .weight(1f)
          .height(38.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable { onUnitSelected(unit) },
        color = if (isSelected) PrimaryBlue else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Slate200)
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
          Text(
            text = unit,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) Color.White else Slate700,
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }
  }
}
