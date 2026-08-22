package com.trainpaths.nonogram.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trainpaths.nonogram.classes.DrawNonogram
import com.trainpaths.nonogram.classes.UNNAMED_NONOGRAM_TITLE
import com.trainpaths.nonogram.navigation.TopAppBar
import com.trainpaths.nonogram.screens.viewModel.AdminViewModel
import com.trainpaths.nonogram.sync.PendingReview

@Composable
fun AdminScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = "Admin panel", onBack = onBack, backArrow = true)

        val review = adminViewModel.current
        when {
            adminViewModel.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }

            review == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No pending requests.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            else -> ReviewCard(
                review = review,
                isDeciding = adminViewModel.isDeciding,
                error = adminViewModel.error,
                onAccept = { adminViewModel.accept() },
                onDeny = { adminViewModel.deny() },
            )
        }
    }
}

@Composable
private fun ReviewCard(
    review: PendingReview,
    isDeciding: Boolean,
    error: String?,
    onAccept: () -> Unit,
    onDeny: () -> Unit,
) {
    val nonogram = review.nonogram
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            nonogram.name ?: UNNAMED_NONOGRAM_TITLE,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            "${nonogram.difficulty.label} · ${nonogram.width}x${nonogram.height}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            DrawNonogram(nonogram.solution)
        }

        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = onDeny,
                enabled = !isDeciding,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryFixed,
                    contentColor = MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text("Deny", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = onAccept,
                enabled = !isDeciding,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onTertiary,
                    contentColor = MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text("Accept", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
