package com.smchess.app.chess

object GameProtocol {
    const val CHALLENGE = "~(defi)~"
    const val ACCEPT = "~(accept)~"
    const val RESIGN = "~(abandon)~"
    const val DRAW_OFFER = "~(nulle)~"
    const val DRAW_ACCEPT = "~(nulleok)~"

    private val movePattern = Regex("^~\\(([A-Ha-h][1-8][A-Ha-h][1-8])([QRBN]?)\\)~$")

    fun isProtocolMessage(body: String?): Boolean {
        if (body == null) return false
        val trimmed = body.trim()
        return trimmed == CHALLENGE || trimmed == ACCEPT || trimmed == RESIGN ||
            trimmed == DRAW_OFFER || trimmed == DRAW_ACCEPT || movePattern.matches(trimmed)
    }

    fun encodeMove(move: Move): String = "~(${move.toCode()})~"

    fun decodeMove(body: String): Move? {
        val match = movePattern.matchEntire(body.trim()) ?: return null
        val coords = match.groupValues[1].uppercase()
        val promoChar = match.groupValues[2]
        val from = Square.fromCode(coords.substring(0, 2)) ?: return null
        val to = Square.fromCode(coords.substring(2, 4)) ?: return null
        val promotion = when (promoChar) {
            "Q" -> PieceType.QUEEN
            "R" -> PieceType.ROOK
            "B" -> PieceType.BISHOP
            "N" -> PieceType.KNIGHT
            else -> null
        }
        val flag = if (promotion != null) MoveFlag.PROMOTION else MoveFlag.NORMAL
        return Move(from, to, flag, promotion)
    }
}

/** État d'une partie reconstitué à partir des SMS échangés dans une conversation */
enum class GamePhase { NONE, CHALLENGE_PENDING, IN_PROGRESS, FINISHED }

data class GameState(
    val phase: GamePhase,
    val challengerIsLocalUser: Boolean = false, // true si c'est nous qui avons envoyé le défi (= joue les noirs)
    val moves: List<Move> = emptyList(),
    val resultText: String? = null
)
