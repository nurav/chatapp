package chatapp;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Varun Joshi on 10/2/2014.
 *
 * This class defines various items -
 * - A callback interface (Server.ReceiverCallback) to access the controller
 * - A ClientThread inner class, whose objects are used to keep track of and communicate with various clients
 * - A ChatObject class, that extends the Serializable interface. Objects of this class are used for communication between client and server.
 *
 *
 */
public class Server extends Thread {
    //Default port, chosen randomly
    public static final int PORT_NO = 7643;
    private ServerSocket serverSocket;
    private List<ClientThread> clientList = new ArrayList<ClientThread>();
    ReceiverCallback callback;
    String address;
    private boolean run;

    public Server(String url, ReceiverCallback callback) {
        address = url.toString();
        this.callback = callback;
    }

    public Server(String url) {
        address = url;
    }

    //Executed after the server thread is started. Sets up the server to accept client connections.
    public void startReceiving() throws IOException {
        run = true;

        //Set socket to reuse address
        this.serverSocket = new ServerSocket();
        this.serverSocket.setReuseAddress(true);
        this.serverSocket.bind(new InetSocketAddress(PORT_NO));

        //Platform.runLater is used so that operations are performed on the UI thread.
        //Here, if the server is successfully created, this updates the UI.
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onHostSuccessfullyCreated();
                    String localIp = "";
                    InetAddress localhost = Inet4Address.getLocalHost();
                    InetAddress[] allIps = Inet4Address.getAllByName(localhost.getCanonicalHostName());
                    for(InetAddress address1 : allIps) {
                        String add = address1.toString().split("/")[1];
                        System.out.println(add);
                        if(add.split("\\.").length >= 2) {
                            if (add.split("\\.")[0].equals("192") && add.split("\\.")[2].equals("1")) {
                                localIp = address1.toString();
                            }
                        }
                    }
                    if(localIp.equals("")) {
                        localIp = Inet4Address.getLocalHost().toString();
                    }
                    callback.onReceivedData("Server successfully started at " + localIp);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //Loop to check for incoming connections from clients.
        while (run) {
            Socket connectionSocket = this.serverSocket.accept();
            ClientThread client = new ClientThread(connectionSocket);
            System.out.println("\nconnected");
            clientList.add(client);

            client.start();
        }


    }

    public void stopReceiving() throws IOException{
        run = false;
        for(ClientThread thread : clientList) {
            thread.socket.close();
        }

        serverSocket.close();
    }

    //This sends the ChatObject message to all connected clients.
    public synchronized void broadcast(ChatObject message) {
        //Used to get the date in a readable format.
        String time = new SimpleDateFormat().format(new Date());

        String sentMessage = time + " " + message + "\n";

        for (ClientThread client : clientList) {
            if(message instanceof ChatObject)
                client.sendMessage(message);
            else
                client.sendMessage(sentMessage);
        }
    }

    //Overloaded method to allow for communication using Strings.
    public synchronized void broadcast(String message) {
        String time = new SimpleDateFormat().format(new Date());

        String sentMessage = time + " " + message + "\n";

        for (ClientThread client : clientList) {
            client.sendMessage(sentMessage);
        }
    }


    synchronized void remove(int id) {
        for (ClientThread client : clientList) {
            if (client.id == id) {
                clientList.remove(client);
                return;
            }
        }
    }

    //This interface is used for communication with the controller class.
    public static interface ReceiverCallback {
        public void onReceivedData(String data);
        public void createAlertDialog(String message);
        public void setSuccess(boolean success);
        public void setServerNull();
        public void setClientNull();
        public void onHostSuccessfullyCreated();
    }

    //A serializable class to allow communication over the network.
    //Has fields for type (file or message), message and data.
    public static class ChatObject implements Serializable {
        static final int MESSAGE = 0, FILE = 1, LIST = 2;

        private int messageType;
        private String messageText;
        private byte[] data;
        private String fileName;

        public ChatObject(int messageType, String messageText) {
            this.messageType = messageType;
            this.messageText = messageText;
        }

        //Getters and setter from here
        public int getMessageType() {
            return messageType;
        }

        public String getMessageText() {
            return messageText;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public byte[] getData(byte[] data) {
            return this.data;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }

    @Override
    public void run() {
        try {
            startReceiving();
        } catch (BindException e) {
            e.printStackTrace();

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    callback.setSuccess(false);
                    callback.setServerNull();
                    callback.createAlertDialog("A host is already running on this machine.");
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Class whose objects keep track of clients
    class ClientThread extends Thread {
        int id;
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;
        private Socket socket;
        private String time;
        private String clientName;

        public ClientThread(Socket socket) throws IOException {
            this.socket = socket;
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.flush();
                inputStream = new ObjectInputStream(socket.getInputStream());

                clientName = (String) inputStream.readObject();
                broadcast(clientName + " has joined");
            } catch (Exception e) {
                e.printStackTrace();
            }

            time = (new Date()).toString() + "\n";
        }

        public void run() {
            boolean run = true;
            ChatObject object = null;
            while (run) {
                try {
                    object = (ChatObject) inputStream.readObject();
                } catch (Exception e) {
                    clientList.remove(ClientThread.this);
                    e.printStackTrace();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            callback.onReceivedData((new SimpleDateFormat()).format(new Date()) +
                                    " " +
                                    ClientThread.this.clientName +
                                    " has logged out"
                            );
                        }
                    });
                    return;
                }

                final String message = object.getMessageText();

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        callback.setSuccess(false);
                    }
                });

                switch (object.getMessageType()) {
                    case ChatObject.MESSAGE:
                        broadcast(clientName + " : " + message);
                        break;
                    case ChatObject.FILE:
                        broadcast(clientName + " has sent a file (" + object.getFileName() + ")");
                        broadcast(object);
                        break;
                    case ChatObject.LIST:
                        String s = "Connected users -\n";
                        for(ClientThread client : clientList) {
                            s += client.clientName + "\n";
                        }
                        broadcast(s);
                }
            }
        }

        private boolean sendMessage(ChatObject message) {
            if (!socket.isConnected()) {
                //TODO: close everything
                return false;
            }

            try {
                outputStream.writeObject(message);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }

        private boolean sendMessage(String message) {
            if (!socket.isConnected()) {
                //TODO: close everything
                return false;
            }

            try {
                outputStream.writeObject(message);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }
    }
}
