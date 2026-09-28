package com.smish.wheresmymoney.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.smish.wheresmymoney.ui.theme.PixelInk
import com.smish.wheresmymoney.util.DateUtils
import java.time.YearMonth
import androidx.compose.ui.tooling.preview.Preview
import com.smish.wheresmymoney.ui.theme.ExpenseTrackerTheme

@Composable
fun MonthSelector(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
        }
        Text(
            text = DateUtils.formatMonth(yearMonth).uppercase(),
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7ECD9)
@Composable
fun MonthSelectorPreview() {
    ExpenseTrackerTheme {
        MonthSelector(yearMonth = YearMonth.now(), onPrevious = {}, onNext = {})
    }
}
