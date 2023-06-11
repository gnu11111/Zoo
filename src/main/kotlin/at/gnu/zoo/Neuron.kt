package at.gnu.zoo

sealed class Neuron(private val initialPower: Int = 0) {

    open val power: Int
        get() = _power.coerceIn(0, 32)

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
        get() = _power.coerceIn(-32, 32)

    override fun toString() = "InnerNeuron$id:$power"
}

sealed class Sensor : Neuron() {
    companion object {
        private val sensors = listOf(Oszillator, Age, DistanceFromEastBorder, DistanceFromWestBorder,
            DistanceFromNorthBorder, DistanceFromSouthBorder, PopulationForward, WallForward)
        fun of(index: Int): Sensor =
            sensors[index % sensors.size]
    }

    abstract fun calculatePower(world: World, blob: Blob): Int
}

sealed class Action : Neuron() {
    companion object {
        private val actions = listOf(Move(), MoveRandomly(), MoveEast(), MoveWest(), MoveNorth(), MoveSouth(),
            TurnRight(), TurnLeft(), TurnAround(), TurnRandomly())
        fun of(index: Int): Action =
            actions[index % actions.size].clone()
    }

    abstract fun apply(world: World, blob: Blob)
    abstract fun clone(): Action

    fun move(blob: Blob, world: World) {
        val x = (blob.position.x + blob.heading.dx).coerceIn(0, world.context.size.maxX - 1)
        val y = (blob.position.y + blob.heading.dy).coerceIn(0, world.context.size.maxY - 1)
        if (world.isEmpty(x, y)) {
            world.free(blob.position.x, blob.position.y)
            world.occupy(x, y)
            blob.position.x = x
            blob.position.y = y
        }
    }
}
