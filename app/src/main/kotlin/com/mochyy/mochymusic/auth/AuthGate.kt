package com.mochyy.mochymusic.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.i18n.AppLanguage
import com.mochyy.mochymusic.i18n.activityOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.ui.input.pointer.pointerInput

private val AuthAccent = Color(0xFFF0CCAA)
private val AuthMuted = Color(0xFFACAAB5)

@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(!PersonalAuth.allowed) }
    var verification by remember { mutableStateOf<Job?>(null) }
    val authorized = remember(PersonalAuth.revision) { PersonalAuth.allowed }
    val navigationState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    fun check(force: Boolean = false) {
        if (verification?.isActive == true) return
        checking = true
        verification = scope.launch {
            try { PersonalAuth.verify(context, force = force) } finally { checking = false }
        }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) check()
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) check()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (authorized) {
        SideEffect { PersonalAuth.navigationReady = true }
        navigationState.SaveableStateProvider("music-navigation-${PersonalAuth.userId}") { content() }
    } else {
        MaterialTheme(colorScheme = darkColorScheme(primary = AuthAccent, onPrimary = Color(0xFF30241C))) {
            if (checking || PersonalAuth.busy) SessionCheckScreen()
            else LoginScreen(
                busy = false,
                status = PersonalAuth.status,
                onSubmit = { email, password ->
                    checking = true
                    verification = scope.launch {
                        try { PersonalAuth.verify(context, email, password) } finally { checking = false }
                    }
                },
                onRetry = { check(force = true) },
                onSignOut = { PersonalAuth.logout(context) },
            )
        }
    }
    PersonalAuth.warning?.let { problem ->
        AlertDialog(
            onDismissRequest = { PersonalAuth.dismissWarning() },
            title = { Text(stringResource(if (authorized) R.string.toto_background_check_title else R.string.toto_access_changed_title)) },
            text = { Text(if (authorized) stringResource(R.string.toto_background_check_detail, stringResource(problem)) else stringResource(problem)) },
            confirmButton = {
                TextButton(onClick = { PersonalAuth.dismissWarning(); if (authorized) check(force = true) }) {
                    Text(stringResource(if (authorized) R.string.toto_retry else android.R.string.ok))
                }
            },
            dismissButton = {
                if (authorized) TextButton(onClick = { PersonalAuth.dismissWarning() }) { Text(stringResource(R.string.toto_continue_listening)) }
            },
        )
    }
}

@Composable
private fun SessionCheckScreen() {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF111014)).pointerInput(Unit) {
            awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
        },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.toto_logo), null, Modifier.size(112.dp))
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(Modifier.size(24.dp), color = AuthAccent, strokeWidth = 2.dp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.toto_checking), color = AuthMuted)
    }
}

@Composable
private fun LoginScreen(busy: Boolean, status: Int, onSubmit: (String, String) -> Unit, onRetry: () -> Unit, onSignOut: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val canSubmit = !busy && email.isNotBlank() && password.isNotBlank()
    val hasError = !busy && status != R.string.toto_sign_in && status != R.string.toto_checking
    val recoverable = status in setOf(R.string.toto_network, R.string.toto_config, R.string.toto_auth_database, R.string.toto_auth_storage, R.string.toto_profile_load_error)
    fun submit() {
        if (!canSubmit) return
        focus.clearFocus()
        val submitted = password
        password = ""; visible = false
        onSubmit(email, submitted)
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1C1920), Color(0xFF0C0D11)))).safeDrawingPadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.widthIn(max = 440.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val selected = AppLanguage.selection(context)
                listOf("vi" to "Tiếng Việt", "en" to "English").forEach { (tag, label) ->
                    TextButton(enabled = !busy, onClick = { scope.launch { context.activityOrNull()?.let { AppLanguage.select(it, tag) } } }) {
                        Text(label, color = if (selected.startsWith(tag)) AuthAccent else AuthMuted, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Image(painterResource(R.drawable.toto_logo), stringResource(R.string.app_name), Modifier.size(112.dp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.app_name), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.toto_auth_tagline), color = AuthMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Surface(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp), color = Color(0xFF1B1B22),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.toto_auth_welcome), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(stringResource(R.string.toto_auth_instruction), style = MaterialTheme.typography.bodySmall, color = AuthMuted)
                    }
                    OutlinedTextField(
                        value = email, onValueChange = { email = it }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        label = { Text(stringResource(R.string.toto_email)) },
                        leadingIcon = { Icon(Icons.Outlined.Email, null) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.White.copy(alpha = 0.18f)),
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        label = { Text(stringResource(R.string.toto_password)) },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) }, singleLine = true,
                        trailingIcon = { IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                stringResource(if (visible) R.string.toto_auth_hide_password else R.string.toto_auth_show_password))
                        } },
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.White.copy(alpha = 0.18f)),
                    )
                    if (hasError) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF362627)) {
                            Text(stringResource(status), Modifier.padding(14.dp), color = Color(0xFFF1C5C0), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(onClick = { submit() }, enabled = canSubmit, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp)) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.toto_checking))
                        } else {
                            Text(stringResource(R.string.toto_sign_in), fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(10.dp))
                            Icon(Icons.Rounded.ArrowForward, null, Modifier.size(18.dp))
                        }
                    }
                    if (hasError && recoverable) {
                        TextButton(onClick = onRetry, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.toto_retry)) }
                        TextButton(onClick = onSignOut, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.toto_auth_other_account)) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.widthIn(max = 360.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Lock, null, Modifier.size(14.dp), tint = AuthMuted)
                Text(stringResource(R.string.toto_auth_private), style = MaterialTheme.typography.labelSmall, color = AuthMuted)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun PersonalLogoutButton() {
    val context = LocalContext.current
    TextButton(onClick = { PersonalAuth.logout(context) }) { Text(stringResource(R.string.toto_logout)) }
}
