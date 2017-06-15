package com.bkhtest.notepadshare;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by Bryan on 6/14/2017.
 */

public class BluetoothConnectionService {
    private static final String TAG="BluetoothConnectionServ"; //debugging tag

    private static final String appName="MYAPP";

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //generate random address that devices use to connect

    private final BluetoothAdapter mBluetoothAdapter;  //handles Bluetooth objects and commands
    Context mContext;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    private String received;


    public BluetoothConnectionService(Context context)
    {
        mContext=context;
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        start();
    }

    //AcceptThread--waiting for connection/for something to try and connect
    //Run on different Thread so it doesn't use resources on MainActivity Thread
    private class AcceptThread extends Thread{
        //Local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                // Create a new listening server socket
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);

                Log.d(TAG,"AcceptThread: Setting up Server using: "+MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.d(TAG,"AcceptThread: IOException: "+e.getMessage());
            }
            mmServerSocket = tmp;
        }
        public void run(){
            Log.d(TAG,"run: AcceptThread Running.");
            BluetoothSocket socket = null;

            try {
                //This is a blocking call and will only return on a successful connection or an exception
                Log.d(TAG,"run: RFCOM server socket start.........");
                socket=mmServerSocket.accept();   //sit here and wait if connection fails/
                Log.d(TAG, "run: RFCOM server socket accepted connection.");
            } catch (IOException e) {
                Log.e(TAG,"AcceptThread: IOException: "+e.getMessage());
            }
            if(socket!=null){
                connected(socket,mmDevice);
            }
            Log.d(TAG,"END mAcceptThread ");

        }
        public void cancel(){
            try {
                Log.d(TAG,"cancel: Cancelling AcceptThread. ");
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG,"cancel: Close of AccecptThread ServerSocket failed. "+e.getMessage());
            }
        }
    }


    //Connect Thread
    //Connects to another device
    //run() automatically runs
    public class ConnectThread extends Thread{
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG,"ConnectThread: started.......");
            mmDevice=device;
            deviceUUID=uuid;
        }
        public void run(){
            BluetoothSocket tmp= null;
            Log.d(TAG,"RUN mConnectThread ");


            try {
                Log.d(TAG,"ConnectThread: Trying to create InsecureRFcommSocket using UUID: "+MY_UUID_INSECURE);
                //Get a BluetoothSocket for a connection with the
                //given BluetoothDevice
                tmp=mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG,"ConnectThread: Could not create InsecureRFcommSocket "+e.getMessage());
            }
            mmSocket=tmp;

            //Always cancel discovery because it will slow down a connection (memory intensive)
            mBluetoothAdapter.cancelDiscovery();

            //Make a connection to the BluetoothSocket



            try {
                //This is a blocking call and will only return on a
                //successful connection or an exception
                mmSocket.connect();
                Log.d(TAG,"run: ConnectThread connected.");
            } catch (IOException e) {
                //Close the socket
                try {
                    mmSocket.close();
                    Log.d(TAG,"run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG,"mConnectThread: run: Unable to close connection in socket "+e1.getMessage());
                }
                Log.e(TAG,"run: ConnectThread: Could not connect to UUID: "+MY_UUID_INSECURE);

            }
            connected(mmSocket,mmDevice);
        }

        public void cancel(){
            try{
                Log.d(TAG,"cancel: Closing Client Socket.");
                mmSocket.close();
            }catch(IOException e){
                Log.e(TAG,"cancel: close() of mmSocket in ConnectThread failed. "+e.getMessage());
            }
        }



    }

    //Initiate the AcceptThread to begin a session in listening
    //(server) mode
    public synchronized void start(){
        Log.d(TAG,"start");
        //Cancel any thread attempting to make a connection
        if(mConnectThread!=null){
            mConnectThread.cancel();
            mConnectThread=null;
        }
        if(mInsecureAcceptThread==null){
            mInsecureAcceptThread= new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG,"startClient: Started.");
        //initprogress dialog
        //pop up when connection is trying to be made
        mProgressDialog=ProgressDialog.show(mContext,"Connecting Bluetooth","Please Wait....",true);

        mConnectThread=new ConnectThread(device,uuid);
        mConnectThread.start();}
    //managing connections(Bluetooth, sending/receiving data
    public class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG,"ConnectedThread: Starting.");
            mmSocket=socket;
            InputStream tmpIn=null;
            OutputStream tmpOut=null;

            //dismiss the progressdialog when connection is established
            try {
                mProgressDialog.dismiss();
            }catch(NullPointerException e)      //catch 2nd device's progress dialogue closing of dialogue that is not there
            {
                Log.e(TAG,"NULL Pointer"+e.getMessage());
            }


            try {
                tmpIn=mmSocket.getInputStream();
                tmpOut=mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream=tmpIn;
            mmOutStream=tmpOut;
        }
        public void run(){
            byte[] buffer=new byte[1024]; //buffer store for the stream

            int bytes; //bytes returned from read

            //Keep listening to the InputStream until an exception occurs
            while(true){
                //Read from the InputStream
                try {
                    bytes=mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0,bytes);
                    received=incomingMessage;
                    Log.d(TAG,"InputStream: "+incomingMessage);
                } catch (IOException e) {
                    Log.e(TAG,"write: Error reading inputStream. "+e.getMessage());
                    break;
                }

            }
        }
        //Call this from the main activity to send data to the remote device
        public void write(byte[] bytes)
        {
            String text=new String(bytes, Charset.defaultCharset());
            Log.d(TAG,"write: Writing to outputstream: "+text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG,"write: Error writing to outputstream. "+e.getMessage());
            }
        }

        //Call this from the main activity to shutdown the connection
        public void cancel()
        {
            try{
                mmSocket.close();
            }catch(IOException e){
            }
        }
    }
    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) { //manage transmission of data
        Log.d(TAG,"connected: Starting.");

        //Start the thread to manage the connection and perform transmissions
        mConnectedThread=new ConnectedThread(mmSocket);   //Access the ConnectedThread
        mConnectedThread.start();
    }
    public void write(byte[] out)
    {
        //Synchronize a copy of the ConnectedThread
        Log.d(TAG,"write: Write Called.");
        mConnectedThread.write(out);
    }
    public String receiveText()
    {
        return received;
    }




}
