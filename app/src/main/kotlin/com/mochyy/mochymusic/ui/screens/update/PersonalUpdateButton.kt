package com.mochyy.mochymusic.ui.screens.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.core.updater.PersonalUpdates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PersonalUpdateButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableIntStateOf(R.string.toto_update_checking) }
    var release by remember { mutableStateOf<PersonalUpdates.Release?>(null) }
    var apk by remember { mutableStateOf<File?>(null) }
    fun check() {
        visible = true
        release = null
        apk = null
        if (!PersonalUpdates.compatible(context)) {
            status = R.string.toto_update_debug
            return
        }
        scope.launch {
            busy = true
            status = R.string.toto_update_checking
            try {
                release = PersonalUpdates.latest()
                status = if (release == null) R.string.toto_update_current else R.string.toto_update_available
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { status = R.string.toto_update_error }
            finally { busy = false }
        }
    }
    TextButton(enabled = !busy, onClick = { check() }) {
        Text(stringResource(R.string.toto_update_check))
    }
    if (visible) AlertDialog(
        onDismissRequest = { if (!busy) visible = false },
        title = { Text(stringResource(R.string.toto_update_title)) },
        text = {
            Text(stringResource(status) + (release?.let { "\n${it.name}" } ?: ""))
        },
        confirmButton = {
            if (!busy && release != null) TextButton(onClick = {
                scope.launch {
                    busy = true
                    try {
                        if (apk == null) {
                            status = R.string.toto_update_downloading
                            apk = PersonalUpdates.download(context, release!!)
                        }
                        status = if (PersonalUpdates.install(context, apk!!)) R.string.toto_update_ready else R.string.toto_update_permission
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) {
                        apk?.delete()
                        apk = null
                        status = R.string.toto_update_error
                    } finally { busy = false }
                }
            }) { Text(stringResource(if (apk == null) R.string.toto_update_download else R.string.toto_update_install)) }
            else if (!busy && status == R.string.toto_update_error) TextButton(onClick = { check() }) {
                Text(stringResource(R.string.toto_update_retry))
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { visible = false }) { Text(stringResource(R.string.done)) } },
    )
}
