package com.example.equipos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
 
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
 
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.equipos.ui.theme.EquiposTheme
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    // Registro para abrir el menú secreto desde eventos de hardware
    private var openSecretMenu: (() -> Unit)? = null
    private var lastVolUpTime: Long = -1L
    private var lastVolDownTime: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
fun AppScaffold(registerSecretOpener: (()->Unit)->Unit = {}) {
    var showInfo by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
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
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                        val currentIsEnglish = currentTags.startsWith("en")
                        val newLocales = if (currentIsEnglish) {
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
                title = { Text(stringResource(R.string.history_title)) },
                text = {
                    if (matches.isEmpty()) {
                        Text(stringResource(R.string.no_history))
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
                                            Text("${m.titleA} vs ${m.titleB}", fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.height(6.dp))
                                            Text("${m.teamA.size} - ${m.teamB.size}", color = MaterialTheme.colorScheme.primary)
                                            if (m.result.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(stringResource(R.string.result_label) + ": " + m.result, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        IconButton(onClick = { pendingDelete = m }) {
                                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                        }
                                    }
                                    Divider()
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
                    title = { Text(stringResource(R.string.delete_match_title)) },
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
                    title = { Text(stringResource(R.string.clear_history_title)) },
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
                    title = { Text("${m.titleA} vs ${m.titleB}") },
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
                                    Text("• ${p.name}")
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Divider()
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
                                    Text("• ${p.name}")
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
    val skill: Double,
    val isGoalkeeper: Boolean = false
) {
    val rating: Double get() = (attack + defense + skill) / 3.0
}

data class SavedMatch(
    val id: Long,
    val time: Long,
    val titleA: String,
    val titleB: String,
    val teamA: List<Player>,
    val teamB: List<Player>,
    val result: String = ""
)

// Comenzar con jugadores iniciales
private val initialPlayers: List<Player> = listOf(
    Player("Rulo", 5.0, 8.0, 8.0),
    Player("Ariel", 7.9, 8.4, 8.4),
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

// Migración simple: limpiar jugadores guardados en la primera ejecución de esta versión
private fun ensurePrefsMigrated(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = prefs.getInt(KEY_PREFS_VERSION, 0)
    if (current < PREFS_VERSION) {
        prefs.edit()
            .remove(KEY_PLAYERS)
            .putInt(KEY_PREFS_VERSION, PREFS_VERSION)
            .apply()
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

private fun savePlayers(context: Context, players: List<Player>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = playersToJson(players)
    prefs.edit().putString(KEY_PLAYERS, json).apply()
}

private fun playersToJson(players: List<Player>): String {
    val arr = JSONArray()
    players.forEach { p ->
        val obj = JSONObject()
        obj.put("name", p.name)
        obj.put("attack", p.attack)
        obj.put("defense", p.defense)
        obj.put("skill", p.skill)
        obj.put("isGoalkeeper", p.isGoalkeeper)
        arr.put(obj)
    }
    return arr.toString()
}

private fun jsonToPlayers(json: String): List<Player> {
    val arr = JSONArray(json)
    val list = mutableListOf<Player>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val name = obj.optString("name")
        val hasAttack = obj.has("attack")
        val hasDefense = obj.has("defense")
        val hasSkill = obj.has("skill")
        val attack = if (hasAttack) obj.optDouble("attack", 5.0) else obj.optDouble("rating", 5.0)
        val defense = if (hasDefense) obj.optDouble("defense", attack) else obj.optDouble("rating", 5.0)
        val skill = if (hasSkill) obj.optDouble("skill", attack) else obj.optDouble("rating", 5.0)
        val isGK = obj.optBoolean("isGoalkeeper", false)
        if (name.isNotBlank()) list += Player(name, attack, defense, skill, isGK)
    }
    return list
}


private fun saveMatches(context: Context, matches: List<SavedMatch>) {
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

private fun loadMatches(context: Context): List<SavedMatch> {
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

private fun addMatch(context: Context, titleA: String, titleB: String, teamA: List<Player>, teamB: List<Player>) {
    val current = loadMatches(context).toMutableList()
    val now = System.currentTimeMillis()
    val match = SavedMatch(id = now, time = now, titleA = titleA, titleB = titleB, teamA = teamA, teamB = teamB, result = "")
    current.add(0, match)
    saveMatches(context, current)
}

private fun deleteMatch(context: Context, id: Long) {
    val current = loadMatches(context)
    val updated = current.filterNot { it.id == id }
    saveMatches(context, updated)
}

private fun clearAllMatches(context: Context) {
    saveMatches(context, emptyList())
}

private fun updateMatchResult(context: Context, id: Long, result: String) {
    val updated = loadMatches(context).map { if (it.id == id) it.copy(result = result) else it }
    saveMatches(context, updated)
}

@Composable
fun PlayersApp(modifier: Modifier = Modifier, registerSecretOpener: (() -> Unit) -> Unit = {}) {
    val context = LocalContext.current
    var players by remember {
        mutableStateOf(loadPlayers(context).toMutableList())
    }
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

    val selectedPlayers = remember(selected, players) {
        players.filter { selected.contains(it.name) }
    }

    // No más menú secreto; las acciones ahora son visibles en la UI
    registerSecretOpener { /* sin-op */ }

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
            Button(onClick = { showEditDialog = true }) { Text(stringResource(R.string.edit)) }
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
                    val gksSel = selectedPlayers.filter { it.isGoalkeeper }
                    val mustInclude = if (gksSel.size >= 2 && n >= 2) gksSel.take(2) else gksSel.take(minOf(gksSel.size, n))
                    val restCount = n - mustInclude.size
                    val restPool = selectedPlayers.filterNot { it.isGoalkeeper }
                    val chosen = mustInclude + restPool.shuffled().take(restCount)
                    val (a, b) = generateBalancedTeams(chosen)
                    teamA = a
                    teamB = b
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
                    Icon(Icons.Filled.Balance, contentDescription = null, modifier = Modifier.size(28.dp))
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
                    Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(28.dp))
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
        Divider()
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
                Divider()
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
                    addMatch(context, titleA, titleB, teamA, teamB)
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

    if (showEditDialog) {
        EditPlayersDialog(
            players = players,
            onUpdatePlayer = { name, a, d, h ->
                players = players.map { if (it.name == name) it.copy(attack = a, defense = d, skill = h) else it }.toMutableList()
            },
            onAddPlayer = { name, a, d, h, isGoalkeeper ->
                if (players.none { it.name.equals(name, ignoreCase = true) }) {
                    players = (players + Player(name.trim(), a, d, h, isGoalkeeper)).toMutableList()
                    selected = selected + name.trim()
                }
            },
            onSave = {
                savePlayers(context, players)
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
            title = { Text(stringResource(R.string.edit_team_name)) },
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
}

@Composable
fun PlayerRow(player: Player, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
    onRenameB: () -> Unit
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
                    Text("• ${p.name}")
                }
            }
        }
    }
}

private fun generateBalancedTeams(players: List<Player>): Pair<List<Player>, List<Player>> {
    if (players.isEmpty()) return emptyList<Player>() to emptyList()
    val teamA = mutableListOf<Player>()
    val teamB = mutableListOf<Player>()
    var aA = 0.0; var dA = 0.0; var hA = 0.0
    var aB = 0.0; var dB = 0.0; var hB = 0.0

    fun objectiveAfter(addToA: Boolean, p: Player): Double {
        val naA = if (addToA) aA + p.attack else aA
        val ndA = if (addToA) dA + p.defense else dA
        val nhA = if (addToA) hA + p.skill else hA
        val naB = if (addToA) aB else aB + p.attack
        val ndB = if (addToA) dB else dB + p.defense
        val nhB = if (addToA) hB else hB + p.skill
        val da = naA - naB
        val dd = ndA - ndB
        val dh = nhA - nhB
        return da*da + dd*dd + dh*dh
    }

    val goalkeepers = players.filter { it.isGoalkeeper }
    val assignedGK = if (goalkeepers.size >= 2) goalkeepers.sortedByDescending { it.rating }.take(2) else emptyList()
    if (assignedGK.size == 2) {
        val gkA = assignedGK[0]
        val gkB = assignedGK[1]
        teamA += gkA
        teamB += gkB
        aA += gkA.attack; dA += gkA.defense; hA += gkA.skill
        aB += gkB.attack; dB += gkB.defense; hB += gkB.skill
    }

    val remaining = players.filter { !assignedGK.contains(it) }.sortedByDescending { it.rating }
    for (p in remaining) {
        val toA = objectiveAfter(true, p)
        val toB = objectiveAfter(false, p)
        if (toA < toB) {
            teamA += p
            aA += p.attack; dA += p.defense; hA += p.skill
        } else {
            teamB += p
            aB += p.attack; dB += p.defense; hB += p.skill
        }
    }
    while (kotlin.math.abs(teamA.size - teamB.size) > 1) {
        if (teamA.size > teamB.size) {
            val moved = teamA.removeAt(teamA.lastIndex)
            teamB += moved
            aA -= moved.attack; dA -= moved.defense; hA -= moved.skill
            aB += moved.attack; dB += moved.defense; hB += moved.skill
        } else {
            val moved = teamB.removeAt(teamB.lastIndex)
            teamA += moved
            aB -= moved.attack; dB -= moved.defense; hB -= moved.skill
            aA += moved.attack; dA += moved.defense; hA += moved.skill
        }
    }
    return teamA to teamB
}

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
        title = { Text(stringResource(R.string.create_teams_title)) },
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
                                    Text("A")
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
                                    Text("B")
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
                                    Text("N")
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
                        var skillText by remember(p.name) { mutableStateOf("${p.skill}") }
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
                                    label = { Text("Ataque", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                                    label = { Text("Defensa", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                                    label = { Text("Físico", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                            label = { Text("Ataque", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                            label = { Text("Defensa", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                            label = { Text("Físico", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            title = { Text(stringResource(R.string.edit_player_title)) },
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
            title = { Text(stringResource(R.string.delete_player_title)) },
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
