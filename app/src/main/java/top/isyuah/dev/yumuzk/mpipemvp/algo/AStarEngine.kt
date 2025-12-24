package top.isyuah.dev.yumuzk.mpipemvp.algo

import java.util.*
import kotlin.math.abs

class AStarEngine : IAlgoEngine {
    override fun run(matrix: List<List<Int>>, context: Map<String, Any>?): AlgorithmResult {
        val rows = matrix.size
        val cols = matrix[0].size
        var start: AlgoPoint? = null
        val goals = mutableListOf<AlgoPoint>()

        // 找到起点和所有终点 (RED)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (matrix[r][c] == CellState.RED) {
                    if (start == null) start = AlgoPoint(c, r)
                    else goals.add(AlgoPoint(c, r))
                }
            }
        }

        if (start == null || goals.isEmpty()) {
            return AlgorithmResult(false, message = "未找到起点或终点 (红色格子)")
        }

        val goal = goals[0] // 简单起见，取第一个终点
        val path = findPath(matrix, start, goal)
        
        return if (path != null) {
            AlgorithmResult(true, data = path)
        } else {
            AlgorithmResult(false, message = "无法找到路径")
        }
    }

    private fun findPath(matrix: List<List<Int>>, start: AlgoPoint, goal: AlgoPoint): List<AlgoPoint>? {
        val openSet = PriorityQueue<Node>(compareBy { it.f })
        val closedSet = mutableSetOf<AlgoPoint>()
        val nodes = mutableMapOf<AlgoPoint, Node>()

        val startNode = Node(start, 0.0, heuristic(start, goal))
        openSet.add(startNode)
        nodes[start] = startNode

        while (openSet.isNotEmpty()) {
            val current = openSet.poll() ?: break
            if (current.point == goal) {
                return reconstructPath(current)
            }

            closedSet.add(current.point)

            for (neighborPt in getNeighbors(current.point, matrix)) {
                if (neighborPt in closedSet) continue
                if (matrix[neighborPt.row][neighborPt.col] == CellState.BLACK) continue

                val tentativeG = current.g + 1.0
                val neighborNode = nodes.getOrPut(neighborPt) { Node(neighborPt) }

                if (tentativeG < neighborNode.g) {
                    neighborNode.parent = current
                    neighborNode.g = tentativeG
                    neighborNode.h = heuristic(neighborPt, goal)
                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode)
                    }
                }
            }
        }
        return null
    }

    private fun heuristic(p1: AlgoPoint, p2: AlgoPoint): Double =
        (abs(p1.col - p2.col) + abs(p1.row - p2.row)).toDouble()

    private fun getNeighbors(p: AlgoPoint, matrix: List<List<Int>>): List<AlgoPoint> {
        val neighbors = mutableListOf<AlgoPoint>()
        val directions = arrayOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        for (d in directions) {
            val nc = p.col + d.first
            val nr = p.row + d.second
            if (nr in matrix.indices && nc in matrix[0].indices) {
                neighbors.add(AlgoPoint(nc, nr))
            }
        }
        return neighbors
    }

    private fun reconstructPath(node: Node): List<AlgoPoint> {
        val path = mutableListOf<AlgoPoint>()
        var curr: Node? = node
        while (curr != null) {
            path.add(0, curr.point)
            curr = curr.parent
        }
        return path
    }

    private class Node(
        val point: AlgoPoint,
        var g: Double = Double.MAX_VALUE,
        var h: Double = 0.0,
        var parent: Node? = null
    ) {
        val f: Double get() = g + h
    }
}
