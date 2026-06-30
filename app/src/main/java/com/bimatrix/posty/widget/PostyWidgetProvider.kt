package com.bimatrix.posty.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.bimatrix.posty.MainActivity
import com.bimatrix.posty.R
import com.bimatrix.posty.data.AndroidPostyStore
import com.bimatrix.posty.data.TaskRepository
import com.bimatrix.posty.ui.dueLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 홈 화면 위젯 — 우선순위 최상위 할 일을 포스트잇처럼 보여준다.
 * 크기 조절 가능(가로/세로). 탭하면 앱이 열린다.
 */
class PostyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> manager.updateAppWidget(id, buildViews(context)) }
    }

    companion object {
        /** 데이터 변경 시 저장소에서 호출 — 모든 위젯 인스턴스를 갱신. */
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, PostyWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { id -> manager.updateAppWidget(id, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val active = runBlocking {
                TaskRepository(AndroidPostyStore(context)).tasks.first()
                    .filter { !it.isCompleted }
                    .sortedBy { it.order }
            }
            val views = RemoteViews(context.packageName, R.layout.widget_posty)

            val top = active.firstOrNull()
            if (top == null) {
                views.setTextViewText(R.id.widget_task, "할 일이 없어요 :)")
                views.setTextViewText(R.id.widget_due, "")
                views.setTextViewText(R.id.widget_more, "")
            } else {
                views.setTextViewText(R.id.widget_task, top.text)
                views.setTextViewText(R.id.widget_due, dueLabel(top.dueDate) ?: "")
                val remaining = active.size - 1
                views.setTextViewText(R.id.widget_more, if (remaining > 0) "외 ${remaining}장 더" else "")
            }

            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            return views
        }
    }
}
