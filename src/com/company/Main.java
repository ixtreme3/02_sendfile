package com.company;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName("10.9.41.92");
	    Client client = new Client("C:\\Users\\iXtreme3\\Desktop\\test.txt", inetAddress, 777);
	    client.sendFile();
    }
}
