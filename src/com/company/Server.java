package com.company;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Server {
    private final int port;

    Server(int port) {
        this.port = port;
    }

    public void handleIncomeRequests() {
        FileHandler fileHandler;
        while(true){
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                fileHandler = new FileHandler(serverSocket.accept());
                new Thread(fileHandler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class FileHandler implements Runnable{
    private final Socket socket;

    private String fileName = null;
    private long fileSize;
    private long localFileSize = 0;

    FileHandler(Socket socket) {
        this.socket = socket;
    }

    private long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }

    private boolean isFilenameValid() {
        String[] ILLEGAL_CHARACTERS = { "/", "\n", "\r", "\t", "\0", "\f", "`", "?", "*", "\\", "<", ">", "|", "\"", ":" };
        if (fileName.length() == 0){
            return false;
        }
        for (String illegal_character : ILLEGAL_CHARACTERS) {
            if (fileName.contains(illegal_character)) {
                return false;
            }
        }
        return true;
    }

    private void receiveFileName(InputStream inputStream) throws IOException {
        byte[] flag = new byte[1];
        inputStream.readNBytes(flag, 0, 1);
        byte[] bufferSize;
        if (flag[0] == 1) {
            bufferSize = new byte[1];
        } else bufferSize = new byte[2];
        inputStream.readNBytes(bufferSize, 0, flag[0]);
        int fileNameBufferSize = new BigInteger(bufferSize).intValue();

        byte[] fileNameBuffer = new byte[fileNameBufferSize];
        inputStream.readNBytes(fileNameBuffer, 0, fileNameBufferSize);
        fileName = new String(fileNameBuffer, StandardCharsets.UTF_8);
        if (isFilenameValid()){
            System.out.println("fileName: " + fileName);
        } else throw new IOException("Error: Invalid file name received");
    }

    private void receiveFileSize(InputStream inputStream) throws IOException {
        byte[] fileSizeBufferLengthBuffer = new byte[1];
        inputStream.readNBytes(fileSizeBufferLengthBuffer, 0, 1);
        int fileSizeBufferLength = new BigInteger(fileSizeBufferLengthBuffer).intValue();
        byte[] fileSizeBuffer = new byte[fileSizeBufferLength];
        inputStream.readNBytes(fileSizeBuffer, 0, fileSizeBufferLength);
        fileSize = bytesToLong(fileSizeBuffer);
        System.out.println("fileSize: " + fileSize + " bytes");

    }

    private void receiveFileByChunks(InputStream inputStream) throws IOException {
        File file = new File("uploads/" + fileName);
        file.getParentFile().mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(file, false);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

        byte[] chunksBuffer = new byte[8192];
        int bytesRead;
        long startTime = System.nanoTime(), startLoopTime = System.nanoTime();
        double overallTimeElapsedInLoop = 0;

        while((bytesRead = inputStream.read(chunksBuffer, 0, chunksBuffer.length)) != -1) {
            if(bytesRead > 0){
                localFileSize += bytesRead;
            }
            bufferedOutputStream.write(chunksBuffer, 0, bytesRead);

            double timeElapsedInLoop = (double) (System.nanoTime() - startLoopTime) / Math.pow(10, 9);
            if (timeElapsedInLoop >= 3){
                overallTimeElapsedInLoop += timeElapsedInLoop;
                double currentFileSizeInMegabytes = ((double)localFileSize / (1024 * 1024));
                System.out.println("Instant speed is: " + String.format("%.3f", currentFileSizeInMegabytes / overallTimeElapsedInLoop) + " Megabytes per second");
                startLoopTime += 3 * Math.pow(10,9);
            }
            if (localFileSize == fileSize){
                break;
            }
        }
        bufferedOutputStream.flush();

        long endTime = System.nanoTime();
        double duration = (double) (endTime - startTime) / Math.pow(10, 9);
        double localFileSizeInMegabytes = ((double)localFileSize / (1024 * 1024));
        System.out.println("Average speed is: " + String.format("%.3f", localFileSizeInMegabytes / duration) + " Megabytes per second");

        fileOutputStream.close();
        bufferedOutputStream.close();
    }

    private void sendConfirmation(OutputStream outputStream) throws IOException {
        if (fileSize == localFileSize){
            System.out.println("Success: File " +  fileName + " has been received");
            outputStream.write(1);
        } else {
            System.out.println("Something went wrong: sendSize != receivedSize");
            outputStream.write(0);
        }
    }

    public void receiveFile() {
        try (
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream()
        ) {
            receiveFileName(inputStream);
            receiveFileSize(inputStream);
            receiveFileByChunks(inputStream);
            sendConfirmation(outputStream);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        receiveFile();
    }
}
