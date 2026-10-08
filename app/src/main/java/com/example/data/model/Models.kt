package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "bank_accounts")
data class BankAccount(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val bankName: String,
  val accountNumberLast4: String
) {
  val displayName: String
    get() = if (accountNumberLast4.isNotBlank()) "$bankName (..$accountNumberLast4)" else bankName
}

@Entity(tableName = "creditors")
data class Creditor(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val phone: String = "",
  val address: String = ""
)

@Entity(tableName = "catalog_items")
data class CatalogItem(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val designName: String,
  val size: String = "",
  val category: String = "Tiles", // Tiles, Chemicals, Accessories, General
  val defaultUnit: String = "Box", // Box, Kg, Pcs, Bag
  val defaultRate: Double = 0.0
)

data class OrderItem(
  val designName: String,
  val size: String = "",
  val quantity: Double = 0.0,
  val unit: String = "Box", // Box, Kg, Pcs, Bag
  val rate: Double = 0.0,
  val itemTotal: Double = 0.0
) {
  fun toJson(): JSONObject {
    val obj = JSONObject()
    obj.put("designName", designName)
    obj.put("size", size)
    obj.put("quantity", quantity)
    obj.put("unit", unit)
    obj.put("rate", rate)
    obj.put("itemTotal", itemTotal)
    return obj
  }

  companion object {
    fun fromJson(obj: JSONObject): OrderItem {
      return OrderItem(
        designName = obj.optString("designName", ""),
        size = obj.optString("size", ""),
        quantity = obj.optDouble("quantity", 0.0),
        unit = obj.optString("unit", "Box"),
        rate = obj.optDouble("rate", 0.0),
        itemTotal = obj.optDouble("itemTotal", 0.0)
      )
    }
  }
}

object JsonUtils {
  fun itemsToJson(items: List<OrderItem>): String {
    val array = JSONArray()
    items.forEach { array.put(it.toJson()) }
    return array.toString()
  }

  fun jsonToItems(jsonStr: String): List<OrderItem> {
    if (jsonStr.isBlank()) return emptyList()
    return try {
      val array = JSONArray(jsonStr)
      val list = mutableListOf<OrderItem>()
      for (i in 0 until array.length()) {
        list.add(OrderItem.fromJson(array.getJSONObject(i)))
      }
      list
    } catch (e: Exception) {
      emptyList()
    }
  }

  fun phonesToJson(phones: List<String>): String {
    val array = JSONArray()
    phones.filter { it.isNotBlank() }.forEach { array.put(it.trim()) }
    return array.toString()
  }

  fun jsonToPhones(jsonStr: String): List<String> {
    if (jsonStr.isBlank()) return emptyList()
    return try {
      val array = JSONArray(jsonStr)
      val list = mutableListOf<String>()
      for (i in 0 until array.length()) {
        list.add(array.getString(i))
      }
      list
    } catch (e: Exception) {
      emptyList()
    }
  }

  fun formatDateKey(timeMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timeMillis))
  }

  fun formatDateDisplay(timeMillis: Long): String {
    // E.g., "12 September, Saturday" as user requested
    val sdf = SimpleDateFormat("d MMMM, EEEE", Locale.getDefault())
    return sdf.format(Date(timeMillis))
  }

  fun formatTimeDisplay(timeMillis: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timeMillis))
  }
}

@Entity(tableName = "transactions")
data class TransactionRecord(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val type: String, // CASH_RECEIPT, CASH_GIVE, CASH_SALE, ONLINE_SALE, CREDIT_SALE, GOODS_RETURN
  val serialNumber: Long = 0L, // Continuous running from 1 to infinity, never resets next day
  val referenceNumber: String = "", // Reference number for Sales, Goods Return, etc.
  val receiptMethod: String = "", // Cash or Online
  val bankAccountId: Long? = null,
  val bankAccountName: String = "",
  val personName: String = "",
  val phoneNumbersJson: String = "[]",
  val address: String = "",
  val deliveryPerson: String = "",
  val itemsJson: String = "[]",
  val cartage: Double = 0.0,
  val totalAmount: Double = 0.0, // received amount, given amount, cash sale total, online sale total, or goods return refund
  val timestamp: Long = System.currentTimeMillis(),
  val dateKey: String = JsonUtils.formatDateKey(System.currentTimeMillis()),
  val dateFormatted: String = JsonUtils.formatDateDisplay(System.currentTimeMillis())
) {
  val itemsList: List<OrderItem>
    get() = JsonUtils.jsonToItems(itemsJson)

  val phoneNumbersList: List<String>
    get() = JsonUtils.jsonToPhones(phoneNumbersJson)

  val formattedTime: String
    get() = JsonUtils.formatTimeDisplay(timestamp)

  val shortItemsDescription: String
    get() {
      val items = itemsList
      if (items.isEmpty()) return ""
      return items.joinToString(", ") { item ->
        val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
        "${item.designName} ($qty ${item.unit})"
      }
    }
}
