package com.example

import com.example.data.model.TransactionRecord
import com.example.printing.EscPosReceiptBuilder
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testEscPosReceiptBuilderCommands() {
    val bytes = EscPosReceiptBuilder.generateTestPrintEscPos()
    assertTrue(bytes.isNotEmpty())

    // First bytes must be ESC @ (0x1B, 0x40) - printer init at start
    assertEquals(0x1B.toByte(), bytes[0])
    assertEquals(0x40.toByte(), bytes[1])

    // Must end with cut command (0x1D, 0x56, 0x42, 0x00)
    val len = bytes.size
    assertEquals(0x1D.toByte(), bytes[len - 4])
    assertEquals(0x56.toByte(), bytes[len - 3])
    assertEquals(0x42.toByte(), bytes[len - 2])
    assertEquals(0x00.toByte(), bytes[len - 1])
  }

  @Test
  fun testCashSaleReceiptZeroWasteFormat() {
    val tx = TransactionRecord(
      id = 1L,
      type = "CASH_SALE",
      personName = "Ramesh Kumar",
      phoneNumbersJson = "[\"9876543210\"]",
      totalAmount = 1450.0,
      dateKey = "2026-09-14",
      timestamp = 1773489000000L,
      cartage = 50.0,
      itemsJson = """[{"designName":"Cotton Bedsheet","size":"King","unit":"Pc","quantity":2.0,"rate":700.0,"itemTotal":1400.0}]"""
    )

    val plainText = EscPosReceiptBuilder.generatePlainText(tx, 1)
    assertTrue(plainText.contains("ESTIMATE"))
    assertTrue(plainText.contains("Ramesh Kumar"))
    assertTrue(plainText.contains("QTY / DESCRIPTION"))
    assertTrue(plainText.contains("V. NO.: ___________"))
    assertTrue(plainText.contains("PAYMENT MODE: CASH"))
    assertTrue(plainText.contains("GRAND TOTAL"))
    assertTrue(plainText.contains("Rs. 1,450"))
    assertTrue(plainText.contains("NO RETURN / NO EXCHANGE"))
    assertTrue(plainText.contains("THANK YOU"))

    val escPosBytes = EscPosReceiptBuilder.generateEscPos(tx, 1, 1)
    assertTrue(escPosBytes.isNotEmpty())
    // Ensure starts with ESC @
    assertEquals(0x1B.toByte(), escPosBytes[0])
    assertEquals(0x40.toByte(), escPosBytes[1])
  }
}
