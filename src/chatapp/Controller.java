/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatapp;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;
import java.util.concurrent.Future;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

/**
 * @author The Joshis
 */
public class Controller implements Initializable, Server.ReceiverCallback {

    @FXML
    private Label label;
    @FXML
    private Button sendbutton;
    @FXML
    private ListView chatview;
    @FXML
    private TextField textarea;

    private ObservableList<String> chatItems = FXCollections.observableArrayList();
    protected static Server server;
    protected static Client serverClient;
    protected static Client client;
    private boolean success = true;

    @FXML
    private void onConnectToServer(ActionEvent event) {
        if(client != null) {
            createAlertDialog("You are already connected to a client");
            return;
        }
        if(server != null) {
            createAlertDialog("You are hosting a server");
            return;
        }
        final Stage dialogStage = new Stage();

        GridPane gridPane = new GridPane();

        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(ChatApp.getInstance().stage);
        gridPane.setAlignment(Pos.BASELINE_CENTER);
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(25, 25, 25, 25));

        Text ip = new Text("Connect to Server");
        ip.setFont(javafx.scene.text.Font.font("Tahoma", FontWeight.NORMAL, 20));
        gridPane.add(ip, 0, 0, 2, 1);

        Text address = new Text("Address:");
        gridPane.add(address, 0, 1);
        address.setFocusTraversable(true);

        Text userName = new Text("Username:");
        gridPane.add(userName, 0, 2);

        final TextField userNameField = new TextField();
        gridPane.add(userNameField, 1, 2);

        final TextField addressText = new TextField();
        gridPane.add(addressText, 1, 1);

        Button button = new Button("OK");
        button.setAlignment(Pos.CENTER);
        gridPane.add(button, 1, 3);

        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(!addressText.getText().equals("") && !userNameField.getText().equals("")) {
                    connectToServer(addressText.getText(), Server.PORT_NO, userNameField.getText());
                    dialogStage.close();
                }
                else createAlertDialog("Username and/or address cannot be blank!");
            }
        });

        dialogStage.setScene(new Scene(gridPane));
        dialogStage.setResizable(false);
        dialogStage.show();

    }

    @FXML
    private void onClose(ActionEvent event) {
        try {
            if(server != null)
                server.stopReceiving();
            Platform.exit();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onHostServer(ActionEvent event) {
        if(server != null) {
            createAlertDialog("Server already running!");
            return;
        }

        server = new Server("localhost", this);
        server.start();
    }

    public void onHostSuccessfullyCreated() {
        serverClient = new Client(server.address, Server.PORT_NO, "SERVER", this);
        serverClient.start();
    }

    @FXML
    private void onShowAbout(ActionEvent event) {
        createAlertDialog("ChatApp 1.0\nCreated by Varun Joshi, Siddharth Maheshwari and Neelesh Jayaraman\n");
    }

    @FXML
    private void sendMessage(ActionEvent event) {
        String text = textarea.getText();
        textarea.clear();

        if(text == "") return;

        if (client == null && server != null) {
            server.broadcast("SERVER : " + text);
        } else if (client != null)
            client.sendMessage(new Server.ChatObject(Server.ChatObject.MESSAGE, text));
    }

    @FXML private void onSendFile(ActionEvent event) throws FileNotFoundException, IOException {
        if(checkIfServerOrConnected()) {
            createAlertDialog("Connect to server or host one first!");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose file to send");

        File file = chooser.showOpenDialog(ChatApp.getInstance().stage);

        if(file == null) return;

        Server.ChatObject fileObject = new Server.ChatObject(Server.ChatObject.FILE, "New file sent");
        fileObject.setData(Files.readAllBytes(file.toPath()));
        fileObject.setFileName(file.getName());
        System.out.println(file.getName());

        if(client == null && server != null)
            serverClient.sendMessage(fileObject);
        else if(client != null)
            client.sendMessage(fileObject);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chatItems.add("You are not connected to any server. Please host a server or connect to one.");
        chatview.setItems(chatItems);
        ChatApp.getInstance().stage.setResizable(false);
    }

    public void onReceivedData(String data) {
        chatItems.add(data);
        System.out.println("recvd");
        chatview.setItems(chatItems);
    }

    private void connectToServer(String server, int port, String username) {
        if(!chatItems.isEmpty())
            chatItems.remove(0);
        Client client = new Client(server, port, username, this);
        client.start();
        this.client = client;
    }

    public void createAlertDialog(String message) {
        final Stage alertDialogStage = new Stage();
        alertDialogStage.initOwner(ChatApp.getInstance().stage);
        alertDialogStage.initModality(Modality.WINDOW_MODAL);

        Text text = new Text(message);
        Button button = new Button("OK");
        button.setAlignment(Pos.CENTER_RIGHT);
        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(10));
        vBox.setSpacing(10);

        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                alertDialogStage.close();
            }
        });

        vBox.getChildren().addAll(text, button);

        Scene scene = new Scene(vBox);
        alertDialogStage.setResizable(false);
        alertDialogStage.setScene(scene);

        alertDialogStage.show();
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setServerNull() {
        chatItems = FXCollections.observableArrayList();
        server = null;
        serverClient = null;
    }

    public void setClientNull() {
        chatItems = FXCollections.observableArrayList();
        client = null;
    }

    @FXML private void listUsers(ActionEvent event) {
        if(checkIfServerOrConnected()) {
            createAlertDialog("Connect to server or host one first!");
            return;
        }
        if(client != null) {
            client.sendMessage(new Server.ChatObject(Server.ChatObject.LIST, ""));
        }
        else if(serverClient != null) {
            serverClient.sendMessage(new Server.ChatObject(Server.ChatObject.LIST, ""));
        }
    }

    @FXML private void saveToFile(ActionEvent event) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter (new FileWriter(System.getProperty("user.home") + File.separator + "Chat Files" + File.separator + "chat.txt"));

            for(String line : chatItems) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }

            bufferedWriter.close();
            createAlertDialog("File saved!");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void sendAudioFile(ActionEvent event) {
        if(checkIfServerOrConnected()) {
            createAlertDialog("Connect to server or host one first!");
            return;
        }
        try {
            AudioCapture audioCapture = new AudioCapture(ChatApp.getInstance().stage);
            byte[] data = audioCapture.show();
            Server.ChatObject object = new Server.ChatObject(Server.ChatObject.FILE, "I've sent audio!");
            System.out.println(data.length);
            object.setData(data);
            if(client != null)
                object.setFileName(client.username + "'s audio.wav");
            else object.setFileName(serverClient.username + "'s audio.wav");
            if(client != null)
                client.sendMessage(object);
            else if(serverClient != null)
                serverClient.sendMessage(object);
            System.out.println("Audio sent");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkIfServerOrConnected() {
        if(server == null && client == null)
            return true;
        else return false;
    }

    @FXML private void sendScreen(ActionEvent event) {
        if(checkIfServerOrConnected()) {
            createAlertDialog("Connect to server or host one first!");
            return;
        }


    }
}
