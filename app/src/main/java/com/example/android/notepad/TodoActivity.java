package com.example.android.notepad;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class TodoActivity extends Activity {

    private ListView listDoing;
    private ListView listDone;
    private SimpleCursorAdapter adapterDoing;
    private SimpleCursorAdapter adapterDone;

    // 查询的列
    private static final String[] PROJECTION = {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_IS_DONE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        // 返回按钮
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_add_todo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到 NoteEditor 去新建一条待办
                Intent intent = new Intent(TodoActivity.this, NoteEditor.class);
                intent.setAction(Intent.ACTION_INSERT);
                intent.setData(NotePad.Notes.CONTENT_URI);
                intent.putExtra("IS_TODO", true);
                startActivity(intent);
            }
        });

        listDoing = (ListView)findViewById(R.id.list_todo_doing);
        listDone = (ListView)findViewById(R.id.list_todo_done);

        setupLists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到这个页面（包括新建待办返回后），自动刷新数据
        loadData();
    }

    private void setupLists() {
        String[] from = { NotePad.Notes.COLUMN_NAME_TITLE };
        int[] to = { R.id.tv_todo_title };

        // 进行中的 Adapter
        adapterDoing = new SimpleCursorAdapter(this, R.layout.item_todo, null, from, to, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                CheckBox cb = (CheckBox) view.findViewById(R.id.cb_todo);
                TextView tv = (TextView) view.findViewById(R.id.tv_todo_title);
                cb.setChecked(false);
                tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tv.setTextColor(0xFFFFFFFF);

                // 关键修复：这里的 id 需要在点击时获取，不能写在外面，防止列表滚动后 position 错乱
                final long itemId = getItemId(position);
                cb.setOnClickListener(v -> updateTodoStatus(itemId, 1));
                return view;
            }
        };
        listDoing.setAdapter(adapterDoing);

        // 已完成的 Adapter
        adapterDone = new SimpleCursorAdapter(this, R.layout.item_todo, null, from, to, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                CheckBox cb = (CheckBox) view.findViewById(R.id.cb_todo);
                TextView tv = (TextView) view.findViewById(R.id.tv_todo_title);
                cb.setChecked(true);
                tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tv.setTextColor(0xFF888888);

                final long itemId = getItemId(position);
                cb.setOnClickListener(v -> updateTodoStatus(itemId, 0));
                return view;
            }
        };
        listDone.setAdapter(adapterDone);
    }

    private void loadData() {
        Uri uri = NotePad.Notes.CONTENT_URI;

        // 查“进行中”
        Cursor cursorDoing = getContentResolver().query(uri, PROJECTION,
                NotePad.Notes.COLUMN_NAME_IS_TODO + " = 1 AND " +
                        NotePad.Notes.COLUMN_NAME_IS_DONE + " = 0", null, null);

        Cursor oldDoing = adapterDoing.getCursor();
        adapterDoing.changeCursor(cursorDoing); // 👈 关键修复：把数据塞给 Adapter
        if (oldDoing != null && oldDoing != cursorDoing) {
            oldDoing.close(); // 关闭旧游标防止内存泄漏
        }

        // 查“已完成”
        Cursor cursorDone = getContentResolver().query(uri, PROJECTION,
                NotePad.Notes.COLUMN_NAME_IS_TODO + " = 1 AND " +
                        NotePad.Notes.COLUMN_NAME_IS_DONE + " = 1", null, null);

        Cursor oldDone = adapterDone.getCursor();
        adapterDone.changeCursor(cursorDone); // 👈 关键修复：把数据塞给 Adapter
        if (oldDone != null && oldDone != cursorDone) {
            oldDone.close();
        }
    }

    // 更新勾选状态
    private void updateTodoStatus(long id, int isDone) {
        Uri noteUri = Uri.withAppendedPath(NotePad.Notes.CONTENT_URI, String.valueOf(id));
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_IS_DONE, isDone);
        getContentResolver().update(noteUri, values, null, null);

        // 更新完后重新加载数据
        loadData();
    }
}