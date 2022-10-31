/*
 * FileRepository.java
 *
 * created at 2022-10-11 by devadm <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package server.repository;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.OutputStream;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import server.ChatServer;
import server.properties.PropertiesManager;


public class FileRepository
{
    private static final String FILES_DIRECTORY = "C:\\DEV\\eclipse\\workspace\\chat-server\\src\\main\\resources\\filesDirectory";
    private static final String DIRECTORY = PropertiesManager.getInstance().propertiesMap.get("filesDirectory");
    private LoadingCache<String, File> fileCache;

    public FileRepository(int capacity)
    {
        this.fileCache = CacheBuilder
                                     .newBuilder()
                                     .maximumSize(capacity)
                                     .expireAfterAccess(2, TimeUnit.MINUTES)
                                     .build(new CacheLoader<String, File>()
                                     {
                                         @Override
                                         public File load(String key)
                                             throws Exception
                                         {
                                             File result = new File(FILES_DIRECTORY + File.separator + key);

                                             if (result.isFile())
                                             {
                                                 return result;
                                             }
                                             throw new FileNotFoundException();
                                         }
                                     });

    }


    /**
     * Saves the file to the cached collection and creates a copy of in the local server directory.
     *
     * @param fileName - name of the file to save in the repository
     * @param length   - size of the file
     * @param reader   - reader of the file
     * @return true if the save of the file is successful
     */
    public boolean addFile(String fileName, int length, BufferedInputStream reader)
    {
        File file = new File(DIRECTORY + fileName);
        try (OutputStream fw = new BufferedOutputStream(new FileOutputStream(file)))
        {

            for (int i = 0; i < length; i++)
            {
                fw.write(reader.read());
            }

            fw.flush();
            fileCache.put(fileName, file);
            return true;
        }
        catch (IOException e)
        {
            ChatServer.logger.error(e.getMessage(), e);
            return false;
        }
    }


    /**
     * Return a file by the short name of the file from the Map.
     *
     * @param key
     * @return the file found in the Map
     * @throws FileNotFoundException if the file is not found in the collection.
     */
    public File getFileByKey(String key)
        throws FileNotFoundException
    {
        try
        {
            File file = fileCache.get(key);
            if (file.isFile())
            {
                return file;
            }
        }
        catch (ExecutionException e)
        {
            throw new FileNotFoundException();

        }
        throw new FileNotFoundException();
    }
}
