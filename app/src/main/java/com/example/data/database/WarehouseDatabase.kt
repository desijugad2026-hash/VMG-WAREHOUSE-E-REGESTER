package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BankAccountDao
import com.example.data.dao.CatalogItemDao
import com.example.data.dao.CreditorDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BankAccount
import com.example.data.model.CatalogItem
import com.example.data.model.Creditor
import com.example.data.model.TransactionRecord

@Database(
  entities = [
    BankAccount::class,
    Creditor::class,
    CatalogItem::class,
    TransactionRecord::class
  ],
  version = 2,
  exportSchema = false
)
abstract class WarehouseDatabase : RoomDatabase() {
  abstract fun bankAccountDao(): BankAccountDao
  abstract fun creditorDao(): CreditorDao
  abstract fun catalogItemDao(): CatalogItemDao
  abstract fun transactionDao(): TransactionDao

  companion object {
    @Volatile
    private var INSTANCE: WarehouseDatabase? = null

    fun getDatabase(context: Context): WarehouseDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          WarehouseDatabase::class.java,
          "warehouse_register.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
