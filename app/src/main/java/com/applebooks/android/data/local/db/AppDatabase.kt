package com.applebooks.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.applebooks.android.data.local.db.dao.BookDao
import com.applebooks.android.data.local.db.dao.CollectionDao
import com.applebooks.android.data.local.db.dao.ReadingProgressDao
import com.applebooks.android.data.local.db.entity.BookCollectionCrossRef
import com.applebooks.android.data.local.db.entity.BookEntity
import com.applebooks.android.data.local.db.entity.CollectionEntity
import com.applebooks.android.data.local.db.entity.ReadingProgressEntity

@Database(
    entities = [
        BookEntity::class,
        CollectionEntity::class,
        BookCollectionCrossRef::class,
        ReadingProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun collectionDao(): CollectionDao
    abstract fun readingProgressDao(): ReadingProgressDao
}
