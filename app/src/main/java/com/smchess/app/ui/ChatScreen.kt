package com.smchess.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smchess.app.chess.GamePhase
import com.smchess.app.chess.GameProtocol
import com.smchess.app.chess.GameState
import com.smchess.app.data.SmsMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactName: String,
    messages: List<SmsMessage>,
    gameState: GameState,
    drawOfferPending: Boolean,
    onBack: () -> Unit,
    onChallenge: () -> Unit,
    onAcceptChallenge: () -> Unit,
    onPlay: () -> Unit,
    onSendText: (String) -> Unit
) {
    var textInput by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contactName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                ActionBar(gameState, drawOfferPending, onChallenge, onAcceptChallenge, onPlay)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message…") }
                    )
                    Button(
                        modifier = Modifier.padding(start = 8.dp),
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSendText(textInput)
                                textInput = ""
                            }
                        }
                    ) { Text("Envoyer") }
                }
            }
        }
    ) { padding ->
        val visibleMessages = messages.filter { !GameProtocol.isProtocolMessage(it.body) }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            items(visibleMessages) { msg ->
                MessageBubble(msg)
            }
        }
    }
}

@Composable
private fun ActionBar(
    gameState: GameState,
    drawOfferPending: Boolean,
    onChallenge: () -> Unit,
    onAcceptChallenge: () -> Unit,
    onPlay: () -> Unit
) {
    when (gameState.phase) {
        GamePhase.NONE, GamePhase.FINISHED -> {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Button(onClick = onChallenge, modifier = Modifier.weight(1f)) {
                    Text("Défier")
                }
            }
        }
        GamePhase.CHALLENGE_PENDING -> {
            if (gameState.challengerIsLocalUser) {
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(
                        "Défi envoyé, en attente de réponse…",
                        modifier = Modifier.padding(12.dp),
                        color = Color.Gray
                    )
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("♟ Défi aux échecs reçu !")
                        Button(onClick = onAcceptChallenge, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Accepter")
                        }
                    }
                }
            }
        }
        GamePhase.IN_PROGRESS -> {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                if (drawOfferPending) {
                    Text(
                        "Nulle proposée — ouvrez la partie pour répondre",
                        modifier = Modifier.weight(1f).padding(8.dp),
                        color = Color.Gray
                    )
                }
                Button(onClick = onPlay, modifier = Modifier.weight(1f)) {
                    Text("Jouer")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: SmsMessage) {
    val bubbleColor = if (msg.isOutgoing) BubbleOutgoing else BubbleIncoming
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Text(msg.body, color = Color.White)
        }
    }
}
