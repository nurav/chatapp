package chatapp;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

/**
 * Created by The Joshis on 10/3/2014.
 * Uses ObjectStreams to communicate with server.
 * Defines ListenerThread. A ListenerThread listens for incoming data and informs Client when data is available from server.
 * Uses a callback to communicate with controller
 */
public class Client extends Thread {
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    private int port;
    private String server;
    public String username;
    private Socket socket;
    private Server.ReceiverCallback callback;

    Client(String server, int port, String username, Server.ReceiverCallback callback) {
        this.server = server;
        this.username = username;
        this.port = port;
        this.callback = callback;
    }

    public void startClient() {
        try {
            socket = new Socket(server, port);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    callback.onReceivedData("You have successfully connected");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    callback.setClientNull();
                    callback.createAlertDialog("Invalid address!");
                }
            });
        }

        System.out.println("connection accepted by " + socket.getInetAddress());
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            outputStream.writeObject(username);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new ListenerThread(inputStream).start();
    }

    public void run() {
        startClient();
    }

    public void sendMessage(Server.ChatObject message) {
        try {
            outputStream.writeObject(message);
            System.out.println(message.getFileName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ListenerThread extends Thread {
        ObjectInputStream inputStream;
        boolean run = true;

        public ListenerThread(ObjectInputStream inputStream) {
            ListenerThread.this.inputStream = inputStream;
        }

        public void run() {
            while (run) {
                try {
                    Server.ChatObject chatObject = null;
                    final Object object = ListenerThread.this.inputStream.readObject();
                    if(object instanceof Server.ChatObject) {
                        chatObject = (Server.ChatObject) object;
                        final String message = chatObject.getMessageText();
                        switch (chatObject.getMessageType()) {
                            case Server.ChatObject.FILE :
                                new File(System.getProperty("user.home") + File.separator + "Chat Files").mkdirs();
                                File toWrite = new File(System.getProperty("user.home") +
                                        File.separator +
                                        "Chat Files" +
                                        File.separator +
                                        ((Server.ChatObject) object).getFileName());
                                if(toWrite.exists()) {
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            callback.createAlertDialog("Name conflicts. Overwriting.");
                                        }
                                    });
                                }
                                FileOutputStream writer = new FileOutputStream(toWrite);
                                writer.write(chatObject.getData());
                                writer.close();
                                break;
                            case Server.ChatObject.MESSAGE :
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onReceivedData(message);
                                    }
                                });
                        }
                    }
                    else Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            callback.onReceivedData((String) object);
                        }
                    });





                } catch (Exception e) {
                    run = false;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            callback.setClientNull();
                            callback.createAlertDialog("Server has stopped!");
                            Platform.exit();
                        }
                    });

                    e.printStackTrace();

                }
            }
        }
    }
}
