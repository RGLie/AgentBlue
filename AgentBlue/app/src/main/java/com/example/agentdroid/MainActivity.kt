package com.example.agentdroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agentdroid.data.AgentPreferences
import com.example.agentdroid.data.ConsentPreferences
import com.example.agentdroid.data.ExecutionEntity
import com.example.agentdroid.data.ModelPreferences
import com.example.agentdroid.data.SessionPreferences
import com.example.agentdroid.legal.ConsentScreen
import com.example.agentdroid.legal.LegalDocumentScreen
import com.example.agentdroid.legal.LegalTexts
import com.example.agentdroid.model.AgentStatus
import com.example.agentdroid.model.AiProvider
import com.example.agentdroid.model.StepLog
import com.example.agentdroid.ui.theme.AgentBlue
import com.example.agentdroid.ui.theme.AgentDroidTheme
import com.example.agentdroid.ui.theme.StatusCancelled
import com.example.agentdroid.ui.theme.StatusCancelledBg
import com.example.agentdroid.ui.theme.StatusCompleted
import com.example.agentdroid.ui.theme.StatusCompletedBg
import com.example.agentdroid.ui.theme.StatusFailed
import com.example.agentdroid.ui.theme.StatusFailedBg
import com.example.agentdroid.ui.theme.StatusIdle
import com.example.agentdroid.ui.theme.StatusIdleBg
import com.example.agentdroid.ui.theme.StatusRunning
import com.example.agentdroid.ui.theme.StatusRunningBg
import com.example.agentdroid.ui.theme.StepError
import com.example.agentdroid.ui.theme.StepSuccess
import com.example.agentdroid.service.AgentAccessibilityService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AgentStateManager.init(this)
        AgentPreferences.init(this)
        SessionPreferences.init(this)
        ConsentPreferences.init(this)

        val defaultKey = try { BuildConfig.OPENAI_API_KEY } catch (_: Exception) { "" }
        ModelPreferences.init(this, defaultKey)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }

        enableEdgeToEdge()
        setContent {
            AgentDroidTheme {
                AppNavigator(
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenOverlaySettings = {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    }
                )
            }
        }
    }
}

enum class AppScreen {
    CONSENT,
    MAIN,
    PRIVACY_POLICY,
    TERMS_OF_SERVICE,
    ACCESSIBILITY_DISCLOSURE
}

@Composable
fun AppNavigator(
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    var currentScreen by remember {
        mutableStateOf(
            if (ConsentPreferences.hasFullConsent()) AppScreen.MAIN else AppScreen.CONSENT
        )
    }
    var previousScreen by remember { mutableStateOf(AppScreen.CONSENT) }

    when (currentScreen) {
        AppScreen.CONSENT -> {
            ConsentScreen(
                onConsentGiven = {
                    ConsentPreferences.saveConsent()
                    currentScreen = AppScreen.MAIN
                },
                onViewPrivacyPolicy = {
                    previousScreen = AppScreen.CONSENT
                    currentScreen = AppScreen.PRIVACY_POLICY
                },
                onViewTermsOfService = {
                    previousScreen = AppScreen.CONSENT
                    currentScreen = AppScreen.TERMS_OF_SERVICE
                },
                onViewAccessibilityDisclosure = {
                    previousScreen = AppScreen.CONSENT
                    currentScreen = AppScreen.ACCESSIBILITY_DISCLOSURE
                }
            )
        }

        AppScreen.MAIN -> {
            AgentDroidApp(
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenOverlaySettings = onOpenOverlaySettings,
                onNavigateToLegal = { screen ->
                    previousScreen = AppScreen.MAIN
                    currentScreen = screen
                }
            )
        }

        AppScreen.PRIVACY_POLICY -> {
            LegalDocumentScreen(
                title = "Privacy Policy",
                content = LegalTexts.PRIVACY_POLICY,
                onBack = { currentScreen = previousScreen }
            )
        }

        AppScreen.TERMS_OF_SERVICE -> {
            LegalDocumentScreen(
                title = "Terms of Service",
                content = LegalTexts.TERMS_OF_SERVICE,
                onBack = { currentScreen = previousScreen }
            )
        }

        AppScreen.ACCESSIBILITY_DISCLOSURE -> {
            LegalDocumentScreen(
                title = "Accessibility API Usage Disclosure",
                content = LegalTexts.ACCESSIBILITY_DISCLOSURE,
                onBack = { currentScreen = previousScreen }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDroidApp(
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onNavigateToLegal: (AppScreen) -> Unit = {}
) {
    val history by AgentStateManager.getHistoryFlow().collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("AgentDroid", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "App info",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear history",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SessionCard() }
            item { ModelSettingsCard() }
            item { AgentSettingsCard() }
            item { SettingsCard(onOpenAccessibilitySettings, onOpenOverlaySettings) }

            if (history.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${history.size} items",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(history, key = { it.id }) { record ->
                    HistoryCard(record)
                }
            } else {
                item { EmptyState() }
            }
        }
    }

    if (showInfoDialog) {
        InfoDialog(
            onDismiss = { showInfoDialog = false },
            onNavigateToLegal = onNavigateToLegal
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Delete all execution history?") },
            confirmButton = {
                TextButton(onClick = {
                    AgentStateManager.clearHistory()
                    showClearDialog = false
                }) {
                    Text("Delete", color = StatusFailed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- AI 모델 설정 카드 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsCard() {
    var expanded by remember { mutableStateOf(false) }

    var selectedProvider by remember { mutableStateOf(ModelPreferences.getProvider()) }
    var selectedModelId by remember { mutableStateOf(ModelPreferences.getModel()) }
    var apiKeyText by remember { mutableStateOf(ModelPreferences.getApiKey(ModelPreferences.getProvider())) }
    var passwordVisible by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    val currentDisplayModel = selectedProvider.models
        .find { it.id == selectedModelId }?.displayName ?: selectedModelId

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI Model Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${selectedProvider.displayName} · $currentDisplayModel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (expanded) "Collapse" else "Change",
                    color = AgentBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            if (!ModelPreferences.hasApiKey() && !expanded) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusFailed.copy(alpha = 0.1f)
                ) {
                    Text(
                        "Please set your API key",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = StatusFailed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Provider",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiProvider.entries.forEach { provider ->
                            FilterChip(
                                selected = selectedProvider == provider,
                                onClick = {
                                    selectedProvider = provider
                                    selectedModelId = ModelPreferences.getModelForProvider(provider)
                                    apiKeyText = ModelPreferences.getApiKey(provider)
                                    savedMessage = null
                                },
                                label = { Text(provider.displayName, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AgentBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = AgentBlue
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Model",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = modelDropdownExpanded,
                        onExpandedChange = { modelDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentDisplayModel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false }
                        ) {
                            selectedProvider.models.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            model.displayName,
                                            fontWeight = if (model.id == selectedModelId) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedModelId = model.id
                                        modelDropdownExpanded = false
                                        savedMessage = null
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "API Key",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = {
                            apiKeyText = it
                            savedMessage = null
                        },
                        placeholder = { Text("Enter your API key") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    if (passwordVisible) "Hide" else "Show",
                                    fontSize = 12.sp,
                                    color = AgentBlue
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            ModelPreferences.save(selectedProvider, selectedModelId, apiKeyText)
                            savedMessage = "Settings saved"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AgentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", modifier = Modifier.padding(vertical = 4.dp))
                    }

                    savedMessage?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            msg,
                            color = StatusCompleted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        LaunchedEffect(msg) {
                            delay(2500)
                            savedMessage = null
                        }
                    }
                }
            }
        }
    }
}

// --- 에이전트 동작 설정 카드 ---

@Composable
fun AgentSettingsCard() {
    var expanded by remember { mutableStateOf(false) }

    var maxSteps by remember { mutableFloatStateOf(AgentPreferences.getMaxSteps().toFloat()) }
    var stepDelay by remember { mutableFloatStateOf(AgentPreferences.getStepDelayMs().toFloat()) }
    var selectedBrowser by remember { mutableStateOf(AgentPreferences.getDefaultBrowser()) }
    var selectedLanguage by remember { mutableStateOf(AgentPreferences.getLanguage()) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Agent Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Max ${maxSteps.toInt()} steps · Delay ${"%.1f".format(stepDelay / 1000f)}s · $selectedBrowser",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (expanded) "Collapse" else "Change",
                    color = AgentBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Max Steps",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Maximum number of attempts the agent will make.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = maxSteps,
                            onValueChange = { maxSteps = it },
                            valueRange = 5f..30f,
                            steps = 24,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = AgentBlue,
                                activeTrackColor = AgentBlue
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${maxSteps.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AgentBlue
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Step Delay",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Wait time between steps. Increase for slower devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = stepDelay,
                            onValueChange = { stepDelay = it },
                            valueRange = 500f..3000f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = AgentBlue,
                                activeTrackColor = AgentBlue
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${"%.1f".format(stepDelay / 1000f)}s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AgentBlue
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Default Browser",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Browser app to use for web searches.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AgentPreferences.BROWSER_OPTIONS.forEach { browser ->
                            FilterChip(
                                selected = selectedBrowser == browser,
                                onClick = { selectedBrowser = browser },
                                label = { Text(browser, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AgentBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = AgentBlue
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Response Language",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Language used for the AI agent's reasoning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AgentPreferences.LANGUAGE_OPTIONS.forEach { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = { selectedLanguage = lang },
                                label = { Text(lang, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AgentBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = AgentBlue
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            AgentPreferences.setMaxSteps(maxSteps.toInt())
                            AgentPreferences.setStepDelayMs(stepDelay.toLong())
                            AgentPreferences.setDefaultBrowser(selectedBrowser)
                            AgentPreferences.setLanguage(selectedLanguage)
                            savedMessage = "Settings saved"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AgentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", modifier = Modifier.padding(vertical = 4.dp))
                    }

                    savedMessage?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            msg,
                            color = StatusCompleted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        LaunchedEffect(msg) {
                            delay(2500)
                            savedMessage = null
                        }
                    }
                }
            }
        }
    }
}

// --- 설정 카드 ---

@Composable
fun SettingsCard(
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Permissions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Enable the permissions below to use AgentBlue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AgentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Accessibility Service", modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenOverlaySettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Overlay Permission", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

// --- 실행 기록 카드 ---

@Composable
fun HistoryCard(record: ExecutionEntity) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val steps = remember(record.stepsJson) { AgentStateManager.parseStepsFromJson(record.stepsJson) }

    val status = try { AgentStatus.valueOf(record.status) } catch (_: Exception) { AgentStatus.IDLE }
    val statusColor = when (status) {
        AgentStatus.COMPLETED -> StatusCompleted
        AgentStatus.FAILED -> StatusFailed
        AgentStatus.CANCELLED -> StatusCancelled
        else -> StatusIdle
    }
    val statusLabel = when (status) {
        AgentStatus.COMPLETED -> "Done"
        AgentStatus.FAILED -> "Failed"
        AgentStatus.CANCELLED -> "Cancelled"
        else -> "Other"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        record.command,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dateFormat.format(Date(record.startTime)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "  ·  ${steps.size} steps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (record.endTime != null) {
                            val duration = (record.endTime - record.startTime) / 1000
                            Text(
                                "  ·  ${duration}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (record.status) {
                        AgentStatus.COMPLETED.name -> StatusCompletedBg
                        AgentStatus.RUNNING.name -> StatusRunningBg
                        AgentStatus.FAILED.name -> StatusFailedBg
                        AgentStatus.CANCELLED.name -> StatusCancelledBg
                        else -> StatusIdleBg
                    }
                ) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!record.resultMessage.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    record.resultMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded && steps.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    steps.forEach { stepLog ->
                        StepLogRow(stepLog)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            if (steps.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (expanded) "Collapse" else "Details (${steps.size} steps)",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgentBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun StepLogRow(stepLog: StepLog) {
    val statusColor = when {
        stepLog.actionType == "DONE" -> StatusCompleted
        stepLog.actionType == "ERROR" -> StepError
        stepLog.success -> StepSuccess
        else -> StatusFailed
    }

    val label = when (stepLog.actionType.uppercase()) {
        "CLICK" -> "TAP"
        "TYPE" -> "TYPE"
        "SCROLL" -> "SCROLL"
        "BACK" -> "BACK"
        "HOME" -> "HOME"
        "DONE" -> "DONE"
        "ERROR" -> "ERR"
        else -> stepLog.actionType
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            val bgColor = when {
                stepLog.actionType == "DONE" -> StatusCompletedBg
                stepLog.actionType == "ERROR" -> StatusFailedBg
                stepLog.success -> StatusRunningBg
                else -> StatusFailedBg
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = bgColor
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Step ${stepLog.step}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (stepLog.targetText != null) {
                        Text(
                            "  →  ${stepLog.targetText}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!stepLog.reasoning.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stepLog.reasoning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// --- 세션 페어링 카드 ---

@Composable
fun SessionCard() {
    var sessionCode by remember { mutableStateOf("") }
    var isPaired by remember { mutableStateOf(SessionPreferences.hasPairedSession()) }
    var pairedCode by remember { mutableStateOf(SessionPreferences.getSessionCode() ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val firestore = remember { FirebaseFirestore.getInstance() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPaired) StatusRunning.copy(alpha = 0.15f)
                    else StatusIdle.copy(alpha = 0.15f)
                ) {
                    Text(
                        if (isPaired) "Connected" else "Not connected",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (isPaired) StatusRunning else StatusIdle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                if (isPaired) "Session code: $pairedCode"
                else "Enter the session code generated by AgentBlueCLI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StatusCancelled.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "⚠ Never share your session code with anyone. Third parties who know the code can remotely control your device.",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = StatusCancelled,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isPaired) {
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = sessionCode,
                    onValueChange = {
                        sessionCode = it.uppercase().take(8)
                        errorMessage = null
                    },
                    placeholder = { Text("Session code (8 chars)") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                errorMessage?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Text(msg, color = StatusFailed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (sessionCode.length != 8) {
                            errorMessage = "Please enter an 8-character session code."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null

                        firestore.collection("sessions")
                            .whereEqualTo("code", sessionCode)
                            .whereEqualTo("status", "waiting")
                            .get()
                            .addOnSuccessListener { snapshots ->
                                if (snapshots.isEmpty) {
                                    isLoading = false
                                    errorMessage = "No valid session found."
                                    return@addOnSuccessListener
                                }
                                val doc = snapshots.documents.first()
                                val uid = FirebaseAuth.getInstance().currentUser?.uid

                                doc.reference.update(
                                    mapOf(
                                        "androidUid" to uid,
                                        "status" to "paired"
                                    )
                                ).addOnSuccessListener {
                                    SessionPreferences.save(doc.id, sessionCode)
                                    isPaired = true
                                    pairedCode = sessionCode
                                    isLoading = false
                                    AgentAccessibilityService.instance?.restartCommandListener()
                                }.addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = "Connection failed: ${e.message}"
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = "Search failed: ${e.message}"
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && sessionCode.length == 8,
                    colors = ButtonDefaults.buttonColors(containerColor = AgentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        Text("Connecting...", modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        Text("Connect", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val oldSessionId = SessionPreferences.getSessionId()
                        SessionPreferences.clear()
                        isPaired = false
                        pairedCode = ""
                        sessionCode = ""

                        if (oldSessionId != null) {
                            firestore.collection("sessions").document(oldSessionId)
                                .update("status", "disconnected")
                        }
                        AgentAccessibilityService.instance?.restartCommandListener()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Disconnect", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

// --- 정보 다이얼로그 ---

@Composable
fun InfoDialog(
    onDismiss: () -> Unit,
    onNavigateToLegal: (AppScreen) -> Unit = {}
) {
    val versionName = try { BuildConfig.VERSION_NAME } catch (_: Exception) { "1.1.0" }
    val context = LocalContext.current
    val webDashboardUrl = "https://agentblue-d83e5.web.app"
    var linkCopiedMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("AgentDroid", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "v$versionName",
                    style = MaterialTheme.typography.labelMedium,
                    color = AgentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "AgentBlue is an AI agent app that analyzes your Android screen and automates tasks. Send commands from AgentBlueCLI or your browser, and the connected Android device executes them automatically.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "Initial Setup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                val setupSteps = listOf(
                    "1. Enable the Accessibility Service.",
                    "2. Allow the Overlay permission.",
                    "3. Set your AI model and API key."
                )
                setupSteps.forEach { step ->
                    Text(
                        step,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "Remote Control via CLI or Web",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Use AgentBlueCLI or open the web dashboard in a browser to send commands remotely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AgentBlue.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            webDashboardUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = AgentBlue,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("AgentBlue Web", webDashboardUrl))
                                    linkCopiedMessage = "Link copied"
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Copy link", fontSize = 12.sp)
                            }
                        }
                        linkCopiedMessage?.let { msg ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                msg,
                                color = StatusCompleted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            LaunchedEffect(msg) {
                                delay(2000)
                                linkCopiedMessage = null
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                val remoteSteps = listOf(
                    "1. Run agentblue start in your terminal to get an 8-char session code.",
                    "2. Enter the code in this app's Session card.",
                    "3. Once connected, type commands in the CLI and this device executes them.",
                    "4. View real-time status and results in the CLI."
                )
                remoteSteps.forEach { step ->
                    Text(
                        step,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "On-device Commands",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap the floating button on screen to enter commands directly without a CLI session.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "Legal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                TextButton(onClick = {
                    onDismiss()
                    onNavigateToLegal(AppScreen.PRIVACY_POLICY)
                }) {
                    Text(
                        "Privacy Policy",
                        color = AgentBlue,
                        fontSize = 14.sp
                    )
                }

                TextButton(onClick = {
                    onDismiss()
                    onNavigateToLegal(AppScreen.TERMS_OF_SERVICE)
                }) {
                    Text(
                        "Terms of Service",
                        color = AgentBlue,
                        fontSize = 14.sp
                    )
                }

                TextButton(onClick = {
                    onDismiss()
                    onNavigateToLegal(AppScreen.ACCESSIBILITY_DISCLOSURE)
                }) {
                    Text(
                        "Accessibility API Disclosure",
                        color = AgentBlue,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "Changelog",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AgentBlue.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "v1.1.0",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AgentBlue
                        )
                        Spacer(Modifier.height(4.dp))
                        val changes110 = listOf(
                            "Agent behavior settings (max steps, delay, browser, language)",
                            "HOME action support",
                            "Stuck detection and auto-recovery system",
                            "App info dialog"
                        )
                        changes110.forEach { change ->
                            Text(
                                "· $change",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "v1.0.0",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "· Initial release",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AgentBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// --- 빈 상태 ---

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🤖", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "No history yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap the floating button or send a command from the CLI to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
