package com.company;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Client {
    private final InetAddress serverIP;
    private final int serverPort;
    private final File file;
    private final String fileName;
    private final Long fileSize;

    Client(String path, InetAddress serverIP, int serverPort) {
        file = new File(path);
        fileSize = file.length();
        if (fileSize > 1099511627776L) {
            System.out.println("Error: file size is too large");
            System.exit(-1);
        }
        fileName = file.getName();
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    private byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    private void sendFileName(OutputStream outputStream) throws IOException {
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

    private void sendFileSize(OutputStream outputStream) throws IOException {
        byte[] fileSizeBuffer = longToBytes(fileSize);
        int fileSizeBufferLength = fileSizeBuffer.length;
        outputStream.write((byte) fileSizeBufferLength);
        outputStream.write(fileSizeBuffer);
    }

    private void sendFileByChunks(BufferedInputStream bufferedInputStream, OutputStream outputStream) throws IOException {
        int chunkSize = 8192;
        byte[] chunksBuffer = new byte[chunkSize];
        int bytesRead;
        while ((bytesRead = bufferedInputStream.read(chunksBuffer, 0, chunkSize)) != -1){
            outputStream.write(chunksBuffer, 0, bytesRead);
        }
    }

    private void receiveConfirmation(InputStream inputStream) throws IOException {
        byte[] flag = new byte[1];
        inputStream.readNBytes(flag, 0, 1);
        if (flag[0] == 1) {
            System.out.println("Success: file was delivered");
        } else System.out.println("Error: server received damaged file");
    }

    public void sendFile(){
        try (
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            Socket socket = new Socket(serverIP, serverPort);
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream()
        ) {
            sendFileName(outputStream);
            sendFileSize(outputStream);
            sendFileByChunks(bufferedInputStream, outputStream);
            receiveConfirmation(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
