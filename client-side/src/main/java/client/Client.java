package client;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import client.properties.ClientPropertiesManager;

/*
 * Client.java
 *
 * created at 2022-10-06 by b.papazov <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */


public class Client
{
    public static Logger logger;
    private Socket socket;
    private Socket fileSocket;
    private ExecutorService fileTransferPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 3);
    private BufferedReader in;
    private BufferedWriter out;
    private BufferedInputStream fileInput;
    private BufferedOutputStream fileOutput;
    private String username;
    private String recieveDirectory = ClientPropertiesManager.getInstance().propertiesMap.get("recieveDirectory");

    public Client(Socket socket, String username, Socket fileSocket)
    {
        logger = LogManager.getLogger();
        try
        {
            this.socket = socket;
            this.fileSocket = fileSocket;
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
            this.fileInput = new BufferedInputStream(this.fileSocket.getInputStream());
            this.fileOutput = new BufferedOutputStream(this.fileSocket.getOutputStream());
            this.username = username;
            writeToServer(username);
        }
        catch (IOException e)
        {
            closeEverything();
            e.printStackTrace();
        }
    }


    /**
     * Starts daemon thread when the client is connected successfully to the server and it is not in the waiting queue.
     * Send a message from client's console to the server every time the client uses the console.
     */
    public void sendMessage()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try (Scanner scanner = new Scanner(System.in))
                {
                    while (!socket.isClosed())
                    {

                        while (System.in.available() != 0)
                        {
                            String message = scanner.nextLine();
                            handleMessage(message);
                        }

                    }
                }
                catch (SocketException e)
                {
                    logger.info("You left the chat!");
                }
                catch (IOException e)
                {
                    logger.error(e.getMessage(), e);
                }
            }

        });
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * Listening for message from the server and handling different commands received from it.
     */
    public void listenForMessage()
    {

        String message;
        while (!this.socket.isClosed())
        {
            try
            {
                message = this.in.readLine();

                if (message == null)
                {
                    closeEverything();
                    break;
                }
                String commands[] = message.split(" ", 3);
                switch (commands[0])
                {
                case "disconnected":
                    closeEverything();
                    break;
                case "Welcome!":
                    sendMessage();
                    logger.info("Welcome");
                    System.out.println(commands[0]);
                    break;
                case "SENDING":
                    fileTransferPool.execute(new Runnable()
                    {

                        @Override
                        public void run()
                        {
                            String fileName = commands[1];
                            int length = Integer.parseInt(commands[2]);
                            recieveFileFromServer(fileName, length);
                        }
                    });
                    break;
                default:
                    logger.info(String.join(" ", message));
                    System.out.println(String.join(" ", message));
                    break;
                }

            }
            catch (Exception e)
            {
                closeEverything();
                break;
            }
        }
    }


    /**
     * Receive the file sent by the server and save it in local directory of the client, who wants the file.
     *
     * @param fileName - the name of the file that is received from the server
     * @param length   - the size of the file
     */
    private void recieveFileFromServer(String fileName, int length)
    {
        this.recieveDirectory = ClientPropertiesManager.getInstance().propertiesMap.get("downloadDirectoryWin");
        File directory = new File(recieveDirectory);
        if (!directory.isDirectory())
        {
            directory.mkdir();
        }
        File file = new File(directory, fileName);
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file)))
        {
            for (int i = 0; i < length; i++)
            {
                os.write(this.fileInput.read());
            }
            os.flush();
            System.out.println("The file is recieved successfully!");
        }
        catch (IOException e)
        {
            System.out.println("Unable to receive the file!");
        }
    }


    /**
     * Handle different message typed from the client before interaction with the server.
     *
     * @param message - message from the console
     */
    private void handleMessage(String message)
    {
        String[] commands = message.split(" ", 2);
        switch (commands[0])
        {
        case "EXIT":
            writeToServer("EXIT");
            closeEverything();
            break;

        case "SEND":
            fileTransferPool.execute(new Runnable()
            {

                @Override
                public void run()
                {
                    File file = new File(commands[1]);
                    writeToServer("SEND " + file.getName() + " " + String.valueOf(file.length()));
                    sendFileToServer(file);
                }
            });
            // msg that inform
            break;
        default:
            writeToServer(message);
            break;
        }

    }


    /**
     * Send a file to the server using FileInpuStream.
     *
     * @param file - file to upload to the server
     */
    private void sendFileToServer(File file)
    {
        try (FileInputStream fis = new FileInputStream(file))
        {
            int ch;
            while ((ch = fis.read()) != -1)
            {
                this.fileOutput.write(ch);
            }
            this.fileOutput.flush();
        }
        catch (IOException e)
        {
            logger.error("Unable to send the file to the srver!");

        }

    }


    /**
     * Write message from the client to the server using the socket's output stream.
     *
     * @param message - message send to the server
     */
    private void writeToServer(String message)
    {
        try
        {
            this.out.write(message);
            this.out.newLine();
            this.out.flush();
        }
        catch (IOException e)
        {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }


    /**
     * Closes the connection with the server from the client side.
     */
    public void closeEverything()
    {
        try
        {
            if (socket != null)
            {
                socket.close();
            }
            if (fileSocket != null)
            {
                fileSocket.close();
            }
            if (in != null)
            {
                in.close();
            }

            if (out != null)
            {
                out.close();
            }

            if (fileInput != null)
            {
                fileInput.close();
            }

            if (fileOutput != null)
            {
                fileOutput.close();
            }

            fileTransferPool.shutdown();
        }
        catch (IOException e)
        {
            logger.error(e.getMessage(), e);
        }

    }


    public static void main(String[] args)
    {
        Scanner scanner = new Scanner(System.in);
        try
        {
            System.out.println("Enter the username: ");
            ClientPropertiesManager props = ClientPropertiesManager.getInstance();
            String username = scanner.nextLine();

            System.setProperty("log4j2.configurationFile", "log4j2.xml");
            Socket socket = new Socket(props.propertiesMap.get("ip"), Integer.parseInt(props.propertiesMap.get("serverPort")));
            Socket fileSocket = new Socket(props.propertiesMap.get("ip"), Integer.parseInt(props.propertiesMap.get("transferServerPort")));
            Client client = new Client(socket, username, fileSocket);
            client.listenForMessage();
            scanner.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
