package com.example.test_filesync.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ContentProvider 实现类
 * 提供对 Note 数据的 CRUD 操作
 * 
 * ContentProvider 是 Android 四大组件之一，用于在不同应用之间共享数据
 * 主要方法：
 * - query(): 查询数据
 * - insert(): 插入数据
 * - update(): 更新数据
 * - delete(): 删除数据
 * - getType(): 返回 MIME 类型
 */
public class NoteProvider extends ContentProvider {

    // UriMatcher 匹配码
    private static final int NOTES = 100;      // 匹配 notes 表（多条记录）
    private static final int NOTE_ID = 101;    // 匹配 notes 表中的单条记录

    // UriMatcher 用于匹配 URI
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // 静态初始化块，设置 URI 匹配规则
    static {
        // 匹配 content://com.example.test_filesync.provider/notes
        sUriMatcher.addURI(NoteContract.AUTHORITY, NoteContract.PATH_NOTES, NOTES);
        
        // 匹配 content://com.example.test_filesync.provider/notes/# (# 表示数字)
        sUriMatcher.addURI(NoteContract.AUTHORITY, NoteContract.PATH_NOTES + "/#", NOTE_ID);
    }

    // 数据库帮助类
    private NoteDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        // 初始化数据库帮助类
        mDbHelper = new NoteDbHelper(getContext());
        return true;
    }

    /**
     * 查询数据
     * 
     * @param uri 查询的 URI
     * @param projection 需要返回的列
     * @param selection WHERE 子句
     * @param selectionArgs WHERE 子句的参数
     * @param sortOrder 排序方式
     * @return 查询结果游标
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case NOTES:
                // 查询所有记录
                cursor = db.query(
                        NoteContract.NoteEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
                
            case NOTE_ID:
                // 查询单条记录
                // 从 URI 中提取 ID
                selection = NoteContract.NoteEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(
                        NoteContract.NoteEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
                
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // 设置通知 URI，当数据变化时通知观察者
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        
        return cursor;
    }

    /**
     * 返回 URI 对应的 MIME 类型
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case NOTES:
                return NoteContract.NoteEntry.CONTENT_TYPE;
            case NOTE_ID:
                return NoteContract.NoteEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    /**
     * 插入数据
     * 
     * @param uri 插入的 URI
     * @param values 要插入的数据
     * @return 新插入记录的 URI
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        int match = sUriMatcher.match(uri);
        
        if (match != NOTES) {
            throw new IllegalArgumentException("Insertion is not supported for URI: " + uri);
        }

        // 数据验证
        if (values == null || !values.containsKey(NoteContract.NoteEntry.COLUMN_TITLE)) {
            throw new IllegalArgumentException("Note requires a title");
        }

        String title = values.getAsString(NoteContract.NoteEntry.COLUMN_TITLE);
        if (TextUtils.isEmpty(title)) {
            throw new IllegalArgumentException("Note requires a non-empty title");
        }

        // 设置时间戳
        long currentTime = System.currentTimeMillis();
        values.put(NoteContract.NoteEntry.COLUMN_CREATED_AT, currentTime);
        values.put(NoteContract.NoteEntry.COLUMN_UPDATED_AT, currentTime);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = db.insert(NoteContract.NoteEntry.TABLE_NAME, null, values);

        if (id == -1) {
            return null;
        }

        // 通知数据变化
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * 删除数据
     * 
     * @param uri 删除的 URI
     * @param selection WHERE 子句
     * @param selectionArgs WHERE 子句的参数
     * @return 删除的行数
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsDeleted;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case NOTES:
                // 删除符合条件的所有记录
                rowsDeleted = db.delete(NoteContract.NoteEntry.TABLE_NAME, selection, selectionArgs);
                break;
                
            case NOTE_ID:
                // 删除单条记录
                selection = NoteContract.NoteEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = db.delete(NoteContract.NoteEntry.TABLE_NAME, selection, selectionArgs);
                break;
                
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // 通知数据变化
        if (rowsDeleted > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    /**
     * 更新数据
     * 
     * @param uri 更新的 URI
     * @param values 要更新的数据
     * @param selection WHERE 子句
     * @param selectionArgs WHERE 子句的参数
     * @return 更新的行数
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        
        if (values == null || values.size() == 0) {
            return 0;
        }

        // 数据验证
        if (values.containsKey(NoteContract.NoteEntry.COLUMN_TITLE)) {
            String title = values.getAsString(NoteContract.NoteEntry.COLUMN_TITLE);
            if (TextUtils.isEmpty(title)) {
                throw new IllegalArgumentException("Note requires a non-empty title");
            }
        }

        // 更新时间戳
        values.put(NoteContract.NoteEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsUpdated;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case NOTES:
                // 更新符合条件的所有记录
                rowsUpdated = db.update(NoteContract.NoteEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
                
            case NOTE_ID:
                // 更新单条记录
                selection = NoteContract.NoteEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsUpdated = db.update(NoteContract.NoteEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
                
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // 通知数据变化
        if (rowsUpdated > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }
}

