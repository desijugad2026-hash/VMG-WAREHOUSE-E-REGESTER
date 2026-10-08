package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.theme.SoftTealIcon
import com.example.ui.viewmodel.WarehouseViewModel

@Composable
fun GoodsReturnScreen(
  viewModel: WarehouseViewModel,
  onBack: () -> Unit
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  val catalogItems by viewModel.catalogItems.collectAsStateWithLifecycle()
  val creditors by viewModel.creditors.collectAsStateWithLifecycle()

  var personName by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var refNo by remember { mutableStateOf("") }
  val returnedItems = remember { mutableStateListOf<OrderItem>() }
  var refundAmountText by remember { mutableStateOf("") }
  var showItemSelector by remember { mutableStateOf(false) }
  var savedTransactionForPrint by remember { mutableStateOf<TransactionRecord?>(null) }
  var showPrinterDialogForSaved by remember { mutableStateOf<ByteArray?>(null) }
  var printCopiesForSaved by remember { mutableStateOf(1) }
  var isSaving by remember { mutableStateOf(false) }

  val isFormValid = personName.isNotBlank() && returnedItems.isNotEmpty()

  Scaffold(
    topBar = {
      WarehouseTopBar(
        title = "Goods Return (G.R)",
        subtitle = "Record returned items and refund",
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
              text = "Returned By (Party / Customer Name) *",
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
              placeholder = { Text("Enter party or customer name", fontSize = 13.sp, maxLines = 1) },
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
              keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_gr_name"),
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
                  .testTag("input_gr_phone"),
                shape = RoundedCornerShape(8.dp)
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Ref / Bill No.",
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
                  .testTag("input_gr_ref_no"),
                shape = RoundedCornerShape(8.dp)
              )
            }
          }

          HorizontalDivider(color = Slate200)

          // Items Returned section
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Returned Items (${returnedItems.size}) *",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Slate900,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Button(
              onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                showItemSelector = true
              },
              colors = ButtonDefaults.buttonColors(containerColor = SoftTealIcon),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.height(34.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("+ Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
          }

          if (returnedItems.isEmpty()) {
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
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "No returned items added yet. Tap '+ Add Item' to select.",
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
            // Structured 2-Line Commercial Table for Returned Items
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
              color = Color.White,
              border = BorderStroke(1.dp, Slate200)
            ) {
              Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "RETURNED ITEM / SIZE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate600,
                    maxLines = 1,
                    softWrap = false
                  )
                  Text(
                    text = "QTY / UNIT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate600,
                    maxLines = 1,
                    softWrap = false
                  )
                }
                HorizontalDivider(color = Slate200)

                returnedItems.forEachIndexed { index, item ->
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 10.dp, vertical = 8.dp)
                  ) {
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
                        onClick = { returnedItems.removeAt(index) },
                        modifier = Modifier.size(26.dp)
                      ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove Item", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                      }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Returned: $qtyStr ${item.unit}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f)
                      )

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
                              returnedItems[index] = item.copy(quantity = (item.quantity - 1.0).coerceAtLeast(1.0))
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
                            returnedItems[index] = item.copy(quantity = item.quantity + 1.0)
                          }
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Qty", tint = SoftTealIcon, modifier = Modifier.size(13.dp))
                          }
                        }
                      }
                    }
                  }

                  if (index < returnedItems.size - 1) {
                    HorizontalDivider(color = Slate200)
                  }
                }
              }
            }
          }

          HorizontalDivider(color = Slate200)

          // Refund Amount Row
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Refund Amount Given Back (₹)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate700,
                maxLines = 1,
                softWrap = false
              )
              Text(
                text = "Optional (0 if credit note)",
                fontSize = 11.sp,
                color = Slate500,
                maxLines = 1,
                softWrap = false
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = refundAmountText,
              onValueChange = { refundAmountText = it },
              placeholder = { Text("0.00", fontSize = 13.sp, maxLines = 1) },
              leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate600, modifier = Modifier.padding(start = 10.dp)) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
              }),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_gr_refund"),
              shape = RoundedCornerShape(8.dp)
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Save Button
          Button(
            onClick = {
              if (isSaving) return@Button
              isSaving = true
              keyboardController?.hide()
              focusManager.clearFocus()
              val refundVal = refundAmountText.toDoubleOrNull() ?: 0.0
              viewModel.saveGoodsReturn(
                personName = personName,
                phone = phoneNumber,
                referenceNumber = refNo,
                items = returnedItems.toList(),
                refundAmount = refundVal,
                onSuccess = { savedRecord ->
                  isSaving = false
                  personName = ""
                  phoneNumber = ""
                  refNo = ""
                  returnedItems.clear()
                  refundAmountText = ""
                  savedTransactionForPrint = savedRecord
                }
              )
            },
            enabled = isFormValid && !isSaving,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("btn_save_goods_return"),
            colors = ButtonDefaults.buttonColors(containerColor = SoftTealIcon),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isSaving) "Saving..." else "Save Goods Return",
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

  // Thermal Printer Selection Dialog for newly saved goods return
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

  if (showItemSelector) {
    ItemSelectorDialog(
      catalogItems = catalogItems,
      showRate = false,
      onDismiss = { showItemSelector = false },
      onItemAdded = { item ->
        returnedItems.add(item)
      }
    )
  }
}
