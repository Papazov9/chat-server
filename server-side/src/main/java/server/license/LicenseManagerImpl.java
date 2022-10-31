/*
 * LicenseManagerImpl.java
 *
 * created at 2022-10-05 by b.papazov <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package server.license;

import server.properties.PropertiesManager;

public class LicenseManagerImpl implements LicenseManager
{
    private int licenseCount;

    public LicenseManagerImpl()
    {
        this.licenseCount = Integer.parseInt(PropertiesManager.getInstance().propertiesMap.get("licenseCount"));
    }

    public boolean verify(int currentCount)
    {
        return licenseCount > currentCount;
    }

}



