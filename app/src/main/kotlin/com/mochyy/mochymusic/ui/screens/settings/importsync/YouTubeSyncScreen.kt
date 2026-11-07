package com.mochyy.mochymusic.ui.screens.settings.importsync

import com.mochyy.mochymusic.i18n.UiText

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.AccountEmailKey
import com.mochyy.mochymusic.constants.AccountNameKey
import com.mochyy.mochymusic.constants.YtExcludeImportsKey
import com.mochyy.mochymusic.constants.YtPullHistoryKey
import com.mochyy.mochymusic.constants.YtReportPlaysKey
import com.mochyy.mochymusic.constants.YtSyncNewPlaylistsKey
import com.mochyy.mochymusic.constants.YtmSyncKey
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.DefaultDialog
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.utils.rememberPreference
import com.mochyy.mochymusic.viewmodels.ImportSyncViewModel

@Composable
fun YouTubeSyncScreen(
    navController: NavController,
    viewModel: ImportSyncViewModel = hiltViewModel(),
) {
    val signedIn = rememberYouTubeSignedIn()
    val accountName by rememberPreference(AccountNameKey, "")
    val accountEmail by rememberPreference(AccountEmailKey, "")
    val (autoSync, onAutoSyncChange) = rememberPreference(YtmSyncKey, true)
    val (syncNewPlaylists, onSyncNewPlaylistsChange) = rememberPreference(YtSyncNewPlaylistsKey, false)
    val (reportPlays, onReportPlaysChange) = rememberPreference(YtReportPlaysKey, true)
    val (pullHistory, onPullHistoryChange) = rememberPreference(YtPullHistoryKey, false)
    val (excludeImports, onExcludeImportsChange) = rememberPreference(YtExcludeImportsKey, false)
    val state by viewModel.youTubeState.collectAsState()

    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmUpload by remember { mutableStateOf(false) }
    var confirmUndo by remember { mutableStateOf(false) }

    SubSettingsScaffold(
        title = UiText.get(R.string.action_sync),
        onBack = { navController.navigateUp() },
    ) {
        SettingsGroup(
            title = UiText.get(R.string.toto_ui_more_youtube_account_cba2dc),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                if (signedIn) {
                    SettingsItem(
                        title = { Text(accountName.ifBlank { UiText.get(R.string.toto_youtube_linked) }) },
                        descriptionText = accountEmail.ifBlank { "YouTube Music" },
                        icon = painterResource(R.drawable.account),
                        iconShape = CircleShape,
                        trailingContent = {
                            TextButton(onClick = { confirmSignOut = true }) { Text(UiText.get(R.string.scrobble_sign_out)) }
                        },
                    )
                } else {
                    SettingsItem(
                        title = { Text(UiText.get(R.string.toto_ui_more_sign_in_to_youtube_2bcb0f)) },
                        descriptionText = UiText.get(R.string.toto_youtube_sync_hint),
                        icon = painterResource(R.drawable.login),
                        iconShape = CircleShape,
                        onClick = { navController.navigate(YouTubeLoginRoute) },
                    )
                }
            ),
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = UiText.get(R.string.toto_ui_more_keep_in_sync_efe09d),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                toggleItem(
                    title = UiText.get(R.string.toto_setup_sync_library_on_launch),
                    description = UiText.get(R.string.toto_ui_more_likes_library_artists_and_saved_playlists_from_youtu_b4b953),
                    icon = R.drawable.sync,
                    checked = autoSync,
                    enabled = signedIn,
                    onChange = onAutoSyncChange,
                ),
                toggleItem(
                    title = UiText.get(R.string.toto_ui_more_new_playlists_go_to_youtube_14dae0),
                    description = UiText.get(R.string.toto_ui_more_playlists_you_create_here_are_created_on_youtube_too_c87ec7),
                    icon = R.drawable.playlist_add,
                    checked = syncNewPlaylists,
                    enabled = signedIn,
                    onChange = onSyncNewPlaylistsChange,
                ),
                toggleItem(
                    title = UiText.get(R.string.toto_setup_push_plays_to_youtube_history),
                    description = UiText.get(R.string.toto_ui_more_songs_you_play_here_show_up_in_your_youtube_history_abed89),
                    icon = R.drawable.history,
                    checked = reportPlays,
                    enabled = signedIn,
                    onChange = onReportPlaysChange,
                ),
                toggleItem(
                    title = UiText.get(R.string.toto_ui_more_copy_youtube_history_here_1aa455),
                    description = UiText.get(R.string.toto_ui_more_plays_from_your_other_devices_join_mochy_s_history_o_1572c9),
                    icon = R.drawable.history,
                    checked = pullHistory,
                    enabled = signedIn,
                    onChange = onPullHistoryChange,
                ),
            ),
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = UiText.get(R.string.toto_ui_more_other_apps_977376),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                toggleItem(
                    title = stringResource(R.string.yt_sync_exclude_title),
                    description = stringResource(R.string.yt_sync_exclude_desc),
                    icon = R.drawable.cloud,
                    checked = excludeImports,
                    enabled = true,
                    onChange = onExcludeImportsChange,
                ),
            ),
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = UiText.get(R.string.toto_ui_more_now_e3b820),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                SettingsItem(
                    title = { Text(UiText.get(R.string.toto_ui_more_sync_now_2b7d93)) },
                    descriptionText = if (pullHistory) "Library, playlists and history" else "Library and playlists",
                    icon = painterResource(R.drawable.refresh),
                    iconShape = CircleShape,
                    enabled = signedIn && !state.running,
                    onClick = viewModel::syncNow,
                ),
                SettingsItem(
                    title = { Text(UiText.get(R.string.toto_ui_more_copy_youtube_history_now_83eb07)) },
                    descriptionText = "Only adds plays Mochy doesn't have yet",
                    icon = painterResource(R.drawable.history),
                    iconShape = CircleShape,
                    enabled = signedIn && !state.running,
                    onClick = viewModel::pullHistoryNow,
                ),
                SettingsItem(
                    title = { Text(stringResource(R.string.yt_sync_undo_title)) },
                    descriptionText = stringResource(R.string.yt_sync_undo_desc),
                    icon = painterResource(R.drawable.undo),
                    iconShape = CircleShape,
                    enabled = signedIn && state.canUndo && !state.running,
                    onClick = { confirmUndo = true },
                ),
                SettingsItem(
                    title = { Text(UiText.get(R.string.toto_ui_more_put_local_playlists_on_youtube_056e25)) },
                    descriptionText = "Copy local playlists to YouTube",
                    detail = "Creates a YouTube copy of every playlist that only exists here",
                    icon = painterResource(R.drawable.cloud),
                    iconShape = CircleShape,
                    enabled = signedIn && !state.running,
                    onClick = { confirmUpload = true },
                ),
            ),
        )

        if (state.running || state.message != null) {
            Spacer(Modifier.height(16.dp))
            ImportInfoCard {
                if (state.running) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(state.label, style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    Text(state.message.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = viewModel::clearYouTubeMessage) { Text("OK") }
                }
            }
        }

        if (!signedIn) {
            Spacer(Modifier.height(16.dp))
            Text(
                UiText.get(R.string.toto_ui_more_import_from_a_youtube_playlist_link_doesn_t_need_an__8c5b3c),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmSignOut) {
        DefaultDialog(
            onDismiss = { confirmSignOut = false },
            horizontalAlignment = Alignment.Start,
            title = { Text(UiText.get(R.string.toto_ui_more_sign_out_of_youtube_af1cbf)) },
            buttons = {
                TextButton(onClick = { confirmSignOut = false }) { Text(UiText.get(R.string.cancel)) }
                TextButton(onClick = {
                    confirmSignOut = false
                    viewModel.signOut()
                }) { Text(UiText.get(R.string.scrobble_sign_out)) }
            },
        ) {
            Text(
                UiText.get(R.string.toto_ui_more_everything_already_in_your_library_stays_sync_stops__63dd60),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    if (confirmUndo) {
        DefaultDialog(
            onDismiss = { confirmUndo = false },
            horizontalAlignment = Alignment.Start,
            title = { Text(stringResource(R.string.yt_sync_undo_dialog_title)) },
            buttons = {
                TextButton(onClick = { confirmUndo = false }) { Text(UiText.get(R.string.cancel)) }
                TextButton(onClick = {
                    confirmUndo = false
                    viewModel.undoLastSync(withYouTube = false)
                }) { Text(stringResource(R.string.yt_sync_undo_phone)) }
                TextButton(
                    enabled = signedIn,
                    onClick = {
                        confirmUndo = false
                        viewModel.undoLastSync(withYouTube = true)
                    },
                ) { Text(stringResource(R.string.yt_sync_undo_both)) }
            },
        ) {
            Text(
                stringResource(R.string.yt_sync_undo_dialog_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.yt_sync_undo_both_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmUpload) {
        DefaultDialog(
            onDismiss = { confirmUpload = false },
            horizontalAlignment = Alignment.Start,
            title = { Text(UiText.get(R.string.toto_ui_more_put_local_playlists_on_youtube_7babc1)) },
            buttons = {
                TextButton(onClick = { confirmUpload = false }) { Text(UiText.get(R.string.cancel)) }
                TextButton(onClick = {
                    confirmUpload = false
                    viewModel.uploadLocalPlaylists()
                }) { Text(UiText.get(R.string.create_room)) }
            },
        ) {
            Text(
                UiText.get(R.string.toto_ui_more_each_playlist_that_only_exists_in_mochy_gets_a_priva_018f73),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun toggleItem(
    title: String,
    description: String,
    icon: Int,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) = SettingsItem(
    title = { Text(title) },
    highlightKey = title,
    descriptionText = description,
    icon = painterResource(icon),
    iconShape = CircleShape,
    enabled = enabled,
    onClick = { onChange(!checked) },
    trailingContent = { AppSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled) },
)
