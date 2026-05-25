package com.vyncrit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyncrit.app.data.project.BuildLog
import com.vyncrit.app.data.project.LogLevel
import com.vyncrit.app.ui.theme.BuildError
import com.vyncrit.app.ui.theme.BuildInfo
import com.vyncrit.app.ui.theme.BuildSuccess
import com.vyncrit.app.ui.theme.BuildWarning
import com.vyncrit.app.ui.theme.TerminalBackground
import com.vyncrit.app.ui.theme.TerminalGreen
import com.vyncrit.app.ui.theme.TerminalText
import com.vyncrit.app.viewmodel.TerminalViewModel

@Composable
fun TerminalScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputCommand by remember { mutableStateOf("") }

    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }

    LaunchedEffect(uiState.logs.size) {
        if (uiState.logs.isNotEmpty()) {
            listState.animateScrollToItem(uiState.logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isBuilding) {
                        IconButton(onClick = { /* cancel build */ }) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = BuildError)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalBackground,
                    titleContentColor = TerminalGreen,
                    navigationIconContentColor = TerminalGreen
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TerminalBackground)
        ) {
            if (uiState.isBuilding) {
                LinearProgressIndicator(
                    progress = { uiState.buildProgress },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = TerminalGreen,
                    trackColor = TerminalBackground,
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(uiState.logs) { log ->
                    val color = when (log.level) {
                        LogLevel.DEBUG -> TerminalText.copy(alpha = 0.6f)
                        LogLevel.INFO -> BuildInfo
                        LogLevel.WARN -> BuildWarning
                        LogLevel.ERROR -> BuildError
                    }
                    Text(
                        text = log.message,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = color
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 1.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBackground)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = TerminalGreen
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
                BasicTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = TerminalText
                    ),
                    cursorBrush = SolidColor(TerminalGreen),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            viewModel.executeCommand(inputCommand)
                            inputCommand = ""
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputCommand.isEmpty()) {
                                Text(
                                    "Type 'help' for commands",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        color = TerminalText.copy(alpha = 0.4f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                IconButton(
                    onClick = {
                        viewModel.executeCommand(inputCommand)
                        inputCommand = ""
                    }
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = TerminalGreen
                    )
                }
            }
        }
    }
}
