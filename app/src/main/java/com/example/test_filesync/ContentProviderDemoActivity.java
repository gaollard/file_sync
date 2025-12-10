package com.example.test_filesync;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.test_filesync.provider.NoteContract;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ContentProvider 演示 Activity
 * 展示如何使用 ContentProvider 进行 CRUD 操作
 */
public class ContentProviderDemoActivity extends AppCompatActivity {

    private EditText etTitle;
    private EditText etContent;
    private EditText etNoteId;
    private TextView tvResult;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_provider_demo);

        // 初始化视图
        initViews();
        
        // 初始化日期格式化器
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        etNoteId = findViewById(R.id.et_note_id);
        tvResult = findViewById(R.id.tv_result);

        Button btnInsert = findViewById(R.id.btn_insert);
        Button btnQueryAll = findViewById(R.id.btn_query_all);
        Button btnQueryById = findViewById(R.id.btn_query_by_id);
        Button btnUpdate = findViewById(R.id.btn_update);
        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnDeleteAll = findViewById(R.id.btn_delete_all);

        // 插入数据
        btnInsert.setOnClickListener(v -> insertNote());
        
        // 查询所有数据
        btnQueryAll.setOnClickListener(v -> queryAllNotes());
        
        // 根据 ID 查询
        btnQueryById.setOnClickListener(v -> queryNoteById());
        
        // 更新数据
        btnUpdate.setOnClickListener(v -> updateNote());
        
        // 删除数据
        btnDelete.setOnClickListener(v -> deleteNote());
        
        // 删除所有数据
        btnDeleteAll.setOnClickListener(v -> showDeleteAllConfirmDialog());
    }

    /**
     * 插入笔记
     */
    private void insertNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建 ContentValues 对象
        ContentValues values = new ContentValues();
        values.put(NoteContract.NoteEntry.COLUMN_TITLE, title);
        values.put(NoteContract.NoteEntry.COLUMN_CONTENT, content);

        // 通过 ContentResolver 插入数据
        Uri newUri = getContentResolver().insert(NoteContract.NoteEntry.CONTENT_URI, values);

        if (newUri != null) {
            long newId = Long.parseLong(newUri.getLastPathSegment());
            showResult("插入成功！\n新记录 ID: " + newId);
            clearInputs();
        } else {
            showResult("插入失败！");
        }
    }

    /**
     * 查询所有笔记
     */
    private void queryAllNotes() {
        // 定义要查询的列
        String[] projection = {
                NoteContract.NoteEntry._ID,
                NoteContract.NoteEntry.COLUMN_TITLE,
                NoteContract.NoteEntry.COLUMN_CONTENT,
                NoteContract.NoteEntry.COLUMN_CREATED_AT,
                NoteContract.NoteEntry.COLUMN_UPDATED_AT
        };

        // 通过 ContentResolver 查询数据
        Cursor cursor = getContentResolver().query(
                NoteContract.NoteEntry.CONTENT_URI,
                projection,
                null,
                null,
                NoteContract.NoteEntry._ID + " DESC"  // 按 ID 降序排列
        );

        displayCursorResult(cursor, "查询所有笔记");
    }

    /**
     * 根据 ID 查询笔记
     */
    private void queryNoteById() {
        String idStr = etNoteId.getText().toString().trim();
        if (idStr.isEmpty()) {
            Toast.makeText(this, "请输入笔记 ID", Toast.LENGTH_SHORT).show();
            return;
        }

        long id = Long.parseLong(idStr);
        
        // 构建单条记录的 URI
        Uri noteUri = NoteContract.NoteEntry.buildNoteUri(id);

        String[] projection = {
                NoteContract.NoteEntry._ID,
                NoteContract.NoteEntry.COLUMN_TITLE,
                NoteContract.NoteEntry.COLUMN_CONTENT,
                NoteContract.NoteEntry.COLUMN_CREATED_AT,
                NoteContract.NoteEntry.COLUMN_UPDATED_AT
        };

        Cursor cursor = getContentResolver().query(noteUri, projection, null, null, null);
        displayCursorResult(cursor, "查询 ID=" + id + " 的笔记");
    }

    /**
     * 更新笔记
     */
    private void updateNote() {
        String idStr = etNoteId.getText().toString().trim();
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (idStr.isEmpty()) {
            Toast.makeText(this, "请输入要更新的笔记 ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "请输入要更新的内容", Toast.LENGTH_SHORT).show();
            return;
        }

        long id = Long.parseLong(idStr);
        Uri noteUri = NoteContract.NoteEntry.buildNoteUri(id);

        ContentValues values = new ContentValues();
        if (!title.isEmpty()) {
            values.put(NoteContract.NoteEntry.COLUMN_TITLE, title);
        }
        if (!content.isEmpty()) {
            values.put(NoteContract.NoteEntry.COLUMN_CONTENT, content);
        }

        // 通过 ContentResolver 更新数据
        int rowsUpdated = getContentResolver().update(noteUri, values, null, null);

        if (rowsUpdated > 0) {
            showResult("更新成功！\n更新了 " + rowsUpdated + " 条记录");
            clearInputs();
        } else {
            showResult("更新失败！未找到 ID=" + id + " 的记录");
        }
    }

    /**
     * 删除笔记
     */
    private void deleteNote() {
        String idStr = etNoteId.getText().toString().trim();
        if (idStr.isEmpty()) {
            Toast.makeText(this, "请输入要删除的笔记 ID", Toast.LENGTH_SHORT).show();
            return;
        }

        long id = Long.parseLong(idStr);
        Uri noteUri = NoteContract.NoteEntry.buildNoteUri(id);

        // 通过 ContentResolver 删除数据
        int rowsDeleted = getContentResolver().delete(noteUri, null, null);

        if (rowsDeleted > 0) {
            showResult("删除成功！\n删除了 " + rowsDeleted + " 条记录");
            etNoteId.setText("");
        } else {
            showResult("删除失败！未找到 ID=" + id + " 的记录");
        }
    }

    /**
     * 显示删除所有确认对话框
     */
    private void showDeleteAllConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除所有笔记吗？此操作不可恢复！")
                .setPositiveButton("确定", (dialog, which) -> deleteAllNotes())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除所有笔记
     */
    private void deleteAllNotes() {
        int rowsDeleted = getContentResolver().delete(
                NoteContract.NoteEntry.CONTENT_URI,
                null,
                null
        );
        showResult("删除完成！\n共删除了 " + rowsDeleted + " 条记录");
    }

    /**
     * 显示游标查询结果
     */
    private void displayCursorResult(Cursor cursor, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(title).append(" ===\n\n");

        if (cursor == null || cursor.getCount() == 0) {
            sb.append("没有找到数据");
            if (cursor != null) {
                cursor.close();
            }
            showResult(sb.toString());
            return;
        }

        sb.append("共 ").append(cursor.getCount()).append(" 条记录\n");
        sb.append("----------------------------------------\n");

        while (cursor.moveToNext()) {
            int idIndex = cursor.getColumnIndex(NoteContract.NoteEntry._ID);
            int titleIndex = cursor.getColumnIndex(NoteContract.NoteEntry.COLUMN_TITLE);
            int contentIndex = cursor.getColumnIndex(NoteContract.NoteEntry.COLUMN_CONTENT);
            int createdIndex = cursor.getColumnIndex(NoteContract.NoteEntry.COLUMN_CREATED_AT);
            int updatedIndex = cursor.getColumnIndex(NoteContract.NoteEntry.COLUMN_UPDATED_AT);

            long id = cursor.getLong(idIndex);
            String noteTitle = cursor.getString(titleIndex);
            String noteContent = cursor.getString(contentIndex);
            long createdAt = cursor.getLong(createdIndex);
            long updatedAt = cursor.getLong(updatedIndex);

            sb.append("ID: ").append(id).append("\n");
            sb.append("标题: ").append(noteTitle).append("\n");
            sb.append("内容: ").append(noteContent != null ? noteContent : "(空)").append("\n");
            sb.append("创建时间: ").append(dateFormat.format(new Date(createdAt))).append("\n");
            sb.append("更新时间: ").append(dateFormat.format(new Date(updatedAt))).append("\n");
            sb.append("----------------------------------------\n");
        }

        cursor.close();
        showResult(sb.toString());
    }

    /**
     * 显示结果
     */
    private void showResult(String result) {
        tvResult.setText(result);
    }

    /**
     * 清空输入框
     */
    private void clearInputs() {
        etTitle.setText("");
        etContent.setText("");
    }
}

