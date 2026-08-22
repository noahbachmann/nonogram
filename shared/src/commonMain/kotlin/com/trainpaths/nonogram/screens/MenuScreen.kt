package com.trainpaths.nonogram.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.trainpaths.nonogram.navigation.AppBarMode
import com.trainpaths.nonogram.navigation.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trainpaths.nonogram.classes.Nonogram
import com.trainpaths.nonogram.screens.viewModel.MenuViewModel
import com.trainpaths.nonogram.classes.NonogramCard
import com.trainpaths.nonogram.classes.NonogramGrid
import com.trainpaths.nonogram.filter.FilterMenuButton

@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    onRefresh: () -> Unit,
    onNonogramClick: (Nonogram) -> Unit,
    onGenClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            showSettings = true,
            mode = AppBarMode.PUZZLE,
            onSwapMode = { onGenClick() },
            navigationContent = {
                FilterMenuButton(
                    entries = viewModel.filterEntries,
                    state = viewModel.filterSort,
                    onApply = viewModel::applyFilterSort,
                )
            },
        )

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = viewModel.isRefreshing,
                onRefresh = onRefresh,
                state = pullState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = viewModel.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = MaterialTheme.colorScheme.primary,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
            ) {
                val visible = viewModel.visibleNonograms
                val showAllNames by viewModel.showNames.collectAsState()
                NonogramGrid {
                    items(visible) { nonogram ->
                        NonogramCard(
                            nonogram = nonogram,
                            progress = viewModel.getProgress(nonogram.id, nonogram.height, nonogram.width),
                            beatCount = viewModel.getBeatCount(nonogram.id),
                            isOwn = nonogram.isOwned(viewModel.userId),
                            alwaysShowName = showAllNames,
                            onClick = { onNonogramClick(nonogram) })
                    }
                }
                if (visible.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No puzzles match",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}
