package at.gnu.zoo

interface Input {

    enum class Key { None, Esc, Right, Left, Up, Down, Pause, New, Endless, Skip, Log }

    fun poll(): Key
    fun read(): Key
}
