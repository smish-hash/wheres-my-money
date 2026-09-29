package com.smish.wheresmymoney.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.smish.wheresmymoney.MainActivity
import com.smish.wheresmymoney.data.local.ExpenseDatabase
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import com.smish.wheresmymoney.util.CurrencyFormatter
import com.smish.wheresmymoney.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.YearMonth

data class ParentWidgetSubtotal(
    val parent: ParentCategoryEntity,
    val subtotal: Double
)

data class WidgetData(
    val monthName: String,
    val grandTotal: Double,
    val parentGroups: List<ParentWidgetSubtotal>
)

class ExpenseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = loadWidgetData(context)

        provideContent {
            WidgetContent(context, widgetData)
        }
    }

    private suspend fun loadWidgetData(context: Context): WidgetData {
        return try {
            val database = ExpenseDatabase.getInstance(context)
            val ym = YearMonth.now()
            val (start, end) = DateUtils.monthRange(ym)

            val parents = database.parentCategoryDao().getAllOnce()
            val categories = database.categoryDao().getAllOnce()
            val totals = database.expenseDao().getCategoryTotals(start, end).first()

            val totalsMap = totals.associateBy({ it.categoryId }, { it.total })
            val byParent = categories.groupBy { it.parentCategoryId }

            val groups = parents.sortedBy { it.sortOrder }.map { parent ->
                val cats = byParent[parent.id] ?: emptyList()
                val subtotal = cats.sumOf { totalsMap[it.id] ?: 0.0 }
                ParentWidgetSubtotal(parent, subtotal)
            }

            val ungroupedCats = byParent[null] ?: emptyList()
            val ungroupedTotal = ungroupedCats.sumOf { totalsMap[it.id] ?: 0.0 }
            val grandTotal = groups.sumOf { it.subtotal } + ungroupedTotal

            WidgetData(
                monthName = ym.month.name.uppercase(),
                grandTotal = grandTotal,
                parentGroups = groups.filter { it.subtotal > 0 || groups.size <= 4 }.take(4)
            )
        } catch (_: Exception) {
            WidgetData(
                monthName = YearMonth.now().month.name.uppercase(),
                grandTotal = 0.0,
                parentGroups = emptyList()
            )
        }
    }

    @Composable
    private fun WidgetContent(context: Context, data: WidgetData) {
        val creamBg = Color(0xFFF7ECD9)
        val pixelInk = Color(0xFF1A1A1A)
        val pixelGreen = Color(0xFFC6F135)
        val pixelLightInk = Color(0xFF706A5C)

        val size = LocalSize.current
        val isCompactHeight = size.height < 100.dp
        val isCompactWidth = size.width < 220.dp

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "add_expense")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Outer container with padding so launcher cell edges never clip the widget
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            // Layer 1: Black shadow block (offset to bottom-right by 3dp)
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(top = 3.dp, start = 3.dp)
                    .background(pixelInk)
            ) {}

            // Layer 2: Black border frame (padding 2dp creates the 2dp black border around all 4 sides)
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(bottom = 3.dp, end = 3.dp)
                    .background(pixelInk)
                    .padding(2.dp)
            ) {
                // Layer 3: Inner Cream Card Container
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(creamBg)
                        .padding(10.dp)
                        .clickable(actionStartActivity(openAppIntent))
                ) {
                    // Header Row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "TOTAL SPENT • ${data.monthName}",
                                style = TextStyle(
                                    color = ColorProvider(day = pixelLightInk, night = pixelLightInk),
                                    fontSize = if (isCompactWidth) 10.sp else 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Box(
                            modifier = GlanceModifier
                                .background(pixelGreen)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .clickable(actionStartActivity(addExpenseIntent)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ ADD",
                                style = TextStyle(
                                    color = ColorProvider(day = pixelInk, night = pixelInk),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    if (!isCompactHeight) {
                        Spacer(GlanceModifier.defaultWeight())
                    }

                    // Grand Total
                    Text(
                        text = CurrencyFormatter.format(data.grandTotal),
                        style = TextStyle(
                            color = ColorProvider(day = pixelInk, night = pixelInk),
                            fontSize = if (isCompactHeight) 22.sp else 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Show Parent Categories Subtotals if there is enough height
                    if (!isCompactHeight && data.parentGroups.isNotEmpty()) {
                        Spacer(GlanceModifier.defaultWeight())

                        val displayedGroups = if (isCompactWidth) data.parentGroups.take(2) else data.parentGroups

                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            displayedGroups.forEachIndexed { index, group ->
                                if (index > 0) {
                                    Spacer(GlanceModifier.width(6.dp))
                                }

                                val colorInt = try {
                                    android.graphics.Color.parseColor(group.parent.colorHex)
                                } catch (_: Exception) {
                                    android.graphics.Color.parseColor("#C6F135")
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = GlanceModifier.defaultWeight()
                                ) {
                                    Box(
                                        modifier = GlanceModifier
                                            .width(7.dp)
                                            .height(7.dp)
                                            .background(Color(colorInt))
                                    ) {}
                                    Spacer(GlanceModifier.width(3.dp))
                                    Column {
                                        Text(
                                            text = group.parent.name.uppercase(),
                                            style = TextStyle(
                                                color = ColorProvider(day = pixelLightInk, night = pixelLightInk),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(group.subtotal),
                                            style = TextStyle(
                                                color = ColorProvider(day = pixelInk, night = pixelInk),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
