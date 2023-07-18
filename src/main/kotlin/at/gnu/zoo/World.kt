package at.gnu.zoo

import at.gnu.zoo.World.KillZone.*
import at.gnu.zoo.World.Walls.*
import kotlin.random.Random

class World(val context: Context) {

    var age = 0
    var oszillator = 0
    var populations: List<Population> = emptyList()
    val area = Array(context.size.maxY) { IntArray(context.size.maxX) }

    private val killZoneArea = Array(context.size.maxY) { IntArray(context.size.maxX) }
    private var oszillatorIncrement = -1

    enum class Walls {
        None, Vertical, Horizontal, Plus, Slash, Grid, Crosshair;
        companion object {
            fun randomType() = values().random()
        }
    }

    enum class KillZone {
        EasternHalf, WesternHalf, NorthernHalf, SouthernHalf, BorderArea, CenterArea, NorthSouth, EastWest, Checkers,
        Corners;
        companion object {
            fun randomType() = values().random()
        }
    }

    init {
        area.createWalls(context.walls)
        killZoneArea.createForType(context.killZone)
    }

    @Synchronized
    fun init(populations: List<Population>): World {
        age = 0
        oszillator = 0
        area.indices.forEach { y -> area[y].indices.forEach { x -> if (area[y][x] > EMPTY) area[y][x] = EMPTY } }
        this.populations = populations
        populations.forEach { population -> population.blobs.forEach { area[it.position.y][it.position.x] = 1 } }
        return this
    }

    @Synchronized
    fun progress(): World {
        age++
        processOszillator()
        populations.forEach { population ->
            population.blobs.asSequence().filter { it.alive }.forEach { blob ->
                blob.brain.think(this, blob).forEach { action -> action.apply(this, blob) }
            }
        }
        return this
    }

    @Synchronized
    fun endOfLifetime(): Int =
        populations.sumOf { it.killPopulation() }

    @Synchronized
    fun repopulate(): World {
        populations.forEach {
            val seededBlobs = if ((context.tribes > 1) && (Random.nextInt(1000) > 990)) {
                context.seeded++
                mutableListOf(populations.random().blobs.first())
            } else
                mutableListOf()
            it.reproduce(seededBlobs)
        }
        return init(populations)
    }

    @Synchronized
    fun kill(x: Int, y: Int) =
        populations.forEach { it.killBlob(x, y) }

    @Synchronized
    fun move(fromX: Int, fromY: Int, toX: Int, toY: Int, population: Int = 1) {
        area[fromY][fromX] = EMPTY
        area[toY][toX] = population
    }

    @Synchronized
    fun getType(x: Int, y: Int): Int =
        area.getOrNull(y)?.getOrNull(x) ?: EMPTY

    @Synchronized
    fun isEmpty(x: Int, y: Int): Boolean =
        (area.getOrNull(y)?.getOrNull(x) ?: 1) == EMPTY

    @Synchronized
    fun isWall(x: Int, y: Int): Boolean =
        (area.getOrNull(y)?.getOrNull(x) ?: EMPTY) == WALL

    @Synchronized
    fun isBlob(x: Int, y: Int): Boolean =
        (area.getOrNull(y)?.getOrNull(x) ?: EMPTY) > EMPTY

    @Synchronized
    fun isKillarea(x: Int, y: Int): Boolean =
        (killZoneArea.getOrNull(y)?.getOrNull(x) ?: 0) == 1

    @Synchronized
    fun freeSpots(): Int =
        area.sumOf { row -> row.count { it == EMPTY } }

    private fun processOszillator() {
        if ((oszillator >= 31) || (oszillator < 1))
            oszillatorIncrement = -oszillatorIncrement
        oszillator += oszillatorIncrement
    }

    private fun Array<IntArray>.createWalls(walls: Walls) {
        val yMax = this.size
        val xMax = this[0].size
        when (walls) {
            None -> this[0][0] = 0
            Vertical -> ((yMax / 4) until (yMax - (yMax / 4))).forEach { y -> this[y][xMax / 2] = WALL }
            Horizontal -> ((xMax / 4) until (xMax - (xMax / 4))).forEach { x -> this[yMax / 2][x] = WALL }
            Plus -> {
                ((xMax / 4) until (xMax - (xMax / 4))).forEach { x -> this[yMax / 2][x] = WALL }
                ((yMax / 4) until (yMax - (yMax / 4))).forEach { y -> this[y][xMax / 2] = WALL }
            }
            Slash -> (1 .. ((xMax / 2) + 1)).forEach { x ->
                this[yMax - (yMax / 4) - (x * yMax / xMax) - 1][(xMax / 4) + x] = WALL
            }
            Grid -> (1 .. (2 * yMax / 5)).forEach { y -> (1 .. (2 * xMax / 5)).forEach { x ->
                this[(yMax / 10) + (2 * y)][(xMax / 10) + (2 * x)] = WALL
            } }
            Crosshair -> {
                (0 .. (xMax / 3)).forEach { x -> this[yMax / 2][x] = WALL; this[yMax / 2][xMax - x - 1] = WALL }
                (0 .. (yMax / 3)).forEach { y -> this[y][xMax / 2] = WALL; this[yMax - y - 1][xMax / 2] = WALL }
            }
        }
    }

    private fun Array<IntArray>.createForType(killZone: KillZone) {
        val yMax = this.size
        val xMax = this[0].size
        when (killZone) {
            WesternHalf -> indices.forEach { y ->
                (0 until xMax).forEach { x -> this[y][x] = if (x < (xMax / 2)) 1 else 0 }
            }
            EasternHalf -> indices.forEach { y ->
                (0 until xMax).forEach { x -> this[y][x] = if (x > (xMax / 2)) 1 else 0 }
            }
            NorthernHalf -> indices.forEach { y ->
                (0 until xMax).forEach { x -> this[y][x] = if (y < (yMax / 2)) 1 else 0 }
            }
            SouthernHalf -> indices.forEach { y ->
                (0 until xMax).forEach { x -> this[y][x] = if (y > (yMax / 2)) 1 else 0 }
            }
            BorderArea -> indices.forEach { y ->
                (0 until xMax).forEach { x ->
                    this[y][x] = if ((x < (xMax / 10)) || (x >= (xMax - (xMax / 10)))
                        || (y < (yMax / 10)) || (y >= (yMax - (yMax / 10)))) 1 else 0
                }
            }
            CenterArea -> indices.forEach { y ->
                (0 until xMax).forEach { x ->
                    this[y][x] = if ((x >= (xMax / 10)) && (x < (xMax - (xMax / 10)))
                        && (y >= (yMax / 10)) && (y < (yMax - (yMax / 10)))) 1 else 0
                }
            }
            NorthSouth -> indices.forEach { y ->
                (0 until xMax).forEach { x ->
                    this[y][x] = if ((y < (yMax / 3)) || (y >= (yMax - (yMax / 3)))) 1 else 0
                }
            }
            EastWest -> indices.forEach { y ->
                (0 until xMax).forEach { x ->
                    this[y][x] = if ((x < (xMax / 3)) || (x >= (xMax - (xMax / 3)))) 1 else 0
                }
            }
            Checkers -> indices.forEach { y ->
                (0 until xMax).forEach { x ->
                    this[y][x] = if (((x <= (xMax / 2)) && (y <= (yMax / 2)))
                        || ((x > (xMax / 2)) && (y > (yMax / 2)))) 1 else 0
                }
            }
            Corners -> indices.forEach { y ->
                (0 until xMax).forEach { x ->
                    this[y][x] = if (((x < (xMax / 4)) && (y < (yMax / 4)))
                        || ((x >= (xMax - (xMax / 4))) && (y >= (yMax - (yMax / 4))))
                        || ((x >= (xMax - (xMax / 4))) && (y < (yMax / 4)))
                        || ((x < (xMax / 4)) && (y >= (yMax - (yMax / 4))))) 1 else 0
                }
            }
        }
    }

    companion object {
        const val EMPTY = 0
        const val WALL = -1

        fun randomWorld(context: Context = Context(Zoo.version)): World {
            val world = World(context)
            world.init(List(context.tribes) { Population.randomPopulation(world, it + 1) })
            return world
        }
    }
}
