/*
 * PropertiesManager.java
 *
 * created at 2022-10-06 by b.papazov <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package server.properties;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import server.ChatServer;


public final class PropertiesManager
{
    private static PropertiesManager instance;
    public Map<String, String> propertiesMap;

    private PropertiesManager(Map<String, String> map)
    {
        this.propertiesMap = map;
    }


    public static PropertiesManager getInstance()
    {
        PropertiesManager result = instance;

        if (result != null)
        {
            return result;
        }

        if (instance == null)
        {
            Map<String, String> map = loadProperties(".\\serverConfig.properties");
            instance = new PropertiesManager(map);
        }

        return instance;
    }


    /**
     * Load all properties from the properties file and saves it in a map with key - short name of the file and value - instance of the
     * file.
     *
     * @param filePath - path of the properties file
     * @return map of the loaded properties from the file
     */
    private static Map<String, String> loadProperties(String filePath)
    {
        Map<String, String> map = new HashMap<>();
        Properties props = new Properties();
        try
        {
            InputStream is = new FileInputStream(filePath);
            props.load(is);
        }
        catch (IOException e)
        {
            ChatServer.logger.error("Unable to load the properties!");
        }

        for (String key : props.stringPropertyNames())
        {
            map.putIfAbsent(key, props.getProperty(key, "0"));
        }

        return map;
    }
}
