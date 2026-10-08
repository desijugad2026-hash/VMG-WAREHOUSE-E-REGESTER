package com.example.printing

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class PrinterManager(private val context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)

  private val isPrintingActive = AtomicBoolean(false)

  private val bluetoothManager: BluetoothManager? =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

  private val usbManager: UsbManager? =
    context.getSystemService(Context.USB_SERVICE) as? UsbManager

  // Standard Serial Port Profile (SPP) UUID for Bluetooth Thermal Printers
  private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

  companion object {
    const val ACTION_USB_PERMISSION = "com.example.printing.USB_PERMISSION"
    private const val KEY_LAST_TYPE = "last_printer_type"
    private const val KEY_LAST_ADDRESS = "last_printer_address"
    private const val KEY_LAST_NAME = "last_printer_name"
  }

  fun hasBluetoothPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
      ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH
      ) == PackageManager.PERMISSION_GRANTED
    }
  }

  fun isBluetoothEnabled(): Boolean {
    return bluetoothAdapter?.isEnabled == true
  }

  fun getLastUsedPrinter(): DiscoveredPrinter? {
    val typeStr = prefs.getString(KEY_LAST_TYPE, null) ?: return null
    val address = prefs.getString(KEY_LAST_ADDRESS, null) ?: return null
    val name = prefs.getString(KEY_LAST_NAME, "Printer") ?: "Printer"
    val type = try {
      PrinterConnectionType.valueOf(typeStr)
    } catch (e: Exception) {
      PrinterConnectionType.BLUETOOTH
    }
    return DiscoveredPrinter(
      name = name,
      address = address,
      connectionType = type,
      isLastUsed = true
    )
  }

  fun saveLastUsedPrinter(printer: DiscoveredPrinter) {
    prefs.edit()
      .putString(KEY_LAST_TYPE, printer.connectionType.name)
      .putString(KEY_LAST_ADDRESS, printer.address)
      .putString(KEY_LAST_NAME, printer.name)
      .apply()
  }

  @SuppressLint("MissingPermission")
  fun getPairedBluetoothPrinters(): List<DiscoveredPrinter> {
    if (!hasBluetoothPermission() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
      return emptyList()
    }
    val lastAddress = prefs.getString(KEY_LAST_ADDRESS, null)
    return try {
      bluetoothAdapter.bondedDevices?.map { device ->
        DiscoveredPrinter(
          name = device.name?.takeIf { it.isNotBlank() } ?: "Bluetooth Printer (${device.address})",
          address = device.address,
          connectionType = PrinterConnectionType.BLUETOOTH,
          isPaired = true,
          isLastUsed = device.address == lastAddress
        )
      } ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  fun getConnectedUsbPrinters(): List<DiscoveredPrinter> {
    if (usbManager == null) return emptyList()
    val lastAddress = prefs.getString(KEY_LAST_ADDRESS, null)
    val list = mutableListOf<DiscoveredPrinter>()

    for (device in usbManager.deviceList.values) {
      if (isUsbPrinter(device)) {
        val printerName = getUsbDeviceName(device)
        list.add(
          DiscoveredPrinter(
            name = printerName,
            address = device.deviceName,
            connectionType = PrinterConnectionType.USB,
            isLastUsed = device.deviceName == lastAddress
          )
        )
      }
    }
    return list
  }

  private fun isUsbPrinter(device: UsbDevice): Boolean {
    if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true
    for (i in 0 until device.interfaceCount) {
      val intf = device.getInterface(i)
      if (intf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
      for (j in 0 until intf.endpointCount) {
        val endpoint = intf.getEndpoint(j)
        if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
          endpoint.direction == UsbConstants.USB_DIR_OUT
        ) {
          return true
        }
      }
    }
    return false
  }

  private fun getUsbDeviceName(device: UsbDevice): String {
    val productName = try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) device.productName else null
    } catch (e: Exception) {
      null
    }
    val manufacturer = try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) device.manufacturerName else null
    } catch (e: Exception) {
      null
    }
    return when {
      !productName.isNullOrBlank() && !manufacturer.isNullOrBlank() -> "$manufacturer $productName"
      !productName.isNullOrBlank() -> productName
      else -> "USB Thermal Printer (ID: ${device.deviceId})"
    }
  }

  /**
   * Discovers Bluetooth devices asynchronously using a BroadcastReceiver callback
   */
  @SuppressLint("MissingPermission")
  fun startBluetoothDiscovery(
    onDeviceFound: (DiscoveredPrinter) -> Unit,
    onFinished: () -> Unit
  ): BroadcastReceiver? {
    if (!hasBluetoothPermission() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
      onFinished()
      return null
    }

    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
          BluetoothDevice.ACTION_FOUND -> {
            val device: BluetoothDevice? =
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                  BluetoothDevice.EXTRA_DEVICE,
                  BluetoothDevice::class.java
                )
              } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
              }
            device?.let {
              val name = it.name?.takeIf { n -> n.isNotBlank() } ?: "Thermal Printer (${it.address})"
              val lastAddress = prefs.getString(KEY_LAST_ADDRESS, null)
              onDeviceFound(
                DiscoveredPrinter(
                  name = name,
                  address = it.address,
                  connectionType = PrinterConnectionType.BLUETOOTH,
                  isPaired = it.bondState == BluetoothDevice.BOND_BONDED,
                  isLastUsed = it.address == lastAddress
                )
              )
            }
          }
          BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
            onFinished()
          }
        }
      }
    }

    val filter = IntentFilter().apply {
      addAction(BluetoothDevice.ACTION_FOUND)
      addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    }

    try {
      context.registerReceiver(receiver, filter)
      if (bluetoothAdapter.isDiscovering) {
        bluetoothAdapter.cancelDiscovery()
      }
      bluetoothAdapter.startDiscovery()
      return receiver
    } catch (e: Exception) {
      onFinished()
      return null
    }
  }

  @SuppressLint("MissingPermission")
  fun stopBluetoothDiscovery(receiver: BroadcastReceiver?) {
    try {
      if (bluetoothAdapter?.isDiscovering == true) {
        bluetoothAdapter.cancelDiscovery()
      }
    } catch (_: Exception) {}
    try {
      receiver?.let { context.unregisterReceiver(it) }
    } catch (_: Exception) {}
  }

  /**
   * Prints the given ESC/POS byte array to the selected printer with duplicate protection
   */
  suspend fun print(printer: DiscoveredPrinter, data: ByteArray, copies: Int = 1): Result<Unit> = withContext(Dispatchers.IO) {
    if (!isPrintingActive.compareAndSet(false, true)) {
      return@withContext Result.failure(IllegalStateException("A print job is already in progress. Please wait."))
    }
    val safeCopies = copies.coerceIn(1, 20)
    try {
      when (printer.connectionType) {
        PrinterConnectionType.BLUETOOTH -> printViaBluetooth(printer.address, data, safeCopies)
        PrinterConnectionType.USB -> printViaUsb(printer.address, data, safeCopies)
      }
      saveLastUsedPrinter(printer)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(sanitizePrintException(e))
    } finally {
      isPrintingActive.set(false)
    }
  }

  /**
   * Sends a small test print receipt to verify connection
   */
  suspend fun testPrint(printer: DiscoveredPrinter): Result<Unit> {
    val testData = EscPosReceiptBuilder.generateTestPrintEscPos()
    return print(printer, testData)
  }

  /**
   * Prints 1 inch of solid black to test thermal print head condition and heating elements
   */
  suspend fun printOneInchBlack(printer: DiscoveredPrinter): Result<Unit> {
    val blackData = EscPosReceiptBuilder.generateOneInchBlackEscPos()
    return print(printer, blackData)
  }

  @SuppressLint("MissingPermission")
  private fun printViaBluetooth(macAddress: String, data: ByteArray, copies: Int = 1) {
    if (!hasBluetoothPermission()) {
      throw IllegalStateException("Bluetooth permission not granted. Please allow Bluetooth permission.")
    }
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
      throw IllegalStateException("Bluetooth is turned off. Please turn it on and try again.")
    }

    try {
      bluetoothAdapter.cancelDiscovery()
    } catch (_: Exception) {}

    val device = try {
      bluetoothAdapter.getRemoteDevice(macAddress)
    } catch (e: Exception) {
      null
    } ?: throw IllegalStateException("Printer not found.")

    var socket: BluetoothSocket? = null
    var out: OutputStream? = null

    // Multi-stage connection chain for POSiFLOW PSF310 & ESC/POS printers:
    // 1. Standard SPP UUID socket
    // 2. Insecure SPP UUID socket (handles printers without authentication/PIN)
    // 3. Direct RFCOMM Channel 1 via reflection
    // 4. Insecure RFCOMM Channel 1 via reflection
    var isConnected = false

    try {
      socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
      socket.connect()
      isConnected = socket.isConnected
    } catch (_: Exception) {
      try { socket?.close() } catch (_: Exception) {}
      socket = null
    }

    if (!isConnected) {
      try {
        socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
        socket.connect()
        isConnected = socket.isConnected
      } catch (_: Exception) {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
      }
    }

    if (!isConnected) {
      try {
        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        socket = m.invoke(device, 1) as? BluetoothSocket
        socket?.connect()
        isConnected = socket?.isConnected == true
      } catch (_: Exception) {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
      }
    }

    if (!isConnected) {
      try {
        val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
        socket = m.invoke(device, 1) as? BluetoothSocket
        socket?.connect()
        isConnected = socket?.isConnected == true
      } catch (_: Exception) {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
      }
    }

    if (!isConnected || socket == null) {
      throw IllegalStateException("Could not connect to printer.")
    }

    try {
      out = socket.outputStream
      // Stream each copy in 1736-byte chunks (exact 24-row raster strip) with 30ms pacing
      // so the printer's SRAM buffer never overflows across multi-copy raster jobs
      val chunkSize = 1736
      for (copyIndex in 1..copies.coerceAtLeast(1)) {
        var offset = 0
        while (offset < data.size) {
          val count = (data.size - offset).coerceAtMost(chunkSize)
          out.write(data, offset, count)
          out.flush()
          offset += count
          if (offset < data.size) {
            Thread.sleep(30)
          }
        }
        if (copyIndex < copies) {
          // Pause between copies so printer stepper motor catches up before next copy starts
          out.write(byteArrayOf(0x1B, 0x64, 0x02))
          out.flush()
          Thread.sleep(950)
        }
      }
      // Feed 2 extra lines after final copy so THANK YOU clears the metal tear bar completely
      out.write(byteArrayOf(0x1B, 0x64, 0x02))
      out.flush()
      // Wait 1400ms for physical printhead to finish printing the final rows before closing socket
      Thread.sleep(1400)
    } catch (e: Exception) {
      throw IllegalStateException("Printing failed. Please try again.")
    } finally {
      try { out?.close() } catch (_: Exception) {}
      try { socket.close() } catch (_: Exception) {}
    }
  }

  private fun printViaUsb(deviceName: String, data: ByteArray, copies: Int = 1) {
    if (usbManager == null) {
      throw IllegalStateException("USB is not supported on this device.")
    }

    val device = usbManager.deviceList.values.find { it.deviceName == deviceName }
      ?: throw IllegalStateException("Printer not found.")

    if (!usbManager.hasPermission(device)) {
      requestUsbPermission(device)
      throw IllegalStateException("USB permission required. Please allow access when prompted and retry.")
    }

    var connection: UsbDeviceConnection? = null
    var printerInterface: UsbInterface? = null

    try {
      connection = usbManager.openDevice(device)
        ?: throw IllegalStateException("Could not connect to printer.")

      var bulkEndpoint: UsbEndpoint? = null

      for (i in 0 until device.interfaceCount) {
        val intf = device.getInterface(i)
        for (j in 0 until intf.endpointCount) {
          val ep = intf.getEndpoint(j)
          if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
            ep.direction == UsbConstants.USB_DIR_OUT
          ) {
            bulkEndpoint = ep
            printerInterface = intf
            break
          }
        }
        if (bulkEndpoint != null) break
      }

      if (bulkEndpoint == null || printerInterface == null) {
        throw IllegalStateException("Could not connect to printer.")
      }

      val claimed = connection.claimInterface(printerInterface, true)
      if (!claimed) {
        throw IllegalStateException("Could not connect to printer.")
      }

      // Send each copy in 1736-byte strips with 15ms pacing for uniform thermal head energy
      val chunkSize = 1736
      for (copyIndex in 1..copies.coerceAtLeast(1)) {
        var offset = 0
        while (offset < data.size) {
          val count = (data.size - offset).coerceAtMost(chunkSize)
          val chunk = ByteArray(count)
          System.arraycopy(data, offset, chunk, 0, count)

          val transferred = connection.bulkTransfer(bulkEndpoint, chunk, count, 5000)
          if (transferred < 0) {
            throw IllegalStateException("Printing failed. Please try again.")
          }
          offset += count
          if (offset < data.size) {
            Thread.sleep(15)
          }
        }
        if (copyIndex < copies) {
          val feedBytes = byteArrayOf(0x1B, 0x64, 0x02)
          connection.bulkTransfer(bulkEndpoint, feedBytes, feedBytes.size, 3000)
          Thread.sleep(700)
        }
      }

      val finalFeed = byteArrayOf(0x1B, 0x64, 0x02)
      connection.bulkTransfer(bulkEndpoint, finalFeed, finalFeed.size, 3000)
      Thread.sleep(900)
    } catch (e: Exception) {
      if (e is IllegalStateException) throw e
      throw IllegalStateException("Printing failed. Please try again.")
    } finally {
      try {
        printerInterface?.let { connection?.releaseInterface(it) }
      } catch (_: Exception) {}
      try {
        connection?.close()
      } catch (_: Exception) {}
    }
  }

  private fun sanitizePrintException(e: Exception): Exception {
    val msg = e.message ?: ""
    return when {
      msg.contains("Bluetooth is turned off", ignoreCase = true) ||
      msg.contains("Bluetooth is disabled", ignoreCase = true) ->
        IllegalStateException("Bluetooth is turned off. Please turn it on and try again.")
      msg.contains("Printer not found", ignoreCase = true) ->
        IllegalStateException("Printer not found.")
      msg.contains("USB permission", ignoreCase = true) ->
        e
      msg.contains("Bluetooth permission", ignoreCase = true) ->
        e
      msg.contains("Could not connect", ignoreCase = true) ->
        IllegalStateException("Could not connect to printer.")
      msg.contains("already in progress", ignoreCase = true) ->
        e
      else ->
        IllegalStateException("Printing failed. Please try again.")
    }
  }

  fun requestUsbPermission(device: UsbDevice) {
    if (usbManager == null) return
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val intent = Intent(ACTION_USB_PERMISSION)
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
    usbManager.requestPermission(device, pendingIntent)
  }
}
