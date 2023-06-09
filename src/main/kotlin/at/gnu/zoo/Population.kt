package at.gnu.zoo

import kotlin.random.Random

class Population(val tribe: Int, val world: World, val blobs: MutableList<Blob> = mutableListOf()) {

    fun reproduce(children: MutableList<Blob> = mutableListOf()) {
        val mutationRate = (100 * (world.context.blobs - world.context.survivors)) / world.context.blobs
        blobs.removeIf { !it.alive }
        val positions = mutableSetOf<Position>()
        val blobsRequired = (world.context.blobs / world.context.tribes) - children.size
        repeat(blobsRequired.coerceAtMost(world.freeSpots())) {
            var newBlob: Blob
            do {
                newBlob = blobs.createOffspring(mutationRate)
            } while (newBlob.position in positions)
            children += newBlob
            positions += newBlob.position
        }
        blobs.clear()
        blobs += children
    }

    fun killBlob(x: Int, y: Int) =
        blobs.firstOrNull { (it.position.x == x) && (it.position.y == y) }?.kill()?.also {
            if (it) world.context.killed++
        }

    fun killPopulation(): Int {
        blobs.forEach { if (world.isKillarea(it.position.x, it.position.y)) it.kill() }
        return blobs.count { it.alive }
    }

    private fun List<Blob>.createOffspring(mutationRate: Int): Blob {
        val position = Position.randomPosition(world)
        val fatherGenom = (if (isEmpty()) Brain.randomBrain(world.context) else this[Random.nextInt(size)].brain).genom
        val motherGenom = (if (isEmpty()) Brain.randomBrain(world.context) else this[Random.nextInt(size)].brain).genom
        val cutPosition = Random.nextInt(fatherGenom.length)
        val childGenom = fatherGenom.substring(0, cutPosition) + motherGenom.substring(cutPosition)
        if (Random.nextInt(10000) >= (world.context.genomSize * mutationRate))
            return Blob(Brain(childGenom), position, tribe)
        world.context.mutations++
        val mutatedChildGenom = mutateGenom(childGenom)
        return Blob(Brain(mutatedChildGenom), position, tribe)
    }

    private fun mutateGenom(genom: String): String {
        val genomPosition = Random.nextInt(genom.length - 4) + 4
        val gene = Integer.toBinaryString(genom[genomPosition].toString().toInt(16))!!.padStart(4, '0')
        val genePosition = Random.nextInt(gene.length)
        val newValue = if (gene[genePosition] == '0') '1' else '0'
        val mutatedGene = gene.substring(0, genePosition) + newValue + gene.substring(genePosition + 1)
        return genom.substring(0, genomPosition) + Integer.toHexString(Integer.parseInt(mutatedGene, 2)) +
                genom.substring(genomPosition + 1)
    }

    companion object {
        fun randomPopulation(world: World, tribe: Int = 1, genom: String? = null): Population {
            val blobs = mutableListOf<Blob>()
            val positions = mutableSetOf<Position>()
            val amount = world.context.blobs / world.context.tribes
            repeat(amount.coerceAtMost(world.freeSpots())) {
                var newBlob: Blob
                do {
                    newBlob = Blob.randomBlob(world, tribe, genom)
                } while (newBlob.position in positions)
                blobs += newBlob
                positions += newBlob.position
            }
            return Population(tribe, world, blobs)
        }
    }
}
