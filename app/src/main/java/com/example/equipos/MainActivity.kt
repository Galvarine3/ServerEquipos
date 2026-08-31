package com.example.equipos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.asin
import kotlin.math.sqrt
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.focus.onFocusChanged
import com.example.equipos.ui.theme.EquiposTheme
import android.util.Patterns
import android.util.Base64
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import android.location.Geocoder
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.FileProvider
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Date
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// Constants for player ratings
private const val WEIGHT_ATTACK = 0.35
private const val WEIGHT_DEFENSE = 0.35
private const val WEIGHT_PHYSICAL = 0.30

private const val MIN_PASSWORD_LENGTH = 6
private const val CHAT_CHANNEL_ID = "chat_messages_channel"
private const val EXTRA_CHAT_POST_ID = "extra_chat_post_id"
private const val EXTRA_CHAT_PEER_NAME = "extra_chat_peer_name"
private const val EXTRA_CHAT_IS_GROUP = "extra_chat_is_group"

private fun isValidEmail(email: String): Boolean =
    Patterns.EMAIL_ADDRESS.matcher(email.trim().lowercase()).matches()

data class CommunityPost(
    val id: Long,
    val time: Long,
    val userId: String? = null,
    val user: String,
    val sport: String,
    val available: Int,
    val total: Int,
    val message: String,
    val locality: String,
    val serverId: String? = null
)

data class ChatMessage(
    val from: String,
    val to: String,
    val text: String,
    val time: Long
)

data class ChatThread(
    val userId: String,
    val userName: String,
    val lastText: String,
    val time: Long
)

private fun passwordStrength(password: String): Int {
    var score = 0
    if (password.length >= MIN_PASSWORD_LENGTH) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    return score.coerceIn(0, 4)
}

private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * asin(sqrt(a))
    return R * c
}

private fun decodeUserIdFromToken(token: String?): String? {
    if (token.isNullOrBlank()) return null
    val parts = token.split(".")
    if (parts.size < 2) return null
    return try {
        val payloadSegment = parts[1]
        val decodedBytes = Base64.decode(payloadSegment, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val payload = String(decodedBytes, Charsets.UTF_8)
        val json = JSONObject(payload)
        val uid = json.opt("uid")
        when (uid) {
            is Number -> uid.toLong().toString()
            is String -> uid
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun fetchThreadsRemote(context: Context, access: String?, refresh: String?, postId: String): Pair<List<ChatThread>?, String?> {
    if (access.isNullOrBlank()) return null to access
    var token = access
    val (code, text) = try { httpGet("/messages/threads?postId=$postId", token) } catch (_: Exception) { -1 to null }
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val (code2, text2) = try { httpGet("/messages/threads?postId=$postId", token) } catch (_: Exception) { -1 to null }
            if (code2 in 200..299 && !text2.isNullOrBlank()) {
                return try {
                    val arr = Json.parseToJsonElement(text2).jsonArray
                    val list = arr.map { el ->
                        val o = el.jsonObject
                        ChatThread(
                            userId = o["userId"]?.jsonPrimitive?.contentOrNull ?: "",
                            userName = o["userName"]?.jsonPrimitive?.contentOrNull ?: "",
                            lastText = o["lastText"]?.jsonPrimitive?.contentOrNull ?: "",
                            time = o["time"]?.jsonPrimitive?.longOrNull ?: 0L
                        )
                    }
                    list to token
                } catch (_: Exception) { null to token }
            }
        }
        return null to token
    }
    if (code in 200..299 && !text.isNullOrBlank()) {
        return try {
            val arr = Json.parseToJsonElement(text).jsonArray
            val list = arr.map { el ->
                val o = el.jsonObject
                ChatThread(
                    userId = o["userId"]?.jsonPrimitive?.contentOrNull ?: "",
                    userName = o["userName"]?.jsonPrimitive?.contentOrNull ?: "",
                    lastText = o["lastText"]?.jsonPrimitive?.contentOrNull ?: "",
                    time = o["time"]?.jsonPrimitive?.longOrNull ?: 0L
                )
            }
            list to token
        } catch (_: Exception) { null to token }
    }
    return null to token
}

@Composable
fun ChatDialog(
    recipient: String,
    messages: List<ChatMessage>,
    loading: Boolean = false,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
    currentUser: String? = null
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(recipient, messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 600.dp)
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipient,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (loading) {
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Cargando mensajes...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White
                        )
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty() && !loading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay mensajes aún",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(messages) { m ->
                                val isIncoming = if (currentUser != null) !m.from.equals(currentUser, ignoreCase = true) else m.from == recipient
                                val bubbleColor = if (isIncoming) Color(0xFF1B5E20) else Color(0xFF43A047)
                                val time = remember(m.time) {
                                    try {
                                        SimpleDateFormat("HH:mm").format(Date(m.time))
                                    } catch (_: Exception) { "" }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = if (isIncoming) Arrangement.Start else Arrangement.End
                                ) {
                                    Surface(
                                        color = bubbleColor,
                                        shape = RoundedCornerShape(16.dp),
                                        tonalElevation = 1.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .widthIn(max = 280.dp)
                                        ) {
                                            Text(m.text, color = Color.White)
                                            if (time.isNotEmpty()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    time,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 2.dp,
                        color = Color.White.copy(alpha = 0.06f)
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            singleLine = true,
                            placeholder = { Text("Escribe un mensaje...", color = Color.White.copy(alpha = 0.7f)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                val t = input.trim()
                                if (t.isNotEmpty()) {
                                    onSend(t)
                                    input = ""
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                disabledBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                    FilledIconButton(
                        onClick = {
                            val t = input.trim()
                            if (t.isNotEmpty()) {
                                onSend(t)
                                input = ""
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        textContentColor = MaterialTheme.colorScheme.onPrimary,
        confirmButton = {}
    )
}

// ===== Remote Community (CRUD) =====
private fun fetchCommunityPostsRemote(context: Context, access: String?, refresh: String?): Pair<List<CommunityPost>?, String?> {
    if (access.isNullOrBlank()) return null to access
    var token = access
    val (code, text) = try { httpGet("/community/posts", token) } catch (_: Exception) { -1 to null }
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val (code2, text2) = try { httpGet("/community/posts", token) } catch (_: Exception) { -1 to null }
            if (code2 in 200..299 && !text2.isNullOrBlank()) {
                return try {
                    val arr = JSONArray(text2)
                    val list = mutableListOf<CommunityPost>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list += CommunityPost(
                            id = o.optLong("time"),
                            time = o.optLong("time"),
                            userId = o.optString("userId", "").takeIf { it.isNotBlank() },
                            user = o.optString("userName", ""),
                            sport = o.optString("sport", "Futbolito"),
                            available = o.optInt("available", 0),
                            total = o.optInt("total", 0),
                            message = o.optString("message", ""),
                            locality = o.optString("locality", ""),
                            serverId = o.optString("id", "").takeIf { it.isNotBlank() }
                        )
                    }
                    list to token
                } catch (_: Exception) { null to token }
            }
        }
        return null to token
    }
    if (code in 200..299 && !text.isNullOrBlank()) {
        return try {
            val arr = Json.parseToJsonElement(text).jsonArray
            val list = arr.map { el ->
                val o = el.jsonObject
                CommunityPost(
                    id = o["time"]?.jsonPrimitive?.longOrNull ?: 0L,
                    time = o["time"]?.jsonPrimitive?.longOrNull ?: 0L,
                    userId = o["userId"]?.jsonPrimitive?.contentOrNull,
                    user = o["userName"]?.jsonPrimitive?.contentOrNull ?: "",
                    sport = o["sport"]?.jsonPrimitive?.contentOrNull ?: "Futbolito",
                    available = o["available"]?.jsonPrimitive?.intOrNull ?: 0,
                    total = o["total"]?.jsonPrimitive?.intOrNull ?: 0,
                    message = o["message"]?.jsonPrimitive?.contentOrNull ?: "",
                    locality = o["locality"]?.jsonPrimitive?.contentOrNull ?: "",
                    serverId = o["id"]?.jsonPrimitive?.contentOrNull
                )
            }
            list to token
        } catch (_: Exception) { null to token }
    }
    return null to token
}

private fun createCommunityPostRemote(context: Context, access: String?, refresh: String?, body: JSONObject): String? {
    if (access.isNullOrBlank()) return null
    var token = access
    var (code, _) = try { httpPostJson("/community/posts", body, token) } catch (_: Exception) { -1 to null }
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val res2 = try { httpPostJson("/community/posts", body, token) } catch (_: Exception) { -1 to null }
            code = res2.first
        }
    }
    return if (code in 200..299) token else null
}

private fun fetchGroupMessagesRemote(context: Context, access: String?, refresh: String?, postId: String): Pair<List<ChatMessage>?, String?> {
    if (access.isNullOrBlank()) return null to access
    var token = access
    val (code, text) = try { httpGet("/messages/posts/$postId", token) } catch (_: Exception) { -1 to null }
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val (code2, text2) = try { httpGet("/messages/posts/$postId", token) } catch (_: Exception) { -1 to null }
            if (code2 in 200..299 && !text2.isNullOrBlank()) {
                return try {
                    val arr = Json.parseToJsonElement(text2).jsonArray
                    val list = arr.map { el ->
                        val o = el.jsonObject
                        ChatMessage(
                            from = o["fromName"]?.jsonPrimitive?.contentOrNull ?: "",
                            to = "",
                            text = o["text"]?.jsonPrimitive?.contentOrNull ?: "",
                            time = o["time"]?.jsonPrimitive?.longOrNull ?: 0L
                        )
                    }
                    list to token
                } catch (_: Exception) { null to token }
            }
        }
        return null to token
    }
    if (code in 200..299 && !text.isNullOrBlank()) {
        return try {
            val arr = Json.parseToJsonElement(text).jsonArray
            val list = arr.map { el ->
                val o = el.jsonObject
                ChatMessage(
                    from = o["fromName"]?.jsonPrimitive?.contentOrNull ?: "",
                    to = "",
                    text = o["text"]?.jsonPrimitive?.contentOrNull ?: "",
                    time = o["time"]?.jsonPrimitive?.longOrNull ?: 0L
                )
            }
            list to token
        } catch (_: Exception) { null to token }
    }
    return null to token
}

private fun sendGroupMessageRemote(context: Context, access: String?, refresh: String?, postId: String, body: JSONObject): String? {
    if (access.isNullOrBlank()) return null
    var token = access
    var (code, _) = try { httpPostJson("/messages/posts/$postId", body, token) } catch (_: Exception) { -1 to null }
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val res2 = try { httpPostJson("/messages/posts/$postId", body, token) } catch (_: Exception) { -1 to null }
            code = res2.first
        }
    }
    return if (code in 200..299) token else null
}

private fun updateCommunityPostRemote(context: Context, access: String?, refresh: String?, id: String, body: JSONObject): String? {
    if (access.isNullOrBlank()) return null
    var token = access
    var (code, _) = httpPostRaw("/community/posts/$id", body.toString(), token) // will be PUT below
    // Use proper PUT method
    val req = Request.Builder().url("$BASE_URL/community/posts/$id").put(body.toString().toRequestBody(JSON_MEDIA)).header("Authorization", "Bearer $token").build()
    try {
        httpClient.newCall(req).execute().use { resp ->
            code = resp.code
        }
    } catch (_: Exception) {
        code = -1
    }
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val req2 = Request.Builder().url("$BASE_URL/community/posts/$id").put(body.toString().toRequestBody(JSON_MEDIA)).header("Authorization", "Bearer $token").build()
            try {
                httpClient.newCall(req2).execute().use { resp2 ->
                    code = resp2.code
                }
            } catch (_: Exception) {
                code = -1
            }
        }
    }
    return if (code in 200..299) token else null
}

private fun deleteCommunityPostRemote(context: Context, access: String?, refresh: String?, id: String): String? {
    if (access.isNullOrBlank()) return null
    var token = access
    var code: Int
    val req = Request.Builder().url("$BASE_URL/community/posts/$id").delete().header("Authorization", "Bearer $token").build()
    try {
        httpClient.newCall(req).execute().use { resp -> code = resp.code }
    } catch (_: Exception) {
        code = -1
    }
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val req2 = Request.Builder().url("$BASE_URL/community/posts/$id").delete().header("Authorization", "Bearer $token").build()
            try {
                httpClient.newCall(req2).execute().use { resp2 -> code = resp2.code }
            } catch (_: Exception) {
                code = -1
            }
        }
    }
    return if (code in 200..299) token else null
}

private fun fetchMessagesRemote(context: Context, access: String?, refresh: String?, otherId: String, postId: String): Pair<List<ChatMessage>?, String?> {
    if (access.isNullOrBlank()) return null to access
    var token = access
    val (code, text) = try { httpGet("/messages?withUser=$otherId&postId=$postId", token) } catch (_: Exception) { -1 to null }
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val (code2, text2) = try { httpGet("/messages?withUser=$otherId&postId=$postId", token) } catch (_: Exception) { -1 to null }
            if (code2 in 200..299 && !text2.isNullOrBlank()) {
                return try {
                    val arr = Json.parseToJsonElement(text2).jsonArray
                    val list = arr.map { el ->
                        val o = el.jsonObject
                        ChatMessage(
                            from = o["fromName"]?.jsonPrimitive?.contentOrNull ?: "",
                            to = o["toName"]?.jsonPrimitive?.contentOrNull ?: "",
                            text = o["text"]?.jsonPrimitive?.contentOrNull ?: "",
                            time = o["time"]?.jsonPrimitive?.longOrNull ?: 0L
                        )
                    }
                    list to token
                } catch (_: Exception) { null to token }
            }
        }
        return null to token
    }
    if (code in 200..299 && !text.isNullOrBlank()) {
        return try {
            val arr = Json.parseToJsonElement(text).jsonArray
            val list = arr.map { el ->
                val o = el.jsonObject
                ChatMessage(
                    from = o["fromName"]?.jsonPrimitive?.contentOrNull ?: "",
                    to = o["toName"]?.jsonPrimitive?.contentOrNull ?: "",
                    text = o["text"]?.jsonPrimitive?.contentOrNull ?: "",
                    time = o["time"]?.jsonPrimitive?.longOrNull ?: 0L
                )
            }
            list to token
        } catch (_: Exception) { null to token }
    }
    return null to token
}

private fun sendMessageRemote(context: Context, access: String?, refresh: String?, body: JSONObject): String? {
    if (access.isNullOrBlank()) return null
    var token = access
    var (code, _) = try { httpPostJson("/messages", body, token) } catch (_: Exception) { -1 to null }
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val res2 = try { httpPostJson("/messages", body, token) } catch (_: Exception) { -1 to null }
            code = res2.first
        }
    }
    return if (code in 200..299) token else null
}

fun saveCommunityPosts(context: Context, posts: List<CommunityPost>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val arr = JSONArray()
    posts.forEach { p ->
        val o = JSONObject()
        o.put("id", p.id)
        o.put("time", p.time)
        o.put("user", p.user)
        o.put("sport", p.sport)
        o.put("available", p.available)
        o.put("total", p.total)
        o.put("message", p.message)
        o.put("locality", p.locality)
        if (p.serverId != null) o.put("serverId", p.serverId)
        arr.put(o)
    }
    prefs.edit().putString(KEY_COMMUNITY_POSTS, arr.toString()).apply()
}

fun loadCommunityPosts(context: Context): List<CommunityPost> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_COMMUNITY_POSTS, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<CommunityPost>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += CommunityPost(
                id = o.optLong("id"),
                time = o.optLong("time"),
                user = o.optString("user", ""),
                sport = o.optString("sport", "Futbolito"),
                available = o.optInt("available", 0),
                total = o.optInt("total", 0),
                message = o.optString("message", ""),
                locality = o.optString("locality", ""),
                serverId = o.optString("serverId", "").takeIf { it.isNotBlank() }
            )
        }
        list
    } catch (_: Exception) { emptyList() }
}

fun addCommunityPost(context: Context, post: CommunityPost) {
    val current = loadCommunityPosts(context).toMutableList()
    current.add(0, post)
    saveCommunityPosts(context, current)
}

fun loadUnreadByPost(context: Context): MutableMap<String, Int> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_COMMUNITY_UNREAD, null) ?: return mutableMapOf()
    return try {
        val obj = JSONObject(json)
        val map = mutableMapOf<String, Int>()
        obj.keys().forEach { key ->
            val v = obj.optInt(key, 0)
            if (v > 0) map[key] = v
        }
        map
    } catch (_: Exception) { mutableMapOf() }
}

fun saveUnreadByPost(context: Context, map: Map<String, Int>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val obj = JSONObject()
    map.forEach { (k, v) -> if (v > 0) obj.put(k, v) }
    prefs.edit().putString(KEY_COMMUNITY_UNREAD, obj.toString()).apply()
}

fun incrementUnreadForPost(context: Context, postId: String) {
    if (postId.isBlank()) return
    val current = loadUnreadByPost(context)
    val next = (current[postId] ?: 0) + 1
    current[postId] = next
    saveUnreadByPost(context, current)
}

fun clearUnreadForPost(context: Context, postId: String) {
    if (postId.isBlank()) return
    val current = loadUnreadByPost(context)
    if (current.remove(postId) != null) {
        saveUnreadByPost(context, current)
    }
}

@Composable
fun CommunityDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasPermission by remember { mutableStateOf(false) }
    var locationDenied by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLon by remember { mutableStateOf<Double?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            hasPermission = fine || coarse
            locationDenied = !hasPermission
            if (hasPermission) {
                isLoading = true
                fused.lastLocation.addOnSuccessListener { loc ->
                    currentLat = loc?.latitude
                    currentLon = loc?.longitude
                    isLoading = false
                }.addOnFailureListener {
                    isLoading = false
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasPermission = fineGranted || coarseGranted
        if (hasPermission) {
            isLoading = true
            fused.lastLocation.addOnSuccessListener { loc ->
                currentLat = loc?.latitude
                currentLon = loc?.longitude
                isLoading = false
            }.addOnFailureListener {
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.community), color = Color.White) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (!hasPermission) {
                    Text(stringResource(R.string.location_denied))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        locationDenied = false
                        launcher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }) { Text(stringResource(R.string.grant_location)) }
                } else {
                    Text(stringResource(R.string.nearby_users))
                    Spacer(Modifier.height(8.dp))
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.searching))
                        }
                    } else {
                        val lat = currentLat
                        val lon = currentLon
                        if (lat == null || lon == null) {
                            Text(stringResource(R.string.no_users_nearby))
                        } else {
                            // Placeholder list
                            val items = listOf(
                                "Diego" to 5.2,
                                "Ariel" to 12.7,
                                "Pablo" to 28.9
                            ).filter { it.second <= 30.0 }
                            if (items.isEmpty()) {
                                Text(stringResource(R.string.no_users_nearby))
                            } else {
                                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                                    items(items) { (name, km) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(name, fontWeight = FontWeight.SemiBold)
                                                Text(stringResource(R.string.distance_km, km))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { /* TODO: crear oferta */ }) { Text(stringResource(R.string.create_match_offer)) }
                                                Button(onClick = { /* TODO: publicar vacantes */ }) { Text(stringResource(R.string.create_vacancy)) }
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        textContentColor = MaterialTheme.colorScheme.onPrimary,
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        dismissButton = null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onBack: () -> Unit,
    initialPostId: String? = null,
    initialPeerName: String? = null,
    initialIsGroup: Boolean = false
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf("Futbolito") }
    var available by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var availableText by remember { mutableStateOf("0") }
    var totalText by remember { mutableStateOf("0") }
    var locality by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    val user = userNameState.value ?: ""
    var currentUserId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sports = remember { listOf("Fútbol", "Futbolito", "Baby Fútbol", "Pádel", "Tenis", "Voleybol") }
    var createSportExpanded by remember { mutableStateOf(false) }
    var editSportExpanded by remember { mutableStateOf(false) }
    val allLocalities = remember {
        listOf(
            // Región de Arica y Parinacota
            "Región de Arica y Parinacota - Arica",
            "Región de Arica y Parinacota - Camarones",
            "Región de Arica y Parinacota - Putre",
            "Región de Arica y Parinacota - General Lagos",

            // Región de Tarapacá
            "Región de Tarapacá - Iquique",
            "Región de Tarapacá - Alto Hospicio",
            "Región de Tarapacá - Pozo Almonte",
            "Región de Tarapacá - Camiña",
            "Región de Tarapacá - Colchane",
            "Región de Tarapacá - Huara",
            "Región de Tarapacá - Pica",

            // Región de Antofagasta
            "Región de Antofagasta - Antofagasta",
            "Región de Antofagasta - Mejillones",
            "Región de Antofagasta - Sierra Gorda",
            "Región de Antofagasta - Taltal",
            "Región de Antofagasta - Calama",
            "Región de Antofagasta - Ollagüe",
            "Región de Antofagasta - San Pedro de Atacama",
            "Región de Antofagasta - Tocopilla",
            "Región de Antofagasta - María Elena",

            // Región de Atacama
            "Región de Atacama - Copiapó",
            "Región de Atacama - Caldera",
            "Región de Atacama - Tierra Amarilla",
            "Región de Atacama - Chañaral",
            "Región de Atacama - Diego de Almagro",
            "Región de Atacama - Vallenar",
            "Región de Atacama - Alto del Carmen",
            "Región de Atacama - Freirina",
            "Región de Atacama - Huasco",

            // Región de Coquimbo
            "Región de Coquimbo - La Serena",
            "Región de Coquimbo - Coquimbo",
            "Región de Coquimbo - Andacollo",
            "Región de Coquimbo - La Higuera",
            "Región de Coquimbo - Paihuano",
            "Región de Coquimbo - Vicuña",
            "Región de Coquimbo - Illapel",
            "Región de Coquimbo - Canela",
            "Región de Coquimbo - Los Vilos",
            "Región de Coquimbo - Salamanca",
            "Región de Coquimbo - Ovalle",
            "Región de Coquimbo - Combarbalá",
            "Región de Coquimbo - Monte Patria",
            "Región de Coquimbo - Punitaqui",
            "Región de Coquimbo - Río Hurtado",

            // Región de Valparaíso
            "Región de Valparaíso - Valparaíso",
            "Región de Valparaíso - Viña del Mar",
            "Región de Valparaíso - Concón",
            "Región de Valparaíso - Quilpué",
            "Región de Valparaíso - Villa Alemana",
            "Región de Valparaíso - Casablanca",
            "Región de Valparaíso - Quintero",
            "Región de Valparaíso - Puchuncaví",
            "Región de Valparaíso - Juan Fernández",
            "Región de Valparaíso - San Antonio",
            "Región de Valparaíso - Cartagena",
            "Región de Valparaíso - El Tabo",
            "Región de Valparaíso - El Quisco",
            "Región de Valparaíso - Algarrobo",
            "Región de Valparaíso - Santo Domingo",
            "Región de Valparaíso - Quillota",
            "Región de Valparaíso - La Cruz",
            "Región de Valparaíso - La Calera",
            "Región de Valparaíso - Nogales",
            "Región de Valparaíso - Hijuelas",
            "Región de Valparaíso - San Felipe",
            "Región de Valparaíso - Putaendo",
            "Región de Valparaíso - Santa María",
            "Región de Valparaíso - Llay-Llay",
            "Región de Valparaíso - Catemu",
            "Región de Valparaíso - Panquehue",
            "Región de Valparaíso - Los Andes",
            "Región de Valparaíso - Calle Larga",
            "Región de Valparaíso - Rinconada",
            "Región de Valparaíso - San Esteban",
            "Región de Valparaíso - Isla de Pascua",

            // Región Metropolitana
            "Región Metropolitana - Santiago",
            "Región Metropolitana - Providencia",
            "Región Metropolitana - Las Condes",
            "Región Metropolitana - Vitacura",
            "Región Metropolitana - Ñuñoa",
            "Región Metropolitana - La Reina",
            "Región Metropolitana - Peñalolén",
            "Región Metropolitana - Macul",
            "Región Metropolitana - La Florida",
            "Región Metropolitana - Puente Alto",
            "Región Metropolitana - San José de Maipo",
            "Región Metropolitana - Pirque",
            "Región Metropolitana - Maipú",
            "Región Metropolitana - Cerrillos",
            "Región Metropolitana - Estación Central",
            "Región Metropolitana - Pudahuel",
            "Región Metropolitana - Lo Prado",
            "Región Metropolitana - Quinta Normal",
            "Región Metropolitana - Cerro Navia",
            "Región Metropolitana - Renca",
            "Región Metropolitana - Independencia",
            "Región Metropolitana - Recoleta",
            "Región Metropolitana - Conchalí",
            "Región Metropolitana - Huechuraba",
            "Región Metropolitana - Quilicura",
            "Región Metropolitana - Lo Barnechea",
            "Región Metropolitana - San Miguel",
            "Región Metropolitana - San Joaquín",
            "Región Metropolitana - La Cisterna",
            "Región Metropolitana - San Ramón",
            "Región Metropolitana - La Granja",
            "Región Metropolitana - El Bosque",
            "Región Metropolitana - San Bernardo",
            "Región Metropolitana - Buin",
            "Región Metropolitana - Paine",
            "Región Metropolitana - Calera de Tango",
            "Región Metropolitana - Talagante",
            "Región Metropolitana - Peñaflor",
            "Región Metropolitana - Isla de Maipo",
            "Región Metropolitana - El Monte",
            "Región Metropolitana - Padre Hurtado",
            "Región Metropolitana - Melipilla",
            "Región Metropolitana - Curacaví",
            "Región Metropolitana - María Pinto",
            "Región Metropolitana - Alhué",

            // Región de O'Higgins
            "Región de O'Higgins - Rancagua",
            "Región de O'Higgins - Machalí",
            "Región de O'Higgins - Graneros",
            "Región de O'Higgins - Mostazal",
            "Región de O'Higgins - Codegua",
            "Región de O'Higgins - Requínoa",
            "Región de O'Higgins - Rengo",
            "Región de O'Higgins - Malloa",
            "Región de O'Higgins - Quinta de Tilcoco",
            "Región de O'Higgins - San Vicente",
            "Región de O'Higgins - Pichidegua",
            "Región de O'Higgins - Peumo",
            "Región de O'Higgins - Las Cabras",
            "Región de O'Higgins - Doñihue",
            "Región de O'Higgins - Coinco",
            "Región de O'Higgins - Coltauco",
            "Región de O'Higgins - San Fernando",
            "Región de O'Higgins - Chimbarongo",
            "Región de O'Higgins - Nancagua",
            "Región de O'Higgins - Placilla",
            "Región de O'Higgins - Santa Cruz",
            "Región de O'Higgins - Chépica",
            "Región de O'Higgins - Palmilla",
            "Región de O'Higgins - Peralillo",
            "Región de O'Higgins - Lolol",
            "Región de O'Higgins - Pumanque",
            "Región de O'Higgins - Pichilemu",
            "Región de O'Higgins - Marchihue",
            "Región de O'Higgins - La Estrella",
            "Región de O'Higgins - Litueche",
            "Región de O'Higgins - Navidad",

            // Región del Maule
            "Región del Maule - Talca",
            "Región del Maule - Maule",
            "Región del Maule - San Clemente",
            "Región del Maule - Pelarco",
            "Región del Maule - Pencahue",
            "Región del Maule - Curepto",
            "Región del Maule - Constitución",
            "Región del Maule - Empedrado",
            "Región del Maule - Linares",
            "Región del Maule - Yerbas Buenas",
            "Región del Maule - Colbún",
            "Región del Maule - Villa Alegre",
            "Región del Maule - San Javier",
            "Región del Maule - Retiro",
            "Región del Maule - Parral",
            "Región del Maule - Cauquenes",
            "Región del Maule - Pelluhue",
            "Región del Maule - Chanco",
            "Región del Maule - Curicó",
            "Región del Maule - Romeral",
            "Región del Maule - Teno",
            "Región del Maule - Rauco",
            "Región del Maule - Sagrada Familia",
            "Región del Maule - Molina",
            "Región del Maule - Hualañé",

            // Región de Ñuble
            "Región de Ñuble - Chillán",
            "Región de Ñuble - Chillán Viejo",
            "Región de Ñuble - San Carlos",
            "Región de Ñuble - Coihueco",
            "Región de Ñuble - San Nicolás",
            "Región de Ñuble - Ñiquén",
            "Región de Ñuble - San Fabián",
            "Región de Ñuble - Bulnes",
            "Región de Ñuble - Quillón",
            "Región de Ñuble - San Ignacio",
            "Región de Ñuble - El Carmen",
            "Región de Ñuble - Yungay",
            "Región de Ñuble - Pemuco",
            "Región de Ñuble - Pinto",
            "Región de Ñuble - Coelemu",
            "Región de Ñuble - Trehuaco",
            "Región de Ñuble - Ránquil",
            "Región de Ñuble - Quirihue",
            "Región de Ñuble - Cobquecura",
            "Región de Ñuble - Ninhue",
            "Región de Ñuble - Portezuelo",

            // Región del Biobío
            "Región del Biobío - Concepción",
            "Región del Biobío - Talcahuano",
            "Región del Biobío - Hualpén",
            "Región del Biobío - Penco",
            "Región del Biobío - Tomé",
            "Región del Biobío - Chiguayante",
            "Región del Biobío - San Pedro de la Paz",
            "Región del Biobío - Coronel",
            "Región del Biobío - Lota",
            "Región del Biobío - Hualqui",
            "Región del Biobío - Santa Juana",
            "Región del Biobío - Florida",
            "Región del Biobío - Cabrero",
            "Región del Biobío - Yumbel",
            "Región del Biobío - Laja",
            "Región del Biobío - San Rosendo",
            "Región del Biobío - Los Ángeles",
            "Región del Biobío - Mulchén",
            "Región del Biobío - Nacimiento",
            "Región del Biobío - Negrete",
            "Región del Biobío - Santa Bárbara",
            "Región del Biobío - Quilaco",
            "Región del Biobío - Quilleco",
            "Región del Biobío - Tucapel",
            "Región del Biobío - Antuco",
            "Región del Biobío - Alto Biobío",
            "Región del Biobío - Arauco",
            "Región del Biobío - Curanilahue",
            "Región del Biobío - Lebu",
            "Región del Biobío - Los Álamos",
            "Región del Biobío - Cañete",
            "Región del Biobío - Tirúa",

            // Región de La Araucanía
            "Región de La Araucanía - Temuco",
            "Región de La Araucanía - Padre Las Casas",
            "Región de La Araucanía - Lautaro",
            "Región de La Araucanía - Vilcún",
            "Región de La Araucanía - Cunco",
            "Región de La Araucanía - Melipeuco",
            "Región de La Araucanía - Curacautín",
            "Región de La Araucanía - Lonquimay",
            "Región de La Araucanía - Galvarino",
            "Región de La Araucanía - Cholchol",
            "Región de La Araucanía - Nueva Imperial",
            "Región de La Araucanía - Carahue",
            "Región de La Araucanía - Saavedra",
            "Región de La Araucanía - Toltén",
            "Región de La Araucanía - Teodoro Schmidt",
            "Región de La Araucanía - Pitrufquén",
            "Región de La Araucanía - Gorbea",
            "Región de La Araucanía - Loncoche",
            "Región de La Araucanía - Villarrica",
            "Región de La Araucanía - Pucón",
            "Región de La Araucanía - Freire",
            "Región de La Araucanía - Perquenco",
            "Región de La Araucanía - Ercilla",
            "Región de La Araucanía - Collipulli",
            "Región de La Araucanía - Angol",
            "Región de La Araucanía - Renaico",
            "Región de La Araucanía - Purén",
            "Región de La Araucanía - Los Sauces",
            "Región de La Araucanía - Traiguén",
            "Región de La Araucanía - Lumaco",

            // Región de Los Ríos
            "Región de Los Ríos - Valdivia",
            "Región de Los Ríos - Corral",
            "Región de Los Ríos - Lanco",
            "Región de Los Ríos - Máfil",
            "Región de Los Ríos - Mariquina",
            "Región de Los Ríos - Paillaco",
            "Región de Los Ríos - Los Lagos",
            "Región de Los Ríos - Futrono",
            "Región de Los Ríos - La Unión",
            "Región de Los Ríos - Río Bueno",

            // Región de Los Lagos
            "Región de Los Lagos - Puerto Montt",
            "Región de Los Lagos - Puerto Varas",
            "Región de Los Lagos - Llanquihue",
            "Región de Los Lagos - Frutillar",
            "Región de Los Lagos - Los Muermos",
            "Región de Los Lagos - Maullín",
            "Región de Los Lagos - Calbuco",
            "Región de Los Lagos - Cochamó",
            "Región de Los Lagos - Osorno",
            "Región de Los Lagos - Puyehue",
            "Región de Los Lagos - Río Negro",
            "Región de Los Lagos - San Pablo",
            "Región de Los Lagos - San Juan de la Costa",
            "Región de Los Lagos - Castro",
            "Región de Los Lagos - Ancud",
            "Región de Los Lagos - Quellón",
            "Región de Los Lagos - Chonchi",
            "Región de Los Lagos - Dalcahue",
            "Región de Los Lagos - Quemchi",
            "Región de Los Lagos - Curaco de Vélez",
            "Región de Los Lagos - Quinchao",
            "Región de Los Lagos - Puqueldón",

            // Región de Aysén
            "Región de Aysén - Coyhaique",
            "Región de Aysén - Lago Verde",
            "Región de Aysén - Aysén",
            "Región de Aysén - Cisnes",
            "Región de Aysén - Guaitecas",
            "Región de Aysén - Cochrane",
            "Región de Aysén - O'Higgins",
            "Región de Aysén - Tortel",
            "Región de Aysén - Chile Chico",
            "Región de Aysén - Río Ibáñez",

            // Región de Magallanes
            "Región de Magallanes - Punta Arenas",
            "Región de Magallanes - Puerto Natales",
            "Región de Magallanes - Torres del Paine",
            "Región de Magallanes - Laguna Blanca",
            "Región de Magallanes - Río Verde",
            "Región de Magallanes - San Gregorio",
            "Región de Magallanes - Porvenir",
            "Región de Magallanes - Primavera",
            "Región de Magallanes - Timaukel",
            "Región de Magallanes - Cabo de Hornos",
            "Región de Magallanes - Antártica"
        )
    }
    val regionsWithCommunes = remember(allLocalities) {
        allLocalities.groupBy { it.substringBefore(" - ") }
            .mapValues { entry -> entry.value.map { it.substringAfter(" - ") } }
    }
    var localitySearch by remember { mutableStateOf("") }
    var localityExpanded by remember { mutableStateOf(false) }
    var expandedRegions by remember { mutableStateOf(setOf<String>()) }
    var filterSport by remember { mutableStateOf<String?>(null) }
    var filterLocality by remember { mutableStateOf<String?>(null) }
    var filterLocalitySearch by remember { mutableStateOf("") }
    var filterLocalityExpanded by remember { mutableStateOf(false) }
    var filterExpandedRegions by remember { mutableStateOf(setOf<String>()) }
    var filterSportExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler { onBack() }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingMessage by remember { mutableStateOf("") }
    var editingSport by remember { mutableStateOf("Futbolito") }
    var editingAvailable by remember { mutableStateOf(0) }
    var editingTotal by remember { mutableStateOf(0) }
    var editingLocality by remember { mutableStateOf("") }
    var editingLocalitySearch by remember { mutableStateOf("") }
    var editingLocalityExpanded by remember { mutableStateOf(false) }
    var chatRecipientName by remember { mutableStateOf<String?>(null) }
    var chatRecipientId by remember { mutableStateOf<String?>(null) }
    var chatPostId by remember { mutableStateOf<String?>(null) }
    var groupPostId by remember { mutableStateOf<String?>(null) }
    val chats = remember { mutableStateMapOf<String, List<ChatMessage>>() }
    val unreadByPost = remember { mutableStateMapOf<String, Int>() }
    var ws by remember { mutableStateOf<WebSocket?>(null) }
    var chatLoading by remember { mutableStateOf(false) }
    var groupLoading by remember { mutableStateOf(false) }
    var deletingPost by remember { mutableStateOf<CommunityPost?>(null) }
    var threadsForPost by remember { mutableStateOf<CommunityPost?>(null) }
    var threads by remember { mutableStateOf<List<ChatThread>>(emptyList()) }
    var threadsLoading by remember { mutableStateOf(false) }
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLon by remember { mutableStateOf<Double?>(null) }
    val localityCoordinates = remember { mutableStateMapOf<String, Pair<Double, Double>>() }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var filterByLocation by remember { mutableStateOf(false) }
    var currentLocalityFilter by remember { mutableStateOf<String?>(null) }
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            hasLocationPermission = fine || coarse
            if (hasLocationPermission) {
                fused.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        currentLat = loc.latitude
                        currentLon = loc.longitude
                        val addresses = try {
                            geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        } catch (_: Exception) { null }
                        val addr = addresses?.firstOrNull()
                        val localityName = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
                        currentLocalityFilter = localityName
                    }
                }
            } else {
                filterByLocation = false
                currentLocalityFilter = null
            }
        }
    )
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = fineGranted || coarseGranted
    }
    DisposableEffect(Unit) {
        isCommunityForeground = true
        onDispose {
            isCommunityForeground = false
        }
    }
    LaunchedEffect(Unit) {
        val (at, rt) = loadTokens(context)
        currentUserId = decodeUserIdFromToken(at)
        val remote = withContext(Dispatchers.IO) { fetchCommunityPostsRemote(context, at, rt).first }
        posts = remote ?: loadCommunityPosts(context)
        val storedUnread = loadUnreadByPost(context)
        unreadByPost.clear()
        unreadByPost.putAll(storedUnread)
        if (!initialPostId.isNullOrBlank()) {
            val pid = initialPostId
            val target = posts.firstOrNull { it.serverId == pid }
            if (initialIsGroup) {
                groupPostId = target?.serverId ?: pid
                cancelChatNotification(context, pid, null, true)
                val key = target?.serverId ?: pid
                if (!key.isNullOrBlank()) {
                    unreadByPost.remove(key)
                    clearUnreadForPost(context, key)
                }
            } else {
                val peerName = initialPeerName
                if (!peerName.isNullOrBlank()) {
                    var peerId: String? = null
                    if (!at.isNullOrBlank()) {
                        val pair = withContext(Dispatchers.IO) { fetchThreadsRemote(context, at, rt, pid) }
                        val list = pair.first
                        peerId = list?.firstOrNull { it.userName.equals(peerName, ignoreCase = true) }?.userId
                    }
                    chatRecipientName = peerName
                    chatRecipientId = peerId
                    chatPostId = pid
                    cancelChatNotification(context, pid, peerName, false)
                    if (!pid.isNullOrBlank()) {
                        unreadByPost.remove(pid)
                        clearUnreadForPost(context, pid)
                    }
                } else if (target != null && !target.userId.isNullOrBlank()) {
                    // Fallback: abrir chat con el autor del post
                    chatRecipientName = target.user
                    chatRecipientId = target.userId
                    chatPostId = target.serverId
                    cancelChatNotification(context, target.serverId, target.user, false)
                    val key = target.serverId
                    if (!key.isNullOrBlank()) {
                        unreadByPost.remove(key)
                        clearUnreadForPost(context, key)
                    }
                }
            }
        }
        // Open WebSocket for realtime
        val access = at
        if (!access.isNullOrBlank()) {
            val wsUrl = BASE_URL.replaceFirst("https", "wss").replaceFirst("http", "ws") + "/ws"
            val req = Request.Builder().url(wsUrl).header("Authorization", "Bearer $access").build()
            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    ws = webSocket
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val root = Json.parseToJsonElement(text).jsonObject
                        val type = root["type"]?.jsonPrimitive?.contentOrNull
                        val data = root["data"]
                        if (type == "post_created" || type == "post_updated") {
                            val o = data?.jsonObject ?: return
                            val p = CommunityPost(
                                id = o["time"]?.jsonPrimitive?.longOrNull ?: 0L,
                                time = o["time"]?.jsonPrimitive?.longOrNull ?: 0L,
                                userId = o["userId"]?.jsonPrimitive?.contentOrNull,
                                user = o["userName"]?.jsonPrimitive?.contentOrNull ?: "",
                                sport = o["sport"]?.jsonPrimitive?.contentOrNull ?: "Futbolito",
                                available = o["available"]?.jsonPrimitive?.intOrNull ?: 0,
                                total = o["total"]?.jsonPrimitive?.intOrNull ?: 0,
                                message = o["message"]?.jsonPrimitive?.contentOrNull ?: "",
                                locality = o["locality"]?.jsonPrimitive?.contentOrNull ?: "",
                                serverId = o["id"]?.jsonPrimitive?.contentOrNull
                            )
                            // Update list on main
                            scope.launch {
                                posts = if (type == "post_created") {
                                    // prepend if not exists
                                    val exists = posts.any { it.serverId == p.serverId }
                                    if (exists) posts.map { if (it.serverId == p.serverId) p else it } else listOf(p) + posts
                                } else {
                                    posts.map { if (it.serverId == p.serverId) p else it }
                                }
                            }
                        } else if (type == "post_deleted") {
                            val id = data?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                            if (id != null) {
                                scope.launch { posts = posts.filterNot { it.serverId == id } }
                            }
                        } else if (type == "message_new") {
                            val o = data?.jsonObject ?: return
                            val fromName = o["fromName"]?.jsonPrimitive?.contentOrNull ?: ""
                            val toName = o["toName"]?.jsonPrimitive?.contentOrNull ?: ""
                            val textMsg = o["text"]?.jsonPrimitive?.contentOrNull ?: ""
                            val t = o["time"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
                            val pid = o["postId"]?.jsonPrimitive?.contentOrNull
                            val partner = if (fromName.equals(user, ignoreCase = true)) toName else fromName
                            scope.launch {
                                val key = if (!pid.isNullOrBlank()) "$pid|$partner" else partner
                                val existing = chats[key] ?: emptyList()
                                val already = existing.any { it.time == t && it.text == textMsg && it.from.equals(fromName, ignoreCase = true) }
                                if (!already) {
                                    chats[key] = existing + ChatMessage(from = fromName, to = toName, text = textMsg, time = t)
                                    if (!fromName.equals(user, ignoreCase = true)) {
                                        val isPrivate = !pid.isNullOrBlank()
                                        val chatOpen = if (isPrivate) {
                                            chatRecipientName?.equals(partner, ignoreCase = true) == true && chatPostId == pid
                                        } else {
                                            chatRecipientName?.equals(partner, ignoreCase = true) == true && chatPostId == null
                                        }
                                        if (!chatOpen) {
                                            val title = if (isPrivate) "Nuevo mensaje de $partner" else "Nuevo mensaje"
                                            showChatNotification(
                                                context,
                                                title,
                                                textMsg,
                                                postId = pid,
                                                peerName = partner,
                                                isGroup = false
                                            )
                                            if (!pid.isNullOrBlank()) {
                                                val current = unreadByPost[pid] ?: 0
                                                unreadByPost[pid] = current + 1
                                                incrementUnreadForPost(context, pid)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (type == "post_message_new") {
                            val o = data?.jsonObject ?: return
                            val fromName = o["fromName"]?.jsonPrimitive?.contentOrNull ?: ""
                            val textMsg = o["text"]?.jsonPrimitive?.contentOrNull ?: ""
                            val t = o["time"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
                            val pid = o["postId"]?.jsonPrimitive?.contentOrNull
                            if (!pid.isNullOrBlank()) {
                                scope.launch {
                                    val key = "$pid|__group"
                                    val existing = chats[key] ?: emptyList()
                                    chats[key] = existing + ChatMessage(from = fromName, to = "", text = textMsg, time = t)
                                    if (!fromName.equals(user, ignoreCase = true)) {
                                        val chatOpen = groupPostId == pid
                                        if (!chatOpen) {
                                            val title = "Nuevo mensaje en comunidad"
                                            val body = "$fromName: $textMsg"
                                            showChatNotification(
                                                context,
                                                title,
                                                body,
                                                postId = pid,
                                                peerName = null,
                                                isGroup = true
                                            )
                                            val current = unreadByPost[pid] ?: 0
                                            unreadByPost[pid] = current + 1
                                            incrementUnreadForPost(context, pid)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (ws === webSocket) ws = null
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (ws === webSocket) ws = null
                }
            }
            try {
                httpClient.newWebSocket(req, listener)
            } catch (_: Exception) {}
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val stored = loadUnreadByPost(context)
            unreadByPost.clear()
            unreadByPost.putAll(stored)
            delay(1000)
        }
    }
    // Ensure socket is closed when leaving the screen
    DisposableEffect(ws) {
        onDispose {
            try { ws?.close(1000, "leaving") } catch (_: Exception) {}
            ws = null
        }
    }
    // Fallback: periodic sync if WebSocket is not connected
    LaunchedEffect(ws) {
        while (true) {
            if (ws == null) {
                val (at, rt) = loadTokens(context)
                if (!at.isNullOrBlank()) {
                    val remote = withContext(Dispatchers.IO) { fetchCommunityPostsRemote(context, at, rt).first }
                    if (remote != null) posts = remote
                }
            }
            delay(15000)
        }
    }

    val visiblePosts = remember(posts, filterSport, filterLocality) {
        var list = posts
        val sportFilter = filterSport
        if (!sportFilter.isNullOrBlank() && !sportFilter.equals("Todos", ignoreCase = true)) {
            list = list.filter { it.sport.equals(sportFilter, ignoreCase = true) }
        }
        val locFilter = filterLocality
        if (!locFilter.isNullOrBlank()) {
            list = list.filter { p ->
                p.locality.contains(locFilter, ignoreCase = true) ||
                        locFilter.contains(p.locality, ignoreCase = true)
            }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.community_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val (at, rt) = loadTokens(context)
                            val remote = withContext(Dispatchers.IO) { fetchCommunityPostsRemote(context, at, rt).first }
                            if (remote != null) {
                                posts = remote
                                snackbarHostState.showSnackbar(context.getString(R.string.post_published_remote))
                            } else {
                                posts = loadCommunityPosts(context)
                                snackbarHostState.showSnackbar(context.getString(R.string.post_published_local))
                            }
                        }
                    }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        val isValid = message.isNotBlank() && locality.isNotBlank() && available > 0 && total > 0 && available <= total
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    border = BorderStroke(1.dp, Color.Black),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text(stringResource(R.string.post_message_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = sport,
                                    onValueChange = { },
                                    label = { Text(stringResource(R.string.sport_label)) },
                                    modifier = Modifier.fillMaxWidth().clickable { createSportExpanded = true },
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { createSportExpanded = !createSportExpanded }) {
                                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color.White,
                                        focusedLabelColor = Color.White,
                                        unfocusedLabelColor = Color.White
                                    )
                                )
                                DropdownMenu(
                                    expanded = createSportExpanded,
                                    onDismissRequest = { createSportExpanded = false },
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    sports.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s, color = MaterialTheme.colorScheme.onPrimary) },
                                            colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                            onClick = {
                                                sport = s
                                                createSportExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                val localityText = if (localitySearch.isNotEmpty()) localitySearch else locality
                                OutlinedTextField(
                                    value = localityText,
                                    onValueChange = { text ->
                                        localitySearch = text
                                        locality = text
                                        localityExpanded = true
                                    },
                                    label = { Text(stringResource(R.string.locality_label)) },
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { localityExpanded = !localityExpanded }) {
                                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color.White,
                                        focusedLabelColor = Color.White,
                                        unfocusedLabelColor = Color.White
                                    )
                                )
                                DropdownMenu(
                                    expanded = localityExpanded,
                                    onDismissRequest = { localityExpanded = false },
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    if (localitySearch.isBlank()) {
                                        regionsWithCommunes.forEach { (region, communes) ->
                                            DropdownMenuItem(
                                                text = { Text(region, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold) },
                                                colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                                onClick = {
                                                    expandedRegions = if (expandedRegions.contains(region)) {
                                                        expandedRegions - region
                                                    } else {
                                                        expandedRegions + region
                                                    }
                                                }
                                            )
                                            if (expandedRegions.contains(region)) {
                                                communes.forEach { comuna ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = comuna,
                                                                color = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.padding(start = 16.dp)
                                                            )
                                                        },
                                                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                                        onClick = {
                                                            val full = "$region - $comuna"
                                                            locality = full
                                                            localitySearch = full
                                                            localityExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        val filtered = allLocalities.filter { it.contains(localitySearch, ignoreCase = true) }
                                        filtered.forEach { loc ->
                                            DropdownMenuItem(
                                                text = { Text(loc, color = MaterialTheme.colorScheme.onPrimary) },
                                                colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                                onClick = {
                                                    locality = loc
                                                    localitySearch = loc
                                                    localityExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = availableText,
                                onValueChange = { v ->
                                    val digits = v.filter { it.isDigit() }
                                    availableText = digits
                                    available = digits.toIntOrNull() ?: 0
                                },
                                label = { Text(stringResource(R.string.available_players)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { state ->
                                        if (state.isFocused && availableText == "0") {
                                            availableText = ""
                                        }
                                    },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White
                                )
                            )
                            OutlinedTextField(
                                value = totalText,
                                onValueChange = { v ->
                                    val digits = v.filter { it.isDigit() }
                                    totalText = digits
                                    total = digits.toIntOrNull() ?: 0
                                },
                                label = { Text(stringResource(R.string.total_players)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { state ->
                                        if (state.isFocused && totalText == "0") {
                                            totalText = ""
                                        }
                                    },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White
                                )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(
                                enabled = isValid,
                                onClick = {
                                    if (message.isBlank() || locality.isBlank() || available <= 0 || total <= 0) {
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.err_post_required)) }
                                        return@Button
                                    }
                                    if (available > total) {
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.err_post_counts)) }
                                        return@Button
                                    }
                                    val now = System.currentTimeMillis()
                                    val post = CommunityPost(
                                        id = now,
                                        time = now,
                                        user = if (user.isNotBlank()) user else "",
                                        sport = sport.ifBlank { "Futbolito" },
                                        available = available,
                                        total = total,
                                        message = message.trim(),
                                        locality = locality.trim()
                                    )
                                    scope.launch {
                                        // Remote create; fallback to local
                                        val (at, rt) = loadTokens(context)
                                        val body = JSONObject()
                                            .put("userName", post.user)
                                            .put("sport", post.sport)
                                            .put("available", post.available)
                                            .put("total", post.total)
                                            .put("message", post.message)
                                            .put("locality", post.locality)
                                            .put("time", post.time)
                                        val token = withContext(Dispatchers.IO) { createCommunityPostRemote(context, at, rt, body) }
                                        if (token != null) {
                                            val remote = withContext(Dispatchers.IO) { fetchCommunityPostsRemote(context, token, rt).first }
                                            if (remote != null) posts = remote
                                            message = ""; available = 0; total = 0; locality = ""
                                            snackbarHostState.showSnackbar(context.getString(R.string.post_published_remote))
                                        } else {
                                            addCommunityPost(context, post)
                                            posts = loadCommunityPosts(context)
                                            message = ""; available = 0; total = 0; locality = ""
                                            snackbarHostState.showSnackbar(context.getString(R.string.post_published_local))
                                        }
                                    }
                                }
                            , colors = ButtonDefaults.buttonColors(contentColor = Color.White)
                        ) { Text(stringResource(R.string.publish)) }
                        }
                    }
                }
            }
            item {
                Column {
                    Button(
                        onClick = { showFilters = !showFilters },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Buscar por deporte y comuna")
                    }
                    if (showFilters) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Filtro por deporte
                            Box(modifier = Modifier.weight(1f)) {
                                val currentFilterSport = filterSport ?: "Todos"
                                OutlinedTextField(
                                    value = currentFilterSport,
                                    onValueChange = { },
                                    label = { Text(stringResource(R.string.sport_label)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { filterSportExpanded = true },
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { filterSportExpanded = !filterSportExpanded }) {
                                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        cursorColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                DropdownMenu(
                                    expanded = filterSportExpanded,
                                    onDismissRequest = { filterSportExpanded = false },
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Todos", color = MaterialTheme.colorScheme.onPrimary) },
                                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                        onClick = {
                                            filterSport = null
                                            sport = ""
                                            filterSportExpanded = false
                                        }
                                    )
                                    sports.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s, color = MaterialTheme.colorScheme.onPrimary) },
                                            colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                            onClick = {
                                                filterSport = s
                                                sport = s
                                                filterSportExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            // Filtro por comuna
                            Box(modifier = Modifier.weight(1f)) {
                                val filterLocText = if (filterLocalitySearch.isNotEmpty()) filterLocalitySearch else (filterLocality ?: "")
                                OutlinedTextField(
                                    value = filterLocText,
                                    onValueChange = { text ->
                                        filterLocalitySearch = text
                                        filterLocality = text
                                        filterLocalityExpanded = true
                                    },
                                    label = { Text(stringResource(R.string.locality_label), color = Color(0xFF4CAF50)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            // Al volver a abrir el menú, limpiamos la búsqueda para permitir una nueva selección desde cero
                                            if (!filterLocalityExpanded) {
                                                filterLocalitySearch = ""
                                            }
                                            filterLocalityExpanded = !filterLocalityExpanded
                                        }) {
                                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        cursorColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                DropdownMenu(
                                    expanded = filterLocalityExpanded,
                                    onDismissRequest = { filterLocalityExpanded = false },
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    if (filterLocalitySearch.isBlank()) {
                                        regionsWithCommunes.forEach { (region, communes) ->
                                            DropdownMenuItem(
                                                text = { Text(region, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold) },
                                                colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                                onClick = {
                                                    filterExpandedRegions = if (filterExpandedRegions.contains(region)) {
                                                        filterExpandedRegions - region
                                                    } else {
                                                        filterExpandedRegions + region
                                                    }
                                                }
                                            )
                                            if (filterExpandedRegions.contains(region)) {
                                                communes.forEach { comuna ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = comuna,
                                                                color = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.padding(start = 16.dp)
                                                            )
                                                        },
                                                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                                        onClick = {
                                                            val full = "$region - $comuna"
                                                            filterLocality = full
                                                            filterLocalitySearch = full
                                                            locality = full
                                                            localitySearch = full
                                                            filterLocalityExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        val filtered = allLocalities.filter { it.contains(filterLocalitySearch, ignoreCase = true) }
                                        filtered.forEach { loc ->
                                            DropdownMenuItem(
                                                text = { Text(loc, color = MaterialTheme.colorScheme.onPrimary) },
                                                colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                                onClick = {
                                                    filterLocality = loc
                                                    filterLocalitySearch = loc
                                                    locality = loc
                                                    localitySearch = loc
                                                    filterLocalityExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.posts_list_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            if (visiblePosts.isEmpty()) {
                item { Text(stringResource(R.string.empty_posts), color = Color.White) }
            } else {
                items(visiblePosts) { p ->
                    val isOwner = p.user.isNotBlank() && user.isNotBlank() &&
                        p.user.trim().equals(user.trim(), ignoreCase = true)
                    val postTime = remember(p.time) {
                        try {
                            SimpleDateFormat("dd/MM HH:mm").format(Date(p.time))
                        } catch (_: Exception) { "" }
                    }
                    val unreadCount = p.serverId?.let { unreadByPost[it] ?: 0 } ?: 0
                    Card(
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (p.user == user) {
                                    if (!p.serverId.isNullOrBlank()) {
                                        threadsForPost = p
                                        unreadByPost.remove(p.serverId)
                                        clearUnreadForPost(context, p.serverId)
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.err_post_required)) }
                                    }
                                } else {
                                    if (!p.userId.isNullOrBlank() && !p.serverId.isNullOrBlank()) {
                                        chatRecipientName = p.user
                                        chatRecipientId = p.userId
                                        chatPostId = p.serverId
                                        cancelChatNotification(context, p.serverId, p.user, false)
                                        unreadByPost.remove(p.serverId)
                                        clearUnreadForPost(context, p.serverId)
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.err_post_required))
                                        }
                                    }
                                }
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_tshirt),
                                        contentDescription = null
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(
                                                R.string.post_format_title,
                                                p.user.ifBlank { "" },
                                                p.sport,
                                                p.available,
                                                p.total
                                            ),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (postTime.isNotEmpty()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                postTime,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                if (isOwner) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = {
                                            if (!p.serverId.isNullOrBlank()) {
                                                threadsForPost = p
                                            } else {
                                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.err_post_required)) }
                                            }
                                        }) {
                                            Text(stringResource(R.string.chat))
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                if (!p.serverId.isNullOrBlank()) {
                                                    groupPostId = p.serverId
                                                    cancelChatNotification(context, p.serverId, null, true)
                                                    unreadByPost.remove(p.serverId)
                                                    clearUnreadForPost(context, p.serverId)
                                                } else {
                                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.err_post_required)) }
                                                }
                                            }
                                        ) {
                                            Text("Chat general")
                                        }
                                        IconButton(onClick = {
                                            if (!p.serverId.isNullOrBlank()) {
                                                threadsForPost = p
                                            } else {
                                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.err_post_required)) }
                                            }
                                        }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Message,
                                                contentDescription = stringResource(R.string.chat)
                                            )
                                        }
                                        IconButton(onClick = {
                                            val togglingTo = editingId != p.id
                                            editingId = if (togglingTo) p.id else null
                                            if (togglingTo) {
                                                editingMessage = p.message
                                                editingSport = p.sport
                                                editingAvailable = p.available
                                                editingTotal = p.total
                                                editingLocality = p.locality
                                            } else {
                                                editingMessage = ""
                                                editingLocality = ""
                                                editingSport = "Futbolito"
                                                editingAvailable = 0
                                                editingTotal = 0
                                            }
                                        }) {
                                            Icon(Icons.Filled.Edit, contentDescription = null)
                                        }
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = {
                                            if (!p.userId.isNullOrBlank() && !p.serverId.isNullOrBlank()) {
                                                chatRecipientName = p.user
                                                chatRecipientId = p.userId
                                                chatPostId = p.serverId
                                                cancelChatNotification(context, p.serverId, p.user, false)
                                                unreadByPost.remove(p.serverId)
                                                clearUnreadForPost(context, p.serverId)
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.err_post_required))
                                                }
                                            }
                                        }) {
                                            Text(stringResource(R.string.chat))
                                        }
                                        IconButton(onClick = {
                                            if (!p.userId.isNullOrBlank() && !p.serverId.isNullOrBlank()) {
                                                chatRecipientName = p.user
                                                chatRecipientId = p.userId
                                                chatPostId = p.serverId
                                                cancelChatNotification(context, p.serverId, p.user, false)
                                                unreadByPost.remove(p.serverId)
                                                clearUnreadForPost(context, p.serverId)
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.err_post_required))
                                                }
                                            }
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = stringResource(R.string.chat))
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (unreadCount > 0) {
                                    Text(
                                        if (unreadCount == 1) "1 mensaje nuevo" else "$unreadCount mensajes nuevos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFFCDD2),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Spacer(Modifier.width(1.dp))
                                }
                                FilledIconButton(
                                    onClick = { deletingPost = p },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Red
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.delete)
                                    )
                                }
                            }
                            if (editingId == p.id) {
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = editingSport,
                                            onValueChange = { },
                                            label = { Text(stringResource(R.string.sport_label)) },
                                            modifier = Modifier.fillMaxWidth().clickable { editSportExpanded = true },
                                            readOnly = true,
                                            trailingIcon = {
                                                IconButton(onClick = { editSportExpanded = !editSportExpanded }) {
                                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                cursorColor = MaterialTheme.colorScheme.onSurface,
                                                focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        DropdownMenu(
                                            expanded = editSportExpanded,
                                            onDismissRequest = { editSportExpanded = false },
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            sports.forEach { s ->
                                                DropdownMenuItem(
                                                    text = { Text(s, color = MaterialTheme.colorScheme.onPrimary) },
                                                    colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                                    onClick = {
                                                        editingSport = s
                                                        editSportExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        val editingLocalityText = if (editingLocalitySearch.isNotEmpty()) editingLocalitySearch else editingLocality
                                        OutlinedTextField(
                                            value = editingLocalityText,
                                            onValueChange = { text ->
                                                editingLocalitySearch = text
                                                editingLocality = text
                                                editingLocalityExpanded = true
                                            },
                                            label = { Text(stringResource(R.string.locality_label)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            trailingIcon = {
                                                IconButton(onClick = { editingLocalityExpanded = !editingLocalityExpanded }) {
                                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                cursorColor = Color.White,
                                                focusedLabelColor = Color.White,
                                                unfocusedLabelColor = Color.White
                                            )
                                        )
                                        DropdownMenu(
                                            expanded = editingLocalityExpanded,
                                            onDismissRequest = { editingLocalityExpanded = false },
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            if (editingLocalitySearch.isBlank()) {
                                                regionsWithCommunes.forEach { (region, communes) ->
                                                    DropdownMenuItem(
                                                        text = { Text(region, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold) },
                                                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                                        onClick = {
                                                            expandedRegions = if (expandedRegions.contains(region)) {
                                                                expandedRegions - region
                                                            } else {
                                                                expandedRegions + region
                                                            }
                                                        }
                                                    )
                                                    if (expandedRegions.contains(region)) {
                                                        communes.forEach { comuna ->
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(
                                                                        text = comuna,
                                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                                        modifier = Modifier.padding(start = 16.dp)
                                                                    )
                                                                },
                                                                colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                                                onClick = {
                                                                    val full = "$region - $comuna"
                                                                    editingLocality = full
                                                                    editingLocalitySearch = full
                                                                    editingLocalityExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val filtered = allLocalities.filter { it.contains(editingLocalitySearch, ignoreCase = true) }
                                                filtered.forEach { loc ->
                                                    DropdownMenuItem(
                                                        text = { Text(loc, color = MaterialTheme.colorScheme.onPrimary) },
                                                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                                        onClick = {
                                                            editingLocality = loc
                                                            editingLocalitySearch = loc
                                                            editingLocalityExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = editingAvailable.toString(),
                                        onValueChange = { v -> editingAvailable = v.filter { it.isDigit() }.toIntOrNull() ?: 0 },
                                        label = { Text(stringResource(R.string.available_players)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = Color.White,
                                            focusedLabelColor = Color.White,
                                            unfocusedLabelColor = Color.White
                                        )
                                    )
                                    OutlinedTextField(
                                        value = editingTotal.toString(),
                                        onValueChange = { v -> editingTotal = v.filter { it.isDigit() }.toIntOrNull() ?: 0 },
                                        label = { Text(stringResource(R.string.total_players)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = Color.White,
                                            focusedLabelColor = Color.White,
                                            unfocusedLabelColor = Color.White
                                        )
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = editingMessage,
                                    onValueChange = { editingMessage = it },
                                    label = { Text(stringResource(R.string.post_message_label)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false,
                                    minLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color.White,
                                        focusedLabelColor = Color.White,
                                        unfocusedLabelColor = Color.White
                                    )
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        editingId = null
                                        editingMessage = ""
                                        editingLocality = ""
                                        editingSport = "Futbolito"
                                        editingAvailable = 0
                                        editingTotal = 0
                                    }) { Text(stringResource(android.R.string.cancel)) }
                                    Button(onClick = {
                                        val idLocal = editingId ?: return@Button
                                        if (editingMessage.isBlank() || editingLocality.isBlank() || editingAvailable <= 0 || editingTotal <= 0) {
                                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.err_post_required)) }
                                            return@Button
                                        }
                                        if (editingAvailable > editingTotal) {
                                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.err_post_counts)) }
                                            return@Button
                                        }
                                        val trimmed = editingMessage.trim()
                                        val current = posts.find { it.id == idLocal }
                                        val sid = current?.serverId
                                        scope.launch {
                                            if (!sid.isNullOrBlank()) {
                                                val (at, rt) = loadTokens(context)
                                                val body = JSONObject()
                                                    .put("userName", current?.user ?: "")
                                                    .put("sport", editingSport.ifBlank { "Futbolito" })
                                                    .put("available", editingAvailable)
                                                    .put("total", editingTotal)
                                                    .put("message", trimmed)
                                                    .put("locality", editingLocality.trim())
                                                val token = withContext(Dispatchers.IO) { updateCommunityPostRemote(context, at, rt, sid, body) }
                                                if (token != null) {
                                                    val remote = withContext(Dispatchers.IO) { fetchCommunityPostsRemote(context, token, rt).first }
                                                    if (remote != null) posts = remote
                                                    snackbarHostState.showSnackbar(context.getString(R.string.post_updated_remote))
                                                }
                                            } else {
                                                val updated = posts.map { post ->
                                                    if (post.id == idLocal) post.copy(
                                                        message = trimmed,
                                                        sport = editingSport.ifBlank { "Futbolito" },
                                                        available = editingAvailable,
                                                        total = editingTotal,
                                                        locality = editingLocality.trim()
                                                    ) else post
                                                }
                                                saveCommunityPosts(context, updated)
                                                posts = updated
                                                snackbarHostState.showSnackbar(context.getString(R.string.post_updated_local))
                                            }
                                            editingId = null
                                            editingMessage = ""
                                            editingLocality = ""
                                            editingSport = "Futbolito"
                                            editingAvailable = 0
                                            editingTotal = 0
                                        }
                                    }, enabled = editingMessage.isNotBlank() && editingLocality.isNotBlank() && editingAvailable > 0 && editingTotal > 0 && editingAvailable <= editingTotal) { Text(stringResource(android.R.string.ok)) }
                                }
                            } else {
                                if (p.message.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        p.message,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Place,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            p.locality,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (postTime.isNotEmpty()) {
                                        Text(
                                            postTime,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val recipient = chatRecipientName
    val toDelete = deletingPost
    val tfp = threadsForPost
    val groupPid = groupPostId
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingPost = null },
            title = { Text(stringResource(R.string.delete), color = Color.White) },
            text = { Text("¿Seguro que quieres borrar este post?") },
            confirmButton = {
                Button(onClick = {
                    val post = toDelete ?: run {
                        deletingPost = null
                        return@Button
                    }
                    val sid = post.serverId
                    if (!sid.isNullOrBlank()) {
                        scope.launch {
                            val (at, rt) = loadTokens(context)
                            val token = withContext(Dispatchers.IO) { deleteCommunityPostRemote(context, at, rt, sid) }
                            if (token != null) {
                                val remote = withContext(Dispatchers.IO) { fetchCommunityPostsRemote(context, token, rt).first }
                                if (remote != null) posts = remote
                                snackbarHostState.showSnackbar(context.getString(R.string.post_deleted_remote))
                            } else {
                                val updated = posts.filterNot { it.id == post.id }
                                saveCommunityPosts(context, updated)
                                posts = updated
                                snackbarHostState.showSnackbar(context.getString(R.string.post_deleted_local))
                            }
                            deletingPost = null
                        }
                    } else {
                        val updated = posts.filterNot { it.id == post.id }
                        saveCommunityPosts(context, updated)
                        posts = updated
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.post_deleted_local)) }
                        deletingPost = null
                    }
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { deletingPost = null }) { Text(stringResource(android.R.string.cancel)) } },
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            textContentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
    if (tfp != null) {
        LaunchedEffect(tfp.serverId) {
            val sid = tfp.serverId
            if (!sid.isNullOrBlank()) {
                threadsLoading = true
                val (at, rt) = loadTokens(context)
                if (!at.isNullOrBlank()) {
                    val pair = withContext(Dispatchers.IO) { fetchThreadsRemote(context, at, rt, sid) }
                    val list = pair.first
                    threads = list ?: emptyList()
                } else {
                    threads = emptyList()
                }
                threadsLoading = false
            }
        }
        AlertDialog(
            onDismissRequest = {
                threadsForPost = null
                threads = emptyList()
            },
            title = { Text(stringResource(R.string.chat), color = Color.White) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    if (threadsLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cargando conversaciones...")
                        }
                    } else if (threads.isEmpty()) {
                        Text("Sin mensajes para este post")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxHeight()) {
                            items(threads) { th ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            chatRecipientName = th.userName
                                            chatRecipientId = th.userId
                                            chatPostId = tfp.serverId
                                            cancelChatNotification(context, tfp.serverId, th.userName, false)
                                            val key = tfp.serverId
                                            if (!key.isNullOrBlank()) {
                                                unreadByPost.remove(key)
                                                clearUnreadForPost(context, key)
                                            }
                                            threadsForPost = null
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(th.userName, fontWeight = FontWeight.SemiBold)
                                        if (th.lastText.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(th.lastText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    threadsForPost = null
                    threads = emptyList()
                }) { Text(stringResource(R.string.close)) }
            }
        )
    }
    if (recipient != null) {
        LaunchedEffect(recipient, chatRecipientId) {
            val toId = chatRecipientId
            val pid = chatPostId
            if (!toId.isNullOrBlank() && !pid.isNullOrBlank()) {
                val key = "$pid|$recipient"
                chatLoading = true
                val (at, rt) = loadTokens(context)
                if (!at.isNullOrBlank()) {
                    val pair = withContext(Dispatchers.IO) { fetchMessagesRemote(context, at, rt, toId, pid) }
                    val list = pair.first
                    if (list != null) chats[key] = list
                }
                chatLoading = false
            }
        }
        // Fallback: si el WebSocket no está conectado, hacer polling periódico del historial
        LaunchedEffect(ws, recipient, chatRecipientId, chatPostId) {
            val toId = chatRecipientId
            val pid = chatPostId
            if (recipient == null || toId.isNullOrBlank() || pid.isNullOrBlank()) return@LaunchedEffect
            val key = "$pid|$recipient"
            while (true) {
                if (ws == null) {
                    val (at, rt) = loadTokens(context)
                    if (!at.isNullOrBlank()) {
                        val pair = withContext(Dispatchers.IO) { fetchMessagesRemote(context, at, rt, toId, pid) }
                        val list = pair.first
                        if (list != null) chats[key] = list
                    }
                }
                delay(3000)
            }
        }
        ChatDialog(
            recipient = recipient,
            loading = chatLoading,
            onSend = { text ->
                val toId = chatRecipientId
                val pid = chatPostId
                if (toId.isNullOrBlank() || pid.isNullOrBlank()) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.err_post_required))
                    }
                    return@ChatDialog
                }
                val wsNow = ws
                val payload = JSONObject()
                    .put("type", "message_send")
                    .put("toUserId", toId)
                    .put("toName", recipient)
                    .put("fromName", user)
                    .put("text", text)
                    .put("time", System.currentTimeMillis())
                    .put("postId", pid)
                var sent = false
                if (wsNow != null) {
                    try { sent = wsNow.send(payload.toString()) } catch (_: Exception) { sent = false }
                }
                // Optimistic update
                val key = "$pid|$recipient"
                val existing = chats[key] ?: emptyList()
                chats[key] = existing + ChatMessage(from = user, to = recipient, text = text, time = System.currentTimeMillis())
                if (!sent) {
                    // Fallback a REST /messages
                    scope.launch {
                        val (at, rt) = loadTokens(context)
                        val body = JSONObject()
                            .put("toUserId", toId)
                            .put("toName", recipient)
                            .put("fromName", user)
                            .put("text", text)
                            .put("time", System.currentTimeMillis())
                            .put("postId", pid)
                        withContext(Dispatchers.IO) { sendMessageRemote(context, at, rt, body) }
                    }
                }
            },
            messages = run {
                val pid = chatPostId
                if (!pid.isNullOrBlank()) chats["$pid|$recipient"] ?: emptyList() else emptyList()
            },
            onDismiss = { chatRecipientName = null; chatRecipientId = null; chatPostId = null }
        )
    }
}

private val userNameState = mutableStateOf<String?>(null)

// WebSocket global para recibir notificaciones de chat aunque la app esté fuera de Comunidad
@Volatile
private var globalChatWebSocket: WebSocket? = null
@Volatile
private var isCommunityForeground: Boolean = false
@Volatile
private var isAppInForeground: Boolean = false

private fun ensureChatChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHAT_CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHAT_CHANNEL_ID,
                "Mensajes de chat",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }
}

private fun showChatNotification(
    context: Context,
    title: String,
    text: String,
    postId: String? = null,
    peerName: String? = null,
    isGroup: Boolean = false
) {
    ensureChatChannel(context)
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (!postId.isNullOrBlank()) {
            putExtra(EXTRA_CHAT_POST_ID, postId)
        }
        if (!peerName.isNullOrBlank()) {
            putExtra(EXTRA_CHAT_PEER_NAME, peerName)
        }
        putExtra(EXTRA_CHAT_IS_GROUP, isGroup)
    }
    val pendingIntent = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
    val id = run {
        val key = buildString {
            append("chat|")
            append(postId ?: "")
            append("|")
            append(peerName ?: "")
            append("|")
            append(if (isGroup) "1" else "0")
        }
        key.hashCode()
    }
    val notification = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_tshirt)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()
    try {
        with(NotificationManagerCompat.from(context)) {
            notify(id, notification)
        }
    } catch (_: SecurityException) {
        // El permiso de notificaciones puede haber sido rechazado en tiempo de ejecución
    }
}

private fun cancelChatNotification(
    context: Context,
    postId: String?,
    peerName: String?,
    isGroup: Boolean
) {
    val id = run {
        val key = buildString {
            append("chat|")
            append(postId ?: "")
            append("|")
            append(peerName ?: "")
            append("|")
            append(if (isGroup) "1" else "0")
        }
        key.hashCode()
    }
    try {
        NotificationManagerCompat.from(context).cancel(id)
    } catch (_: Exception) {
    }
}

private fun startGlobalChatListener(context: Context) {
    if (globalChatWebSocket != null) return
    val (access, _) = loadTokens(context)
    if (access.isNullOrBlank()) return
    val wsUrl = BASE_URL.replaceFirst("https", "wss").replaceFirst("http", "ws") + "/ws"
    val req = Request.Builder().url(wsUrl).header("Authorization", "Bearer $access").build()
    val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            globalChatWebSocket = webSocket
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val root = Json.parseToJsonElement(text).jsonObject
                val type = root["type"]?.jsonPrimitive?.contentOrNull
                val data = root["data"]
                val currentUser = userNameState.value
                if (type == "message_new") {
                    val o = data?.jsonObject ?: return
                    val fromName = o["fromName"]?.jsonPrimitive?.contentOrNull ?: ""
                    val toName = o["toName"]?.jsonPrimitive?.contentOrNull ?: ""
                    val textMsg = o["text"]?.jsonPrimitive?.contentOrNull ?: ""
                    val pid = o["postId"]?.jsonPrimitive?.contentOrNull
                    if (currentUser != null && fromName.equals(currentUser, ignoreCase = true)) return
                    val partner = if (currentUser != null && fromName.equals(currentUser, ignoreCase = true)) toName else fromName
                    val title = if (!partner.isNullOrBlank()) "Nuevo mensaje de $partner" else "Nuevo mensaje"
                    // Evitar notificación sólo si la pantalla de Comunidad está visible y la app en primer plano
                    val shouldShowNotification = !(isCommunityForeground && isAppInForeground)
                    if (shouldShowNotification) {
                        showChatNotification(
                            context,
                            title,
                            textMsg,
                            postId = pid,
                            peerName = partner,
                            isGroup = false
                        )
                    }
                    if (!pid.isNullOrBlank()) {
                        incrementUnreadForPost(context, pid)
                    }
                } else if (type == "post_message_new") {
                    val o = data?.jsonObject ?: return
                    val fromName = o["fromName"]?.jsonPrimitive?.contentOrNull ?: ""
                    val textMsg = o["text"]?.jsonPrimitive?.contentOrNull ?: ""
                    val pid = o["postId"]?.jsonPrimitive?.contentOrNull
                    if (currentUser != null && fromName.equals(currentUser, ignoreCase = true)) return
                    val title = "Nuevo mensaje en comunidad"
                    val body = if (fromName.isNotBlank()) "$fromName: $textMsg" else textMsg
                    // Evitar notificación sólo si la pantalla de Comunidad está visible y la app en primer plano
                    val shouldShowNotification = !(isCommunityForeground && isAppInForeground)
                    if (shouldShowNotification) {
                        showChatNotification(
                            context,
                            title,
                            body,
                            postId = pid,
                            peerName = null,
                            isGroup = true
                        )
                    }
                    if (!pid.isNullOrBlank()) {
                        incrementUnreadForPost(context, pid)
                    }
                }
            } catch (_: Exception) {}
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (globalChatWebSocket === webSocket) globalChatWebSocket = null
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (globalChatWebSocket === webSocket) globalChatWebSocket = null
        }
    }
    try {
        httpClient.newWebSocket(req, listener)
    } catch (_: Exception) {}
}

private fun stopGlobalChatListener() {
    try {
        globalChatWebSocket?.close(1000, "logout")
    } catch (_: Exception) {}
    globalChatWebSocket = null
}

// ===== Network & Auth helpers (top-level) =====
private fun httpPostRaw(path: String, json: String, token: String? = null): Pair<Int, String?> {
    val req = Request.Builder()
        .url("$BASE_URL$path")
        .post(json.toRequestBody(JSON_MEDIA))
        .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
        .build()
    httpClient.newCall(req).execute().use { resp ->
        val text = resp.body?.string()
        return resp.code to text
    }
}

private suspend fun postJsonWithRetry(path: String, body: JSONObject): Pair<Int, String?> {
    var attempt = 0
    var last: Pair<Int, String?> = 0 to null
    while (attempt < 2) {
        last = try {
            withContext(Dispatchers.IO) { httpPostJson(path, body) }
        } catch (_: Exception) { 0 to null }
        val (code, text) = last
        val trimmed = text?.trimStart()
        val looksHtml = trimmed?.startsWith("<!DOCTYPE", ignoreCase = true) == true || trimmed?.startsWith("<html", ignoreCase = true) == true
        if (code in 200..299 && !looksHtml) return last
        if (code in 500..599 || looksHtml) {
            delay(800)
            attempt++
            continue
        }
        return last
    }
    return last
}

private fun httpPostJson(path: String, body: JSONObject, token: String? = null): Pair<Int, String?> {
    val req = Request.Builder()
        .url("$BASE_URL$path")
        .post(body.toString().toRequestBody(JSON_MEDIA))
        .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
        .build()
    httpClient.newCall(req).execute().use { resp ->
        val text = resp.body?.string()
        return resp.code to text
    }
}

private fun httpGet(path: String, token: String): Pair<Int, String?> {
    val req = Request.Builder().url("$BASE_URL$path").get().header("Authorization", "Bearer $token").build()
    httpClient.newCall(req).execute().use { resp ->
        val text = resp.body?.string()
        return resp.code to text
    }
}

private fun saveTokens(context: Context, access: String?, refresh: String?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
        if (access == null) remove(KEY_ACCESS_TOKEN) else putString(KEY_ACCESS_TOKEN, access)
        if (refresh == null) remove(KEY_REFRESH_TOKEN) else putString(KEY_REFRESH_TOKEN, refresh)
    }.apply()
}

private fun loadTokens(context: Context): Pair<String?, String?> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_ACCESS_TOKEN, null) to prefs.getString(KEY_REFRESH_TOKEN, null)
}

private fun saveUserName(context: Context, name: String?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
        if (name == null) remove(KEY_USER_NAME) else putString(KEY_USER_NAME, name)
    }.apply()
}

private fun loadUserName(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_USER_NAME, null)
}

 private fun tryRefresh(context: Context, refreshToken: String?): String? {
    if (refreshToken.isNullOrBlank()) return null
    return try {
        val body = JSONObject().put("refreshToken", refreshToken)
        val (code, text) = httpPostJson("/auth/refresh", body)
        if (code in 200..299 && !text.isNullOrBlank()) {
            val obj = JSONObject(text)
            val at = obj.optString("accessToken").takeIf { it.isNotBlank() }
            if (at != null) {
                saveTokens(context, at, refreshToken)
                at
            } else null
        } else null
    } catch (_: Exception) { null }
}

private fun fetchPlayersRemote(context: Context, access: String?, refresh: String?): Pair<List<Player>?, String?> {
    if (access.isNullOrBlank()) return null to access
    var token = access
    val (code, text) = try {
        httpGet("/players", token)
    } catch (_: Exception) {
        return null to token
    }
    val trimmed = text?.trimStart()
    val looksHtml = trimmed?.startsWith("<!DOCTYPE", ignoreCase = true) == true ||
            trimmed?.startsWith("<html", ignoreCase = true) == true
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val (code2, text2) = try {
                httpGet("/players", token)
            } catch (_: Exception) {
                return null to token
            }
            val trimmed2 = text2?.trimStart()
            val looksHtml2 = trimmed2?.startsWith("<!DOCTYPE", ignoreCase = true) == true ||
                    trimmed2?.startsWith("<html", ignoreCase = true) == true
            if (code2 in 200..299 && !text2.isNullOrBlank() && !looksHtml2) {
                return try {
                    val arr = JSONArray(text2)
                    val list = mutableListOf<Player>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list += Player(
                            o.getString("name"),
                            o.getDouble("attack"),
                            o.getDouble("defense"),
                            o.getDouble("physical"),
                            o.optBoolean("isGoalkeeper", false)
                        )
                    }
                    list to token
                } catch (_: Exception) {
                    null to token
                }
            }
        }
        return null to token
    }
    if (code in 200..299 && !text.isNullOrBlank() && !looksHtml) {
        return try {
            val arr = JSONArray(text)
            val list = mutableListOf<Player>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += Player(
                    o.getString("name"),
                    o.getDouble("attack"),
                    o.getDouble("defense"),
                    o.getDouble("physical"),
                    o.optBoolean("isGoalkeeper", false)
                )
            }
            list to token
        } catch (_: Exception) {
            null to token
        }
    }
    return null to token
}

private fun postPlayersBulkRemote(context: Context, access: String?, refresh: String?, players: List<Player>): String? {
    if (access.isNullOrBlank()) return null
    val arr = JSONArray(players.map { JSONObject().put("name", it.name).put("attack", it.attack).put("defense", it.defense).put("physical", it.physical).put("isGoalkeeper", it.isGoalkeeper) })
    var token = access
    var (code, _) = httpPostJson("/players/bulk", JSONObject().put("players", arr), token)
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val res2 = httpPostJson("/players/bulk", JSONObject().put("players", arr), token)
            code = res2.first
        }
    }
    return if (code in 200..299) token else null
}

private fun postMatchRemote(context: Context, access: String?, refresh: String?, titleA: String, titleB: String, teamA: List<Player>, teamB: List<Player>): String? {
    if (access.isNullOrBlank()) return null
    val body = JSONObject()
        .put("time", System.currentTimeMillis())
        .put("titleA", titleA)
        .put("titleB", titleB)
        .put("teamA", JSONArray(teamA.map {
            JSONObject()
                .put("name", it.name)
                .put("isGoalkeeper", it.isGoalkeeper)
                .put("isCaptain", it.isCaptain)
                .put("hasYellowCard", it.hasYellowCard)
                .put("hasRedCard", it.hasRedCard)
        }))
        .put("teamB", JSONArray(teamB.map {
            JSONObject()
                .put("name", it.name)
                .put("isGoalkeeper", it.isGoalkeeper)
                .put("isCaptain", it.isCaptain)
                .put("hasYellowCard", it.hasYellowCard)
                .put("hasRedCard", it.hasRedCard)
        }))
        .put("result", "")
    var token = access
    var (code, _) = httpPostJson("/matches", body, token)
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val res2 = httpPostJson("/matches", body, token)
            code = res2.first
        }
    }
    return if (code in 200..299) token else null
}

class MainActivity : AppCompatActivity() {
    // Registro para abrir el menú secreto desde eventos de hardware
    private var openSecretMenu: (() -> Unit)? = null
    private var lastVolUpTime: Long = -1L
    private var lastVolDownTime: Long = -1L
    private var pendingChatPostId: String? = null
    private var pendingChatPeerName: String? = null
    private var pendingChatIsGroup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleChatIntent(intent)
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (currentTags.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
        }
        setContent {
            EquiposTheme {
                AppScaffold(
                    registerSecretOpener = { opener -> openSecretMenu = opener },
                    pendingChatPostId = pendingChatPostId,
                    pendingChatPeerName = pendingChatPeerName,
                    pendingChatIsGroup = pendingChatIsGroup,
                    clearPendingChat = {
                        pendingChatPostId = null
                        pendingChatPeerName = null
                        pendingChatIsGroup = false
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleChatIntent(intent)
    }

    private fun handleChatIntent(intent: Intent) {
        pendingChatPostId = intent.getStringExtra(EXTRA_CHAT_POST_ID)
        pendingChatPeerName = intent.getStringExtra(EXTRA_CHAT_PEER_NAME)
        pendingChatIsGroup = intent.getBooleanExtra(EXTRA_CHAT_IS_GROUP, false)
    }

    override fun onStart() {
        super.onStart()
        isAppInForeground = true
    }

    override fun onStop() {
        super.onStop()
        isAppInForeground = false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val now = SystemClock.uptimeMillis()
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                lastVolUpTime = now
                if (lastVolDownTime > 0 && (now - lastVolDownTime) <= 300) {
                    openSecretMenu?.invoke()
                    return true
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                lastVolDownTime = now
                if (lastVolUpTime > 0 && (now - lastVolUpTime) <= 300) {
                    openSecretMenu?.invoke()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

private fun List<Player>.avgRating(): Double {
    if (isEmpty()) return 0.0
    return this.sumOf { it.rating } / this.size
}

private fun playerLine(p: Player): String {
    val gk = if (p.isGoalkeeper) "(GK) " else ""
    val captain = if (p.isCaptain) " (C)" else ""
    val yellow = if (p.hasYellowCard) " [TA]" else ""
    val red = if (p.hasRedCard) " [TR]" else ""
    return "• ${gk}${p.name}$captain$yellow$red"
}

private fun formatTeamBlock(context: Context, title: String, team: List<Player>): String {
    val header = "$title (${team.size})"
    val body = team.joinToString("\n") { playerLine(it) }
    return "$header\n$body"
}

private fun formatTeamsText(context: Context, titleA: String, titleB: String, teamA: List<Player>, teamB: List<Player>): String {
    val a = formatTeamBlock(context, titleA, teamA)
    val b = formatTeamBlock(context, titleB, teamB)
    return "$a\n\n$b"
}

private fun formatSavedMatchText(context: Context, m: SavedMatch): String {
    val a = formatTeamBlock(context, m.titleA, m.teamA)
    val b = formatTeamBlock(context, m.titleB, m.teamB)
    val resultLabel = context.getString(R.string.result_label)
    val result = if (m.result.isNotBlank()) "\n\n$resultLabel: ${m.result}" else ""
    return "$a\n\n$b$result"
}

private fun createPitchBitmap(
    teamA: List<Player>,
    teamB: List<Player>,
    sport: String,
    teamAColor: Int = AndroidColor.parseColor("#FFEB3B"),
    teamBColor: Int = AndroidColor.parseColor("#03A9F4"),
    width: Int = 800,
    height: Int = 1400
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val fieldPaint = Paint().apply {
        color = AndroidColor.parseColor("#43A047")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val linePaint = Paint().apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = (kotlin.math.min(width, height) * 0.01f)
        isAntiAlias = true
    }
    val textPaint = Paint().apply {
        color = AndroidColor.WHITE
        textAlign = Paint.Align.CENTER
        textSize = width * 0.04f
        isAntiAlias = true
    }

    val isTennis = sport.equals("Tenis", ignoreCase = true)
    val isPadel = sport.equals("Pádel", ignoreCase = true) || sport.equals("Padel", ignoreCase = true)
    val isVolleyball = sport.equals("Voleybol", ignoreCase = true) || sport.equals("Vóleibol", ignoreCase = true)
    val isBabyFootball = sport.equals("Baby Fútbol", ignoreCase = true) || sport.equals("Baby Futbol", ignoreCase = true) || sport.equals("Baby futbol", ignoreCase = true) || sport.equals("Baby fútbol", ignoreCase = true)
    val isFootball = !isTennis && !isPadel && !isVolleyball && !isBabyFootball

    val padelCourtPaint = Paint().apply {
        // Zona azul de la cancha (#2EA4DA)
        color = AndroidColor.parseColor("#2EA4DA")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val padelWallPaint = Paint().apply {
        // Borde azul oscuro de la zona de juego (#082A40)
        color = AndroidColor.parseColor("#082A40")
        style = Paint.Style.STROKE
        strokeWidth = linePaint.strokeWidth * 1.5f
        isAntiAlias = true
    }
    val volleyCourtPaint = Paint().apply {
        color = AndroidColor.parseColor("#FF9800")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Fondo de cancha (siempre ocupa todo el bitmap)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fieldPaint)

    // Escala más notoria para fútbol, tenis y voleybol: todo el diseño se dibuja más pequeño
    if (isFootball || isBabyFootball || isTennis || isVolleyball) {
        canvas.save()
        canvas.scale(0.8f, 0.8f, width / 2f, height / 2f)
    }

    // Borde
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), linePaint)

    val centerY = height / 2f
    val boxHeight = height * 0.18f

    if (isTennis) {
        // Cancha de tenis basada en el nuevo SVG (800x1400), escalada al tamaño actual
        val scaleX = width / 36f
        val scaleY = height / 78f

        fun sx(x: Float) = x * scaleX
        fun sy(y: Float) = y * scaleY

        // Fondo verde específico de tenis (#66CC66) dentro del área escalada
        val tennisBgPaint = Paint().apply {
            color = AndroidColor.parseColor("#2F9E3A")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), tennisBgPaint)

        val baseStroke = linePaint.strokeWidth
        val mainStroke = baseStroke * 0.75f   // líneas internas ~6px si base≈8
        val outerStroke = baseStroke          // borde exterior ~8px

        val mainLinePaint = Paint(linePaint).apply { strokeWidth = mainStroke }
        val outerLinePaint = Paint(linePaint).apply { strokeWidth = outerStroke }
        val netLinePaint = Paint(linePaint).apply { strokeWidth = mainStroke * 1.2f }

        // Líneas verticales (x=4.5, 18 [solo entre 18..60], 31.5)
        canvas.drawLine(sx(4.5f), sy(0f), sx(4.5f), sy(78f), mainLinePaint)
        canvas.drawLine(sx(18f), sy(18f), sx(18f), sy(60f), mainLinePaint)
        canvas.drawLine(sx(31.5f), sy(0f), sx(31.5f), sy(78f), mainLinePaint)

        // Líneas horizontales (servicio y red)
        canvas.drawLine(sx(4.5f), sy(18f), sx(31.5f), sy(18f), mainLinePaint)
        canvas.drawLine(sx(0f), sy(39f), sx(36f), sy(39f), netLinePaint)
        canvas.drawLine(sx(4.5f), sy(60f), sx(31.5f), sy(60f), mainLinePaint)
        // Líneas de fondo (baselines)
        canvas.drawLine(sx(0f), sy(0f), sx(36f), sy(0f), mainLinePaint)
        canvas.drawLine(sx(0f), sy(78f), sx(36f), sy(78f), mainLinePaint)

        // Borde exterior (marco) rect(0.075,0.075, 35.85x77.85)
        val outerRect = android.graphics.RectF(
            sx(0.075f),
            sy(0.075f),
            sx(0.075f + 35.85f),
            sy(0.075f + 77.85f)
        )
        canvas.drawRect(outerRect, outerLinePaint)

        // Marcas centrales (ticks) superior e inferior
        val tickPaint = Paint(linePaint).apply {
            strokeWidth = mainStroke * 0.8f
            isAntiAlias = true
        }
        // Superior: rect(392,28,16x6)
        canvas.drawLine(sx(18f), sy(-0.8f), sx(18f), sy(0.8f), tickPaint)
        // Inferior: rect(392,1366,16x6)
        canvas.drawLine(sx(18f), sy(77.2f), sx(18f), sy(78.8f), tickPaint)

        // Postes de red (opcionales)
        val postPaint = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(sx(0.075f), sy(38.6f), sx(0.075f + 0.05f), sy(38.6f + 0.8f), postPaint)
        canvas.drawRect(sx(35.875f), sy(38.6f), sx(35.875f + 0.05f), sy(38.6f + 0.8f), postPaint)
  } else if (isPadel) {
        // Cancha de pádel basada en el nuevo SVG (800x1400), escalada al tamaño actual
        val scaleX = width / 800f
        val scaleY = height / 1400f

        fun sx(x: Float) = x * scaleX
        fun sy(y: Float) = y * scaleY

        // Pinturas blancas con distinto grosor
        val whiteThickPaint = Paint(linePaint).apply {
            // Línea central gruesa (aprox. 12px cuando base es 8px)
            strokeWidth = linePaint.strokeWidth * 1.5f
        }
        val whiteThinPaint = Paint(linePaint).apply {
            // Líneas delgadas laterales y vertical central (aprox. 4px)
            strokeWidth = linePaint.strokeWidth * 0.5f
        }

        // Zona azul (cancha)
        canvas.drawRect(
            sx(80f),
            sy(40f),
            sx(80f + 640f),
            sy(40f + 1320f),
            padelCourtPaint
        )

        // Borde de la zona azul (marco)
        canvas.drawRect(
            sx(80f),
            sy(40f),
            sx(80f + 640f),
            sy(40f + 1320f),
            padelWallPaint
        )

        // LÍNEA CENTRAL GRUESA
        canvas.drawLine(
            sx(80f),
            sy(700f),
            sx(720f),
            sy(700f),
            whiteThickPaint
        )

        // LÍNEAS DELGADAS LATERALES SUPERIOR / INFERIOR
        canvas.drawLine(
            sx(80f),
            sy(360f),
            sx(720f),
            sy(360f),
            whiteThinPaint
        )
        canvas.drawLine(
            sx(80f),
            sy(1040f),
            sx(720f),
            sy(1040f),
            whiteThinPaint
        )

        // LÍNEA VERTICAL DELGADA (solo entre las horizontales delgadas)
        canvas.drawLine(
            sx(400f),
            sy(360f),
            sx(400f),
            sy(1040f),
            whiteThinPaint
        )

        // Marcos exteriores azul oscuro arriba/abajo (#073150)
        val padelBarPaint = Paint().apply {
            color = AndroidColor.parseColor("#073150")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(
            sx(70f),
            sy(30f),
            sx(70f + 660f),
            sy(30f + 20f),
            padelBarPaint
        )
        canvas.drawRect(
            sx(70f),
            sy(1350f),
            sx(70f + 660f),
            sy(1350f + 20f),
            padelBarPaint
        )
    } else if (isVolleyball) {
        // Escala basada en el SVG de vóley (viewBox 9x18)
        val scaleX = width / 9f
        val scaleY = height / 18f
        val unit = kotlin.math.min(scaleX, scaleY)

        fun sx(x: Float) = x * scaleX
        fun sy(y: Float) = y * scaleY

        // Fondo verde exterior (#168548)
        val bgPaint = Paint().apply {
            color = AndroidColor.parseColor("#168548")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Área de juego naranja (#f49a40) con borde blanco 0.1 unidades
        val courtFill = Paint().apply {
            color = AndroidColor.parseColor("#f49a40")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val courtStroke = Paint(linePaint).apply { strokeWidth = unit * 0.1f }
        val lineStroke = Paint(linePaint).apply { strokeWidth = unit * 0.1f }
        val dottedStroke = Paint(linePaint).apply { strokeWidth = unit * 0.07f }

        val left = sx(0.25f)
        val top = sy(0.25f)
        val right = sx(0.25f + 8.5f)
        val bottom = sy(0.25f + 17.5f)

        // Relleno
        canvas.drawRect(left, top, right, bottom, courtFill)
        // Borde blanco
        val courtRect = android.graphics.RectF(left, top, right, bottom)
        canvas.drawRect(courtRect, courtStroke)

        // Línea central (y = 9) y líneas de ataque (y = 6, 12) entre x=0.25..8.75
        val xStart = sx(0.25f)
        val xEnd = sx(8.75f)
        canvas.drawLine(xStart, sy(9f), xEnd, sy(9f), lineStroke)
        canvas.drawLine(xStart, sy(6f), xEnd, sy(6f), lineStroke)
        canvas.drawLine(xStart, sy(12f), xEnd, sy(12f), lineStroke)

        // Marcas de zona de saque
        val tickFill = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        // Izquierda (x=0.15, y=1 y y=17), tamaño 0.1 x 0.4
        canvas.drawRect(sx(0.15f), sy(1f), sx(0.15f + 0.1f), sy(1f + 0.4f), tickFill)
        canvas.drawRect(sx(0.15f), sy(17f), sx(0.15f + 0.1f), sy(17f + 0.4f), tickFill)
        // Derecha (x=8.75, y=1 y y=17)
        canvas.drawRect(sx(8.75f), sy(1f), sx(8.75f + 0.1f), sy(1f + 0.4f), tickFill)
        canvas.drawRect(sx(8.75f), sy(17f), sx(8.75f + 0.1f), sy(17f + 0.4f), tickFill)

        // Líneas punteadas externas (lado izquierdo, según SVG provisto)
        canvas.drawLine(sx(0f), sy(4.5f), sx(0.2f), sy(4.5f), dottedStroke)
        canvas.drawLine(sx(0f), sy(5f), sx(0.2f), sy(5f), dottedStroke)
        canvas.drawLine(sx(0f), sy(5.5f), sx(0.2f), sy(5.5f), dottedStroke)
        canvas.drawLine(sx(0f), sy(13.5f), sx(0.2f), sy(13.5f), dottedStroke)
        canvas.drawLine(sx(0f), sy(14f), sx(0.2f), sy(14f), dottedStroke)
        canvas.drawLine(sx(0f), sy(14.5f), sx(0.2f), sy(14.5f), dottedStroke)
    } else if (isBabyFootball) {
        // Cancha de baby fútbol basada en SVG (viewBox 20x40)
        val scaleX = width / 20f
        val scaleY = height / 40f
        val unit = kotlin.math.min(scaleX, scaleY)

        fun sx(x: Float) = x * scaleX
        fun sy(y: Float) = y * scaleY

        // Fondo verde pasto
        val bg = Paint().apply {
            color = AndroidColor.parseColor("#279A49")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

        val lineW = unit * 0.12f
        val goalW = unit * 0.14f
        val lineP = Paint(linePaint).apply { strokeWidth = lineW; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        val goalP = Paint(linePaint).apply { strokeWidth = goalW }

        // Marco interior
        val innerRect = android.graphics.RectF(sx(0.2f), sy(0.2f), sx(19.8f), sy(39.8f))
        canvas.drawRect(innerRect, lineP)

        // Línea central
        canvas.drawLine(sx(0.2f), sy(20f), sx(19.8f), sy(20f), lineP)

        // Círculo central
        canvas.drawCircle(sx(10f), sy(20f), 3f * unit, lineP)

        // Arcos grandes (r=6)
        canvas.drawLine(sx(0.2f), sy(6.2f), sx(19.8f), sy(6.2f), lineP)
        canvas.drawLine(sx(0.2f), sy(33.8f), sx(19.8f), sy(33.8f), lineP)

        // Arcos pequeños (r=4)

        // Porterías
        canvas.drawRect(sx(7.5f), sy(0.2f), sx(7.5f + 5f), sy(0.2f + 1f), goalP)
        canvas.drawRect(sx(7.5f), sy(38.8f), sx(7.5f + 5f), sy(38.8f + 1f), goalP)

        // Puntos de penal
        val spot = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(sx(10f), sy(6.2f), unit * 0.08f, spot)
        canvas.drawCircle(sx(10f), sy(33.8f), unit * 0.08f, spot)
    } else {
        // Fútbol Baby: 800x1400 base (versión con áreas penales agrandadas)
        val scaleX = width / 800f
        val scaleY = height / 1400f
        val circleScale = kotlin.math.min(scaleX, scaleY)

        fun sx(x: Float) = x * scaleX
        fun sy(y: Float) = y * scaleY

        // BORDE EXTERIOR
        val outerRect = android.graphics.RectF(
            sx(20f),
            sy(20f),
            sx(20f + 760f),
            sy(20f + 1360f)
        )
        canvas.drawRoundRect(outerRect, 6f * circleScale, 6f * circleScale, linePaint)

        // MEDIO CAMPO
        canvas.drawLine(sx(20f), sy(700f), sx(780f), sy(700f), linePaint)
        canvas.drawCircle(sx(400f), sy(700f), 90f * circleScale, linePaint)

        // Punto central
        val spotPaint = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(sx(400f), sy(700f), 6f * circleScale, spotPaint)

        // ÁREA PENAL SUPERIOR – AGRANDADA
        // Área grande
        canvas.drawRect(sx(250f), sy(20f), sx(250f + 300f), sy(20f + 180f), linePaint)
        // Área chica
        canvas.drawRect(sx(310f), sy(20f), sx(310f + 180f), sy(20f + 90f), linePaint)
        // Semicírculo penal (tangente al área, más pequeño)
        val rTop = 36f
        val topArcRect = android.graphics.RectF(
            sx(400f - rTop), sy(200f),
            sx(400f + rTop), sy(200f + 2 * rTop)
        )
        canvas.drawArc(topArcRect, 0f, 180f, false, linePaint)
        // Punto penal superior (se mantiene en 170f)
        canvas.drawCircle(sx(400f), sy(170f), 5f * circleScale, spotPaint)

        // ÁREA PENAL INFERIOR – AGRANDADA
        // Área grande
        canvas.drawRect(sx(250f), sy(1200f), sx(250f + 300f), sy(1200f + 180f), linePaint)
        // Área chica
        canvas.drawRect(sx(310f), sy(1270f), sx(310f + 180f), sy(1270f + 90f), linePaint)
        // Semicírculo penal inferior (tangente al área, más pequeño)
        val rBottom = 36f
        val bottomArcRect = android.graphics.RectF(
            sx(400f - rBottom), sy(1200f - 2 * rBottom),
            sx(400f + rBottom), sy(1200f)
        )
        canvas.drawArc(bottomArcRect, 180f, 180f, false, linePaint)
        // Punto penal inferior (se mantiene en 1230f)
        canvas.drawCircle(sx(400f), sy(1230f), 5f * circleScale, spotPaint)
    }

    fun splitTeam(team: List<Player>): Pair<Player?, List<Player>> {
        val gk = team.firstOrNull { it.isGoalkeeper }
        val field = if (gk != null) team.filterNot { it.isGoalkeeper } else team
        return gk to field
    }

    val (gkA, fieldA) = splitTeam(teamA)
    val (gkB, fieldB) = splitTeam(teamB)

    fun positionsHalf(teamSize: Int, hasGoalkeeper: Boolean, isTop: Boolean): List<Pair<Float, Float>> {
        if (teamSize <= 0) return emptyList()

        // Caso especial: equipos de 5 (1 arquero + 4 de campo)
        // Colocamos 2 jugadores en línea defensiva y 2 en línea ofensiva.
        if (hasGoalkeeper && teamSize == 4) {
            val halfHeight = height / 2f
            val innerBand = halfHeight - boxHeight
            val areaLine = if (isTop) boxHeight else height - boxHeight
            val sign = if (isTop) 1f else -1f

            val backY = areaLine + sign * innerBand * 0.15f
            val frontY = areaLine + sign * innerBand * 0.6f

            val xLeft = width * 0.33f
            val xRight = width * 0.67f

            return listOf(
                xLeft to backY,
                xRight to backY,
                xLeft to frontY,
                xRight to frontY
            )
        }

        // Caso especial: 5 jugadores de campo sin arquero -> 3 atrás y 2 adelante
        if (!hasGoalkeeper && teamSize == 5) {
            val halfHeight = height / 2f
            val innerBand = halfHeight - boxHeight
            val areaLine = if (isTop) boxHeight else height - boxHeight
            val sign = if (isTop) 1f else -1f

            val backY = areaLine + sign * innerBand * 0.18f
            val frontY = areaLine + sign * innerBand * 0.65f

            val xBack1 = width * 0.25f
            val xBack2 = width * 0.50f
            val xBack3 = width * 0.75f
            val xFront1 = width * 0.33f
            val xFront2 = width * 0.67f

            return listOf(
                xBack1 to backY,
                xBack2 to backY,
                xBack3 to backY,
                xFront1 to frontY,
                xFront2 to frontY
            )
        }

        // Distribución genérica para otros tamaños: rejilla simple por mitad
        // Siempre entre la línea del área y el medio campo, simétrico para ambos equipos.
        val cols = kotlin.math.min(4, kotlin.math.max(1, teamSize))
        val rows = ((teamSize + cols - 1) / cols)
        val halfHeight = height / 2f
        val innerBand = halfHeight - boxHeight
        val areaLine = if (isTop) boxHeight else height - boxHeight
        val sign = if (isTop) 1f else -1f
        val cellW = width / (cols + 1).toFloat()
        val cellH = innerBand / (rows + 1)

        val result = mutableListOf<Pair<Float, Float>>()
        var idx = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (idx >= teamSize) break
                val x = cellW * (c + 1)
                val y = areaLine + sign * cellH * (r + 1)
                result.add(x to y)
                idx++
            }
        }
        return result
    }

    val posA = positionsHalf(fieldA.size, hasGoalkeeper = gkA != null, isTop = true)
    val posB = positionsHalf(fieldB.size, hasGoalkeeper = gkB != null, isTop = false)

    val teamAPaint = Paint().apply {
        color = teamAColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val teamBPaint = Paint().apply {
        color = teamBColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val radius = kotlin.math.min(width, height) * 0.018f

    fun drawJersey(centerX: Float, centerY: Float, paint: Paint, isGoalkeeper: Boolean = false) {
        val bodyWidth = radius * 2.8f
        val bodyHeight = radius * 2.6f
        val left = centerX - bodyWidth / 2f
        val top = centerY - bodyHeight / 2f
        val bodyRect = android.graphics.RectF(left, top, left + bodyWidth, top + bodyHeight)

        val headRadius = radius * 0.8f
        val headCenterY = top - headRadius * 0.7f
        val headPaint = Paint().apply {
            color = AndroidColor.parseColor("#E0E0E0")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, headCenterY, headRadius, headPaint)

        canvas.drawRoundRect(bodyRect, radius * 0.6f, radius * 0.6f, paint)

        val sleeveWidth = bodyWidth * 0.38f
        val sleeveHeight = bodyHeight * 0.45f
        val sleeveTopY = top + bodyHeight * 0.15f

        val leftSleeveRect = android.graphics.RectF(
            left - sleeveWidth * 0.55f,
            sleeveTopY,
            left - sleeveWidth * 0.55f + sleeveWidth,
            sleeveTopY + sleeveHeight
        )
        val rightSleeveRect = android.graphics.RectF(
            left + bodyWidth - sleeveWidth * 0.45f,
            sleeveTopY,
            left + bodyWidth - sleeveWidth * 0.45f + sleeveWidth,
            sleeveTopY + sleeveHeight
        )
        canvas.drawRoundRect(leftSleeveRect, radius * 0.4f, radius * 0.4f, paint)
        canvas.drawRoundRect(rightSleeveRect, radius * 0.4f, radius * 0.4f, paint)

        val neckWidth = bodyWidth * 0.42f
        val neckHeight = bodyHeight * 0.26f
        val neckLeft = centerX - neckWidth / 2f
        val neckTop = top - neckHeight * 0.4f
        val neckRect = android.graphics.RectF(neckLeft, neckTop, neckLeft + neckWidth, neckTop + neckHeight)
        val neckPaint = Paint().apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(neckRect, radius * 0.35f, radius * 0.35f, neckPaint)

        if (isGoalkeeper) {
            val borderPaint = Paint().apply {
                color = AndroidColor.WHITE
                style = Paint.Style.STROKE
                strokeWidth = linePaint.strokeWidth
                isAntiAlias = true
            }
            canvas.drawRoundRect(bodyRect, radius * 0.6f, radius * 0.6f, borderPaint)
        }
    }

    fieldA.zip(posA).forEach { (player, pos) ->
        drawJersey(pos.first, pos.second, teamAPaint, isGoalkeeper = false)
        canvas.drawText(player.name, pos.first, pos.second + radius * 2.1f, textPaint)
    }

    fieldB.zip(posB).forEach { (player, pos) ->
        drawJersey(pos.first, pos.second, teamBPaint, isGoalkeeper = false)
        canvas.drawText(player.name, pos.first, pos.second + radius * 2.1f, textPaint)
    }

    // Arqueros cerca de cada arco
    gkA?.let {
        val x = width / 2f
        val y = boxHeight * 0.5f
        drawJersey(x, y, teamAPaint, isGoalkeeper = true)
        canvas.drawText(it.name, x, y + radius * 2.1f, textPaint)
    }

    gkB?.let {
        val x = width / 2f
        val y = height - boxHeight * 0.5f
        drawJersey(x, y, teamBPaint, isGoalkeeper = true)
        canvas.drawText(it.name, x, y + radius * 2.1f, textPaint)
    }

    if (isFootball || isTennis || isVolleyball) {
        canvas.restore()
    }

    return bitmap
}

private fun shareTeamsWithImage(
    context: Context,
    titleA: String,
    titleB: String,
    teamA: List<Player>,
    teamB: List<Player>,
    sport: String,
    teamAColor: Int,
    teamBColor: Int
) {
    if (teamA.isEmpty() || teamB.isEmpty()) return
    val text = formatTeamsText(context, titleA, titleB, teamA, teamB)
    val bitmap = createPitchBitmap(teamA, teamB, sport, teamAColor, teamBColor)
    try {
        val dir = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(dir, "pitch_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject_current))
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    } catch (_: IOException) {
        // Si falla, hacer fallback a compartir solo texto
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject_current))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    registerSecretOpener: (() -> Unit) -> Unit = {},
    pendingChatPostId: String? = null,
    pendingChatPeerName: String? = null,
    pendingChatIsGroup: Boolean = false,
    clearPendingChat: () -> Unit = {},
    onShowHistory: () -> Unit = {}
) {
    var showInfo by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentUserName by userNameState
    LaunchedEffect(Unit) { userNameState.value = loadUserName(context) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SportsSoccer, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.title_app_bar), color = Color.White)
                        if (!currentUserName.isNullOrBlank()) {
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.welcome_user, currentUserName ?: ""),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                        val newLocales = if (currentTags.startsWith("en")) {
                            LocaleListCompat.forLanguageTags("es")
                        } else {
                            LocaleListCompat.forLanguageTags("en")
                        }
                        AppCompatDelegate.setApplicationLocales(newLocales)
                    }) {
                        Icon(Icons.Filled.Language, contentDescription = stringResource(R.string.action_language))
                    }
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Filled.History, contentDescription = stringResource(R.string.history))
                    }
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.info_title))
                    }
                }
            )
        }
    ) { innerPadding ->
        PlayersApp(
            modifier = Modifier.padding(innerPadding),
            registerSecretOpener = registerSecretOpener,
            pendingChatPostId = pendingChatPostId,
            pendingChatPeerName = pendingChatPeerName,
            pendingChatIsGroup = pendingChatIsGroup,
            clearPendingChat = clearPendingChat,
            onShowHistory = { showHistory = true }
        )
        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text(stringResource(R.string.info_title), color = Color.White) },
                text = {
                    Text(stringResource(R.string.info_text), textAlign = TextAlign.Start, color = Color.White)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                textContentColor = MaterialTheme.colorScheme.onPrimary,
                confirmButton = { Button(onClick = { showInfo = false }) { Text(stringResource(R.string.info_ok)) } }
            )
        }
        if (showHistory) {
            val context = LocalContext.current
            var matches by remember(showHistory) { mutableStateOf(loadMatches(context)) }
            var selectedMatch by remember(showHistory) { mutableStateOf<SavedMatch?>(null) }
            var pendingDelete by remember(showHistory) { mutableStateOf<SavedMatch?>(null) }
            var confirmClearAll by remember(showHistory) { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showHistory = false },
                title = { Text(stringResource(R.string.history_title), color = Color.White) },
                text = {
                    if (matches.isEmpty()) {
                        Text(stringResource(R.string.active_session), color = Color.White)
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                                items(matches) { m ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMatch = m }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(stringResource(R.string.versus_format, m.titleA, m.titleB), fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.height(6.dp))
                                            Text(stringResource(R.string.score_format, m.teamA.size, m.teamB.size), color = MaterialTheme.colorScheme.primary)
                                            if (m.result.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(stringResource(R.string.result_label) + ": " + m.result, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        IconButton(onClick = { pendingDelete = m }) {
                                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                },
                titleContentColor = Color.White,
                textContentColor = Color.White,
                dismissButton = {
                    if (matches.isNotEmpty()) {
                        TextButton(onClick = { confirmClearAll = true }) { Text(stringResource(R.string.clear_history)) }
                    }
                },
                confirmButton = { Button(onClick = { showHistory = false }) { Text(stringResource(R.string.close)) } }
            )

            if (pendingDelete != null) {
                val m = pendingDelete!!
                AlertDialog(
                    onDismissRequest = { pendingDelete = null },
                    title = { Text(stringResource(R.string.delete_match_title), color = Color.White) },
                    text = { Text(stringResource(R.string.delete_match_confirm, m.titleA, m.titleB)) },
                    dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } },
                    confirmButton = {
                        Button(onClick = {
                            deleteMatch(context, m.id)
                            matches = loadMatches(context)
                            pendingDelete = null
                        }) { Text(stringResource(R.string.delete)) }
                    }
                )
            }

            if (confirmClearAll) {
                AlertDialog(
                    onDismissRequest = { confirmClearAll = false },
                    title = { Text(stringResource(R.string.clear_history_title), color = Color.White) },
                    text = { Text(stringResource(R.string.clear_history_confirm)) },
                    dismissButton = { TextButton(onClick = { confirmClearAll = false }) { Text(stringResource(R.string.cancel)) } },
                    confirmButton = {
                        Button(onClick = {
                            clearAllMatches(context)
                            matches = emptyList()
                            confirmClearAll = false
                        }) { Text(stringResource(R.string.clear_history)) }
                    }
                )
            }

            if (selectedMatch != null) {
                val m = selectedMatch!!
                var resultText by remember(m.id) { mutableStateOf(m.result) }
                var editableMatch by remember(m.id) { mutableStateOf(m) }
                var selectedTeam by remember(m.id) { mutableStateOf<Char?>(null) }
                var selectedPlayerName by remember(m.id) { mutableStateOf<String?>(null) }
                AlertDialog(
                    onDismissRequest = { selectedMatch = null },
                    title = { Text(stringResource(R.string.versus_format, m.titleA, m.titleB), color = Color.White) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Barra de edición global: tarjetas y capitán
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.result_label),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Tarjeta amarilla
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFFEB3B))
                                            .clickable(enabled = selectedPlayerName != null) {
                                                val team = selectedTeam
                                                val name = selectedPlayerName
                                                if (team != null && name != null) {
                                                    if (team == 'A') {
                                                        val updated = editableMatch.teamA.map {
                                                            if (it.name == name) it.copy(hasYellowCard = !it.hasYellowCard) else it
                                                        }
                                                        editableMatch = editableMatch.copy(teamA = updated)
                                                    } else {
                                                        val updated = editableMatch.teamB.map {
                                                            if (it.name == name) it.copy(hasYellowCard = !it.hasYellowCard) else it
                                                        }
                                                        editableMatch = editableMatch.copy(teamB = updated)
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {}

                                    // Tarjeta roja
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Red)
                                            .clickable(enabled = selectedPlayerName != null) {
                                                val team = selectedTeam
                                                val name = selectedPlayerName
                                                if (team != null && name != null) {
                                                    if (team == 'A') {
                                                        val updated = editableMatch.teamA.map {
                                                            if (it.name == name) it.copy(hasRedCard = !it.hasRedCard) else it
                                                        }
                                                        editableMatch = editableMatch.copy(teamA = updated)
                                                    } else {
                                                        val updated = editableMatch.teamB.map {
                                                            if (it.name == name) it.copy(hasRedCard = !it.hasRedCard) else it
                                                        }
                                                        editableMatch = editableMatch.copy(teamB = updated)
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {}

                                    // Capitán (C con fondo cuadrado)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .clickable(enabled = selectedPlayerName != null) {
                                                val team = selectedTeam
                                                val name = selectedPlayerName
                                                if (team != null && name != null) {
                                                    if (team == 'A') {
                                                        val updated = editableMatch.teamA.map {
                                                            if (it.name == name) it.copy(isCaptain = !it.isCaptain) else it
                                                        }
                                                        editableMatch = editableMatch.copy(teamA = updated)
                                                    } else {
                                                        val updated = editableMatch.teamB.map {
                                                            if (it.name == name) it.copy(isCaptain = !it.isCaptain) else it
                                                        }
                                                        editableMatch = editableMatch.copy(teamB = updated)
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("C", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }

                            Text(editableMatch.titleA, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            editableMatch.teamA.forEach { p ->
                                val isSelected = selectedTeam == 'A' && selectedPlayerName == p.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            selectedTeam = 'A'
                                            selectedPlayerName = p.name
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (p.isGoalkeeper) {
                                            Icon(Icons.Filled.BackHand, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(6.dp))
                                        } else {
                                            Icon(painterResource(id = R.drawable.ic_tshirt), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(stringResource(R.string.bullet_player_name, p.name))
                                        if (p.isCaptain) {
                                            Spacer(Modifier.width(4.dp))
                                            Text("C", fontWeight = FontWeight.Bold)
                                        }
                                        if (p.hasYellowCard) {
                                            Spacer(Modifier.width(4.dp))
                                            Text("TA", color = Color(0xFFFFEB3B))
                                        }
                                        if (p.hasRedCard) {
                                            Spacer(Modifier.width(4.dp))
                                            Text("TR", color = Color.Red)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            Text(editableMatch.titleB, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            editableMatch.teamB.forEach { p ->
                                val isSelected = selectedTeam == 'B' && selectedPlayerName == p.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            selectedTeam = 'B'
                                            selectedPlayerName = p.name
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (p.isGoalkeeper) {
                                            Icon(Icons.Filled.BackHand, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(6.dp))
                                        } else {
                                            Icon(painterResource(id = R.drawable.ic_tshirt), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(stringResource(R.string.bullet_player_name, p.name))
                                        if (p.isCaptain) {
                                            Spacer(Modifier.width(4.dp))
                                            Text("C", fontWeight = FontWeight.Bold)
                                        }
                                        if (p.hasYellowCard) {
                                            Spacer(Modifier.width(4.dp))
                                            Text("TA", color = Color(0xFFFFEB3B))
                                        }
                                        if (p.hasRedCard) {
                                            Spacer(Modifier.width(4.dp))
                                            Text("TR", color = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { selectedMatch = null }) { Text(stringResource(R.string.close)) }
                            val context = LocalContext.current
                            Button(onClick = {
                                val text = formatSavedMatchText(context, editableMatch.copy(result = resultText))
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject_match, editableMatch.titleA, editableMatch.titleB))
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                            }) { Text(stringResource(R.string.share)) }
                        }
                    },
                    confirmButton = {
                        val context = LocalContext.current
                        Button(onClick = {
                            val finalMatch = editableMatch.copy(result = resultText.trim())
                            updateMatch(context, finalMatch)
                            matches = loadMatches(context)
                            selectedMatch = null
                        }) { Text(stringResource(R.string.save)) }
                    }
                )
            }
        }
    }
}

data class Player(
    val name: String,
    val attack: Double,
    val defense: Double,
    val physical: Double,
    val isGoalkeeper: Boolean = false,
    val isCaptain: Boolean = false,
    val hasYellowCard: Boolean = false,
    val hasRedCard: Boolean = false
) {
    val rating: Double
        get() = attack * WEIGHT_ATTACK + defense * WEIGHT_DEFENSE + physical * WEIGHT_PHYSICAL
}

// Constants for player ratings

data class SavedMatch(
    val id: Long,
    val time: Long,
    val titleA: String,
    val titleB: String,
    val teamA: List<Player>,
    val teamB: List<Player>,
    val result: String = ""
)

data class SavedTeam(
    val id: Long,
    val name: String,
    val players: List<Player>
)

data class TournamentMatch(
    val id: Int,
    val teamA: SavedTeam?,
    val teamB: SavedTeam?,
    val winnerId: Long? = null
)

private fun isPowerOfTwo(n: Int): Boolean {
    return n > 0 && (n and (n - 1)) == 0
}

private fun buildTournamentBracket(teams: List<SavedTeam>): List<List<TournamentMatch>> {
    if (teams.isEmpty()) return emptyList()
    val shuffled = teams.shuffled()
    var idCounter = 0
    val firstRound = mutableListOf<TournamentMatch>()
    var i = 0
    while (i < shuffled.size) {
        val a = shuffled[i]
        val b = if (i + 1 < shuffled.size) shuffled[i + 1] else null
        firstRound += TournamentMatch(id = idCounter++, teamA = a, teamB = b)
        i += 2
    }
    val rounds = mutableListOf<List<TournamentMatch>>()
    rounds += firstRound
    var currentSize = firstRound.size
    while (currentSize > 1) {
        val nextRound = mutableListOf<TournamentMatch>()
        var j = 0
        while (j < currentSize) {
            nextRound += TournamentMatch(id = idCounter++, teamA = null, teamB = null)
            j += 2
        }
        rounds += nextRound
        currentSize = nextRound.size
    }
    return rounds
}

private val initialPlayers: List<Player> = listOf(
    Player("Rulo", 5.0, 8.0, 7.0),
    Player("Ariel", 7.9, 8.4, 8.0),
    Player("Diego", 7.3, 7.4, 7.3),
    Player("Jaime", 7.2, 7.5, 7.6),
    Player("Pablo V", 8.0, 8.0, 8.0),
    Player("Carlitos", 7.0, 7.5, 7.5),
    Player("Seba", 7.5, 6.8, 7.6),
    Player("Feña", 6.5, 7.0, 6.7),
    Player("Gustavo (P)", 7.3, 7.3, 7.2),
    Player("Tío Seba", 6.2, 7.0, 6.1),
    Player("Manuel", 7.3, 7.6, 7.6),
    Player("Pablo P", 6.8, 6.6, 7.2),
    Player("Kevin", 7.7, 7.1, 7.0),
    Player("David", 7.2, 6.9, 7.2),
    Player("Benja", 7.3, 7.5, 7.5),
    Player("Juan", 7.1, 7.4, 7.2),
    Player("Marín", 7.2, 7.5, 7.7),
    Player("Felipe Ep", 7.2, 7.0, 7.5),
    Player("Chiqui", 8.8, 7.8, 8.2),
    Player("Bubu", 7.6, 7.2, 7.3),
    Player("Vicho", 8.8, 8.4, 8.8),
    Player("Emilio", 8.8, 7.6, 8.5),
    Player("Jesús", 7.3, 7.3, 7.3),
    Player("Shuvert", 7.3, 7.5, 7.7),
    Player("Gastón", 7.8, 7.5, 8.0),
    Player("Richard", 7.5, 7.5, 7.8),
    Player("Víctor", 7.4, 7.4, 7.4),
    Player("Gustavo Riquelme", 7.1, 7.1, 7.1),
    Player("Navaloco", 6.7, 6.4, 6.3)
)

// ===== Persistencia simple en SharedPreferences (JSON) =====
private const val PREFS_NAME = "equipos_prefs"
private const val KEY_PLAYERS = "players_json"
private const val KEY_PREFS_VERSION = "prefs_version"
private const val PREFS_VERSION = 1
private const val KEY_MATCHES = "matches_json"
private const val KEY_SAVED_TEAMS = "saved_teams_json"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER_NAME = "user_name"
private const val KEY_COMMUNITY_POSTS = "community_posts_json"
private const val KEY_COMMUNITY_UNREAD = "community_unread_json"

// Remote API
private const val BASE_URL = "https://server-equiposapp.onrender.com"
private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
private val httpClient: OkHttpClient by lazy {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
    OkHttpClient.Builder().addInterceptor(logging).build()
}

private fun apiPost(path: String, body: JSONObject, token: String? = null): JSONObject? {
    val req = Request.Builder()
        .url("$BASE_URL$path")
        .post(body.toString().toRequestBody(JSON_MEDIA))
        .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
        .build()
    httpClient.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return null
        val text = resp.body?.string() ?: return null
        return JSONObject(text)
    }
}

private fun apiGet(path: String, token: String): JSONObject? {
    val req = Request.Builder().url("$BASE_URL$path").get().header("Authorization", "Bearer $token").build()
    httpClient.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return null
        val text = resp.body?.string() ?: return null
        return JSONObject().put("_", 1).apply { put("body", text) }
    }
}

// Migración simple: limpiar jugadores guardados en la primera ejecución de esta versión
private fun ensurePrefsMigrated(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = prefs.getInt(KEY_PREFS_VERSION, 0)
    if (current < PREFS_VERSION) {
        val editor = prefs.edit()
        editor.remove(KEY_PLAYERS)
        editor.putInt(KEY_PREFS_VERSION, PREFS_VERSION)
        editor.apply()
    }
}

private fun loadPlayers(context: Context): List<Player> {
    ensurePrefsMigrated(context)
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_PLAYERS, null)
    return try {
        if (json.isNullOrBlank()) initialPlayers else jsonToPlayers(json)
    } catch (_: Exception) {
        initialPlayers
    }
}

fun savePlayers(context: Context, players: List<Player>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = playersToJson(players)
    prefs.edit().putString(KEY_PLAYERS, json).apply()
}

fun playersToJson(players: List<Player>): String {
    val arr = JSONArray()
    players.forEach { p ->
        val obj = JSONObject()
        obj.put("name", p.name)
        obj.put("attack", p.attack)
        obj.put("defense", p.defense)
        obj.put("physical", p.physical)
        obj.put("isGoalkeeper", p.isGoalkeeper)
        obj.put("isCaptain", p.isCaptain)
        obj.put("hasYellowCard", p.hasYellowCard)
        obj.put("hasRedCard", p.hasRedCard)
        arr.put(obj)
    }
    return arr.toString()
}

fun jsonToPlayers(json: String): List<Player> {
    val arr = JSONArray(json)
    val list = mutableListOf<Player>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val name = obj.optString("name")
        val hasAttack = obj.has("attack")
        val hasDefense = obj.has("defense")
        val hasPhysical = obj.has("physical") || obj.has("skill")
        val attack = if (hasAttack) obj.optDouble("attack", 5.0) else obj.optDouble("rating", 5.0)
        val defense = if (hasDefense) obj.optDouble("defense", attack) else obj.optDouble("rating", 5.0)
        val physical = when {
            obj.has("physical") -> obj.optDouble("physical", attack)
            obj.has("skill") -> obj.optDouble("skill", attack)
            else -> obj.optDouble("rating", 5.0)
        }
        val isGK = obj.optBoolean("isGoalkeeper", false)
        val isCaptain = obj.optBoolean("isCaptain", false)
        val hasYellow = obj.optBoolean("hasYellowCard", false)
        val hasRed = obj.optBoolean("hasRedCard", false)
        if (name.isNotBlank()) list += Player(name, attack, defense, physical, isGK, isCaptain, hasYellow, hasRed)
    }
    return list
}

fun saveTeams(context: Context, teams: List<SavedTeam>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val arr = JSONArray()
    teams.forEach { t ->
        val obj = JSONObject()
        obj.put("id", t.id)
        obj.put("name", t.name)
        obj.put("players", JSONArray(playersToJson(t.players)))
        arr.put(obj)
    }
    prefs.edit().putString(KEY_SAVED_TEAMS, arr.toString()).apply()
}

fun loadTeams(context: Context): List<SavedTeam> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_SAVED_TEAMS, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<SavedTeam>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optLong("id", System.currentTimeMillis())
            val name = obj.optString("name", "")
            val players = jsonToPlayers(obj.getJSONArray("players").toString())
            if (name.isNotBlank()) {
                list += SavedTeam(id, name, players)
            }
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

fun addTeam(context: Context, name: String, players: List<Player>) {
    val current = loadTeams(context).toMutableList()
    val now = System.currentTimeMillis()
    val team = SavedTeam(id = now, name = name, players = players)
    current.add(0, team)
    saveTeams(context, current)
}

fun updateTeam(context: Context, team: SavedTeam) {
    val updated = loadTeams(context).map { if (it.id == team.id) team else it }
    saveTeams(context, updated)
}

fun deleteTeam(context: Context, id: Long) {
    val updated = loadTeams(context).filterNot { it.id == id }
    saveTeams(context, updated)
}

fun saveMatches(context: Context, matches: List<SavedMatch>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val arr = JSONArray()
    matches.forEach { m ->
        val obj = JSONObject()
        obj.put("id", m.id)
        obj.put("time", m.time)
        obj.put("titleA", m.titleA)
        obj.put("titleB", m.titleB)
        obj.put("teamA", JSONArray(playersToJson(m.teamA)))
        obj.put("teamB", JSONArray(playersToJson(m.teamB)))
        obj.put("result", m.result)
        arr.put(obj)
    }
    prefs.edit().putString(KEY_MATCHES, arr.toString()).apply()
}

fun loadMatches(context: Context): List<SavedMatch> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_MATCHES, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<SavedMatch>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optLong("id", System.currentTimeMillis())
            val time = obj.optLong("time", id)
            val titleA = obj.optString("titleA", "")
            val titleB = obj.optString("titleB", "")
            val teamA = jsonToPlayers(obj.getJSONArray("teamA").toString())
            val teamB = jsonToPlayers(obj.getJSONArray("teamB").toString())
            val result = obj.optString("result", "")
            list += SavedMatch(id, time, titleA, titleB, teamA, teamB, result)
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

fun addMatch(context: Context, titleA: String, titleB: String, teamA: List<Player>, teamB: List<Player>) {
    val current = loadMatches(context).toMutableList()
    val now = System.currentTimeMillis()
    val match = SavedMatch(id = now, time = now, titleA = titleA, titleB = titleB, teamA = teamA, teamB = teamB, result = "")
    current.add(0, match)
    saveMatches(context, current)
}

fun deleteMatch(context: Context, id: Long) {
    val current = loadMatches(context)
    val updated = current.filterNot { it.id == id }
    saveMatches(context, updated)
}

fun clearAllMatches(context: Context) {
    saveMatches(context, emptyList())
}

fun updateMatchResult(context: Context, id: Long, result: String) {
    val updated = loadMatches(context).map { if (it.id == id) it.copy(result = result) else it }
    saveMatches(context, updated)
}

fun updateMatch(context: Context, updated: SavedMatch) {
    val list = loadMatches(context).map { if (it.id == updated.id) updated else it }
    saveMatches(context, list)
}

@Composable
fun EditSavedTeamDialog(
    allPlayers: List<Player>,
    initialName: String,
    initialPlayers: List<Player>,
    onSave: (String, List<Player>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val initialSelected = remember(initialPlayers) { initialPlayers.map { it.name }.toSet() }
    var selectedNames by remember(initialSelected) { mutableStateOf(initialSelected) }
    val canSave = name.trim().isNotEmpty() && selectedNames.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Equipo", color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del equipo", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.7f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text("Jugadores", fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(allPlayers) { p ->
                        val checked = selectedNames.contains(p.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedNames =
                                        if (checked) selectedNames - p.name
                                        else selectedNames + p.name
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedNames =
                                        if (it) selectedNames + p.name
                                        else selectedNames - p.name
                                }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(p.name, color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val trimmed = name.trim()
                    val selectedPlayers = allPlayers.filter { selectedNames.contains(it.name) }
                    onSave(trimmed, selectedPlayers)
                }
            ) {
                Text(stringResource(R.string.save), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
        }
    )
}

@Composable
fun TournamentBracketDialog(
    teams: List<SavedTeam>,
    onDismiss: () -> Unit
) {
    if (teams.isEmpty()) {
        onDismiss()
        return
    }

    var rounds by remember(teams) { mutableStateOf(buildTournamentBracket(teams)) }
    var champion by remember(teams) { mutableStateOf<SavedTeam?>(null) }
    val totalRounds = rounds.size

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101010)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Torneo",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Toca un equipo en cada cruce para hacerlo avanzar a la siguiente ronda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Equipos: ${teams.size} · Rondas: $totalRounds",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rounds.forEachIndexed { roundIndex, matches ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                            val title = when {
                                rounds.size == 1 -> "Final"
                                roundIndex == rounds.lastIndex -> "Final"
                                roundIndex == rounds.lastIndex - 1 -> "Semifinal"
                                roundIndex == 0 -> "Octavos / Cuartos"
                                else -> "Ronda ${roundIndex + 1}"
                            }
                            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)

                            matches.forEachIndexed { matchIndex, match ->
                                TournamentMatchCard(
                                    match = match,
                                    onPickWinner = { winner ->
                                        val updated = rounds.mapIndexed { rIndex, roundMatches ->
                                            if (rIndex == roundIndex) {
                                                roundMatches.mapIndexed { mIndex, m ->
                                                    if (mIndex == matchIndex) m.copy(winnerId = winner.id) else m
                                                }
                                            } else {
                                                roundMatches
                                            }
                                        }.toMutableList()

                                        if (roundIndex + 1 < updated.size) {
                                            val nextMatchIndex = matchIndex / 2
                                            val isA = matchIndex % 2 == 0
                                            val nextRound = updated[roundIndex + 1].toMutableList()
                                            val next = nextRound[nextMatchIndex]
                                            val newNext = if (isA) {
                                                next.copy(teamA = winner)
                                            } else {
                                                next.copy(teamB = winner)
                                            }
                                            nextRound[nextMatchIndex] = newNext
                                            updated[roundIndex + 1] = nextRound
                                        }
                                        rounds = updated

                                        // Si estamos en la última ronda y este partido es la final, definir campeón
                                        if (roundIndex == updated.lastIndex) {
                                            val finalRound = updated.lastOrNull()
                                            val finalMatch = finalRound?.getOrNull(matchIndex)
                                            val winnerTeam = when (finalMatch?.winnerId) {
                                                finalMatch?.teamA?.id -> finalMatch?.teamA
                                                finalMatch?.teamB?.id -> finalMatch?.teamB
                                                else -> null
                                            }
                                            if (winnerTeam != null) {
                                                champion = winnerTeam
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Mostrar copa y confeti cuando ya hay campeón
                champion?.let { champ ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B5E20), RoundedCornerShape(20.dp))
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🎉", fontSize = 24.sp)
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text("🎉", fontSize = 24.sp)
                            }
                            Text(
                                text = "¡Campeón!",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = champ.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentMatchCard(
    match: TournamentMatch,
    onPickWinner: (SavedTeam) -> Unit
) {
    Surface(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        color = Color(0xFF202020)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            @Composable
            fun teamRow(team: SavedTeam?, isTop: Boolean) {
                val selected = team != null && match.winnerId == team.id
                val background = if (selected) Color(0xFF388E3C) else Color(0xFF303030)
                val label = team?.name ?: "Pendiente"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(background, RoundedCornerShape(if (isTop) 8.dp else 0.dp))
                        .clickable(enabled = team != null) {
                            if (team != null) onPickWinner(team)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp
                    )
                }
            }

            teamRow(match.teamA, true)
            Spacer(Modifier.height(4.dp))
            teamRow(match.teamB, false)
        }
    }
}

@Composable
fun SavedTeamsScreen(
    players: List<Player>,
    onBack: () -> Unit,
    onStartTournament: (List<SavedTeam>) -> Unit
) {
    val context = LocalContext.current
    var teams by remember { mutableStateOf(loadTeams(context)) }
    var pendingDeleteTeam by remember { mutableStateOf<SavedTeam?>(null) }
    var tournamentMode by remember { mutableStateOf(false) }
    var selectedTournamentTeams by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showTeamEditorDialog by remember { mutableStateOf(false) }
    var editingTeam by remember { mutableStateOf<SavedTeam?>(null) }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Text(
                text = "Equipos guardados",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(Modifier.height(8.dp))

        if (teams.isEmpty()) {
            Text("No hay equipos guardados", color = Color.White)
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF303030), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (tournamentMode) "Modo torneo: selecciona equipos" else "Modo normal",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = {
                            tournamentMode = !tournamentMode
                            if (!tournamentMode) {
                                selectedTournamentTeams = emptySet()
                            }
                        },
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (tournamentMode) "Salir" else "Torneo")
                    }
                }
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(teams) { t ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            var expanded by remember(t.id) { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.name, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "(" + t.players.size + " jugadores)",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                                if (tournamentMode) {
                                    Checkbox(
                                        checked = selectedTournamentTeams.contains(t.id),
                                        onCheckedChange = { checked ->
                                            selectedTournamentTeams = if (checked) {
                                                selectedTournamentTeams + t.id
                                            } else {
                                                selectedTournamentTeams - t.id
                                            }
                                        }
                                    )
                                }
                                Icon(
                                    imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            if (expanded) {
                                Spacer(Modifier.height(4.dp))
                                t.players.forEach { p ->
                                    Text(
                                        text = "• " + p.name,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        editingTeam = t
                                        showTeamEditorDialog = true
                                    },
                                    border = BorderStroke(2.dp, Color.Black),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.edit),
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                }
                                OutlinedButton(
                                    onClick = { pendingDeleteTeam = t },
                                    border = BorderStroke(2.dp, Color.Black),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.close), color = Color.White)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val selectedTeamsList = teams.filter { selectedTournamentTeams.contains(it.id) }
                val size = selectedTeamsList.size
                val canStartTournament = tournamentMode && size in 2..16 && size % 2 == 0
                if (tournamentMode) {
                    Button(
                        enabled = canStartTournament,
                        onClick = {
                            if (canStartTournament) {
                                onStartTournament(selectedTeamsList)
                            }
                        }
                    ) {
                        Text("Iniciar torneo", color = Color.White)
                    }
                }
                Button(onClick = {
                    editingTeam = null
                    showTeamEditorDialog = true
                }) {
                    Text("Crear", color = Color.White)
                }
            }
        }

        if (pendingDeleteTeam != null) {
            val t = pendingDeleteTeam!!
            AlertDialog(
                onDismissRequest = { pendingDeleteTeam = null },
                title = { Text("Eliminar equipo", color = Color.White) },
                text = { Text("¿Eliminar el equipo \"${t.name}\"?", color = Color.White) },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteTeam = null }) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        deleteTeam(context, t.id)
                        teams = loadTeams(context)
                        pendingDeleteTeam = null
                    }) {
                        Text(stringResource(R.string.delete), color = Color.White)
                    }
                }
            )
        }

        if (showTeamEditorDialog) {
            val initial = editingTeam
            val initialName = initial?.name ?: ""
            val initialPlayers = initial?.players ?: emptyList()
            EditSavedTeamDialog(
                allPlayers = players,
                initialName = initialName,
                initialPlayers = initialPlayers,
                onSave = { name, selectedPlayers ->
                    if (initial == null) {
                        addTeam(context, name, selectedPlayers)
                    } else {
                        updateTeam(context, initial.copy(name = name, players = selectedPlayers))
                    }
                    teams = loadTeams(context)
                    showTeamEditorDialog = false
                },
                onDismiss = { showTeamEditorDialog = false }
            )
        }
    }
}

@Composable
fun PlayersApp(
    modifier: Modifier = Modifier,
    registerSecretOpener: (() -> Unit) -> Unit = {},
    pendingChatPostId: String? = null,
    pendingChatPeerName: String? = null,
    pendingChatIsGroup: Boolean = false,
    clearPendingChat: () -> Unit = {},
    onShowHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    var players by remember {
        mutableStateOf(loadPlayers(context).toMutableList())
    }
    var accessToken by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshToken by rememberSaveable { mutableStateOf<String?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showCommunity by remember { mutableStateOf(false) }
    var deepLinkPostId by rememberSaveable { mutableStateOf<String?>(null) }
    var deepLinkPeerName by rememberSaveable { mutableStateOf<String?>(null) }
    var deepLinkIsGroup by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var count by rememberSaveable { mutableStateOf(10) }
    var teamA by remember { mutableStateOf<List<Player>>(emptyList()) }
    var teamB by remember { mutableStateOf<List<Player>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var showSavedTeamsDialog by remember { mutableStateOf(false) }
    var showTeamEditorDialog by remember { mutableStateOf(false) }
    var editingTeam by remember { mutableStateOf<SavedTeam?>(null) }
    var customTeamATitle by remember { mutableStateOf<String?>(null) }
    var customTeamBTitle by remember { mutableStateOf<String?>(null) }
    var showRenameATeamDialog by remember { mutableStateOf(false) }
    var showRenameBTeamDialog by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(true) }
    var showFullPitchDialog by remember { mutableStateOf(false) }
    var showTournamentDialog by remember { mutableStateOf(false) }
    var tournamentTeams by remember { mutableStateOf<List<SavedTeam>>(emptyList()) }
    // Selector de deporte para creación de partidos
    val matchSports = remember { listOf("Fútbol", "Futbolito", "Baby Fútbol", "Pádel", "Tenis", "Voleybol") }
    var matchSport by remember { mutableStateOf("Futbolito") }
    var matchSportExpanded by remember { mutableStateOf(false) }
    val teamColors = remember { listOf("Blanco", "Negro", "Azul", "Rojo", "Amarillo", "Verde", "Morado") }
    var teamAColorName by remember { mutableStateOf("Amarillo") }
    var teamBColorName by remember { mutableStateOf("Azul") }
    var teamAColorExpanded by remember { mutableStateOf(false) }
    var teamBColorExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val mainListState = rememberLazyListState()
    var showPlayersList by remember { mutableStateOf(true) }

    val selectedPlayers = remember(selected, players) {
        players.filter { selected.contains(it.name) }
    }

    // No más menú secreto; las acciones ahora son visibles en la UI
    registerSecretOpener { /* sin-op */ }

    LaunchedEffect(pendingChatPostId, pendingChatPeerName, pendingChatIsGroup) {
        if (!pendingChatPostId.isNullOrBlank() || !pendingChatPeerName.isNullOrBlank()) {
            deepLinkPostId = pendingChatPostId
            deepLinkPeerName = pendingChatPeerName
            deepLinkIsGroup = pendingChatIsGroup
            showCommunity = true
            clearPendingChat()
        }
    }

    LaunchedEffect(Unit) {
        try {
            val (at, rt) = loadTokens(context)
            accessToken = at
            refreshToken = rt
            if (at != null) {
                val (remote, maybeAt) = withContext(Dispatchers.IO) { fetchPlayersRemote(context, at, rt) }
                if (maybeAt != at) {
                    accessToken = maybeAt
                    saveTokens(context, maybeAt, refreshToken)
                }
                if (remote != null) {
                    players = remote.toMutableList()
                    savePlayers(context, players)
                }
                startGlobalChatListener(context)
            }
        } catch (_: Exception) {
            // En caso de error de red u otro fallo en el arranque, seguimos con datos locales
        }
    }

    if (showCommunity) {
        CommunityScreen(
            onBack = { showCommunity = false },
            initialPostId = deepLinkPostId,
            initialPeerName = deepLinkPeerName,
            initialIsGroup = deepLinkIsGroup
        )
        return
    }

    if (showSavedTeamsDialog) {
        SavedTeamsScreen(
            players = players,
            onBack = { showSavedTeamsDialog = false },
            onStartTournament = { selectedTeams ->
                tournamentTeams = selectedTeams
                showTournamentDialog = true
                showSavedTeamsDialog = false
            }
        )
        return
    }

    fun scrollToResults() {
        scope.launch {
            delay(100)
            val index = players.size
            mainListState.animateScrollToItem(index.coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.select_players_and_count),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { showAuthDialog = true }) {
                    Text(stringResource(if (accessToken != null) R.string.logout else R.string.login))
                }
                if (accessToken != null) {
                    Button(
                        onClick = { showCommunity = true },
                        border = BorderStroke(2.dp, Color.Black),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(stringResource(R.string.open_community))
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.enabled_count, selectedPlayers.size))
        }
        Spacer(Modifier.height(4.dp))
        val allSelected = players.isNotEmpty() && selected.size == players.size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = { checked ->
                        selected = if (checked) players.map { it.name }.toSet() else emptySet()
                    }
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.select_all))
            }
            Button(
                onClick = { showEditDialog = true },
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.edit))
            }
        }
        Spacer(Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.players_count, count))
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = count.toFloat(),
                onValueChange = { count = it.roundToInt().coerceIn(2, 22) },
                valueRange = 2f..22f,
                steps = 19,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp),
                onClick = {
                    error = null
                    val n = count
                    if (n < 2) {
                        error = context.getString(R.string.min_is_2)
                        return@Button
                    }
                    if (n > selectedPlayers.size) {
                        error = context.getString(R.string.not_enough_selected)
                        return@Button
                    }
                    // Asegurarse de que hay suficientes jugadores seleccionados
                    if (selectedPlayers.size < 2) {
                        error = "Selecciona al menos 2 jugadores"
                        return@Button
                    }
                    
                    // Filtrar arqueros y jugadores de campo
                    val gksSel = selectedPlayers.filter { it.isGoalkeeper }
                    val fieldPlayers = selectedPlayers.filterNot { it.isGoalkeeper }
                    
                    // Manejar el caso de no tener suficientes arqueros
                    val mustInclude = when {
                        gksSel.size >= 2 -> gksSel.shuffled().take(2)
                        gksSel.size == 1 -> gksSel + fieldPlayers.shuffled().first()
                        else -> fieldPlayers.shuffled().take(2)
                    }
                    
                    // Tomar el resto de jugadores necesarios
                    val remainingPlayers = (selectedPlayers - mustInclude.toSet()).shuffled()
                    val restCount = (n - mustInclude.size).coerceAtLeast(0)
                    val chosen = mustInclude + remainingPlayers.take(restCount)
                    
                    // Generar equipos balanceados
                    val result = generateBalancedTeams(chosen)
                    teamA = result.first
                    teamB = result.second
                    
                    // Mostrar los equipos generados
                    showResults = true
                    scrollToResults()
                },
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Ícono de balanza para representar generación equilibrada de equipos
                    Icon(Icons.Filled.Scale, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.generate), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp),
                onClick = { showCustomizeDialog = true },
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.create), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    onClick = {
                        teamA = emptyList()
                        teamB = emptyList()
                    },
                    border = BorderStroke(2.dp, Color.Black),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    // Botón de deshacer solo con icono
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.undo), modifier = Modifier.size(28.dp))
                }

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    onClick = { showSavedTeamsDialog = true },
                    border = BorderStroke(2.dp, Color.Black),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    // Botón tipo pizarra para gestionar jugadores / equipos
                    Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showPlayersList = !showPlayersList },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            border = BorderStroke(2.dp, Color.Black),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropUp,
                        contentDescription = null
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.players_tap_to_select),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f), state = mainListState) {
            if (showPlayersList) {
                items(players) { p ->
                    val checked = selected.contains(p.name)
                    PlayerRow(
                        player = p,
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            selected = if (isChecked) selected + p.name else selected - p.name
                        }
                    )
                    HorizontalDivider()
                }
            }

            if (teamA.isNotEmpty() && teamB.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    val titleA = customTeamATitle ?: stringResource(R.string.team_a)
                    val titleB = customTeamBTitle ?: stringResource(R.string.team_b)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showResults = !showResults }) {
                            Text(if (showResults) stringResource(R.string.hide_results) else stringResource(R.string.show_results))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = matchSport,
                                onValueChange = { },
                                label = { Text(stringResource(R.string.sport_label)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { matchSportExpanded = true },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { matchSportExpanded = !matchSportExpanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.primary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = Color.White,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            DropdownMenu(
                                expanded = matchSportExpanded,
                                onDismissRequest = { matchSportExpanded = false },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                matchSports.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s, color = MaterialTheme.colorScheme.onPrimary) },
                                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                        onClick = {
                                            matchSport = s
                                            matchSportExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(0.9f)) {
                            OutlinedTextField(
                                value = teamAColorName,
                                onValueChange = { },
                                label = { Text(stringResource(R.string.team_a_color_label)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { teamAColorExpanded = true },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { teamAColorExpanded = !teamAColorExpanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.primary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = Color.White,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            DropdownMenu(
                                expanded = teamAColorExpanded,
                                onDismissRequest = { teamAColorExpanded = false },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                teamColors.forEach { cName ->
                                    DropdownMenuItem(
                                        text = { Text(cName, color = MaterialTheme.colorScheme.onPrimary) },
                                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                        onClick = {
                                            teamAColorName = cName
                                            teamAColorExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(0.9f)) {
                            OutlinedTextField(
                                value = teamBColorName,
                                onValueChange = { },
                                label = { Text(stringResource(R.string.team_b_color_label)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { teamBColorExpanded = true },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { teamBColorExpanded = !teamBColorExpanded }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.primary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = Color.White,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            DropdownMenu(
                                expanded = teamBColorExpanded,
                                onDismissRequest = { teamBColorExpanded = false },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                teamColors.forEach { cName ->
                                    DropdownMenuItem(
                                        text = { Text(cName, color = MaterialTheme.colorScheme.onPrimary) },
                                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.onPrimary),
                                        onClick = {
                                            teamBColorName = cName
                                            teamBColorExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Actions should be available regardless of results visibility
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        OutlinedButton(onClick = {
                            shareTeamsWithImage(
                                context,
                                titleA,
                                titleB,
                                teamA,
                                teamB,
                                matchSport,
                                teamAndroidColorFromName(teamAColorName),
                                teamAndroidColorFromName(teamBColorName)
                            )
                        }) { Text(stringResource(R.string.share)) }
                        Button(onClick = {
                            scope.launch {
                                if (accessToken.isNullOrBlank()) {
                                    addMatch(context, titleA, titleB, teamA, teamB)
                                } else {
                                    val newAt = withContext(Dispatchers.IO) {
                                        postMatchRemote(context, accessToken, refreshToken, titleA, titleB, teamA, teamB)
                                    }
                                    if (newAt != null && newAt != accessToken) {
                                        accessToken = newAt
                                        saveTokens(context, newAt, refreshToken)
                                    }
                                    addMatch(context, titleA, titleB, teamA, teamB)
                                }
                                onShowHistory()
                            }
                        }) { Text(stringResource(R.string.save_match)) }
                    }
                    if (showResults) {
                        PitchView(
                            teamA = teamA,
                            teamB = teamB,
                            sport = matchSport,
                            teamAColor = teamColorFromName(teamAColorName),
                            teamBColor = teamColorFromName(teamBColorName)
                        )
                        Spacer(Modifier.height(12.dp))
                        TeamsResult(
                            teamA = teamA,
                            teamB = teamB,
                            titleA = titleA,
                            titleB = titleB,
                            onRenameA = { showRenameATeamDialog = true },
                            onRenameB = { showRenameBTeamDialog = true }
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = { showFullPitchDialog = true }) {
                                Text("Ver cancha grande")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

    }

    if (showFullPitchDialog && teamA.isNotEmpty() && teamB.isNotEmpty()) {
        Dialog(onDismissRequest = { showFullPitchDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                PitchView(
                    teamA = teamA,
                    teamB = teamB,
                    sport = matchSport,
                    teamAColor = teamColorFromName(teamAColorName),
                    teamBColor = teamColorFromName(teamBColorName),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showSavedTeamsDialog) {
        val context = LocalContext.current
        var teams by remember(showSavedTeamsDialog) { mutableStateOf(loadTeams(context)) }
        var pendingDeleteTeam by remember(showSavedTeamsDialog) { mutableStateOf<SavedTeam?>(null) }
        var tournamentMode by remember(showSavedTeamsDialog) { mutableStateOf(false) }
        var selectedTournamentTeams by remember(showSavedTeamsDialog) { mutableStateOf<Set<Long>>(emptySet()) }

        Dialog(onDismissRequest = { showSavedTeamsDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF101010)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("Equipos guardados", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))

                    if (teams.isEmpty()) {
                        Text("No hay equipos guardados", color = Color.White)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF303030), RoundedCornerShape(16.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (tournamentMode) "Modo torneo: selecciona equipos" else "Modo normal",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(
                                    onClick = {
                                        tournamentMode = !tournamentMode
                                        if (!tournamentMode) {
                                            selectedTournamentTeams = emptySet()
                                        }
                                    },
                                    border = BorderStroke(1.dp, Color.White),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (tournamentMode) "Salir" else "Torneo")
                                }
                            }
                            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                                items(teams) { t ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        var expanded by remember(t.id) { mutableStateOf(false) }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expanded = !expanded },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(t.name, fontWeight = FontWeight.SemiBold, color = Color.White)
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = "(" + t.players.size + " jugadores)",
                                                    color = Color.White,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            if (tournamentMode) {
                                                Checkbox(
                                                    checked = selectedTournamentTeams.contains(t.id),
                                                    onCheckedChange = { checked ->
                                                        selectedTournamentTeams = if (checked) {
                                                            selectedTournamentTeams + t.id
                                                        } else {
                                                            selectedTournamentTeams - t.id
                                                        }
                                                    }
                                                )
                                            }
                                            Icon(
                                                imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                        if (expanded) {
                                            Spacer(Modifier.height(4.dp))
                                            t.players.forEach { p ->
                                                Text(
                                                    text = "• " + p.name,
                                                    color = Color.White,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    editingTeam = t
                                                    showTeamEditorDialog = true
                                                },
                                                border = BorderStroke(2.dp, Color.Black),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Icon(
                                                    Icons.Filled.Edit,
                                                    contentDescription = stringResource(R.string.edit),
                                                    modifier = Modifier.size(20.dp),
                                                    tint = Color.White
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = { pendingDeleteTeam = t },
                                                border = BorderStroke(2.dp, Color.Black),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = stringResource(R.string.delete),
                                                    modifier = Modifier.size(20.dp),
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showSavedTeamsDialog = false }) {
                            Text(stringResource(R.string.close), color = Color.White)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val selectedTeamsList = teams.filter { selectedTournamentTeams.contains(it.id) }
                            val size = selectedTeamsList.size
                            val canStartTournament = tournamentMode && size in 2..16 && size % 2 == 0
                            if (tournamentMode) {
                                Button(
                                    enabled = canStartTournament,
                                    onClick = {
                                        if (canStartTournament) {
                                            tournamentTeams = selectedTeamsList
                                            showTournamentDialog = true
                                            showSavedTeamsDialog = false
                                        }
                                    }
                                ) {
                                    Text("Iniciar torneo", color = Color.White)
                                }
                            }
                            Button(onClick = {
                                editingTeam = null
                                showTeamEditorDialog = true
                            }) {
                                Text("Crear", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (pendingDeleteTeam != null) {
            val t = pendingDeleteTeam!!
            AlertDialog(
                onDismissRequest = { pendingDeleteTeam = null },
                title = { Text("Eliminar equipo", color = Color.White) },
                text = { Text("¿Eliminar el equipo \"${t.name}\"?", color = Color.White) },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteTeam = null }) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        deleteTeam(context, t.id)
                        teams = loadTeams(context)
                        pendingDeleteTeam = null
                    }) {
                        Text(stringResource(R.string.delete), color = Color.White)
                    }
                }
            )
        }

        if (showTeamEditorDialog) {
            val initial = editingTeam
            val initialName = initial?.name ?: ""
            val initialPlayers = initial?.players ?: emptyList()
            EditSavedTeamDialog(
                allPlayers = players,
                initialName = initialName,
                initialPlayers = initialPlayers,
                onSave = { name, selectedPlayers ->
                    if (initial == null) {
                        addTeam(context, name, selectedPlayers)
                    } else {
                        updateTeam(context, initial.copy(name = name, players = selectedPlayers))
                    }
                    // Refrescar la lista de equipos guardados para que se vea el cambio de inmediato
                    teams = loadTeams(context)
                    showTeamEditorDialog = false
                },
                onDismiss = { showTeamEditorDialog = false }
            )
        }
    }

    if (showCustomizeDialog) {
        CustomizeTeamsDialog(
            players = players,
            initialA = teamA,
            initialB = teamB,
            onApply = { a, b ->
                teamA = a
                teamB = b
                showResults = true
                scrollToResults()
            },
            onDismiss = { showCustomizeDialog = false }
        )
    }

    if (showEditDialog) {
        EditPlayersDialog(
            players = players,
            onUpdatePlayer = { name, a, d, h ->
                players = players.map { if (it.name == name) it.copy(attack = a, defense = d, physical = h) else it }.toMutableList()
            },
            onAddPlayer = { name, a, d, h, isGoalkeeper ->
                if (players.none { it.name.equals(name, ignoreCase = true) }) {
                    players = (players + Player(name.trim(), a, d, h, isGoalkeeper)).toMutableList()
                    selected = selected + name.trim()
                    savePlayers(context, players)
                    if (!accessToken.isNullOrBlank()) {
                        scope.launch {
                            val newAt = withContext(Dispatchers.IO) { postPlayersBulkRemote(context, accessToken, refreshToken, players) }
                            if (newAt != null && newAt != accessToken) {
                                accessToken = newAt
                                saveTokens(context, newAt, refreshToken)
                            }
                        }
                    }
                }
            },
            onSave = {
                if (!accessToken.isNullOrBlank()) {
                    scope.launch {
                        val newAt = withContext(Dispatchers.IO) { postPlayersBulkRemote(context, accessToken, refreshToken, players) }
                        if (newAt != null && newAt != accessToken) {
                            accessToken = newAt
                            saveTokens(context, newAt, refreshToken)
                        }
                        if (newAt == null) savePlayers(context, players)
                    }
                } else {
                    savePlayers(context, players)
                }
            },
            onDismiss = { showEditDialog = false },
            onRenamePlayer = { oldName, newName ->
                val trimmed = newName.trim()
                if (trimmed.isNotEmpty() && (oldName.equals(trimmed, ignoreCase = true) || players.none { it.name.equals(trimmed, ignoreCase = true) })) {
                    players = players.map { if (it.name == oldName) it.copy(name = trimmed) else it }.toMutableList()
                    val updated = players.find { it.name == trimmed }
                    teamA = teamA.map { if (it.name == oldName) updated ?: it.copy(name = trimmed) else it }
                    teamB = teamB.map { if (it.name == oldName) updated ?: it.copy(name = trimmed) else it }
                    if (selected.contains(oldName)) {
                        val newSelected = selected.toMutableSet()
                        newSelected.remove(oldName)
                        newSelected.add(trimmed)
                        selected = newSelected
                    }
                }
            },
            onDeletePlayer = { name ->
                players = players.filter { it.name != name }.toMutableList()
                selected = selected - name
                teamA = teamA.filter { it.name != name }
                teamB = teamB.filter { it.name != name }
                // Guardar cambios localmente y sincronizar eliminación con backend
                savePlayers(context, players)
                if (!accessToken.isNullOrBlank()) {
                    scope.launch {
                        val newAt = withContext(Dispatchers.IO) {
                            postPlayersBulkRemote(context, accessToken, refreshToken, players)
                        }
                        if (newAt != null && newAt != accessToken) {
                            accessToken = newAt
                            saveTokens(context, newAt, refreshToken)
                        }
                    }
                }
            },
            onToggleGoalkeeper = { name, isGK ->
                players = players.map { if (it.name == name) it.copy(isGoalkeeper = isGK) else it }.toMutableList()
            }
        )
    }

    if (showRenameATeamDialog) {
        val current = customTeamATitle ?: stringResource(R.string.team_a)
        var text by remember(current, showRenameATeamDialog) { mutableStateOf(current) }
        AlertDialog(
            onDismissRequest = { showRenameATeamDialog = false },
            title = { Text(stringResource(R.string.edit_team_name), color = Color.White) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(R.string.team_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = Color.White,
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val t = text.trim()
                    customTeamATitle = if (t.isEmpty()) null else t
                    showRenameATeamDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showRenameATeamDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showRenameBTeamDialog) {
        val current = customTeamBTitle ?: stringResource(R.string.team_b)
        var text by remember(current, showRenameBTeamDialog) { mutableStateOf(current) }
        AlertDialog(
            onDismissRequest = { showRenameBTeamDialog = false },
            title = { Text(stringResource(R.string.edit_team_name), color = Color.White) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(R.string.team_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val t = text.trim()
                    customTeamBTitle = if (t.isEmpty()) null else t
                    showRenameBTeamDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showRenameBTeamDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showAuthDialog) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var loginError by remember { mutableStateOf<String?>(null) }
        val currentUser = remember { userNameState.value }
        var emailError by remember { mutableStateOf<String?>(null) }
        var passwordError by remember { mutableStateOf<String?>(null) }
        var loginPasswordVisible by remember { mutableStateOf(false) }
        var isSubmittingLogin by remember { mutableStateOf(false) }
        val loginPrefs = remember { context.getSharedPreferences("login_prefs", 0) }
        var rememberMe by remember { mutableStateOf(loginPrefs.getBoolean("remember", false)) }
        var isRegister by remember { mutableStateOf(false) }
        var registerName by remember { mutableStateOf("") }
        var registerConfirm by remember { mutableStateOf("") }
        var registerError by remember { mutableStateOf<String?>(null) }
        var isSubmittingRegister by remember { mutableStateOf(false) }
        var canResendVerification by remember { mutableStateOf(false) }
        var resendDone by remember { mutableStateOf(false) }
        LaunchedEffect(showAuthDialog) {
            if (showAuthDialog && currentUser == null && rememberMe) {
                email = loginPrefs.getString("email", "") ?: ""
                password = loginPrefs.getString("password", "") ?: ""
            }
        }
        
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = {
                Text(
                    when {
                        currentUser != null -> stringResource(R.string.logout)
                        isRegister -> stringResource(R.string.register_title)
                        else -> stringResource(R.string.login)
                    },
                    color = Color.White
                )
            },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    if (currentUser != null) {
                        // Pantalla de confirmación de cierre de sesión
                        Text(stringResource(R.string.logout_confirm, currentUser ?: ""))
                    } else {
                        // Pantalla de inicio de sesión / registro
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; emailError = null; loginError = null },
                            label = { Text(stringResource(R.string.email)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = Color.White
                            )
                        )
                        emailError?.let { msg ->
                            Spacer(Modifier.height(4.dp))
                            Text(text = msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (isRegister) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = registerName,
                                onValueChange = { registerName = it; registerError = null },
                                label = { Text(stringResource(R.string.name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    cursorColor = Color.White
                                )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; passwordError = null; loginError = null },
                            label = { Text(stringResource(R.string.password_label)) },
                            singleLine = true,
                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                    Icon(if (loginPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = Color.White
                            )
                        )
                        passwordError?.let { msg ->
                            Spacer(Modifier.height(4.dp))
                            Text(text = msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (isRegister) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = registerConfirm,
                                onValueChange = { registerConfirm = it; registerError = null },
                                label = { Text(stringResource(R.string.confirm_password)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    cursorColor = Color.White
                                )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { checked ->
                                    rememberMe = checked
                                    val e = loginPrefs.edit()
                                    e.putBoolean("remember", checked)
                                    if (!checked) {
                                        e.remove("email"); e.remove("password")
                                    }
                                    e.apply()
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Recordar datos")
                        }
                        
                        loginError?.let { error ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (canResendVerification && isValidEmail(email)) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = {
                                    scope.launch {
                                        try {
                                            postJsonWithRetry("/auth/send-verification", JSONObject().put("email", email.trim().lowercase()))
                                            resendDone = true
                                        } catch (_: Exception) { /* ignore */ }
                                    }
                                }) { Text("Reenviar verificación") }
                            }
                            if (resendDone) {
                                Spacer(Modifier.height(4.dp))
                                Text("Correo de verificación reenviado", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (isRegister) {
                            registerError?.let { msg ->
                                Spacer(Modifier.height(8.dp))
                                Text(text = msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                isRegister = !isRegister
                                // limpiar errores al cambiar
                                emailError = null; passwordError = null; loginError = null; registerError = null
                            }) {
                                Text(if (isRegister) stringResource(R.string.have_account_login) else stringResource(R.string.create_account))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (accessToken != null) {
                    Button(
                        onClick = {
                            // logout
                            saveTokens(context, null, null)
                            saveUserName(context, null)
                            userNameState.value = null
                            accessToken = null
                            refreshToken = null
                            // Limpiar completamente jugadores y equipos locales al salir de la sesión
                            players = mutableListOf()
                            selected = emptySet()
                            teamA = emptyList()
                            teamB = emptyList()
                            savePlayers(context, players)
                            stopGlobalChatListener()
                            showAuthDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text(stringResource(R.string.logout)) }
                } else {
                    if (isRegister) {
                        Button(
                            enabled = email.isNotBlank() && isValidEmail(email) && password.length >= 6 && registerConfirm == password && registerName.isNotBlank() && !isSubmittingRegister,
                            onClick = {
                                registerError = null
                                if (!isValidEmail(email)) { registerError = context.getString(R.string.err_email_invalid); return@Button }
                                if (password.length < 6) { registerError = context.getString(R.string.err_password_min, 6); return@Button }
                                if (password != registerConfirm) { registerError = context.getString(R.string.err_passwords_mismatch); return@Button }
                                isSubmittingRegister = true
                                scope.launch {
                                    try {
                                        val body = JSONObject().apply {
                                            put("email", email.trim().lowercase())
                                            put("password", password)
                                            put("name", registerName.trim())
                                        }
                                        val (code, _) = postJsonWithRetry("/auth/register", body)
                                        if (code in 200..299) {
                                            loginError = null
                                            canResendVerification = true
                                            resendDone = false
                                            // Mostrar indicación
                                            registerError = null
                                        } else {
                                            registerError = when (code) {
                                                409 -> context.getString(R.string.err_email_taken)
                                                400 -> context.getString(R.string.err_invalid_data)
                                                in 500..599 -> context.getString(R.string.err_server_try_again)
                                                else -> context.getString(R.string.err_register_generic_with_detail, "")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        registerError = context.getString(R.string.err_connection_with_detail, e.message ?: "")
                                    } finally {
                                        isSubmittingRegister = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Text(stringResource(R.string.register_title)) }
                    } else {
                        Button(
                            enabled = email.isNotBlank() && password.isNotBlank() && !isSubmittingLogin,
                            onClick = {
                                emailError = if (email.isBlank()) context.getString(R.string.err_email_or_name_required) else null
                                passwordError = if (password.isBlank()) context.getString(R.string.err_password_required) else null
                                if (emailError != null || passwordError != null) return@Button
                                
                                isSubmittingLogin = true
                                scope.launch {
                                    try {
                                        val body = JSONObject().apply {
                                            put("email", email.trim().lowercase())
                                            put("password", password)
                                        }
                                        
                                        val (code, text) = postJsonWithRetry("/auth/login", body)
                                        
                                        if (code in 200..299 && !text.isNullOrBlank()) {
                                            val obj = JSONObject(text)
                                            val at = obj.optString("accessToken").takeIf { it.isNotBlank() }
                                            val rt = obj.optString("refreshToken").takeIf { it.isNotBlank() }
                                            val name = obj.optJSONObject("user")?.optString("name")?.takeIf { it.isNotBlank() }

                                            if (at != null && rt != null) {
                                                // Guardar tokens y actualizar estado
                                                accessToken = at
                                                refreshToken = rt
                                                saveTokens(context, at, rt)

                                                // Guardar nombre de usuario si está disponible
                                                if (!name.isNullOrBlank()) {
                                                    saveUserName(context, name)
                                                    userNameState.value = name
                                                }

                                                // Guardar o limpiar credenciales según preferencia
                                                if (rememberMe) {
                                                    loginPrefs.edit()
                                                        .putString("email", email.trim().lowercase())
                                                        .putString("password", password)
                                                        .apply()
                                                } else {
                                                    loginPrefs.edit()
                                                        .remove("email")
                                                        .remove("password")
                                                        .apply()
                                                }

                                                // Sincronizar jugadores desde backend inmediatamente después de login
                                                val (remotePlayers, maybeAt) = withContext(Dispatchers.IO) {
                                                    fetchPlayersRemote(context, at, rt)
                                                }
                                                if (maybeAt != at) {
                                                    accessToken = maybeAt
                                                    saveTokens(context, maybeAt, refreshToken)
                                                }
                                                if (remotePlayers != null) {
                                                    players = remotePlayers.toMutableList()
                                                    savePlayers(context, players)
                                                }

                                                // Iniciar listener global de notificaciones de chat
                                                startGlobalChatListener(context)

                                                withContext(Dispatchers.Main) {
                                                    showAuthDialog = false
                                                }
                                            } else {
                                                loginError = context.getString(R.string.err_server_response)
                                            }
                                        } else {
                                            canResendVerification = (code == 403)
                                            loginError = when (code) {
                                                401 -> context.getString(R.string.err_invalid_credentials)
                                                400 -> context.getString(R.string.err_invalid_data)
                                                403 -> "Tu email no está verificado."
                                                in 500..599 -> context.getString(R.string.err_server_try_again)
                                                else -> context.getString(R.string.err_login_generic)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        loginError = context.getString(R.string.err_connection_with_detail, e.message ?: "")
                                    } finally {
                                        isSubmittingLogin = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Text(stringResource(R.string.login)) }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showRegisterDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var submitError by remember { mutableStateOf<String?>(null) }
        var submitSuccess by remember { mutableStateOf(false) }
        var isSubmitting by remember { mutableStateOf(false) }
        var confirmPassword by remember { mutableStateOf("") }
        var nameError by remember { mutableStateOf<String?>(null) }
        var emailError by remember { mutableStateOf<String?>(null) }
        var passwordError by remember { mutableStateOf<String?>(null) }
        var confirmError by remember { mutableStateOf<String?>(null) }
        var passwordVisible by remember { mutableStateOf(false) }
        var confirmVisible by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text(stringResource(R.string.register_title), color = Color.White) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { val t = it; name = t; nameError = if (t.trim().isBlank()) context.getString(R.string.err_name_required) else null },
                        label = { Text(stringResource(R.string.name)) },
                        singleLine = true,
                        isError = nameError != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = Color.White
                        )
                    )
                    nameError?.let { msg ->
                        Spacer(Modifier.height(4.dp))
                        Text(msg, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { val t = it; email = t; emailError = if (t.trim().isBlank()) context.getString(R.string.err_email_invalid) else if (!isValidEmail(t.trim())) context.getString(R.string.err_email_invalid) else null },
                        label = { Text(stringResource(R.string.email)) },
                        singleLine = true,
                        isError = emailError != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = Color.White
                        )
                    )
                    emailError?.let { msg ->
                        Spacer(Modifier.height(4.dp))
                        Text(msg, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { val t = it; password = t; passwordError = if (t.length < MIN_PASSWORD_LENGTH) context.getString(R.string.err_password_min, MIN_PASSWORD_LENGTH) else null; confirmError = if (confirmPassword != t) context.getString(R.string.err_passwords_mismatch) else null },
                        label = { Text(stringResource(R.string.password_min, MIN_PASSWORD_LENGTH)) },
                        singleLine = true,
                        isError = passwordError != null,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = Color.White
                        )
                    )
                    passwordError?.let { msg ->
                        Spacer(Modifier.height(4.dp))
                        Text(msg, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { val t = it; confirmPassword = t; confirmError = if (t != password) context.getString(R.string.err_passwords_mismatch) else null },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        singleLine = true,
                        isError = confirmError != null,
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(if (confirmVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = Color.White
                        )
                    )
                    confirmError?.let { msg ->
                        Spacer(Modifier.height(4.dp))
                        Text(msg, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    val strength = passwordStrength(password)
                    LinearProgressIndicator(progress = { strength / 4f }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (strength) {
                            0,1 -> stringResource(R.string.password_strength_weak)
                            2 -> stringResource(R.string.password_strength_medium)
                            3 -> stringResource(R.string.password_strength_good)
                            else -> stringResource(R.string.password_strength_strong)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    if (isSubmitting) {
                        Text(stringResource(R.string.registering), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    submitError?.let { msg ->
                        Spacer(Modifier.height(4.dp))
                        Text(msg, color = MaterialTheme.colorScheme.error)
                    }
                    if (submitSuccess) {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.registration_done), color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                val canSubmit = name.trim().isNotEmpty() && isValidEmail(email.trim()) && password.length >= MIN_PASSWORD_LENGTH && confirmPassword == password
                Button(enabled = canSubmit && !isSubmitting, onClick = {
                    nameError = if (name.trim().isBlank()) context.getString(R.string.err_name_required) else null
                    emailError = if (!isValidEmail(email.trim())) context.getString(R.string.err_email_invalid) else null
                    passwordError = if (password.length < MIN_PASSWORD_LENGTH) context.getString(R.string.err_password_min, MIN_PASSWORD_LENGTH) else null
                    confirmError = if (confirmPassword != password) context.getString(R.string.err_passwords_mismatch) else null
                    if (nameError != null || emailError != null || passwordError != null || confirmError != null) return@Button
                    scope.launch {
                        isSubmitting = true
                        submitError = null
                        val body = JSONObject()
                            .put("name", name.trim())
                            .put("email", email.trim())
                            .put("password", password)
                        val (code, _) = postJsonWithRetry("/auth/register", body)
                        if (code in 200..299) {
                            // Registro aceptado. Se envió (o intentó enviar) verificación por correo.
                            submitSuccess = true
                        } else {
                            submitError = when (code) {
                                409 -> context.getString(R.string.err_email_taken)
                                400 -> context.getString(R.string.err_invalid_data)
                                in 500..599 -> context.getString(R.string.err_server_try_again)
                                else -> context.getString(R.string.err_register_generic_with_detail, "")
                            }
                        }
                        isSubmitting = false
                    }
                }) { Text(if (isSubmitting) stringResource(R.string.registering) else stringResource(R.string.register_title)) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showRegisterDialog = false }) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { showRegisterDialog = false; showAuthDialog = true }) { Text(stringResource(R.string.have_account_login)) }
                }
            }
        )
    }

    if (showTournamentDialog && tournamentTeams.isNotEmpty()) {
        TournamentBracketDialog(
            teams = tournamentTeams,
            onDismiss = {
                showTournamentDialog = false
                tournamentTeams = emptyList()
            }
        )
    }

}

@Composable
fun PlayerRow(
    player: Player, 
    checked: Boolean, 
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (player.isGoalkeeper) {
                    Icon(Icons.Filled.BackHand, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                } else {
                    Icon(painterResource(id = R.drawable.ic_tshirt), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                }
                Text(player.name, fontWeight = FontWeight.Medium)
            }
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun PitchView(
    teamA: List<Player>,
    teamB: List<Player>,
    sport: String,
    teamAColor: Color = Color(0xFFFFEB3B),
    teamBColor: Color = Color(0xFF03A9F4),
    modifier: Modifier = Modifier
) {
    if (teamA.isEmpty() && teamB.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.6f)
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
        ) {
            val fieldColor = Color(0xFF43A047)
            val lineColor = Color.White
            val strokeWidth = size.minDimension * 0.01f

            val isTennis = sport.equals("Tenis", ignoreCase = true)
            val isPadel = sport.equals("Pádel", ignoreCase = true) || sport.equals("Padel", ignoreCase = true)
            val isVolleyball = sport.equals("Voleybol", ignoreCase = true) || sport.equals("Vóleibol", ignoreCase = true)
            val isBabyFootball = sport.equals("Baby Fútbol", ignoreCase = true) || sport.equals("Baby Futbol", ignoreCase = true) || sport.equals("Baby futbol", ignoreCase = true) || sport.equals("Baby fútbol", ignoreCase = true)
            // Colores específicos de Pádel según el SVG
            val padelCourtColor = Color(0xFF2EA4DA)   // Zona azul
            val padelWallColor = Color(0xFF082A40)    // Borde azul oscuro de la zona
            val volleyCourtColor = Color(0xFFFF9800)

            // Fondo general
            drawRect(color = fieldColor, size = size)

            // Borde exterior
            drawRect(
                color = lineColor,
                style = Stroke(width = strokeWidth),
                size = Size(width = size.width, height = size.height)
            )

            val boxWidth = size.width * 0.6f
            val boxHeight = size.height * 0.18f
            val boxLeft = (size.width - boxWidth) / 2f

            if (isTennis) {
                // Cancha de tenis basada en el nuevo SVG (800x1400), escalada al tamaño del Canvas
                val scaleX = size.width / 36f
                val scaleY = size.height / 78f

                fun sx(x: Float) = x * scaleX
                fun sy(y: Float) = y * scaleY

                // Fondo verde específico de tenis (#66CC66)
                drawRect(
                    color = Color(0xFF2F9E3A),
                    size = size
                )

                val baseStroke = strokeWidth
                val mainStroke = baseStroke * 0.75f   // líneas internas ~6px relativo
                val outerStroke = baseStroke          // borde exterior ~8px relativo
                val netStroke = mainStroke * 1.2f

                // Líneas verticales (x=120,400,680)
                drawLine(
                    lineColor,
                    Offset(sx(4.5f), sy(0f)),
                    Offset(sx(4.5f), sy(78f)),
                    mainStroke
                )
                drawLine(
                    lineColor,
                    Offset(sx(18f), sy(18f)),
                    Offset(sx(18f), sy(60f)),
                    mainStroke
                )
                drawLine(
                    lineColor,
                    Offset(sx(31.5f), sy(0f)),
                    Offset(sx(31.5f), sy(78f)),
                    mainStroke
                )

                // Líneas horizontales (y=356,696,1037)
                drawLine(
                    lineColor,
                    Offset(sx(4.5f), sy(18f)),
                    Offset(sx(31.5f), sy(18f)),
                    mainStroke
                )
                drawLine(
                    lineColor,
                    Offset(sx(0f), sy(39f)),
                    Offset(sx(36f), sy(39f)),
                    netStroke
                )
                drawLine(
                    lineColor,
                    Offset(sx(4.5f), sy(60f)),
                    Offset(sx(31.5f), sy(60f)),
                    mainStroke
                )
                // Baselines
                drawLine(
                    lineColor,
                    Offset(sx(0f), sy(0f)),
                    Offset(sx(36f), sy(0f)),
                    mainStroke
                )
                drawLine(
                    lineColor,
                    Offset(sx(0f), sy(78f)),
                    Offset(sx(36f), sy(78f)),
                    mainStroke
                )

                // Borde exterior (marco)
                drawRect(
                    color = lineColor,
                    style = Stroke(width = outerStroke),
                    topLeft = Offset(sx(0.075f), sy(0.075f)),
                    size = Size(sx(35.85f), sy(77.85f))
                )

                // Ticks centrales en extremos
                drawLine(
                    color = lineColor,
                    start = Offset(sx(18f), sy(-0.8f)),
                    end = Offset(sx(18f), sy(0.8f)),
                    strokeWidth = mainStroke * 0.8f
                )
                drawLine(
                    color = lineColor,
                    start = Offset(sx(18f), sy(77.2f)),
                    end = Offset(sx(18f), sy(78.8f)),
                    strokeWidth = mainStroke * 0.8f
                )
                // Postes de red (opcionales)
                drawRect(
                    color = lineColor,
                    topLeft = Offset(sx(0.075f), sy(38.6f)),
                    size = Size(sx(0.05f), sy(0.8f))
                )
                drawRect(
                    color = lineColor,
                    topLeft = Offset(sx(35.875f), sy(38.6f)),
                    size = Size(sx(0.05f), sy(0.8f))
                )
            } else if (isPadel) {
                // Cancha de pádel basada en el nuevo SVG (800x1400), escalada al tamaño actual del Canvas
                val scaleX = size.width / 800f
                val scaleY = size.height / 1400f

                fun sx(x: Float) = x * scaleX
                fun sy(y: Float) = y * scaleY

                // Refuerzos de grosor: central más gruesa, resto más delgadas
                val thickStroke = strokeWidth * 1.5f  // línea central ~12px
                val thinStroke = strokeWidth * 0.5f   // líneas laterales/vertical ~4px

                // Zona azul (cancha)
                drawRect(
                    color = padelCourtColor,
                    topLeft = Offset(sx(80f), sy(40f)),
                    size = Size(sx(640f), sy(1320f))
                )

                // Borde de la zona azul (marco)
                drawRect(
                    color = padelWallColor,
                    style = Stroke(width = strokeWidth * 1.5f),
                    topLeft = Offset(sx(80f), sy(40f)),
                    size = Size(sx(640f), sy(1320f))
                )

                // LÍNEA CENTRAL GRUESA
                drawLine(
                    lineColor,
                    Offset(sx(80f), sy(700f)),
                    Offset(sx(720f), sy(700f)),
                    thickStroke
                )

                // LÍNEAS DELGADAS LATERALES SUPERIOR / INFERIOR
                drawLine(
                    lineColor,
                    Offset(sx(80f), sy(360f)),
                    Offset(sx(720f), sy(360f)),
                    thinStroke
                )
                drawLine(
                    lineColor,
                    Offset(sx(80f), sy(1040f)),
                    Offset(sx(720f), sy(1040f)),
                    thinStroke
                )

                // LÍNEA VERTICAL DELGADA (solo entre las horizontales delgadas)
                drawLine(
                    lineColor,
                    Offset(sx(400f), sy(360f)),
                    Offset(sx(400f), sy(1040f)),
                    thinStroke
                )

                // Marcos exteriores azul oscuro arriba/abajo (#073150)
                val barColor = Color(0xFF073150)
                drawRect(
                    color = barColor,
                    topLeft = Offset(sx(70f), sy(30f)),
                    size = Size(sx(660f), sy(20f))
                )
                drawRect(
                    color = barColor,
                    topLeft = Offset(sx(70f), sy(1350f)),
                    size = Size(sx(660f), sy(20f))
                )
            } else if (isVolleyball) {
                // Escala basada en el SVG (viewBox 9x18)
                val scaleX = size.width / 9f
                val scaleY = size.height / 18f
                val unit = minOf(scaleX, scaleY)

                fun sx(x: Float) = x * scaleX
                fun sy(y: Float) = y * scaleY

                // Fondo verde exterior (#168548)
                drawRect(
                    color = Color(0xFF168548),
                    size = size
                )

                // Área de juego naranja con borde blanco (stroke 0.1 unidades)
                val innerTopLeft = Offset(sx(0.25f), sy(0.25f))
                val innerSize = Size(sx(8.5f), sy(17.5f))
                drawRect(
                    color = Color(0xFFF49A40),
                    topLeft = innerTopLeft,
                    size = innerSize
                )
                drawRect(
                    color = lineColor,
                    style = Stroke(width = unit * 0.1f),
                    topLeft = innerTopLeft,
                    size = innerSize
                )

                // Línea central y líneas de ataque
                val xStart = sx(0.25f)
                val xEnd = sx(8.75f)
                val lineW = unit * 0.1f
                drawLine(color = lineColor, start = Offset(xStart, sy(9f)), end = Offset(xEnd, sy(9f)), strokeWidth = lineW)
                drawLine(color = lineColor, start = Offset(xStart, sy(6f)), end = Offset(xEnd, sy(6f)), strokeWidth = lineW)
                drawLine(color = lineColor, start = Offset(xStart, sy(12f)), end = Offset(xEnd, sy(12f)), strokeWidth = lineW)

                // Marcas de zona de saque (rectángulos blancos)
                val tickW = sx(0.1f) - sx(0f)
                val tickH = sy(0.4f) - sy(0f)
                // Izquierda
                drawRect(color = lineColor, topLeft = Offset(sx(0.15f), sy(1f)), size = Size(tickW, tickH))
                drawRect(color = lineColor, topLeft = Offset(sx(0.15f), sy(17f)), size = Size(tickW, tickH))
                // Derecha
                drawRect(color = lineColor, topLeft = Offset(sx(8.75f), sy(1f)), size = Size(tickW, tickH))
                drawRect(color = lineColor, topLeft = Offset(sx(8.75f), sy(17f)), size = Size(tickW, tickH))

                // Líneas punteadas externas (lado izquierdo)
                val dotW = unit * 0.07f
                drawLine(color = lineColor, start = Offset(sx(0f), sy(4.5f)), end = Offset(sx(0.2f), sy(4.5f)), strokeWidth = dotW)
                drawLine(color = lineColor, start = Offset(sx(0f), sy(5f)), end = Offset(sx(0.2f), sy(5f)), strokeWidth = dotW)
                drawLine(color = lineColor, start = Offset(sx(0f), sy(5.5f)), end = Offset(sx(0.2f), sy(5.5f)), strokeWidth = dotW)
                drawLine(color = lineColor, start = Offset(sx(0f), sy(13.5f)), end = Offset(sx(0.2f), sy(13.5f)), strokeWidth = dotW)
                drawLine(color = lineColor, start = Offset(sx(0f), sy(14f)), end = Offset(sx(0.2f), sy(14f)), strokeWidth = dotW)
                drawLine(color = lineColor, start = Offset(sx(0f), sy(14.5f)), end = Offset(sx(0.2f), sy(14.5f)), strokeWidth = dotW)
            } else if (isBabyFootball) {
                // Baby fútbol: SVG (viewBox 20x40)
                val scaleX = size.width / 20f
                val scaleY = size.height / 40f
                val unit = minOf(scaleX, scaleY)

                fun sx(x: Float) = x * scaleX
                fun sy(y: Float) = y * scaleY

                // Fondo
                drawRect(
                    color = Color(0xFF279A49),
                    size = size
                )

                val lineW = unit * 0.12f
                val goalW = unit * 0.14f

                // Marco interior
                drawRect(
                    color = lineColor,
                    style = Stroke(width = lineW),
                    topLeft = Offset(sx(0.2f), sy(0.2f)),
                    size = Size(sx(19.6f), sy(39.6f))
                )

                // Línea central
                drawLine(
                    color = lineColor,
                    start = Offset(sx(0.2f), sy(20f)),
                    end = Offset(sx(19.8f), sy(20f)),
                    strokeWidth = lineW
                )

                // Círculo central
                drawCircle(
                    color = lineColor,
                    radius = 3f * unit,
                    center = Offset(sx(10f), sy(20f)),
                    style = Stroke(width = lineW)
                )

                // Líneas rectas reemplazando arcos
                drawLine(
                    color = lineColor,
                    start = Offset(sx(0.2f), sy(6.2f)),
                    end = Offset(sx(19.8f), sy(6.2f)),
                    strokeWidth = lineW
                )
                drawLine(
                    color = lineColor,
                    start = Offset(sx(0.2f), sy(33.8f)),
                    end = Offset(sx(19.8f), sy(33.8f)),
                    strokeWidth = lineW
                )

                // Porterías
                drawRect(
                    color = lineColor,
                    style = Stroke(width = goalW),
                    topLeft = Offset(sx(7.5f), sy(0.2f)),
                    size = Size(sx(5f), sy(1f))
                )
                drawRect(
                    color = lineColor,
                    style = Stroke(width = goalW),
                    topLeft = Offset(sx(7.5f), sy(38.8f)),
                    size = Size(sx(5f), sy(1f))
                )

                // Puntos de penal
                drawCircle(color = lineColor, radius = unit * 0.08f, center = Offset(sx(10f), sy(6.2f)))
                drawCircle(color = lineColor, radius = unit * 0.08f, center = Offset(sx(10f), sy(33.8f)))
            } else {
                // Fútbol Baby: 800x1400 base
                val scaleX = size.width / 800f
                val scaleY = size.height / 1400f
                val circleScale = minOf(scaleX, scaleY)

                fun sx(x: Float) = x * scaleX
                fun sy(y: Float) = y * scaleY

                //-----------------------------------------------------
                // BORDE EXTERIOR
                //-----------------------------------------------------
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(sx(20f), sy(20f)),
                    size = Size(sx(760f), sy(1360f)),
                    cornerRadius = CornerRadius(6f * circleScale, 6f * circleScale),
                    style = Stroke(width = strokeWidth)
                )

                //-----------------------------------------------------
                // MEDIO CAMPO
                //-----------------------------------------------------
                drawLine(
                    color = lineColor,
                    start = Offset(sx(20f), sy(700f)),
                    end = Offset(sx(780f), sy(700f)),
                    strokeWidth = strokeWidth
                )

                drawCircle(
                    color = lineColor,
                    radius = 90f * circleScale,
                    center = Offset(sx(400f), sy(700f)),
                    style = Stroke(width = strokeWidth)
                )

                drawCircle(
                    color = lineColor,
                    radius = 6f * circleScale,
                    center = Offset(sx(400f), sy(700f))
                )


                //=====================================================
                // ÁREA PENAL SUPERIOR – AGRANDADA PROPORCIONALMENTE
                //=====================================================

                // Área grande (más ancha y profunda)
                drawRect(
                    color = lineColor,
                    topLeft = Offset(sx(250f), sy(20f)),          // antes 320   → ahora más ancha
                    size = Size(sx(300f), sy(180f)),              // antes 160x140 → ahora más grande
                    style = Stroke(width = strokeWidth)
                )

                // Área chica (más grande)
                drawRect(
                    color = lineColor,
                    topLeft = Offset(sx(310f), sy(20f)),
                    size = Size(sx(180f), sy(90f)),               // antes 90x70 → aumentado
                    style = Stroke(width = strokeWidth)
                )

                // Semicírculo penal (tangente al área, más pequeño)
                val rTop = 36f
                val topArcRect = android.graphics.RectF(
                    sx(400f - rTop), sy(200f),
                    sx(400f + rTop), sy(200f + 2 * rTop)
                )
                drawContext.canvas.nativeCanvas.drawArc(
                    topArcRect,
                    0f,
                    180f,
                    false,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        style = android.graphics.Paint.Style.STROKE
                        this.strokeWidth = strokeWidth
                        isAntiAlias = true
                    }
                )

                // Punto penal superior (ajustado)
                drawCircle(
                    color = lineColor,
                    radius = 5f * circleScale,
                    center = Offset(sx(400f), sy(170f))
                )


                //=====================================================
                // ÁREA PENAL INFERIOR – MISMAS PROPORCIONES
                //=====================================================

                // Área grande
                drawRect(
                    color = lineColor,
                    topLeft = Offset(sx(250f), sy(1200f)),
                    size = Size(sx(300f), sy(180f)),
                    style = Stroke(width = strokeWidth)
                )

                // Área chica
                drawRect(
                    color = lineColor,
                    topLeft = Offset(sx(310f), sy(1270f)),
                    size = Size(sx(180f), sy(90f)),
                    style = Stroke(width = strokeWidth)
                )

                // Semicírculo penal inferior (tangente al área, más pequeño)
                val rBottom = 36f
                val bottomArcRect = android.graphics.RectF(
                    sx(400f - rBottom), sy(1200f - 2 * rBottom),
                    sx(400f + rBottom), sy(1200f)
                )
                drawContext.canvas.nativeCanvas.drawArc(
                    bottomArcRect,
                    180f,
                    180f,
                    false,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        style = android.graphics.Paint.Style.STROKE
                        this.strokeWidth = strokeWidth
                        isAntiAlias = true
                    }
                )

                // Punto penal inferior
                drawCircle(
                    color = lineColor,
                    radius = 5f * circleScale,
                    center = Offset(sx(400f), sy(1230f))
                )
            }

            val radiusPlayer = size.minDimension * 0.018f
            val canvas = drawContext.canvas.nativeCanvas
            val textPaint = Paint().apply {
                color = AndroidColor.WHITE
                textAlign = Paint.Align.CENTER
                textSize = size.minDimension * 0.035f
                isAntiAlias = true
            }

            val gkA = teamA.firstOrNull { it.isGoalkeeper }
            val gkB = teamB.firstOrNull { it.isGoalkeeper }
            val fieldA = if (gkA != null) teamA.filterNot { it.isGoalkeeper } else teamA
            val fieldB = if (gkB != null) teamB.filterNot { it.isGoalkeeper } else teamB

            fun drawJersey(center: Offset, baseColor: Color, isGoalkeeper: Boolean = false) {
                val bodyWidth = radiusPlayer * 2.8f
                val bodyHeight = radiusPlayer * 2.6f
                val bodyTopLeft = Offset(center.x - bodyWidth / 2f, center.y - bodyHeight / 2f)

                val headRadius = radiusPlayer * 0.8f
                val headCenter = Offset(center.x, bodyTopLeft.y - headRadius * 0.7f)
                drawCircle(
                    color = Color(0xFFE0E0E0),
                    radius = headRadius,
                    center = headCenter
                )

                drawRoundRect(
                    color = baseColor,
                    topLeft = bodyTopLeft,
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(radiusPlayer * 0.6f, radiusPlayer * 0.6f)
                )

                val sleeveWidth = bodyWidth * 0.38f
                val sleeveHeight = bodyHeight * 0.45f
                val sleeveTopY = bodyTopLeft.y + bodyHeight * 0.15f

                val leftSleeveTopLeft = Offset(bodyTopLeft.x - sleeveWidth * 0.55f, sleeveTopY)
                drawRoundRect(
                    color = baseColor,
                    topLeft = leftSleeveTopLeft,
                    size = Size(sleeveWidth, sleeveHeight),
                    cornerRadius = CornerRadius(radiusPlayer * 0.4f, radiusPlayer * 0.4f)
                )

                val rightSleeveTopLeft = Offset(bodyTopLeft.x + bodyWidth - sleeveWidth * 0.45f, sleeveTopY)
                drawRoundRect(
                    color = baseColor,
                    topLeft = rightSleeveTopLeft,
                    size = Size(sleeveWidth, sleeveHeight),
                    cornerRadius = CornerRadius(radiusPlayer * 0.4f, radiusPlayer * 0.4f)
                )

                val neckWidth = bodyWidth * 0.42f
                val neckHeight = bodyHeight * 0.26f
                val neckTopLeft = Offset(center.x - neckWidth / 2f, bodyTopLeft.y - neckHeight * 0.4f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = neckTopLeft,
                    size = Size(neckWidth, neckHeight),
                    cornerRadius = CornerRadius(radiusPlayer * 0.35f, radiusPlayer * 0.35f)
                )

                if (isGoalkeeper) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = bodyTopLeft,
                        size = Size(bodyWidth, bodyHeight),
                        style = Stroke(width = strokeWidth * 0.9f)
                    )
                }
            }

            fun positionsHalf(teamSize: Int, hasGoalkeeper: Boolean, isTop: Boolean): List<Offset> {
                if (teamSize <= 0) return emptyList()

                // Caso especial: equipos de 5 (1 arquero + 4 de campo)
                // Colocamos 2 jugadores en línea defensiva y 2 en línea ofensiva.
                if (hasGoalkeeper && teamSize == 4) {
                    val halfHeight = size.height / 2f
                    val innerBand = halfHeight - boxHeight
                    val areaLine = if (isTop) boxHeight else size.height - boxHeight
                    val sign = if (isTop) 1f else -1f

                    val backY = areaLine + sign * innerBand * 0.15f
                    val frontY = areaLine + sign * innerBand * 0.6f

                    val xLeft = size.width * 0.33f
                    val xRight = size.width * 0.67f

                    return listOf(
                        Offset(xLeft, backY),
                        Offset(xRight, backY),
                        Offset(xLeft, frontY),
                        Offset(xRight, frontY)
                    )
                }

                // Caso especial: 5 jugadores de campo sin arquero -> 3 atrás y 2 adelante
                if (!hasGoalkeeper && teamSize == 5) {
                    val halfHeight = size.height / 2f
                    val innerBand = halfHeight - boxHeight
                    val areaLine = if (isTop) boxHeight else size.height - boxHeight
                    val sign = if (isTop) 1f else -1f

                    val backY = areaLine + sign * innerBand * 0.18f
                    val frontY = areaLine + sign * innerBand * 0.65f

                    val xBack1 = size.width * 0.25f
                    val xBack2 = size.width * 0.50f
                    val xBack3 = size.width * 0.75f
                    val xFront1 = size.width * 0.33f
                    val xFront2 = size.width * 0.67f

                    return listOf(
                        Offset(xBack1, backY),
                        Offset(xBack2, backY),
                        Offset(xBack3, backY),
                        Offset(xFront1, frontY),
                        Offset(xFront2, frontY)
                    )
                }

                // Distribución genérica para otros tamaños: rejilla simple por mitad
                // Siempre entre la línea del área y el medio campo, simétrico para ambos equipos.
                val cols = minOf(4, maxOf(1, teamSize))
                val rows = ((teamSize + cols - 1) / cols)
                val halfHeight = size.height / 2f
                val innerBand = halfHeight - boxHeight
                val areaLine = if (isTop) boxHeight else size.height - boxHeight
                val sign = if (isTop) 1f else -1f
                val cellW = size.width / (cols + 1)
                val cellH = innerBand / (rows + 1)

                val result = mutableListOf<Offset>()
                var idx = 0
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (idx >= teamSize) break
                        val x = cellW * (c + 1)
                        val y = areaLine + sign * cellH * (r + 1)
                        result.add(Offset(x, y))
                        idx++
                    }
                }
                return result
            }

            val posA = positionsHalf(fieldA.size, hasGoalkeeper = gkA != null, isTop = true)
            val posB = positionsHalf(fieldB.size, hasGoalkeeper = gkB != null, isTop = false)

            fieldA.zip(posA).forEach { (player, pos) ->
                drawJersey(pos, teamAColor, isGoalkeeper = false)
                canvas.drawText(
                    player.name,
                    pos.x,
                    pos.y + radiusPlayer * 2.4f,
                    textPaint
                )
            }

            fieldB.zip(posB).forEach { (player, pos) ->
                drawJersey(pos, teamBColor, isGoalkeeper = false)
                canvas.drawText(
                    player.name,
                    pos.x,
                    pos.y + radiusPlayer * 2.4f,
                    textPaint
                )
            }

            // Arqueros cerca de cada arco
            gkA?.let {
                val x = size.width / 2f
                val y = boxHeight * 0.5f
                drawJersey(Offset(x, y), teamAColor, isGoalkeeper = true)
                canvas.drawText(it.name, x, y + radiusPlayer * 2.4f, textPaint)
            }

            gkB?.let {
                val x = size.width / 2f
                val y = size.height - boxHeight * 0.5f
                drawJersey(Offset(x, y), teamBColor, isGoalkeeper = true)
                canvas.drawText(it.name, x, y + radiusPlayer * 2.4f, textPaint)
            }
        }
    }
}

fun teamColorFromName(name: String): Color {
    return when (name.lowercase()) {
        "blanco", "white" -> Color.White
        "negro", "black" -> Color.Black
        "azul", "blue" -> Color(0xFF2196F3)
        "rojo", "red" -> Color(0xFFF44336)
        "amarillo", "yellow" -> Color(0xFFFFEB3B)
        "verde", "green" -> Color(0xFF4CAF50)
        "morado", "purple" -> Color(0xFF9C27B0)
        else -> Color(0xFFFFEB3B)
    }
}

fun teamAndroidColorFromName(name: String): Int {
    return when (name.lowercase()) {
        "blanco", "white" -> AndroidColor.WHITE
        "negro", "black" -> AndroidColor.BLACK
        "azul", "blue" -> AndroidColor.parseColor("#2196F3")
        "rojo", "red" -> AndroidColor.parseColor("#F44336")
        "amarillo", "yellow" -> AndroidColor.parseColor("#FFEB3B")
        "verde", "green" -> AndroidColor.parseColor("#4CAF50")
        "morado", "purple" -> AndroidColor.parseColor("#9C27B0")
        else -> AndroidColor.parseColor("#FFEB3B")
    }
}

@Composable
fun TeamsResult(
    teamA: List<Player>,
    teamB: List<Player>,
    titleA: String,
    titleB: String,
    onRenameA: () -> Unit,
    onRenameB: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (teamA.isEmpty() && teamB.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.empty_state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val displayA = remember(teamA) { teamA.shuffled() }
    val displayB = remember(teamB) { teamB.shuffled() }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TeamCard(
            title = titleA,
            players = displayA,
            onRename = onRenameA,
            modifier = Modifier.weight(1f)
        )
        TeamCard(
            title = titleB,
            players = displayB,
            onRename = onRenameB,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TeamCard(
    title: String,
    players: List<Player>,
    onRename: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = Color.White)
                IconButton(onClick = onRename) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                }
            }
            Spacer(Modifier.height(8.dp))
            players.forEach { p ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (p.isGoalkeeper) {
                        Icon(Icons.Filled.BackHand, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Icon(painterResource(id = R.drawable.ic_tshirt), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(stringResource(R.string.bullet_player_name, p.name))
                }
            }
        }
    }
}
fun generateBalancedTeams(players: List<Player>): Pair<List<Player>, List<Player>> {
    if (players.isEmpty()) return emptyList<Player>() to emptyList()
    val teamA = mutableListOf<Player>()
    val teamB = mutableListOf<Player>()
    var aA = 0.0; var dA = 0.0; var pA = 0.0; var rA = 0.0
    var aB = 0.0; var dB = 0.0; var pB = 0.0; var rB = 0.0

    fun objectiveAfter(addToA: Boolean, p: Player): Double {
        val naA = if (addToA) aA + p.attack else aA
        val ndA = if (addToA) dA + p.defense else dA
        val npA = if (addToA) pA + p.physical else pA
        val nrA = if (addToA) rA + p.rating else rA
        val naB = if (addToA) aB else aB + p.attack
        val ndB = if (addToA) dB else dB + p.defense
        val npB = if (addToA) pB else pB + p.physical
        val nrB = if (addToA) rB else rB + p.rating
        val da = naA - naB
        val dd = ndA - ndB
        val dp = npA - npB
        val dr = nrA - nrB
        return da * da + dd * dd + dp * dp + dr * dr
    }

    val goalkeepers = players.filter { it.isGoalkeeper }
    val assignedGK = if (goalkeepers.size >= 2) goalkeepers.sortedByDescending { it.rating }.take(2) else emptyList()
    if (assignedGK.size == 2) {
        val gkA = assignedGK[0]
        val gkB = assignedGK[1]
        teamA += gkA
        teamB += gkB
        aA += gkA.attack; dA += gkA.defense; pA += gkA.physical; rA += gkA.rating
        aB += gkB.attack; dB += gkB.defense; pB += gkB.physical; rB += gkB.rating
    }

    val remaining = players.filter { !assignedGK.contains(it) }.sortedByDescending { it.rating }
    for (p in remaining) {
        val toA = objectiveAfter(true, p)
        val toB = objectiveAfter(false, p)
        if (toA < toB) {
            teamA += p
            aA += p.attack; dA += p.defense; pA += p.physical; rA += p.rating
        } else {
            teamB += p
            aB += p.attack; dB += p.defense; pB += p.physical; rB += p.rating
        }
    }
    while (kotlin.math.abs(teamA.size - teamB.size) > 1) {
        if (teamA.size > teamB.size) {
            val moved = teamA.removeAt(teamA.lastIndex)
            teamB += moved
            aA -= moved.attack; dA -= moved.defense; pA -= moved.physical; rA -= moved.rating
            aB += moved.attack; dB += moved.defense; pB += moved.physical; rB += moved.rating
        } else {
            val moved = teamB.removeAt(teamB.lastIndex)
            teamA += moved
            aB -= moved.attack; dB -= moved.defense; pB -= moved.physical; rB -= moved.rating
            aA += moved.attack; dA += moved.defense; pA += moved.physical; rA += moved.rating
        }
    }
    return teamA to teamB
}

@Composable
fun CustomizeTeamsDialog(
    players: List<Player>,
    initialA: List<Player>,
    initialB: List<Player>,
    onApply: (List<Player>, List<Player>) -> Unit,
    onDismiss: () -> Unit
) {
    val namesA = initialA.map { it.name }.toSet()
    val namesB = initialB.map { it.name }.toSet()
    var assignments by remember(players, namesA, namesB) {
        mutableStateOf(
            players.associate { p ->
                val v = when {
                    namesA.contains(p.name) -> "A"
                    namesB.contains(p.name) -> "B"
                    else -> "N"
                }
                p.name to v
            }.toMutableMap()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_teams_title), color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.assign_each_player), fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(players) { p ->
                        val current = assignments[p.name] ?: "N"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                if (p.isGoalkeeper) {
                                    Icon(Icons.Filled.BackHand, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(p.name, color = Color.White)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = current == "A",
                                        onClick = {
                                            val m = assignments.toMutableMap()
                                            m[p.name] = "A"
                                            assignments = m
                                        }
                                    )
                                    Text(stringResource(R.string.assignment_a), color = Color.White)
                                }
                                Spacer(Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = current == "B",
                                        onClick = {
                                            val m = assignments.toMutableMap()
                                            m[p.name] = "B"
                                            assignments = m
                                        }
                                    )
                                    Text(stringResource(R.string.assignment_b), color = Color.White)
                                }
                                Spacer(Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = current == "N",
                                        onClick = {
                                            val m = assignments.toMutableMap()
                                            m[p.name] = "N"
                                            assignments = m
                                        }
                                    )
                                    Text(stringResource(R.string.assignment_none), color = Color.White)
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = players.filter { assignments[it.name] == "A" }
                val b = players.filter { assignments[it.name] == "B" }
                onApply(a, b)
                onDismiss()
            }) { Text(stringResource(R.string.save_changes)) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
fun EditPlayersDialog(
    players: List<Player>,
    onUpdatePlayer: (String, Double, Double, Double) -> Unit,
    onAddPlayer: (String, Double, Double, Double, Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onRenamePlayer: (String, String) -> Unit,
    onDeletePlayer: (String) -> Unit,
    onToggleGoalkeeper: (String, Boolean) -> Unit
) {
    val pendingEdits = remember { mutableStateOf(mutableMapOf<String, Triple<String, String, String>>()) }
    val pendingGK = remember { mutableStateOf(mutableMapOf<String, Boolean>()) }

    var newName by remember { mutableStateOf("") }
    var newAttackText by remember { mutableStateOf("") }
    var newDefenseText by remember { mutableStateOf("") }
    var newSkillText by remember { mutableStateOf("") }
    var newIsGoalkeeper by remember { mutableStateOf(false) }

    var showEditPlayerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Player?>(null) }
    var editName by remember { mutableStateOf("") }

    val configuration = LocalConfiguration.current
    val maxDialogHeight = configuration.screenHeightDp.dp * 0.8f
    val listScrollState = rememberScrollState()

    LaunchedEffect(players.size) {
        // Cuando cambia la cantidad de jugadores (por ejemplo, se agrega uno nuevo),
        // desplazar el scroll al final para que el nuevo jugador sea visible de inmediato.
        // Si la lista es corta, maxValue será 0 y no habrá movimiento perceptible.
        listScrollState.animateScrollTo(listScrollState.maxValue)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_player_title), color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .verticalScroll(listScrollState)
                ) {
                    players.forEach { p ->
                        var attackText by remember(p.name, p.attack) { mutableStateOf(p.attack.toString()) }
                        var defenseText by remember(p.name, p.defense) { mutableStateOf(p.defense.toString()) }
                        var skillText by remember(p.name, p.physical) { mutableStateOf(p.physical.toString()) }
                        val isGK = pendingGK.value[p.name] ?: p.isGoalkeeper

                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.name, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    editTarget = p
                                    editName = p.name
                                    showEditPlayerDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isGK,
                                        onCheckedChange = { checked ->
                                            val map = pendingGK.value.toMutableMap()
                                            map[p.name] = checked
                                            pendingGK.value = map
                                        }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.goalkeeper))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = attackText,
                                    onValueChange = {
                                        val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                                        attackText = filtered
                                        val curr = pendingEdits.value[p.name]?.copy(first = filtered) ?: Triple(filtered, defenseText, skillText)
                                        pendingEdits.value[p.name] = curr
                                    },
                                    label = { Text(stringResource(R.string.attack), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        cursorColor = Color.White
                                    )
                                )
                                OutlinedTextField(
                                    value = defenseText,
                                    onValueChange = {
                                        val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                                        defenseText = filtered
                                        val curr = pendingEdits.value[p.name]?.copy(second = filtered) ?: Triple(attackText, filtered, skillText)
                                        pendingEdits.value[p.name] = curr
                                    },
                                    label = { Text(stringResource(R.string.defense), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        cursorColor = Color.White
                                    )
                                )
                                OutlinedTextField(
                                    value = skillText,
                                    onValueChange = {
                                        val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                                        skillText = filtered
                                        val curr = pendingEdits.value[p.name]?.copy(third = filtered) ?: Triple(attackText, defenseText, filtered)
                                        pendingEdits.value[p.name] = curr
                                    },
                                    label = { Text(stringResource(R.string.physical), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        cursorColor = Color.White
                                    )
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.add_new_player), fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.name)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newAttackText,
                            onValueChange = { newAttackText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(stringResource(R.string.attack), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = newDefenseText,
                            onValueChange = { newDefenseText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(stringResource(R.string.defense), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = Color.White
                            )
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newSkillText,
                            onValueChange = { newSkillText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(stringResource(R.string.physical), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = Color.White
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = newIsGoalkeeper,
                                onCheckedChange = { newIsGoalkeeper = it }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.goalkeeper))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = {
                            val name = newName.trim()
                            val a = newAttackText.toDoubleOrNull()
                            val d = newDefenseText.toDoubleOrNull()
                            val h = newSkillText.toDoubleOrNull()
                            if (name.isNotEmpty() && a != null && d != null && h != null &&
                                a in 1.0..10.0 && d in 1.0..10.0 && h in 1.0..10.0) {
                                onAddPlayer(name, a, d, h, newIsGoalkeeper)
                                newName = ""
                                newAttackText = ""
                                newDefenseText = ""
                                newSkillText = ""
                                newIsGoalkeeper = false
                            }
                        }) { Text(stringResource(R.string.save)) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                pendingEdits.value.forEach { (name, triple) ->
                    val a = triple.first.toDoubleOrNull()
                    val d = triple.second.toDoubleOrNull()
                    val h = triple.third.toDoubleOrNull()
                    if (a != null && d != null && h != null && a in 1.0..10.0 && d in 1.0..10.0 && h in 1.0..10.0) {
                        onUpdatePlayer(name, a, d, h)
                    }
                }
                pendingEdits.value.clear()
                players.forEach { p ->
                    val newVal = pendingGK.value[p.name]
                    if (newVal != null && newVal != p.isGoalkeeper) {
                        onToggleGoalkeeper(p.name, newVal)
                    }
                }
                pendingGK.value.clear()
                onSave()
                onDismiss()
            }) { Text(stringResource(R.string.save_changes)) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )

    if (showEditPlayerDialog && editTarget != null) {
        AlertDialog(
            onDismissRequest = { showEditPlayerDialog = false },
            title = { Text(stringResource(R.string.edit_player_title), color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.name)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = Color.White,
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            },
            confirmButton = {
                Row {
                    Button(onClick = {
                        val old = editTarget!!.name
                        val new = editName.trim()
                        if (new.isNotEmpty()) {
                            onRenamePlayer(old, new)
                            showEditPlayerDialog = false
                        }
                    }) { Text(stringResource(R.string.save)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            showEditPlayerDialog = false
                            showDeleteConfirm = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text(stringResource(R.string.delete)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPlayerDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteConfirm && editTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_player_title), color = Color.White) },
            text = { Text(stringResource(R.string.delete_confirm, editTarget!!.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlayer(editTarget!!.name)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    EquiposTheme {
        AppScaffold()
    }
}

}
