package at.gnu.zoo

import java.awt.Color
import kotlin.random.Random

data class Blob(val brain: Brain, val position: Position = Position(0, 0)) {

    val color = brain.getColor()
    val heading = Heading.randomHeading()

    private fun Brain.getColor(): Color {
        val color = genom.chunked(genom.length / 3).map { it.fold(0) { acc, c -> (acc + c.code) % 255 } }
        return Color(color[0], color[1], color[2])
    }

    companion object {
        fun randomBlob(world: World, genom: String? = null) =
            Blob(if (genom != null) Brain(genom) else Brain.randomBrain(world.context), Position.randomPosition(world))
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
