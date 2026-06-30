package com.bimatrix.posty.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.bimatrix.posty.MainActivity
import com.bimatrix.posty.R

/**
 * 홈 화면 위젯 — 미완료 할 일을 우선순위 순으로 '여러 장' 리스트로 보여준다.
 * 위젯을 크게 늘리면 더 많이 보이고, 넘치면 스크롤된다. 탭하면 앱이 열린다.
 */
class PostyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, id)) }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
    }

    companion object {
        /** 데이터 변경 시 저장소에서 호출 — 모든 위젯 인스턴스의 리스트를 갱신. */
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, PostyWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, id)) }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }

        private fun buildViews(context: Context, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_posty)

            // 리스트 어댑터(위젯 인스턴스별 고유 data URI 로 갱신 보장).
            val serviceIntent = Intent(context, PostyWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // 행 탭 → 앱 열기(공통 템플릿).
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setPendingIntentTemplate(R.id.widget_list, open)
            return views
        }
    }
}
