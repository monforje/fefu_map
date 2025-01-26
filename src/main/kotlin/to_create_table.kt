import java.sql.Connection
import kotlin.math.sqrt

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

// Функция для расчета веса между двумя точками
fun deductWeight(x1: Double, x2: Double, y1: Double, y2: Double): Double {
    return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
}