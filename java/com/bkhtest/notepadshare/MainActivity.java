package com.bkhtest.notepadshare;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private static final String TAG ="MainActivity";
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public ArrayList<BluetoothDevice> mBTDevices=new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    BluetoothConnectionService mBluetoothConnection;
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothDevice mBTDevice;
    ListView lsDeviceAdapter;
    EditText etSend;
    TextView rText;
    Button btnSend;
    Button btnStartConnection;
    boolean enable;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state= intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,mBluetoothAdapter.ERROR);
                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG ,"onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG ,"onReceive: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG ,"onReceive: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG ,"onReceive: STATE TURNING ON");
                        break;


                }
            }
        }
    };
    private final BroadcastReceiver mReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int state= intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,mBluetoothAdapter.ERROR);
                switch(state){
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG ,"onReceive: DISCOVERABILITY ENABLED");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG ,"onReceive: DISCOVERABILITY DISABLED. ABLE TO RECEIVE CONNECTIONS");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG ,"onReceive: DISCOVERABILITY DISABLED. UNABLE TO RECEIVE CONNECTIONS");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG ,"onReceive: CONNECTING....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG,"onReceive: CONNECTED");
                        break;



                }
            }
        }
    };
    private final BroadcastReceiver mReceiver3 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName()!=null) {
                    mBTDevices.add(device);
                    Log.d(TAG, "onReceive: " + device.getName() + ":" + device.getAddress());
                    mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                    lsDeviceAdapter.setAdapter(mDeviceListAdapter);
                }
                }
            }

    };
    private final BroadcastReceiver mReceiver4 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(mDevice.getBondState()==BluetoothDevice.BOND_BONDED)
                {
                    Log.d(TAG,"mReceiver4: BOND_BONDED");
                    mBTDevice=mDevice;
                }
                if(mDevice.getBondState()==BluetoothDevice.BOND_BONDING)
                {
                    Log.d(TAG,"mReceiver4: BOND_BONDING");
                }
                if(mDevice.getBondState()==BluetoothDevice.BOND_NONE)
                {
                    Log.d(TAG,"mReceiver4: BOND_NONE");
                }
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button bluTooth=(Button)findViewById(R.id.blueT);

        etSend=(EditText)findViewById(R.id.editText);
        rText=(TextView)findViewById(R.id.rText);
        btnSend=(Button)findViewById(R.id.btnSend);
        btnStartConnection=(Button)findViewById(R.id.startConnection);

        lsDeviceAdapter=(ListView)findViewById(R.id.lsDeviceAdapter);
        mBTDevices=new ArrayList<>();
        lsDeviceAdapter.setOnItemClickListener(MainActivity.this);

        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver4,filter);

        bluTooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpBluTth();
            }
        });

        Button disco=(Button)findViewById(R.id.devFound);
        disco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListFound();
            }
        });

        Button bluePow=(Button)findViewById(R.id.bluePower);
        bluePow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                startActivity(discoverableIntent);

                IntentFilter filter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                registerReceiver(mReceiver2, filter);
            }
        });
        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnection();
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes=etSend.getText().toString().getBytes(Charset.defaultCharset());
                try {
                    mBluetoothConnection.write(bytes);
                }catch(NullPointerException e)
                {
                    Log.e(TAG,"write: String: "+e.getMessage());
                }
            }
        });
        if(enable&&mBluetoothConnection.receiveText()!=null)
        rText.setText("You got a message: \n");//+mBluetoothConnection.receiveText());


    }
    //create method for starting connection
    //remember the connection will fail and app will crash if you haven't paired first
    public void startConnection(){
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }
    //start chat service method
    public void startBTConnection(BluetoothDevice device,UUID uuid){
        Log.d(TAG,"startBTConnection: Initializing RFCOM Bluetooth Connection. ");
        mBluetoothConnection.startClient(device,uuid);//initiate dialogue box
        enable=true;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            this.unregisterReceiver(mReceiver);
        }catch(final Exception e)
        {
            Log.e(TAG,"onDestroy: mReceiver: "+e.getMessage());
        }
        try {
            this.unregisterReceiver(mReceiver2);
        }catch(final Exception e)
        {
            Log.e(TAG,"onDestroy: mReceiver2: "+e.getMessage());
        }
        try {
            this.unregisterReceiver(mReceiver3);
        }catch(final Exception e)
        {
            Log.e(TAG,"onDestroy: mReceiver3: "+e.getMessage());
        }
        try {
            this.unregisterReceiver(mReceiver4);
        }catch(final Exception e)
        {
            Log.e(TAG,"onDestroy: mReceiver4: "+e.getMessage());
        }

    }
    // Create a BroadcastReceiver for ACTION_FOUND.

    protected void setUpBluTth()
    {
        final int REQUEST_ENABLE_BT=1;
        if(mBluetoothAdapter==null){

        }
        else if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            IntentFilter filter = new IntentFilter(mBluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver, filter);
        }
        if(mBluetoothAdapter.isEnabled())
        {
          mBluetoothAdapter.disable();

            IntentFilter filter= new IntentFilter(mBluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver,filter);
        }
    }
    protected void getListFound()
    {

        Log.d(TAG, "mBTDevices: LOOKING FOR UNPAIRED DEVICES");
        if(mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();

            Log.d(TAG, "mBTDevices: CANCELING DISCOVERY");

            //check permissions for versions greater than Lollipop
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver3,filter);
        }
        if(!mBluetoothAdapter.isDiscovering())
        {
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver3,filter);
        }

    }


    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //cancel discovery because it's very labor intensive
        mBluetoothAdapter.cancelDiscovery();
        Log.d(TAG,"onClickItem: You clicked on a device");
        String deviceName=mBTDevices.get(position).getName();
        String deviceAddress=mBTDevices.get(position).getAddress();

        Log.d(TAG,"onClickItem: deviceName = "+deviceName);
        Log.d(TAG,"onClickItem: deviceAddress = "+deviceAddress);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG,"Trying to pair with "+deviceName);
            mBTDevices.get(position).createBond();

            mBTDevice=mBTDevices.get(position);
            mBluetoothConnection=new BluetoothConnectionService(MainActivity.this);
        }


    }
}
