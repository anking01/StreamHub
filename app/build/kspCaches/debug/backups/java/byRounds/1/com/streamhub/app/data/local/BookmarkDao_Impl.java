package com.streamhub.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BookmarkDao_Impl implements BookmarkDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BookmarkEntity> __insertionAdapterOfBookmarkEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public BookmarkDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBookmarkEntity = new EntityInsertionAdapter<BookmarkEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `bookmarks` (`id`,`title`,`description`,`thumbnailUrl`,`contentUrl`,`sourceUrl`,`sourceName`,`source`,`category`,`type`,`publishedAt`,`duration`,`viewCount`,`isLive`,`author`,`savedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BookmarkEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getThumbnailUrl());
        statement.bindString(5, entity.getContentUrl());
        statement.bindString(6, entity.getSourceUrl());
        statement.bindString(7, entity.getSourceName());
        statement.bindString(8, entity.getSource());
        statement.bindString(9, entity.getCategory());
        statement.bindString(10, entity.getType());
        statement.bindLong(11, entity.getPublishedAt());
        if (entity.getDuration() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getDuration());
        }
        if (entity.getViewCount() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getViewCount());
        }
        final int _tmp = entity.isLive() ? 1 : 0;
        statement.bindLong(14, _tmp);
        if (entity.getAuthor() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getAuthor());
        }
        statement.bindLong(16, entity.getSavedAt());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bookmarks WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bookmarks";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final BookmarkEntity bookmark,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBookmarkEntity.insert(bookmark);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BookmarkEntity>> getAllBookmarks() {
    final String _sql = "SELECT * FROM bookmarks ORDER BY savedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"bookmarks"}, new Callable<List<BookmarkEntity>>() {
      @Override
      @NonNull
      public List<BookmarkEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final int _cursorIndexOfContentUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "contentUrl");
          final int _cursorIndexOfSourceUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceUrl");
          final int _cursorIndexOfSourceName = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceName");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPublishedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "publishedAt");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfViewCount = CursorUtil.getColumnIndexOrThrow(_cursor, "viewCount");
          final int _cursorIndexOfIsLive = CursorUtil.getColumnIndexOrThrow(_cursor, "isLive");
          final int _cursorIndexOfAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "author");
          final int _cursorIndexOfSavedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "savedAt");
          final List<BookmarkEntity> _result = new ArrayList<BookmarkEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BookmarkEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpThumbnailUrl;
            _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            final String _tmpContentUrl;
            _tmpContentUrl = _cursor.getString(_cursorIndexOfContentUrl);
            final String _tmpSourceUrl;
            _tmpSourceUrl = _cursor.getString(_cursorIndexOfSourceUrl);
            final String _tmpSourceName;
            _tmpSourceName = _cursor.getString(_cursorIndexOfSourceName);
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpPublishedAt;
            _tmpPublishedAt = _cursor.getLong(_cursorIndexOfPublishedAt);
            final Integer _tmpDuration;
            if (_cursor.isNull(_cursorIndexOfDuration)) {
              _tmpDuration = null;
            } else {
              _tmpDuration = _cursor.getInt(_cursorIndexOfDuration);
            }
            final Long _tmpViewCount;
            if (_cursor.isNull(_cursorIndexOfViewCount)) {
              _tmpViewCount = null;
            } else {
              _tmpViewCount = _cursor.getLong(_cursorIndexOfViewCount);
            }
            final boolean _tmpIsLive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLive);
            _tmpIsLive = _tmp != 0;
            final String _tmpAuthor;
            if (_cursor.isNull(_cursorIndexOfAuthor)) {
              _tmpAuthor = null;
            } else {
              _tmpAuthor = _cursor.getString(_cursorIndexOfAuthor);
            }
            final long _tmpSavedAt;
            _tmpSavedAt = _cursor.getLong(_cursorIndexOfSavedAt);
            _item = new BookmarkEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpThumbnailUrl,_tmpContentUrl,_tmpSourceUrl,_tmpSourceName,_tmpSource,_tmpCategory,_tmpType,_tmpPublishedAt,_tmpDuration,_tmpViewCount,_tmpIsLive,_tmpAuthor,_tmpSavedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<String>> getAllBookmarkIds() {
    final String _sql = "SELECT id FROM bookmarks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"bookmarks"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object isBookmarked(final String id, final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM bookmarks WHERE id = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM bookmarks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
