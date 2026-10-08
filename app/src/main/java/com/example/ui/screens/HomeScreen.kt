package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MintGreenBg
import com.example.ui.theme.MintGreenIcon
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoftBlueBg
import com.example.ui.theme.SoftBlueIcon
import com.example.ui.theme.SoftPurpleIcon
import com.example.ui.theme.SoftRoseBg
import com.example.ui.theme.SoftRoseIcon
import com.example.ui.theme.SoftTealBg
import com.example.ui.theme.SoftTealIcon
import com.example.ui.viewmodel.WarehouseViewModel

enum class AppDestination {
  HOME,
  CASH_RECEIPTS, // Receipt
  CASH_GIVE,     // Payment
  ORDERS,        // Sales (Cash Sale, Online Sale, Debtors)
  GOODS_RETURN,  // Goods Return
  HISTORY,       // Accessible via Top-Right Settings Menu & Quick Bar
  DATA_ENTRY     // Accessible via Top-Right Settings Menu & Quick Bar (Tiles & Chemical)
}

@Composable
fun HomeScreen(
  viewModel: WarehouseViewModel,
  onNavigate: (AppDestination) -> Unit
) {
  var settingsMenuExpanded by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = Color(0xFFF8FAFC)
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 580.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        // Top Header Bar
        Surface(
          color = Color.White,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, Slate200),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "VMG",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Slate900,
                maxLines = 1,
                softWrap = false
              )
              Spacer(modifier = Modifier.width(10.dp))
              Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Slate200)
              ) {
                Text(
                  text = "OFFLINE LEDGER",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.6.sp,
                  color = Slate600,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            // Setting button in top right corner with 2 options: 1 History, 2 Data Entry
            Box {
              IconButton(
                onClick = { settingsMenuExpanded = true },
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFFF8FAFC))
                  .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                  .testTag("btn_settings")
              ) {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = "Settings",
                  tint = Slate700,
                  modifier = Modifier.size(20.dp)
                )
              }

              DropdownMenu(
                expanded = settingsMenuExpanded,
                onDismissRequest = { settingsMenuExpanded = false },
                modifier = Modifier.width(190.dp)
              ) {
                DropdownMenuItem(
                  text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = SoftPurpleIcon,
                        modifier = Modifier.size(18.dp)
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                      Text(
                        text = "History",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Slate900
                      )
                    }
                  },
                  onClick = {
                    settingsMenuExpanded = false
                    onNavigate(AppDestination.HISTORY)
                  }
                )

                DropdownMenuItem(
                  text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                      Text(
                        text = "Data Entry",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Slate900
                      )
                    }
                  },
                  onClick = {
                    settingsMenuExpanded = false
                    onNavigate(AppDestination.DATA_ENTRY)
                  }
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "DAILY VOUCHERS & BILLING",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.8.sp,
          color = Slate500,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        // 4 Primary Vouchers in a Clean, Structured Commercial List
        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          HomeSectionCard(
            title = "1. Sales",
            subtitle = "Cash Sale • Online Sale • Debtors (Credit)",
            icon = Icons.Default.ReceiptLong,
            iconBgColor = SoftBlueBg,
            iconColor = SoftBlueIcon,
            testTag = "card_sales",
            onClick = { onNavigate(AppDestination.ORDERS) }
          )

          HomeSectionCard(
            title = "2. Receipt",
            subtitle = "Record incoming payments (Cash & Online)",
            icon = Icons.Default.ArrowDownward,
            iconBgColor = MintGreenBg,
            iconColor = MintGreenIcon,
            testTag = "card_receipt",
            onClick = { onNavigate(AppDestination.CASH_RECEIPTS) }
          )

          HomeSectionCard(
            title = "3. Payment",
            subtitle = "Record outgoing cash payments",
            icon = Icons.Default.ArrowUpward,
            iconBgColor = SoftRoseBg,
            iconColor = SoftRoseIcon,
            testTag = "card_payment",
            onClick = { onNavigate(AppDestination.CASH_GIVE) }
          )

          HomeSectionCard(
            title = "4. Goods Return",
            subtitle = "Record returned tiles/chemical & refund amount",
            icon = Icons.Default.Replay,
            iconBgColor = SoftTealBg,
            iconColor = SoftTealIcon,
            testTag = "card_goods_return",
            onClick = { onNavigate(AppDestination.GOODS_RETURN) }
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "RECORDS & MASTER CATALOG",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.8.sp,
          color = Slate500,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        // Direct 1-Tap Utility Row for Register History & Master Data Entry
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .clickable { onNavigate(AppDestination.HISTORY) },
            color = Color.White,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Slate200)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = SoftPurpleIcon,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Register History",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate900,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "Daily bills & print",
                  fontSize = 11.sp,
                  color = Slate500,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .clickable { onNavigate(AppDestination.DATA_ENTRY) },
            color = Color.White,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Slate200)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.EditNote,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Master Catalog",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate900,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "Tiles, Debtors, Banks",
                  fontSize = 11.sp,
                  color = Slate500,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
fun HomeSectionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconBgColor: Color,
  iconColor: Color,
  testTag: String,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag)
      .clickable { onClick() },
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, Slate200),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 13.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(iconBgColor),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = iconColor,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = Slate900,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = Slate600,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = "Open",
        tint = Slate400,
        modifier = Modifier.size(14.dp)
      )
    }
  }
}
