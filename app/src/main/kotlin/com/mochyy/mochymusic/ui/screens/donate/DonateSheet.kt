package com.mochyy.mochymusic.ui.screens.donate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.mochyy.mochymusic.ui.screens.settings.devspace.openDevUrl
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.PopupSheetState
fun PopupSheetState.showDonateSheet() {
    show { PersonalSupport() }
}
@Composable private fun PersonalSupport() {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.app_name), color = androidx.compose.ui.graphics.Color.White)
        Text(stringResource(R.string.toto_support), color = androidx.compose.ui.graphics.Color.White)
        Image(painterResource(R.drawable.toto_donate_qr), stringResource(R.string.toto_qr), Modifier.fillMaxWidth().aspectRatio(1080f / 1920f))
        Text(stringResource(R.string.toto_contact), color = androidx.compose.ui.graphics.Color.White)
        listOf(R.string.toto_owner_social_label to R.string.toto_owner_social,
            R.string.toto_owner_instagram_label to R.string.toto_owner_instagram,
            R.string.toto_owner_community_label to R.string.toto_owner_community).forEach { (label, link) ->
            val url = stringResource(link)
            if (url.isNotBlank()) TextButton(onClick = { context.openDevUrl(url) }) { Text(stringResource(label)) }
        }
    }
}
