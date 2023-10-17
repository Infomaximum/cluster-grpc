package com.infomaximum.cluster.test.utils;


import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class FinderFreeHostPort {

    private static final int MIN_PORT_NUMBER = 32000;
    private static final int MAX_PORT_NUMBER = 61000;

    private static final Map<Integer, String> lockPorts = new ConcurrentHashMap<>();

    private static Random random = new Random();

    public static int find(){
        int safety=10000;

        int port = genRandomPort();
        while (safety-->0 && !available(port)){
            port = genRandomPort();
        }

        synchronized (lockPorts){
            if (lockPorts.containsKey(port)){
                port = find();
            } else {
                lockPorts.put(port, "LOCK");
            }
        }
        return port;
    }

    //Генерируем случайный порт
    private static int genRandomPort(){
        return random.nextInt(MAX_PORT_NUMBER-MIN_PORT_NUMBER)+MIN_PORT_NUMBER;
    }

    private static boolean available(final int port) {
        if (lockPorts.containsKey(port)) return false;
        return availableReal(port);
    }


    private static boolean availableReal(final int port) {
        ServerSocket serverSocket = null;
        DatagramSocket dataSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            dataSocket = new DatagramSocket(port);
            dataSocket.setReuseAddress(true);
            return true;
        } catch (final IOException e) {
            return false;
        } finally {
            if (dataSocket != null) {
                dataSocket.close();
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    // can never happen
                }
            }
        }
    }
}
