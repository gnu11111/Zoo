package at.gnu.zoo

import java.awt.Color
import kotlin.random.Random

class Blob(val brain: Brain, val position: Position = Position(0, 0), val tribe: Int = 1) {

    val color = brain.getColor()
    val heading = Heading.randomHeading()
    var alive = true
        private set

    fun kill() =
        alive.also { alive = false }

    private fun Brain.getColor(): Color {
        val color = genom.chunked(genom.length / 3).map { it.fold(0) { acc, c -> (acc + c.code) % 255 } }
        return Color(color[0], color[1], color[2])
    }

    companion object {
        fun randomBlob(world: World, tribe: Int = 1, genom: String? = null): Blob {
            val brain = if (genom != null) Brain(genom) else Brain.randomBrain(world.context)
            return Blob(brain, Position.randomPosition(world), tribe)
        }
    }
}

data class Position(var x: Int, var y: Int) {

    companion object {
        fun randomPosition(world: World): Position {
            var x: Int
            var y: Int
            do {
                x = Random.nextInt(world.context.size.maxX)
                y = Random.nextInt(world.context.size.maxY)
            } while (!world.isEmpty(x, y))
            return Position(x, y)
        }
    }
}

class Heading(var dx: Int, var dy: Int) {

    companion object {
        fun randomHeading() =
            if (Random.nextInt(2) > 0) Heading(randomDirection(), 0) else Heading(0, randomDirection())

        fun randomDirection() =
            if (Random.nextInt(2) > 0) 1 else -1
    }
}
