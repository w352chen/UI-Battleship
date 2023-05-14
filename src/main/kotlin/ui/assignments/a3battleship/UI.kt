package ui.assignments.a3battleship

import javafx.animation.AnimationTimer
import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import ui.assignments.a3battleship.model.*
import kotlin.math.absoluteValue

class UI(private var game : Game) : Canvas(875.0, 375.0) {

    companion object {
        val MARGIN = 25.0
        val BOARD_WIDTH = 300.0
        val WIDTH = 875.0
        val HEIGHT = 375.0
        val AI_BOARD_OFFSET = WIDTH - BOARD_WIDTH - Cell.Size * 1.75
        val ROTATING_DEGREE_PER_STEP = 9.0
    }

    private val ships = listOf(
        Ship(355.0, 25.0, ShipType.Destroyer),
        Ship(385.0, 25.0, ShipType.Cruiser),
        Ship(415.0, 25.0, ShipType.Submarine),
        Ship(445.0, 25.0, ShipType.Battleship),
        Ship(475.0, 25.0, ShipType.Carrier)
    )

    private val gc = graphicsContext2D
    private var currentSelectedShip : Ship? = null

    private var lastMX = 0.0
    private var lastMY = 0.0

    private val rotationTimer : AnimationTimer
    private val translationTimer : AnimationTimer
    private val rotatingShips = mutableListOf<Ship>()
    private val translatingShips = mutableListOf<Ship>()

    private var centralLabel = "My Fleet"

    private var gameTerminating = false

    init {
        rotationTimer = object : AnimationTimer() {
            override fun handle(now: Long) {
                draw()
                val shipsToRemove = mutableListOf<Ship>()
                for (animatedShip in rotatingShips) {
                    if ((animatedShip.orientation == Orientation.Horizontal && (animatedShip.rotatingInProgressDegree - 90.0).absoluteValue < ROTATING_DEGREE_PER_STEP) ||
                        (animatedShip.orientation == Orientation.Vertical && animatedShip.rotatingInProgressDegree.absoluteValue < ROTATING_DEGREE_PER_STEP)
                    ) {
                        animatedShip.rotatingInProgressDegree = if (animatedShip.orientation == Orientation.Horizontal) 90.0 else 0.0
                        shipsToRemove.add(animatedShip)
                    } else {
                        animatedShip.rotatingInProgressDegree +=
                            if (animatedShip.orientation == Orientation.Horizontal) ROTATING_DEGREE_PER_STEP else -ROTATING_DEGREE_PER_STEP
                    }
                }
                for (shipToRemove in shipsToRemove) {
                    rotatingShips.remove(shipToRemove)
                }
                if (rotatingShips.size == 0) {
                    stop()
                }
            }
        }

        translationTimer = object : AnimationTimer() {
            override fun handle(now: Long) {
                val shipsToRemove = mutableListOf<Ship>()
                for (animatedShip in translatingShips) {
                    animatedShip.offsetXInProgress += animatedShip.offsetXProgressionStep
                    animatedShip.offsetYInProgress += animatedShip.offsetYProgressionStep
                    if (animatedShip.offsetYInProgress.absoluteValue < animatedShip.offsetXProgressionStep.absoluteValue ||
                        animatedShip.offsetYInProgress.absoluteValue < animatedShip.offsetYProgressionStep.absoluteValue) {
                        animatedShip.offsetXInProgress = 0.0
                        animatedShip.offsetYInProgress = 0.0
                        shipsToRemove.add(animatedShip)
                    }
                }
                for (shipToRemove in shipsToRemove) {
                    translatingShips.remove(shipToRemove)
                }
                if (translatingShips.size == 0) {
                    stop()
                }
                draw()
            }
        }

        setOnMouseClicked {
            if (game.gameStateProperty.get() == GameState.HumanSetup) {
                if (it.button == MouseButton.PRIMARY) {
                    if (currentSelectedShip == null) { // selection mode
                        val selectedShipId = shipHitTest(it.x, it.y)
                        if (selectedShipId >= 0) {
                            currentSelectedShip = ships[selectedShipId]
                            currentSelectedShip!!.setOffset(it.x, it.y)
                            lastMX = it.x
                            lastMY = it.y
                            draw()
                        }
                    } else { // releasing/deselecting, i.e., currentSelectedShip != null
                        val bow = calculateBowPoint(currentSelectedShip!!)
                        if (bow != null) {
                            // Remove the ship first, and then re-place (if re-placing failed, place it in its original location)
                            if (currentSelectedShip!!.placedShipId != Cell.NoShip) {
                                game.removeShip(currentSelectedShip!!.placedShipId)
                            }
                            val shipId = game.placeShip(currentSelectedShip!!.shipType, currentSelectedShip!!.orientation, bow.first, bow.second)
                            if (shipId == Cell.NoShip) {
                                if (currentSelectedShip!!.placedShipId == Cell.NoShip) {
                                    returnToHarbour(currentSelectedShip!!)
                                } else {
                                    val newShipId = game.placeShip(currentSelectedShip!!.shipType, currentSelectedShip!!.orientation, currentSelectedShip!!.bowX, currentSelectedShip!!.bowY)
                                    currentSelectedShip!!.placedShipId = newShipId
                                    currentSelectedShip!!.reset()
                                }
                            } else {
                                currentSelectedShip!!.placeShip(bow.first, bow.second, shipId)
                                // check setup completes
                                if (game.getShipsPlacedCount(Player.Human) == ships.size) {
                                    game.humanReadyToStartAttack.value = true
                                }
                            }
                        } else { // outside the board: go back to Player Harbour (regardless if the ship has already currently placed)
                            if (currentSelectedShip!!.placedShipId != Cell.NoShip) {
                                game.removeShip(currentSelectedShip!!.placedShipId)
                            }
                            currentSelectedShip!!.placedShipId = Cell.NoShip
                            returnToHarbour(currentSelectedShip!!)
                        }
                        currentSelectedShip = null
                        draw()
                    }
                } else if (it.button == MouseButton.SECONDARY) {
                    if (currentSelectedShip != null) {
                        rotateCurrentSelectedShip(currentSelectedShip!!)
                    }
                }
            } else if (game.gameStateProperty.get() == GameState.HumanAttack) {
                if (it.button == MouseButton.PRIMARY) {
                    val relX = it.x - AI_BOARD_OFFSET - MARGIN
                    val relY = it.y - (height - BOARD_WIDTH - MARGIN)
                    if (relX >= 0.0 && relY >= 0.0) {
                        val col = (relX / Cell.Size).toInt()
                        val row = (relY / Cell.Size).toInt()
                        if (col in 0..9 && row in 0..9 &&!game.getCell(Player.Ai, col, row).wasAttacked) {
                            game.attackCell(col, row)
                            draw()
                        }
                    }
                }
            }
        }

        setOnMouseMoved {
            if (currentSelectedShip != null) {
                currentSelectedShip!!.offsetX += it.x - lastMX
                currentSelectedShip!!.offsetY += it.y - lastMY
                lastMX = it.x
                lastMY = it.y
                draw()
            }
        }

        game.gameStateProperty.addListener { _, _, newGameState ->
            centralLabel = when (newGameState) {
                GameState.HumanWon -> "You won!"
                GameState.AiWon -> "You were defeated!"
                else -> "My Fleet"
            }
            if (newGameState == GameState.HumanWon || newGameState == GameState.AiWon) {
                gameTerminating = true
                for (ship in ships) {
                    if (!game.isSunk(Player.Human, ship.placedShipId)) {
                        ship.placedShipId = Cell.NoShip
                        if (ship.orientation == Orientation.Horizontal) {
                            ship.rotate(false)
                        }
                        ship.reset()
                    }
                }
            }
            draw()
        }

        draw()
    }

    private fun returnToHarbour(ship: Ship) {
        game.humanReadyToStartAttack.value = false
        ship.setOffsetInProgress()
        ship.reset()
        translatingShips.add(ship)
        translationTimer.start()
        if (ship.orientation == Orientation.Horizontal) {
            rotateCurrentSelectedShip(ship)
        }
    }

    private fun rotateCurrentSelectedShip(ship: Ship) {
        ship.rotate()
        rotatingShips.add(ship)
        rotationTimer.start()
    }

    private fun calculateBowPoint(ship: Ship): Pair<Int, Int>? {
        var mx1 = ship.getImageX() + ship.offsetX
        var my1 = ship.getImageY() + ship.offsetY
        var mx2 = mx1 + ship.getImageWidth()
        var my2 = my1 + ship.getImageHeight()
        if (ship.orientation == Orientation.Horizontal) {
            val centralPoint = ship.getCentralPoint()
            mx1 = centralPoint.x + ship.offsetX - ship.getImageHeight() / 2
            my1 = centralPoint.y + ship.offsetY - ship.getImageWidth() / 2
            mx2 = mx1 + ship.getImageHeight()
            my2 = my1 + ship.getImageWidth()
        }
        val relX1 = mx1 - MARGIN
        val relY1 = my1 - (height - BOARD_WIDTH - MARGIN)
        val relX2 = mx2 - MARGIN
        val relY2 = my2 - (height - BOARD_WIDTH - MARGIN)

        val col1 = (relX1 / Cell.Size).toInt()
        val row1 = (relY1 / Cell.Size).toInt()
        val col: Int
        val row: Int
        if (ship.orientation == Orientation.Vertical) {
            val col2 = (relX2 / Cell.Size).toInt()
            val spaceX1 = Cell.Size.toInt() - relX1.toInt() % Cell.Size.toInt()
            val spaceX2 = relX2.toInt() % Cell.Size.toInt()
            col = if (col1 == col2 || spaceX1 > spaceX2) col1 else col2
            row = if (relY1.toInt() % Cell.Size.toInt() <= (Cell.Size / 2).toInt()) row1 else row1 + 1
        } else {
            val row2 = (relY2 / Cell.Size).toInt()
            val spaceY1 = Cell.Size.toInt() - relY1.toInt() % Cell.Size.toInt()
            val spaceY2 = relY2.toInt() % Cell.Size.toInt()
            row = if (row1 == row2 || spaceY1 > spaceY2) row1 else row2
            col = if (relX1.toInt() % Cell.Size.toInt() <= (Cell.Size / 2).toInt()) col1 else col1 + 1
        }

        return if (col in 0..9 && row in 0..9) {
            Pair(col, row)
        } else {
            null
        }
    }

    private fun shipHitTest(mx: Double, my: Double): Int {
        for ((idx, ship) in ships.withIndex()) {
            val x1 = ship.getImageX() + ship.offsetX +
                    if (ship.orientation == Orientation.Horizontal) (ship.getImageWidth() - ship.getImageHeight()) / 2 else 0.0
            val y1 = ship.getImageY() + ship.offsetY +
                    if (ship.orientation == Orientation.Horizontal) (ship.getImageHeight() - ship.getImageWidth()) / 2 else 0.0
            val x2 = x1 + if (ship.orientation == Orientation.Vertical) ship.getImageWidth() else ship.getImageHeight()
            val y2 = y1 + if (ship.orientation == Orientation.Vertical) ship.getImageHeight() else ship.getImageWidth()
            if (mx in x1..x2 && my in y1..y2) {
                return idx
            }
        }
        return -1
    }


    private fun draw() {
        gc.fill = Color.WHITE
        gc.fillRect(0.0, 0.0, width, height)
        gc.textAlign = TextAlignment.CENTER

        // Titles
        gc.font = Font(13.5)
        gc.textBaseline = VPos.TOP
        drawText("My Formation", MARGIN + BOARD_WIDTH / 2, 5.0)
        drawText(centralLabel, WIDTH / 2, 5.0)
        drawText("Opponent's Waters", WIDTH - MARGIN - BOARD_WIDTH / 2, 5.0)

        // Player's board frame
        gc.fill = Color.LIGHTBLUE
        gc.fillRect(MARGIN, height - MARGIN - BOARD_WIDTH, BOARD_WIDTH, BOARD_WIDTH)
        gc.font = Font(11.0)
        for (i in 0..10) {
            val y1 = height - BOARD_WIDTH - MARGIN
            val y2 = height - MARGIN
            gc.stroke = Color.BLACK
            gc.strokeLine(MARGIN + Cell.Size * i, y1, MARGIN + Cell.Size * i, y2)
            gc.strokeLine(MARGIN, y1 + Cell.Size * i, BOARD_WIDTH + MARGIN, y1 + Cell.Size * i)
            if (i > 0) {
                gc.textBaseline = VPos.BOTTOM
                drawText(i.toString(), MARGIN + Cell.Size * (i - 1) + Cell.Size / 2, y1 - 2.5)
                gc.textBaseline = VPos.TOP
                drawText(i.toString(), MARGIN + Cell.Size * (i - 1) + Cell.Size / 2, y2 + 2.5)
                gc.textBaseline = VPos.CENTER
                drawText(('A' + i - 1).toString(), MARGIN - 7.5, y1 + Cell.Size * (i - 1) + Cell.Size / 2)
                drawText(
                    ('A' + i - 1).toString(),
                    BOARD_WIDTH + MARGIN + 7.5,
                    y1 + Cell.Size * (i - 1) + Cell.Size / 2
                )
            }
        }
        // Player's board cells
        for ((rowId, cellRow) in game.getBoard(Player.Human).withIndex()) {
            for ((colId, cell) in cellRow.withIndex()) {
                gc.fill = when(cell) {
                    CellState.Ocean -> Color.LIGHTBLUE
                    CellState.Attacked -> Color.LIGHTGRAY
                    CellState.ShipHit -> Color.CORAL
                    else -> Color.DARKGRAY // CellState.ShipSunk
                }
                if (cell == CellState.Ocean) { // for performance consideration, no need to display OCEAN as it has been rendered above
                    continue
                }
                if (gameTerminating.not() || cell == CellState.ShipSunk) {
                    gc.fillRect(
                        colId * Cell.Size + MARGIN + 0.5,
                        rowId * Cell.Size + (height - BOARD_WIDTH - MARGIN) + 0.5,
                        Cell.Size - 1.0,
                        Cell.Size - 1.0
                    )
                }
            }
        }

        // AI's board frame
        gc.fill = Color.LIGHTBLUE
        gc.fillRect(AI_BOARD_OFFSET + MARGIN, height - MARGIN - BOARD_WIDTH, BOARD_WIDTH, BOARD_WIDTH)
        for (i in 0..10) {
            val y1 = height - BOARD_WIDTH - MARGIN
            val y2 = height - MARGIN
            gc.stroke = Color.BLACK
            gc.strokeLine(AI_BOARD_OFFSET + MARGIN + Cell.Size * i, y1, AI_BOARD_OFFSET + MARGIN + Cell.Size * i, y2)
            gc.strokeLine(
                AI_BOARD_OFFSET + MARGIN,
                y1 + Cell.Size * i,
                AI_BOARD_OFFSET + BOARD_WIDTH + MARGIN,
                y1 + Cell.Size * i
            )
            if (i > 0) {
                gc.textBaseline = VPos.BOTTOM
                drawText(i.toString(), AI_BOARD_OFFSET + MARGIN + Cell.Size * (i - 1) + Cell.Size / 2, y1 - 2.5)
                gc.textBaseline = VPos.TOP
                drawText(i.toString(), AI_BOARD_OFFSET + MARGIN + Cell.Size * (i - 1) + Cell.Size / 2, y2 + 2.5)
                gc.textBaseline = VPos.CENTER
                drawText(
                    ('A' + i - 1).toString(),
                    AI_BOARD_OFFSET + MARGIN - 7.5,
                    y1 + Cell.Size * (i - 1) + Cell.Size / 2
                )
                drawText(
                    ('A' + i - 1).toString(),
                    AI_BOARD_OFFSET + BOARD_WIDTH + MARGIN + 7.5,
                    y1 + Cell.Size * (i - 1) + Cell.Size / 2
                )
            }
        }

        // AI's board cells
        for ((rowId, cellRow) in game.getBoard(Player.Ai).withIndex()) {
            for ((colId, cell) in cellRow.withIndex()) {
                gc.fill = when(cell) {
                    CellState.Ocean -> Color.LIGHTBLUE
                    CellState.Attacked -> Color.LIGHTGRAY
                    CellState.ShipHit -> Color.CORAL
                    else -> Color.DARKGRAY // CellState.ShipSunk
                }
                if (cell == CellState.Ocean) { // for performance consideration, no need to display OCEAN as it has been rendered above
                    continue
                }
                if (gameTerminating.not() || cell == CellState.ShipSunk) {
                    gc.fillRect(
                        colId * Cell.Size + MARGIN + AI_BOARD_OFFSET + 0.5,
                        rowId * Cell.Size + (height - BOARD_WIDTH - MARGIN) + 0.5,
                        Cell.Size - 1.0,
                        Cell.Size - 1.0
                    )
                }
            }
        }

        // draw Player's ships
        for (ship in ships) {
            gc.stroke = Color.BLACK
            drawShip(ship)
        }
    }

   private fun drawShip(ship: Ship) {
        gc.save()
        if (ship.offsetXInProgress != 0.0 || ship.offsetYInProgress != 0.0) {
           gc.translate(ship.offsetXInProgress, ship.offsetYInProgress)
        } else {
           gc.translate(ship.offsetX, ship.offsetY)
        }
            if (ship.rotatingInProgressDegree != 0.0) {
                val center = ship.getCentralPoint()
                gc.translate(center.x, center.y)
                gc.rotate(-ship.rotatingInProgressDegree)
                gc.translate(-center.x, -center.y)
            }

        gc.drawImage(ship.image, ship.getImageX(), ship.getImageY(), ship.getImageWidth(), ship.getImageHeight())
        gc.restore()
   }

   private fun drawText(text : String, x: Double, y: Double, color : Color = Color.GRAY) {
        gc.stroke = color
        gc.fill = color
        gc.strokeText(text, x, y)
        gc.strokeText(text, x, y)
   }
}