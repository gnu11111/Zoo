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
