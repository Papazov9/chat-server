package server.handlers;


import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import server.ChatServer;
import server.license.LicenseManager;
import server.license.LicenseManagerImpl;

/*
 * WaitHandler.java
 *
 * created at 2022-10-06 by b.papazov <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */


public class WaitHandler extends BaseHandler implements Runnable
{
    private String username;
    private int timeOutCounter;
    private LicenseManager lisLicenseManager;
    public static List<WaitHandler> waitingList = Collections.synchronizedList(new ArrayList<WaitHandler>());

    public WaitHandler(Socket socket, Socket fileSocket)
    {
        super(socket, fileSocket);
        try
        {
            this.username = in.readLine();
            this.timeOutCounter = 0;
            this.lisLicenseManager = new LicenseManagerImpl();
            waitingList.add(this);
        }
        catch (IOException e)
        {
            future.cancel(true);
            closeEverything();
        }

    }


    @Override
    public void run()
    {

        if (this.timeOutCounter == 10)
        {
            try
            {
                future.cancel(true);
                this.out.write("You are disconnected! Too much time on the queue!");
                this.out.newLine();
                this.out.flush();
                closeEverything();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            checkForSpace();
            this.timeOutCounter++;
        }
    }


    @Override
    public void closeEverything()
    {
        waitingList.remove(this);
        try
        {
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

            if (this.socket != null)
            {
                this.socket.close();
            }
        }
        catch (IOException e)
        {
            ChatServer.logger.error(e.getMessage(), e);
        }

    }


    /**
     * Check if there is a free spot in the chat server
     */
    private void checkForSpace()
    {
        if (lisLicenseManager.verify(ChatServer.connections.size()))
        {
            future.cancel(true);
            ConnectionHandler handler = new ConnectionHandler(socket, username, fileSocket);
            handler.setFuture(ChatServer.pool.submit(handler));

        }
        else
        {
            writeToClient("SERVER: The chat room is full! You are on a waiting queue! Position in queue: " + waitingList.size());
        }
    }
}
