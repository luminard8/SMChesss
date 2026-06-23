package com.smchess.app.chess

enum class PieceColor { WHITE, BLACK }

enum class PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

data class Piece(val type: PieceType, val color: PieceColor)

/** row 0 = rangée 8 (haut de l'échiquier blanc), row 7 = rangée 1. col 0 = colonne a, col 7 = colonne h. */
data class Square(val row: Int, val col: Int) {
    fun isValid() = row in 0..7 && col in 0..7

    /** Notation type "A2" (colonne + rangée, 1=bas/rang1, 8=haut/rang8) */
    fun toCode(): String {
        val file = ('A' + col)
        val rank = 8 - row
        return "$file$rank"
    }

    companion object {
        fun fromCode(code: String): Square? {
            if (code.length != 2) return null
            val file = code[0].uppercaseChar()
            val rankChar = code[1]
            if (file < 'A' || file > 'H') return null
            val rank = rankChar.digitToIntOrNull() ?: return null
            if (rank < 1 || rank > 8) return null
            val col = file - 'A'
            val row = 8 - rank
            return Square(row, col)
        }
    }
}

enum class MoveFlag { NORMAL, DOUBLE_PAWN, EN_PASSANT, CASTLE_KING_SIDE, CASTLE_QUEEN_SIDE, PROMOTION }

data class Move(
    val from: Square,
    val to: Square,
    val flag: MoveFlag = MoveFlag.NORMAL,
    val promotion: PieceType? = null
) {
    /** Code SMS du coup, ex: "A2E4" ou "A7A8Q" pour une promotion en dame */
    fun toCode(): String {
        val base = from.toCode() + to.toCode()
        return if (promotion != null) {
            base + when (promotion) {
                PieceType.QUEEN -> "Q"
                PieceType.ROOK -> "R"
                PieceType.BISHOP -> "B"
                PieceType.KNIGHT -> "N"
                else -> ""
            }
        } else base
    }
}

enum class GameStatus { ONGOING, CHECKMATE, STALEMATE, DRAW_AGREED, RESIGNED }
