package top.isyuah.dev.yumuzk.mpipemvp.algo

class GomokuEngine : IAlgoEngine {
    override fun run(matrix: List<List<Int>>, context: Map<String, Any>?): AlgorithmResult {
        val rows = matrix.size
        val cols = matrix[0].size
        
        // Determine whose turn it is from context, default to BLACK (AI)
        val aiTurn = context?.get("gomoku_turn") as? Int ?: CellState.BLACK
        val opponentTurn = if (aiTurn == CellState.BLACK) CellState.RED else CellState.BLACK

        var bestScore = -1
        var bestMove: AlgoPoint? = null

        // Simple heuristic evaluation for each empty cell
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (matrix[r][c] == CellState.EMPTY) {
                    val score = evaluateMove(matrix, r, c, aiTurn, opponentTurn)
                    if (score > bestScore) {
                        bestScore = score
                        bestMove = AlgoPoint(c, r)
                    }
                }
            }
        }

        return if (bestMove != null) {
            AlgorithmResult(true, data = bestMove)
        } else {
            AlgorithmResult(false, message = "棋盘已满")
        }
    }

    private fun evaluateMove(matrix: List<List<Int>>, r: Int, c: Int, ai: Int, opponent: Int): Int {
        // Simple scoring based on consecutive pieces in 4 directions
        var score = 0
        val directions = arrayOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        
        for (d in directions) {
            score += countConsecutive(matrix, r, c, d.first, d.second, ai) * 10
            score += countConsecutive(matrix, r, c, d.first, d.second, opponent) * 8 // Defensive
        }
        return score
    }

    private fun countConsecutive(matrix: List<List<Int>>, r: Int, c: Int, dr: Int, dc: Int, piece: Int): Int {
        var count = 0
        // Check forward
        var nr = r + dr
        var nc = c + dc
        while (nr in matrix.indices && nc in matrix[0].indices && matrix[nr][nc] == piece) {
            count++
            nr += dr
            nc += dc
        }
        // Check backward
        nr = r - dr
        nc = c - dc
        while (nr in matrix.indices && nc in matrix[0].indices && matrix[nr][nc] == piece) {
            count++
            nr -= dr
            nc -= dc
        }
        return count
    }
}
