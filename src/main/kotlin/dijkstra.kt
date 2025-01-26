import java.sql.Connection
import java.sql.DriverManager

// Класс для представления узла графа
data class Node(
    val id: Int,
    val name: String,
    val x: Double,
    val y: Double,
    val floor: Int
)

// Класс для представления ребра графа
data class Edge(
    val id: Int,
    val first: Node,
    val second: Node,
    val weight: Double
)

fun main() {
    val dbUrl = "jdbc:sqlite:paths.db"

    try {
        DriverManager.getConnection(dbUrl).use { connection ->
            println("Успешное подключение к базе данных SQLite.")

//            createTables(connection)
//            clearTables(connection)
//            insertData(connection)

            val nodes = loadNodes(connection)
            val nodesById = nodes.associateBy { it.id }
            val edges = loadEdges(connection, nodesById)

            val graph = Graph(nodes, edges)

            println("Введите имя начального узла:")
            val start = readlnOrNull()?.trim()

            println("Введите имя целевого узла:")
            val target = readlnOrNull()?.trim()

            if (start.isNullOrEmpty() || target.isNullOrEmpty()) {
                println("Ошибка: Имя узла не может быть пустым.")
                return
            }

            val startNode = nodes.find { it.name == start }
            val targetNode = nodes.find { it.name == target }

            if (startNode == null || targetNode == null) {
                println("Ошибка: Один из узлов не найден в графе.")
                return
            }

            val (distance, path) = dijkstraWithPath(graph, startNode, targetNode)
            println("Кратчайший путь от узла ${startNode.name} до узла ${targetNode.name}:")
            println("Расстояние: $distance")
            println("Путь: ${path.joinToString(" -> ") { "${it.id}" }}")
        }
    } catch (e: Exception) {
        println("Ошибка при работе с базой данных: ${e.message}")
    }
}

// Загрузка узлов из базы данных
fun loadNodes(connection: Connection): List<Node> {
    val query = "SELECT * FROM nodes"
    val nodes = mutableListOf<Node>()
    connection.createStatement().use { statement ->
        val resultSet = statement.executeQuery(query)
        while (resultSet.next()) {
            nodes.add(
                Node(
                    resultSet.getInt("id"),
                    resultSet.getString("name"),
                    resultSet.getDouble("latitude"),
                    resultSet.getDouble("longitude"),
                    resultSet.getInt("floor")
                )
            )
        }
    }
    return nodes
}

// Загрузка ребер из базы данных
fun loadEdges(connection: Connection, nodesById: Map<Int, Node>): List<Edge> {
    val query = "SELECT * FROM edges"
    val edges = mutableListOf<Edge>()
    connection.createStatement().use { statement ->
        val resultSet = statement.executeQuery(query)
        while (resultSet.next()) {
            val fromNode = nodesById[resultSet.getInt("from_node")]
            val toNode = nodesById[resultSet.getInt("to_node")]
            if (fromNode != null && toNode != null) {
                edges.add(
                    Edge(
                        resultSet.getInt("id"),
                        fromNode,
                        toNode,
                        resultSet.getDouble("weight")
                    )
                )
            }
        }
    }
    return edges
}

// Класс для представления графа
class Graph(nodes: List<Node>, edges: List<Edge>) {
    private val adjacencyList: MutableMap<Node, MutableList<Pair<Node, Double>>> = mutableMapOf()

    init {
        nodes.forEach { node -> adjacencyList[node] = mutableListOf() }
        edges.forEach { edge ->
            adjacencyList[edge.first]?.add(edge.second to edge.weight)
            adjacencyList[edge.second]?.add(edge.first to edge.weight)
        }
    }

    fun getNodes(): List<Node> = adjacencyList.keys.toList()

    fun getNeighbors(node: Node): List<Pair<Node, Double>> = adjacencyList[node] ?: emptyList()
}

// Реализация алгоритма Дейкстры для нахождения кратчайшего пути
fun dijkstraWithPath(graph: Graph, startNode: Node, targetNode: Node): Pair<Double, List<Node>> {
    val distances = mutableMapOf<Node, Double>().apply {
        graph.getNodes().forEach { node -> this[node] = Double.MAX_VALUE }
        this[startNode] = 0.0
    }

    val visited = mutableSetOf<Node>()
    val previousNodes = mutableMapOf<Node, Node?>().apply {
        graph.getNodes().forEach { node -> this[node] = null }
    }

    val priorityQueue = java.util.PriorityQueue<Pair<Node, Double>>(compareBy { it.second })
    priorityQueue.add(startNode to 0.0)

    while (priorityQueue.isNotEmpty()) {
        val (currentNode, currentDistance) = priorityQueue.poll()

        if (currentNode in visited) continue

        visited.add(currentNode)

        if (currentNode == targetNode) break

        graph.getNeighbors(currentNode).forEach { (neighbor, weight) ->
            if (neighbor !in visited) {
                val newDistance = currentDistance + weight
                if (newDistance < distances[neighbor]!!) {
                    distances[neighbor] = newDistance
                    previousNodes[neighbor] = currentNode
                    priorityQueue.add(neighbor to newDistance)
                }
            }
        }
    }

    val path = mutableListOf<Node>()
    var currentNode: Node? = targetNode
    while (currentNode != null) {
        path.add(currentNode)
        currentNode = previousNodes[currentNode]
    }
    path.reverse()

    return distances[targetNode]!! to path
}