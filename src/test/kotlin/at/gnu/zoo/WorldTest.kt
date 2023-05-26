package at.gnu.zoo

import org.junit.Test
import kotlin.test.assertEquals

class WorldTest {

    @Test
    fun `render world after some progress`() {
        val context = Context(version = Zoo.version, blobs = 10, size = Size(10, 10))
        println(context)
        val world = World.randomWorld(context).progress()
        assertEquals(10, world.population.blobs.size)
        var blobs = 0
        for (y in world.area.indices) {
            for (x in world.area.first().indices) {
                if (world.isWall(x, y))
                    print("#")
                else if (!world.isEmpty(x, y)) {
                    print("*")
                    blobs++
                } else if (world.isKillarea(x, y))
                    print(":")
                else
                    print(".")
            }
            println()
        }
        assertEquals(10, blobs)
    }
}
