package com.smish.wheresmymoney.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()

    companion object {
        fun updateWidget(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                ExpenseWidget().updateAll(context.applicationContext)
            }
        }
    }
}
