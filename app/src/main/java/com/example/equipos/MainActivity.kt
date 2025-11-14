package com.example.equipos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.equipos.ui.theme.EquiposTheme
import android.util.Patterns
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
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Date

// Constants for player ratings
private const val WEIGHT_ATTACK = 0.35
private const val WEIGHT_DEFENSE = 0.35
private const val WEIGHT_PHYSICAL = 0.30

private const val MIN_PASSWORD_LENGTH = 6

private fun isValidEmail(email: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(email).matches()

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

private fun passwordStrength(password: String): Int {
    var score = 0
    if (password.length >= MIN_PASSWORD_LENGTH) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    return score.coerceIn(0, 4)
}

@Composable
fun ChatDialog(recipient: String, messages: List<ChatMessage>, onSend: (String) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = recipient, color = Color.White) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                LazyColumn(state = listState, modifier = Modifier.heightIn(max = 360.dp)) {
                    items(messages) { m ->
                        val isIncoming = m.from == recipient
                        val bubbleColor = if (isIncoming) Color(0xFF1B5E20) else Color(0xFF43A047)
                        val time = remember(m.time) {
                            try {
                                SimpleDateFormat("HH:mm").format(Date(m.time))
                            } catch (_: Exception) { "" }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = if (isIncoming) Arrangement.Start else Arrangement.End
                        ) {
                            Surface(
                                color = bubbleColor,
                                shape = RoundedCornerShape(16.dp),
                                tonalElevation = 1.dp
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).widthIn(max = 280.dp)) {
                                    Text(m.text, color = Color.White)
                                    if (time.isNotEmpty()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(time, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
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
                            unfocusedLabelColor = Color.White
                        )
                    )
                    Button(
                        onClick = {
                            val t = input.trim()
                            if (t.isNotEmpty()) {
                                onSend(t)
                                input = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(contentColor = Color.White)
                    ) { Text("Enviar") }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        textContentColor = MaterialTheme.colorScheme.onPrimary,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
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
                            userId = o.optString("userId", null),
                            user = o.optString("userName", ""),
                            sport = o.optString("sport", "Futbolito"),
                            available = o.optInt("available", 0),
                            total = o.optInt("total", 0),
                            message = o.optString("message", ""),
                            locality = o.optString("locality", ""),
                            serverId = o.optString("id", null)
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
                serverId = o.optString("serverId", null)
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
fun CommunityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf("Futbolito") }
    var available by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var locality by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    val user = userNameState.value ?: ""
    val snackbarHostState = remember { SnackbarHostState() }
    val sports = remember { listOf("Fútbol", "Futbolito", "Baby Fútbol", "Pádel", "Tenis", "Voleybol") }
    var createSportExpanded by remember { mutableStateOf(false) }
    var editSportExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler { onBack() }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingMessage by remember { mutableStateOf("") }
    var editingSport by remember { mutableStateOf("Futbolito") }
    var editingAvailable by remember { mutableStateOf(0) }
    var editingTotal by remember { mutableStateOf(0) }
    var editingLocality by remember { mutableStateOf("") }
    var chatRecipientName by remember { mutableStateOf<String?>(null) }
    var chatRecipientId by remember { mutableStateOf<String?>(null) }
    val chats = remember { mutableStateMapOf<String, MutableList<ChatMessage>>() }
    var ws by remember { mutableStateOf<WebSocket?>(null) }
    LaunchedEffect(Unit) {
        val (at, rt) = loadTokens(context)
        val remote = withContext(Dispatchers.IO) { fetchCommunityPostsRemote(context, at, rt).first }
        posts = remote ?: loadCommunityPosts(context)
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
                            val partner = if (fromName.equals(user, ignoreCase = true)) toName else fromName
                            scope.launch {
                                val list = chats.getOrPut(partner) { mutableListOf<ChatMessage>() }
                                list.add(ChatMessage(from = fromName, to = toName, text = textMsg, time = t))
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
    // Ensure socket is closed when leaving the screen
    DisposableEffect(Unit) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.community_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
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
                    }) { Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White) }
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
                            OutlinedTextField(
                                value = locality,
                                onValueChange = { locality = it },
                                label = { Text(stringResource(R.string.locality_label)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = available.toString(),
                                onValueChange = { v -> available = v.filter { it.isDigit() }.toIntOrNull() ?: 0 },
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
                                value = total.toString(),
                                onValueChange = { v -> total = v.filter { it.isDigit() }.toIntOrNull() ?: 0 },
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
            item { Text(stringResource(R.string.posts_list_title), style = MaterialTheme.typography.titleMedium, color = Color.White) }
            if (posts.isEmpty()) {
                item { Text(stringResource(R.string.empty_posts), color = Color.White) }
            } else {
                items(posts) { p ->
                    Card(
                        border = BorderStroke(1.dp, Color.Black),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chatRecipientName = p.user; chatRecipientId = p.userId }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(painterResource(id = R.drawable.ic_tshirt), contentDescription = null)
                                    Text(stringResource(R.string.post_format_title, p.user.ifBlank { "" }, p.sport, p.available, p.total), fontWeight = FontWeight.SemiBold)
                                }
                                if (p.user.isNotBlank() && p.user == user) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { chatRecipientName = p.user; chatRecipientId = p.userId }) {
                                            Text(stringResource(R.string.chat))
                                        }
                                        IconButton(onClick = { chatRecipientName = p.user; chatRecipientId = p.userId }) {
                                            Icon(Icons.Filled.Message, contentDescription = stringResource(R.string.chat))
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
                                        IconButton(onClick = {
                                            val sid = p.serverId
                                            if (!sid.isNullOrBlank()) {
                                                scope.launch {
                                                    val (at, rt) = loadTokens(context)
                                                    val token = withContext(Dispatchers.IO) { deleteCommunityPostRemote(context, at, rt, sid) }
                                                    if (token != null) {
                                                        val remote = withContext(Dispatchers.IO) { fetchCommunityPostsRemote(context, token, rt).first }
                                                        if (remote != null) posts = remote
                                                        snackbarHostState.showSnackbar(context.getString(R.string.post_deleted_remote))
                                                    }
                                                }
                                            } else {
                                                val updated = posts.filterNot { it.id == p.id }
                                                saveCommunityPosts(context, updated)
                                                posts = updated
                                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.post_deleted_local)) }
                                            }
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                        }
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { chatRecipientName = p.user; chatRecipientId = p.userId }) {
                                            Text(stringResource(R.string.chat))
                                        }
                                        IconButton(onClick = { chatRecipientName = p.user; chatRecipientId = p.userId }) {
                                            Icon(Icons.Filled.Message, contentDescription = stringResource(R.string.chat))
                                        }
                                    }
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
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                cursorColor = Color.White,
                                                focusedLabelColor = Color.White,
                                                unfocusedLabelColor = Color.White
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
                                    OutlinedTextField(
                                        value = editingLocality,
                                        onValueChange = { editingLocality = it },
                                        label = { Text(stringResource(R.string.locality_label)) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
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
                            } else if (p.message.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(p.message, color = Color.White)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(p.locality, color = Color.White)
                        }
                    }
                }
            }
        }
    }
    val recipient = chatRecipientName
    if (recipient != null) {
        ChatDialog(
            recipient = recipient,
            onSend = { text ->
                val toId = chatRecipientId
                val wsNow = ws
                val payload = JSONObject()
                    .put("type", "message_send")
                    .put("toUserId", toId ?: "")
                    .put("toName", recipient)
                    .put("fromName", user)
                    .put("text", text)
                    .put("time", System.currentTimeMillis())
                var sent = false
                if (wsNow != null && toId != null) {
                    try { sent = wsNow.send(payload.toString()) } catch (_: Exception) { sent = false }
                }
                // Optimistic update
                val list = chats.getOrPut(recipient) { mutableListOf() }
                list.add(ChatMessage(from = user, to = recipient, text = text, time = System.currentTimeMillis()))
                if (!sent) {
                    // Optionally could fallback to REST /messages
                }
            },
            messages = chats[recipient] ?: emptyList(),
            onDismiss = { chatRecipientName = null; chatRecipientId = null }
        )
    }
}

private val userNameState = mutableStateOf<String?>(null)

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
            val at = obj.optString("accessToken", null)
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
    val (code, text) = httpGet("/players", token)
    if (code == 401) {
        val newAt = tryRefresh(context, refresh)
        if (newAt != null) {
            token = newAt
            val (code2, text2) = httpGet("/players", token)
            if (code2 in 200..299 && !text2.isNullOrBlank()) {
                val arr = JSONArray(text2)
                val list = mutableListOf<Player>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list += Player(o.getString("name"), o.getDouble("attack"), o.getDouble("defense"), o.getDouble("physical"), o.optBoolean("isGoalkeeper", false))
                }
                return list to token
            }
        }
        return null to token
    }
    if (code in 200..299 && !text.isNullOrBlank()) {
        val arr = JSONArray(text)
        val list = mutableListOf<Player>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += Player(o.getString("name"), o.getDouble("attack"), o.getDouble("defense"), o.getDouble("physical"), o.optBoolean("isGoalkeeper", false))
        }
        return list to token
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
        .put("teamA", JSONArray(teamA.map { JSONObject().put("name", it.name).put("isGoalkeeper", it.isGoalkeeper) }))
        .put("teamB", JSONArray(teamB.map { JSONObject().put("name", it.name).put("isGoalkeeper", it.isGoalkeeper) }))
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (currentTags.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
        }
        setContent {
            EquiposTheme {
                AppScaffold(registerSecretOpener = { opener -> openSecretMenu = opener })
            }
        }
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
    return "• ${gk}${p.name}"
}

private fun formatTeamBlock(context: Context, title: String, team: List<Player>): String {
    val avg = "%.2f".format(team.avgRating())
    val avgText = context.getString(R.string.average, avg)
    val header = "$title (${team.size}) - $avgText"
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppScaffold(registerSecretOpener: (() -> Unit)->Unit = {}) {
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
                                Text(stringResource(R.string.welcome_user, currentUserName ?: ""), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        PlayersApp(modifier = Modifier.padding(innerPadding), registerSecretOpener = registerSecretOpener)
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
                AlertDialog(
                    onDismissRequest = { selectedMatch = null },
                    title = { Text(stringResource(R.string.versus_format, m.titleA, m.titleB), color = Color.White) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(m.titleA, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            m.teamA.forEach { p ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (p.isGoalkeeper) {
                                        Icon(Icons.Filled.BackHand, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(6.dp))
                                    } else {
                                        Icon(painterResource(id = R.drawable.ic_tshirt), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(stringResource(R.string.bullet_player_name, p.name))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            Text(m.titleB, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            m.teamB.forEach { p ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (p.isGoalkeeper) {
                                        Icon(Icons.Filled.BackHand, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(6.dp))
                                    } else {
                                        Icon(painterResource(id = R.drawable.ic_tshirt), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(stringResource(R.string.bullet_player_name, p.name))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = resultText,
                                onValueChange = { resultText = it },
                                label = { Text(stringResource(R.string.result_label)) },
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
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { selectedMatch = null }) { Text(stringResource(R.string.close)) }
                            Button(onClick = {
                                val text = formatSavedMatchText(context, m.copy(result = resultText))
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject_match, m.titleA, m.titleB))
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                            }) { Text(stringResource(R.string.share)) }
                        }
                    },
                    confirmButton = {
                        val context = LocalContext.current
                        Button(onClick = {
                            updateMatchResult(context, m.id, resultText.trim())
                            // Refresh list and close detail
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
    val isGoalkeeper: Boolean = false
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
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER_NAME = "user_name"
private const val KEY_COMMUNITY_POSTS = "community_posts_json"

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
        if (name.isNotBlank()) list += Player(name, attack, defense, physical, isGK)
    }
    return list
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

@Composable
fun PlayersApp(modifier: Modifier = Modifier, registerSecretOpener: (() -> Unit) -> Unit = {}) {
    val context = LocalContext.current
    var players by remember {
        mutableStateOf(loadPlayers(context).toMutableList())
    }
    var accessToken by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshToken by rememberSaveable { mutableStateOf<String?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showCommunity by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var count by rememberSaveable { mutableStateOf(10) }
    var teamA by remember { mutableStateOf<List<Player>>(emptyList()) }
    var teamB by remember { mutableStateOf<List<Player>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var customTeamATitle by remember { mutableStateOf<String?>(null) }
    var customTeamBTitle by remember { mutableStateOf<String?>(null) }
    var showRenameATeamDialog by remember { mutableStateOf(false) }
    var showRenameBTeamDialog by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val selectedPlayers = remember(selected, players) {
        players.filter { selected.contains(it.name) }
    }

    // No más menú secreto; las acciones ahora son visibles en la UI
    registerSecretOpener { /* sin-op */ }

    LaunchedEffect(Unit) {
        val (at, rt) = loadTokens(context)
        accessToken = at
        refreshToken = rt
        if (at != null) {
            val (remote, maybeAt) = withContext(Dispatchers.IO) { fetchPlayersRemote(context, at, rt) }
            if (maybeAt != at) {
                accessToken = maybeAt
                saveTokens(context, maybeAt, refreshToken)
            }
            if (remote != null) players = remote.toMutableList()
        }
    }

    if (showCommunity) {
        CommunityScreen(
            onBack = { showCommunity = false }
        )
        return
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
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.enabled_count, selectedPlayers.size))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showAuthDialog = true }) {
                    Text(stringResource(if (accessToken != null) R.string.logout else R.string.login))
                }
                if (accessToken != null) {
                    OutlinedButton(onClick = { showCommunity = true }, border = BorderStroke(1.dp, Color.Black)) {
                        Text(stringResource(R.string.open_community))
                    }
                }
                Button(onClick = { showEditDialog = true }) { Text(stringResource(R.string.edit)) }
            }
        }
        Spacer(Modifier.height(4.dp))
        val allSelected = players.isNotEmpty() && selected.size == players.size
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
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 80.dp),
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
                    Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.generate), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 80.dp),
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
            
            OutlinedButton(
                modifier = Modifier
                    .weight(0.8f)
                    .heightIn(min = 80.dp),
                onClick = {
                    teamA = emptyList()
                    teamB = emptyList()
                },
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) { 
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.undo), maxLines = 1, overflow = TextOverflow.Ellipsis)
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

        Text(
            stringResource(R.string.players_tap_to_select),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
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

        Spacer(Modifier.height(12.dp))
        val titleA = customTeamATitle ?: stringResource(R.string.team_a)
        val titleB = customTeamBTitle ?: stringResource(R.string.team_b)
        if (teamA.isNotEmpty() && teamB.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showResults = !showResults }) {
                    Text(if (showResults) stringResource(R.string.hide_results) else stringResource(R.string.show_results))
                }
            }
            Spacer(Modifier.height(8.dp))
            // Actions should be available regardless of results visibility
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                OutlinedButton(onClick = {
                    val text = formatTeamsText(context, titleA, titleB, teamA, teamB)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject_current))
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                }) { Text(stringResource(R.string.share)) }
                Button(onClick = {
                    if (accessToken.isNullOrBlank()) {
                        addMatch(context, titleA, titleB, teamA, teamB)
                    } else {
                        val newAt = postMatchRemote(context, accessToken, refreshToken, titleA, titleB, teamA, teamB)
                        if (newAt != null) {
                            if (newAt != accessToken) {
                                accessToken = newAt
                                saveTokens(context, newAt, refreshToken)
                            }
                        } else {
                            addMatch(context, titleA, titleB, teamA, teamB)
                        }
                    }
                }) { Text(stringResource(R.string.save_match)) }
            }
            if (showResults) {
                TeamsResult(
                    teamA = teamA,
                    teamB = teamB,
                    titleA = titleA,
                    titleB = titleB,
                    onRenameA = { showRenameATeamDialog = true },
                    onRenameB = { showRenameBTeamDialog = true }
                )
                Spacer(Modifier.height(8.dp))
            }
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
                        selected = selected - oldName + trimmed
                    }
                }
            },
            onDeletePlayer = { name ->
                players = players.filter { it.name != name }.toMutableList()
                selected = selected - name
                teamA = teamA.filter { it.name != name }
                teamB = teamB.filter { it.name != name }
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
        
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { 
                Text(if (currentUser != null) stringResource(R.string.logout) else stringResource(R.string.login), color = Color.White)
            },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    if (currentUser != null) {
                        // Pantalla de confirmación de cierre de sesión
                        Text(stringResource(R.string.logout_confirm, currentUser ?: ""))
                    } else {
                        // Pantalla de inicio de sesión
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; emailError = null; loginError = null },
                            label = { Text(stringResource(R.string.email_or_name)) },
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
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; passwordError = null; loginError = null },
                            label = { Text(stringResource(R.string.password_label)) },
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
                        passwordError?.let { msg ->
                            Spacer(Modifier.height(4.dp))
                            Text(text = msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        loginError?.let { error ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
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
                            showAuthDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text(stringResource(R.string.logout)) }
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
                                        put("password", password)
                                        if (isValidEmail(email)) put("email", email.trim()) else put("name", email.trim())
                                    }
                                    
                                    val (code, text) = postJsonWithRetry("/auth/login", body)
                                    
                                    if (code in 200..299 && !text.isNullOrBlank()) {
                                        val obj = JSONObject(text)
                                        val at = obj.optString("accessToken", null)
                                        val rt = obj.optString("refreshToken", null)
                                        val name = obj.optJSONObject("user")?.optString("name", null)
                                        
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
                                            
                                            // Sincronizar datos del usuario
                                            withContext(Dispatchers.IO) { 
                                                val (remote, maybeAt) = fetchPlayersRemote(context, at, rt) 
                                                if (maybeAt != at) {
                                                    accessToken = maybeAt
                                                    saveTokens(context, maybeAt, rt)
                                                }
                                                if (remote != null) {
                                                    players = remote.toMutableList()
                                                }
                                            }
                                            
                                            withContext(Dispatchers.Main) {
                                                showAuthDialog = false
                                            }
                                        } else {
                                            loginError = context.getString(R.string.err_server_response)
                                        }
                                    } else {
                                        loginError = when (code) {
                                            401 -> context.getString(R.string.err_invalid_credentials)
                                            400 -> context.getString(R.string.err_invalid_data)
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
                    ) { Text(if (isSubmittingLogin) stringResource(R.string.logging_in) else stringResource(R.string.login)) }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showAuthDialog = false }) { Text(stringResource(R.string.cancel)) }
                    if (currentUser == null) {
                        TextButton(onClick = { showAuthDialog = false; showRegisterDialog = true }) { Text(stringResource(R.string.create_account)) }
                    }
                }
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
                        val (code, text) = postJsonWithRetry("/auth/register", body)
                        val trimmed = text?.trimStart()
                        val looksHtml = trimmed?.startsWith("<!DOCTYPE", ignoreCase = true) == true || trimmed?.startsWith("<html", ignoreCase = true) == true
                        if (code in 200..299 && !text.isNullOrBlank() && !looksHtml) {
                            val obj = JSONObject(text)
                            val at = obj.optString("accessToken", null)
                            val rt = obj.optString("refreshToken", null)
                            val name = obj.optJSONObject("user")?.optString("name", null)
                            if (at != null && rt != null) {
                                accessToken = at
                                refreshToken = rt
                                saveTokens(context, at, rt)
                                if (!name.isNullOrBlank()) {
                                    saveUserName(context, name)
                                    userNameState.value = name
                                }
                                val (remote, maybeAt) = withContext(Dispatchers.IO) { fetchPlayersRemote(context, at, rt) }
                                if (maybeAt != at) {
                                    accessToken = maybeAt
                                    saveTokens(context, maybeAt, rt)
                                }
                                if (remote != null) players = remote.toMutableList()
                                submitSuccess = true
                                isSubmitting = false
                                // cerrar automáticamente tras una breve confirmación
                                delay(1200)
                                showRegisterDialog = false
                            } else {
                                isSubmitting = false
                                submitError = context.getString(R.string.err_server_response)
                            }
                        } else {
                            isSubmitting = false
                            submitError = when {
                                looksHtml || code in 500..599 -> context.getString(R.string.err_server_unavailable)
                                code == 409 -> context.getString(R.string.err_email_taken)
                                code == 400 -> context.getString(R.string.err_invalid_data)
                                else -> context.getString(R.string.err_register_generic_with_detail, if (!text.isNullOrBlank()) ": $text" else "")
                            }
                        }
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
fun TeamsResult(
    teamA: List<Player>,
    teamB: List<Player>,
    titleA: String,
    titleB: String,
    onRenameA: () -> Unit,
    onRenameB: () -> Unit,
    modifier: Modifier = Modifier
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
    
    // Mostrar en orden aleatorio solo para la vista, estable por cambio de equipos
    val displayA = remember(teamA) { teamA.shuffled() }
    val displayB = remember(teamB) { teamB.shuffled() }
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TeamCard(title = titleA, players = displayA, onRename = onRenameA, modifier = Modifier.weight(1f))
        TeamCard(title = titleB, players = displayB, onRename = onRenameB, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TeamCard(title: String, players: List<Player>, onRename: () -> Unit, modifier: Modifier = Modifier) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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

// Generate balanced teams function
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
        return da*da + dd*dd + dp*dp + dr*dr
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

// End of file

@Composable
private fun CustomizeTeamsDialog(
    players: List<Player>,
    initialA: List<Player>,
    initialB: List<Player>,
    onApply: (List<Player>, List<Player>) -> Unit,
    onDismiss: () -> Unit
) {
    val namesA = remember(initialA) { initialA.map { it.name }.toSet() }
    val namesB = remember(initialB) { initialB.map { it.name }.toSet() }
    val assignments = remember(players, namesA, namesB) {
        mutableStateOf(players.associate { p ->
            val v = when {
                namesA.contains(p.name) -> "A"
                namesB.contains(p.name) -> "B"
                else -> "N"
            }
            p.name to v
        }.toMutableMap())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_teams_title), color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.assign_each_player), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(players) { p ->
                        val current = assignments.value[p.name] ?: "N"
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
                                Text(p.name)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = current == "A",
                                        onClick = {
                                            val m = assignments.value.toMutableMap()
                                            m[p.name] = "A"
                                            assignments.value = m
                                        }
                                    )
                                    Text(stringResource(R.string.assignment_a))
                                }
                                Spacer(Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = current == "B",
                                        onClick = {
                                            val m = assignments.value.toMutableMap()
                                            m[p.name] = "B"
                                            assignments.value = m
                                        }
                                    )
                                    Text(stringResource(R.string.assignment_b))
                                }
                                Spacer(Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = current == "N",
                                        onClick = {
                                            val m = assignments.value.toMutableMap()
                                            m[p.name] = "N"
                                            assignments.value = m
                                        }
                                    )
                                    Text(stringResource(R.string.assignment_none))
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = players.filter { assignments.value[it.name] == "A" }
                val b = players.filter { assignments.value[it.name] == "B" }
                onApply(a, b)
                onDismiss()
            }) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditPlayersDialog(
    players: List<Player>,
    onUpdatePlayer: (name: String, attack: Double, defense: Double, skill: Double) -> Unit,
    onAddPlayer: (name: String, attack: Double, defense: Double, skill: Double, isGoalkeeper: Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onRenamePlayer: (oldName: String, newName: String) -> Unit,
    onDeletePlayer: (name: String) -> Unit,
    onToggleGoalkeeper: (name: String, isGoalkeeper: Boolean) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newAttackText by remember { mutableStateOf("") }
    var newDefenseText by remember { mutableStateOf("") }
    var newSkillText by remember { mutableStateOf("") }
    val pendingEdits = remember { mutableStateOf(mutableMapOf<String, Triple<String, String, String>>()) }
    var editTarget by remember { mutableStateOf<Player?>(null) }
    var editName by remember { mutableStateOf("") }
    var showEditPlayerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var newIsGoalkeeper by remember { mutableStateOf(false) }
    val pendingGK = remember { mutableStateOf(mutableMapOf<String, Boolean>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_players_title), color = Color.White) },
        text = {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.edit_existing_ratings), fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(players) { p ->
                        var attackText by remember(p.name) { mutableStateOf("${p.attack}") }
                        var defenseText by remember(p.name) { mutableStateOf("${p.defense}") }
                        var skillText by remember(p.name) { mutableStateOf("${p.physical}") }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        editTarget = p
                                        editName = p.name
                                        showEditPlayerDialog = true
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (p.isGoalkeeper) {
                                    Icon(Icons.Filled.BackHand, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                } else {
                                    Icon(painterResource(id = R.drawable.ic_tshirt), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(p.name, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Divider()
                    }
                }

                Spacer(Modifier.height(12.dp))
                Divider()
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
                // Apply pending goalkeeper toggles only on save
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

    // Diálogo para renombrar/eliminar jugador
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
