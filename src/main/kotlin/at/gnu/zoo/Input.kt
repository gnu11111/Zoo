package at.gnu.zoo

interface Input {

    enum class Key { None, Esc, Right, Left, Up, Down, Pause, New, Endless, Skip }

    fun poll(): Key
    fun read(): Key
}
