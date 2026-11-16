# NotePad 记事本应用

> 项目来自于官方早期的数据库操作的基本教程。更多的解析参考[Android Sample--NotePad解析](https://blog.csdn.net/llfjfz/article/details/67638499)

## 一、项目简介
本项目基于 Google 官方 NotePad 源码进行功能扩展，完成了移动软件开发期中实验的基本要求，并在此基础上扩展了待办事项、多媒体笔记（图片/视频）、导入导出等实用功能。

## 二、开发环境
- **开发工具**：Android Studio 2026.1.3
- **编程语言**：Java
- **目标 SDK**：API 34
- **测试设备**：Android 模拟器 - Pixel 8 (Android 14.0, API 34)

## 三、功能实现

###  基本要求（已完成）
1. **笔记时间戳显示**：在 NoteList 界面中，每条笔记下方显示最后修改时间（格式：`yyyy-MM-dd HH:mm`），利用 `SimpleCursorAdapter.ViewBinder` 实现。
2. **笔记查询功能**：在列表顶部增加搜索框，通过 `TextWatcher` 监听输入，支持按**标题**或**内容**进行实时模糊查询。

###  附加功能（自选扩展）
- **待办功能**：新增待办笔记列表，支持与普通笔记分类展示，标记完成状态。
- **图片笔记**：通过系统相册选择图片，列表显示缩略图，点击可调用系统查看器打开。
- **视频笔记**：通过文件选择器选择视频，列表显示播放图标，点击调用系统播放器播放。
- **笔记导出**：支持将全部笔记或单条笔记导出为 `.txt` 文件。
- **笔记导入/恢复**：支持从 `.txt` 文件批量导入笔记，自动解析并写入数据库。
- **UI 美化**：自定义列表项布局，增加底部功能按钮（速记、待办、图片、视频），优化视觉层次。

## 四、项目结构
com.example.android.notepad
├── NotePad.java # 数据契约类
├── NotePadProvider.java # ContentProvider，负责数据库增删改查
├── NotesList.java # 主界面：笔记列表、搜索、导入导出、多媒体入口
├── NoteEditor.java # 笔记编辑界面
├── TitleEditor.java # 标题编辑界面
├── TodoActivity.java # 待办笔记列表
└── NotesLiveFolder.java # ContentProvider的LiveFolder（实时文件夹）


## 五、关键实现说明
### 1. 时间戳显示
在查询投影中加入 `COLUMN_NAME_MODIFICATION_DATE`，通过 `ViewBinder` 将 `long` 型时间戳格式化为可读字符串。

### 2. 搜索功能
动态拼接 SQL `WHERE` 子句：`(title LIKE ? OR note LIKE ?) AND is_todo = 0`，重新查询并刷新列表。

### 3. 多媒体笔记
将图片/视频的 `content://` URI 存入笔记的 `note` 字段，列表通过 MIME 类型（`image/*` 或 `video/*`）区分显示缩略图或播放图标，点击时调用系统组件打开。

### 4. 导入导出
使用 `ACTION_CREATE_DOCUMENT` 创建 `.txt` 文件进行导出，使用 `ACTION_OPEN_DOCUMENT` 选择文件进行导入，逐行解析笔记内容。

## 六、运行说明
1. 使用 Android Studio 打开项目，等待 Gradle 同步完成。
2. 连接模拟器或真机（建议 Android 8.0 以上）。
3. 点击 Run 按钮安装并启动应用。
4. 首次启动会自动创建数据库表。

## 七、已知问题
- 语音速记功能依赖系统语音识别服务，模拟器兼容性差，故未集成。
- 导入的笔记修改时间会更新为导入时刻，原时间戳未保留。

## 八、参考资源
- [NotePad 源码](https://github.com/fjnu-cse/NotePad)
- [NotePad 源码分析](https://blog.csdn.net/llfjfz/article/details/67638499)
- [Android 数据存储官方文档](https://developer.android.google.cn/guide/topics/data/data-storage)