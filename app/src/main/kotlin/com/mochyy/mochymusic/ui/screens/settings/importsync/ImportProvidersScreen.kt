package com.mochyy.mochymusic.ui.screens.settings.importsync

import com.mochyy.mochymusic.i18n.UiText

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.importer.LinkSource
import com.mochyy.mochymusic.importer.account.AccountService
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.viewmodels.ImportSyncViewModel

@Composable
fun ImportProvidersScreen(
    navController: NavController,
    viewModel: ImportSyncViewModel = hiltViewModel(),
) {
    var linkSource by rememberSaveable { mutableStateOf<LinkSource?>(null) }

    // some file managers label .csv as text/plain or octet-stream, so don't filter too hard
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.loadFile(uri, sourceLabel = "Mochy backup")
    }

    SubSettingsScaffold(
        title = UiText.get(R.string.toto_setup_import),
        onBack = { navController.navigateUp() },
    ) {
        SettingsGroup(
            title = UiText.get(R.string.toto_ui_more_sign_in_and_choose_8fc871),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                AccountService.SPOTIFY to "Liked Songs and your playlists",
                AccountService.APPLE_MUSIC to "Library playlists, Favorite Songs and your whole library",
                AccountService.YOUTUBE_MUSIC to "Liked music and your playlists, exact songs · also supports sync",
                AccountService.TIDAL to "My Tracks and your playlists",
            ).map { (service, description) ->
                SettingsItem(
                    title = { Text(service.label) },
                    highlightKey = "${service.label} account",
                    descriptionText = description,
                    icon = painterResource(R.drawable.login),
                    iconShape = CircleShape,
                    onClick = { navController.navigate(accountImportRoute(service)) },
                )
            },
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = UiText.get(R.string.toto_ui_more_or_paste_a_public_link_627fa6),
            style = SettingsGroupStyle.Grouped,
            items = LinkSource.entries.map { source ->
                SettingsItem(
                    title = { Text(source.label) },
                    highlightKey = source.label,
                    descriptionText = when (source) {
                        LinkSource.SPOTIFY -> "Public playlists (first 100 songs) and albums"
                        LinkSource.APPLE_MUSIC -> "Public playlists and albums"
                        LinkSource.DEEZER -> "Public playlists and albums, any length"
                        LinkSource.YOUTUBE -> "Playlists and albums, exact songs, no searching"
                    },
                    icon = painterResource(R.drawable.link),
                    iconShape = CircleShape,
                    onClick = { linkSource = source },
                )
            },
        )


        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = UiText.get(R.string.toto_ui_more_from_a_file_e91bc6),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                SettingsItem(
                    title = { Text(UiText.get(R.string.toto_ui_more_mochy_backup_f93cb0)) },
                    highlightKey = "Mochy backup",
                    descriptionText = "A CSV from Export to Mochy CSV. Restores exact songs",
                    icon = painterResource(R.drawable.backup),
                    iconShape = CircleShape,
                    onClick = { backupPicker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) },
                ),
                SettingsItem(
                    title = { Text(UiText.get(R.string.toto_ui_more_tunemymusic_csv_85cdbd)) },
                    highlightKey = "TuneMyMusic CSV",
                    descriptionText = "Private playlists, liked songs, and every other service",
                    icon = painterResource(R.drawable.web_link),
                    iconShape = CircleShape,
                    onClick = { navController.navigate(ImportTuneMyMusicRoute) },
                ),
            ),
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = UiText.get(R.string.toto_ui_more_other_services_4c1838),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                SettingsItem(
                    title = { Text(UiText.get(R.string.toto_ui_more_amazon_music_soundcloud_pandora_06af5e)) },
                    descriptionText = "Goes through TuneMyMusic",
                    detail = "These go through TuneMyMusic, which can sign in to them for you",
                    icon = painterResource(R.drawable.library_music),
                    iconShape = CircleShape,
                    onClick = { navController.navigate(ImportTuneMyMusicRoute) },
                ),
            ),
        )
    }

    linkSource?.let { source ->
        LinkInputDialog(
            source = source,
            onDismiss = { linkSource = null },
            onSubmit = { link ->
                linkSource = null
                viewModel.loadLink(link)
            },
        )
    }

    PendingImportHost(viewModel = viewModel, navController = navController)
}
