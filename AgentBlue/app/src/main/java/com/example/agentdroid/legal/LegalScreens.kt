package com.example.agentdroid.legal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agentdroid.ui.theme.AgentBlue

@Composable
fun ConsentScreen(
    onConsentGiven: () -> Unit,
    onViewPrivacyPolicy: () -> Unit,
    onViewTermsOfService: () -> Unit,
    onViewAccessibilityDisclosure: () -> Unit
) {
    var privacyChecked by remember { mutableStateOf(false) }
    var termsChecked by remember { mutableStateOf(false) }
    var accessibilityChecked by remember { mutableStateOf(false) }
    var allChecked by remember { mutableStateOf(false) }

    val canProceed = privacyChecked && termsChecked && accessibilityChecked

    fun updateAllChecked() {
        allChecked = privacyChecked && termsChecked && accessibilityChecked
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                "AgentDroid",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = AgentBlue
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "서비스 이용 동의",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "AgentDroid를 사용하기 위해 아래 약관에 동의해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AgentBlue.copy(alpha = 0.05f)
                )
            ) {
                Text(
                    LegalTexts.CONSENT_SUMMARY.trim(),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = allChecked,
                    onCheckedChange = { checked ->
                        allChecked = checked
                        privacyChecked = checked
                        termsChecked = checked
                        accessibilityChecked = checked
                    },
                    colors = CheckboxDefaults.colors(checkedColor = AgentBlue)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "전체 동의",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            ConsentItem(
                checked = privacyChecked,
                onCheckedChange = {
                    privacyChecked = it
                    updateAllChecked()
                },
                label = "[필수] 개인정보 처리방침 동의",
                onViewDetail = onViewPrivacyPolicy
            )

            Spacer(Modifier.height(4.dp))

            ConsentItem(
                checked = termsChecked,
                onCheckedChange = {
                    termsChecked = it
                    updateAllChecked()
                },
                label = "[필수] 이용약관 동의",
                onViewDetail = onViewTermsOfService
            )

            Spacer(Modifier.height(4.dp))

            ConsentItem(
                checked = accessibilityChecked,
                onCheckedChange = {
                    accessibilityChecked = it
                    updateAllChecked()
                },
                label = "[필수] 접근성 API 사용 동의",
                onViewDetail = onViewAccessibilityDisclosure
            )

            Spacer(Modifier.height(32.dp))

            val buttonColor by animateColorAsState(
                targetValue = if (canProceed) AgentBlue else AgentBlue.copy(alpha = 0.3f),
                label = "buttonColor"
            )

            Button(
                onClick = onConsentGiven,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = buttonColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (canProceed) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "동의하고 시작하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "동의하지 않으실 경우 서비스 이용이 제한됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConsentItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    onViewDetail: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = AgentBlue)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal
            )
        }
        TextButton(onClick = onViewDetail) {
            Text(
                "보기",
                fontSize = 13.sp,
                color = AgentBlue,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(
    title: String,
    content: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = content.trim(),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
