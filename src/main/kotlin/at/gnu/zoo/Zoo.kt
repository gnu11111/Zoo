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
import kotlin.random.Random
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val arguments = parseArguments(args)
    if (arguments["-?"] != null) printUsageAndExit()
    val quiet = (arguments["-q"] != null)
    val delay = arguments["-d"]?.firstOrNull()?.toLongOrNull() ?: Zoo.DEFAULT_DELAY
    val genom = arguments.getGenom()
    val renderer = Terminal(Size(79, 39))
    val size = renderer.open()
    val defaultContext = arguments.getDefaultContext(delay, size)
    do {
        val genomSize = if (genom != null) (genom.length - 4) / Brain.GENE_SIZE else 5 + (5 * Random.nextInt(10))
        val innerNeurons = genom?.drop(2)?.take(2)?.toInt(16) ?: (2 + (2 * Random.nextInt(5)))
        val tribes = 1 + Random.nextInt(4)
        val context = defaultContext
            ?: Context(
                version = Zoo.VERSION,
                generations = -1, // 100 + (100 * Random.nextInt(50)),
                lifetime = 50 + (5 * Random.nextInt(50)),
                tribes = tribes,
                blobs = tribes * ((50 + (5 * Random.nextInt(size.maxX * size.maxY / 64))) / tribes),
                genomSize = genomSize,
                innerNeurons = innerNeurons,
                killNeuronActive = (Random.nextInt(10) > 8),
                killZone = KillZone.randomType(),
                walls = Walls.randomType(),
                delay = delay,
                size = size
            )
        val zoo = Zoo(World.randomWorld(context), renderer, renderer, quiet)
    } while (zoo.run() && (genom == null))
    renderer.close()
}

class Zoo(private val world: World, private val renderer: Renderer, private val input: Input,
          private val quiet: Boolean) {

    companion object {
        const val VERSION = "0.8.2"
        const val DEFAULT_DELAY = 50L
        val log: Logger = LoggerFactory.getLogger(Zoo::class.java)
        val json = Json { encodeDefaults = true }
    }

    fun run(): Boolean {
        var render = true
        var silent = false
        var streak = 0
        while (true) {
            if (world.context.survivors == world.context.blobs) streak++ else streak = 0
            val endOfLife = (((world.context.generations >= 0)
                    && (world.context.generation >= world.context.generations))
                    || ((world.context.generations < 0) && (streak >= world.context.streak)))
            if (endOfLife) {
                if (!quiet) Toolkit.getDefaultToolkit().beep()
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
                    Input.Key.Right -> world.context.delay = (world.context.delay - 25).coerceAtLeast(0)
                    Input.Key.Left -> world.context.delay = (world.context.delay + 25).coerceAtMost(225)
                    Input.Key.Up -> if (render) render = false else { render = true; fastForward = true }
                    Input.Key.Down -> { silent = !silent; renderer.init(world) }
                    Input.Key.Skip -> fastForward = true
                    Input.Key.Pause -> input.read()
                    Input.Key.Endless -> world.context.generations = -1
                    Input.Key.Log -> world.logFirstGenom()
                    Input.Key.New -> return true
                    Input.Key.Esc -> return false
                }
                world.progress()
                if (render && !fastForward && !silent)
                    renderer.view(world)
            }
            renderer.finish(world, silent)
            world.context.survivors = world.endOfLifetime()
            if (endOfLife)
                break
            if (render && !silent)
                sleep((world.context.delay * 40L).coerceAtMost(3000L))
            world.context.generation++
            world.repopulate()
        }
        return when (input.read()) {
            Input.Key.Log -> true.also { world.logFirstGenom() }
            Input.Key.Esc -> false
            else -> true
        }
    }

    private fun World.logFirstGenom() {
        val contextString = json.encodeToString(context).replace("\"", "\\\"")
        populations.forEach { log.info("-c $contextString -g ${it.blobs.firstOrNull()?.brain?.genom ?: ""}") }
    }
}

@Serializable
data class Context(
    val version: String,
    val lifetime: Int = 1,
    val streak: Int = 12,
    val tribes: Int = 1,
    val blobs: Int = 1,
    val genomSize: Int = 10,
    val innerNeurons: Int = 4,
    val killNeuronActive: Boolean = false,
    val killZone: KillZone = KillZone.randomType(),
    val walls: Walls = Walls.randomType(),
    var generations: Int = 1,
    var size: Size = Size(80, 24),
    var generation: Int = 0,
    var delay: Long = Zoo.DEFAULT_DELAY,
    var survivors: Int = 0,
    var mutations: Int = 0,
    var killed: Int = 0,
    var seeded: Int = 0
)

@Serializable
data class Size(val maxX: Int, val maxY: Int)

private fun printUsageAndExit() {
    println("""
        USAGE: java -jar zoo-${Zoo.VERSION}.jar [-c <context>] [-g <genom>] [-d <delay>] [-q] [-?]
        
            -c <context> ... always use this context
            -g <genom>   ... use this genom as starting-point for every simulation
            -d <delay>   ... delay between steps for first simulation [ms]
            -q           ... quiet mode, do not ping
    """.trimIndent())
    exitProcess(0)
}

private fun parseArguments(args: Array<String>): Map<String, List<String>> =
    args.fold<String, Pair<Map<String, List<String>>, String>>(Pair(emptyMap(), "")) { (map, lastKey), arg ->
        if (arg.startsWith("-"))
            Pair(map + (arg to emptyList()), arg)
        else
            Pair(map + (lastKey to map.getOrDefault(lastKey, emptyList()) + arg), lastKey)
    }.first

private fun Map<String, List<String>>.getGenom(): String? {
    val genom = this["-g"]?.firstOrNull()
    val version = genom?.take(2)
    if ((version != null) && !Zoo.VERSION.replace(".", "").startsWith(version)) {
        println("Incompatible genom-version, exiting! genom-version: $version, program-version: ${Zoo.VERSION}")
        exitProcess(-1)
    }
    return genom
}

private fun Map<String, List<String>>.getDefaultContext(delay: Long, size: Size): Context? {
    val contextString = this["-c"]?.firstOrNull()
    if (contextString != null) {
        try {
            val defaultContext = Json.decodeFromString<Context>(contextString).apply {
                generation = 0
                survivors = 0
                mutations = 0
                this.delay = delay
                this.size = size
            }
            if (defaultContext.version != Zoo.VERSION) {
                println("Incompatible context-version, exiting! context-version: ${defaultContext.version}, " +
                        "program-version: ${Zoo.VERSION}")
                exitProcess(-3)
            }
            return defaultContext
        } catch (e: Exception) {
            println("Unable to parse context: '$contextString'")
            exitProcess(-2)
        }
    }
    return null
}
