package com.andrew.Encryptor.EncryptorService;/*
 * Copyright (c) Andrew Pegg 2022.
 * All rights reversed
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import java.util.Base64;

/**
 * Util wrapper for usage in different encryptor services
 * @author Andrew Pegg
 * @version 1.00 07/12/2022
 */
public class Utils {
    /**
     * Method will encode a byte array into a base64 string
     * @param data the byte array to be converted into a base64 string
     * @return a base64 encoded string
     */
    public static String encodeBase64(byte[] data)
    {
        return  Base64.getEncoder().encodeToString(data);
    }
    public static byte[] decodeBase64(String data)
    {
        return Base64.getDecoder().decode(data);
    }

    /**
     * Method takes in a file name and returns the name of the file without the extension
     * @param fileName the name of the entire file without path ie text.txt
     * @return everything before the extension ie text from case above
     */
    public static String getFileName(String fileName)
    {
        int lastIndex = fileName.lastIndexOf(".");
        return lastIndex==-1?"":fileName.substring(0,lastIndex);
    }

    /**
     * Method takes in a file name and returns extension of the file without the preceding text
     * @param fileName the name of the entire file without path ie text.txt
     * @return everything after the dot including the dot ie .txt
     */
    public static String getFileExtension(String fileName){
        int lastIndex = fileName.lastIndexOf(".");
        return lastIndex==-1?"":fileName.substring(lastIndex);
    }


}
