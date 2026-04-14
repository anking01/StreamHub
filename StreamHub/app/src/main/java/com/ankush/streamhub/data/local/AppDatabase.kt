package com.ankush.streamhub.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ankush.streamhub.data.model.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Bookmark Entity
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val contentUrl: String,
    val sourceUrl: String,
    val sourceName: String,
    val source: String,
    val category: String,
    val type: String,
    val publishedAt: Long,
    val duration: Int?,
    val viewCount: Long?,
    val isLive: Boolean,
    val author: String?,
    @ColumnInfo(name = "collection_id") val collectionId: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)

fun BookmarkEntity.toDomain() = ContentItem(
    id           = id,
    title        = title,
    description  = description,
    thumbnailUrl = thumbnailUrl,
    contentUrl   = contentUrl,
    sourceUrl    = sourceUrl,
    sourceName   = sourceName,
    source       = FeedSource.valueOf(source),
    category     = Category.valueOf(category),
    type         = ContentType.valueOf(type),
    publishedAt  = publishedAt,
    duration     = duration,
    viewCount    = viewCount,
    isLive       = isLive,
    author       = author,
    isBookmarked = true
)

fun ContentItem.toEntity(collectionId: String? = null) = BookmarkEntity(
    id           = id,
    title        = title,
    description  = description,
    thumbnailUrl = thumbnailUrl,
    contentUrl   = contentUrl,
    sourceUrl    = sourceUrl,
    sourceName   = sourceName,
    source       = source.name,
    category     = category.name,
    type         = type.name,
    publishedAt  = publishedAt,
    duration     = duration,
    viewCount    = viewCount,
    isLive       = isLive,
    author       = author,
    collectionId = collectionId
)

// ─────────────────────────────────────────────────────────────────────────────
// Cached Feed Entity  (offline support)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "cached_feed")
data class CachedFeedEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val contentUrl: String,
    val sourceUrl: String,
    val sourceName: String,
    val source: String,
    val category: String,
    val type: String,
    val publishedAt: Long,
    val duration: Int?,
    val viewCount: Long?,
    val isLive: Boolean,
    val author: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

fun CachedFeedEntity.toDomain() = ContentItem(
    id           = id,
    title        = title,
    description  = description,
    thumbnailUrl = thumbnailUrl,
    contentUrl   = contentUrl,
    sourceUrl    = sourceUrl,
    sourceName   = sourceName,
    source       = FeedSource.valueOf(source),
    category     = Category.valueOf(category),
    type         = ContentType.valueOf(type),
    publishedAt  = publishedAt,
    duration     = duration,
    viewCount    = viewCount,
    isLive       = isLive,
    author       = author
)

fun ContentItem.toCachedEntity() = CachedFeedEntity(
    id           = id,
    title        = title,
    description  = description,
    thumbnailUrl = thumbnailUrl,
    contentUrl   = contentUrl,
    sourceUrl    = sourceUrl,
    sourceName   = sourceName,
    source       = source.name,
    category     = category.name,
    type         = type.name,
    publishedAt  = publishedAt,
    duration     = duration,
    viewCount    = viewCount,
    isLive       = isLive,
    author       = author
)

// ─────────────────────────────────────────────────────────────────────────────
// Watch History Entity
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val id: String,           // UUID per watch event
    val contentId: String,
    val title: String,
    val sourceName: String,
    val source: String,
    val contentUrl: String,
    val thumbnailUrl: String,
    val type: String,
    val watchedAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────────────────────
// Collection Entity  (bookmark folders)
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String = "📁",
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────────────────────
// DAOs
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE collection_id IS NULL ORDER BY savedAt DESC")
    fun getUncategorizedBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE collection_id = :collectionId ORDER BY savedAt DESC")
    fun getBookmarksByCollection(collectionId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT id FROM bookmarks")
    fun getAllBookmarkIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE id = :id)")
    suspend fun isBookmarked(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun getCount(): Int

    @Query("UPDATE bookmarks SET collection_id = :collectionId WHERE id = :bookmarkId")
    suspend fun moveToCollection(bookmarkId: String, collectionId: String?)
}

@Dao
interface CachedFeedDao {

    @Query("SELECT * FROM cached_feed ORDER BY publishedAt DESC")
    suspend fun getAll(): List<CachedFeedEntity>

    @Query("SELECT * FROM cached_feed WHERE category = :category ORDER BY publishedAt DESC")
    suspend fun getByCategory(category: String): List<CachedFeedEntity>

    @Query("SELECT * FROM cached_feed ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun getTop(limit: Int): List<CachedFeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedFeedEntity>)

    @Query("DELETE FROM cached_feed")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM cached_feed")
    suspend fun getCount(): Int
}

@Dao
interface WatchHistoryDao {

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT 100")
    fun getHistory(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE contentId = :contentId")
    suspend fun deleteByContentId(contentId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY createdAt ASC")
    fun getAll(): Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionEntity)

    @Delete
    suspend fun delete(collection: CollectionEntity)
}

// ─────────────────────────────────────────────────────────────────────────────
// Migration  1 → 2
// ─────────────────────────────────────────────────────────────────────────────

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // cached_feed
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `cached_feed` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `thumbnailUrl` TEXT NOT NULL,
                `contentUrl` TEXT NOT NULL,
                `sourceUrl` TEXT NOT NULL,
                `sourceName` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `publishedAt` INTEGER NOT NULL,
                `duration` INTEGER,
                `viewCount` INTEGER,
                `isLive` INTEGER NOT NULL,
                `author` TEXT,
                `cachedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // watch_history
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `watch_history` (
                `id` TEXT NOT NULL,
                `contentId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `sourceName` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `contentUrl` TEXT NOT NULL,
                `thumbnailUrl` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `watchedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // collections
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `collections` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `emoji` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // add collection_id column to bookmarks
        database.execSQL("ALTER TABLE bookmarks ADD COLUMN `collection_id` TEXT")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Database
// ─────────────────────────────────────────────────────────────────────────────

@Database(
    entities = [
        BookmarkEntity::class,
        CachedFeedEntity::class,
        WatchHistoryEntity::class,
        CollectionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao
    abstract fun cachedFeedDao(): CachedFeedDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "streamhub_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
