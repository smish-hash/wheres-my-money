package com.smish.wheresmymoney.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import com.smish.wheresmymoney.ui.theme.ExpenseTrackerTheme
import com.smish.wheresmymoney.util.CurrencyFormatter
import com.smish.wheresmymoney.util.toComposeColor

@Composable
fun ExpenseWidgetPixelCardPreview(
    data: WidgetData,
    modifier: Modifier = Modifier
) {
    val creamBg = Color(0xFFF7ECD9)
    val pixelInk = Color(0xFF1A1A1A)
    val pixelGreen = Color(0xFFC6F135)
    val pixelLightInk = Color(0xFF706A5C)

    // Outer padding prevents border clipping by launcher bounds
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        // Shadow offset block
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(pixelInk)
        )
        // Main card box with 2dp border
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(creamBg)
                .border(2.dp, pixelInk)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL SPENT • ${data.monthName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = pixelLightInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Plus Button (+)
                Box(
                    modifier = Modifier
                        .background(pixelGreen)
                        .border(1.dp, pixelInk)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ ADD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = pixelInk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Total Amount
            Text(
                text = CurrencyFormatter.format(data.grandTotal),
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = pixelInk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // Parent Categories Subtotals
            if (data.parentGroups.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    data.parentGroups.forEach { group ->
                        val color = try {
                            group.parent.colorHex.toComposeColor()
                        } catch (_: Exception) {
                            pixelGreen
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(color)
                            )
                            Spacer(Modifier.width(3.dp))
                            Column {
                                Text(
                                    text = group.parent.name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = pixelLightInk,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = CurrencyFormatter.format(group.subtotal),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = pixelInk,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7ECD9, widthDp = 340)
@Composable
fun ExpenseWidgetPreview() {
    val sampleData = WidgetData(
        monthName = "SEPTEMBER",
        grandTotal = 1245.50,
        parentGroups = listOf(
            ParentWidgetSubtotal(ParentCategoryEntity(id = 1, name = "Food", colorHex = "#C6F135"), 450.00),
            ParentWidgetSubtotal(ParentCategoryEntity(id = 2, name = "Bills", colorHex = "#6FA8DC"), 320.00),
            ParentWidgetSubtotal(ParentCategoryEntity(id = 3, name = "Fun", colorHex = "#D9603B"), 150.00),
            ParentWidgetSubtotal(ParentCategoryEntity(id = 4, name = "Other", colorHex = "#F2C14E"), 325.50)
        )
    )

    ExpenseTrackerTheme {
        Surface(modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.background) {
            ExpenseWidgetPixelCardPreview(data = sampleData)
        }
    }
}
