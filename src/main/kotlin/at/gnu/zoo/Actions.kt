package at.gnu.zoo

import kotlin.random.Random

class Move : Action() {
    override fun apply(world: World, blob: Blob) =
        move(blob, world)

    override fun clone() = Move()
    override fun toString() = "Move:$power"
}

class MoveRandomly : Action() {
    override fun apply(world: World, blob: Blob) =
        if (Random.nextInt(2) > 0)
            move(blob.apply { heading.dx = Heading.randomDirection(); heading.dy = 0 }, world)
        else
            move(blob.apply { heading.dx = 0; heading.dy = Heading.randomDirection() }, world)

    override fun clone() = MoveRandomly()
    override fun toString() = "MoveRandomly:$power"
}

class MoveEast : Action() {
    override fun apply(world: World, blob: Blob) =
        move(blob.apply { heading.dx = 1; heading.dy = 0 }, world)

    override fun clone() = MoveEast()
    override fun toString() = "MoveEast:$power"
}

class MoveWest : Action() {
    override fun apply(world: World, blob: Blob) =
        move(blob.apply { heading.dx = -1; heading.dy = 0 }, world)

    override fun clone() = MoveWest()
    override fun toString() = "MoveWest:$power"
}

class MoveNorth : Action() {
    override fun apply(world: World, blob: Blob) =
        move(blob.apply { heading.dx = 0; heading.dy = -1 }, world)

    override fun clone() = MoveNorth()
    override fun toString() = "MoveNorth:$power"
}

class MoveSouth : Action() {
    override fun apply(world: World, blob: Blob) =
        move(blob.apply { heading.dx = 0; heading.dy = 1 }, world)

    override fun clone() = MoveSouth()
    override fun toString() = "MoveSouth:$power"
}

class TurnLeft : Action() {
    override fun apply(world: World, blob: Blob) =
        when {
            ((blob.heading.dx == 1) && (blob.heading.dy == 0)) -> { blob.heading.dx = 0; blob.heading.dy = -1 }
            ((blob.heading.dx == 0) && (blob.heading.dy == -1)) -> { blob.heading.dx = -1; blob.heading.dy = 0 }
            ((blob.heading.dx == -1) && (blob.heading.dy == 0)) -> { blob.heading.dx = 0; blob.heading.dy = 1 }
            else -> { blob.heading.dx = 1; blob.heading.dy = 0 }
        }

    override fun clone() = TurnLeft()
    override fun toString() = "TurnLeft:$power"
}

class TurnRight : Action() {
    override fun apply(world: World, blob: Blob) =
        when {
            ((blob.heading.dx == 1) && (blob.heading.dy == 0)) -> { blob.heading.dx = 0; blob.heading.dy = 1 }
            ((blob.heading.dx == 0) && (blob.heading.dy == 1)) -> { blob.heading.dx = -1; blob.heading.dy = 0 }
            ((blob.heading.dx == -1) && (blob.heading.dy == 0)) -> { blob.heading.dx = 0; blob.heading.dy = -1 }
            else -> { blob.heading.dx = 1; blob.heading.dy = 0 }
        }

    override fun clone() = TurnRight()
    override fun toString() = "TurnRight:$power"
}

class TurnAround : Action() {
    override fun apply(world: World, blob: Blob) {
        blob.heading.dx = -blob.heading.dx
        blob.heading.dy = -blob.heading.dy
    }

    override fun clone() = TurnAround()
    override fun toString() = "TurnAround:$power"
}

class TurnRandomly : Action() {
    override fun apply(world: World, blob: Blob) =
        when {
            (Random.nextInt(3) > 1) -> { blob.heading.dx = -blob.heading.dx; blob.heading.dy = -blob.heading.dy }
            (blob.heading.dx != 0) -> { blob.heading.dx = 0; blob.heading.dy = Heading.randomDirection() }
            else -> { blob.heading.dx = Heading.randomDirection(); blob.heading.dy = 0 }
        }

    override fun clone() = TurnRandomly()
    override fun toString() = "TurnRandomly:$power"
}

class Kill : Action() {
    override fun apply(world: World, blob: Blob) {
        if (!world.context.killNeuronActive) return
        val x = blob.position.x + blob.heading.dx
        val y = blob.position.y + blob.heading.dy
        if (world.isBlob(x, y) && (Random.nextInt(100) > 98))
            world.population.killBlob(x, y)
    }

    override fun clone() = Kill()
    override fun toString() = "Kill:$power"
}
