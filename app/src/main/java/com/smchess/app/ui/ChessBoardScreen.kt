package com.smchess.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smchess.app.chess.ChessEngine
import com.smchess.app.chess.Move
import com.smchess.app.chess.PieceColor
import com.smchess.app.chess.PieceType
import com.smchess.app.chess.Square

private fun pieceSymbol(type: PieceType, color: PieceColor): String {
    val white = color == PieceColor.WHITE
    return when (type) {
        PieceType.KING -> if (white) "♔" else "♚"
        PieceType.QUEEN -> if (white) "♕" else "♛"
        PieceType.ROOK -> if (white) "♖" else "♜"
        PieceType.BISHOP -> if (white) "♗" else "♝"
        PieceType.KNIGHT -> if (white) "♘" else "♞"
        PieceType.PAWN -> if (white) "♙" else "♟"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    engine: ChessEngine,
    localPlaysColor: PieceColor,
    gameOverText: String?,
    onBack: () -> Unit,
    onMovePlayed: (Move) -> Unit,
    onResign: () -> Unit,
    onOfferDraw: () -> Unit,
    onRematch: () -> Unit
) {
    var selected by remember { mutableStateOf<Square?>(null) }
    var legalTargets by remember { mutableStateOf<List<Move>>(emptyList()) }
    var pendingPromotionMove by remember { mutableStateOf<Pair<Square, Square>?>(null) }
    var showResignConfirm by remember { mutableStateOf(false) }

    val isLocalTurn = engine.sideToMove == localPlaysColor && gameOverText == null

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    if (gameOverText != null) "Partie terminée"
                    else if (isLocalTurn) "À vous de jouer" else "Au tour de l'adversaire"
                )
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp)
                    .pointerInput(selected, isLocalTurn) {
                        if (!isLocalTurn) return@pointerInput
                        detectTapGestures { tapOffset ->
                            val cell = size.width / 8f
                            val col = (tapOffset.x / cell).toInt().coerceIn(0, 7)
                            val row = (tapOffset.y / cell).toInt().coerceIn(0, 7)
                            val tapped = Square(row, col)

                            val currentSelected = selected
                            if (currentSelected == null) {
                                val piece = engine.pieceAt(tapped)
                                if (piece != null && piece.color == localPlaysColor) {
                                    selected = tapped
                                    legalTargets = engine.legalMoves(tapped)
                                }
                            } else {
                                val candidates = legalTargets.filter { it.to == tapped }
                                if (candidates.size > 1) {
                                    // Plusieurs coups possibles vers cette case = choix de promotion à faire
                                    pendingPromotionMove = currentSelected to tapped
                                    selected = null
                                    legalTargets = emptyList()
                                } else if (candidates.size == 1) {
                                    onMovePlayed(candidates[0])
                                    selected = null
                                    legalTargets = emptyList()
                                } else {
                                    val piece = engine.pieceAt(tapped)
                                    if (piece != null && piece.color == localPlaysColor) {
                                        selected = tapped
                                        legalTargets = engine.legalMoves(tapped)
                                    } else {
                                        selected = null
                                        legalTargets = emptyList()
                                    }
                                }
                            }
                        }
                    }
            ) {
                BoardCanvas(engine, selected, legalTargets)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (gameOverText == null) {
                    OutlinedButton(onClick = { showResignConfirm = true }) { Text("Abandonner") }
                    OutlinedButton(onClick = onOfferDraw) { Text("Proposer nulle") }
                } else {
                    OutlinedButton(onClick = onBack) { Text("Quitter") }
                    Button(onClick = onRematch) { Text("Revanche") }
                }
            }
        }
    }

    if (gameOverText != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Fin de partie") },
            text = { Text(gameOverText) },
            confirmButton = {
                Button(onClick = onRematch) { Text("Revanche") }
            },
            dismissButton = {
                TextButton(onClick = onBack) { Text("Quitter") }
            }
        )
    }

    if (showResignConfirm) {
        AlertDialog(
            onDismissRequest = { showResignConfirm = false },
            title = { Text("Abandonner la partie ?") },
            confirmButton = {
                Button(onClick = {
                    showResignConfirm = false
                    onResign()
                }) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { showResignConfirm = false }) { Text("Annuler") }
            }
        )
    }

    pendingPromotionMove?.let { (from, to) ->
        AlertDialog(
            onDismissRequest = { pendingPromotionMove = null },
            title = { Text("Promotion du pion") },
            text = { Text("Choisissez la pièce :") },
            confirmButton = {},
            dismissButton = {
                Row {
                    listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { type ->
                        TextButton(onClick = {
                            onMovePlayed(Move(from, to, com.smchess.app.chess.MoveFlag.PROMOTION, type))
                            pendingPromotionMove = null
                        }) {
                            Text(pieceSymbol(type, localPlaysColor), fontSize = 22.sp)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun BoardCanvas(engine: ChessEngine, selected: Square?, legalTargets: List<Move>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cell = size.width / 8f
        for (row in 0..7) {
            for (col in 0..7) {
                val isLight = (row + col) % 2 == 0
                drawRect(
                    color = if (isLight) BoardLight else BoardDark,
                    topLeft = Offset(col * cell, row * cell),
                    size = androidx.compose.ui.geometry.Size(cell, cell)
                )
            }
        }
        selected?.let {
            drawRect(
                color = Color(0xFF25D366).copy(alpha = 0.5f),
                topLeft = Offset(it.col * cell, it.row * cell),
                size = androidx.compose.ui.geometry.Size(cell, cell)
            )
        }
        for (move in legalTargets) {
            drawCircle(
                color = Color(0xFF25D366).copy(alpha = 0.6f),
                radius = cell * 0.15f,
                center = Offset(move.to.col * cell + cell / 2, move.to.row * cell + cell / 2)
            )
        }
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = engine.board[row][col] ?: continue
                val symbol = pieceSymbol(piece.type, piece.color)
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        textSize = cell * 0.65f
                        textAlign = android.graphics.Paint.Align.CENTER
                        color = if (piece.color == PieceColor.WHITE) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        isAntiAlias = true
                    }
                    val cx = col * cell + cell / 2f
                    val cy = row * cell + cell / 2f - (paint.ascent() + paint.descent()) / 2f
                    canvas.nativeCanvas.drawText(symbol, cx, cy, paint)
                }
            }
        }
    }
}
