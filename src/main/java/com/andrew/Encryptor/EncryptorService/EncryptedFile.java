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

import java.io.*;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Objects;

/**
 * @author Andrew Pegg
 * @version  1.0 07/12/2022
 */
public class EncryptedFile {
    /**
     * Private enum that houses all possible encryption types
     */
    public enum EncryptionType {AES, BLOWFISH, XOR}

    /**
     * Enum that denotes what encryption service should be used or has been used in case of decryption
     */
    private EncryptionType encryptionType;
    /**
     * underlying layer of the file, houses location and how to access it
     */
    private File file;
    /**
     *  used for decryption only houses name
     *  license
     */
    private String fileName;
    /**
     * houses filetype of underlying file, only used for decryption usage
     */
    private String fileType;
    /**
     * Offset to be used in decryption to bypass prepended header
     */
    private long byteOffset;
    /**
     * Used for decryption as we need it to provide the first step in decryption, base64 encoded this also has the salt attached to it at the end
     */
    private String IV;

    public EncryptedFile(String fileName, String fileType, EncryptionType type, File file,long byteOffset, String iv){
        setFile(file);
        setFileName(fileName);
        setFileType(fileType);
        setEncryptionType(type);
        setByteOffset(byteOffset);
        setIV(iv);

    }
    public EncryptedFile(String fileName,String fileType,EncryptionType type,File file)
    {
        if(!fileName.equals(Utils.getFileName(file.getName())))
        {
            throw new IllegalArgumentException("Given file name string does not match name from file");
        }
        if(!fileType.equals(Utils.getFileExtension(file.getName())))
        {
            throw new IllegalArgumentException("Given file extension does not match that of the file given");
        }
        setFile(file);
        setFileType(fileType);
        setFileName(fileName);
        setEncryptionType(type);
    }

    /**
     * Sets all properties to default setting
     * @apiNote This method should only be used for testing purposes and not for active usage
     */
    public EncryptedFile(){
       setFileName("Default");
       setEncryptionType(EncryptionType.AES);
       setFileType("Unknown");
       setFile(new File("./"+getFileName()+"."+getFileType()));
    }

    /**
     * Function is used to read an encrypted files headers, note this method is non-destructive
     * @param file the file that should be unencrypted this should be a valid file
     * @return A EncryptedFile object back with proper information necessary to decrypt file
     * @throws UnsupportedFileException if file is missing proper header or header is malformed
     * @throws IOException if an IO error occurs
     */
    public static EncryptedFile initRead(File file) throws UnsupportedFileException, IOException{
        if(!file.exists() || !file.canRead()){
            throw new UnsupportedFileException("File does not exist or can not be read");
        }
        //read only and try with resource, lock is used here to prevent another process to write to the file while we have accesses
        try(RandomAccessFile randomAccessFile = new RandomAccessFile(file,"r");FileLock lock = randomAccessFile.getChannel().tryLock(0L, Long.MAX_VALUE, true)) {
            String line = randomAccessFile.readLine();
            if(!line.startsWith("BOF:")){
                lock.release();
                throw new UnsupportedFileException("file does not have the correct prepend header");
            }
            int lineCount;
            try {
               lineCount = Integer.parseInt(line.split(":")[1]);
            }catch (NumberFormatException e){
                lock.release();
                throw new UnsupportedFileException("Prepended header malformed");
            }
            HashMap<String,String> properties = new HashMap<>();
            for(int i=1; i<lineCount; i++){
                line = randomAccessFile.readLine();
                if(line.equals("EOF"))
                {
                    //if EOF is not listed at line count throw error code
                    if(i < lineCount-1){
                        lock.release();
                        throw new UnsupportedFileException("Expected EOF at line: " + lineCount +" but EOF was listed at: " + i);
                    }
                    break;
                }
                String metaName, metaValue;
                String[] property = line.split(":");
                metaName = property[0];
                metaValue = property[1];
                properties.put(metaName,metaValue);
            }
            if(!properties.containsKey("EncryptionType") || !properties.containsKey("FileName") || !properties.containsKey("FileType") || !properties.containsKey("IV"))
            {
                lock.release();
                throw new UnsupportedFileException("Missing necessary property in file header");
            }
            lock.release();
            return new EncryptedFile(properties.get("FileName"),properties.get("FileType"),EncryptionType.valueOf(properties.get("EncryptionType")),file,randomAccessFile.getFilePointer(), properties.get("IV"));
        }catch (IOException e){
            throw new IOException(e);
        }
    }

    /**
     * Returns the encryptionType variable
     * @return an enum that describes what encryption has been used or should be used
     * @see #encryptionType
     */
    public EncryptionType getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(EncryptionType encryptionType) {
        this.encryptionType = encryptionType;
    }

    /**
     *
     * @return mutable copy of file, this does not matter as files are shared objects in java
     */
    public File getFile() {
        return file;
    }

    /**
     * This method should only be used in menu selection process
     * @param file new file to replace old one
     */
    public void setFile(File file) {
        if(!file.exists())
        {
            throw new IllegalArgumentException("File does not exists");
        }
        this.file = file;
    }

    /**
     * Returns name of the file
     * @return String representation of fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Used to change the file of EncryptedFile object
     * @param fileName new name of the file
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private void setByteOffset(long byteOffset){
        this.byteOffset = byteOffset;
    }

    /**
     * This returns the String representation of the extension of the file ie .txt .mp4 and so forth
     * @return file extension of the file in String form
     */
    public String getFileType() {
        return fileType;
    }

    /**
     *
     * @return byte offset to be used during decryption
     */
    public long getByteOffset() {
        return byteOffset;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setIV(String iv) {
        this.IV = iv;
    }
    public String getIV(){
        return IV;
    }

    @Override
    public String toString() {
        return "EncryptedFile{" +
                "encryptionType=" + encryptionType +
                ", file=" + file +
                ", fileName='" + fileName + '\'' +
                ", fileType='" + fileType + '\'' +
                ", byteOffset=" + byteOffset +
                ", IV='" + IV + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncryptedFile that)) return false;
        return byteOffset == that.byteOffset && encryptionType == that.encryptionType && Objects.equals(file, that.file) && Objects.equals(fileName, that.fileName) && Objects.equals(fileType, that.fileType) && Objects.equals(IV, that.IV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encryptionType, file, fileName, fileType, byteOffset, IV);
    }
}
