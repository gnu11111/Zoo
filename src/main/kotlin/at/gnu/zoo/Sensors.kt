package at.gnu.zoo

object Oszillator : Sensor() {

    override fun calculatePower(world: World, blob: Blob): Int =
        setAndGetPower(world.oszillator)

    override fun toString() = "Oszillator:$power"
}

object Age : Sensor() {

    override fun calculatePower(world: World, blob: Blob): Int =
        setAndGetPower((32 * world.age) / world.context.lifetime)

    override fun toString() = "Age:$power"
}

object DistanceFromEastBorder : Sensor() {

    override fun calculatePower(world: World, blob: Blob): Int =
        setAndGetPower((32 * (world.context.size.maxX - blob.position.x)) / world.context.size.maxX)

    override fun toString() = "DistanceFromEastBorder:$power"
}

object DistanceFromWestBorder : Sensor() {

    override fun calculatePower(world: World, blob: Blob): Int =
        setAndGetPower((32 * blob.position.x) / world.context.size.maxX)

    override fun toString() = "DistanceFromWestBorder:$power"
}


object DistanceFromNorthBorder : Sensor() {

    override fun calculatePower(world: World, blob: Blob): Int =
        setAndGetPower((32 * blob.position.y) / world.context.size.maxY)

    override fun toString() = "DistanceFromNorthBorder:$power"
}

object DistanceFromSouthBorder : Sensor() {

    override fun calculatePower(world: World, blob: Blob): Int =
        setAndGetPower((32 * (world.context.size.maxY - blob.position.y)) / world.context.size.maxY)

    override fun toString() = "DistanceFromSouthBorder:$power"
}

object PopulationForward : Sensor() {

    override fun calculatePower(world: World, blob: Blob): Int {
        val power = (1..8).fold(0) { acc, i ->
            val x = blob.position.x + (i * blob.heading.dx)
            val y = blob.position.y + (i * blob.heading.dy)
            acc + if (world.isBlob(x, y)) 4 else 0
        }
        return setAndGetPower(power)
    }

    override fun toString() = "PopulationForward:$power"
}

object WallForward : Sensor() {

    override fun calculatePower(world: World, blob: Blob): Int {
        for (i in 1..8)
            if (world.isWall(blob.position.x + (i * blob.heading.dx), blob.position.y + (i * blob.heading.dy)))
                return setAndGetPower((9 - i) * 4)
        return setAndGetPower(0)
    }

    override fun toString() = "WallForward:$power"
}