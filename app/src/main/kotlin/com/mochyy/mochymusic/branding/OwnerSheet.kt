package com.mochyy.mochymusic.branding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.PopupSheetState
import com.mochyy.mochymusic.ui.screens.donate.showSupportSheet
import com.mochyy.mochymusic.ui.screens.settings.devspace.openDevUrl

fun PopupSheetState.showOwnerSheet(onCredits: () -> Unit = {}) {
    val popup = this
    show { OwnerContent(popup, onCredits) }
}

@Composable
private fun OwnerContent(popup: PopupSheetState, onCredits: () -> Unit) {
    val context = LocalContext.current
    val links = listOf(
        R.string.toto_owner_website_label to R.string.toto_owner_website,
        R.string.toto_owner_social_label to R.string.toto_owner_social,
        R.string.toto_owner_instagram_label to R.string.toto_owner_instagram,
        R.string.toto_owner_community_label to R.string.toto_owner_community,
        R.string.toto_owner_contact_label to R.string.toto_owner_contact,
        R.string.toto_owner_repo_label to R.string.toto_owner_repo,
    ).map { stringResource(it.first) to stringResource(it.second) }.filter { it.second.isNotBlank() }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Image(painterResource(R.drawable.toto_logo), null, Modifier.size(112.dp))
        Text(stringResource(R.string.app_name))
        Text(stringResource(R.string.toto_owner_bio))
        links.forEach { (label, url) -> TextButton(onClick = { context.openDevUrl(url) }) { Text(label) } }
        if (links.isEmpty()) Text(stringResource(R.string.toto_owner_links_pending))
        TextButton(onClick = { popup.showSupportSheet() }) { Text(stringResource(R.string.toto_owner_support)) }
        TextButton(onClick = onCredits) { Text(stringResource(R.string.toto_upstream_credits)) }
    }
}
