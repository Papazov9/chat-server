package server.handlers;
/*
 * BaseHandler.java
 *
 * created at 2022-10-17 by devadm <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.Future;

import server.ChatServer;

abstract class BaseHandler
{
    protected Socket socket;
    protected Socket fileSocket;
    protected Future< ? > future;
    protected BufferedReader in;
    protected BufferedWriter out;
    protected BufferedInputStream fileInput;
    protected BufferedOutputStream fileOutput;

    protected BaseHandler(Socket socket, Socket fileSocket)
    {
        this.socket = socket;
        this.fileSocket = fileSocket;

        try
        {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.fileInput = new BufferedInputStream(this.fileSocket.getInputStream());
            this.fileOutput = new BufferedOutputStream(this.fileSocket.getOutputStream());
        }
        catch (IOException e)
        {
            ChatServer.logger.error(e.getMessage(), e);
        }
    }

    /**
     * Closes the connection between the server and the client.
     */
    abstract void closeEverything();

    public void setFuture(Future< ? > future)
    {
        this.future = future;
    }


    /**
     * Send a message to the client connected to the current connection instance
     */
    protected void writeToClient(String message)
    {
        try
        {
            this.out.write(message);
            this.out.newLine();
            this.out.flush();
        }
        catch (IOException e)
        {
            ChatServer.logger.error(e.getMessage(), e);
        }
    }
}



