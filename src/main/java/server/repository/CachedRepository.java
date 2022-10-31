/*
 * CachedRepository.java
 *
 * created at 2022-10-10 by b.papazov <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package server.repository;


import java.io.File;
import java.io.FileNotFoundException;

import com.google.common.cache.CacheLoader;


public class CachedRepository extends CacheLoader<String, File>
{
private static final String FILES_DIRECTORY = "C:\\DEV\\eclipse\\workspace\\chat-server\\src\\main\\resources\\filesDirectory";


    @Override
    public File load(String key)
        throws Exception
    {
        File result = new File(FILES_DIRECTORY + File.separator + key );

        if (result.isFile())
        {
            return result;
        }
        throw new FileNotFoundException();
    }

}
