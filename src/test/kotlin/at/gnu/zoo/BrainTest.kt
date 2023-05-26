package at.gnu.zoo

import org.junit.Test

class BrainTest {

    @Test
    fun `create a brain`() {
        val brain = Brain.randomBrain(Context(Zoo.version))
        val blob = Blob(brain)
        val actions = brain.think(World.randomWorld(), blob)
        brain.print()
        println(actions)
    }
}
