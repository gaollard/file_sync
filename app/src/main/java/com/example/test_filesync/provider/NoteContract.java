package com.example.test_filesync.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * ContentProvider 契约类
 * 定义了 ContentProvider 的 URI、表名、列名等常量
 */
public final class NoteContract {

    // ContentProvider 的 authority，用于标识 ContentProvider
    public static final String AUTHORITY = "com.example.test_filesync.provider";

    // 基础 URI
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // 表路径
    public static final String PATH_NOTES = "notes";

    // 私有构造函数，防止实例化
    private NoteContract() {
    }

    /**
     * Note 表的契约类
     * 继承 BaseColumns 以获得 _ID 和 _COUNT 列
     */
    public static final class NoteEntry implements BaseColumns {

        // 表的完整 URI: content://com.example.test_filesync.provider/notes
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_NOTES)
                .build();

        // MIME 类型：目录（多条记录）
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + AUTHORITY + "/" + PATH_NOTES;

        // MIME 类型：单条记录
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + AUTHORITY + "/" + PATH_NOTES;

        // 表名
        public static final String TABLE_NAME = "notes";

        // 列名：标题
        public static final String COLUMN_TITLE = "title";

        // 列名：内容
        public static final String COLUMN_CONTENT = "content";

        // 列名：创建时间
        public static final String COLUMN_CREATED_AT = "created_at";

        // 列名：更新时间
        public static final String COLUMN_UPDATED_AT = "updated_at";

        /**
         * 构建单条记录的 URI
         * @param id 记录 ID
         * @return 单条记录的 URI
         */
        public static Uri buildNoteUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }
    }
}

