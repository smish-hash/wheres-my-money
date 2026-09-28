package com.smish.wheresmymoney.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smish.wheresmymoney.ui.theme.PixelInk
import com.smish.wheresmymoney.ui.theme.ExpenseTrackerTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview

/** The "stacked card" look: a solid black block offset behind a bordered card. */
@Composable
fun BoxScope.PixelCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: PaddingValues = PaddingValues(ExpenseTrackerTheme.spacing.l),
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val spacing = ExpenseTrackerTheme.spacing
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = spacing.xs, y = spacing.xs)
                .background(MaterialTheme.colorScheme.onSurface)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .border(BorderStroke(2.dp, borderColor))
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

/** A non-BoxScope variant for top-level usage where matchParentSize can be simulated or outer Box handles it */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: PaddingValues = PaddingValues(ExpenseTrackerTheme.spacing.l),
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val spacing = ExpenseTrackerTheme.spacing
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = spacing.xs, y = spacing.xs)
                .background(MaterialTheme.colorScheme.onSurface)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .border(BorderStroke(2.dp, borderColor))
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PixelCardPreview() {
    ExpenseTrackerTheme {
        Box(Modifier.padding(ExpenseTrackerTheme.spacing.xl)) {
            PixelCard {
                Text("This is a pixel card", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
