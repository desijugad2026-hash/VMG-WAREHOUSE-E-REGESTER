package com.example.data.repository

import com.example.data.dao.BankAccountDao
import com.example.data.dao.CatalogItemDao
import com.example.data.dao.CreditorDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BankAccount
import com.example.data.model.CatalogItem
import com.example.data.model.Creditor
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

class WarehouseRepository(
  private val bankAccountDao: BankAccountDao,
  private val creditorDao: CreditorDao,
  private val catalogItemDao: CatalogItemDao,
  private val transactionDao: TransactionDao
) {
  val bankAccounts: Flow<List<BankAccount>> = bankAccountDao.getAll()
  val creditors: Flow<List<Creditor>> = creditorDao.getAll()
  val catalogItems: Flow<List<CatalogItem>> = catalogItemDao.getAll()
  val allTransactions: Flow<List<TransactionRecord>> = transactionDao.getAllTransactions()

  suspend fun insertBankAccount(account: BankAccount): Long = bankAccountDao.insert(account)
  suspend fun updateBankAccount(account: BankAccount) = bankAccountDao.update(account)
  suspend fun deleteBankAccount(account: BankAccount) = bankAccountDao.delete(account)
  suspend fun deleteBankAccountById(id: Long) = bankAccountDao.deleteById(id)

  suspend fun insertCreditor(creditor: Creditor): Long = creditorDao.insert(creditor)
  suspend fun insertAllCreditors(creditorsList: List<Creditor>) = creditorDao.insertAll(creditorsList)
  suspend fun updateCreditor(creditor: Creditor) = creditorDao.update(creditor)
  suspend fun deleteCreditor(creditor: Creditor) = creditorDao.delete(creditor)
  suspend fun deleteCreditorById(id: Long) = creditorDao.deleteById(id)

  suspend fun insertCatalogItem(item: CatalogItem): Long = catalogItemDao.insert(item)
  suspend fun insertAllCatalogItems(items: List<CatalogItem>) = catalogItemDao.insertAll(items)
  suspend fun updateCatalogItem(item: CatalogItem) = catalogItemDao.update(item)
  suspend fun deleteCatalogItem(item: CatalogItem) = catalogItemDao.delete(item)
  suspend fun deleteCatalogItemById(id: Long) = catalogItemDao.deleteById(id)

  suspend fun insertTransaction(tx: TransactionRecord): Long = transactionDao.insert(tx)
  suspend fun getNextSerialNumber(): Long {
    val max = transactionDao.getMaxSerialNumber() ?: 0L
    if (max > 0L) return max + 1L
    val count = transactionDao.getCount()
    return (count + 1).toLong()
  }
  suspend fun deleteTransactionById(id: Long) = transactionDao.deleteById(id)
  suspend fun deleteTransactionsByDate(dateKey: String) = transactionDao.deleteByDateKey(dateKey)
}
