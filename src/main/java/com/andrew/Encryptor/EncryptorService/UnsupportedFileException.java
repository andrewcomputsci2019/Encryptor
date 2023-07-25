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

import java.io.File;

/**
 * Class to be used to identify weather a file is supported by this program for encryption / decryption
 * @author Andrew Pegg
 * @version 1.0 07/12/2022
 * @see Exception
 *
 */
public class UnsupportedFileException extends Exception{
    public UnsupportedFileException(String message){
        super(message);
    }
    public UnsupportedFileException(File file){
        super(String.format("File: %s is not supported",file.getName()));
    }
}
