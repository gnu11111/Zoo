package at.gnu.zoo

import at.gnu.zoo.World.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Toolkit
import java.lang.Thread.sleep
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val arguments = parseArguments(args)
    if (arguments["-?"] != null) printUsageAndExit()
    val renderer = Terminal(Size(80, 40))
    do {
        val delay = arguments["-d"]?.firstOrNull()?.toLongOrNull() ?: Zoo.defaultDelay
        val genom = arguments["-g"]?.firstOrNull()
        val version = genom?.take(2)
        if ((version != null) && !Zoo.version.replace(".", "").startsWith(version)) {
            println("Incompatible genom-version, exiting! genom-version: $version, program-version: ${Zoo.version}")
            exitProcess(-1)
        }
        val innerNeurons = genom?.drop(2)?.take(2)?.toInt(16) ?: (2 + (2 * Random.nextInt(5)))
        val genomSize = if (genom != null) (genom.length - 4) / Brain.geneSize else 5 + (5 * Random.nextInt(5))
        val contextString = arguments["-c"]?.firstOrNull()
        val context = if (contextString != null)
            Json.decodeFromString<Context>(contextString).apply {
                generation = 0
                survivors = 0
                mutations = 0
                this.delay = delay
                size = renderer.open()
            }
        else
            Context(
                version = Zoo.version,
                generations = -1, // 100 + (100 * Random.nextInt(50)),
                lifetime = 50 + (5 * Random.nextInt(50)),
                blobs = 50 + (5 * Random.nextInt(50)),
                genomSize = genomSize,
                innerNeurons = innerNeurons,
                killZone = KillZone.randomType(),
                walls = Walls.randomType(),
                delay = delay,
                size = renderer.open()
            )
        if (context.version != Zoo.version) {
            println("Incompatible context-version, exiting! context-version: ${context.version}, " +
                    "program-version: ${Zoo.version}")
            exitProcess(-1)
        }
        val world = World(context)
        val zoo = Zoo(world.init(Population.randomPopulation(world, genom)), renderer, renderer)
    } while (zoo.run())
    renderer.close()
}

fun printUsageAndExit() {
    println("""
        USAGE: java -jar zoo-${Zoo.version}.jar [-c <context>] [-g <genom>] [-d <delay>]
        
            -c <context> ... always use this context
            -g <genom>   ... use this genom as starting-point for every simulation
            -d <delay>   ... delay between steps for first simulation [ms]
    """.trimIndent())
    exitProcess(0)
}

class Zoo(private val world: World, private val renderer: Renderer, private val input: Input) {

    companion object {
        const val version = "0.5.1"
        const val defaultDelay = 50L
        val log: Logger = LoggerFactory.getLogger(Zoo::class.java)
        val json = Json { encodeDefaults = true }
    }

    fun run(): Boolean {
        var render = true
        var silent = false
        var streak = 0
        while (true) {
            if (world.context.survivors == world.context.blobs) streak++ else streak = 0
            if (((world.context.generations >= 0) && (world.context.generation >= world.context.generations))
                || ((world.context.generations < 0) && (streak > 23))) {
                val context = json.encodeToString(world.context).replace("\"", "\\\"")
                log.info("-c $context -g ${world.population.blobs.firstOrNull()?.brain?.genom ?: ""}")
                repeat(3) { Toolkit.getDefaultToolkit().beep() }
                world.context.delay = 50L
                render = true
                silent = false
                input.read()
            }
            if (render && !silent)
                renderer.init(world)
            var fastForward = false
            while (world.age < world.context.lifetime) {
                when (input.poll()) {
                    Input.Key.None -> if (!fastForward && render && !silent) sleep(world.context.delay)
                    Input.Key.Right -> world.context.delay = max(0, world.context.delay - 25)
                    Input.Key.Left -> world.context.delay = min(225, world.context.delay + 25)
                    Input.Key.Up -> if (render) render = false else { render = true; fastForward = true }
                    Input.Key.Down -> { silent = !silent }
                    Input.Key.Skip -> fastForward = true
                    Input.Key.Pause -> input.read()
                    Input.Key.Endless -> world.context.generations = -1
                    Input.Key.New -> return true
                    Input.Key.Esc -> return false
                }
                world.progress()
                if (render && !fastForward && !silent)
                    renderer.view(world)
            }
            val remainingPopulation = world.population.killPopulation(world)
            world.context.survivors = remainingPopulation.blobs.size
            renderer.finish(world, silent)
            if (((world.context.generations >= 0) && (world.context.generation >= world.context.generations))
                || ((world.context.generations < 0) && (streak > 23)))
                break
            if (render && !silent)
                sleep(min(3000, (world.context.delay * 40L)))
            world.context.generation++
            world.init(remainingPopulation.reproduce(world))
        }
        return (input.read() != Input.Key.Esc)
    }
}

@Serializable
data class Context(
    val version: String,
    val lifetime: Int = 1,
    val blobs: Int = 1,
    val genomSize: Int = 10,
    val innerNeurons: Int = 4,
    val killZone: KillZone = KillZone.randomType(),
    val walls: Walls = Walls.randomType(),
    var generations: Int = 1,
    var size: Size = Size(80, 24),
    var generation: Int = 0,
    var delay: Long = Zoo.defaultDelay,
    var survivors: Int = 0,
    var mutations: Int = 0
)

@Serializable
data class Size(val maxX: Int, val maxY: Int)

private fun parseArguments(args: Array<String>): Map<String, List<String>> =
    args.fold<String, Pair<Map<String, List<String>>, String>>(Pair(emptyMap(), "")) { (map, lastKey), arg ->
        if (arg.startsWith("-"))
            Pair(map + (arg to emptyList()), arg)
        else
            Pair(map + (lastKey to map.getOrDefault(lastKey, emptyList()) + arg), lastKey)
    }.first
