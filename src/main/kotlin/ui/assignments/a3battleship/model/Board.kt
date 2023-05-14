package ui.assignments.a3battleship.model

class Board (private val dimension: Int) {

    // list of all cells, as a 1D array
    private val cells = List(dimension * dimension) { Cell(it % dimension, it / dimension) }

    // id of the last added ship
    private var lastShipId = Cell.NoShip

    /**
     * Places a ship on the board if it fits on the board.
     * @param shipType the type of the ship
     * @param orientation the orientation of the ship
     * @param bowX the x-coordinate of the cell in which the bow of the ship is located
     * @param bowY the y-coordinate of the cell in which the bow of the ship is located
     * @return the shipId of the newly placed ship, or [Cell.NoShip] if the ship was not placed
     */
    fun placeShip(shipType: ShipType, orientation : Orientation, bowX: Int, bowY: Int) : Int {
        val shipCanBePlaced = when (orientation) {
            Orientation.Horizontal -> {
                ((bowY in 0 until dimension) and (0 <= bowX) and (bowX + Game.shipLength[shipType]!! <= dimension)) and // does ship fit?
                cells.filter { (it.y == bowY) and (it.x in bowX until (bowX + Game.shipLength[shipType]!!)) }.all { it.shipId == Cell.NoShip } // are cells unoccupied?
            }
            Orientation.Vertical -> {
                ((bowX in 0 until dimension) and (0 <= bowY) and (bowY + Game.shipLength[shipType]!! <= dimension)) and // does ship fit?
                cells.filter { (it.x == bowX) and (it.y in bowY until (bowY + Game.shipLength[shipType]!!)) }.all { it.shipId == Cell.NoShip } // are cells unoccupied?
            }
        }
        if (shipCanBePlaced) {
            ++lastShipId // increment ship id
            when (orientation) {
                Orientation.Horizontal -> {
                    cells.filter { (it.y == bowY) and (it.x in bowX until (bowX + Game.shipLength[shipType]!!)) }.forEach { it.shipId = lastShipId }
                }
                Orientation.Vertical -> {
                    cells.filter { (it.x == bowX) and (it.y in bowY until (bowY + Game.shipLength[shipType]!!)) }.forEach { it.shipId = lastShipId }
                }
            }
            return lastShipId
        }
        return Cell.NoShip
    }

    /**
     * Removes a ship from the board.
     * @param shipId the id of the ship to be removed
     */
    fun removeShip(shipId: Int) {
        cells.filter { it.shipId == shipId }.forEach { it.shipId = Cell.NoShip }
    }

    /**
     * Attacks the cell.
     * @param x: the x-coordinate of the attacked cell
     * @param y: the y-coordinate of the attacked cell
     */
    fun attackCell(x: Int, y: Int) {
        if ((x in 0 until dimension) and (y in 0 until dimension))
            cells.find { (it.x == x) and (it.y == y) }!!.wasAttacked = true
    }

    fun getCell(x: Int, y: Int): Cell {
        return cells[y * dimension + x]
    }

    /**
     * Returns if there is at least one ship afloat on the board.
     * @return true if there is least one ship afloat, and false otherwise
     */
    fun hasShips() : Boolean {
        return cells.count { (it.shipId != Cell.NoShip) and (!it.wasAttacked) } > 0
    }

    /**
     * Returns if a particular ship was sunk.
     * @param shipId the id of the ship queried
     * @return true if the ship was sunk, and false otherwise
     */
    fun isSunk(shipId: Int) : Boolean {
        return cells.count { (it.shipId == shipId) and (!it.wasAttacked) } == 0
    }

    /**
     * Returns the number of ships that are currently placed.
     * @return the number of ships
     */
    fun getPlacedShipCount() : Int {
        return cells.map { it.shipId }.toSet().count() - 1 // -1 because we do not count Cell.NoShip
    }

    /**
     * Returns the current state of the board.
     * @return a 2d-list of all cell states; first (outer) dimension represents the y-coordinate, second (inner) one the x-coordinate
     */
    fun getBoardStates(debug: Boolean = false) : List<List<CellState>> {
        if (debug) {
//            println()
//            println("private board (coordinate, ship id, attacked?):\n${cells.chunked(dimension).fold("") { acc, cur -> "$acc${cur.fold("") { acc2, cur2 -> "$acc2\t$cur2" } }\n" }}")
            for ((idx, cell) in cells.withIndex()) {
                if (idx % 10 == 0 && idx > 0) {
                    println()
                }
                if (cell.shipId == Cell.NoShip) {
                    print("-- ")
                } else {
                    print("${String.format("%02d ", cell.shipId)}")
                }
            }
        }

        return cells.map {
            if ((it.shipId != Cell.NoShip) and (isSunk(it.shipId)))
                CellState.ShipSunk
            else if ((it.shipId != Cell.NoShip) and isSunk(it.shipId).not() and (it.wasAttacked))
                CellState.ShipHit
            else if (it.wasAttacked)
                CellState.Attacked
            else
                CellState.Ocean
        }.chunked(dimension)
    }
}