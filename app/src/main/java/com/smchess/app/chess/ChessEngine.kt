package com.smchess.app.chess

/**
 * Moteur d'échecs avec validation complète des règles officielles.
 * Représentation : board[row][col], row 0 = rangée 8, row 7 = rangée 1.
 */
class ChessEngine {

    val board: Array<Array<Piece?>> = Array(8) { arrayOfNulls(8) }
    var sideToMove: PieceColor = PieceColor.WHITE
    var enPassantTarget: Square? = null

    var whiteCanCastleKingSide = true
    var whiteCanCastleQueenSide = true
    var blackCanCastleKingSide = true
    var blackCanCastleQueenSide = true

    val moveHistory = mutableListOf<Move>()

    init {
        setupStartPosition()
    }

    private fun setupStartPosition() {
        val backRank = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        )
        for (col in 0..7) {
            board[0][col] = Piece(backRank[col], PieceColor.BLACK)
            board[1][col] = Piece(PieceType.PAWN, PieceColor.BLACK)
            board[6][col] = Piece(PieceType.PAWN, PieceColor.WHITE)
            board[7][col] = Piece(backRank[col], PieceColor.WHITE)
        }
    }

    fun pieceAt(sq: Square): Piece? = board[sq.row][sq.col]

    private fun opposite(c: PieceColor) = if (c == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE

    /** Toutes les cases attaquées par la couleur donnée (sans tenir compte de la mise en échec de son propre roi) */
    fun isSquareAttackedBy(target: Square, byColor: PieceColor): Boolean {
        for (row in 0..7) {
            for (col in 0..7) {
                val p = board[row][col] ?: continue
                if (p.color != byColor) continue
                val from = Square(row, col)
                if (attacksSquare(p, from, target)) return true
            }
        }
        return false
    }

    private fun attacksSquare(piece: Piece, from: Square, target: Square): Boolean {
        val dr = target.row - from.row
        val dc = target.col - from.col
        return when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) -1 else 1
                dr == dir && (dc == 1 || dc == -1)
            }
            PieceType.KNIGHT -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2)
            PieceType.KING -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1 && (dr != 0 || dc != 0)
            PieceType.BISHOP -> Math.abs(dr) == Math.abs(dc) && dr != 0 && pathClear(from, target)
            PieceType.ROOK -> (dr == 0 || dc == 0) && (dr != 0 || dc != 0) && pathClear(from, target)
            PieceType.QUEEN -> {
                val straight = (dr == 0 || dc == 0) && (dr != 0 || dc != 0)
                val diag = Math.abs(dr) == Math.abs(dc) && dr != 0
                (straight || diag) && pathClear(from, target)
            }
        }
    }

    private fun pathClear(from: Square, to: Square): Boolean {
        val dr = Integer.signum(to.row - from.row)
        val dc = Integer.signum(to.col - from.col)
        var r = from.row + dr
        var c = from.col + dc
        while (r != to.row || c != to.col) {
            if (board[r][c] != null) return false
            r += dr
            c += dc
        }
        return true
    }

    fun findKing(color: PieceColor): Square {
        for (row in 0..7) for (col in 0..7) {
            val p = board[row][col]
            if (p != null && p.type == PieceType.KING && p.color == color) return Square(row, col)
        }
        throw IllegalStateException("Roi $color introuvable")
    }

    fun isInCheck(color: PieceColor): Boolean = isSquareAttackedBy(findKing(color), opposite(color))

    /** Coups pseudo-légaux (sans filtrer ceux qui laissent le roi en échec) */
    private fun pseudoMoves(from: Square): List<Move> {
        val piece = pieceAt(from) ?: return emptyList()
        val moves = mutableListOf<Move>()
        val color = piece.color

        fun addIfValidCapture(to: Square) {
            if (!to.isValid()) return
            val target = pieceAt(to)
            if (target == null || target.color != color) moves.add(Move(from, to))
        }

        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (color == PieceColor.WHITE) -1 else 1
                val startRow = if (color == PieceColor.WHITE) 6 else 1
                val promoRow = if (color == PieceColor.WHITE) 0 else 7
                val oneStep = Square(from.row + dir, from.col)
                if (oneStep.isValid() && pieceAt(oneStep) == null) {
                    if (oneStep.row == promoRow) {
                        for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                            moves.add(Move(from, oneStep, MoveFlag.PROMOTION, promo))
                        }
                    } else {
                        moves.add(Move(from, oneStep))
                        val twoStep = Square(from.row + 2 * dir, from.col)
                        if (from.row == startRow && pieceAt(twoStep) == null) {
                            moves.add(Move(from, twoStep, MoveFlag.DOUBLE_PAWN))
                        }
                    }
                }
                for (dc in listOf(-1, 1)) {
                    val capSq = Square(from.row + dir, from.col + dc)
                    if (!capSq.isValid()) continue
                    val target = pieceAt(capSq)
                    if (target != null && target.color != color) {
                        if (capSq.row == promoRow) {
                            for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                moves.add(Move(from, capSq, MoveFlag.PROMOTION, promo))
                            }
                        } else {
                            moves.add(Move(from, capSq))
                        }
                    } else if (capSq == enPassantTarget) {
                        moves.add(Move(from, capSq, MoveFlag.EN_PASSANT))
                    }
                }
            }
            PieceType.KNIGHT -> {
                val deltas = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
                for ((dr, dc) in deltas) addIfValidCapture(Square(from.row + dr, from.col + dc))
            }
            PieceType.KING -> {
                for (dr in -1..1) for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    addIfValidCapture(Square(from.row + dr, from.col + dc))
                }
                // Roque
                val row = from.row
                val canKingSide = if (color == PieceColor.WHITE) whiteCanCastleKingSide else blackCanCastleKingSide
                val canQueenSide = if (color == PieceColor.WHITE) whiteCanCastleQueenSide else blackCanCastleQueenSide
                if (!isInCheck(color)) {
                    if (canKingSide && board[row][5] == null && board[row][6] == null &&
                        !isSquareAttackedBy(Square(row, 5), opposite(color)) &&
                        !isSquareAttackedBy(Square(row, 6), opposite(color))
                    ) {
                        moves.add(Move(from, Square(row, 6), MoveFlag.CASTLE_KING_SIDE))
                    }
                    if (canQueenSide && board[row][1] == null && board[row][2] == null && board[row][3] == null &&
                        !isSquareAttackedBy(Square(row, 2), opposite(color)) &&
                        !isSquareAttackedBy(Square(row, 3), opposite(color))
                    ) {
                        moves.add(Move(from, Square(row, 2), MoveFlag.CASTLE_QUEEN_SIDE))
                    }
                }
            }
            PieceType.ROOK, PieceType.BISHOP, PieceType.QUEEN -> {
                val directions = when (piece.type) {
                    PieceType.ROOK -> listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                    PieceType.BISHOP -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
                    else -> listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, -1 to 1, 1 to -1, 1 to 1)
                }
                for ((dr, dc) in directions) {
                    var r = from.row + dr
                    var c = from.col + dc
                    while (Square(r, c).isValid()) {
                        val target = pieceAt(Square(r, c))
                        if (target == null) {
                            moves.add(Move(from, Square(r, c)))
                        } else {
                            if (target.color != color) moves.add(Move(from, Square(r, c)))
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
            }
        }
        return moves
    }

    /** Coups légaux pour la pièce en `from` (filtrés : ne laissent pas son propre roi en échec) */
    fun legalMoves(from: Square): List<Move> {
        val piece = pieceAt(from) ?: return emptyList()
        return pseudoMoves(from).filter { move ->
            val snapshot = snapshotState()
            applyMoveInternal(move)
            val stillLegal = !isInCheck(piece.color)
            restoreState(snapshot)
            stillLegal
        }
    }

    fun allLegalMoves(color: PieceColor): List<Move> {
        val result = mutableListOf<Move>()
        for (row in 0..7) for (col in 0..7) {
            val p = board[row][col]
            if (p != null && p.color == color) result.addAll(legalMoves(Square(row, col)))
        }
        return result
    }

    fun isCheckmate(color: PieceColor): Boolean = isInCheck(color) && allLegalMoves(color).isEmpty()
    fun isStalemate(color: PieceColor): Boolean = !isInCheck(color) && allLegalMoves(color).isEmpty()

    /** Joue le coup pour de vrai (met à jour sideToMove, roque, en passant, historique) */
    fun applyMove(move: Move) {
        applyMoveInternal(move)
        sideToMove = opposite(sideToMove)
        moveHistory.add(move)
    }

    private data class StateSnapshot(
        val board: Array<Array<Piece?>>,
        val enPassantTarget: Square?,
        val wks: Boolean, val wqs: Boolean, val bks: Boolean, val bqs: Boolean
    )

    private fun snapshotState(): StateSnapshot {
        val copy = Array(8) { r -> Array(8) { c -> board[r][c] } }
        return StateSnapshot(copy, enPassantTarget, whiteCanCastleKingSide, whiteCanCastleQueenSide, blackCanCastleKingSide, blackCanCastleQueenSide)
    }

    private fun restoreState(s: StateSnapshot) {
        for (r in 0..7) for (c in 0..7) board[r][c] = s.board[r][c]
        enPassantTarget = s.enPassantTarget
        whiteCanCastleKingSide = s.wks
        whiteCanCastleQueenSide = s.wqs
        blackCanCastleKingSide = s.bks
        blackCanCastleQueenSide = s.bqs
    }

    private fun applyMoveInternal(move: Move) {
        val piece = pieceAt(move.from) ?: return
        var newEnPassant: Square? = null

        when (move.flag) {
            MoveFlag.EN_PASSANT -> {
                val capturedRow = move.from.row
                board[capturedRow][move.to.col] = null
            }
            MoveFlag.DOUBLE_PAWN -> {
                newEnPassant = Square((move.from.row + move.to.row) / 2, move.from.col)
            }
            MoveFlag.CASTLE_KING_SIDE -> {
                val row = move.from.row
                board[row][5] = board[row][7]
                board[row][7] = null
            }
            MoveFlag.CASTLE_QUEEN_SIDE -> {
                val row = move.from.row
                board[row][3] = board[row][0]
                board[row][0] = null
            }
            else -> {}
        }

        board[move.to.row][move.to.col] = if (move.flag == MoveFlag.PROMOTION && move.promotion != null)
            Piece(move.promotion, piece.color) else piece
        board[move.from.row][move.from.col] = null
        enPassantTarget = newEnPassant

        // Mise à jour des droits au roque
        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) {
                whiteCanCastleKingSide = false; whiteCanCastleQueenSide = false
            } else {
                blackCanCastleKingSide = false; blackCanCastleQueenSide = false
            }
        }
        if (piece.type == PieceType.ROOK) {
            if (move.from == Square(7, 7)) whiteCanCastleKingSide = false
            if (move.from == Square(7, 0)) whiteCanCastleQueenSide = false
            if (move.from == Square(0, 7)) blackCanCastleKingSide = false
            if (move.from == Square(0, 0)) blackCanCastleQueenSide = false
        }
        if (move.to == Square(7, 7)) whiteCanCastleKingSide = false
        if (move.to == Square(7, 0)) whiteCanCastleQueenSide = false
        if (move.to == Square(0, 7)) blackCanCastleKingSide = false
        if (move.to == Square(0, 0)) blackCanCastleQueenSide = false
    }

    /** Rejoue une partie entière à partir d'une liste de coups (reconstitution depuis l'historique SMS) */
    fun replay(moves: List<Move>) {
        for (m in moves) {
            val legal = legalMoves(m.from).find { it.to == m.to && it.promotion == m.promotion }
            if (legal != null) applyMove(legal)
        }
    }
}
