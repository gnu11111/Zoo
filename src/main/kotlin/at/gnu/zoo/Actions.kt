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
