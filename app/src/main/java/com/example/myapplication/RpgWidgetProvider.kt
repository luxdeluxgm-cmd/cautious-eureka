package com.example.myapplication

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class RpgWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // 1. INTENT DO OTWIERANIA APLIKACJI
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // 2. PODPINAMY KLIKANIE WSZĘDZIE
            // Klik w nagłówek
            views.setOnClickPendingIntent(R.id.ll_widget_header, appPendingIntent)
            // Klik w tło widgetu (jakby lista była pusta albo mała)
            views.setOnClickPendingIntent(R.id.ll_widget_root, appPendingIntent)
            // Klik w tytuł (dla pewności)
            views.setOnClickPendingIntent(R.id.tv_widget_title, appPendingIntent)

            // 3. OBSŁUGA LISTY
            val intent = Intent(context, RpgWidgetService::class.java)
            views.setRemoteAdapter(R.id.widget_list_view, intent)
            views.setEmptyView(R.id.widget_list_view, R.id.appwidget_empty_view)

            // 4. SZABLON KLIKANIA (To musi być podpięte pod LISTĘ - R.id.widget_list_view)
            // ... (reszta kodu wyżej bez zmian) ...

            // 4. SZABLON KLIKANIA (POPRAWIONY)
            val clickIntentTemplate = Intent(context, MainActivity::class.java).apply {
                // TA LINIJKA JEST KLUCZOWA!
                // Bez unikalnej akcji Android ignoruje kliknięcia w liście
                action = "OTWORZ_QUESTA"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val clickPendingIntent = PendingIntent.getActivity(
                context,
                1, // Request code musi być unikalny (inny niż w nagłówku, tam było 0)
                clickIntentTemplate,
                // FLAG_MUTABLE jest tu niezbędne!
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setPendingIntentTemplate(R.id.widget_list_view, clickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}