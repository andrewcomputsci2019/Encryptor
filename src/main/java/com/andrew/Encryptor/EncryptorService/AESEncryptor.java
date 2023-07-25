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

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;


/**
 * AES encryptor service design to encrypt and decrypt files
 * @author Andrew Pegg
 * @version 1.0 07/12/2022
 */
public class AESEncryptor implements Encryptor {
    /**
     * Cipher instance to be used for encryption and decryption methods
     */
    private Cipher cipher;
    /**
     * The key that was used to encrypt the file
     */
    private SecretKey secretKey;
    /**
     * The nonce that was used to encrypt the file
     */
    private byte[] IV;
    /**
     * The Key size that will be generated in bytes
     */
    private static final int KEY_SIZE = 256;
    /**
     * The salt size in bytes, note that salt will only be used in password based encryption and decryption
     * but will always be generated
     */
    private static final int SALT_SIZE = 16;
    /**
     * The nonce byte size
     */
    private static final int IV_SIZE = 16;
    /**
     * the tag length to be used by GCM
     */
    private static final int T_LEN = 128;
    /**
     * Name of hash algorithm for password based key generation
     */
    private static final String PBKDF2_NAME = "PBKDF2WithHmacSHA256";
    /**
     * Number of hashes to perform
     */
    private static final int PBKDF2_ITER_COUNT = 50000;
    /**
     * Salt, of encryption method
     */
    private byte[] salt;

    private boolean PasswordEncryption;
    /**
     * The name of the encryption algorithm to be used
     */
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * Private Constructor to prevent improper construction
     */
    private AESEncryptor() {

    }

    @Override
    public Path decrypt(EncryptedFile file) throws IOException,GeneralSecurityException {
        Path path;
        try{
            path = Files.createTempFile(file.getFileName(),file.getFileType().replaceFirst(".",""));
        }catch (IOException e){
            System.err.println("Failed to create temp file for decryption file");
            throw new IOException(e);
        }
        try(FileInputStream fileInputStream = new FileInputStream(file.getFile()); FileLock ignored = fileInputStream.getChannel().tryLock(0,Long.MAX_VALUE,true); FileOutputStream fileOutputStream = new FileOutputStream(path.toFile()); FileLock ignored1 = fileOutputStream.getChannel().tryLock()){
            byte[] buffer = new byte[4096];
            long byteOffset;
            long reamingBytes = file.getByteOffset();
            //skip header of the file
            do{
                byteOffset = fileInputStream.skip(reamingBytes);
                reamingBytes = reamingBytes-byteOffset;
            }while(reamingBytes>0); //needed as we are not guaranteed the fact that all bytes have been read
            int bytesRead;
            //read till buffer is empty
            while((bytesRead = fileInputStream.read(buffer))!=-1){
                byte[] bytesCipher = cipher.update(buffer,0,bytesRead);
                if(bytesCipher!=null) {
                    fileOutputStream.write(bytesCipher);
                }
            }
            byte[] finalBytes = cipher.doFinal();
            if(finalBytes!=null){
                fileOutputStream.write(finalBytes);
            }
            fileOutputStream.flush();
        }catch (IOException e){
            e.printStackTrace();
            if(Files.deleteIfExists(path)){
                System.out.println("deleted temp file after IOError occurred");
            }else{
                System.out.println("either temp failed to be deleted or it does not exist");
            }
            throw new IOException(e);
        }catch (GeneralSecurityException e){
            e.printStackTrace();
            System.err.println("cipher error occurred");
            if(Files.deleteIfExists(path)){
                System.out.println("deleted temp file after IOError occurred");
            }else{
                System.out.println("either temp failed to be deleted or it does not exist");
            }
            throw new GeneralSecurityException(e);
        }
        return path;
    }

    /**
     * Encrypts the given encryptedFile object, writes into a tmp file, once fished it return a Path pair contain paths needed to get the temp resources
     * @param encryptedFile the file that is supposed to be encrypted, this method is non-destructive and only reads the file bytes
     * @return A PathPair that contains an encrypted file path and optionally a keyfile path, may be null
     * @throws IOException if an io error occurs at anypoint
     * @throws GeneralSecurityException if any cipher error occurs
     */
    @Override
    public PathPair<Path,Path> encrypt(EncryptedFile encryptedFile) throws IOException, GeneralSecurityException {
        //path to encrypted file
        Path path;
        //path to key file if one is generated null otherwise
        Path keyPath = null;
        try {
            path = Files.createTempFile(encryptedFile.getFileName(), ".enc");
        } catch (IOException e) {
            System.err.println("Failed to create Temp file");
            throw new IOException(e);
        }
        //try with resource to handle auto closing of file streams and locks
        try (FileOutputStream writer = new FileOutputStream(path.toFile()); FileLock ignored1 = writer.getChannel().tryLock(); FileInputStream fileInputStream = new FileInputStream(encryptedFile.getFile()); FileLock ignored = fileInputStream.getChannel().tryLock(0, Long.MAX_VALUE, true)) {
            writeHeader(encryptedFile, writer);
            //creates an internal buffer of 4kb
            byte[] buffer = new byte[4096];
            //read till end of file
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    byte[] cipherOutPut = cipher.update(buffer,0,bytesRead);
                    if(cipherOutPut!=null){
                        writer.write(cipherOutPut);
                    }
            }
            byte[] outputBytes = cipher.doFinal();
            if(outputBytes!=null){
                writer.write(outputBytes);
            }
            writer.flush();
            //don't need to close files streams as try with resource will take care of that
        } catch (IOException | OverlappingFileLockException exception) {
            if (Files.deleteIfExists(path)) {
                System.out.println("deleted temp files");
            } else {
                System.err.println("Failed to delete temp files after io error");
            }
            throw new IOException("File was unable to be locked, this could be caused by another process is using the file");
        }catch (GeneralSecurityException e) {
            if (Files.deleteIfExists(path)) {
                System.out.println("deleted temp files");
            } else {
                System.err.println("Failed to delete temp files after cipher error");
            }
            System.err.println("cipher error occurred");
            throw new GeneralSecurityException(e);
        }
        //need to write the key file if not password based
        if(!PasswordEncryption){
            try{
                keyPath = Files.createTempFile(encryptedFile.getFileName(),".key");
            }catch (IOException e){
                if (Files.deleteIfExists(path)) {
                    System.out.println("deleted temp files");
                } else {
                    System.err.println("Failed to delete temp files after failed to create key file");
                }
                throw new IOException(e);
            }
            try(FileOutputStream fileOutPutStream = new FileOutputStream(keyPath.toFile()); FileLock ignored = fileOutPutStream.getChannel().tryLock()){
                fileOutPutStream.write(Utils.encodeBase64(this.secretKey.getEncoded()).getBytes(StandardCharsets.UTF_8));
            }catch (IOException e){
                if(Files.deleteIfExists(path) && Files.deleteIfExists(keyPath)){
                    System.out.println("deleted temp files");
                }else {
                    System.err.println("Failed to delete temp files after failed to create key file");
                }
                throw new IOException(e);
            }
        }
        //returns encrypted file path
        return new PathPair<>(path, Optional.ofNullable(keyPath));
    }

    /**
     * Writes magic header to file, contains basic information about file like name, file extension and its IV and Salt
     * @param file the file being encrypted
     * @param stream Stream used to write to the file
     * @throws IOException if an IOError occurs at anypoint
     */
    private void writeHeader(EncryptedFile file, OutputStream stream) throws IOException {
        //fileName,fileType,EncryptionType,IV
        byte[] IVAndSalt = new byte[IV_SIZE+SALT_SIZE];
        System.arraycopy(IV,0,IVAndSalt,0,IV.length);
        System.arraycopy(salt,0,IVAndSalt,IV.length,salt.length);
        stream.write("BOF:6\n".getBytes(StandardCharsets.UTF_8));
        stream.write(("FileName:" + file.getFileName() + "\n").getBytes(StandardCharsets.UTF_8));
        stream.write(("FileType:" + file.getFileType() + "\n").getBytes(StandardCharsets.UTF_8));
        stream.write(("EncryptionType:" + file.getEncryptionType().name() + "\n").getBytes(StandardCharsets.UTF_8));
        stream.write(("IV:" + Utils.encodeBase64(IVAndSalt) + "\n").getBytes(StandardCharsets.UTF_8));
        stream.write("EOF\n".getBytes(StandardCharsets.UTF_8));
        stream.flush();
    }

    /**
     * Method used to generate a random secret key based encryption mode instance of AESEncryptor
     *
     * @return AESEncryptor instance that is ready to encrypt file
     * @see #init(String)
     */
    public static AESEncryptor init() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        AESEncryptor encryptor = new AESEncryptor();
        SecureRandom random = new SecureRandom();
        //make a byte array of SALT SIZE
        encryptor.salt = new byte[SALT_SIZE];
        //FIll salt array with random bytes
        random.nextBytes(encryptor.salt);
        //make a byte array of IV_SIZE
        encryptor.IV = new byte[IV_SIZE];
        //FIll IV with random bytes from secure random
        random.nextBytes(encryptor.IV);
        //key gen for random generation of key
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        //get key from key gen
        encryptor.secretKey = keyGenerator.generateKey();
        //get a cipher of AES GCM mode
        encryptor.cipher = Cipher.getInstance(ALGORITHM);
        //init cipher for encryption given above parameters
        encryptor.cipher.init(Cipher.ENCRYPT_MODE, encryptor.secretKey, new IvParameterSpec(encryptor.IV));
        encryptor.PasswordEncryption = false;
        return encryptor;
    }

    /**
     * method used to generate a password encryption mode instance of AESEncryptor
     *
     * @param password the password to be hashed and be used as a secret key
     * @return AESEncryptor instance that is ready to encrypt file
     * @see #init()
     */
    public static AESEncryptor init(String password) throws NoSuchAlgorithmException, IllegalArgumentException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        AESEncryptor encryptor = new AESEncryptor();
        //invalid parameters
        if (password == null || password.equals("")) {
            throw new IllegalArgumentException("Empty or null string provided");
        }
        //gen salt and password hash
        SecureRandom random = new SecureRandom();
        encryptor.salt = new byte[SALT_SIZE];
        random.nextBytes(encryptor.salt);
        //make a hash of 256 bytes long using the hash of the password plus salt
        PBEKeySpec pwSpec = new PBEKeySpec(password.toCharArray(), encryptor.salt, PBKDF2_ITER_COUNT, KEY_SIZE);
        //using hash generate a key
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_NAME);
        encryptor.secretKey = new SecretKeySpec(factory.generateSecret(pwSpec).getEncoded(),"AES");
        encryptor.IV = new byte[IV_SIZE];
        random.nextBytes(encryptor.IV);
        encryptor.cipher = Cipher.getInstance(ALGORITHM);
        //this init the cipher to be able to encrypt files and such
        encryptor.cipher.init(Cipher.ENCRYPT_MODE, encryptor.secretKey, new IvParameterSpec(encryptor.IV));
        //at this point a file can be encrypted
        encryptor.PasswordEncryption = true;
        return encryptor;
    }

    /**
     * @param password the password used previously, for the AESEncryptor to be used to decrypt the file, this function uses PBKDF2
     * @param IV       the base64 encoded nonce this should also contain the 16 byte salt also used attached at the end
     * @return an instance of AESEncryptor configured for decryption using password based methods
     * @see #init_key(String, String)
     */
    public static AESEncryptor init_password(String password, String IV) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        AESEncryptor encryptor = new AESEncryptor();
        //decode base64 IV-Salt into bytes
        byte[] IVAndSalt = Utils.decodeBase64(IV);
        //create new salt byte array of SALT_SIZE
        encryptor.salt = new byte[SALT_SIZE];
        //create new IV byte array of IV_SIZE
        encryptor.IV = new byte[IV_SIZE];
        //IV is contained in the first IV_SIZE bytes
        System.arraycopy(IVAndSalt, 0, encryptor.IV, 0, IV_SIZE);
        //SALT is contained in the sequential bytes that follow the IV
        System.arraycopy(IVAndSalt, IV_SIZE, encryptor.salt, 0, SALT_SIZE);
        //hash the password and use it to generate an AES cryptographic Key
        PBEKeySpec pwSpec = new PBEKeySpec(password.toCharArray(), encryptor.salt, PBKDF2_ITER_COUNT, KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_NAME);
        encryptor.secretKey = new SecretKeySpec(factory.generateSecret(pwSpec).getEncoded(),"AES");
        //create AES cipher given IV and cryptographic Key
        encryptor.cipher = Cipher.getInstance(ALGORITHM);
        encryptor.cipher.init(Cipher.DECRYPT_MODE, encryptor.secretKey, new IvParameterSpec(encryptor.IV));
        return encryptor;
    }

    /**
     * Method initializes encryptor service so that it can decrypt an Encrypted file using a secret key and IV
     *
     * @param key this is the key that will be used decrypt the file, currently only using string form
     * @param IV  the string representation of initialization vector aka nonce, this should still have salt attached but will be discarded during init process
     * @return an instance of the AESEncryptor that has been configured for decryption
     * @see #init_password(String, String)
     */
    public static AESEncryptor init_key(String key, String IV) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        AESEncryptor encryptor = new AESEncryptor();
        byte[] IVAndSalt = Utils.decodeBase64(IV);
        encryptor.IV = new byte[IV_SIZE];
        System.arraycopy(IVAndSalt, 0, encryptor.IV, 0, IV_SIZE);
        byte[] keyBytes = Utils.decodeBase64(key);
        encryptor.secretKey = new SecretKeySpec(keyBytes, "AES");
        encryptor.cipher = Cipher.getInstance(ALGORITHM);
        encryptor.cipher.init(Cipher.DECRYPT_MODE, encryptor.secretKey, new IvParameterSpec(encryptor.IV));
        return encryptor;
    }
}
