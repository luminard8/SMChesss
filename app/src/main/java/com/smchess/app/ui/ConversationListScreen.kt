package com.smchess.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smchess.app.chess.GamePhase
import com.smchess.app.data.Conversation

data class ConversationDisplay(
    val conversation: Conversation,
    val gamePhase: GamePhase
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    conversations: List<ConversationDisplay>,
    onOpenConversation: (Conversation) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMChess", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aucune conversation SMS", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(conversations) { item ->
                    ConversationRow(item, onClick = { onOpenConversation(item.conversation) })
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(item: ConversationDisplay, onClick: () -> Unit) {
    val conv = item.conversation
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar avec initiale
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AccentGreen.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                conv.displayName.firstOrNull()?.uppercase() ?: "?",
                color = AccentGreen,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(conv.displayName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                GameBadge(item.gamePhase)
            }
            Spacer(Modifier.size(2.dp))
            Text(
                conv.lastMessage.ifBlank { "…" },
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GameBadge(phase: GamePhase) {
    val (label, color) = when (phase) {
        GamePhase.CHALLENGE_PENDING -> "Défi en attente" to Color(0xFFE0A526)
        GamePhase.IN_PROGRESS -> "Partie en cours" to AccentGreen
        GamePhase.FINISHED -> "Partie terminée" to Color.Gray
        GamePhase.NONE -> return
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}
