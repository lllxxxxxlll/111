package top.isyuah.dev.yumuzk.mpipemvp.algo

import java.util.*

/**
 * 迷宫生成 - 递归回溯算法 (Recursive Backtracker)
 * 适配 9x9 网格，生成具有墙壁感的迷宫
 */
class MazeGenEngine : IAlgoEngine {
    override fun run(matrix: List<List<Int>>, context: Map<String, Any>?): AlgorithmResult {
        val rows = matrix.size
        val cols = matrix[0].size
        // 初始全部设为墙 (BLACK)
        val maze = MutableList(rows) { MutableList(cols) { CellState.BLACK } }

        // 从 (1,1) 开始生成，步长为 2 以留出墙壁空间
        generate(1, 1, maze)

        return AlgorithmResult(true, data = maze)
    }

    private fun generate(r: Int, c: Int, maze: MutableList<MutableList<Int>>) {
        maze[r][c] = CellState.EMPTY
        
        val dirs = mutableListOf(
            Pair(0, 2), Pair(0, -2), Pair(2, 0), Pair(-2, 0)
        ).apply { shuffle() }

        for (d in dirs) {
            val nr = r + d.first
            val nc = c + d.second
            
            if (nr in 1 until maze.size - 1 && nc in 1 until maze[0].size - 1) {
                if (maze[nr][nc] == CellState.BLACK) {
                    // 打通中间的墙
                    maze[r + d.first / 2][c + d.second / 2] = CellState.EMPTY
                    generate(nr, nc, maze)
                }
            }
        }
    }
}
