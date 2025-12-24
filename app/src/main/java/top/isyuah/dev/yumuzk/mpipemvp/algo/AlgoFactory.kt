package top.isyuah.dev.yumuzk.mpipemvp.algo

object AlgoFactory {
    fun createEngine(type: String): IAlgoEngine? {
        return when (type) {
            "A*路径搜索", "PATH_ASTAR" -> AStarEngine()
            "五子棋AI", "GOMOKU_AI" -> GomokuEngine()
            "生命游戏", "GAME_OF_LIFE" -> GameOfLifeEngine()
            "迷宫生成", "MAZE_GEN_PRIMS" -> MazeGenEngine()
            else -> null
        }
    }
}
