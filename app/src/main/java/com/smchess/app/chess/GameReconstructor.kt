package com.smchess.app.chess

import com.smchess.app.data.SmsMessage

object GameReconstructor {

    /**
     * Analyse les messages d'une conversation (triés du plus ancien au plus récent)
     * et reconstitue l'état actuel de la partie + un ChessEngine rejoué.
     */
    fun reconstruct(messages: List<SmsMessage>): Pair<GameState, ChessEngine> {
        val engine = ChessEngine()
        var phase = GamePhase.NONE
        var challengerIsLocalUser = false
        val moves = mutableListOf<Move>()
        var resultText: String? = null

        for (msg in messages) {
            val body = msg.body.trim()
            when {
                body == GameProtocol.CHALLENGE -> {
                    phase = GamePhase.CHALLENGE_PENDING
                    challengerIsLocalUser = msg.isOutgoing
                    moves.clear()
                    resultText = null
                }
                body == GameProtocol.ACCEPT -> {
                    if (phase == GamePhase.CHALLENGE_PENDING) {
                        phase = GamePhase.IN_PROGRESS
                    }
                }
                body == GameProtocol.RESIGN -> {
                    if (phase == GamePhase.IN_PROGRESS) {
                        phase = GamePhase.FINISHED
                        // Celui qui abandonne perd. L'expéditeur du message d'abandon est le perdant.
                        val loserIsChallenger = msg.isOutgoing == challengerIsLocalUser
                        resultText = if (loserIsChallenger) "Les blancs gagnent (abandon)" else "Les noirs gagnent (abandon)"
                    }
                }
                body == GameProtocol.DRAW_ACCEPT -> {
                    if (phase == GamePhase.IN_PROGRESS) {
                        phase = GamePhase.FINISHED
                        resultText = "Partie nulle (accord mutuel)"
                    }
                }
                body == GameProtocol.DRAW_OFFER -> { /* géré côté UI pour affichage du bouton, pas d'effet sur l'état du jeu ici */ }
                GameProtocol.decodeMove(body) != null -> {
                    if (phase == GamePhase.IN_PROGRESS) {
                        GameProtocol.decodeMove(body)?.let { moves.add(it) }
                    }
                }
            }
        }

        engine.replay(moves)

        if (phase == GamePhase.IN_PROGRESS) {
            if (engine.isCheckmate(engine.sideToMove)) {
                phase = GamePhase.FINISHED
                val winnerIsBlack = engine.sideToMove == PieceColor.WHITE
                resultText = if (winnerIsBlack) "Les noirs gagnent (échec et mat)" else "Les blancs gagnent (échec et mat)"
            } else if (engine.isStalemate(engine.sideToMove)) {
                phase = GamePhase.FINISHED
                resultText = "Partie nulle (pat)"
            }
        }

        return Pair(GameState(phase, challengerIsLocalUser, moves, resultText), engine)
    }

    /** true si le dernier message non répondu est une offre de nulle en attente */
    fun isDrawOfferPending(messages: List<SmsMessage>): Boolean {
        val lastRelevant = messages.lastOrNull {
            it.body.trim() == GameProtocol.DRAW_OFFER || it.body.trim() == GameProtocol.DRAW_ACCEPT ||
                GameProtocol.decodeMove(it.body) != null || it.body.trim() == GameProtocol.RESIGN
        }
        return lastRelevant != null && lastRelevant.body.trim() == GameProtocol.DRAW_OFFER
    }
}
