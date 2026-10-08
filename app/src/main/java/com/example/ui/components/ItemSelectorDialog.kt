package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CatalogItem
import com.example.data.model.OrderItem
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
fun ItemSelectorDialog(
  catalogItems: List<CatalogItem>,
  showRate: Boolean = true, // false for Credit Sale & Goods Return
  onDismiss: () -> Unit,
  onItemAdded: (OrderItem) -> Unit
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val quantityFocusRequester = remember { FocusRequester() }

  var searchQuery by remember { mutableStateOf("") }
  var designName by remember { mutableStateOf("") }
  var size by remember { mutableStateOf("") }
  var quantityText by remember { mutableStateOf("") }
  var selectedUnit by remember { mutableStateOf("Box") }
  var rateText by remember { mutableStateOf("") }
  var isSelectingFromList by remember { mutableStateOf(true) }
  var pickedFromCatalog by remember { mutableStateOf(false) }

  // Feedback banner when adding multiple items in a row
  var addedItemsCount by remember { mutableIntStateOf(0) }
  var lastAddedSummary by remember { mutableStateOf<String?>(null) }

  var selectedCategoryFilter by remember { mutableStateOf("All") } // "All", "Tiles", "Chemical"
  var selectedSizeFilter by remember { mutableStateOf("All") }

  val availableSizes = remember(catalogItems) {
    val sizes = catalogItems
      .filter { it.category.equals("Tiles", ignoreCase = true) || it.category.isBlank() }
      .mapNotNull { it.size.trim().takeIf { s -> s.isNotEmpty() } }
      .distinct()
    listOf("All") + sizes
  }

  val filteredCatalog = remember(searchQuery, selectedCategoryFilter, selectedSizeFilter, catalogItems) {
    catalogItems.filter { item ->
      val matchesCategory = when (selectedCategoryFilter) {
        "Tiles" -> item.category.equals("Tiles", ignoreCase = true) || item.category.isBlank()
        "Chemical" -> item.category.startsWith("Chem", ignoreCase = true)
        else -> true
      }
      val matchesSize = selectedSizeFilter == "All" || item.size.equals(selectedSizeFilter, ignoreCase = true)
      val matchesSearch = if (searchQuery.isBlank()) true
      else item.designName.contains(searchQuery, ignoreCase = true) ||
          item.size.contains(searchQuery, ignoreCase = true) ||
          item.category.contains(searchQuery, ignoreCase = true)
      matchesCategory && matchesSize && matchesSearch
    }
  }

  // Auto-focus quantity input when an item is chosen from the catalog
  LaunchedEffect(isSelectingFromList, pickedFromCatalog) {
    if (!isSelectingFromList && pickedFromCatalog) {
      try {
        quantityFocusRequester.requestFocus()
        keyboardController?.show()
      } catch (_: Exception) {}
    }
  }

  fun buildCurrentOrderItem(): OrderItem? {
    val parsed = WarehouseViewModel.parseTileEntry(designName, size)
    val finalSize = parsed.first.trim()
    val finalDesign = parsed.second.trim()
    val qty = quantityText.toDoubleOrNull() ?: 0.0
    if (finalDesign.isBlank() || qty <= 0.0) return null
    val rate = if (showRate) (rateText.toDoubleOrNull() ?: 0.0) else 0.0
    val total = qty * rate
    return OrderItem(
      designName = finalDesign,
      size = finalSize,
      quantity = qty,
      unit = selectedUnit,
      rate = rate,
      itemTotal = total
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .widthIn(max = 540.dp)
        .fillMaxHeight(0.88f),
      shape = RoundedCornerShape(12.dp),
      color = Color.White,
      shadowElevation = 8.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .padding(14.dp)
      ) {
        // Classic Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = if (isSelectingFromList) "Select Item from Catalog" else "Enter Quantity & Details",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = Slate900,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
            if (addedItemsCount > 0) {
              Text(
                text = "$addedItemsCount ${if (addedItemsCount == 1) "item" else "items"} added to bill",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MintGreenIcon,
                maxLines = 1,
                softWrap = false
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600, modifier = Modifier.size(20.dp))
          }
        }

        // Last added item confirmation strip
        lastAddedSummary?.let { summary ->
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            color = MintGreenBg,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintGreenIcon, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = summary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MintGreenIcon,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isSelectingFromList) {
          // Search Input
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search design number or size (e.g. 115, 12*24)...", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate500, modifier = Modifier.size(16.dp))
                }
              }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
              keyboardController?.hide()
              focusManager.clearFocus()
            }),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Category Tabs: All | Tiles | Chemical
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
              listOf("All", "Tiles", "Chemical").forEach { cat ->
                val isSelected = selectedCategoryFilter == cat
                Surface(
                  modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                      selectedCategoryFilter = cat
                      if (cat == "Chemical") selectedSizeFilter = "All"
                    },
                  color = if (isSelected) Color.White else Color.Transparent,
                  shape = RoundedCornerShape(6.dp),
                  shadowElevation = if (isSelected) 1.dp else 0.dp
                ) {
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                      text = cat,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                      color = if (isSelected) PrimaryBlue else Slate600,
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }
              }
            }
          }

          // Size Filter Chips when viewing Tiles/All
          if (selectedCategoryFilter != "Chemical" && availableSizes.size > 1) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              availableSizes.forEach { sz ->
                val isSelected = selectedSizeFilter.equals(sz, ignoreCase = true)
                Surface(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { selectedSizeFilter = sz },
                  color = if (isSelected) PrimaryBlue else Color(0xFFF8FAFC),
                  shape = RoundedCornerShape(6.dp),
                  border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else Slate200)
                ) {
                  Text(
                    text = if (sz == "All") "All Sizes" else sz,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else Slate700,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Count & Write Manual Item action
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Catalog (${filteredCatalog.size})",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Slate600,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.weight(1f)
            )
            TextButton(
              onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                if (searchQuery.isNotBlank()) {
                  val parsed = WarehouseViewModel.parseTileEntry(searchQuery, if (selectedSizeFilter != "All") selectedSizeFilter else "")
                  size = parsed.first
                  designName = parsed.second
                } else {
                  designName = ""
                  size = if (selectedSizeFilter != "All") selectedSizeFilter else ""
                }
                pickedFromCatalog = false
                isSelectingFromList = false
              },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Write Manual Item", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Dense, Clean Commercial Catalog List
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (filteredCatalog.isEmpty()) {
              item {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("No matching catalog items", color = Slate500, fontSize = 13.sp)
                  Spacer(modifier = Modifier.height(8.dp))
                  Button(
                    onClick = {
                      keyboardController?.hide()
                      focusManager.clearFocus()
                      val parsed = WarehouseViewModel.parseTileEntry(searchQuery, if (selectedSizeFilter != "All") selectedSizeFilter else "")
                      size = parsed.first
                      designName = parsed.second
                      pickedFromCatalog = false
                      isSelectingFromList = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Text(
                      text = if (searchQuery.isNotBlank()) "Add \"$searchQuery\" Manually" else "Write Manual Item",
                      fontSize = 13.sp,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            } else {
              items(filteredCatalog, key = { it.id }) { item ->
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      designName = item.designName
                      size = item.size
                      selectedUnit = item.defaultUnit
                      quantityText = ""
                      if (showRate && item.defaultRate > 0) {
                        rateText = if (item.defaultRate % 1.0 == 0.0) item.defaultRate.toInt().toString() else item.defaultRate.toString()
                      }
                      pickedFromCatalog = true
                      isSelectingFromList = false
                    },
                  color = Color(0xFFF8FAFC),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, Slate200)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      modifier = Modifier.weight(1f).padding(end = 8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                      }

                      Text(
                        text = item.designName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate900,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                      )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (showRate && item.defaultRate > 0) {
                        val rStr = if (item.defaultRate % 1.0 == 0.0) item.defaultRate.toInt().toString() else item.defaultRate.toString()
                        Text(
                          text = "₹$rStr/${item.defaultUnit}",
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp,
                          color = PrimaryBlue,
                          maxLines = 1,
                          softWrap = false
                        )
                      } else {
                        Text(
                          text = item.defaultUnit,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Medium,
                          color = Slate500,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        } else {
          // Quantity & Details Entry Mode
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Navigation & Selected Item Banner
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(
                onClick = {
                  keyboardController?.hide()
                  focusManager.clearFocus()
                  isSelectingFromList = true
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
              ) {
                Text(
                  text = "← Back to Catalog",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = PrimaryBlue,
                  maxLines = 1,
                  softWrap = false
                )
              }

              if (size.isNotBlank()) {
                Surface(
                  color = Color(0xFFEFF6FF),
                  shape = RoundedCornerShape(4.dp),
                  border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                  Text(
                    text = "Size: $size",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
            }

            // Selected Item Display Banner (when picked from catalog)
            if (designName.isNotBlank()) {
              Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    if (size.isNotBlank()) {
                      Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Slate200)
                      ) {
                        Text(
                          text = size,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.ExtraBold,
                          color = PrimaryBlue,
                          maxLines = 1,
                          softWrap = false,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                      text = designName,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = Slate900,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }

            // Primary Quantity & Rate Entry Box
            Surface(
              color = Color(0xFFF8FAFC),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Slate200),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  // Quantity
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Quantity ($selectedUnit) *",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = Slate900,
                      maxLines = 1,
                      softWrap = false
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                      value = quantityText,
                      onValueChange = { quantityText = it },
                      placeholder = { Text("e.g. 10", fontSize = 13.sp, maxLines = 1) },
                      keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = if (showRate) ImeAction.Next else ImeAction.Done
                      ),
                      keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Right) },
                        onDone = {
                          keyboardController?.hide()
                          focusManager.clearFocus()
                        }
                      ),
                      singleLine = true,
                      modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(quantityFocusRequester),
                      shape = RoundedCornerShape(8.dp)
                    )
                  }

                  // Rate per unit (Only shown if showRate = true)
                  if (showRate) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "Rate / $selectedUnit (₹)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        softWrap = false
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        placeholder = { Text("e.g. 260", fontSize = 13.sp, maxLines = 1) },
                        keyboardOptions = KeyboardOptions(
                          keyboardType = KeyboardType.Decimal,
                          imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                          onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                          }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                      )
                    }
                  }
                }

                // Quick Quantity Chips (single horizontal non-wrapping row)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "QTY:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    maxLines = 1,
                    softWrap = false
                  )
                  listOf("1", "2", "4", "5", "10", "15", "20", "50").forEach { presetQty ->
                    val isMatched = quantityText == presetQty
                    Surface(
                      modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { quantityText = presetQty },
                      color = if (isMatched) PrimaryBlue else Color.White,
                      border = BorderStroke(1.dp, if (isMatched) PrimaryBlue else Slate200),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = presetQty,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMatched) Color.White else Slate700,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                      )
                    }
                  }
                }

                // Calculation preview if rate is shown
                if (showRate) {
                  val qty = quantityText.toDoubleOrNull() ?: 0.0
                  val rate = rateText.toDoubleOrNull() ?: 0.0
                  val itemTotal = qty * rate

                  if (qty > 0 && rate > 0) {
                    Surface(
                      color = Color(0xFFEFF6FF),
                      shape = RoundedCornerShape(6.dp),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "Item Subtotal",
                          fontSize = 12.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = Slate700,
                          maxLines = 1,
                          softWrap = false
                        )
                        Text(
                          text = "₹%.2f".format(itemTotal),
                          fontSize = 14.sp,
                          fontWeight = FontWeight.ExtraBold,
                          color = PrimaryBlue,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }
                  }
                }
              }
            }

            // Unit Selector
            Column {
              Text(
                text = "Unit",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate700,
                maxLines = 1,
                softWrap = false
              )
              Spacer(modifier = Modifier.height(4.dp))
              UnitSelectorPills(
                selectedUnit = selectedUnit,
                onUnitSelected = {
                  keyboardController?.hide()
                  focusManager.clearFocus()
                  selectedUnit = it
                }
              )
            }

            // Side-by-side Size & Design Name inputs for compact zero-scroll editing
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Column(modifier = Modifier.weight(0.38f)) {
                Text(
                  text = "Size (12*24)",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Slate700,
                  maxLines = 1,
                  softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                  value = size,
                  onValueChange = { size = it },
                  placeholder = { Text("12*24", fontSize = 13.sp, maxLines = 1) },
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                  keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp)
                )
              }

              Column(modifier = Modifier.weight(0.62f)) {
                Text(
                  text = "Design / Item Name *",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Slate700,
                  maxLines = 1,
                  softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                  value = designName,
                  onValueChange = { designName = it },
                  placeholder = { Text("e.g. 115 HL01", fontSize = 13.sp, maxLines = 1) },
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
            }
          }

          HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 8.dp))

          // Pinned Bottom Action Buttons
          val isValid = designName.isNotBlank() && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                val item = buildCurrentOrderItem() ?: return@OutlinedButton
                onItemAdded(item)
                addedItemsCount++
                val qStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                lastAddedSummary = "Added: $qStr ${item.unit} × ${item.designName}${if (item.size.isNotBlank()) " [${item.size}]" else ""}"
                quantityText = ""
                isSelectingFromList = true
              },
              enabled = isValid,
              modifier = Modifier
                .weight(1f)
                .height(44.dp),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, if (isValid) PrimaryBlue else Slate200),
              contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
              Text(
                text = "+ Add & Next",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isValid) PrimaryBlue else Slate500,
                maxLines = 1,
                softWrap = false
              )
            }

            Button(
              onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                val item = buildCurrentOrderItem() ?: return@Button
                onItemAdded(item)
                onDismiss()
              },
              enabled = isValid,
              modifier = Modifier
                .weight(1.15f)
                .height(44.dp),
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
              Text(
                text = "Add Item to Order",
                fontSize = 13.sp,
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
}
