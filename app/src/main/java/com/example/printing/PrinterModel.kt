package com.example.printing

enum class PrinterConnectionType {
  BLUETOOTH,
  USB
}

data class DiscoveredPrinter(
  val name: String,
  val address: String, // MAC address for Bluetooth, or DeviceName for USB
  val connectionType: PrinterConnectionType,
  val isPaired: Boolean = false,
  val isLastUsed: Boolean = false
)

data class PrintJob(
  val title: String,
  val plainTextReceipt: String,
  val escPosData: ByteArray,
  val copies: Int = 1
)
