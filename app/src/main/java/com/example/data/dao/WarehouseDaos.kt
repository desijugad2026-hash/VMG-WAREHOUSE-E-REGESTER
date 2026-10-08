package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankAccount
import com.example.data.model.CatalogItem
import com.example.data.model.Creditor
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
  @Query("SELECT * FROM bank_accounts ORDER BY bankName ASC")
  fun getAll(): Flow<List<BankAccount>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(account: BankAccount): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(accounts: List<BankAccount>)

  @Update
  suspend fun update(account: BankAccount)

  @Delete
  suspend fun delete(account: BankAccount)

  @Query("DELETE FROM bank_accounts WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("SELECT COUNT(*) FROM bank_accounts")
  suspend fun getCount(): Int
}

@Dao
interface CreditorDao {
  @Query("SELECT * FROM creditors ORDER BY name ASC")
  fun getAll(): Flow<List<Creditor>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(creditor: Creditor): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(creditors: List<Creditor>)

  @Update
  suspend fun update(creditor: Creditor)

  @Delete
  suspend fun delete(creditor: Creditor)

  @Query("DELETE FROM creditors WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("SELECT COUNT(*) FROM creditors")
  suspend fun getCount(): Int
}

@Dao
interface CatalogItemDao {
  @Query("SELECT * FROM catalog_items ORDER BY designName ASC")
  fun getAll(): Flow<List<CatalogItem>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: CatalogItem): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<CatalogItem>)

  @Update
  suspend fun update(item: CatalogItem)

  @Delete
  suspend fun delete(item: CatalogItem)

  @Query("DELETE FROM catalog_items WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("SELECT COUNT(*) FROM catalog_items")
  suspend fun getCount(): Int
}

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
  fun getAllTransactions(): Flow<List<TransactionRecord>>

  @Query("SELECT * FROM transactions WHERE dateKey = :dateKey ORDER BY timestamp ASC")
  fun getTransactionsByDate(dateKey: String): Flow<List<TransactionRecord>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(tx: TransactionRecord): Long

  @Query("SELECT MAX(serialNumber) FROM transactions")
  suspend fun getMaxSerialNumber(): Long?

  @Query("SELECT COUNT(*) FROM transactions")
  suspend fun getCount(): Int

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM transactions WHERE dateKey = :dateKey")
  suspend fun deleteByDateKey(dateKey: String)

  @Query("DELETE FROM transactions")
  suspend fun clearAll()
}
