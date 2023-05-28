package at.gnu.zoo

import kotlin.math.min
import kotlin.random.Random

data class Population(val blobs: List<Blob> = emptyList()) {

    fun reproduce(world: World): Population {
        val mutationRate = (100 * (world.context.blobs - world.context.survivors)) / world.context.blobs
        val population = mutableListOf<Blob>()
        val positions = mutableSetOf<Position>()
        repeat(min(world.context.blobs, world.freeSpots())) {
            var newBlob: Blob
            do {
                newBlob = blobs.createOffspring(world, mutationRate)
            } while (newBlob.position in positions)
            population += newBlob
            positions += newBlob.position
        }
        return Population(population)
    }

    fun killPopulation(world: World): Population =
        Population(blobs.filter { !world.isKillarea(it.position.x, it.position.y) })

    private fun List<Blob>.createOffspring(world: World, mutationRate: Int): Blob {
        val position = Position.randomPosition(world)
        val blob = if (isEmpty()) Blob.randomBlob(world) else this[Random.nextInt(size)]
        val genom = blob.brain.genom
        val select = Random.nextInt(1000)
        if (select < 200)
            return Blob(Brain(blob.brain.genom), position)
        else if (select < 998 - mutationRate) {
            val genom2 = if (isEmpty()) Blob.randomBlob(world).brain.genom else this[Random.nextInt(size)].brain.genom
            if (genom.length == genom2.length) {
                val cutPosition = Random.nextInt(genom.length)
                val newBrain = Brain(genom.substring(0, cutPosition) + genom2.substring(cutPosition))
                return Blob(newBrain, position)
            }
        }
        world.context.mutations++
        val genomPosition = Random.nextInt(genom.length - 4) + 4
        val gene = Integer.toBinaryString(genom[genomPosition].toString().toInt(16))!!.padStart(4, '0')
        val genePosition = Random.nextInt(gene.length)
        val newValue = if (gene[genePosition] == '0') '1' else '0'
        val newGene = gene.substring(0, genePosition) + newValue + gene.substring(genePosition + 1)
        val newGenom = genom.substring(0, genomPosition) + Integer.toHexString(Integer.parseInt(newGene, 2)) +
                genom.substring(genomPosition + 1)
        return Blob(Brain(newGenom), position)
    }

    companion object {
        fun randomPopulation(world: World, genom: String? = null): Population {
            val population = mutableListOf<Blob>()
            val positions = mutableSetOf<Position>()
            repeat(min(world.context.blobs, world.freeSpots())) {
                var newBlob: Blob
                do {
                    newBlob = Blob.randomBlob(world, genom)
                } while (newBlob.position in positions)
                population += newBlob
                positions += newBlob.position
            }
            return Population(population)
        }
    }
}
