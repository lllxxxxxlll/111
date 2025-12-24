package top.isyuah.dev.yumuzk.mpipemvp.algo

/**
 * 对应 Python 版的 CellState
 */
object CellState {
    const val EMPTY = 0
    const val BLACK = 1  // 障碍 / 黑棋
    const val RED = 2    // 起点/终点 / 红棋
    const val BLUE = 3   // 蓝棋
}

/**
 * 对应 Python 版的 AlgorithmResult
 */
data class AlgorithmResult(
    val success: Boolean,
    val data: Any? = null,
    val message: String = ""
)

/**
 * 坐标点对象
 */
data class AlgoPoint(val col: Int, val row: Int)

/**
 * 算法类型枚举
 */
enum class AlgoType {
    GOMOKU_AI,
    MAZE_GEN_PRIMS,
    PATH_ASTAR,
    MAZE_SOLVE_BFS,
    MAZE_SOLVE_DFS,
    GAME_OF_LIFE
}

interface IAlgoEngine {
    fun run(matrix: List<List<Int>>, context: Map<String, Any>? = null): AlgorithmResult
}
