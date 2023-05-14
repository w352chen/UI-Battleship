package ui.assignments.a3battleship

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import ui.assignments.a3battleship.model.Game

class Battleship : Application() {
    override fun start(stage: Stage) {
        val game = Game(10, false)
        val player = UI(game)
        val computer = AI(game)

        val startButton = Button("Start Game")
        startButton.apply {
            prefWidth = 120.0
            onAction = EventHandler {
                game.humanReadyToStartAttack.value = false
                game.startGame()
            }
            disableProperty().bind(game.humanReadyToStartAttack.not())
        }

        val exitButton = Button("Exit Game")
        exitButton.apply {
            onAction = EventHandler {
                Platform.exit()
            }
        }
        val vbox = VBox(startButton, exitButton)
        vbox.apply {
            maxWidth = 125.0
            maxHeight = 55.0
            translateX = 0.0
            translateY = 135.0
        }

        exitButton.prefWidth = 120.0
        val pane = StackPane(player, vbox)

        game.startGame()

        stage.apply {
            scene = Scene(pane, 875.0, 375.0)
            title = "CS349 - A3 Battleship - w352chen"
        }.show()
    }
}