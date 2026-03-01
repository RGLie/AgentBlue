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
                title = "개인정보 처리방침",
                content = LegalTexts.PRIVACY_POLICY,
                onBack = { currentScreen = previousScreen }
            )
        }

        AppScreen.TERMS_OF_SERVICE -> {
            LegalDocumentScreen(
                title = "이용약관",
                content = LegalTexts.TERMS_OF_SERVICE,
                onBack = { currentScreen = previousScreen }
            )
        }

        AppScreen.ACCESSIBILITY_DISCLOSURE -> {
            LegalDocumentScreen(
                title = "접근성 API 사용 고지",
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
                            contentDescription = "앱 정보",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "기록 삭제",
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
                            "실행 기록",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${history.size}건",
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
            title = { Text("기록 삭제") },
            text = { Text("모든 실행 기록을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    AgentStateManager.clearHistory()
                    showClearDialog = false
                }) {
                    Text("삭제", color = StatusFailed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("취소")
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
                        "AI Agent 모델 설정",
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
                    if (expanded) "접기" else "변경",
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
                        "API 키를 설정해주세요",
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
                        "프로바이더",
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
                        "모델",
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
                        placeholder = { Text("API 키를 입력하세요") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    if (passwordVisible) "숨기기" else "보기",
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
                            savedMessage = "설정이 저장되었습니다"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AgentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("저장", modifier = Modifier.padding(vertical = 4.dp))
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
                        "에이전트 설정",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "최대 ${maxSteps.toInt()}스텝 · 딜레이 ${"%.1f".format(stepDelay / 1000f)}초 · $selectedBrowser",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (expanded) "접기" else "변경",
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
                        "최대 스텝 수",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "에이전트가 최대 몇 번까지 시도할지 설정합니다.",
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
                        "스텝 딜레이",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "각 스텝 사이의 대기 시간입니다. 느린 기기에서는 길게 설정하세요.",
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
                            "${"%.1f".format(stepDelay / 1000f)}초",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AgentBlue
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "기본 브라우저",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "검색 시 사용할 브라우저 앱을 지정합니다.",
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
                        "응답 언어",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "AI 에이전트의 추론 언어를 설정합니다.",
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
                            savedMessage = "설정이 저장되었습니다"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AgentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("저장", modifier = Modifier.padding(vertical = 4.dp))
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
                "서비스 설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "AgentDroid를 사용하려면 아래 설정을 활성화해주세요.",
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
                Text("접근성 서비스 설정", modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenOverlaySettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("화면 위에 표시 권한 설정", modifier = Modifier.padding(vertical = 4.dp))
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
        AgentStatus.COMPLETED -> "완료"
        AgentStatus.FAILED -> "실패"
        AgentStatus.CANCELLED -> "취소"
        else -> "기타"
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
                            "  ·  ${steps.size}스텝",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (record.endTime != null) {
                            val duration = (record.endTime - record.startTime) / 1000
                            Text(
                                "  ·  ${duration}초",
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
                    if (expanded) "접기" else "상세 보기 (${steps.size}스텝)",
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
                    "세션 연결",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPaired) StatusRunning.copy(alpha = 0.15f)
                    else StatusIdle.copy(alpha = 0.15f)
                ) {
                    Text(
                        if (isPaired) "연결됨" else "미연결",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (isPaired) StatusRunning else StatusIdle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                if (isPaired) "세션 코드: $pairedCode"
                else "Desktop에서 생성한 세션 코드를 입력하세요.",
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
                    "⚠ 세션 코드를 절대 타인에게 공유하지 마세요. 코드를 아는 제3자가 기기를 원격 조작할 수 있습니다.",
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
                    placeholder = { Text("세션 코드 (8자리)") },
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
                            errorMessage = "8자리 세션 코드를 입력해주세요."
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
                                    errorMessage = "유효한 세션을 찾을 수 없습니다."
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
                                    errorMessage = "연결 실패: ${e.message}"
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = "검색 실패: ${e.message}"
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && sessionCode.length == 8,
                    colors = ButtonDefaults.buttonColors(containerColor = AgentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        Text("연결 중...", modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        Text("세션 연결", modifier = Modifier.padding(vertical = 4.dp))
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
                    Text("세션 연결 해제", modifier = Modifier.padding(vertical = 4.dp))
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
                    "AgentDroid는 AI가 안드로이드 화면을 분석하고 자동으로 조작하는 에이전트 앱입니다. PC 또는 다른 기기의 웹 브라우저에서 명령을 입력하면, 연결된 안드로이드 기기가 자동으로 실행합니다.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "초기 설정",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                val setupSteps = listOf(
                    "1. 접근성 서비스를 활성화합니다.",
                    "2. 화면 위에 표시 권한을 허용합니다.",
                    "3. AI 모델과 API 키를 설정합니다."
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
                    "Desktop 웹에서 원격 명령 실행",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "PC나 다른 기기의 브라우저에서 아래 웹 페이지에 접속하여 명령을 보낼 수 있습니다.",
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
                                    clipboard.setPrimaryClip(ClipData.newPlainText("AgentDroid Web", webDashboardUrl))
                                    linkCopiedMessage = "링크가 복사되었습니다"
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("링크 복사", fontSize = 12.sp)
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
                    "1. 웹 페이지에서 '세션 생성'을 눌러 8자리 세션 코드를 발급받습니다.",
                    "2. 이 앱의 '세션 연결' 카드에서 해당 코드를 입력합니다.",
                    "3. 연결 완료 후, 웹에서 명령을 입력하면 이 기기에서 자동 실행됩니다.",
                    "4. 실행 상태와 결과를 웹에서 실시간으로 확인할 수 있습니다."
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
                    "기기에서 직접 명령 실행",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "세션 연결 없이도 화면에 표시되는 플로팅 버튼을 눌러 직접 명령을 입력할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "법적 고지",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                TextButton(onClick = {
                    onDismiss()
                    onNavigateToLegal(AppScreen.PRIVACY_POLICY)
                }) {
                    Text(
                        "개인정보 처리방침",
                        color = AgentBlue,
                        fontSize = 14.sp
                    )
                }

                TextButton(onClick = {
                    onDismiss()
                    onNavigateToLegal(AppScreen.TERMS_OF_SERVICE)
                }) {
                    Text(
                        "이용약관",
                        color = AgentBlue,
                        fontSize = 14.sp
                    )
                }

                TextButton(onClick = {
                    onDismiss()
                    onNavigateToLegal(AppScreen.ACCESSIBILITY_DISCLOSURE)
                }) {
                    Text(
                        "접근성 API 사용 고지",
                        color = AgentBlue,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "변경 로그",
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
                            "에이전트 동작 설정 추가 (최대 스텝, 딜레이, 브라우저, 언어)",
                            "HOME 액션 지원",
                            "Stuck 감지 및 자동 복구 시스템",
                            "앱 정보 다이얼로그"
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
                            "· 최초 릴리즈",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = AgentBlue, fontWeight = FontWeight.SemiBold)
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
            "아직 실행 기록이 없습니다",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "플로팅 버튼을 눌러 첫 명령을 시작해보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
