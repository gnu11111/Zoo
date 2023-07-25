package at.gnu.zoo

import com.googlecode.lanterna.*
import com.googlecode.lanterna.TextColor.ANSI.*
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType.*
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import java.awt.Color

class Terminal(size: Size) : Renderer, Input {

    private val screen: TerminalScreen
    private val occupied = mutableListOf<Pair<Int, Int>>()

    private var streak = 0
    private var quality = 0

    init {
        val terminalSize = TerminalSize((size.maxX + statusSize).coerceAtMost(215), size.maxY.coerceAtMost(65))
        val defaultTerminalFactory = DefaultTerminalFactory().setInitialTerminalSize(terminalSize)
            .setTerminalEmulatorTitle("Zoo by gnu")
        val terminal = defaultTerminalFactory.createTerminal()
        screen = TerminalScreen(terminal)
    }

    override fun open(): Size {
        screen.startScreen()
        screen.cursorPosition = null
        return Size(screen.terminalSize.columns - statusSize, screen.terminalSize.rows)
    }

    override fun init(world: World) {
        val textGraphics = screen.newTextGraphics()
        (0 until screen.terminalSize.rows).forEach { y ->
            (0 until screen.terminalSize.columns - statusSize).forEach { x ->
                if (world.isWall(x, y)) textGraphics.backgroundColor = BLUE else textGraphics.backgroundColor = BLACK
                textGraphics.putString(x, y, " ")
            }
        }
        val statusX = screen.terminalSize.columns - statusSize + 2
        textGraphics.foregroundColor = BLUE
        textGraphics.backgroundColor = BLACK_BRIGHT
        val statusText = "║" + " ".repeat(statusSize - 2) + "║"
        (0 until screen.terminalSize.rows).forEach { textGraphics.putString(statusX - 2, it, statusText) }
        textGraphics.foregroundColor = WHITE_BRIGHT
        textGraphics.putString(statusX, 12, "Tribes:     ${world.context.tribes}")
        textGraphics.putString(statusX, 13, "Blobs:      ${world.context.blobs}")
        textGraphics.putString(statusX, 14, "Genom-Size: ${world.context.genomSize}")
        textGraphics.putString(statusX, 15, "Neurons:    ${world.context.innerNeurons}")
        textGraphics.putString(statusX, 16, "Kill-Zone:  ${world.context.killZone}")
        textGraphics.foregroundColor = YELLOW_BRIGHT
        textGraphics.putString(statusX + 3, 1, "o")
        textGraphics.foregroundColor = CYAN_BRIGHT
        textGraphics.putString(statusX + 4, 1, "o")
        textGraphics.foregroundColor = WHITE
        textGraphics.putString(statusX + 5, 1, " by gnu  (v${world.context.version})")
        textGraphics.foregroundColor = WHITE_BRIGHT
        textGraphics.backgroundColor = RED
        textGraphics.putString(statusX + 2, 1, "Z")
        streak = 0
        quality = 0
        textGraphics.showResult(world, false)
        occupied.clear()
        renderPopulation(world)
        screen.refresh()
    }

    override fun view(world: World) {
        val textGraphics = screen.newTextGraphics()
        val statusX = screen.terminalSize.columns - statusSize + 2
        textGraphics.foregroundColor = WHITE_BRIGHT
        textGraphics.backgroundColor = BLACK_BRIGHT
        val speed = if (world.context.delay == 0L) "MAX" else "${(10L - (world.context.delay / 25L))}  "
        textGraphics.putString(statusX, 7, "Age:        ${world.age} / ${world.context.lifetime}  ")
        textGraphics.putString(statusX, 8, "Speed:      $speed")
        renderPopulation(world)
        screen.refresh()
    }

    override fun finish(world: World, silent: Boolean) {
        val textGraphics = screen.newTextGraphics()
        if (!silent) {
            renderPopulation(world)
            (0 until screen.terminalSize.rows).forEach { y ->
                (0 until screen.terminalSize.columns - statusSize).forEach { x ->
                    if (world.isKillarea(x, y)) {
                        if (world.isWall(x, y))
                            textGraphics.backgroundColor = RED_BRIGHT
                        else
                            textGraphics.backgroundColor = RED
                        textGraphics.putString(x, y, " ")
                    }
                }
            }
            world.populations.forEach { population ->
                population.blobs.forEach {
                    if (world.isKillarea(it.position.x, it.position.y))
                        setCharacterInBackbuffer(population.tribe, it.position.x, it.position.y, false, it.color)
                    else
                        setCharacterInBackbuffer(population.tribe, it.position.x, it.position.y, it.alive, it.color)
                }
            }
        }
        textGraphics.showResult(world, true)
        screen.refresh()
    }

    override fun close() {
        screen.close()
    }

    override fun poll(): Input.Key =
        mapKey(screen.terminal.pollInput())

    override fun read(): Input.Key =
        mapKey(screen.terminal.readInput())

    private fun renderPopulation(world: World) {
        occupied.forEach { clearCharacterInBackbuffer(it.first, it.second) }
        occupied.clear()
        world.populations.forEach { population ->
            population.blobs.forEach {
                setCharacterInBackbuffer(population.tribe, it.position.x, it.position.y, it.alive, it.color)
                occupied.add(it.position.x to it.position.y)
            }
        }
    }

    private fun TextGraphics.showResult(world: World, finish: Boolean) {
        val statusX = screen.terminalSize.columns - statusSize + 2
        val rate = (100 * world.context.survivors) / world.context.blobs
        backgroundColor = BLACK_BRIGHT
        foregroundColor = if (finish) GREEN_BRIGHT else WHITE_BRIGHT
        if (world.context.generations >= 0)
            putString(statusX, 3, "Generation: ${world.context.generation} / ${world.context.generations}")
        else
            putString(statusX, 3, "Generation: ${world.context.generation}        ")
        putString(statusX, 4, "Mutations:  ${world.context.mutations}")
        putString(statusX, 5, "Seeded:     ${world.context.seeded}")
        if (world.context.killNeuronActive)
            putString(statusX, 6, "Killed:     ${world.context.killed}")
        putString(statusX, 10, "                        ")
        calculateStreak(rate)
        backgroundColor = if (rate > 90) GREEN else if (rate > 60) CYAN else WHITE
        putString(statusX, 10, " ".repeat((24 * rate) / 100))
        backgroundColor = GREEN_BRIGHT
        putString(statusX, 10, " ".repeat((24 * streak / world.context.streak).coerceAtMost(24)))
    }

    private fun calculateStreak(rate: Int) {
        if (rate == 100) {
            quality++
            if (streak < quality) streak++
        } else {
            quality = 0
            if (streak > 0) streak--
        }
    }

    private fun mapKey(key: KeyStroke?): Input.Key =
        when {
            (key?.keyType == ArrowLeft) -> Input.Key.Left
            (key?.keyType == ArrowRight) -> Input.Key.Right
            (key?.keyType == ArrowUp) -> Input.Key.Up
            (key?.keyType == ArrowDown) -> Input.Key.Down
            (key?.character == 'e') -> Input.Key.Endless
            (key?.character == 'p') -> Input.Key.Pause
            (key?.character == 's') -> Input.Key.Skip
            (key?.character == 'l') -> Input.Key.Log
            ((key?.character == 'q') || (key?.character == 'x') || (key?.keyType == Escape)) -> Input.Key.Esc
            ((key?.character == 'n') || (key?.character == ' ') || (key?.keyType == Enter)) -> Input.Key.New
            else -> Input.Key.None
        }

    private fun setCharacterInBackbuffer(tribe: Int, x: Int, y: Int, alive: Boolean, color: Color,
                                         background: TextColor = BLACK) {
        val cellToModify = TerminalPosition(x, y)
        // ×o+*øƟ⁜⁕※∆∅⊞⊟⊕⊗⊙⊚⊛⊠⊡⋈⎈⎊①②③④⑤⑥⑦⑧⑨▢▣△▽◇◉○◌◻✫✻♥♣★◈∎◍
        val character = "♣★o▲♥"[tribe % 5]
        val foreground = color.toForegroundColor()
        val foregroundColor = if (!alive || (foreground == background)) WHITE_BRIGHT else foreground
        val backgroundColor = if (alive) background else RED
        screen.setCharacter(cellToModify, screen.getBackCharacter(cellToModify)
            .withForegroundColor(foregroundColor).withBackgroundColor(backgroundColor).withCharacter(character))
    }

    private fun clearCharacterInBackbuffer(x: Int, y: Int) {
        val cellToModify = TerminalPosition(x, y)
        screen.setCharacter(cellToModify, screen.getBackCharacter(cellToModify).withBackgroundColor(BLACK)
            .withCharacter(' '))
    }

    private fun Color.toForegroundColor(): TextColor =
        when {
            ((red >= 192) && (green >= 192) && (blue >= 192)) -> WHITE_BRIGHT
            ((red >= 192) && (green < 128) && (blue < 128)) -> RED_BRIGHT
            ((red >= 64) && (green < 64) && (blue < 64)) -> RED
            ((red < 128) && (green >= 192) && (blue < 128)) -> GREEN_BRIGHT
            ((red < 64) && (green >= 64) && (blue < 64)) -> GREEN
            ((red < 128) && (green < 128) && (blue >= 192)) -> BLUE_BRIGHT
            ((red < 64) && (green < 64) && (blue >= 64)) -> BLUE
            ((red >= 192) && (green < 128) && (blue >= 192)) -> MAGENTA_BRIGHT
            ((red >= 64) && (green < 64) && (blue >= 64)) -> MAGENTA
            ((red >= 192) && (green >= 192) && (blue < 128)) -> YELLOW_BRIGHT
            ((red >= 64) && (green >= 64) && (blue < 64)) -> YELLOW
            ((red < 128) && (green >= 192) && (blue >= 192)) -> CYAN_BRIGHT
            ((red < 64) && (green >= 64) && (blue >= 64)) -> CYAN
            ((red >= 64) && (green >= 64) && (blue >= 64)) -> WHITE
            else -> BLACK_BRIGHT
        }

//    @Suppress("unused")
//    fun lanternaTest3() {
//        val terminalFactory = DefaultTerminalFactory()
//        val screen = terminalFactory.createScreen()
//        screen.startScreen()
//        val textGUI = MultiWindowTextGUI(screen)
//        val window = BasicWindow("My Root Window")
//        val contentPanel = Panel(GridLayout(2))
//        val gridLayout = contentPanel.layoutManager as GridLayout
//        gridLayout.horizontalSpacing = 3
//        val title = Label("This is a label that spans two columns")
//        title.layoutData = GridLayout.createLayoutData(
//            GridLayout.Alignment.BEGINNING, GridLayout.Alignment.BEGINNING,
//            true, false, 2, 1
//        )
//        contentPanel.addComponent(title)
//        contentPanel.addComponent(Label("Text Box (aligned)"))
//
//        contentPanel.addComponent(
//            TextBox()
//                .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER))
//        )
//
//        contentPanel.addComponent(Label("Password Box (right aligned)"))
//        contentPanel.addComponent(
//            TextBox().setMask('*')
//                .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER))
//        )
//
//        contentPanel.addComponent(Label("Read-only Combo Box (forced size)"))
//        val timezonesAsStrings = listOf(*TimeZone.getAvailableIDs())
//        val readOnlyComboBox = ComboBox(timezonesAsStrings)
//        readOnlyComboBox.isReadOnly = true
//        readOnlyComboBox.preferredSize = TerminalSize(20, 1)
//        contentPanel.addComponent(readOnlyComboBox)
//        contentPanel.addComponent(Label("Editable Combo Box (filled)"))
//        contentPanel.addComponent(
//            ComboBox("Item #1", "Item #2", "Item #3", "Item #4")
//                .setReadOnly(false)
//                .setLayoutData(GridLayout.createHorizontallyFilledLayoutData(1))
//        )
//
//        contentPanel.addComponent(Label("Button (centered)"))
//        contentPanel.addComponent(Button("Button") {
//            MessageDialog.showMessageDialog(
//                textGUI,
//                "MessageBox",
//                "This is a message box",
//                MessageDialogButton.OK
//            )
//        }.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER)))
//
//        contentPanel.addComponent(
//            EmptySpace()
//                .setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2))
//        )
//        contentPanel.addComponent(
//            Separator(Direction.HORIZONTAL)
//                .setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2))
//        )
//        val button = Button("Close", window::close)
//        contentPanel.addComponent(
//            button
//                .setLayoutData(GridLayout.createHorizontallyEndAlignedLayoutData(2))
//        )
//
//        window.component = contentPanel
//        textGUI.addWindowAndWait(window)
//        screen.stopScreen()
//    }
//
//    @Suppress("unused")
//    private fun lanternaTest2() {
//        val defaultTerminalFactory = DefaultTerminalFactory()
//        val terminal = defaultTerminalFactory.createTerminal()
//        val screen = TerminalScreen(terminal)
//        screen.startScreen()
//        screen.cursorPosition = null
//        val random = Random()
//        var terminalSize: TerminalSize = screen.terminalSize
//        for (column in 0 until terminalSize.columns) {
//            for (row in 0 until terminalSize.rows) {
//                screen.setCharacter(
//                    column, row, TextCharacter.fromCharacter(
//                        ' ',
//                        DEFAULT,  // This will pick a random background color
//                        TextColor.ANSI.values()[random.nextInt(TextColor.ANSI.values().size)]
//                    )[0]
//                )
//            }
//        }
//        screen.refresh()
//
//        while (true) {
//            val keyStroke: KeyStroke? = screen.pollInput()
//            if (keyStroke != null && (keyStroke.keyType == Escape || keyStroke.keyType == EOF)) {
//                break
//            }
//
//            val newSize: TerminalSize? = screen.doResizeIfNecessary()
//            if (newSize != null)
//                terminalSize = newSize
//
//            val charactersToModifyPerLoop = 10
//            for (i in 0 until charactersToModifyPerLoop) {
//                val cellToModify = TerminalPosition(
//                    random.nextInt(terminalSize.columns),
//                    random.nextInt(terminalSize.rows)
//                )
//                val color = TextColor.ANSI.values()[random.nextInt(TextColor.ANSI.values().size)]
//                var characterInBackBuffer: TextCharacter = screen.getBackCharacter(cellToModify)
//                characterInBackBuffer = characterInBackBuffer.withBackgroundColor(color)
//                characterInBackBuffer = characterInBackBuffer.withCharacter(' ')
//                screen.setCharacter(cellToModify, characterInBackBuffer)
//            }
//
//            val sizeLabel = "Terminal Size: $terminalSize"
//            val labelBoxTopLeft = TerminalPosition(1, 1)
//            val labelBoxSize = TerminalSize(sizeLabel.length + 2, 3)
//            val labelBoxTopRightCorner = labelBoxTopLeft.withRelativeColumn(labelBoxSize.columns - 1)
//            val textGraphics: TextGraphics = screen.newTextGraphics()
//            textGraphics.fillRectangle(labelBoxTopLeft, labelBoxSize, ' ')
//            textGraphics.drawLine(
//                labelBoxTopLeft.withRelativeColumn(1),
//                labelBoxTopLeft.withRelativeColumn(labelBoxSize.columns - 2),
//                Symbols.DOUBLE_LINE_HORIZONTAL
//            )
//            textGraphics.drawLine(
//                labelBoxTopLeft.withRelativeRow(2).withRelativeColumn(1),
//                labelBoxTopLeft.withRelativeRow(2).withRelativeColumn(labelBoxSize.columns - 2),
//                Symbols.DOUBLE_LINE_HORIZONTAL
//            )
//
//            textGraphics.setCharacter(labelBoxTopLeft, Symbols.DOUBLE_LINE_TOP_LEFT_CORNER)
//            textGraphics.setCharacter(labelBoxTopLeft.withRelativeRow(1), Symbols.DOUBLE_LINE_VERTICAL)
//            textGraphics.setCharacter(labelBoxTopLeft.withRelativeRow(2), Symbols.DOUBLE_LINE_BOTTOM_LEFT_CORNER)
//            textGraphics.setCharacter(labelBoxTopRightCorner, Symbols.DOUBLE_LINE_TOP_RIGHT_CORNER)
//            textGraphics.setCharacter(labelBoxTopRightCorner.withRelativeRow(1), Symbols.DOUBLE_LINE_VERTICAL)
//            textGraphics.setCharacter(
//                labelBoxTopRightCorner.withRelativeRow(2),
//                Symbols.DOUBLE_LINE_BOTTOM_RIGHT_CORNER
//            )
//
//            textGraphics.putString(labelBoxTopLeft.withRelative(1, 1), sizeLabel)
//            screen.refresh()
//            Thread.sleep(100)
//        }
//        screen.close()
//    }
//
//    @Suppress("unused")
//    private fun lanternaTest1() {
//        val defaultTerminalFactory = DefaultTerminalFactory()
//        val terminal = defaultTerminalFactory.createTerminal()
//        terminal.enterPrivateMode()
//        terminal.setCursorVisible(false)
//        val textGraphics = terminal.newTextGraphics()
//        textGraphics.foregroundColor = WHITE
//        textGraphics.backgroundColor = BLACK
//        textGraphics.putString(2, 1, "Lanterna Tutorial 2 - Press ESC to exit", SGR.BOLD)
//        textGraphics.foregroundColor = DEFAULT
//        textGraphics.backgroundColor = DEFAULT
//        textGraphics.putString(5, 3, "Terminal Size: ", SGR.BOLD)
//        textGraphics.putString(5 + "Terminal Size: ".length, 3, terminal.terminalSize.toString())
//        terminal.addResizeListener { terminal1, newSize ->
//            textGraphics.drawLine(5, 3, newSize.columns - 1, 3, ' ')
//            textGraphics.putString(5, 3, "Terminal Size: ", SGR.BOLD)
//            textGraphics.putString(5 + "Terminal Size: ".length, 3, newSize.toString())
//            terminal1.flush()
//        }
//        textGraphics.putString(5, 4, "Last Keystroke: ", SGR.BOLD)
//        textGraphics.putString(5 + "Last Keystroke: ".length, 4, "<Pending>")
//        terminal.flush()
//        var keyStroke: KeyStroke = terminal.readInput()
//        while (keyStroke.keyType != Escape) {
//            textGraphics.drawLine(5, 4, terminal.terminalSize.columns - 1, 4, ' ')
//            textGraphics.putString(5, 4, "Last Keystroke: ", SGR.BOLD)
//            textGraphics.putString(5 + "Last Keystroke: ".length, 4, keyStroke.toString())
//            terminal.flush()
//            keyStroke = terminal.readInput()
//        }
//        terminal.close()
//    }

    companion object {
        const val statusSize = 28
    }
}
