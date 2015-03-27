/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatapp;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.swing.*;

/**
 * @author The Joshis
 */
public class ChatApp extends Application {
    private static ChatApp instance;
    Scene scene;
    public Stage stage;

    public ChatApp() {
        instance = this;
    }

    public static ChatApp getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));

        scene = new Scene(root);

        stage.setScene(scene);
        stage.setHeight(454);
        stage.setWidth(606);
        stage.setResizable(false);
        stage.setTitle("Chat");
        stage.setOnHiding(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                try {
                    if(Controller.server != null)
                        Controller.server.stopReceiving();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
