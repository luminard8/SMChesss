package com.smchess.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.smchess.app.chess.GameProtocol
import com.smchess.app.chess.GameReconstructor
import com.smchess.app.chess.PieceColor
import com.smchess.app.data.Conversation
import com.smchess.app.data.SmsRepository
import com.smchess.app.permissions.rememberPermissionsState
import com.smchess.app.sms.SmsEvents
import com.smchess.app.sms.SmsSender
import com.smchess.app.ui.ChatScreen
import com.smchess.app.ui.ChessBoardScreen
import com.smchess.app.ui.ConversationDisplay
import com.smchess.app.ui.ConversationListScreen
import com.smchess.app.ui.DefaultSmsAppScreen
import com.smchess.app.ui.PermissionScreen
import com.smchess.app.ui.SMChessTheme
import com.smchess.app.ui.rememberIsDefaultSmsApp
import com.smchess.app.sms.AppState
import com.smchess.app.sms.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this) // 👈 NOUVEAU
        setContent {
            SMChessTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }

    // 👇 NOUVEAU : tracker premier plan
    override fun onResume() {
        super.onResume()
        AppState.isInForeground = true
    }

    override fun onPause() {
        super.onPause()
        AppState.isInForeground = false
    }
}

private sealed class Screen {
    object ConversationList : Screen()
    data class Chat(val conversation: Conversation) : Screen()
    data class Board(val conversation: Conversation) : Screen()
}

@Composable
private fun AppRoot() {
    val (permState, requestPermissions) = rememberPermissionsState()

    if (!permState.allGranted) {
        PermissionScreen(
            requestedAtLeastOnce = permState.requestedAtLeastOnce,
            onRequestPermissions = requestPermissions
        )
        return
    }

    val isDefaultSmsApp = rememberIsDefaultSmsApp()
    if (!isDefaultSmsApp) {
        DefaultSmsAppScreen()
        return
    }

    MainApp()
}

@Composable
private fun MainApp() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.ConversationList) }
    val tick by SmsEvents.tick.collectAsState()

    // Liste des conversations, recalculée à chaque nouveau SMS (tick) ou retour à l'écran
    var conversations by remember { mutableStateOf<List<ConversationDisplay>>(emptyList()) }
    LaunchedEffect(tick) {
        val raw = SmsRepository.getConversations(context)
        conversations = raw.map { conv ->
            val messages = SmsRepository.getMessagesForThread(context, conv.threadId)
            val (state, _) = GameReconstructor.reconstruct(messages)
            ConversationDisplay(conv, state.phase)
        }
    }

    when (val current = screen) {
        is Screen.ConversationList -> {
            ConversationListScreen(
                conversations = conversations,
                onOpenConversation = { screen = Screen.Chat(it) }
            )
        }
        is Screen.Chat -> {
            val conv = current.conversation
            var messages by remember(conv.threadId, tick) {
                mutableStateOf(SmsRepository.getMessagesForThread(context, conv.threadId))
            }
            LaunchedEffect(tick, conv.threadId) {
                messages = SmsRepository.getMessagesForThread(context, conv.threadId)
            }
            val (gameState, _) = remember(messages) { GameReconstructor.reconstruct(messages) }
            val drawPending = remember(messages) { GameReconstructor.isDrawOfferPending(messages) }

            ChatScreen(
                contactName = conv.displayName,
                messages = messages,
                gameState = gameState,
                drawOfferPending = drawPending,
                onBack = { screen = Screen.ConversationList },
                onChallenge = {
                    SmsSender.send(context, conv.address, GameProtocol.CHALLENGE)
                },
                onAcceptChallenge = {
                    SmsSender.send(context, conv.address, GameProtocol.ACCEPT)
                },
                onPlay = { screen = Screen.Board(conv) },
                onSendText = { text ->
                    SmsSender.send(context, conv.address, text)
                }
            )
        }
        is Screen.Board -> {
            val conv = current.conversation
            var messages by remember(conv.threadId, tick) {
                mutableStateOf(SmsRepository.getMessagesForThread(context, conv.threadId))
            }
            LaunchedEffect(tick, conv.threadId) {
                messages = SmsRepository.getMessagesForThread(context, conv.threadId)
            }
            val (gameState, engine) = remember(messages) { GameReconstructor.reconstruct(messages) }

            // Le défieur (celui qui a envoyé le défi) joue les noirs, l'accepteur joue les blancs.
            val localPlaysColor = if (gameState.challengerIsLocalUser) PieceColor.BLACK else PieceColor.WHITE

            ChessBoardScreen(
                engine = engine,
                localPlaysColor = localPlaysColor,
                gameOverText = gameState.resultText,
                onBack = { screen = Screen.Chat(conv) },
                onMovePlayed = { move ->
                    SmsSender.send(context, conv.address, GameProtocol.encodeMove(move))
                },
                onResign = {
                    SmsSender.send(context, conv.address, GameProtocol.RESIGN)
                },
                onOfferDraw = {
                    SmsSender.send(context, conv.address, GameProtocol.DRAW_OFFER)
                },
                onRematch = {
                    SmsSender.send(context, conv.address, GameProtocol.CHALLENGE)
                    screen = Screen.Chat(conv)
                }
            )
        }
    }
}
