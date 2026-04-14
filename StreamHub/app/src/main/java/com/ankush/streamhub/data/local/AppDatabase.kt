package com.ankush.streamhub.data.local

import android.content.Context
import androidx.room.*
import com.ankush.streamhub.data.model.Category
import com.ankush.streamhub.data.model.ContentItem
import com.ankush.streamhub.data.model.ContentType
import com.ankush.streamhub.data.model.FeedSource
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Room Entity
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
    val source: String,         // FeedSource.name()
    val category: String,       // Category.name()
    val type: String,           // ContentType.name()
    val publishedAt: Long,
    val duration: Int?,
    val viewCount: Long?,
    val isLive: Boolean,
    val author: String?,
    val savedAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────────────────────
// Extension helpers: Entity ↔ Domain model
// ─────────────────────────────────────────────────────────────────────────────

fun BookmarkEntity.toDomain() = ContentItem(
    id          = id,
    title       = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    contentUrl  = contentUrl,
    sourceUrl   = sourceUrl,
    sourceName  = sourceName,
    source      = FeedSource.valueOf(source),
    category    = Category.valueOf(category),
    type        = ContentType.valueOf(type),
    publishedAt = publishedAt,
    duration    = duration,
    viewCount   = viewCount,
    isLive      = isLive,
    author      = author,
    isBookmarked = true
)

fun ContentItem.toEntity() = BookmarkEntity(
    id          = id,
    title       = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    contentUrl  = contentUrl,
    sourceUrl   = sourceUrl,
    sourceName  = sourceName,
    source      = source.name,
    category    = category.name,
    type        = type.name,
    publishedAt = publishedAt,
    duration    = duration,
    viewCount   = viewCount,
    isLive      = isLive,
    author      = author
)

// ─────────────────────────────────────────────────────────────────────────────
// DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

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
}

// ─────────────────────────────────────────────────────────────────────────────
// Database
// ─────────────────────────────────────────────────────────────────────────────

@Database(
    entities = [BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
