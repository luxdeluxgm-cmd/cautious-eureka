package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

// To jest ten serwis, którego brakowało!
class RpgWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return RpgWidgetFactory(this.applicationContext)
    }
}

// A to fabryka, która buduje wiersze na liście
class RpgWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var questList: List<Quest> = emptyList()

    override fun onCreate() {
        // Ładujemy dane przy tworzeniu
        GameManager.loadGame(context)
        questList = GameManager.dailyQuests.toList()
    }

    override fun onDataSetChanged() {
        // To się wywołuje, gdy widget się odświeża
        GameManager.loadGame(context)
        questList = GameManager.dailyQuests.toList()
    }

    override fun onDestroy() { questList = emptyList() }
    override fun getCount(): Int = questList.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= questList.size) return RemoteViews(context.packageName, R.layout.widget_item)

        val quest = questList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)

        views.setTextViewText(R.id.tv_widget_task_name, quest.title)
        views.setTextViewText(R.id.tv_widget_xp, "${quest.xp} XP")

        // Zmiana ikony i koloru
        if (quest.isCompleted) {
            views.setImageViewResource(R.id.img_widget_status, android.R.drawable.checkbox_on_background)
            views.setTextColor(R.id.tv_widget_task_name, 0xFFAAAAAA.toInt())
        } else {
            views.setImageViewResource(R.id.img_widget_status, android.R.drawable.checkbox_off_background)
            views.setTextColor(R.id.tv_widget_task_name, 0xFF333333.toInt())
        }

        // === TU BYŁA ZMIANA ===
        // Kliknięcie w CAŁY pasek (tło, tekst, ikona) otwiera apkę
        val fillInIntent = Intent()
        // Ważne: ID musi pasować do LinearLayout w pliku widget_item.xml
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}