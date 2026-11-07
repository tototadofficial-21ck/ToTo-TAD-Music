// the developer playground page: what the repo did since this build, its issues, and the experimental settings

package com.mochyy.mochymusic.ui.screens.settings.devspace

import com.mochyy.mochymusic.i18n.UiText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.screens.settings.CategoryBubble

const val DevPlaygroundRoute = "developer_playground"

@Composable
fun DevPlaygroundScreen(navController: NavController) {
    val stats by DevSpaceRepository.current.collectAsState()
    val loading by DevSpaceRepository.loading.collectAsState()
    val error by DevSpaceRepository.error.collectAsState()
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { DevSpaceRepository.refresh(BuildConfig.BUILD_TIME_MILLIS, force = reload > 0) }
    val settled = stats != null || !loading

    SubSettingsScaffold(
        title = UiText.get(R.string.toto_ui_more_developer_playground_101e3f),
        backLabel = "Dev Spaces",
        onBack = { navController.navigateUp() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RepoFiguresCard(
                title = UiText.get(R.string.toto_ui_more_since_this_build_8433ca),
                subtitle = UiText.get(R.string.toto_ui_more_from_when_this_build_was_made_until_today_d56451),
                figures = listOf(
                    stats?.newIssues to "new reported issues",
                    stats?.newResolved to "new resolved issue",
                    stats?.commits to "commits",
                    stats?.releases to "releases",
                ),
                settled = settled,
            )
            RepoFiguresCard(
                title = UiText.get(R.string.toto_ui_more_in_total_29a05c),
                figures = listOf(
                    stats?.reportedTotal to "reported issues",
                    stats?.closedTotal to "resolved issue",
                ),
                settled = settled,
            )
            RepoIssuesSection(stats = stats, loading = loading, error = error, onRetry = { reload++ })
            Spacer(Modifier.height(6.dp))
            CategoryBubble(
                title = UiText.get(R.string.toto_ui_more_experimental_settings_2502ec),
                iconRes = R.drawable.biotech,
                onClick = { navController.navigate("experimental_settings") },
            )
        }
    }
}
