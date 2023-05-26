package at.gnu.zoo

import kotlin.math.max
import kotlin.math.min

sealed class Neuron(private val initialPower: Int = 0) {

    open val power: Int
        get() = max(0, min(_power, 32))

    protected var _power: Int = initialPower

    fun reset() {
        _power = initialPower
    }

    fun addPower(amount: Int): Int {
        _power += amount
        return power
    }

    fun setAndGetPower(amount: Int): Int {
        _power = amount
        return power
    }
}

class InnerNeuron(private val id: Int = 0) : Neuron(1) {

    override val power: Int
        get() = max(-32, min(_power, 32))

    override fun toString() = "InnerNeuron$id:$power"
}

sealed class Sensor : Neuron() {
    companion object {
        private val sensors = listOf(Oszillator, Age, DistanceFromEastBorder, DistanceFromWestBorder,
            DistanceFromNorthBorder, DistanceFromSouthBorder)
        fun of(index: Int): Sensor =
            sensors[index % sensors.size]
    }

    abstract fun calculatePower(world: World, blob: Blob): Int
}

sealed class Action : Neuron() {
    companion object {
        private val actions = listOf(Move(), MoveRandomly(), MoveEast(), MoveWest(), MoveNorth(), MoveSouth())
        fun of(index: Int): Action =
            actions[index % actions.size].clone()
    }

    abstract fun apply(world: World, blob: Blob)
    abstract fun clone(): Action

    fun move(blob: Blob, world: World) {
        val x = max(0, min(blob.position.x + blob.heading.dx, world.context.size.maxX - 1))
        val y = max(0, min(blob.position.y + blob.heading.dy, world.context.size.maxY - 1))
        if (world.isEmpty(x, y)) {
            world.free(blob.position.x, blob.position.y)
            world.occupy(x, y)
            blob.position.x = x
            blob.position.y = y
        }
    }
}
