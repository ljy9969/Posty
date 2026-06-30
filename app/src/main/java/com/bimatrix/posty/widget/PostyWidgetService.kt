package com.bimatrix.posty.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.bimatrix.posty.R
import com.bimatrix.posty.data.AndroidPostyStore
import com.bimatrix.posty.data.Task
import com.bimatrix.posty.data.TaskRepository
import com.bimatrix.posty.ui.dueLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** 위젯 리스트(여러 장)에 미완료 할 일을 우선순위 순으로 공급. */
class PostyWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        PostyRemoteViewsFactory(applicationContext)
}

/** 스티키 종이색(StickyPalette 와 동일) — RemoteViews 용 ARGB. */
private val PaperColors = intArrayOf(
    0xFFFFF3B0.toInt(), 0xFFC7F0DB.toInt(), 0xFFFFD8CC.toInt(),
    0xFFD6E8FF.toInt(), 0xFFE8DFF5.toInt(), 0xFFFFE3F1.toInt(),
)

private class PostyRemoteViewsFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<Task> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items = runBlocking {
            TaskRepository(AndroidPostyStore(context)).tasks.first()
                .filter { !it.isCompleted }
                .sortedWith(compareByDescending<Task> { it.pinned }.thenBy { it.order })
        }
    }

    override fun onDestroy() { items = emptyList() }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val task = items[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_item)
        rv.setTextViewText(R.id.widget_item_text, task.text)
        rv.setInt(
            R.id.widget_item_dot,
            "setBackgroundColor",
            PaperColors[((task.colorIndex % PaperColors.size) + PaperColors.size) % PaperColors.size],
        )
        val due = dueLabel(task.dueDate)
        if (due != null) {
            rv.setTextViewText(R.id.widget_item_due, due)
            rv.setViewVisibility(R.id.widget_item_due, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.widget_item_due, View.GONE)
        }
        // 행 탭 → 앱 열기(템플릿은 Provider 에서 설정).
        rv.setOnClickFillInIntent(R.id.widget_item_root, Intent())
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = items[position].id.hashCode().toLong()
    override fun hasStableIds(): Boolean = true
}
