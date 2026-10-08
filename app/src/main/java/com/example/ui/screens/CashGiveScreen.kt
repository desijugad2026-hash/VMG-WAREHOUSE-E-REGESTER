package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionRecord
import com.example.printing.EscPosReceiptBuilder
import com.example.printing.PrinterSelectionDialog
import com.example.ui.components.SavePrintPromptDialog
import com.example.ui.components.WarehouseTopBar
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.SoftRoseIcon
import com.example.ui.viewmodel.WarehouseViewModel

@Composable
fun CashGiveScreen(
  viewModel: WarehouseViewModel,
  onBack: () -> Unit
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val creditors by viewModel.creditors.collectAsStateWithLifecycle()

  var personName by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var refNo by remember { mutableStateOf("") }
  var savedTransactionForPrint by remember { mutableStateOf<TransactionRecord?>(null) }
  var showPrinterDialogForSaved by remember { mutableStateOf<ByteArray?>(null) }
  var printCopiesForSaved by remember { mutableStateOf(1) }
  var isSaving by remember { mutableStateOf(false) }

  val amountVal = amountText.toDoubleOrNull() ?: 0.0
  val isFormValid = personName.isNotBlank() && amountVal > 0.0

  Scaffold(
    topBar = {
      WarehouseTopBar(
        title = "Payment Voucher",
        subtitle = "Record outgoing cash payment",
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
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Name
          Column {
            Text(
              text = "Paid To (Party / Person Name) *",
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryBlue else Slate200),
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
              placeholder = { Text("Enter payee or vendor name", fontSize = 13.sp, maxLines = 1) },
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
              keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_give_name"),
              shape = RoundedCornerShape(8.dp)
            )
          }

          // Amount
          Column {
            Text(
              text = "Amount Paid (₹) *",
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
              placeholder = { Text("e.g. 5000", fontSize = 13.sp, maxLines = 1) },
              leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate700, modifier = Modifier.padding(start = 10.dp)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
              keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_give_amount"),
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
                  .testTag("input_give_phone"),
                shape = RoundedCornerShape(8.dp)
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Voucher / Ref No.",
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
                  .testTag("input_give_ref_no"),
                shape = RoundedCornerShape(8.dp)
              )
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
              viewModel.saveCashGive(
                personName = personName,
                phone = phoneNumber,
                amount = amountVal,
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
              .testTag("btn_save_give"),
            colors = ButtonDefaults.buttonColors(
              containerColor = SoftRoseIcon
            ),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isSaving) "Saving..." else "Save Payment",
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

  // Thermal Printer Selection Dialog for newly saved payment
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
