package ui.assignments.a3battleship.model

/**
 * Cell represent a single cell on the board.
 * @param x represents the x-coordinate od the cell
 * @param y represents the y-coordinate od the cell
 * @param shipId the id of a present ship, or [Cell.NoShip] if no ship iis present
 * @param wasAttacked true if the cell has been attacked, or false otherwise
 */
data class Cell(val x: Int, val y: Int, var shipId: Int = NoShip, var wasAttacked: Boolean = false) {
    companion object {
        const val NoShip = -1
        const val Size = 30.0
    }

    /**
     * Returns a string representation of the [Cell].
     */
    override fun toString(): String {
        return "($x,$y) ${if (shipId == NoShip) "--" else String.format("%02d", shipId)} ${if (wasAttacked) "Y" else "N"}"
    }
}
