package com.brotherjing.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;

import com.brotherjing.utils.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPServer extends Service {

    private ServerSocket serverSocket;

    private List<ClientThread> clients;

    private String IP;
    private boolean isConnected;

    private Thread serverThread;

    public TCPServer() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IP = null;
        isConnected = false;
        clients = new ArrayList<>();

        IP = getLocalIP();
        if(IP==null){
            return;
        }
        Logger.i(IP);
        serverThread = new Thread(runnable);
        serverThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    public void sendToAll(String str){
        for(ClientThread thread : clients){
            thread.send(str);
        }
    }

    public void quitAll(){
        Logger.i(Thread.currentThread().getName() + " in server thread");
        for(ClientThread thread : clients){
            thread.quitSelf();
        }
        clients.clear();
        try {
            serverSocket.close();
            isConnected = false;
            Logger.i("server closed");
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    private String getLocalIP(){
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        if(ipAddress==0)return null;
        return ((ipAddress & 0xff)+"."+(ipAddress>>8 & 0xff)+"."
                +(ipAddress>>16 & 0xff)+"."+(ipAddress>>24 & 0xff));
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try{
                serverSocket = new ServerSocket(CONSTANT.PORT);
                Intent intent1 = new Intent(CONSTANT.ACTION_SERVER_UP);
                intent1.putExtra(CONSTANT.KEY_IP_ADDR, IP);
                sendBroadcast(intent1);
                Logger.i(IP + " in msg");
                Logger.i(Thread.currentThread().getName()+" in server thread");
                isConnected = true;
                while (isConnected){
                    Socket socket = serverSocket.accept();
                    String name = System.currentTimeMillis()+"";
                    Logger.i("new client! "+name);
                    ClientThread clientThread = new ClientThread(name,socket,TCPServer.this);
                    clientThread.start();
                    clients.add(clientThread);
                }
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        quitAll();
        serverThread.interrupt();
    }

    public class MyBinder extends Binder{
        public String getIP(){
            return IP;
        }
    }

}