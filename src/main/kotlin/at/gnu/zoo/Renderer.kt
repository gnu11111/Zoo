package at.gnu.zoo

interface Renderer {

    fun open(): Size
    fun init(world: World)
    fun view(world: World)
    fun finish(world: World, silent: Boolean = false)
    fun close()
}
