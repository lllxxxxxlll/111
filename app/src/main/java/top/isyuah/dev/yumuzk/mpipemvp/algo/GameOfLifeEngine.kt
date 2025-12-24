package top.isyuah.dev.yumuzk.mpipemvp.algo

class GameOfLifeEngine : IAlgoEngine {
    override fun run(matrix: List<List<Int>>, context: Map<String, Any>?): AlgorithmResult {
        val rows = matrix.size
        val cols = matrix[0].size
        val nextMatrix = MutableList(rows) { MutableList(cols) { CellState.EMPTY } }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val neighbors = countAliveNeighbors(matrix, r, c)
                val isAlive = matrix[r][c] == CellState.BLACK // 我们约定黑色为活细胞

                if (isAlive) {
                    // 规则 1 & 3: 孤单或拥挤导致死亡
                    // 规则 2: 2-3个邻居继续存活
                    nextMatrix[r][c] = if (neighbors in 2..3) CellState.BLACK else CellState.EMPTY
                } else {
                    // 规则 4: 繁殖
                    if (neighbors == 3) nextMatrix[r][c] = CellState.BLACK
                }
            }
        }
        return AlgorithmResult(true, data = nextMatrix)
    }

    private fun countAliveNeighbors(matrix: List<List<Int>>, r: Int, c: Int): Int {
        var count = 0
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in matrix.indices && nc in matrix[0].indices && matrix[nr][nc] == CellState.BLACK) {
                    count++
                }
            }
        }
        return count
    }
}
