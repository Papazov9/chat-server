package server.handlers;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

import server.properties.PropertiesManager;
import server.ChatServer;

/*
 * ConnectionHandler.java
 *
 * created at 2022-10-06 by b.papazov <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */


public class ConnectionHandler extends BaseHandler implements Runnable
{
    private String username;
    private Long timeInactive;
    private Long maximumTimeInactive;
    private boolean isClosed;
    private AtomicBoolean isShutdownActivated = new AtomicBoolean(false);

    // when it is directly connected to the chat (without waiting in the queue)
    public ConnectionHandler(Socket socket, Socket fileSocket)
    {
        super(socket, fileSocket);
        this.isClosed = false;
        this.timeInactive = System.currentTimeMillis();
        this.maximumTimeInactive = Long.parseLong(PropertiesManager.getInstance().propertiesMap.get("timeToLive")) * 1000;
        try
        {
            this.username = this.in.readLine();
        }
        catch (IOException e)
        {
            closeEverything();
            e.printStackTrace();
        }
        ChatServer.connections.add(this);
        writeToClient("Welcome!");
        broadcastMessage(this.username + " has entered the chat!", "NOT ALL");
    }


    // when it is connected to the chat after some time in the waiting queue
    public ConnectionHandler(Socket socket, String username, Socket fileSocket)
    {
        super(socket, fileSocket);
        this.isClosed = false;
        this.username = username;
        this.timeInactive = System.currentTimeMillis();
        this.maximumTimeInactive = Long.parseLong(PropertiesManager.getInstance().propertiesMap.get("timeToLive")) * 1000;
        this.socket = socket;
        ChatServer.connections.add(this);
        writeToClient("Welcome!");
        broadcastMessage(this.username + " has entered the chat!", "NOT ALL");
    }


    @Override
    public void run()
    {
        String clientMessage;

        while (!socket.isClosed())
        {
            try
            {
                if (System.currentTimeMillis() - timeInactive >= maximumTimeInactive)
                {
                    broadcastMessage("Kicked out of the server due to inactivity!", this.username);
                    closeEverything();
                }

                if (this.in.ready())
                {
                    this.timeInactive = System.currentTimeMillis();
                    clientMessage = in.readLine();
                    handleMessage(clientMessage);
                }

            }
            catch (IOException e)
            {
                ChatServer.logger.error("Error trying to access the input stream.");
                closeEverything();
                break;
            }
        }
    }


    /**
     * Handle different kind of messages received from the client's socket.
     * Check if it is a specific command or just a regular message.
     *
     * @param clientMessage - message received from the client
     */
    private void handleMessage(String clientMessage)
    {
        String[] commands = clientMessage.split(" ", 3);

        switch (commands[0])
        {
        case "EXIT":
            closeEverything();
            break;

        case "TIME":
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy HH:mm:ss");
            writeToClient("Current time is: " + now.format(formatter));
            break;

        case "SEND":
            if (isShutdownActivated.get())
            {
                writeToClient("Unable to get files from the server! Server is shutdowning...");
            }
            else
            {
                ChatServer.asyncPool.execute(recieveFileFromUser(commands));
            }

            break;

        case "FILE-GET":

            if (isShutdownActivated.get())
            {
                writeToClient("Unable to get files from the server! Server is shutdowning...");
            }
            else
            {
                ChatServer.asyncPool.execute(sendFileToUser(commands));

            }
            break;

        case "ADMIN:DRAIN":
            broadcastMessage("The server will stop working after 1 minute!", "ALL");
            isShutdownActivated.set(true);
            stopServer();
            break;

        case "ADMIN:KILL":
            String userToKill = commands[1];
            removeByName(userToKill);
            break;

        case "LIST-FILES":
            File filesDirectory = new File(PropertiesManager.getInstance().propertiesMap.get("filesDirectory"));
            String[] list = filesDirectory.list();
            writeToClient("List of all Files:");
            for (String name : list)
            {
                writeToClient("- " + name);
            }
            break;
        default:
            broadcastMessage(this.username + ": " + clientMessage, "NOT ALL");
            break;

        }
    }


    private Runnable recieveFileFromUser(String[] commands)
    {
        return () ->
        {
            int length = Integer.parseInt(commands[2]);
            if (ChatServer.repository.addFile(commands[1], length, fileInput))
            {
                broadcastMessage(String.format("The file with name: %s was sent to the server from user: %s!%n", commands[1],
                                               username),
                                 "ALL");
            }
            else
            {
                writeToClient(String.format("Unable to save this file!%n", username));
            }
        };
    }


    /**
     * Remove client from the group chat by username if the user exists.
     * userToKill - username of the client to be kicked
     */
    private void removeByName(String userToKill)
    {
        if (!this.username.equals(userToKill))
        {
            ConnectionHandler ch = ChatServer.connections.stream().filter(c -> c.username.equals(userToKill)).findFirst().orElse(null);
            if (ch != null)
            {
                ch.closeEverything();
            }
        }

    }


    /**
     * Send a file from the server directory to a client if the file exists.
     *
     * @param commands[] - array of the parts of given command from the client
     */
    private Runnable sendFileToUser(String[] commands)
    {
        return new Runnable()
        {

            @Override
            public void run()
            {

                File result = null;
                FileInputStream fis = null;
                try
                {
                    result = ChatServer.repository.getFileByKey(commands[1]);
                    if (result == null)
                    {
                        throw new FileNotFoundException();
                    }
                    fis = new FileInputStream(result);
                    writeToClient("Sending...");
                    writeToClient("SENDING " + result.getName() + " " + result.length());
                    int ch;
                    while ((ch = fis.read()) != -1)
                    {
                        fileOutput.write(ch);
                    }
                    fileOutput.flush();
                }
                catch (FileNotFoundException e1)
                {
                    ChatServer.logger.error("File was not found in the repository!");
                    writeToClient("File not found in the repository!");
                }
                catch (IOException e)
                {
                    ChatServer.logger.error(e.getMessage(), e);
                    writeToClient("File not found in the repository!");
                }
                finally
                {
                    if (fis != null)
                    {
                        try
                        {
                            fis.close();
                        }
                        catch (IOException e)
                        {
                            ChatServer.logger.error(e.getMessage(), e);
                        }
                    }
                }

            }
        };
    }


    /**
     * Sending a message to specific group of clients, it depends on the type param.
     *
     * @param clientMessage - message to send to the clients
     * @param type          - specification of group of clients to which the message has to be send
     */
    private void broadcastMessage(String clientMessage, String type)
    {
        for (ConnectionHandler cHandler : ChatServer.connections)
        {
            try
            {
                if (type.equals("NOT ALL"))
                {
                    if (!cHandler.username.equals(username))
                    {
                        cHandler.out.write(clientMessage);
                        cHandler.out.newLine();
                        cHandler.out.flush();
                    }
                }
                else
                {
                    cHandler.out.write(clientMessage);
                    cHandler.out.newLine();
                    cHandler.out.flush();
                }

            }
            catch (IOException e)
            {
                closeEverything();
            }

        }
    }


    @Override
    public void closeEverything()
    {

        if (!isClosed)
        {
            future.cancel(true);
            try
            {
                if (this.socket != null)
                {
                    if (!isShutdownActivated.get())
                    {
                        ChatServer.connections.remove(this);
                        broadcastMessage(username + " left the chat!", "NOT ALL");
                        System.out.println(this.username + " left the chat!");
                    }
                    this.socket.close();

                }
                if (this.in != null)
                {
                    this.in.close();
                }

                if (this.out != null)
                {
                    this.out.close();
                }

                if (this.fileInput != null)
                {
                    this.fileInput.close();
                }

                if (this.fileOutput != null)
                {
                    this.fileOutput.close();
                }
                this.isClosed = true;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }


    /**
     * Stop the server if drain command is invoked.
     */
    private void stopServer()
    {
        ChatServer.closeServerSocket();
        closeAllConnections();
    }


    /**
     * Close all client connections when the drain is invoked.
     */
    private void closeAllConnections()
    {
        new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(1000);
                    for (ConnectionHandler cHandler : ChatServer.connections)
                    {
                        cHandler.closeEverything();
                    }
                }
                catch (InterruptedException e)
                {
                    ChatServer.logger.error(e.getMessage(), e);
                }

            }
        }).start();

    }
}
