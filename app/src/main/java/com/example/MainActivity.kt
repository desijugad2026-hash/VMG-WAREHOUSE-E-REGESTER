package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.WarehouseDatabase
import com.example.data.repository.WarehouseRepository
import com.example.ui.screens.AppDestination
import com.example.ui.screens.CashGiveScreen
import com.example.ui.screens.CashReceiptScreen
import com.example.ui.screens.DataEntryScreen
import com.example.ui.screens.GoodsReturnScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.theme.WarehouseRegisterTheme
import com.example.ui.viewmodel.WarehouseViewModel
import com.example.ui.viewmodel.WarehouseViewModelFactory
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Database & Repository
    val database = WarehouseDatabase.getDatabase(applicationContext)
    val repository = WarehouseRepository(
      transactionDao = database.transactionDao(),
      bankAccountDao = database.bankAccountDao(),
      creditorDao = database.creditorDao(),
      catalogItemDao = database.catalogItemDao()
    )
    val viewModelFactory = WarehouseViewModelFactory(repository)

    setContent {
      WarehouseRegisterTheme {
        val viewModel: WarehouseViewModel = viewModel(factory = viewModelFactory)
        val snackbarHostState = remember { SnackbarHostState() }

        // Observe snackbar notifications
        LaunchedEffect(Unit) {
          viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
          }
        }

        // Navigation BackStack
        val backStack = remember { mutableStateListOf(AppDestination.HOME) }
        var dataEntryInitialTab by remember { mutableIntStateOf(0) }
        val currentDestination = backStack.lastOrNull() ?: AppDestination.HOME

        fun navigateTo(destination: AppDestination, tab: Int = 0) {
          if (destination == AppDestination.DATA_ENTRY) {
            dataEntryInitialTab = tab
          }
          backStack.add(destination)
        }

        fun navigateBack() {
          if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
          }
        }

        // Hardware / Gesture back button
        BackHandler(enabled = backStack.size > 1) {
          navigateBack()
        }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
          snackbarHost = {
            SnackbarHost(
              hostState = snackbarHostState,
              modifier = Modifier.navigationBarsPadding()
            )
          }
        ) { _ ->
          androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize()
          ) {
            when (currentDestination) {
              AppDestination.HOME -> {
                HomeScreen(
                  viewModel = viewModel,
                  onNavigate = { dest -> navigateTo(dest, 0) }
                )
              }
              AppDestination.CASH_RECEIPTS -> {
                CashReceiptScreen(
                  viewModel = viewModel,
                  onBack = { navigateBack() },
                  onNavigateToDataEntry = { navigateTo(AppDestination.DATA_ENTRY, 3) }
                )
              }
              AppDestination.CASH_GIVE -> {
                CashGiveScreen(
                  viewModel = viewModel,
                  onBack = { navigateBack() }
                )
              }
              AppDestination.ORDERS -> {
                OrdersScreen(
                  viewModel = viewModel,
                  onBack = { navigateBack() },
                  onNavigateToDataEntry = { tab -> navigateTo(AppDestination.DATA_ENTRY, tab) }
                )
              }
              AppDestination.GOODS_RETURN -> {
                GoodsReturnScreen(
                  viewModel = viewModel,
                  onBack = { navigateBack() }
                )
              }
              AppDestination.HISTORY -> {
                HistoryScreen(
                  viewModel = viewModel,
                  onBack = { navigateBack() }
                )
              }
              AppDestination.DATA_ENTRY -> {
                DataEntryScreen(
                  viewModel = viewModel,
                  initialTab = dataEntryInitialTab,
                  onBack = { navigateBack() }
                )
              }
            }
          }
        }
      }
    }
  }
}

