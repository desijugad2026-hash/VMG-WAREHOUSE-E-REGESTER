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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.model.CatalogItem
import com.example.data.model.Creditor
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.UnitSelectorPills
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
fun DataEntryScreen(
  viewModel: WarehouseViewModel,
  initialTab: Int = 0,
  onBack: () -> Unit
) {
  val catalogItems by viewModel.catalogItems.collectAsStateWithLifecycle()
  val creditors by viewModel.creditors.collectAsStateWithLifecycle()
  val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()

  // 4 Tabs in Data Entry:
  // 0 -> Tiles, 1 -> Chemical, 2 -> Debtors, 3 -> Bank Accounts
  var selectedTab by remember { mutableStateOf(initialTab.coerceIn(0, 3)) }

  // Search & Filter states
  var searchQuery by remember { mutableStateOf("") }
  var selectedTileSizeFilter by remember { mutableStateOf("All") }

  // Catalog Item Dialog States (Tiles & Chemical)
  var showItemDialog by remember { mutableStateOf(false) }
  var showBulkTilesDialog by remember { mutableStateOf(false) }
  var editingItem by remember { mutableStateOf<CatalogItem?>(null) }
  var itemToDelete by remember { mutableStateOf<CatalogItem?>(null) }

  // Debtor (Creditor) Dialog States
  var showDebtorDialog by remember { mutableStateOf(false) }
  var showBulkDebtorDialog by remember { mutableStateOf(false) }
  var editingDebtor by remember { mutableStateOf<Creditor?>(null) }
  var debtorToDelete by remember { mutableStateOf<Creditor?>(null) }

  // Bank Account Dialog States
  var showBankDialog by remember { mutableStateOf(false) }
  var editingBank by remember { mutableStateOf<BankAccount?>(null) }
  var bankToDelete by remember { mutableStateOf<BankAccount?>(null) }

  // Filtered Lists
  val allTilesList = remember(catalogItems) {
    catalogItems.filter { it.category.equals("Tiles", ignoreCase = true) || it.category.isBlank() }
  }
  val availableTileSizes = remember(allTilesList) {
    val sizes = allTilesList.mapNotNull { it.size.trim().takeIf { s -> s.isNotEmpty() } }.distinct()
    listOf("All") + sizes
  }
  val filteredTilesList = remember(allTilesList, searchQuery, selectedTileSizeFilter) {
    allTilesList.filter { item ->
      val matchesSize = selectedTileSizeFilter == "All" || item.size.equals(selectedTileSizeFilter, ignoreCase = true)
      val matchesSearch = searchQuery.isBlank() ||
          item.designName.contains(searchQuery, ignoreCase = true) ||
          item.size.contains(searchQuery, ignoreCase = true)
      matchesSize && matchesSearch
    }
  }

  val allChemicalList = remember(catalogItems) {
    catalogItems.filter { it.category.startsWith("Chem", ignoreCase = true) }
  }
  val filteredChemicalList = remember(allChemicalList, searchQuery) {
    if (searchQuery.isBlank()) allChemicalList
    else allChemicalList.filter {
      it.designName.contains(searchQuery, ignoreCase = true) ||
          it.size.contains(searchQuery, ignoreCase = true)
    }
  }

  val filteredDebtorsList = remember(creditors, searchQuery) {
    if (searchQuery.isBlank()) creditors
    else creditors.filter {
      it.name.contains(searchQuery, ignoreCase = true) ||
          it.phone.contains(searchQuery, ignoreCase = true) ||
          it.address.contains(searchQuery, ignoreCase = true)
    }
  }

  val filteredBankList = remember(bankAccounts, searchQuery) {
    if (searchQuery.isBlank()) bankAccounts
    else bankAccounts.filter {
      it.bankName.contains(searchQuery, ignoreCase = true) ||
          it.accountNumberLast4.contains(searchQuery, ignoreCase = true)
    }
  }

  // --- Confirm Delete Dialogs ---
  itemToDelete?.let { item ->
    ConfirmDialog(
      title = "Delete ${if (item.category.startsWith("Chem", true)) "Chemical" else "Tile"}?",
      message = "Are you sure you want to delete \"${item.designName}\" (${item.size.ifBlank { "No size" }})?",
      onConfirm = {
        viewModel.deleteCatalogItem(item.id)
        itemToDelete = null
      },
      onDismiss = { itemToDelete = null }
    )
  }

  debtorToDelete?.let { debtor ->
    ConfirmDialog(
      title = "Delete Debtor?",
      message = "Are you sure you want to remove \"${debtor.name}\" from Debtors?",
      onConfirm = {
        viewModel.deleteCreditor(debtor.id)
        debtorToDelete = null
      },
      onDismiss = { debtorToDelete = null }
    )
  }

  bankToDelete?.let { bank ->
    ConfirmDialog(
      title = "Delete Bank Account?",
      message = "Are you sure you want to remove \"${bank.displayName}\"?",
      onConfirm = {
        viewModel.deleteBankAccount(bank.id)
        bankToDelete = null
      },
      onDismiss = { bankToDelete = null }
    )
  }

  // --- Add / Edit Dialogs ---
  if (showItemDialog) {
    ItemEntryDialog(
      initialItem = editingItem,
      category = if (selectedTab == 1) "Chemical" else "Tiles",
      existingSizes = availableTileSizes.filter { it != "All" },
      onDismiss = {
        showItemDialog = false
        editingItem = null
      },
      onSave = { design, size, unit, rate ->
        viewModel.saveCatalogItem(
          id = editingItem?.id ?: 0L,
          designName = design,
          size = size,
          category = if (selectedTab == 1) "Chemical" else "Tiles",
          defaultUnit = unit,
          defaultRate = rate
        )
        showItemDialog = false
        editingItem = null
      },
      onSaveAndNext = { design, size, unit, rate ->
        viewModel.saveCatalogItem(
          id = 0L,
          designName = design,
          size = size,
          category = if (selectedTab == 1) "Chemical" else "Tiles",
          defaultUnit = unit,
          defaultRate = rate
        )
      }
    )
  }

  if (showBulkTilesDialog) {
    BulkTilesEntryDialog(
      onDismiss = { showBulkTilesDialog = false },
      onSaveBulk = { rawLines, defaultSize, unit, rate ->
        viewModel.saveBulkTiles(
          rawText = rawLines,
          defaultSize = defaultSize,
          defaultUnit = unit,
          defaultRate = rate
        )
        showBulkTilesDialog = false
      }
    )
  }

  if (showDebtorDialog) {
    DebtorEntryDialog(
      initialDebtor = editingDebtor,
      onDismiss = {
        showDebtorDialog = false
        editingDebtor = null
      },
      onSave = { name, phone, address ->
        viewModel.saveCreditor(
          id = editingDebtor?.id ?: 0L,
          name = name,
          phone = phone,
          address = address
        )
        showDebtorDialog = false
        editingDebtor = null
      },
      onSaveAndNext = { name, phone, address ->
        viewModel.saveCreditor(
          id = 0L,
          name = name,
          phone = phone,
          address = address
        )
      }
    )
  }

  if (showBulkDebtorDialog) {
    BulkDebtorEntryDialog(
      onDismiss = { showBulkDebtorDialog = false },
      onSaveBulk = { rawLines ->
        viewModel.saveBulkCreditors(rawLines)
        showBulkDebtorDialog = false
      }
    )
  }

  if (showBankDialog) {
    BankAccountEntryDialog(
      initialBank = editingBank,
      onDismiss = {
        showBankDialog = false
        editingBank = null
      },
      onSave = { bankName, last4 ->
        viewModel.saveBankAccount(
          id = editingBank?.id ?: 0L,
          bankName = bankName,
          last4 = last4
        )
        showBankDialog = false
        editingBank = null
      }
    )
  }

  Scaffold(
    topBar = {
      WarehouseTopBar(
        title = "Master Data & Catalog",
        subtitle = "Tiles • Chemical • Debtors • Bank Accounts",
        onBackClick = onBack
      )
    },
    containerColor = Color(0xFFF8FAFC)
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      // Classic 4-Tab Segmented Bar: Tiles, Chemical, Debtors, Bank A/C
      Surface(
        color = Color(0xFFE2E8F0),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp),
          horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          DataEntryPill(
            title = "Tiles",
            count = allTilesList.size,
            isSelected = selectedTab == 0,
            onClick = {
              selectedTab = 0
              searchQuery = ""
            },
            modifier = Modifier
              .weight(1f)
              .testTag("tab_data_tiles")
          )

          DataEntryPill(
            title = "Chemical",
            count = allChemicalList.size,
            isSelected = selectedTab == 1,
            onClick = {
              selectedTab = 1
              searchQuery = ""
            },
            modifier = Modifier
              .weight(1f)
              .testTag("tab_data_chemical")
          )

          DataEntryPill(
            title = "Debtors",
            count = creditors.size,
            isSelected = selectedTab == 2,
            onClick = {
              selectedTab = 2
              searchQuery = ""
            },
            modifier = Modifier
              .weight(1f)
              .testTag("tab_data_debtors")
          )

          DataEntryPill(
            title = "Banks",
            count = bankAccounts.size,
            isSelected = selectedTab == 3,
            onClick = {
              selectedTab = 3
              searchQuery = ""
            },
            modifier = Modifier
              .weight(1f)
              .testTag("tab_data_banks")
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Search Bar for current tab
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = {
          Text(
            when (selectedTab) {
              0 -> "Search tile size (e.g. 12*24, 2*2) or design name..."
              1 -> "Search chemical name or packing..."
              2 -> "Search debtor name, phone or address..."
              else -> "Search bank name or last 4 digits..."
            },
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate500, modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_data_entry_search"),
        shape = RoundedCornerShape(8.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      when (selectedTab) {
        0 -> {
          // ==================== TAB 0: TILES ====================
          // Size Filter Chips (All, 12*24, 2*2, etc.)
          if (availableTileSizes.size > 1) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              availableTileSizes.forEach { sizeChip ->
                val isSelected = selectedTileSizeFilter.equals(sizeChip, ignoreCase = true)
                Surface(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { selectedTileSizeFilter = sizeChip },
                  color = if (isSelected) PrimaryBlue else Color.White,
                  shape = RoundedCornerShape(6.dp),
                  border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else Color(0xFFCBD5E1))
                ) {
                  Text(
                    text = if (sizeChip == "All") "All Sizes (${allTilesList.size})" else sizeChip,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Slate700,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
          }

          // Header + Action Buttons (+ Add Tile & Bulk Add)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "TILES CATALOG (${filteredTilesList.size})",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Slate700,
              letterSpacing = 0.4.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 8.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              OutlinedButton(
                onClick = { showBulkTilesDialog = true },
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                  .height(34.dp)
                  .testTag("btn_bulk_add_tiles")
              ) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(15.dp), tint = Slate700)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bulk Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700, maxLines = 1, softWrap = false)
              }

              Button(
                onClick = {
                  editingItem = null
                  showItemDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                  .height(34.dp)
                  .testTag("btn_add_data_entry_item")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("+ Add Tile", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (filteredTilesList.isEmpty()) {
            EmptyStateBox(
              message = "No tiles in catalog yet.\nTap '+ Add Tile' or 'Bulk Add' to add Tile Size (e.g. 12*24, 2*2) and Design Name."
            )
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(filteredTilesList, key = { it.id }) { item ->
                TileCatalogCard(
                  item = item,
                  onEdit = {
                    editingItem = item
                    showItemDialog = true
                  },
                  onDelete = { itemToDelete = item }
                )
              }
            }
          }
        }

        1 -> {
          // ==================== TAB 1: CHEMICAL ====================
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "CHEMICAL CATALOG (${filteredChemicalList.size})",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Slate700,
              letterSpacing = 0.4.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 8.dp)
            )

            Button(
              onClick = {
                editingItem = null
                showItemDialog = true
              },
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
              modifier = Modifier
                .height(34.dp)
                .testTag("btn_add_chemical_item")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("+ Add Chemical", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (filteredChemicalList.isEmpty()) {
            EmptyStateBox(
              message = "No chemicals added yet.\nTap '+ Add Chemical' to add adhesives, grout, or epoxy."
            )
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(filteredChemicalList, key = { it.id }) { item ->
                TileCatalogCard(
                  item = item,
                  isChemical = true,
                  onEdit = {
                    editingItem = item
                    showItemDialog = true
                  },
                  onDelete = { itemToDelete = item }
                )
              }
            }
          }
        }

        2 -> {
          // ==================== TAB 2: DEBTORS ====================
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "DEBTORS DIRECTORY (${filteredDebtorsList.size})",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Slate700,
              letterSpacing = 0.4.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 8.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              OutlinedButton(
                onClick = { showBulkDebtorDialog = true },
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                  .height(34.dp)
                  .testTag("btn_bulk_add_debtors")
              ) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(15.dp), tint = Slate700)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bulk Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700, maxLines = 1, softWrap = false)
              }

              Button(
                onClick = {
                  editingDebtor = null
                  showDebtorDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                  .height(34.dp)
                  .testTag("btn_add_debtor")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("+ Add Debtor", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (filteredDebtorsList.isEmpty()) {
            EmptyStateBox(
              message = "No debtors added yet.\nTap '+ Add Debtor' or 'Bulk Add' to add Party names for Credit Sales."
            )
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(filteredDebtorsList, key = { it.id }) { debtor ->
                Surface(
                  color = Color.White,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      editingDebtor = debtor
                      showDebtorDialog = true
                    }
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      modifier = Modifier.weight(1f).padding(end = 8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.size(34.dp)
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Slate700,
                            modifier = Modifier.size(18.dp)
                          )
                        }
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = debtor.name,
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.sp,
                          color = Slate900,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        if (debtor.phone.isNotBlank() || debtor.address.isNotBlank()) {
                          Text(
                            text = listOfNotNull(
                              debtor.phone.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
                              debtor.address.takeIf { it.isNotBlank() }
                            ).joinToString(" • "),
                            fontSize = 11.sp,
                            color = Slate600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                      }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                      IconButton(
                        onClick = {
                          editingDebtor = debtor
                          showDebtorDialog = true
                        },
                        modifier = Modifier.size(30.dp)
                      ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Debtor", tint = Slate600, modifier = Modifier.size(17.dp))
                      }
                      IconButton(
                        onClick = { debtorToDelete = debtor },
                        modifier = Modifier.size(30.dp)
                      ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Debtor", tint = Color(0xFFDC2626), modifier = Modifier.size(17.dp))
                      }
                    }
                  }
                }
              }
            }
          }
        }

        3 -> {
          // ==================== TAB 3: BANK ACCOUNTS ====================
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "BANK ACCOUNTS (${filteredBankList.size})",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Slate700,
              letterSpacing = 0.4.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).padding(end = 8.dp)
            )

            Button(
              onClick = {
                editingBank = null
                showBankDialog = true
              },
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
              modifier = Modifier
                .height(34.dp)
                .testTag("btn_add_bank_account")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("+ Add Bank A/C", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (filteredBankList.isEmpty()) {
            EmptyStateBox(
              message = "No bank accounts added yet.\nTap '+ Add Bank A/C' to add accounts for Online Sales and Online Receipts."
            )
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(filteredBankList, key = { it.id }) { bank ->
                Surface(
                  color = Color.White,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      editingBank = bank
                      showBankDialog = true
                    }
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      modifier = Modifier.weight(1f).padding(end = 8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.size(34.dp)
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                          )
                        }
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = bank.bankName,
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.sp,
                          color = Slate900,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = if (bank.accountNumberLast4.isNotBlank()) "A/C Ending: •••• ${bank.accountNumberLast4}" else "UPI / Bank Account",
                          fontSize = 11.sp,
                          color = Slate600,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                      IconButton(
                        onClick = {
                          editingBank = bank
                          showBankDialog = true
                        },
                        modifier = Modifier.size(30.dp)
                      ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Bank", tint = Slate600, modifier = Modifier.size(17.dp))
                      }
                      IconButton(
                        onClick = { bankToDelete = bank },
                        modifier = Modifier.size(30.dp)
                      ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Bank", tint = Color(0xFFDC2626), modifier = Modifier.size(17.dp))
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
  }
}

@Composable
private fun TileCatalogCard(
  item: CatalogItem,
  isChemical: Boolean = false,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    color = Color.White,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable { onEdit() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 9.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f).padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Crisp Size Badge on the Left
        Surface(
          color = if (isChemical) Color(0xFFF0FDF4) else Color(0xFFEFF6FF),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, if (isChemical) Color(0xFFBBF7D0) else Color(0xFFBFDBFE))
        ) {
          Text(
            text = item.size.ifBlank { "STD" },
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isChemical) Color(0xFF15803D) else PrimaryBlue,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Design Number / Name + Unit & Rate
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.designName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Slate900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Unit: ${item.defaultUnit}", fontSize = 11.sp, color = Slate600, maxLines = 1, softWrap = false)
            if (item.defaultRate > 0) {
              val rateFormatted = if (item.defaultRate % 1.0 == 0.0) item.defaultRate.toInt().toString() else item.defaultRate.toString()
              Text(
                text = "• ₹$rateFormatted/${item.defaultUnit}",
                fontSize = 11.sp,
                color = PrimaryBlue,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
              )
            }
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
          Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate600, modifier = Modifier.size(17.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(17.dp))
        }
      }
    }
  }
}

@Composable
private fun EmptyStateBox(message: String) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 16.dp),
    shape = RoundedCornerShape(8.dp),
    color = Color.White,
    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = message,
        color = Slate600,
        fontSize = 12.sp,
        lineHeight = 18.sp
      )
    }
  }
}

@Composable
private fun DataEntryPill(
  title: String,
  count: Int,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .height(38.dp)
      .clip(RoundedCornerShape(6.dp))
      .clickable { onClick() },
    color = if (isSelected) PrimaryBlue else Color.Transparent,
    shape = RoundedCornerShape(6.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 4.dp)
    ) {
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
        color = if (isSelected) Color.White else Slate700,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = "($count)",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Slate500,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

@Composable
private fun ItemEntryDialog(
  initialItem: CatalogItem?,
  category: String, // "Tiles" or "Chemical"
  existingSizes: List<String> = emptyList(),
  onDismiss: () -> Unit,
  onSave: (designName: String, size: String, unit: String, rate: Double) -> Unit,
  onSaveAndNext: ((designName: String, size: String, unit: String, rate: Double) -> Unit)? = null
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  var size by remember { mutableStateOf(initialItem?.size ?: "") }
  var designName by remember { mutableStateOf(initialItem?.designName ?: "") }
  var selectedUnit by remember { mutableStateOf(initialItem?.defaultUnit ?: if (category == "Tiles") "Box" else "Kg") }
  var rateText by remember {
    mutableStateOf(
      if (initialItem != null && initialItem.defaultRate > 0) {
        if (initialItem.defaultRate % 1.0 == 0.0) initialItem.defaultRate.toInt().toString() else initialItem.defaultRate.toString()
      } else ""
    )
  }
  var savedCounter by remember { mutableStateOf(0) }

  val commonSizes = remember(existingSizes) {
    (listOf("12*24", "2*2", "2*4", "12*18", "16*16", "1*1", "1*2") + existingSizes).distinct()
  }

  // Live preview of smart split if user types/pastes "12*24 VMG 1001" into Design Name
  val parsedPreview = remember(designName, size, category) {
    if (category == "Tiles") {
      WarehouseViewModel.parseTileEntry(designName, size)
    } else {
      Pair(size.trim(), designName.trim())
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (initialItem == null) "Add New $category" else "Edit $category",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Slate900
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
          }
        }

        // SECTION 1: SIZE (First section as requested: 12*24, 2*2, etc.)
        Column {
          Text(
            text = if (category == "Tiles") "1. Tile Size Section (e.g. 12*24, 2*2) *" else "1. Packing Size / Weight (e.g. 20 Kg, 5 Kg)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700
          )
          if (category == "Tiles") {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              commonSizes.forEach { presetSize ->
                val isSelected = size.equals(presetSize, ignoreCase = true)
                Surface(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { size = presetSize },
                  color = if (isSelected) PrimaryBlue else Slate100,
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = presetSize,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Slate700,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = size,
            onValueChange = { size = it },
            placeholder = {
              Text(if (category == "Tiles") "Type or tap size above (e.g. 12*24, 2*2)" else "e.g. 20 Kg / 5 Kg / 1 Ltr")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_tile_size"),
            shape = RoundedCornerShape(10.dp)
          )
        }

        // SECTION 2: DESIGN NUMBER / NAME (Separate section, strips VMG shop name automatically)
        Column {
          Text(
            text = if (category == "Tiles") "2. Design Number / Name Section *" else "2. Chemical / Product Name *",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700
          )
          if (category == "Tiles") {
            Text(
              text = "Shop name 'VMG' is automatically excluded so only the Design No./Name is saved here.",
              fontSize = 11.sp,
              color = Slate500
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = designName,
            onValueChange = { designName = it },
            placeholder = {
              Text(if (category == "Tiles") "e.g. 1001 / Statuario / HL-204" else "e.g. Tile Adhesive / Epoxy Grout")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_tile_design_name"),
            shape = RoundedCornerShape(10.dp)
          )

          // Show smart split preview if user typed "12*24 VMG 1001"
          if (category == "Tiles" && designName.isNotBlank() &&
            (parsedPreview.first != size.trim() || parsedPreview.second != designName.trim())
          ) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              color = Color(0xFFEFF6FF),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Will save as -> Size: ${parsedPreview.first.ifBlank { "N/A" }}  |  Design: ${parsedPreview.second}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = PrimaryBlue
                )
              }
            }
          }
        }

        // Unit selector (Box, Kg, Pcs, Bag)
        Column {
          Text("Unit (Box, Kg, Pcs, Bag) *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          Spacer(modifier = Modifier.height(6.dp))
          UnitSelectorPills(
            selectedUnit = selectedUnit,
            onUnitSelected = {
              keyboardController?.hide()
              focusManager.clearFocus()
              selectedUnit = it
            }
          )
        }

        // Default Rate
        Column {
          Text("Default Rate / $selectedUnit (₹ Optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = rateText,
            onValueChange = { rateText = it },
            placeholder = { Text("Optional (leave blank if rate varies)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
              keyboardController?.hide()
              focusManager.clearFocus()
            }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (savedCounter > 0) {
          Surface(
            color = Color(0xFFDCFCE7),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "✓ Saved ($savedCounter added in this session). Enter next design below.",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF15803D),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (initialItem == null && onSaveAndNext != null) {
            OutlinedButton(
              onClick = {
                val rate = rateText.toDoubleOrNull() ?: 0.0
                onSaveAndNext(parsedPreview.second, parsedPreview.first, selectedUnit, rate)
                size = parsedPreview.first // keep size selected for rapid same-size tile entry!
                designName = ""
                savedCounter += 1
              },
              enabled = parsedPreview.second.isNotBlank(),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("btn_save_and_next_catalog_item"),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, PrimaryBlue),
              contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
              Text(
                text = "Save & Next",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = PrimaryBlue,
                maxLines = 1,
                softWrap = false
              )
            }
          }

          Button(
            onClick = {
              keyboardController?.hide()
              focusManager.clearFocus()
              val rate = rateText.toDoubleOrNull() ?: 0.0
              onSave(parsedPreview.second, parsedPreview.first, selectedUnit, rate)
            },
            enabled = parsedPreview.second.isNotBlank(),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("btn_save_catalog_item"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
          ) {
            Text("Save $category", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

@Composable
private fun BulkTilesEntryDialog(
  onDismiss: () -> Unit,
  onSaveBulk: (rawLines: String, defaultSize: String, unit: String, rate: Double) -> Unit
) {
  var defaultSize by remember { mutableStateOf("") }
  var rawText by remember { mutableStateOf("") }
  var selectedUnit by remember { mutableStateOf("Box") }
  val presetSizes = listOf("12*24", "2*2", "2*4", "12*18", "16*16", "1*1", "1*2")

  // Live preview of parsed lines
  val previewItems = remember(rawText, defaultSize) {
    var activeSize = defaultSize.trim()
    val sizeOnlyRegex = Regex("^\\s*(\\d+(?:\\.\\d+)?\\s*[*xX×]\\s*\\d+(?:\\.\\d+)?(?:\\s*(?:mm|ft|in))?)\\s*(?:VMG)?\\s*:?\\s*$", RegexOption.IGNORE_CASE)
    val result = mutableListOf<Pair<String, String>>()
    for (line in rawText.lines()) {
      val trimmed = line.trim()
      if (trimmed.isBlank()) continue
      val headerMatch = sizeOnlyRegex.find(trimmed)
      if (headerMatch != null) {
        activeSize = headerMatch.groupValues[1].replace(Regex("\\s*([*xX×])\\s*"), "*").trim()
        continue
      }
      val parsed = WarehouseViewModel.parseTileEntry(trimmed, activeSize)
      if (parsed.second.isNotBlank()) {
        result.add(parsed)
      }
    }
    result
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Bulk Add Tiles",
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              color = Slate900
            )
            Text(
              text = "Automatically splits <Size> VMG <Design> into Size & Design sections",
              fontSize = 11.sp,
              color = Slate500
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
          }
        }

        // Optional Default Size if pasting only design numbers
        Column {
          Text("Default Size (Optional - if not written on each line)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            presetSizes.forEach { sz ->
              val isSelected = defaultSize.equals(sz, ignoreCase = true)
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { defaultSize = if (isSelected) "" else sz },
                color = if (isSelected) PrimaryBlue else Slate100,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = sz,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) Color.White else Slate700,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }
        }

        Column {
          Text(
            text = "Paste or Type Tile Lines (One per line) *",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700
          )
          Text(
            text = "Example:\n12*24 VMG 1001\n12*24 VMG 1002\n2*2 VMG STATUARIO",
            fontSize = 11.sp,
            color = Slate500
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            placeholder = { Text("12*24 VMG 1001\n2*2 VMG ALFA WHITE\n...") },
            minLines = 5,
            maxLines = 8,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_bulk_tiles"),
            shape = RoundedCornerShape(10.dp)
          )
        }

        if (previewItems.isNotEmpty()) {
          Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "Preview (${previewItems.size} tiles ready - 'VMG' removed):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
              )
              Spacer(modifier = Modifier.height(4.dp))
              previewItems.take(5).forEach { (sz, dName) ->
                Text(
                  text = "• Size: [${sz.ifBlank { "N/A" }}]   |   Design: $dName",
                  fontSize = 11.sp,
                  color = Slate700
                )
              }
              if (previewItems.size > 5) {
                Text(
                  text = "...and ${previewItems.size - 5} more",
                  fontSize = 11.sp,
                  color = Slate500
                )
              }
            }
          }
        }

        Button(
          onClick = { onSaveBulk(rawText, defaultSize, selectedUnit, 0.0) },
          enabled = previewItems.isNotEmpty(),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Save ${previewItems.size} Tiles", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun DebtorEntryDialog(
  initialDebtor: Creditor?,
  onDismiss: () -> Unit,
  onSave: (name: String, phone: String, address: String) -> Unit,
  onSaveAndNext: ((name: String, phone: String, address: String) -> Unit)? = null
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  var name by remember { mutableStateOf(initialDebtor?.name ?: "") }
  var phone by remember { mutableStateOf(initialDebtor?.phone ?: "") }
  var address by remember { mutableStateOf(initialDebtor?.address ?: "") }
  var savedCounter by remember { mutableStateOf(0) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (initialDebtor == null) "Add Debtor / Party" else "Edit Debtor / Party",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Slate900
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
          }
        }

        Column {
          Text("Debtor / Shop Owner Name *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("e.g. Sharma Traders / Gupta Hardware") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Slate500) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_debtor_name"),
            shape = RoundedCornerShape(10.dp)
          )
        }

        Column {
          Text("Phone Number (Optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            placeholder = { Text("e.g. 9876543210") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Slate500) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_debtor_phone"),
            shape = RoundedCornerShape(10.dp)
          )
        }

        Column {
          Text("Address / Area (Optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            placeholder = { Text("e.g. Main Market") },
            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Slate500) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
              keyboardController?.hide()
              focusManager.clearFocus()
            }),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_debtor_address"),
            shape = RoundedCornerShape(10.dp)
          )
        }

        if (savedCounter > 0) {
          Surface(
            color = Color(0xFFDCFCE7),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "✓ Saved ($savedCounter added). Enter next Debtor below.",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF15803D),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (initialDebtor == null && onSaveAndNext != null) {
            OutlinedButton(
              onClick = {
                onSaveAndNext(name.trim(), phone.trim(), address.trim())
                name = ""
                phone = ""
                address = ""
                savedCounter += 1
              },
              enabled = name.isNotBlank(),
              modifier = Modifier
                .weight(1f)
                .height(48.dp),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, PrimaryBlue),
              contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
              Text(
                text = "Save & Next",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = PrimaryBlue,
                maxLines = 1,
                softWrap = false
              )
            }
          }

          Button(
            onClick = {
              keyboardController?.hide()
              focusManager.clearFocus()
              onSave(name.trim(), phone.trim(), address.trim())
            },
            enabled = name.isNotBlank(),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("btn_save_debtor"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
          ) {
            Text("Save Debtor", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

@Composable
private fun BulkDebtorEntryDialog(
  onDismiss: () -> Unit,
  onSaveBulk: (rawLines: String) -> Unit
) {
  var rawText by remember { mutableStateOf("") }
  val count = remember(rawText) {
    rawText.lines().count { it.trim().isNotBlank() }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Bulk Add Debtors",
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              color = Slate900
            )
            Text(
              text = "Enter multiple debtor / party names (one per line)",
              fontSize = 11.sp,
              color = Slate500
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
          }
        }

        OutlinedTextField(
          value = rawText,
          onValueChange = { rawText = it },
          placeholder = { Text("Sharma Tiles\nR.K. Enterprises\nVerma Builders\n...") },
          minLines = 6,
          maxLines = 10,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_bulk_debtors"),
          shape = RoundedCornerShape(10.dp)
        )

        Button(
          onClick = { onSaveBulk(rawText) },
          enabled = count > 0,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Save $count Debtors", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun BankAccountEntryDialog(
  initialBank: BankAccount?,
  onDismiss: () -> Unit,
  onSave: (bankName: String, last4: String) -> Unit
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  var bankName by remember { mutableStateOf(initialBank?.bankName ?: "") }
  var last4 by remember { mutableStateOf(initialBank?.accountNumberLast4 ?: "") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (initialBank == null) "Add Bank Account" else "Edit Bank Account",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Slate900
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate600)
          }
        }

        Column {
          Text("Bank Name / Account Holder *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = bankName,
            onValueChange = { bankName = it },
            placeholder = { Text("e.g. HDFC Bank / SBI / ICICI UPI") },
            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Slate500) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_bank_name"),
            shape = RoundedCornerShape(10.dp)
          )
        }

        Column {
          Text("Account Number Last 4 Digits (Optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = last4,
            onValueChange = { if (it.length <= 4) last4 = it.filter { ch -> ch.isDigit() } },
            placeholder = { Text("e.g. 4821") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
              keyboardController?.hide()
              focusManager.clearFocus()
            }),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_bank_last4"),
            shape = RoundedCornerShape(10.dp)
          )
        }

        Button(
          onClick = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onSave(bankName.trim(), last4.trim())
          },
          enabled = bankName.isNotBlank(),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_save_bank"),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Save Bank Account", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
