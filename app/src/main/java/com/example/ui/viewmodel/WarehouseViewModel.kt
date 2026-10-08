package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.InitialCatalogSeeder
import com.example.data.model.BankAccount
import com.example.data.model.CatalogItem
import com.example.data.model.Creditor
import com.example.data.model.JsonUtils
import com.example.data.model.OrderItem
import com.example.data.model.TransactionRecord
import com.example.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayHistoryGroup(
  val dateKey: String,
  val dateFormatted: String,
  val transactions: List<TransactionRecord>,
  val totalCashReceived: Double,
  val totalCashGiven: Double,
  val totalOnlineReceived: Double = 0.0,
  val netCash: Double
)

class WarehouseViewModel(
  private val repository: WarehouseRepository
) : ViewModel() {

  private val _snackbarMessage = MutableSharedFlow<String>()
  val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

  init {
    viewModelScope.launch {
      InitialCatalogSeeder.seedIfNeeded(repository)
    }
  }

  val bankAccounts: StateFlow<List<BankAccount>> = repository.bankAccounts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val creditors: StateFlow<List<Creditor>> = repository.creditors
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val catalogItems: StateFlow<List<CatalogItem>> = repository.catalogItems
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allTransactions: StateFlow<List<TransactionRecord>> = repository.allTransactions
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Grouped history by day with calculated totals
  val dayHistoryGroups: StateFlow<List<DayHistoryGroup>> = repository.allTransactions
    .combine(MutableStateFlow(Unit)) { txList, _ ->
      val grouped = txList.groupBy { it.dateKey }
      grouped.map { (dateKey, list) ->
        val sortedList = list.sortedBy { it.timestamp }
        val dateFormatted = sortedList.firstOrNull()?.dateFormatted ?: dateKey

        var cashReceived = 0.0
        var onlineReceived = 0.0
        var cashGiven = 0.0

        for (tx in list) {
          when (tx.type) {
            "CASH_RECEIPT" -> {
              if (tx.receiptMethod.equals("Online", ignoreCase = true)) {
                onlineReceived += tx.totalAmount
              } else {
                cashReceived += tx.totalAmount
              }
            }
            "CASH_SALE" -> cashReceived += tx.totalAmount
            "ONLINE_SALE" -> onlineReceived += tx.totalAmount
            "CASH_GIVE" -> cashGiven += tx.totalAmount
            "GOODS_RETURN" -> cashGiven += tx.totalAmount // refund amount given back
            "CREDIT_SALE" -> {} // Credit sale doesn't impact immediate drawer cash
          }
        }

        DayHistoryGroup(
          dateKey = dateKey,
          dateFormatted = dateFormatted,
          transactions = sortedList,
          totalCashReceived = cashReceived,
          totalCashGiven = cashGiven,
          totalOnlineReceived = onlineReceived,
          netCash = cashReceived - cashGiven
        )
      }.sortedByDescending { it.dateKey }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Overall cash summary for today
  val todaySummary: StateFlow<DayHistoryGroup?> = dayHistoryGroups
    .combine(MutableStateFlow(Unit)) { groups, _ ->
      val todayKey = JsonUtils.formatDateKey(System.currentTimeMillis())
      groups.firstOrNull { it.dateKey == todayKey } ?: DayHistoryGroup(
        dateKey = todayKey,
        dateFormatted = JsonUtils.formatDateDisplay(System.currentTimeMillis()),
        transactions = emptyList(),
        totalCashReceived = 0.0,
        totalCashGiven = 0.0,
        totalOnlineReceived = 0.0,
        netCash = 0.0
      )
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  fun showMessage(msg: String) {
    viewModelScope.launch {
      _snackbarMessage.emit(msg)
    }
  }

  // --- Transactions Save ---

  fun saveCashReceipt(
    method: String, // "Cash" or "Online"
    personName: String,
    amount: Double,
    phone: String,
    bankAccountId: Long?,
    bankAccountName: String,
    referenceNumber: String = "",
    onSuccess: (TransactionRecord) -> Unit = {}
  ) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val phonesJson = if (phone.isNotBlank()) JsonUtils.phonesToJson(listOf(phone)) else "[]"
      val nextSerial = repository.getNextSerialNumber()
      val record = TransactionRecord(
        type = "CASH_RECEIPT",
        serialNumber = nextSerial,
        referenceNumber = referenceNumber.trim(),
        receiptMethod = method,
        personName = personName.trim(),
        phoneNumbersJson = phonesJson,
        bankAccountId = if (method == "Online") bankAccountId else null,
        bankAccountName = if (method == "Online") bankAccountName else "",
        totalAmount = amount,
        timestamp = now,
        dateKey = JsonUtils.formatDateKey(now),
        dateFormatted = JsonUtils.formatDateDisplay(now)
      )
      val id = repository.insertTransaction(record)
      val saved = record.copy(id = id)
      _snackbarMessage.emit("Cash Receipt recorded (#$nextSerial)")
      onSuccess(saved)
    }
  }

  fun saveCashGive(
    personName: String,
    phone: String,
    amount: Double,
    referenceNumber: String = "",
    onSuccess: (TransactionRecord) -> Unit = {}
  ) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val phonesJson = if (phone.isNotBlank()) JsonUtils.phonesToJson(listOf(phone)) else "[]"
      val nextSerial = repository.getNextSerialNumber()
      val record = TransactionRecord(
        type = "CASH_GIVE",
        serialNumber = nextSerial,
        referenceNumber = referenceNumber.trim(),
        personName = personName.trim(),
        phoneNumbersJson = phonesJson,
        totalAmount = amount,
        timestamp = now,
        dateKey = JsonUtils.formatDateKey(now),
        dateFormatted = JsonUtils.formatDateDisplay(now)
      )
      val id = repository.insertTransaction(record)
      val saved = record.copy(id = id)
      _snackbarMessage.emit("Payment recorded (#$nextSerial)")
      onSuccess(saved)
    }
  }

  fun saveCashSaleOrder(
    customerName: String,
    phoneNumbers: List<String>,
    address: String,
    referenceNumber: String = "",
    deliveryPerson: String = "",
    items: List<OrderItem>,
    cartage: Double,
    onSuccess: (TransactionRecord) -> Unit = {}
  ) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val itemsTotal = items.sumOf { it.itemTotal }
      val grandTotal = itemsTotal + cartage
      val nextSerial = repository.getNextSerialNumber()
      val record = TransactionRecord(
        type = "CASH_SALE",
        serialNumber = nextSerial,
        referenceNumber = referenceNumber.trim(),
        receiptMethod = "Cash",
        personName = customerName.trim(),
        phoneNumbersJson = JsonUtils.phonesToJson(phoneNumbers),
        address = address.trim(),
        deliveryPerson = "",
        itemsJson = JsonUtils.itemsToJson(items),
        cartage = cartage,
        totalAmount = grandTotal,
        timestamp = now,
        dateKey = JsonUtils.formatDateKey(now),
        dateFormatted = JsonUtils.formatDateDisplay(now)
      )
      val id = repository.insertTransaction(record)
      val saved = record.copy(id = id)
      _snackbarMessage.emit("Cash Sale Order recorded (#$nextSerial)")
      onSuccess(saved)
    }
  }

  fun saveOnlineSaleOrder(
    customerName: String,
    phoneNumbers: List<String>,
    address: String,
    referenceNumber: String = "",
    bankAccountId: Long?,
    bankAccountName: String,
    items: List<OrderItem>,
    cartage: Double,
    onSuccess: (TransactionRecord) -> Unit = {}
  ) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val itemsTotal = items.sumOf { it.itemTotal }
      val grandTotal = itemsTotal + cartage
      val nextSerial = repository.getNextSerialNumber()
      val record = TransactionRecord(
        type = "ONLINE_SALE",
        serialNumber = nextSerial,
        referenceNumber = referenceNumber.trim(),
        receiptMethod = "Online",
        bankAccountId = bankAccountId,
        bankAccountName = bankAccountName.trim(),
        personName = customerName.trim(),
        phoneNumbersJson = JsonUtils.phonesToJson(phoneNumbers),
        address = address.trim(),
        deliveryPerson = "",
        itemsJson = JsonUtils.itemsToJson(items),
        cartage = cartage,
        totalAmount = grandTotal,
        timestamp = now,
        dateKey = JsonUtils.formatDateKey(now),
        dateFormatted = JsonUtils.formatDateDisplay(now)
      )
      val id = repository.insertTransaction(record)
      val saved = record.copy(id = id)
      _snackbarMessage.emit("Online Sale Order recorded (#$nextSerial)")
      onSuccess(saved)
    }
  }

  fun saveCreditSaleOrder(
    creditorName: String,
    referenceNumber: String = "",
    deliveryPerson: String = "",
    items: List<OrderItem>,
    onSuccess: (TransactionRecord) -> Unit = {}
  ) {
    viewModelScope.launch {
      val cleanName = creditorName.trim()
      val matchedCreditor = creditors.value.firstOrNull {
        it.name.equals(cleanName, ignoreCase = true)
      }
      // If user typed a brand-new debtor name during billing, save it into Debtors directory automatically
      if (matchedCreditor == null && cleanName.isNotBlank()) {
        val phoneMatch = Regex("\\(([^)]+)\\)\\s*$").find(cleanName)
        val extractedPhone = phoneMatch?.groupValues?.get(1)?.trim() ?: ""
        repository.insertCreditor(Creditor(name = cleanName, phone = extractedPhone, address = ""))
      }
      val phonesList = if (!matchedCreditor?.phone.isNullOrBlank()) {
        listOf(matchedCreditor!!.phone.trim())
      } else {
        emptyList()
      }
      val now = System.currentTimeMillis()
      val nextSerial = repository.getNextSerialNumber()
      val record = TransactionRecord(
        type = "CREDIT_SALE",
        serialNumber = nextSerial,
        referenceNumber = referenceNumber.trim(),
        receiptMethod = "Credit",
        personName = cleanName,
        phoneNumbersJson = JsonUtils.phonesToJson(phonesList),
        address = matchedCreditor?.address?.trim() ?: "",
        deliveryPerson = "",
        itemsJson = JsonUtils.itemsToJson(items),
        cartage = 0.0,
        totalAmount = 0.0, // No rate / cartage in credit sale
        timestamp = now,
        dateKey = JsonUtils.formatDateKey(now),
        dateFormatted = JsonUtils.formatDateDisplay(now)
      )
      val id = repository.insertTransaction(record)
      val saved = record.copy(id = id)
      _snackbarMessage.emit("Debtors (Credit Sale) recorded (#$nextSerial)")
      onSuccess(saved)
    }
  }

  fun saveGoodsReturn(
    personName: String,
    phone: String,
    referenceNumber: String = "",
    items: List<OrderItem>,
    refundAmount: Double,
    onSuccess: (TransactionRecord) -> Unit = {}
  ) {
    viewModelScope.launch {
      val now = System.currentTimeMillis()
      val phonesJson = if (phone.isNotBlank()) JsonUtils.phonesToJson(listOf(phone)) else "[]"
      val nextSerial = repository.getNextSerialNumber()
      val record = TransactionRecord(
        type = "GOODS_RETURN",
        serialNumber = nextSerial,
        referenceNumber = referenceNumber.trim(),
        personName = personName.trim(),
        phoneNumbersJson = phonesJson,
        itemsJson = JsonUtils.itemsToJson(items),
        totalAmount = refundAmount, // refund given back
        timestamp = now,
        dateKey = JsonUtils.formatDateKey(now),
        dateFormatted = JsonUtils.formatDateDisplay(now)
      )
      val id = repository.insertTransaction(record)
      val saved = record.copy(id = id)
      _snackbarMessage.emit("Goods Return saved (#$nextSerial)")
      onSuccess(saved)
    }
  }

  fun deleteTransaction(id: Long) {
    viewModelScope.launch {
      repository.deleteTransactionById(id)
      _snackbarMessage.emit("Transaction deleted")
    }
  }

  fun deleteDayTransactions(dateKey: String) {
    viewModelScope.launch {
      repository.deleteTransactionsByDate(dateKey)
      _snackbarMessage.emit("Deleted transactions for selected day")
    }
  }

  // --- Data Entry: Bank Accounts ---

  fun saveBankAccount(id: Long = 0, bankName: String, last4: String) {
    viewModelScope.launch {
      val cleanLast4 = last4.trim().takeLast(4)
      if (id == 0L) {
        repository.insertBankAccount(BankAccount(bankName = bankName.trim(), accountNumberLast4 = cleanLast4))
        _snackbarMessage.emit("Bank account added")
      } else {
        repository.updateBankAccount(BankAccount(id = id, bankName = bankName.trim(), accountNumberLast4 = cleanLast4))
        _snackbarMessage.emit("Bank account updated")
      }
    }
  }

  fun deleteBankAccount(id: Long) {
    viewModelScope.launch {
      repository.deleteBankAccountById(id)
      _snackbarMessage.emit("Bank account removed")
    }
  }

  // --- Data Entry: Creditors (Debtors) ---

  fun saveCreditor(id: Long = 0, name: String, phone: String, address: String) {
    viewModelScope.launch {
      if (id == 0L) {
        repository.insertCreditor(Creditor(name = name.trim(), phone = phone.trim(), address = address.trim()))
        _snackbarMessage.emit("Debtor added")
      } else {
        repository.updateCreditor(Creditor(id = id, name = name.trim(), phone = phone.trim(), address = address.trim()))
        _snackbarMessage.emit("Debtor updated")
      }
    }
  }

  fun saveBulkCreditors(rawText: String) {
    viewModelScope.launch {
      val existingNames = creditors.value.map { it.name.lowercase() }.toSet()
      val newCreditors = rawText.lines()
        .map { line ->
          // Strip optional leading serial numbers like "1.", "1)", "1 -"
          line.trim().replace(Regex("^\\d+\\s*[.):-]\\s*"), "").trim()
        }
        .filter { it.isNotBlank() && it.lowercase() !in existingNames }
        .distinctBy { it.lowercase() }
        .map { name ->
          // Support optional "Name | Phone | Address" or just "Name"
          val parts = name.split("|").map { it.trim() }
          Creditor(
            name = parts.getOrElse(0) { name },
            phone = parts.getOrElse(1) { "" },
            address = parts.getOrElse(2) { "" }
          )
        }

      if (newCreditors.isNotEmpty()) {
        repository.insertAllCreditors(newCreditors)
        _snackbarMessage.emit("${newCreditors.size} Debtors added")
      } else {
        _snackbarMessage.emit("No new debtors to add")
      }
    }
  }

  fun deleteCreditor(id: Long) {
    viewModelScope.launch {
      repository.deleteCreditorById(id)
      _snackbarMessage.emit("Debtor removed")
    }
  }

  // --- Data Entry: Catalog Items ---

  companion object {
    // Matches "<SIZE> VMG <DESIGN>" or "<SIZE> <DESIGN>", e.g. "12*24 VMG 1001", "2*2 VMG STATUARIO"
    private val SIZE_VMG_DESIGN_REGEX = Regex(
      "^\\s*(\\d+(?:\\.\\d+)?\\s*[*xX×]\\s*\\d+(?:\\.\\d+)?(?:\\s*(?:mm|ft|in))?)\\s*(?:[-:•|]\\s*)?(?:(?:VMG|VMF|VM)\\b\\s*(?:[-:•|]\\s*)?)?(.+)$",
      RegexOption.IGNORE_CASE
    )
    private val VMG_SIZE_DESIGN_REGEX = Regex(
      "^\\s*(?:VMG|VMF|VM)\\b\\s*(?:[-:•|]\\s*)?(\\d+(?:\\.\\d+)?\\s*[*xX×]\\s*\\d+(?:\\.\\d+)?(?:\\s*(?:mm|ft|in))?)\\s*(?:[-:•|]\\s*)?(.+)$",
      RegexOption.IGNORE_CASE
    )
    private val LEADING_VMG_REGEX = Regex("^\\s*(?:VMG|VMF|VM)\\b\\s*(?:[-:•|]\\s*)?", RegexOption.IGNORE_CASE)

    /**
     * Parses a tile line written as "<SIZE> VMG <DESIGN NUMBER/NAME>" into separate (size, designName).
     * E.g. "12*24 VMG 5001" -> Pair("12*24", "5001")
     * E.g. "2*2 VMG ROYAL STATUARIO" -> Pair("2*2", "ROYAL STATUARIO")
     */
    fun parseTileEntry(rawDesignOrLine: String, explicitSize: String = ""): Pair<String, String> {
      val cleanedLine = rawDesignOrLine.trim()
      if (cleanedLine.isBlank()) return Pair(explicitSize.trim(), "")

      // Check if the line contains "|", e.g. "12*24 | 5001"
      if (cleanedLine.contains("|")) {
        val parts = cleanedLine.split("|", limit = 2).map { it.trim() }
        val pSize = parts[0].replace(LEADING_VMG_REGEX, "").trim()
        val pName = parts[1].replace(LEADING_VMG_REGEX, "").trim()
        return Pair(pSize.ifBlank { explicitSize.trim() }, pName)
      }

      val match1 = SIZE_VMG_DESIGN_REGEX.find(cleanedLine)
      if (match1 != null) {
        val extractedSize = match1.groupValues[1].replace(Regex("\\s*([*xX×])\\s*"), "*").trim()
        val extractedDesign = match1.groupValues[2].replace(LEADING_VMG_REGEX, "").trim()
        val finalSize = explicitSize.trim().ifBlank { extractedSize }
        return Pair(finalSize, extractedDesign)
      }

      val match2 = VMG_SIZE_DESIGN_REGEX.find(cleanedLine)
      if (match2 != null) {
        val extractedSize = match2.groupValues[1].replace(Regex("\\s*([*xX×])\\s*"), "*").trim()
        val extractedDesign = match2.groupValues[2].replace(LEADING_VMG_REGEX, "").trim()
        val finalSize = explicitSize.trim().ifBlank { extractedSize }
        return Pair(finalSize, extractedDesign)
      }

      val strippedDesign = cleanedLine.replace(LEADING_VMG_REGEX, "").trim()
      val normalizedExplicitSize = explicitSize.trim().replace(Regex("\\s*([*xX×])\\s*"), "*")
      return Pair(normalizedExplicitSize, strippedDesign.ifBlank { cleanedLine })
    }
  }

  fun saveCatalogItem(
    id: Long = 0,
    designName: String,
    size: String,
    category: String,
    defaultUnit: String,
    defaultRate: Double
  ) {
    viewModelScope.launch {
      val (finalSize, finalDesign) = if (category.equals("Tiles", ignoreCase = true)) {
        parseTileEntry(designName, size)
      } else {
        Pair(size.trim(), designName.trim())
      }

      val item = CatalogItem(
        id = id,
        designName = finalDesign,
        size = finalSize,
        category = category.trim(),
        defaultUnit = defaultUnit.trim(),
        defaultRate = defaultRate
      )
      if (id == 0L) {
        repository.insertCatalogItem(item)
        _snackbarMessage.emit("Item added to catalog")
      } else {
        repository.updateCatalogItem(item)
        _snackbarMessage.emit("Item updated")
      }
    }
  }

  fun saveBulkTiles(
    rawText: String,
    defaultSize: String = "",
    defaultUnit: String = "Box",
    defaultRate: Double = 0.0
  ) {
    viewModelScope.launch {
      var activeSizeHeader = defaultSize.trim().replace(Regex("\\s*([*xX×])\\s*"), "*")
      val sizeOnlyRegex = Regex("^\\s*(\\d+(?:\\.\\d+)?\\s*[*xX×]\\s*\\d+(?:\\.\\d+)?(?:\\s*(?:mm|ft|in))?)\\s*(?:VMG)?\\s*:?\\s*$", RegexOption.IGNORE_CASE)

      val newItems = mutableListOf<CatalogItem>()
      for (rawLine in rawText.lines()) {
        val line = rawLine.trim()
        if (line.isBlank()) continue

        // Check if the line is just a size header like "12*24" or "2*2"
        val headerMatch = sizeOnlyRegex.find(line)
        if (headerMatch != null) {
          activeSizeHeader = headerMatch.groupValues[1].replace(Regex("\\s*([*xX×])\\s*"), "*").trim()
          continue
        }

        val (parsedSize, parsedDesign) = parseTileEntry(line, activeSizeHeader)
        if (parsedDesign.isNotBlank()) {
          newItems.add(
            CatalogItem(
              designName = parsedDesign,
              size = parsedSize,
              category = "Tiles",
              defaultUnit = defaultUnit.trim().ifBlank { "Box" },
              defaultRate = defaultRate
            )
          )
        }
      }

      if (newItems.isNotEmpty()) {
        repository.insertAllCatalogItems(newItems)
        _snackbarMessage.emit("${newItems.size} Tiles added with separate Size & Design Name")
      } else {
        _snackbarMessage.emit("No valid tile lines found")
      }
    }
  }

  fun deleteCatalogItem(id: Long) {
    viewModelScope.launch {
      repository.deleteCatalogItemById(id)
      _snackbarMessage.emit("Item removed from catalog")
    }
  }
}

class WarehouseViewModelFactory(
  private val repository: WarehouseRepository
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(WarehouseViewModel::class.java)) {
      return WarehouseViewModel(repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
