package ui.assignments.a3battleship.model

import javafx.geometry.Point2D
import javafx.scene.image.Image
import ui.assignments.a3battleship.UI
import java.io.FileInputStream


class Ship(
    private val x: Double, private val y: Double, val shipType: ShipType,
    var orientation: Orientation = Orientation.Vertical) {
    var offsetX = 0.0
    var offsetY = 0.0
    var placedShipId = Cell.NoShip
    var bowX = -1
    var bowY = -1

    var rotatingInProgressDegree = 0.0
    var offsetXInProgress = 0.0
    var offsetYInProgress = 0.0
    var offsetXProgressionStep = 0.0
    var offsetYProgressionStep = 0.0

    private val width = when(shipType) {
        ShipType.Destroyer -> 15.0
        ShipType.Cruiser -> 20.0
        ShipType.Submarine -> 15.0
        ShipType.Battleship -> 25.0
        else -> 25.0
    }
    val image = when(shipType) {
        ShipType.Destroyer -> Image(FileInputStream("./src/resources/destroyer.png"))
        ShipType.Cruiser -> Image(FileInputStream("./src/resources/cruiser.png"))
        ShipType.Submarine -> Image(FileInputStream("./src/resources/submarine.png"))
        ShipType.Battleship -> Image(FileInputStream("./src/resources/battleship.png"))
        else -> Image(FileInputStream("./src/resources/carrier.png"))
    }

    private val vMargin = 5.0

    fun setOffsetInProgress() {
        offsetXInProgress = offsetX
        offsetYInProgress = offsetY
        offsetXProgressionStep = -offsetXInProgress / 10.0
        offsetYProgressionStep = -offsetYInProgress / 10.0
    }

    fun reset() {
        val boardX = UI.MARGIN
        val boardY = UI.HEIGHT - UI.BOARD_WIDTH - UI.MARGIN

        if (placedShipId != Cell.NoShip) {
            offsetX = bowX * Cell.Size + boardX - x
            offsetY = bowY * Cell.Size + boardY - y
            if (orientation == Orientation.Horizontal) {
                offsetX += (Game.shipLength[shipType]!! * Cell.Size - Cell.Size) / 2
                offsetY += (Cell.Size - Game.shipLength[shipType]!! * Cell.Size) / 2
            }
        } else {
            offsetX = 0.0
            offsetY = 0.0
        }
    }

    fun placeShip(bowX: Int, bowY: Int, shipId: Int) {
        placedShipId = shipId
        this.bowX = bowX
        this.bowY = bowY
        reset()
    }

    fun rotate(animation: Boolean = true) {
        this.orientation = when(orientation) {
            Orientation.Vertical -> Orientation.Horizontal
            else -> Orientation.Vertical
        }
        if (!animation) {
            rotatingInProgressDegree = if (orientation == Orientation.Vertical) 0.0 else 90.0
        }
    }

    fun getImageX() : Double {
        return x + (Cell.Size - width) / 2
    }

    fun getImageY() : Double {
        return y + vMargin
    }

    fun getImageWidth() : Double {
        return width
    }

    fun getImageHeight() : Double {
        return Game.shipLength[shipType]!! * Cell.Size - vMargin * 2
    }

    fun getCentralPoint() : Point2D {
        return Point2D(x + Cell.Size / 2, y + Game.shipLength[shipType]!! * Cell.Size / 2)
    }

    fun setOffset(mx: Double, my: Double) {
        offsetX = mx - (x + Cell.Size / 2)
        offsetY = my - (y + Game.shipLength[shipType]!! * Cell.Size / 2)
    }

}