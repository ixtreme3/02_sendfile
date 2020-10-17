package com.company;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class Server { // receives file
    private int port;
    private ServerSocket serverSocket;

    Server(int port) throws IOException {
        this.port = port;
        serverSocket = new ServerSocket(port);
    }

    public void handleIncomeRequests() throws IOException {
        FileHandler fileHandler;
        while(true){ // serverSocket.accept() может выбросить exception - обработать
            fileHandler = new FileHandler(serverSocket.accept()); // завернуть в try/catch
            new Thread(fileHandler).start(); // .start() может упасть - завернуть в try/catch
        }
    }
}

class FileHandler implements Runnable{
    private Socket socket;
    private InputStream inputStream;

    private String fileName = null;
    private long fileSize; // true size (the one that has been send to us by client)
    private long localFileSize = 0; // local size (sum of all chunks)

    FileHandler(Socket socket) throws IOException {
        this.socket = socket;
        inputStream = socket.getInputStream(); // перенести в receiveFile() и сделать try with resources
    }

    private long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip(); // need flip
        return buffer.getLong();
    }

    private void receiveFileName() throws IOException {
        byte[] flag = new byte[1];
        inputStream.readNBytes(flag, 0, 1);

        byte[] bufferSize;
        if (flag[0] == 1) { // casting flag[0] to int is redundant
            bufferSize = new byte[1];
        } else bufferSize = new byte[2];
        inputStream.readNBytes(bufferSize, 0, flag[0]);
        int fileNameBufferSize = new BigInteger(bufferSize).intValue();

        byte[] fileNameBuffer = new byte[fileNameBufferSize];
        inputStream.readNBytes(fileNameBuffer, 0, fileNameBufferSize);
        fileName = new String(fileNameBuffer, StandardCharsets.UTF_8);
        // проверить, что это действит. имя файла
        System.out.println("fileName: " + fileName);
    }

    private void receiveFileSize() throws IOException {
        byte[] fileSizeBufferLengthBuffer = new byte[1];
        inputStream.readNBytes(fileSizeBufferLengthBuffer, 0, 1);
        int fileSizeBufferLength = new BigInteger(fileSizeBufferLengthBuffer).intValue();

        byte[] fileSizeBuffer = new byte[fileSizeBufferLength];
        inputStream.readNBytes(fileSizeBuffer, 0, fileSizeBufferLength);
        fileSize = bytesToLong(fileSizeBuffer);
        System.out.println("fileSize: " + fileSize + " bytes");

    }

    private void receiveFileByChunks() throws IOException {
        byte[] chunksBuffer = new byte[8192];
        String savePath = "uploads/" + fileName;
        File file = new File(savePath);
        file.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(file, false);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);


        int bytesRead = 0;
        Instant start = Instant.now(), now;
        while((bytesRead = inputStream.read(chunksBuffer, 0, chunksBuffer.length)) != -1) { // возможно, нужно будет переделать и считывание

            if(bytesRead > 0){
                localFileSize += bytesRead;
            }
            now = Instant.now();
            double timeElapsedInLoop = (double) Duration.between(start, now).toNanos() / Math.pow(10, 9);
            if (timeElapsedInLoop >= 3) {
                System.out.println("Instant speed is: " + bytesRead/timeElapsedInLoop + " Megabytes per second"); // bytesRead поменять на сумму байт, полученную в теч 3 секунд, timeElapsedInLoop измерить правильно
            }
            bufferedOutputStream.write(chunksBuffer, 0, bytesRead);

        }
        bufferedOutputStream.flush();

        Instant finish = Instant.now();
        double timeElapsed = (double)Duration.between(start, finish).toNanos() / Math.pow(10, 9);
        double localFileSizeInMegabytes = ((double)localFileSize / (1024 * 1024));
        System.out.println("Average speed is: " + (localFileSizeInMegabytes)/timeElapsed + " Megabytes per second");

        fileOutputStream.close();
        bufferedOutputStream.close();
    }

    private void compareSizes(){
        if (fileSize == localFileSize){
            System.out.println("Success: File " +  fileName + " has been received");
        } else System.out.println("Something went wrong: sendSize != receivedSize");
    }

    private void cleanup() throws IOException {
        inputStream.close();
        socket.close();
    }

    public void receiveFile() {
        try {
            receiveFileName();
            receiveFileSize();
            receiveFileByChunks();
            compareSizes();
            cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        receiveFile();
    }
}