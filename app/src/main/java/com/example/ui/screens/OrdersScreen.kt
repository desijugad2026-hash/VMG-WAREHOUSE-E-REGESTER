package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankAccount
import com.example.data.model.Creditor
import com.example.data.model.OrderItem
import com.example.data.model.TransactionRecord
import com.example.printing.EscPosReceiptBuilder
import com.example.printing.PrinterSelectionDialog
import com.example.ui.components.ItemSelectorDialog
import com.example.ui.components.SavePrintPromptDialog
import com.example.ui.components.WarehouseTopBar
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.WarehouseViewModel

@Composable
fun OrdersScreen(
  viewModel: WarehouseViewModel,
  onBack: () -> Unit,
  onNavigateToDataEntry: (Int) -> Unit = {}
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  val catalogItems by viewModel.catalogItems.collectAsStateWithLifecycle()
  val creditors by viewModel.creditors.collectAsStateWithLifecycle()
  val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()

  // 3 Buttons in Sale: 0 -> Cash Sale, 1 -> Online Sale, 2 -> Debtors (Credit Sale)
  var selectedTab by remember { mutableStateOf(0) }

  // --- Cash Sale Fields ---
  var cashCustomerName by remember { mutableStateOf("") }
  val cashPhoneNumbers = remember { mutableStateListOf("") }
  var cashAddress by remember { mutableStateOf("") }
  var cashRefNo by remember { mutableStateOf("") }
  val cashSaleItems = remember { mutableStateListOf<OrderItem>() }
  var cashCartageText by remember { mutableStateOf("") }

  // --- Online Sale Fields ---
  var onlineCustomerName by remember { mutableStateOf("") }
  val onlinePhoneNumbers = remember { mutableStateListOf("") }
  var onlineAddress by remember { mutableStateOf("") }
  var onlineRefNo by remember { mutableStateOf("") }
  var onlineSelectedBank by remember { mutableStateOf<BankAccount?>(null) }
  var onlineCustomBankName by remember { mutableStateOf("") }
  var onlineBankDropdownExpanded by remember { mutableStateOf(false) }
  val onlineSaleItems = remember { mutableStateListOf<OrderItem>() }
  var onlineCartageText by remember { mutableStateOf("") }

  // --- Debtors (Credit Sale) Fields ---
  var debtorName by remember { mutableStateOf("") }
  var debtorRefNo by remember { mutableStateOf("") }
  val debtorSaleItems = remember { mutableStateListOf<OrderItem>() }
  var showDebtorPicker by remember { mutableStateOf(false) }

  // Modal dialog states
  var showItemSelector by remember { mutableStateOf(false) }
  var savedTransactionForPrint by remember { mutableStateOf<TransactionRecord?>(null) }
  var showPrinterDialogForSaved by remember { mutableStateOf<ByteArray?>(null) }
  var printCopiesForSaved by remember { mutableStateOf(1) }
  var isSavingOrder by remember { mutableStateOf(false) }

  // Initialize at least 1 phone field
  if (cashPhoneNumbers.isEmpty()) cashPhoneNumbers.add("")
  if (onlinePhoneNumbers.isEmpty()) onlinePhoneNumbers.add("")

  // Default bank for online sale if available
  remember(bankAccounts) {
    if (onlineSelectedBank == null && bankAccounts.isNotEmpty()) {
      onlineSelectedBank = bankAccounts.first()
    }
    true
  }

  // Cash Calculations
  val cashCartageVal = cashCartageText.toDoubleOrNull() ?: 0.0
  val cashItemsTotal = cashSaleItems.sumOf { it.itemTotal }
  val cashGrandTotal = cashItemsTotal + cashCartageVal
  val isCashSaleValid = cashCustomerName.isNotBlank() && cashSaleItems.isNotEmpty()

  // Online Calculations
  val onlineCartageVal = onlineCartageText.toDoubleOrNull() ?: 0.0
  val onlineItemsTotal = onlineSaleItems.sumOf { it.itemTotal }
  val onlineGrandTotal = onlineItemsTotal + onlineCartageVal
  val isOnlineSaleValid = onlineCustomerName.isNotBlank() && onlineSaleItems.isNotEmpty()

  // Debtors Validation
  val isDebtorSaleValid = debtorName.isNotBlank() && debtorSaleItems.isNotEmpty()

  Scaffold(
    topBar = {
      WarehouseTopBar(
        title = "Sales Register",
        subtitle = when (selectedTab) {
          0 -> "1. Cash Sale Voucher"
          1 -> "2. Online Sale Voucher"
          else -> "3. Debtors (Credit Delivery)"
        },
        onBackClick = onBack
      )
    },
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
          .widthIn(max = 600.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Segmented Mode Bar: 1. Cash Sale | 2. Online Sale | 3. Debtors
        Surface(
          color = Slate100,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, Slate200),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            SaleTabButton(
              title = "1. Cash Sale",
              isSelected = selectedTab == 0,
              modifier = Modifier.weight(1f),
              testTag = "tab_cash_sale",
              onClick = { selectedTab = 0 }
            )

            SaleTabButton(
              title = "2. Online Sale",
              isSelected = selectedTab == 1,
              modifier = Modifier.weight(1f),
              testTag = "tab_online_sale",
              onClick = { selectedTab = 1 }
            )

            SaleTabButton(
              title = "3. Debtors",
              isSelected = selectedTab == 2,
              modifier = Modifier.weight(1f),
              testTag = "tab_debtors_sale",
              onClick = { selectedTab = 2 }
            )
          }
        }

        when (selectedTab) {
          0 -> {
            // ================= 1. CASH SALE FORM =================
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Slate200),
              elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Party / Customer Name
                Column {
                  Text(
                    text = "Customer / Party Name *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700,
                    maxLines = 1,
                    softWrap = false
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  OutlinedTextField(
                    value = cashCustomerName,
                    onValueChange = { cashCustomerName = it },
                    placeholder = { Text("Enter customer name", fontSize = 13.sp, maxLines = 1) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true,
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("input_cash_customer_name"),
                    shape = RoundedCornerShape(8.dp)
                  )
                }

                // Primary Mobile & Reference No side-by-side for compact layout
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Mobile No.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                        maxLines = 1,
                        softWrap = false
                      )
                      Text(
                        text = "+ Add Phone",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clickable { cashPhoneNumbers.add("") }
                      )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                      value = cashPhoneNumbers[0],
                      onValueChange = { cashPhoneNumbers[0] = it },
                      placeholder = { Text("Phone number", fontSize = 13.sp, maxLines = 1) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                      keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      shape = RoundedCornerShape(8.dp)
                    )
                  }

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Ref / Challan No.",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = Slate700,
                      maxLines = 1,
                      softWrap = false
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                      value = cashRefNo,
                      onValueChange = { cashRefNo = it },
                      placeholder = { Text("Optional Ref #", fontSize = 13.sp, maxLines = 1) },
                      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                      keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                      singleLine = true,
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_cash_ref_no"),
                      shape = RoundedCornerShape(8.dp)
                    )
                  }
                }

                // Additional phone numbers if added
                if (cashPhoneNumbers.size > 1) {
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (index in 1 until cashPhoneNumbers.size) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        OutlinedTextField(
                          value = cashPhoneNumbers[index],
                          onValueChange = { cashPhoneNumbers[index] = it },
                          placeholder = { Text("Alternate Phone #${index + 1}", fontSize = 13.sp, maxLines = 1) },
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                          singleLine = true,
                          modifier = Modifier.weight(1f),
                          shape = RoundedCornerShape(8.dp)
                        )
                        IconButton(
                          onClick = { cashPhoneNumbers.removeAt(index) },
                          modifier = Modifier.size(36.dp)
                        ) {
                          Icon(Icons.Default.Close, contentDescription = "Remove Phone", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        }
                      }
                    }
                  }
                }

                // Address
                Column {
                  Text(
                    text = "Address (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                    maxLines = 1,
                    softWrap = false
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  OutlinedTextField(
                    value = cashAddress,
                    onValueChange = { cashAddress = it },
                    placeholder = { Text("Delivery or billing address", fontSize = 13.sp, maxLines = 1) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                      keyboardController?.hide()
                      focusManager.clearFocus()
                    }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                  )
                }

                HorizontalDivider(color = Slate200)

                // Items Section
                ItemsSectionHeader(
                  title = "Items Sold (${cashSaleItems.size}) *",
                  onAddItem = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    showItemSelector = true
                  }
                )

                ItemsListContainer(
                  items = cashSaleItems,
                  showRate = true,
                  onUpdateQuantity = { index, newQty ->
                    val current = cashSaleItems[index]
                    if (newQty > 0) {
                      cashSaleItems[index] = current.copy(
                        quantity = newQty,
                        itemTotal = newQty * current.rate
                      )
                    }
                  },
                  onRemoveItem = { index -> cashSaleItems.removeAt(index) }
                )

                HorizontalDivider(color = Slate200)

                // Cartage & Total Summary
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Text(
                    text = "Cartage / Transport (₹):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                  )
                  OutlinedTextField(
                    value = cashCartageText,
                    onValueChange = { cashCartageText = it },
                    placeholder = { Text("0", fontSize = 13.sp, maxLines = 1) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                      keyboardController?.hide()
                      focusManager.clearFocus()
                    }),
                    singleLine = true,
                    modifier = Modifier.width(135.dp),
                    shape = RoundedCornerShape(8.dp)
                  )
                }

                TotalSummaryCard(
                  itemsTotal = cashItemsTotal,
                  cartage = cashCartageVal,
                  grandTotal = cashGrandTotal,
                  modeLabel = "Cash Sale"
                )

                Button(
                  onClick = {
                    if (isSavingOrder) return@Button
                    isSavingOrder = true
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.saveCashSaleOrder(
                      customerName = cashCustomerName,
                      phoneNumbers = cashPhoneNumbers.filter { it.isNotBlank() },
                      address = cashAddress,
                      referenceNumber = cashRefNo,
                      items = cashSaleItems.toList(),
                      cartage = cashCartageVal,
                      onSuccess = { savedRecord ->
                        isSavingOrder = false
                        cashCustomerName = ""
                        cashPhoneNumbers.clear()
                        cashPhoneNumbers.add("")
                        cashAddress = ""
                        cashRefNo = ""
                        cashSaleItems.clear()
                        cashCartageText = ""
                        savedTransactionForPrint = savedRecord
                      }
                    )
                  },
                  enabled = isCashSaleValid && !isSavingOrder,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_cash_sale"),
                  colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Save Cash Sale Order", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
              }
            }
          }

          1 -> {
            // ================= 2. ONLINE SALE FORM =================
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Slate200),
              elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Customer Name
                Column {
                  Text(
                    text = "Customer / Party Name *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700,
                    maxLines = 1,
                    softWrap = false
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  OutlinedTextField(
                    value = onlineCustomerName,
                    onValueChange = { onlineCustomerName = it },
                    placeholder = { Text("Enter customer name", fontSize = 13.sp, maxLines = 1) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true,
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("input_online_customer_name"),
                    shape = RoundedCornerShape(8.dp)
                  )
                }

                // Primary Mobile & Reference No side-by-side
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Mobile No.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                        maxLines = 1,
                        softWrap = false
                      )
                      Text(
                        text = "+ Add Phone",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clickable { onlinePhoneNumbers.add("") }
                      )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                      value = onlinePhoneNumbers[0],
                      onValueChange = { onlinePhoneNumbers[0] = it },
                      placeholder = { Text("Phone number", fontSize = 13.sp, maxLines = 1) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                      keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      shape = RoundedCornerShape(8.dp)
                    )
                  }

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "UTR / Ref No.",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = Slate700,
                      maxLines = 1,
                      softWrap = false
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                      value = onlineRefNo,
                      onValueChange = { onlineRefNo = it },
                      placeholder = { Text("Optional Ref #", fontSize = 13.sp, maxLines = 1) },
                      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                      keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                      singleLine = true,
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_online_ref_no"),
                      shape = RoundedCornerShape(8.dp)
                    )
                  }
                }

                // Additional phone numbers if added
                if (onlinePhoneNumbers.size > 1) {
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (index in 1 until onlinePhoneNumbers.size) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        OutlinedTextField(
                          value = onlinePhoneNumbers[index],
                          onValueChange = { onlinePhoneNumbers[index] = it },
                          placeholder = { Text("Alternate Phone #${index + 1}", fontSize = 13.sp, maxLines = 1) },
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                          singleLine = true,
                          modifier = Modifier.weight(1f),
                          shape = RoundedCornerShape(8.dp)
                        )
                        IconButton(
                          onClick = { onlinePhoneNumbers.removeAt(index) },
                          modifier = Modifier.size(36.dp)
                        ) {
                          Icon(Icons.Default.Close, contentDescription = "Remove Phone", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        }
                      }
                    }
                  }
                }

                // Address
                Column {
                  Text(
                    text = "Address (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                    maxLines = 1,
                    softWrap = false
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  OutlinedTextField(
                    value = onlineAddress,
                    onValueChange = { onlineAddress = it },
                    placeholder = { Text("Delivery or billing address", fontSize = 13.sp, maxLines = 1) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                      keyboardController?.hide()
                      focusManager.clearFocus()
                    }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                  )
                }

                // Bank Account selection for Online Sale
                Column {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Receiving Bank Account",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = Slate700,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f)
                    )
                    TextButton(
                      onClick = { onNavigateToDataEntry(3) },
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                      modifier = Modifier.height(26.dp)
                    ) {
                      Text(
                        text = "+ Manage Bank A/C",
                        fontSize = 11.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))

                  if (bankAccounts.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                      Surface(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(46.dp)
                          .clip(RoundedCornerShape(8.dp))
                          .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                          .clickable { onlineBankDropdownExpanded = true },
                        color = Color(0xFFF8FAFC)
                      ) {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                          ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                              text = onlineSelectedBank?.displayName ?: (if (onlineCustomBankName.isNotBlank()) onlineCustomBankName else "Select Receiving Bank Account"),
                              fontSize = 13.sp,
                              fontWeight = FontWeight.SemiBold,
                              color = Slate900,
                              maxLines = 1,
                              softWrap = false,
                              overflow = TextOverflow.Ellipsis
                            )
                          }
                          Spacer(modifier = Modifier.width(6.dp))
                          Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Slate600)
                        }
                      }

                      DropdownMenu(
                        expanded = onlineBankDropdownExpanded,
                        onDismissRequest = { onlineBankDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                      ) {
                        bankAccounts.forEach { bank ->
                          DropdownMenuItem(
                            text = {
                              Text("${bank.bankName} (•••• ${bank.accountNumberLast4})", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            },
                            onClick = {
                              onlineSelectedBank = bank
                              onlineCustomBankName = bank.displayName
                              onlineBankDropdownExpanded = false
                            }
                          )
                        }
                      }
                    }
                  } else {
                    OutlinedTextField(
                      value = onlineCustomBankName,
                      onValueChange = { onlineCustomBankName = it },
                      placeholder = { Text("e.g. HDFC Bank / UPI / QR", fontSize = 13.sp, maxLines = 1) },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth(),
                      shape = RoundedCornerShape(8.dp)
                    )
                  }
                }

                HorizontalDivider(color = Slate200)

                // Items Section
                ItemsSectionHeader(
                  title = "Items Sold (${onlineSaleItems.size}) *",
                  onAddItem = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    showItemSelector = true
                  }
                )

                ItemsListContainer(
                  items = onlineSaleItems,
                  showRate = true,
                  onUpdateQuantity = { index, newQty ->
                    val current = onlineSaleItems[index]
                    if (newQty > 0) {
                      onlineSaleItems[index] = current.copy(
                        quantity = newQty,
                        itemTotal = newQty * current.rate
                      )
                    }
                  },
                  onRemoveItem = { index -> onlineSaleItems.removeAt(index) }
                )

                HorizontalDivider(color = Slate200)

                // Cartage & Total Summary
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Text(
                    text = "Cartage / Transport (₹):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                  )
                  OutlinedTextField(
                    value = onlineCartageText,
                    onValueChange = { onlineCartageText = it },
                    placeholder = { Text("0", fontSize = 13.sp, maxLines = 1) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                      keyboardController?.hide()
                      focusManager.clearFocus()
                    }),
                    singleLine = true,
                    modifier = Modifier.width(135.dp),
                    shape = RoundedCornerShape(8.dp)
                  )
                }

                TotalSummaryCard(
                  itemsTotal = onlineItemsTotal,
                  cartage = onlineCartageVal,
                  grandTotal = onlineGrandTotal,
                  modeLabel = "Online Sale"
                )

                Button(
                  onClick = {
                    if (isSavingOrder) return@Button
                    isSavingOrder = true
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val bankName = onlineSelectedBank?.displayName ?: onlineCustomBankName
                    viewModel.saveOnlineSaleOrder(
                      customerName = onlineCustomerName,
                      phoneNumbers = onlinePhoneNumbers.filter { it.isNotBlank() },
                      address = onlineAddress,
                      referenceNumber = onlineRefNo,
                      bankAccountId = onlineSelectedBank?.id,
                      bankAccountName = bankName,
                      items = onlineSaleItems.toList(),
                      cartage = onlineCartageVal,
                      onSuccess = { savedRecord ->
                        isSavingOrder = false
                        onlineCustomerName = ""
                        onlinePhoneNumbers.clear()
                        onlinePhoneNumbers.add("")
                        onlineAddress = ""
                        onlineRefNo = ""
                        onlineSaleItems.clear()
                        onlineCartageText = ""
                        savedTransactionForPrint = savedRecord
                      }
                    )
                  },
                  enabled = isOnlineSaleValid && !isSavingOrder,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_online_sale"),
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Save Online Sale Order", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
              }
            }
          }

          2 -> {
            // ================= 3. DEBTORS (CREDIT SALE) FORM =================
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Slate200),
              elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Debtor Name with popup picker + quick-pick chips
                Column {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Debtor / Party Name *",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = Slate700,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f)
                    )
                    TextButton(
                      onClick = { onNavigateToDataEntry(2) },
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                      modifier = Modifier.height(26.dp)
                    ) {
                      Text(
                        text = "+ Manage Debtors",
                        fontSize = 11.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))

                  Surface(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(46.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .border(1.dp, if (debtorName.isNotBlank()) PrimaryBlue else Slate200, RoundedCornerShape(8.dp))
                      .clickable {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        showDebtorPicker = true
                      }
                      .testTag("btn_select_debtor"),
                    color = if (debtorName.isNotBlank()) Color(0xFFF8FAFC) else Color.White
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                      ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = debtorName.ifBlank { "Tap to Select or Type Debtor Name" },
                          fontSize = 14.sp,
                          fontWeight = if (debtorName.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                          color = if (debtorName.isNotBlank()) Slate900 else Slate500,
                          maxLines = 1,
                          softWrap = false,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Icon(Icons.Default.Search, contentDescription = "Search", tint = Slate600, modifier = Modifier.size(18.dp))
                    }
                  }

                  // 1-Tap Quick Debtor Chips right below selector for rapid billing
                  if (creditors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      creditors.take(15).forEach { creditor ->
                        val isSelected = debtorName.equals(creditor.name, ignoreCase = true)
                        Surface(
                          modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { debtorName = creditor.name },
                          color = if (isSelected) PrimaryBlue else Color(0xFFF8FAFC),
                          border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else Slate200),
                          shape = RoundedCornerShape(6.dp)
                        ) {
                          Text(
                            text = creditor.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Slate700,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                          )
                        }
                      }
                    }
                  }
                }

                // Reference Number
                Column {
                  Text(
                    text = "Ref / Challan No. (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                    maxLines = 1,
                    softWrap = false
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  OutlinedTextField(
                    value = debtorRefNo,
                    onValueChange = { debtorRefNo = it },
                    placeholder = { Text("e.g. Challan No / Book Ref No", fontSize = 13.sp, maxLines = 1) },
                    singleLine = true,
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("input_debtor_ref_no"),
                    shape = RoundedCornerShape(8.dp)
                  )
                }

                HorizontalDivider(color = Slate200)

                // Items Delivered Section (NO RATE, NO CARTAGE!)
                ItemsSectionHeader(
                  title = "Items Delivered (${debtorSaleItems.size}) *",
                  subtitle = "Credit delivery (Qty & Unit only)",
                  onAddItem = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    showItemSelector = true
                  }
                )

                ItemsListContainer(
                  items = debtorSaleItems,
                  showRate = false,
                  onUpdateQuantity = { index, newQty ->
                    val current = debtorSaleItems[index]
                    if (newQty > 0) {
                      debtorSaleItems[index] = current.copy(quantity = newQty)
                    }
                  },
                  onRemoveItem = { index -> debtorSaleItems.removeAt(index) }
                )

                Button(
                  onClick = {
                    if (isSavingOrder) return@Button
                    isSavingOrder = true
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.saveCreditSaleOrder(
                      creditorName = debtorName,
                      referenceNumber = debtorRefNo,
                      deliveryPerson = "",
                      items = debtorSaleItems.toList(),
                      onSuccess = { savedRecord ->
                        isSavingOrder = false
                        debtorName = ""
                        debtorRefNo = ""
                        debtorSaleItems.clear()
                        savedTransactionForPrint = savedRecord
                      }
                    )
                  },
                  enabled = isDebtorSaleValid && !isSavingOrder,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_debtor_sale"),
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Save Debtors (Credit Sale)", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
              }
            }
          }
        }
      }
    }
  }

  // Item Selector Dialog
  if (showItemSelector) {
    ItemSelectorDialog(
      catalogItems = catalogItems,
      showRate = (selectedTab == 0 || selectedTab == 1),
      onDismiss = { showItemSelector = false },
      onItemAdded = { item ->
        when (selectedTab) {
          0 -> cashSaleItems.add(item)
          1 -> onlineSaleItems.add(item)
          2 -> debtorSaleItems.add(item)
        }
      }
    )
  }

  // Debtor / Creditor Picker Dialog
  if (showDebtorPicker) {
    DebtorPickerDialog(
      creditors = creditors,
      currentName = debtorName,
      onSelectName = { name ->
        debtorName = name
        showDebtorPicker = false
      },
      onDismiss = { showDebtorPicker = false }
    )
  }

  // Save & Print Prompt Dialog
  savedTransactionForPrint?.let { savedTx ->
    val serialNum = if (savedTx.serialNumber > 0L) savedTx.serialNumber else savedTx.id.coerceAtLeast(1L)
    SavePrintPromptDialog(
      transaction = savedTx,
      serialNumber = serialNum,
      onCancel = {
        savedTransactionForPrint = null
        onBack()
      },
      onPrint = { selectedCopies ->
        printCopiesForSaved = selectedCopies
        val escPos = EscPosReceiptBuilder.generateEscPos(savedTx, serialNum, 1)
        showPrinterDialogForSaved = escPos
        savedTransactionForPrint = null
      }
    )
  }

  // Thermal Printer Selection Dialog for newly saved order
  showPrinterDialogForSaved?.let { escPosBytes ->
    PrinterSelectionDialog(
      escPosData = escPosBytes,
      copies = printCopiesForSaved,
      onDismiss = {
        showPrinterDialogForSaved = null
        onBack()
      },
      onPrintSuccess = {
        showPrinterDialogForSaved = null
        onBack()
      }
    )
  }
}

@Composable
private fun SaleTabButton(
  title: String,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  testTag: String,
  onClick: () -> Unit
) {
  Surface(
    modifier = modifier
      .height(38.dp)
      .clip(RoundedCornerShape(6.dp))
      .clickable { onClick() }
      .testTag(testTag),
    color = if (isSelected) Color.White else Color.Transparent,
    shape = RoundedCornerShape(6.dp),
    shadowElevation = if (isSelected) 1.dp else 0.dp
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 4.dp)
    ) {
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
        color = if (isSelected) PrimaryBlue else Slate600,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ItemsSectionHeader(
  title: String,
  subtitle: String? = null,
  onAddItem: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
      Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Slate900,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          fontSize = 11.sp,
          color = Slate500,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    Button(
      onClick = onAddItem,
      colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
      shape = RoundedCornerShape(6.dp),
      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
      modifier = Modifier.height(34.dp)
    ) {
      Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text("+ Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
  }
}

@Composable
private fun ItemsListContainer(
  items: List<OrderItem>,
  showRate: Boolean,
  onUpdateQuantity: (Int, Double) -> Unit = { _, _ -> },
  onRemoveItem: (Int) -> Unit
) {
  if (items.isEmpty()) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(8.dp),
      color = Color(0xFFF8FAFC),
      border = BorderStroke(1.dp, Slate200)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "No items added yet. Tap '+ Add Item' to select.",
          color = Slate500,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  } else {
    // Classic Structured Commercial Invoice Table
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(8.dp),
      color = Color.White,
      border = BorderStroke(1.dp, Slate200)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Table Header Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "ITEM / SIZE & DESIGN",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Slate600,
            maxLines = 1,
            softWrap = false
          )
          Text(
            text = if (showRate) "QTY & AMOUNT" else "QTY / UNIT",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Slate600,
            maxLines = 1,
            softWrap = false
          )
        }
        HorizontalDivider(color = Slate200)

        items.forEachIndexed { index, item ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp)
          ) {
            // Line 1: Size Badge + Full-Width Design Name + Delete Button
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.weight(1f).padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${index + 1}.",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate500,
                  maxLines = 1,
                  softWrap = false
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (item.size.isNotBlank()) {
                  Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                  ) {
                    Text(
                      text = item.size,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = PrimaryBlue,
                      maxLines = 1,
                      softWrap = false,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                  text = item.designName,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = Slate900,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }

              IconButton(
                onClick = { onRemoveItem(index) },
                modifier = Modifier.size(26.dp)
              ) {
                Icon(
                  Icons.Default.Delete,
                  contentDescription = "Remove Item",
                  tint = Color(0xFFDC2626),
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Line 2: Rate / Subtotal on Left | Compact [- Qty +] Stepper on Right
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (showRate) {
                val rateStr = if (item.rate % 1.0 == 0.0) item.rate.toInt().toString() else "%.2f".format(item.rate)
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                  Text(
                    text = "Rate: ₹$rateStr/${item.unit}",
                    fontSize = 12.sp,
                    color = Slate600,
                    maxLines = 1,
                    softWrap = false
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "₹%.2f".format(item.itemTotal),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryBlue,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              } else {
                Text(
                  text = "Delivered: $qtyStr ${item.unit}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Slate600,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.weight(1f)
                )
              }

              // Compact Inline Stepper: [-] 10 Box [+]
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Surface(
                  modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp)),
                  color = Color(0xFFF8FAFC),
                  border = BorderStroke(1.dp, Slate200),
                  onClick = {
                    if (item.quantity > 1.0) {
                      val next = if (item.quantity % 1.0 == 0.0) item.quantity - 1.0 else (item.quantity - 1.0).coerceAtLeast(0.5)
                      onUpdateQuantity(index, next)
                    }
                  }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Qty", tint = Slate700, modifier = Modifier.size(13.dp))
                  }
                }

                Text(
                  text = "$qtyStr ${item.unit}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate900,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 4.dp)
                )

                Surface(
                  modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp)),
                  color = Color(0xFFF8FAFC),
                  border = BorderStroke(1.dp, Slate200),
                  onClick = {
                    onUpdateQuantity(index, item.quantity + 1.0)
                  }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Qty", tint = Slate700, modifier = Modifier.size(13.dp))
                  }
                }
              }
            }
          }

          if (index < items.size - 1) {
            HorizontalDivider(color = Slate200)
          }
        }
      }
    }
  }
}

@Composable
private fun TotalSummaryCard(
  itemsTotal: Double,
  cartage: Double,
  grandTotal: Double,
  modeLabel: String
) {
  Surface(
    color = Color(0xFFF8FAFC),
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, Slate200),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Items Subtotal", fontSize = 12.sp, color = Slate600, maxLines = 1, softWrap = false)
        Text("₹%.2f".format(itemsTotal), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900, maxLines = 1, softWrap = false)
      }
      if (cartage > 0.0) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Cartage / Transport", fontSize = 12.sp, color = Slate600, maxLines = 1, softWrap = false)
          Text("₹%.2f".format(cartage), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900, maxLines = 1, softWrap = false)
        }
      }
      HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 2.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "GRAND TOTAL ($modeLabel)",
          fontSize = 13.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Slate900,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Text(
          text = "₹%.2f".format(grandTotal),
          fontSize = 17.sp,
          fontWeight = FontWeight.Black,
          color = PrimaryBlue,
          maxLines = 1,
          softWrap = false
        )
      }
    }
  }
}

@Composable
fun DebtorPickerDialog(
  creditors: List<Creditor>,
  currentName: String,
  onSelectName: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf(currentName) }

  val filteredList = remember(searchQuery, creditors) {
    if (searchQuery.isBlank()) creditors
    else creditors.filter {
      it.name.contains(searchQuery, ignoreCase = true) ||
          it.address.contains(searchQuery, ignoreCase = true)
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 500.dp)
        .heightIn(max = 520.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Select Debtor / Party",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Slate900,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(1f)
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search or type new debtor name...", fontSize = 13.sp, maxLines = 1) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (searchQuery.isNotBlank() && creditors.none { it.name.equals(searchQuery.trim(), ignoreCase = true) }) {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectName(searchQuery.trim()) }
              .padding(vertical = 2.dp),
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Use name: \"${searchQuery.trim()}\"",
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(filteredList, key = { it.id }) { creditor ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSelectName(creditor.name) },
              color = Color(0xFFF8FAFC),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Slate200)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = creditor.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  if (creditor.address.isNotBlank() || creditor.phone.isNotBlank()) {
                    Text(
                      text = listOfNotNull(creditor.phone.takeIf { it.isNotBlank() }, creditor.address.takeIf { it.isNotBlank() }).joinToString(" • "),
                      fontSize = 11.sp,
                      color = Slate600,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
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
