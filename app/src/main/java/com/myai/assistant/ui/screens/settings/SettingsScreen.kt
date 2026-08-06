package com.myai.assistant.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myai.assistant.data.manager.SettingsManager
import com.myai.assistant.domain.model.AiModelType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // API 配置卡片
            ApiConfigCard(
                apiKey = uiState.apiKey,
                baseUrl = uiState.baseUrl,
                selectedModel = uiState.selectedModel,
                onApiKeyChange = viewModel::updateApiKey,
                onBaseUrlChange = viewModel::updateBaseUrl,
                onModelSelected = viewModel::updateModel,
                onSave = { 
                    scope.launch {
                        viewModel.saveSettings(context)
                    }
                }
            )

            // 数据管理卡片
            DataManagementCard(
                memoryCount = uiState.memoryCount,
                onClearMemories = { viewModel.clearMemories() },
                onExportData = { viewModel.exportData(context) }
            )

            // 关于卡片
            AboutCard()
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ApiConfigCard(
    apiKey: String,
    baseUrl: String,
    selectedModel: AiModelType,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelSelected: (AiModelType) -> Unit,
    onSave: () -> Unit
) {
    var showApiKey by remember { mutableStateOf(false) }
    var localApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var localBaseUrl by remember(baseUrl) { mutableStateOf(baseUrl) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "AI 模型配置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // API Key 输入
            OutlinedTextField(
                value = localApiKey,
                onValueChange = { 
                    localApiKey = it
                    onApiKeyChange(it)
                },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) 
                                androidx.compose.material.icons.Icons.Default.VisibilityOff 
                            else 
                                androidx.compose.material.icons.Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "隐藏" else "显示"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("您的密钥将加密存储在本地设备中") }
            )

            // Base URL 输入 (高级选项)
            OutlinedTextField(
                value = localBaseUrl,
                onValueChange = { 
                    localBaseUrl = it
                    onBaseUrlChange(it)
                },
                label = { Text("API Base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("支持兼容 OpenAI 格式的第三方接口") }
            )

            // 模型选择
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedModel.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("选择模型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AiModelType.entries.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.displayName) },
                            onClick = {
                                onModelSelected(model)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存配置")
            }
        }
    }
}

@Composable
fun DataManagementCard(
    memoryCount: Int,
    onClearMemories: () -> Unit,
    onExportData: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "数据管理",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("当前记忆数量：$memoryCount")
                AssistChip(
                    onClick = onExportData,
                    label = { Text("导出备份") }
                )
            }

            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("清空所有记忆数据")
            }
        }
    }
    
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认清空") },
            text = { Text("此操作将删除所有本地记忆数据，且无法恢复。确定继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearMemories()
                        showConfirmDialog = false
                    }
                ) {
                    Text("确认", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "关于 AI Assistant",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("版本：1.0.0")
            Text("构建：Debug")
            Text("所有数据均存储于本地，仅通过 API 调用云端模型。")
            Text("支持多人格、长期记忆、RAG 检索增强等功能。")
        }
    }
}
