package at.gnu.zoo

import kotlin.random.Random

class Brain(val genom: String) {

    data class Connection(val from: Neuron, val to: Neuron, val weight: Int)

    private val connections: List<Connection>
    private val actions: Set<Action>
    private val innerNeurons = genom.drop(2).take(2).toInt(16)
    private val neurons = List(innerNeurons) { InnerNeuron(it) }

    init {
        connections = createActiveConnections()
        actions = connections.filter { it.to is Action }.map { it.to as Action }.toSet()
    }

    fun think(world: World, blob: Blob): List<Action> {
        neurons.forEach { it.reset() }
        actions.forEach { it.reset() }
        for (connection in connections) {
            val power = if (connection.from is Sensor)
                connection.from.calculatePower(world, blob)
            else
                connection.from.power
            connection.to.addPower((power * connection.weight) / 32)
        }
        return actions.filter { it.power > ACTION_THRESHOLD }
    }

    fun print() {
        println(genom)
        connections.forEach { println(it) }
    }

    private fun createActiveConnections(): List<Connection> {
        val allConnections = createAllConnections()
        val activeConnections = mutableListOf<Connection>()
        do {
            val moreActiveConnections = allConnections.filter {
                it !in activeConnections &&
                        (it.to is Action || it.to in activeConnections.map { connection -> connection.from })
            }
            activeConnections += moreActiveConnections
        } while (moreActiveConnections.isNotEmpty())
        return activeConnections.reversed().toList()
    }

    private fun createAllConnections(): List<Connection> {
        fun getTypeAndIndexOf(gene: String): Pair<Int, Int> {
            val index = gene.toInt(16)
            return if (index < 128) Pair(0, index) else Pair(1, index - 128)
        }

        fun getInnerNeuron(index: Int) =
            neurons[index % neurons.size]

        val allActions = mutableSetOf<Action>()
        fun getAction(toIndex: Int): Action {
            val action = Action.of(toIndex)
            val existingAction = allActions.firstOrNull { it.toString() == action.toString() }
            if (existingAction != null)
                return existingAction
            allActions += action
            return action
        }

        val allConnections = mutableListOf<Connection>()
        fun addConnection(from: Neuron, to: Neuron, weight: Int) {
            val existingConnection = allConnections.firstOrNull {
                it.from.toString() == from.toString() && it.to.toString() == to.toString()
            }
            if (existingConnection == null) {
                allConnections += Connection(from, to, weight)
                return
            }
            allConnections += Connection(from, to, existingConnection.weight + weight)
            allConnections -= existingConnection
        }

        genom.drop(4).chunked(GENE_SIZE) {
            val (fromType, fromIndex) = getTypeAndIndexOf(it.substring(0, 2))
            val (toType, toIndex) = getTypeAndIndexOf(it.substring(2, 4))
            val weight = it.substring(4, GENE_SIZE).toInt(16) - 128
            when {
                ((fromType == 0) && (toType == 0)) ->
                    addConnection(Sensor.of(fromIndex), getInnerNeuron(toIndex), weight)
                ((fromType == 1) && (toType == 0)) ->
                    addConnection(getInnerNeuron(fromIndex), getInnerNeuron(toIndex), weight)
                ((fromType == 0) && (toType == 1)) ->
                    addConnection(Sensor.of(fromIndex), getAction(toIndex), weight)
                ((fromType == 1) && (toType == 1)) ->
                    addConnection(getInnerNeuron(fromIndex), getAction(toIndex), weight)
            }
        }

        return allConnections
    }

    @Suppress("unused")
    private fun fastTanh(x: Float): Float {
        val x2 = x * x
        val a = x * (135135.0f + x2 * (17325.0f + x2 * (378.0f + x2)))
        val b = 135135.0f + x2 * (62370.0f + x2 * (3150.0f + x2 * 28.0f))
        return a / b
    }

    companion object {
        const val GENE_SIZE = 6
        const val ACTION_THRESHOLD = 15

        fun randomBrain(context: Context): Brain = Brain(randomGenom(context))

        private fun randomGenom(context: Context): String {
            val version = context.version.replace(".", "").take(2).padStart(2, '0')
            val neurons = (if (context.innerNeurons <= 0)
                Random.nextInt(32)
            else
                context.innerNeurons).toString(16).padStart(2, '0')
            val connections = (1..(context.genomSize * GENE_SIZE)).fold("") { acc, _ ->
                acc + Random.nextInt(16).toString(16)
            }
            return version + neurons + connections
        }
    }
}
