package com.company;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        InetAddress inetAddress = InetAddress.getByName("10.9.41.92");
        Client client = new Client("C:\\Users\\iXtreme3\\IdeaProjects\\СетиLab2\\src\\com\\files\\openjdk-15_windows-x64_bin.zip", inetAddress, 777);
        client.sendFile();
    }
}
