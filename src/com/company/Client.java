package com.company;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class Client { // sends file
    private Socket clientSocket = null;
    private InetAddress serverIP = null;
    private int serverPort;

    private File file = null;
    private String path = null;
    private String fileName = null;
    private Long fileSize = null;
    // для работы с файлом
    FileInputStream fileInputStream = null;
    BufferedInputStream bufferedInputStream = null;
    // потоки ввода вывода сокета
    OutputStream outputStream = null;

    Client(String path, InetAddress serverIP, int serverPort) throws IOException { // get all params and create socket
        file = new File(path);
        fileSize = file.length();
        assert(fileSize <= 1099511627776L); // throw an exception
        fileName = getFileName(path); // replace with getName method

        fileInputStream = new FileInputStream(file);
        bufferedInputStream = new BufferedInputStream(fileInputStream);

        this.serverIP = serverIP;
        this.serverPort = serverPort;
        clientSocket = new Socket(serverIP, serverPort);

        outputStream = clientSocket.getOutputStream();
    }

    private String getFileName(String path){
        String retName = null;
        if (path.contains("\\")) {
            retName = path.split("\\\\")[path.split("\\\\").length - 1];
            return retName;
        }
        return path;
    }

    private byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    private void sendFileName() throws IOException {
        byte[] fileNameBuffer = fileName.getBytes(StandardCharsets.UTF_8);
        if (fileNameBuffer.length > 4096){
            throw new IOException("Error: reached max file name length");
        }
        byte[] bufferLength = BigInteger.valueOf(fileNameBuffer.length).toByteArray(); // size varies from 1 to 2
        if (bufferLength.length == 1){
            outputStream.write(1);
        } else outputStream.write(2);

        outputStream.write(bufferLength);
        outputStream.write(fileNameBuffer);
    }

    private void sendFileSize() throws IOException {
        byte[] fileSizeBuffer = longToBytes(fileSize);
        int fileSizeBufferLength = fileSizeBuffer.length;
        outputStream.write((byte) fileSizeBufferLength);
        outputStream.write(fileSizeBuffer);
    }

    private void sendFileByChunks() throws IOException {
        byte[] chunksBuffer = new byte[8192]; // 8 Кбайт
        long current = 0;
        int chunkSize;
        while(current != fileSize) {
            chunkSize = 8192;
            if (fileSize - current >= chunkSize)
                current += chunkSize;
            else {
                chunkSize = (int) (fileSize - current);
                current = fileSize;
            }
            bufferedInputStream.read(chunksBuffer, 0, chunkSize);
            // писать в outputStream столько, сколько вернёт read
            outputStream.write(chunksBuffer, 0, chunkSize);
        }
    }

    private void cleanup() throws IOException {
        fileInputStream.close();
        bufferedInputStream.close();
        outputStream.close();
        clientSocket.close();
    }

    public void sendFile(){
        try { // replace with try with resources
            sendFileName();
            sendFileSize();
            sendFileByChunks();
            cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
