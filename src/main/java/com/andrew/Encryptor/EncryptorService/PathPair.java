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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class PathPair<T extends Path, V extends Path> {

    private T file;
    private V key;
    public PathPair(T path1, Optional<V> vOptional){
        this.file = path1;
        this.key = vOptional.orElse(null);
    }
    public T getFile(){
        return file;
    }
    public V getKey(){
        return key;
    }

    public boolean deleteFiles(){
        if(key != null){
            try {
                return Files.deleteIfExists(file) && Files.deleteIfExists(key);
            }catch (IOException e) {
                System.out.println("failed to delete key and encrypted temp files because of an error");
                return false;
            }
        }
        try{
            return Files.deleteIfExists(file);
        }catch (IOException e){
            System.err.println("failed to delete temp encrypted file after an IoError");
            return false;
        }
    }
}
