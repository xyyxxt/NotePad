/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends ListActivity {

    //日志
    private static final String TAG = "NotesList";

    //查询时请求的列
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1 标题
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 2 日期
            NotePad.Notes.COLUMN_NAME_NOTE, // 3 内容
            NotePad.Notes.COLUMN_NAME_IS_TODO // 4 是否为待办
    };

    //标题列在游标中的索引，供上下文菜单标题使用。
    private static final int COLUMN_INDEX_TITLE = 1;

    //列表适配器，用 SimpleCursorAdapter 将 Cursor 数据绑定到列表项视图。
    private SimpleCursorAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.noteslist);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        //如果启动本 Activity 的 Intent 没有带数据 URI，则默认使用 NotePad.Notes.CONTENT_URI
        Intent intent = getIntent();

        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        //注册上下文菜单监听器
        getListView().setOnCreateContextMenuListener(this);

        //用哪些数据
        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE ,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE} ;

        //显示到界面上的哪个位置
        int[] viewIDs = { android.R.id.text1 ,
                R.id.text2};

        //通过 ContentResolver 查询数据
        //Cursor 就像一张 Excel 表，指针一行一行指着读。
        // 【修改点】：加上查询条件，只显示 is_todo = 0 的普通笔记
        Cursor initialCursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                NotePad.Notes.COLUMN_NAME_IS_TODO + " = 0",
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // 用 SimpleCursorAdapter 把 Cursor 绑定到 noteslist_item.xml
        adapter = new SimpleCursorAdapter(
                this,                             // The Context for the ListView
                R.layout.noteslist_item,          // Points to the XML for a list item
                initialCursor,                           // The cursor to get items from
                dataColumns,
                viewIDs
        );

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.text2) {
                    long timeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    ((TextView) view).setText(sdf.format(new Date(timeMillis)));
                    return true;
                }
                if (view.getId() == R.id.item_image) {
                    String noteContent = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_NOTE));
                    ImageView imageView = (ImageView) view;


                    if (noteContent != null && noteContent.startsWith("content://")) {
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageURI(Uri.parse(noteContent));
                    } else {
                        imageView.setVisibility(View.GONE);
                    }
                    return true;
                }
                return false;
            }
        });

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        setListAdapter(adapter);

        //五个方形按钮的事件
        //1.速写
        android.widget.ImageButton btnShorthand = (android.widget.ImageButton)findViewById(R.id.list_shorthand);
        btnShorthand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesList.this, NoteEditor.class);
                intent.setAction(Intent.ACTION_INSERT);
                intent.setData(getIntent().getData());
                // 标记这是一个普通笔记
                intent.putExtra("IS_TODO", false);
                startActivity(intent);
            }
        });

        //2.待办
        android.widget.ImageButton btnTodo = (android.widget.ImageButton)findViewById(R.id.list_todo);
        btnTodo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesList.this, TodoActivity.class);
                startActivity(intent);
            }
        });

        // 3. 图片按钮
        android.widget.ImageButton btnPhoto = (android.widget.ImageButton)findViewById(R.id.list_photo);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 打开系统相册选择图片
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*"); // 限制只能选图片
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(intent, 1001); // 1001 是请求码
            }
        });

        EditText searchInput = (EditText) findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                performSearch(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    /**
     * 自定义的查询方法
     * @param keyword 搜索关键字
     */
    private void performSearch(String keyword){
        String selection = NotePad.Notes.COLUMN_NAME_IS_TODO + " = 0"; // 基础条件：只能是普通笔记
        String[] selectionArgs = null;

        if(keyword != null && !keyword.trim().isEmpty()){
            selection = "(" + NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR "
                    + NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?)"
                    + " AND " + NotePad.Notes.COLUMN_NAME_IS_TODO + " = 0";
            String searchPattern = "%" + keyword.trim() + "%";
            selectionArgs = new String[]{searchPattern,searchPattern};
        }
        Cursor newCursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        Cursor oldCursor = adapter.getCursor();

        adapter.changeCursor(newCursor);

        if(oldCursor != null && oldCursor != newCursor){
            oldCursor.close();
        }
    }

    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * Sets up a menu that provides the Insert option plus a list of alternative actions for
     * this Activity. Other applications that want to handle notes can "register" themselves in
     * Android by providing an intent filter that includes the category ALTERNATIVE and the
     * mimeTYpe NotePad.Notes.CONTENT_TYPE. If they do this, the code in onCreateOptionsMenu()
     * will add the Activity that contains the intent filter to its list of options. In effect,
     * the menu will offer the user other applications that can handle notes.
     * @param menu A Menu object, to which menu items should be added.
     * @return True, always. The menu should be displayed.
     */
    @Override  //。。。
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override//菜单显示前动态调整
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                    Menu.NONE,                  // A unique item ID is not required.
                    Menu.NONE,                  // The alternatives don't need to be in order.
                    null,                       // The caller's name is not excluded from the group.
                    specifics,                  // These specific options must appear first.
                    intent,                     // These Intent objects map to the options in specifics.
                    Menu.NONE,                  // No flags are required.
                    items                       // The menu items generated from the specifics-to-
                    // Intents mapping
            );
            // If the Edit menu item exists, adds shortcuts for it.
            if (items[0] != null) {

                // Sets the Edit menu item shortcut to numeric "1", letter "e"
                items[0].setShortcut('1', 'e');
            }
        } else {
            // If the list is empty, removes any existing alternative actions from the menu
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user selects an option from the menu, but no item
     * in the list is selected. If the option was INSERT, then a new Intent is sent out with action
     * ACTION_INSERT. The data from the incoming Intent is put into the new Intent. In effect,
     * this triggers the NoteEditor activity in the NotePad application.
     *
     * If the item was not INSERT, then most likely it was an alternative option from another
     * application. The parent method is called to process the item.
     * @param item The menu item that was selected by the user
     * @return True, if the INSERT menu item was selected; otherwise, the result of calling
     * the parent method.
     */
    @Override//。。。菜单点击处理
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_INSERT. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            Intent intent = new Intent(this, NoteEditor.class);
            intent.setAction(Intent.ACTION_INSERT);
            intent.setData(getIntent().getData());
            intent.putExtra("IS_TODO", false); // 标记为普通笔记
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_paste) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_PASTE. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            Intent intent = new Intent(this, NoteEditor.class);
            intent.setAction(Intent.ACTION_PASTE);
            intent.setData(getIntent().getData());
            intent.putExtra("IS_TODO", false); // 标记为普通笔记
            startActivity(intent);
            return true;
        }else if (item.getItemId() == R.id.menu_export) {
            exportNotesToTxt();
            return true;
        } else if (item.getItemId() == R.id.menu_import) {
            importNotesFromTxt();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private static final int REQUEST_CODE_CREATE_FILE = 1;

    //导出全部
    private void exportNotesToTxt() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String fileName = "NotePad_Backup_" + timeStamp + ".txt";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            startActivityForResult(intent, REQUEST_CODE_CREATE_FILE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "找不到文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    private static final int REQUEST_CODE_EXPORT_SINGLE = 2;
    private Uri pendingExportUri;

    //导出一个
    private void exportSingleNoteToTxt(Uri noteUri) {
        pendingExportUri = noteUri;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Note_Single_" + timeStamp + ".txt";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            startActivityForResult(intent, REQUEST_CODE_EXPORT_SINGLE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "找不到文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    //实际写文件
    private void writeDataToUri(Uri targetUri, Uri fileUri) {
        String[] exportProjection = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_NOTE
        };

        Cursor cursor = getContentResolver().query(
                targetUri,
                exportProjection,
                null, null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        if (cursor == null) {
            Toast.makeText(this, "查询数据失败", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            OutputStream outputStream = getContentResolver().openOutputStream(fileUri);
            if (outputStream == null) return;

            OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");

            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_TITLE));
                long timeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_NOTE));

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String timeStr = sdf.format(new Date(timeMillis));

                writer.write("标题：" + (title == null ? "无标题" : title) + "\n");
                writer.write("时间：" + timeStr + "\n");
                writer.write("内容：" + (note == null ? "" : note) + "\n");
                writer.write("----------------------------------------\n\n");
            }

            writer.flush();
            writer.close();
            outputStream.close();

            Toast.makeText(this, "导出成功！", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    private static final int REQUEST_CODE_IMPORT_FILE = 3;

    //选择要导入的 TXT
    private void importNotesFromTxt() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain"); // 只显示 txt 文件

        try {
            startActivityForResult(intent, REQUEST_CODE_IMPORT_FILE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "找不到文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    // 在 onActivityResult 中拦截导入请求
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK || data == null) return;

        Uri fileUri = data.getData();
        if (fileUri == null) return;

        // 1. 处理图片插入
        if (requestCode == 1001) {
            try {
                getContentResolver().takePersistableUriPermission(
                        fileUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, "[图片笔记] " + new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date()));
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, fileUri.toString());
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
            values.put(NotePad.Notes.COLUMN_NAME_IS_TODO, 0);

            getContentResolver().insert(getIntent().getData(), values);

            performSearch(""); // 刷新列表
            Toast.makeText(this, "图片笔记已保存", Toast.LENGTH_SHORT).show();

        }
        // 2. 处理 TXT 导入
        else if (requestCode == REQUEST_CODE_IMPORT_FILE) {
            parseAndSaveTxt(fileUri);
        }
        // 3. 处理全部导出
        else if (requestCode == REQUEST_CODE_CREATE_FILE) {
            writeDataToUri(getIntent().getData(), fileUri);
        }
        // 4. 处理单条导出
        else if (requestCode == REQUEST_CODE_EXPORT_SINGLE && pendingExportUri != null) {
            writeDataToUri(pendingExportUri, fileUri);
        }
    }

    //解析 TXT 文件并插入数据库
    private void parseAndSaveTxt(Uri fileUri) {
        int importCount = 0;
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(fileUri);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, "UTF-8"));

            String line;
            String currentTitle = null;
            StringBuilder currentContent = new StringBuilder();
            // 使用临时 List 先存下来，最后统一插入
            java.util.List<android.content.ContentValues> noteList = new java.util.ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("标题：")) {
                    // 如果之前已经收集了一个完整的笔记，先暂存到 List
                    if (currentTitle != null) {
                        noteList.add(buildNoteValues(currentTitle, currentContent.toString()));
                        importCount++;
                    }
                    currentTitle = line.substring(3).trim();
                    currentContent.setLength(0); // 清空内容

                } else if (line.startsWith("时间：")) {
                    // 忽略时间行
                    continue;
                } else if (line.startsWith("-----")) {
                    // 分割线，代表这一篇笔记结束了
                    if (currentTitle != null) {
                        noteList.add(buildNoteValues(currentTitle, currentContent.toString()));
                        importCount++;
                        currentTitle = null;
                        currentContent.setLength(0);
                    }
                } else if (line.startsWith("内容：")) {
                    currentContent.append(line.substring(3));
                } else {
                    // 多行内容拼接
                    if (currentContent.length() > 0) {
                        currentContent.append("\n");
                    }
                    currentContent.append(line);
                }
            }

            // 处理最后一条笔记
            if (currentTitle != null) {
                noteList.add(buildNoteValues(currentTitle, currentContent.toString()));
                importCount++;
            }

            reader.close();
            inputStream.close();

            // 统一批量插入
            for (android.content.ContentValues values : noteList) {
                getContentResolver().insert(getIntent().getData(), values);
            }

            Toast.makeText(this, "成功恢复 " + importCount + " 条笔记！", Toast.LENGTH_LONG).show();
            performSearch(""); // 刷新列表

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "恢复失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // 提取出来的辅助方法，用于构建 ContentValues
    private android.content.ContentValues buildNoteValues(String title, String content) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, content);
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        values.put(NotePad.Notes.COLUMN_NAME_IS_TODO, 0);
        return values;
    }

    // 将解析出的单条笔记插入 ContentProvider
    private void saveNoteToDatabase(String title, String content) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, content);
        // 插入当前时间作为修改时间
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

        // 【修改点】：导入的笔记默认为普通笔记
        values.put(NotePad.Notes.COLUMN_NAME_IS_TODO, 0);

        getContentResolver().insert(getIntent().getData(), values);
    }

    /**
     * This method is called when the user context-clicks a note in the list. NotesList registers
     * itself as the handler for context menus in its ListView (this is done in onCreate()).
     *
     * The only available options are COPY and DELETE.
     *
     * Context-click is equivalent to long-press.
     *
     * @param menu A ContexMenu object to which items should be added.
     * @param view The View for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     * @throws ClassCastException
     */
    @Override  //创建上下文菜单
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item. As a result,
         * getItem() returns that data as a Cursor.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see onCreateContextMenu()). The only menu items that are actually handled are DELETE and
     * COPY. Anything else is an alternative option, for which default handling should be done.
     *
     * @param item The selected menu item
     * @return True if the menu item was DELETE, and no default processing is need, otherwise false,
     * which triggers the default handling of the item.
     * @throws ClassCastException
     */
    @Override//上下文菜单事件触发
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        int id = item.getItemId();
        if (id == R.id.context_open) {
            // Launch activity to view/edit the currently selected item
            Intent editIntent = new Intent(this, NoteEditor.class);
            editIntent.setAction(Intent.ACTION_EDIT);
            editIntent.setData(noteUri);
            startActivity(editIntent);
            return true;
        } else if (id == R.id.context_copy) { //BEGIN_INCLUDE(copy)
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            // Copies the notes URI to the clipboard. In effect, this copies the note itself
            clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
                    getContentResolver(),               // resolver to retrieve URI info
                    "Note",                             // label for the clip
                    noteUri));                          // the URI

            // Returns to the caller and skips further processing.
            return true;
            //END_INCLUDE(copy)
        } else if (id == R.id.context_delete) {
            // Deletes the note from the provider by passing in a URI in note ID format.
            // Please see the introductory note about performing provider operations on the
            // UI thread.
            getContentResolver().delete(
                    noteUri,  // The URI of the provider
                    null,     // No where clause is needed, since only a single note ID is being
                    // passed in.
                    null      // No where clause is used, so no where arguments are needed.
            );

            // Returns to the caller and skips further processing.
            return true;
        }else if (id == R.id.context_export) {
            exportSingleNoteToTxt(noteUri);
            return true;
        }
        return super.onContextItemSelected(item);
    }


    /**
     * This method is called when the user clicks a note in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    @SuppressLint("WrongConstant")
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        Cursor cursor = (Cursor) getListAdapter().getItem(position);
        String noteContent = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_NOTE));

        if (noteContent != null && noteContent.startsWith("content://")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                String mimeType = getContentResolver().getType(Uri.parse(noteContent));
                if (mimeType == null) mimeType = "image/*";

                intent.setDataAndType(Uri.parse(noteContent), mimeType);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(intent, "选择应用打开图片"));

            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "没有找到能查看图片的应用", Toast.LENGTH_SHORT).show();
            }
        } else {
            Intent intent = new Intent(this, NoteEditor.class);
            intent.setAction(Intent.ACTION_EDIT);
            intent.setData(uri);
            startActivity(intent);
        }
    }
}