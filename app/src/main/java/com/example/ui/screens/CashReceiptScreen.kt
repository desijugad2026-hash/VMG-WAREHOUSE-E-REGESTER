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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankAccount
import com.example.data.model.TransactionRecord
import com.example.printing.EscPosReceiptBuilder
import com.example.printing.PrinterSelectionDialog
import com.example.ui.components.SavePrintPromptDialog
import com.example.ui.components.WarehouseTopBar
import com.example.ui.theme.MintGreenBg
import com.example.ui.theme.MintGreenIcon
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.WarehouseViewModel

@Composable
fun CashReceiptScreen(
  viewModel: WarehouseViewModel,
  onBack: () -> Unit,
  onNavigateToDataEntry: () -> Unit
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
  val creditors by viewModel.creditors.collectAsStateWithLifecycle()

  var isOnlineMethod by remember { mutableStateOf(false) }
  var personName by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var refNo by remember { mutableStateOf("") }
  var selectedBank by remember { mutableStateOf<BankAccount?>(null) }
  var bankDropdownExpanded by remember { mutableStateOf(false) }
  var savedTransactionForPrint by remember { mutableStateOf<TransactionRecord?>(null) }
  var showPrinterDialogForSaved by remember { mutableStateOf<ByteArray?>(null) }
  var printCopiesForSaved by remember { mutableStateOf(1) }
  var isSaving by remember { mutableStateOf(false) }

  // Set default bank if available
  remember(bankAccounts) {
    if (selectedBank == null && bankAccounts.isNotEmpty()) {
      selectedBank = bankAccounts.first()
    }
    true
  }

  val amountVal = amountText.toDoubleOrNull() ?: 0.0
  val isFormValid = personName.isNotBlank() &&
      amountVal > 0.0 &&
      (!isOnlineMethod || selectedBank != null)

  Scaffold(
    topBar = {
      WarehouseTopBar(
        title = "Receipt Voucher",
        subtitle = "Record incoming payment (Cash & Online)",
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
      // 2 Options Selector: 1. Cash | 2. Online
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
          // Cash Option
          Surface(
            modifier = Modifier
              .weight(1f)
              .height(38.dp)
              .clip(RoundedCornerShape(6.dp))
              .clickable { isOnlineMethod = false }
              .testTag("tab_receipt_cash"),
            color = if (!isOnlineMethod) Color.White else Color.Transparent,
            shape = RoundedCornerShape(6.dp),
            shadowElevation = if (!isOnlineMethod) 1.dp else 0.dp
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.padding(horizontal = 6.dp)
            ) {
              Icon(
                Icons.Default.Payments,
                contentDescription = null,
                tint = if (!isOnlineMethod) MintGreenIcon else Slate500,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "1. Cash",
                fontSize = 13.sp,
                fontWeight = if (!isOnlineMethod) FontWeight.Bold else FontWeight.SemiBold,
                color = if (!isOnlineMethod) Slate900 else Slate600,
                maxLines = 1,
                softWrap = false
              )
            }
          }

          // Online Option
          Surface(
            modifier = Modifier
              .weight(1f)
              .height(38.dp)
              .clip(RoundedCornerShape(6.dp))
              .clickable { isOnlineMethod = true }
              .testTag("tab_receipt_online"),
            color = if (isOnlineMethod) Color.White else Color.Transparent,
            shape = RoundedCornerShape(6.dp),
            shadowElevation = if (isOnlineMethod) 1.dp else 0.dp
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.padding(horizontal = 6.dp)
            ) {
              Icon(
                Icons.Default.AccountBalance,
                contentDescription = null,
                tint = if (isOnlineMethod) PrimaryBlue else Slate500,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "2. Online",
                fontSize = 13.sp,
                fontWeight = if (isOnlineMethod) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isOnlineMethod) Slate900 else Slate600,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      // Input Form Container
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
          // Person Name
          Column {
            Text(
              text = "Received From (Party / Person Name) *",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Slate700,
              maxLines = 1,
              softWrap = false
            )
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
                  val isSelected = personName.equals(creditor.name, ignoreCase = true)
                  Surface(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .clickable {
                        personName = creditor.name
                        if (creditor.phone.isNotBlank() && phoneNumber.isBlank()) {
                          phoneNumber = creditor.phone
                        }
                      },
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
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = personName,
              onValueChange = { personName = it },
              placeholder = { Text("Enter party or person name", fontSize = 13.sp, maxLines = 1) },
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
              keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_receipt_name"),
              shape = RoundedCornerShape(8.dp)
            )
          }

          // Amount
          Column {
            Text(
              text = "Amount Received (₹) *",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Slate700,
              maxLines = 1,
              softWrap = false
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = amountText,
              onValueChange = { amountText = it },
              placeholder = { Text("e.g. 15000", fontSize = 13.sp, maxLines = 1) },
              leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate700, modifier = Modifier.padding(start = 10.dp)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
              keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_receipt_amount"),
              shape = RoundedCornerShape(8.dp)
            )
          }

          // Mobile Number & Reference Number side-by-side
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Mobile No. (Optional)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate700,
                maxLines = 1,
                softWrap = false
              )
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                placeholder = { Text("Phone number", fontSize = 13.sp, maxLines = 1) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_receipt_phone"),
                shape = RoundedCornerShape(8.dp)
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isOnlineMethod) "UTR / Ref No." else "Receipt / Ref No.",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate700,
                maxLines = 1,
                softWrap = false
              )
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = refNo,
                onValueChange = { refNo = it },
                placeholder = { Text("Optional Ref #", fontSize = 13.sp, maxLines = 1) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                  keyboardController?.hide()
                  focusManager.clearFocus()
                }),
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_receipt_ref_no"),
                shape = RoundedCornerShape(8.dp)
              )
            }
          }

          // Bank Account Selection (Only for Online Option)
          if (isOnlineMethod) {
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Receiving Bank Account *",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate700,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                  text = "+ Manage Bank A/C",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = PrimaryBlue,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigateToDataEntry() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))

              if (bankAccounts.isEmpty()) {
                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFFFEF3C7),
                  border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "No bank accounts added yet",
                      fontSize = 12.sp,
                      color = Color(0xFF92400E),
                      maxLines = 1,
                      softWrap = false,
                      modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    OutlinedButton(
                      onClick = onNavigateToDataEntry,
                      shape = RoundedCornerShape(6.dp),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF92400E)),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.height(32.dp)
                    ) {
                      Text("+ Add Bank", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                  }
                }
              } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                  Surface(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(46.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                      .clickable { bankDropdownExpanded = true }
                      .testTag("dropdown_bank_select"),
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
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                      ) {
                        Icon(
                          Icons.Default.AccountBalance,
                          contentDescription = null,
                          tint = PrimaryBlue,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = selectedBank?.displayName ?: "Choose Bank Account",
                          fontSize = 13.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = if (selectedBank != null) Slate900 else Slate500,
                          maxLines = 1,
                          softWrap = false,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                      Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = Slate600
                      )
                    }
                  }

                  DropdownMenu(
                    expanded = bankDropdownExpanded,
                    onDismissRequest = { bankDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                  ) {
                    bankAccounts.forEach { bank ->
                      DropdownMenuItem(
                        text = {
                          Column {
                            Text(bank.bankName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            if (bank.accountNumberLast4.isNotBlank()) {
                              Text("A/C: •••• ${bank.accountNumberLast4}", fontSize = 11.sp, color = Slate500)
                            }
                          }
                        },
                        onClick = {
                          selectedBank = bank
                          bankDropdownExpanded = false
                        }
                      )
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Save Button
          Button(
            onClick = {
              if (isSaving) return@Button
              isSaving = true
              keyboardController?.hide()
              focusManager.clearFocus()
              val method = if (isOnlineMethod) "Online" else "Cash"
              viewModel.saveCashReceipt(
                method = method,
                personName = personName,
                amount = amountVal,
                phone = phoneNumber,
                bankAccountId = if (isOnlineMethod) selectedBank?.id else null,
                bankAccountName = if (isOnlineMethod) (selectedBank?.displayName ?: "") else "",
                referenceNumber = refNo,
                onSuccess = { savedRecord ->
                  isSaving = false
                  personName = ""
                  amountText = ""
                  phoneNumber = ""
                  refNo = ""
                  savedTransactionForPrint = savedRecord
                }
              )
            },
            enabled = isFormValid && !isSaving,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("btn_save_receipt"),
            colors = ButtonDefaults.buttonColors(
              containerColor = MintGreenIcon
            ),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isSaving) "Saving..." else "Save Receipt",
              fontSize = 15.sp,
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

  // Save & Print Prompt Dialog (2 options: Cancel and Print, plus Copies selector)
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

  // Thermal Printer Selection Dialog for newly saved receipt
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
