package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.ui.components.SearchGroundingBar
import com.example.ui.theme.WhereDeepGreen
import com.example.ui.theme.WhereMainText
import com.example.ui.theme.WhereMintBorder
import com.example.ui.theme.WhereMintSurface
import com.example.ui.theme.WhereMutedText
import com.example.ui.theme.WhereSubtleBorder
import com.example.ui.theme.WhereWhite

private val ABUJA_STARTER_PROMPTS = listOf(
    "Best route from Wuse Market to Jabi Lake Mall?",
    "Cheaper option from Kubwa to Federal Secretariat?",
    "Rush hour tips around Berger & AYA?",
    "Where are Keke & Okada restricted in Abuja?"
)

@Composable
fun AiGuideScreen(
    messages: List<ChatMessage>,
    learnedCorridorsCount: Int,
    isSending: Boolean,
    chatError: String?,
    onSendMessage: (String) -> Unit,
    onRetryLastMessage: () -> Unit,
    onResetChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WhereWhite)
            .imePadding()
    ) {
        // Header Bar
        Surface(
            color = WhereWhite,
            border = BorderStroke(1.dp, WhereSubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = WhereDeepGreen
                    )
                    Column {
                        Text(
                            text = "Abuja AI Guide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhereMainText
                        )
                        Text(
                            text = "Abuja transport only · $learnedCorridorsCount learned corridors",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhereMutedText
                        )
                    }
                }
                IconButton(
                    onClick = onResetChat,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "New Abuja transit chat",
                        tint = WhereDeepGreen
                    )
                }
            }
        }

        // Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }
        }

        if (chatError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("chat_error_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = chatError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onRetryLastMessage,
                        enabled = !isSending
                    ) {
                        Text(
                            text = "Retry",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Quick Prompts
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ABUJA_STARTER_PROMPTS) { prompt ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = WhereMintSurface,
                    border = BorderStroke(1.dp, WhereMintBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(enabled = !isSending) { onSendMessage(prompt) }
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WhereDeepGreen,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Composer
        Surface(
            color = WhereWhite,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, WhereSubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about Abuja routes, rush hour, landmarks, safety...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )
                FilledIconButton(
                    onClick = {
                        val toSend = inputText
                        inputText = ""
                        onSendMessage(toSend)
                    },
                    enabled = inputText.isNotBlank() && !isSending,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = WhereDeepGreen,
                        contentColor = WhereWhite
                    ),
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("chat_send_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = WhereWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (message.isUser) WhereDeepGreen else WhereWhite
    val contentColor = if (message.isUser) WhereWhite else WhereMainText

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = if (message.isUser) null else BorderStroke(1.dp, WhereSubtleBorder),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (message.text.isBlank() && message.isStreaming) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = WhereDeepGreen,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Checking Abuja transit memory...",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhereMutedText
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }

                if (!message.isUser && !message.isStreaming) {
                    SearchGroundingBar(
                        webSources = message.webSources,
                        searchEntryPointHtml = message.searchEntryPointHtml
                    )
                }
            }
        }
    }
}
