import java.sql.Connection
import java.sql.DriverManager
import kotlin.math.sqrt

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

// Функция для расчета веса между двумя точками
fun deductWeight(x1: Double, x2: Double, y1: Double, y2: Double): Double {
    return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
}

fun main() {
    val dbUrl = "jdbc:sqlite:database.db"

    try {
        DriverManager.getConnection(dbUrl).use { connection ->
            println("Успешное подключение к базе данных SQLite.")

            createTables(connection)
            clearTables(connection)
            insertData(connection)

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
            println("Путь: ${path.joinToString(" -> ") { it.name }}")
        }
    } catch (e: Exception) {
        println("Ошибка при работе с базой данных: ${e.message}")
    }
}

// Функция для создания таблиц
fun createTables(connection: Connection) {
    val createNodesTable = """
        CREATE TABLE IF NOT EXISTS nodes (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            floor INTEGER NOT NULL
        );
    """.trimIndent()

    val createEdgesTable = """
        CREATE TABLE IF NOT EXISTS edges (
            id INTEGER PRIMARY KEY,
            from_node INTEGER NOT NULL,
            to_node INTEGER NOT NULL,
            weight REAL NOT NULL,
            FOREIGN KEY(from_node) REFERENCES nodes(id) ON DELETE CASCADE,
            FOREIGN KEY(to_node) REFERENCES nodes(id) ON DELETE CASCADE
        );
    """.trimIndent()

    connection.createStatement().use { statement ->
        statement.execute(createNodesTable)
        statement.execute(createEdgesTable)
        println("Таблицы 'nodes' и 'edges' успешно созданы.")
    }
}

// Функция для очистки таблиц
fun clearTables(connection: Connection) {
    connection.createStatement().use { statement ->
        statement.execute("DELETE FROM edges;")
        statement.execute("DELETE FROM nodes;")
        println("Таблицы успешно очищены.")
    }
}

// Функция для вставки данных в таблицы
fun insertData(connection: Connection) {
    val insertNodeQuery = """
        INSERT INTO nodes (id, name, latitude, longitude, floor) VALUES (?, ?, ?, ?, ?)
    """.trimIndent()

    val insertEdgeQuery = """
        INSERT INTO edges (id, from_node, to_node, weight) VALUES (?, ?, ?, ?)
    """.trimIndent()

    connection.prepareStatement(insertNodeQuery).use { statement ->
        val nodes = listOf(
            listOf(1, "D1", 1.0, 3.0, 1),
            listOf(2, "Коридор 1", 3.5, 6.0, 1),
            listOf(3, "D2", 3.5, 10.0, 1),
            listOf(4, "Коридор 2", 7.5, 6.0, 1),
            listOf(5, "D3", 7.5, 10.0, 1),
            listOf(6, "D4", 11.0, 6.0, 1)
        )

        for (node in nodes) {
            statement.setInt(1, node[0] as Int)
            statement.setString(2, node[1] as String)
            statement.setDouble(3, node[2] as Double)
            statement.setDouble(4, node[3] as Double)
            statement.setInt(5, node[4] as Int)
            statement.executeUpdate()
        }

        println("Данные успешно добавлены в таблицу 'nodes'.")
    }

    connection.prepareStatement(insertEdgeQuery).use { statement ->
        val nodes = listOf(
            Node(1, "D1", 1.0, 3.0, 1),
            Node(2, "Коридор 1", 3.5, 6.0, 1),
            Node(3, "D2", 3.5, 10.0, 1),
            Node(4, "Коридор 2", 7.5, 6.0, 1),
            Node(5, "D3", 7.5, 10.0, 1),
            Node(6, "D4", 11.0, 6.0, 1)
        )

        val nodesById = nodes.associateBy { it.id }

        val edges = listOf(
            Edge(1, nodesById[1]!!, nodesById[2]!!, deductWeight(nodesById[1]!!.x, nodesById[2]!!.x, nodesById[1]!!.y, nodesById[2]!!.y)),
            Edge(2, nodesById[2]!!, nodesById[3]!!, deductWeight(nodesById[2]!!.x, nodesById[3]!!.x, nodesById[2]!!.y, nodesById[3]!!.y)),
            Edge(3, nodesById[2]!!, nodesById[4]!!, deductWeight(nodesById[2]!!.x, nodesById[4]!!.x, nodesById[2]!!.y, nodesById[4]!!.y)),
            Edge(4, nodesById[4]!!, nodesById[5]!!, deductWeight(nodesById[4]!!.x, nodesById[5]!!.x, nodesById[4]!!.y, nodesById[5]!!.y)),
            Edge(5, nodesById[4]!!, nodesById[6]!!, deductWeight(nodesById[4]!!.x, nodesById[6]!!.x, nodesById[4]!!.y, nodesById[6]!!.y))
        )

        for ((index, edge) in edges.withIndex()) {
            statement.setInt(1, index + 1)
            statement.setInt(2, edge.first.id)
            statement.setInt(3, edge.second.id)
            statement.setDouble(4, edge.weight)
            statement.executeUpdate()
        }

        println("Данные успешно добавлены в таблицу 'edges'.")
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