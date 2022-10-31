/*
 * LicenseManager.java
 *
 * created at 2022-10-26 by b.papazov <YOURMAILADDRESS>
 *
 * Copyright (c) SEEBURGER AG, Germany. All Rights Reserved.
 */
package server.license;

public interface LicenseManager
{
    /**
     * Check if the number of clients in the chat is less than the capacity of the server
     * @param currentCount - current count of the clients in the server
     * @return true if there is free spots.
     */
    boolean verify(int currentCount);
}



