package com.example.printing

import android.content.BroadcastReceiver
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MintGreenBg
import com.example.ui.theme.MintGreenIcon
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoftRoseBg
import com.example.ui.theme.SoftRoseIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PrinterSelectionDialog(
  escPosData: ByteArray,
  copies: Int,
  onDismiss: () -> Unit,
  onPrintSuccess: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val printerManager = remember { PrinterManager(context) }

  // Tabs: 0 -> Bluetooth, 1 -> USB
  var selectedTab by remember { mutableIntStateOf(0) }
  var activeCopies by remember(copies) { mutableIntStateOf(copies.coerceIn(1, 10)) }

  // Discovered printers lists
  val bluetoothPrinters = remember { mutableStateListOf<DiscoveredPrinter>() }
  val usbPrinters = remember { mutableStateListOf<DiscoveredPrinter>() }

  var isSearchingBluetooth by remember { mutableStateOf(false) }
  var discoveryReceiver by remember { mutableStateOf<BroadcastReceiver?>(null) }

  // Printing state
  var isPrinting by remember { mutableStateOf(false) }
  var printSuccess by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var selectedPrinterForRetry by remember { mutableStateOf<DiscoveredPrinter?>(null) }

  val lastUsedPrinter = remember { printerManager.getLastUsedPrinter() }

  // Bluetooth permission launcher for Android 12+
  val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions.values.all { it }
    if (granted) {
      // Reload paired devices
      bluetoothPrinters.clear()
      bluetoothPrinters.addAll(printerManager.getPairedBluetoothPrinters())
      startBluetoothSearch(printerManager, bluetoothPrinters) { isSearchingBluetooth = it }
    } else {
      errorMessage = "Bluetooth permission is required to search and connect to printers."
    }
  }

  fun performPrint(printer: DiscoveredPrinter) {
    isPrinting = true
    errorMessage = null
    printSuccess = false
    selectedPrinterForRetry = printer

    scope.launch {
      val result = printerManager.print(printer, escPosData, activeCopies)
      isPrinting = false
      if (result.isSuccess) {
        printSuccess = true
        delay(1200)
        onPrintSuccess()
        onDismiss()
      } else {
        val ex = result.exceptionOrNull()
        errorMessage = ex?.message ?: "Printing failed. Please check printer power and paper."
      }
    }
  }

  fun performTestPrint(printer: DiscoveredPrinter) {
    isPrinting = true
    errorMessage = null
    printSuccess = false
    selectedPrinterForRetry = printer

    scope.launch {
      val result = printerManager.testPrint(printer)
      isPrinting = false
      if (result.isSuccess) {
        printSuccess = true
        delay(2000)
        printSuccess = false
      } else {
        val ex = result.exceptionOrNull()
        errorMessage = ex?.message ?: "Test print failed. Please check printer power and paper."
      }
    }
  }

  fun performBlackInchTest(printer: DiscoveredPrinter) {
    isPrinting = true
    errorMessage = null
    printSuccess = false
    selectedPrinterForRetry = printer

    scope.launch {
      val result = printerManager.printOneInchBlack(printer)
      isPrinting = false
      if (result.isSuccess) {
        printSuccess = true
        delay(2000)
        printSuccess = false
      } else {
        val ex = result.exceptionOrNull()
        errorMessage = ex?.message ?: "1-inch black test failed. Please check printer power and paper."
      }
    }
  }

  // Load initial devices
  LaunchedEffect(Unit) {
    // Load Bluetooth paired devices
    if (printerManager.hasBluetoothPermission()) {
      bluetoothPrinters.clear()
      bluetoothPrinters.addAll(printerManager.getPairedBluetoothPrinters())
    }
    // Load USB devices
    usbPrinters.clear()
    usbPrinters.addAll(printerManager.getConnectedUsbPrinters())

    // If last used printer was USB, default to USB tab
    if (lastUsedPrinter?.connectionType == PrinterConnectionType.USB) {
      selectedTab = 1
    }
  }

  // USB Permission and Bluetooth discovery receiver cleanup
  DisposableEffect(Unit) {
    val usbPermissionReceiver = object : BroadcastReceiver() {
      override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
        if (intent?.action == PrinterManager.ACTION_USB_PERMISSION) {
          usbPrinters.clear()
          usbPrinters.addAll(printerManager.getConnectedUsbPrinters())
        }
      }
    }
    val filter = android.content.IntentFilter(PrinterManager.ACTION_USB_PERMISSION)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(usbPermissionReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
    } else {
      context.registerReceiver(usbPermissionReceiver, filter)
    }

    onDispose {
      try { context.unregisterReceiver(usbPermissionReceiver) } catch (_: Exception) {}
      discoveryReceiver?.let { printerManager.stopBluetoothDiscovery(it) }
    }
  }

  Dialog(
    onDismissRequest = {
      if (!isPrinting) onDismiss()
    },
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 520.dp)
        .fillMaxHeight(0.82f),
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
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Select Printer",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
            Text(
              text = "Printing $activeCopies ${if (activeCopies == 1) "copy" else "copies"}",
              fontSize = 12.sp,
              color = PrimaryBlue,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Inline Copies Stepper
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
              .background(Slate100, RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("Copies:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            Surface(
              modifier = Modifier
                .size(26.dp)
                .clip(CircleShape),
              color = Color.White,
              border = BorderStroke(1.dp, Slate200),
              onClick = { if (!isPrinting && activeCopies > 1) activeCopies-- }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Remove, contentDescription = "Less", tint = Slate700, modifier = Modifier.size(14.dp))
              }
            }
            Text(
              text = "$activeCopies",
              fontSize = 14.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Slate900
            )
            Surface(
              modifier = Modifier
                .size(26.dp)
                .clip(CircleShape),
              color = Color.White,
              border = BorderStroke(1.dp, Slate200),
              onClick = { if (!isPrinting && activeCopies < 10) activeCopies++ }
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "More", tint = Slate700, modifier = Modifier.size(14.dp))
              }
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(
            onClick = onDismiss,
            enabled = !isPrinting,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Last Used Printer Shortcut Card
        lastUsedPrinter?.let { lastPrinter ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Star, contentDescription = null, tint = MintGreenIcon, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = lastPrinter.name,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = Slate900,
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      color = Color(0xFFDCFCE7),
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        "LAST USED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintGreenIcon,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(
                    text = "${lastPrinter.connectionType.name} • Ready to print",
                    fontSize = 11.sp,
                    color = Slate600,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                }
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { performTestPrint(lastPrinter) },
                  enabled = !isPrinting,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(36.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
                ) {
                  Text("Test", fontSize = 12.sp, maxLines = 1)
                }

                Button(
                  onClick = { performPrint(lastPrinter) },
                  enabled = !isPrinting,
                  colors = ButtonDefaults.buttonColors(containerColor = MintGreenIcon),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .height(36.dp)
                    .widthIn(min = 84.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                  Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Print", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        // Connection Tabs (Bluetooth vs USB)
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = Slate100,
          contentColor = Slate900,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = PrimaryBlue,
              height = 3.dp
            )
          }
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bluetooth", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, maxLines = 1, softWrap = false)
              }
            }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = {
              selectedTab = 1
              // Refresh USB devices on tab switch
              usbPrinters.clear()
              usbPrinters.addAll(printerManager.getConnectedUsbPrinters())
            },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Wired / USB", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, maxLines = 1, softWrap = false)
              }
            }
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status Messages: Printing, Success, or Error
        if (isPrinting) {
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
              Spacer(modifier = Modifier.width(10.dp))
              Text("Connecting to printer and printing receipt...", fontSize = 13.sp, color = Slate800)
            }
          }
        }

        if (printSuccess) {
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MintGreenBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintGreenIcon, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text("Printed Successfully!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MintGreenIcon)
            }
          }
        }

        errorMessage?.let { msg ->
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SoftRoseBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SoftRoseIcon, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(msg, fontSize = 12.sp, color = Color(0xFF9F1239), modifier = Modifier.weight(1f))
              }
              selectedPrinterForRetry?.let { retryPrinter ->
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                  onClick = { performPrint(retryPrinter) },
                  colors = ButtonDefaults.buttonColors(containerColor = SoftRoseIcon),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.height(32.dp)
                ) {
                  Text("Retry Print", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        // Device List Section
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          if (selectedTab == 0) {
            // Bluetooth List
            Column(modifier = Modifier.fillMaxHeight()) {
              // Search button
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Bluetooth Printers",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate800,
                  maxLines = 1,
                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                OutlinedButton(
                  onClick = {
                    if (!printerManager.hasBluetoothPermission()) {
                      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        bluetoothPermissionLauncher.launch(
                          arrayOf(
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.BLUETOOTH_SCAN
                          )
                        )
                      } else {
                        bluetoothPermissionLauncher.launch(
                          arrayOf(
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_ADMIN,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                          )
                        )
                      }
                    } else if (!printerManager.isBluetoothEnabled()) {
                      errorMessage = "Bluetooth is disabled. Please turn on Bluetooth on your device."
                    } else {
                      discoveryReceiver?.let { printerManager.stopBluetoothDiscovery(it) }
                      discoveryReceiver = printerManager.startBluetoothDiscovery(
                        onDeviceFound = { newDev ->
                          if (bluetoothPrinters.none { it.address == newDev.address }) {
                            bluetoothPrinters.add(newDev)
                          }
                        },
                        onFinished = {
                          isSearchingBluetooth = false
                        }
                      )
                      isSearchingBluetooth = true
                    }
                  },
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(34.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                  enabled = !isSearchingBluetooth && !isPrinting
                ) {
                  if (isSearchingBluetooth) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scanning...", fontSize = 11.sp, maxLines = 1, softWrap = false)
                  } else {
                    Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan Bluetooth", fontSize = 11.sp, maxLines = 1, softWrap = false)
                  }
                }
              }

              if (bluetoothPrinters.isEmpty()) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Slate600, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                      "No Bluetooth printers found yet",
                      fontSize = 13.sp,
                      color = Slate700,
                      fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      "Make sure printer is paired or tap 'Search Bluetooth Printers'",
                      fontSize = 11.sp,
                      color = Slate600
                    )
                  }
                }
              } else {
                LazyColumn(
                  modifier = Modifier.fillMaxHeight(),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  items(bluetoothPrinters, key = { it.address }) { printer ->
                    PrinterItemRow(
                      printer = printer,
                      isPrinting = isPrinting,
                      onTestPrint = { performTestPrint(printer) },
                      onSelect = { performPrint(printer) }
                    )
                  }
                }
              }
            }
          } else {
            // USB List
            Column(modifier = Modifier.fillMaxHeight()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Connected USB Printers",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate800
                )

                IconButton(
                  onClick = {
                    usbPrinters.clear()
                    usbPrinters.addAll(printerManager.getConnectedUsbPrinters())
                  },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = "Refresh USB", tint = Slate700)
                }
              }

              if (usbPrinters.isEmpty()) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                  contentAlignment = Alignment.Center
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                  ) {
                    Icon(Icons.Default.Usb, contentDescription = null, tint = Slate600, modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                      "No USB Printer Detected",
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = Slate900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      "Connect your printer via USB cable / OTG adapter, then tap the refresh button.",
                      fontSize = 12.sp,
                      color = Slate600,
                      textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                  }
                }
              } else {
                LazyColumn(
                  modifier = Modifier.fillMaxHeight(),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  items(usbPrinters, key = { it.address }) { printer ->
                    PrinterItemRow(
                      printer = printer,
                      isPrinting = isPrinting,
                      onTestPrint = { performTestPrint(printer) },
                      onSelect = { performPrint(printer) }
                    )
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

private fun startBluetoothSearch(
  printerManager: PrinterManager,
  list: MutableList<DiscoveredPrinter>,
  onSearchingChanged: (Boolean) -> Unit
) {
  onSearchingChanged(true)
  printerManager.startBluetoothDiscovery(
    onDeviceFound = { dev ->
      if (list.none { it.address == dev.address }) {
        list.add(dev)
      }
    },
    onFinished = {
      onSearchingChanged(false)
    }
  )
}

@Composable
private fun PrinterItemRow(
  printer: DiscoveredPrinter,
  isPrinting: Boolean,
  onTestPrint: () -> Unit,
  onSelect: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Slate50),
    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Slate200, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (printer.connectionType == PrinterConnectionType.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Usb,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
          Text(
            text = printer.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate900,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
          Text(
            text = if (printer.isPaired) "Paired • Ready" else "Discovered • Ready",
            fontSize = 11.sp,
            color = Slate600,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onTestPrint,
          enabled = !isPrinting,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(36.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
        ) {
          Text("Test", fontSize = 12.sp, maxLines = 1)
        }

        Button(
          onClick = onSelect,
          enabled = !isPrinting,
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .height(36.dp)
            .widthIn(min = 72.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
          Text("Print", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }
    }
  }
}
