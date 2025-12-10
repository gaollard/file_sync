package com.example.test_filesync.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite 数据库帮助类
 * 负责数据库的创建和版本管理
 */
public class NoteDbHelper extends SQLiteOpenHelper {

    // 数据库名称
    private static final String DATABASE_NAME = "notes.db";

    // 数据库版本
    private static final int DATABASE_VERSION = 1;

    // 创建表的 SQL 语句
    private static final String SQL_CREATE_NOTES_TABLE =
            "CREATE TABLE " + NoteContract.NoteEntry.TABLE_NAME + " (" +
                    NoteContract.NoteEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    NoteContract.NoteEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                    NoteContract.NoteEntry.COLUMN_CONTENT + " TEXT, " +
                    NoteContract.NoteEntry.COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                    NoteContract.NoteEntry.COLUMN_UPDATED_AT + " INTEGER NOT NULL" +
                    ")";

    // 删除表的 SQL 语句
    private static final String SQL_DELETE_NOTES_TABLE =
            "DROP TABLE IF EXISTS " + NoteContract.NoteEntry.TABLE_NAME;

    public NoteDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建表
        db.execSQL(SQL_CREATE_NOTES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 简单的升级策略：删除旧表，创建新表
        // 实际应用中应该使用更安全的迁移策略
        db.execSQL(SQL_DELETE_NOTES_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}


