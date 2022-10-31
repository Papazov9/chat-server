package server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import server.handlers.ConnectionHandler;
import server.handlers.WaitHandler;
import server.license.LicenseManager;
import server.license.LicenseManagerImpl;
import server.repository.FileRepository;

/*
 * Server.java
 *
 * created at 2022-10-06 by b.papazov <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */


public class ChatServer
{
    public static Logger logger;
    public static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
    private ScheduledExecutorService waitingPool;
    public static ExecutorService asyncPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
    public static List<ConnectionHandler> connections = Collections.synchronizedList(new ArrayList<ConnectionHandler>());
    private static ServerSocket serverSocket;
    private ServerSocket fileTransferSocket;
    private LicenseManager licenseManager;
    public static FileRepository repository;

    public ChatServer(ServerSocket serverSocket, ServerSocket fileTransferSocket)
    {
        logger = LogManager.getLogger("log4j2-server");
        repository = new FileRepository(10);
        ChatServer.serverSocket = serverSocket;
        this.fileTransferSocket = fileTransferSocket;
        this.licenseManager = new LicenseManagerImpl();
        this.waitingPool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() / 2);
    }


    /**
     * Starts the server
     */
    public void startServer()
    {
        System.out.println("Started!");

        try
        {

            while (!ChatServer.serverSocket.isClosed() && !fileTransferSocket.isClosed())
            {
                Socket socket = serverSocket.accept();
                Socket fileSocket = fileTransferSocket.accept();

                if (licenseManager.verify(connections.size()))
                {
                    logger.info("New client has connected!");
                    ConnectionHandler handler = new ConnectionHandler(socket, fileSocket);
                    handler.setFuture(pool.submit(handler));

                }
                else
                {
                    WaitHandler waitHandler = new WaitHandler(socket, fileSocket);
                    waitHandler.setFuture(waitingPool.scheduleAtFixedRate(waitHandler, 0, 1, TimeUnit.SECONDS));
                }
            }
        }
        catch (SocketException e)
        {
            try
            {
                Thread.sleep(1000);
                asyncPool.shutdown();
                pool.shutdownNow();
                waitingPool.shutdownNow();
                logger.info("Server is shuted down!");
            }
            catch (InterruptedException e1)
            {
                logger.error("Thread was interrupted while sleeping!");
            }
        }
        catch (IOException e)
        {
            logger.fatal(e.getMessage(), e);
            closeServerSocket();
        }
    }


    /**
     * Closes the server socket when we want to shutdown the server or an exception is thrown.
     */
    public static void closeServerSocket()
    {

        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
            }
        }
        catch (IOException e)
        {
            logger.error("Cannot close the server socket!", e.getMessage(), e);
        }

    }


    public static void main(String[] args)
    {
        ServerSocket socket = null;
        ServerSocket transferSocket = null;

        try
        {
            socket = new ServerSocket(1234);
            transferSocket = new ServerSocket(2345);
        }
        catch (IOException e)
        {
            logger.error(e.getMessage(), e);
        }
        System.setProperty("log4j2.configurationFile", "log4j2-server.xml");
        ChatServer server = new ChatServer(socket, transferSocket);
        server.startServer();
    }
}
