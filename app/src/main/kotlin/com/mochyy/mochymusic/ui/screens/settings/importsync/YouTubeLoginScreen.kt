package com.mochyy.mochymusic.ui.screens.settings.importsync

import com.mochyy.mochymusic.i18n.UiText

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.mochyy.mochymusic.LocalPlayerAwareWindowInsets
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.AccountChannelHandleKey
import com.mochyy.mochymusic.constants.AccountEmailKey
import com.mochyy.mochymusic.constants.AccountNameKey
import com.mochyy.mochymusic.constants.DataSyncIdKey
import com.mochyy.mochymusic.constants.InnerTubeCookieKey
import com.mochyy.mochymusic.constants.VisitorDataKey
import com.mochyy.mochymusic.utils.dataStore
import com.music.innertube.YouTube
import com.music.innertube.models.AccountInfo
import com.music.innertube.utils.parseCookieString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.mochyy.mochymusic.ui.utils.stableStatusBarsPadding

private const val LOGIN_URL = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"

/** reads the session ids youtube music keeps in its page config */
private const val READ_CONFIG_JS = """
(function() {
  try {
    var get = function(k) {
      if (window.ytcfg && ytcfg.get) { var v = ytcfg.get(k); if (v) return v; }
      if (window.yt && yt.config_ && yt.config_[k]) return yt.config_[k];
      return null;
    };
    return { visitorData: get('VISITOR_DATA'), dataSyncId: get('DATASYNC_ID') };
  } catch (e) { return null; }
})();
"""

/** the full-screen sign-in page used from the sync settings */
@Composable
fun YouTubeLoginScreen(navController: NavController) {
    val context = LocalContext.current
    var pageLoading by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .stableStatusBarsPadding()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = UiText.get(R.string.back_button_desc), tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(UiText.get(R.string.toto_ui_more_sign_in_to_youtube_2bcb0f), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    UiText.get(R.string.toto_ui_more_google_s_own_sign_in_page_mochy_never_sees_your_pass_e4c1d4),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
        Box(modifier = Modifier.height(2.dp).fillMaxWidth()) {
            if (pageLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        YouTubeLoginWebContent(
            onSignedIn = { info ->
                Toast.makeText(context, info?.name?.let { UiText.get(R.string.toto_setup_signed_in_as, it) } ?: UiText.get(R.string.toto_youtube_linked), Toast.LENGTH_SHORT).show()
                navController.navigateUp()
            },
            onExit = { navController.navigateUp() },
            onLoadingChange = { pageLoading = it },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

/** google sign-in in a webview without chrome, once signed in on music.youtube.com it saves the session where the app reads it */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLoginWebContent(
    onSignedIn: (AccountInfo?) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    onUrlChange: (String) -> Unit = {},
    onLoadingChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)
    val currentOnUrlChange by rememberUpdatedState(onUrlChange)
    val currentOnLoadingChange by rememberUpdatedState(onLoadingChange)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var saving by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var loginMessage by remember { mutableStateOf<Int?>(null) }
    fun diagnostic(event: String) {
        if (BuildConfig.DEBUG) android.util.Log.i("ToToYouTubeLogin", "event=$event")
    }

    fun finishLogin(view: WebView, url: String, manual: Boolean = false) {
        if (saving || finished) return
        val page = android.net.Uri.parse(url)
        if (page.scheme != "https" || page.host != "music.youtube.com") {
            if (manual && page.scheme == "https" && page.host in setOf("youtube.com", "www.youtube.com", "m.youtube.com")) {
                view.loadUrl("https://music.youtube.com")
            } else if (manual) loginMessage = R.string.toto_youtube_finish_google
            return
        }
        val cookie = CookieManager.getInstance().getCookie("https://music.youtube.com").orEmpty()
        if (runCatching { parseCookieString(cookie)["SAPISID"].isNullOrBlank() }.getOrDefault(true)) {
            diagnostic("COOKIE_PENDING")
            if (manual) loginMessage = R.string.toto_youtube_wait_cookie
            return
        }
        loginMessage = null
        diagnostic("VERIFYING")
        saving = true
        view.evaluateJavascript(READ_CONFIG_JS) { raw ->
            val config = runCatching { JSONObject(raw) }.getOrNull()
            val visitorData = config?.optString("visitorData")?.takeIf { it.isNotBlank() && it != "null" }
            val dataSyncId = config?.optString("dataSyncId")?.takeIf { it.isNotBlank() && it != "null" }
            scope.launch(Dispatchers.IO) {
                val previousCookie = YouTube.cookie
                val previousVisitor = YouTube.visitorData
                val previousSync = YouTube.dataSyncId
                try {
                    // account details need the new session, the app would set these from prefs anyway
                    YouTube.cookie = cookie
                    visitorData?.let { YouTube.visitorData = it }
                    YouTube.dataSyncId = dataSyncId?.let {
                        it.takeIf { !it.contains("||") }
                            ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                            ?: it.substringAfter("||")
                    }
                    val info = YouTube.accountInfo().getOrThrow()
                    context.dataStore.edit { settings ->
                        settings[InnerTubeCookieKey] = cookie
                        visitorData?.let { settings[VisitorDataKey] = it }
                        dataSyncId?.let { settings[DataSyncIdKey] = it }
                        settings[AccountNameKey] = info.name
                        settings[AccountEmailKey] = info.email.orEmpty()
                        settings[AccountChannelHandleKey] = info.channelHandle.orEmpty()
                    }
                    withContext(Dispatchers.Main) {
                        CookieManager.getInstance().flush()
                        diagnostic("LINKED")
                        finished = true
                        currentOnSignedIn(info)
                    }
                } catch (e: Exception) {
                    if (YouTube.cookie == cookie) {
                        YouTube.cookie = previousCookie
                        YouTube.visitorData = previousVisitor
                        YouTube.dataSyncId = previousSync
                    }
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    diagnostic(when {
                        e is java.io.IOException -> "NETWORK_FAILURE"
                        e is NullPointerException || e is kotlinx.serialization.SerializationException -> "ACCOUNT_RESPONSE_FAILURE"
                        e is io.ktor.client.plugins.ResponseException -> "ACCOUNT_HTTP_FAILURE"
                        else -> "ACCOUNT_VERIFICATION_FAILED"
                    })
                    withContext(Dispatchers.Main) {
                        saving = false
                        loginMessage = R.string.toto_youtube_verify_error
                    }
                }
            }
        }
    }

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack() && !saving) view.goBack() else onExit()
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                destroy()
            }
            webView = null
        }
    }

    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(UiText.get(loginMessage ?: R.string.toto_youtube_finish_hint), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(enabled = !saving && !finished, onClick = {
                webView?.let { finishLogin(it, it.url.orEmpty(), manual = true) }
            }) { Text(UiText.get(R.string.toto_youtube_finish_link)) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    // see CaptureWebContent: wrap_content makes chromium lay pages out at zero height
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    // google refuses sign-in from pages that announce themselves as an embedded webview
                    settings.userAgentString = settings.userAgentString.replace("; wv", "")
                    disableForcedDarkening()
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            currentOnLoadingChange(true)
                            url?.let(currentOnUrlChange)
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            currentOnLoadingChange(false)
                            url?.let(currentOnUrlChange)
                            if (url != null) finishLogin(view, url)
                        }
                    }
                    loadUrl(LOGIN_URL)
                    webView = this
                }
            },
        )
        if (saving) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
}
