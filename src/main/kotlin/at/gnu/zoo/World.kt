package at.gnu.zoo

import at.gnu.zoo.World.KillZone.*
import at.gnu.zoo.World.Walls.*

class World(val context: Context) {

    var age = 0
    var oszillator = 0
    var population: Population = Population(this)
    val area = Array(context.size.maxY) { IntArray(context.size.maxX) }

    private val killZoneArea = Array(context.size.maxY) { IntArray(context.size.maxX) }
    private var oszillatorIncrement = -1

    enum class Walls {
        None, Vertical, Horizontal, Plus, Slash, Grid;
        companion object {
            fun randomType() = values().random()
        }
    }

    enum class KillZone {
        EasternHalf, WesternHalf, NorthernHalf, SouthernHalf, BorderArea, CenterArea, NorthSouth, EastWest, Checkers;
        companion object {
            fun randomType() = values().random()
        }
    }

    init {
        area.createWalls(context.walls)
        killZoneArea.createForType(context.killZone)
    }

    @Synchronized
    fun init(population: Population): World {
        age = 0
        oszillator = 0
        area.indices.forEach { y -> area[y].indices.forEach { x -> if (area[y][x] == BLOB) area[y][x] = EMPTY } }
        this.population = population
        population.blobs.forEach { area[it.position.y][it.position.x] = BLOB }
        return this
    }

    @Synchronized
    fun progress(): World {
        age++
        processOszillator()
        population.blobs.asSequence().filter { it.alive }.forEach { blob ->
            blob.brain.think(this, blob).forEach { action -> action.apply(this, blob) }
        }
        return this
    }

    @Synchronized
    fun move(fromX: Int, fromY: Int, toX: Int, toY: Int) {
        area[fromY][fromX] = EMPTY
        area[toY][toX] = BLOB
    }

    @Synchronized
    fun isEmpty(x: Int, y: Int): Boolean =
        (area.getOrNull(y)?.getOrNull(x) ?: BLOB) == EMPTY

    @Synchronized
    fun isWall(x: Int, y: Int): Boolean =
        (area.getOrNull(y)?.getOrNull(x) ?: EMPTY) == WALL

    @Synchronized
    fun isBlob(x: Int, y: Int): Boolean =
        (area.getOrNull(y)?.getOrNull(x) ?: EMPTY) == BLOB

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
            Slash -> (0 until (xMax / 2)).forEach { x ->
                this[yMax - (yMax / 4) - (x * yMax / xMax)][(xMax / 4) + x] = WALL
            }
            Grid -> (0 until (2 * yMax / 5)).forEach { y -> (0 until (2 * xMax / 5)).forEach { x ->
                this[(yMax / 10) + (2 * y)][(xMax / 10) + (2 * x)] = WALL
            } }
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
                (0 until xMax).forEach { x -> this[y][x] = if (x >= (xMax / 2)) 1 else 0 }
            }
            NorthernHalf -> indices.forEach { y ->
                (0 until xMax).forEach { x -> this[y][x] = if (y < (yMax / 2)) 1 else 0 }
            }
            SouthernHalf -> indices.forEach { y ->
                (0 until xMax).forEach { x -> this[y][x] = if (y >= (yMax / 2)) 1 else 0 }
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
                    this[y][x] = if (((x < (xMax / 2)) && (y < (yMax / 2)))
                        || ((x >= (xMax / 2)) && (y >= (yMax / 2)))) 1 else 0
                }
            }
        }
    }

    companion object {
        const val EMPTY = 0
        const val WALL = 1
        const val BLOB = 2

        fun randomWorld(context: Context = Context(Zoo.version)): World {
            val world = World(context)
            world.init(Population.randomPopulation(world))
            return world
        }
    }
}
