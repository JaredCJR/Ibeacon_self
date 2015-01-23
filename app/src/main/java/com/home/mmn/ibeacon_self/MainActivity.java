package com.home.mmn.ibeacon_self;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.HandlerThread;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Handler;



public class MainActivity extends ActionBarActivity {

    private String hexScanRecord = "error";
    private int major = -999;
    private int minor =-999;
    private int get_rssi = -999;
    private String get_uuid = "error";
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 2;
    private double dist = -1;
    private int txPower = -59;
    private beacon[] myIbeacon = new beacon[6];//有5個 beacon

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            int startByte = 2;
            boolean patternFound = false;
            while (startByte <= 5) {
                if (    ((int) scanRecord[startByte + 2] & 0xff) == 0x02 && //Identifies an iBeacon
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15) { //Identifies correct data length
                    patternFound = true;
                    hexScanRecord = bytesToHex(scanRecord);


                    break;
                }
                startByte++;
            }

            if (patternFound) {
                //Convert to hex String
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte+4, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);

                //Here is your UUID
                String uuid =  hexString.substring(0,8) + "-" +
                        hexString.substring(8,12) + "-" +
                        hexString.substring(12,16) + "-" +
                        hexString.substring(16,20) + "-" +
                        hexString.substring(20,32);

                //Here is your Major value
                major = (scanRecord[startByte+20] & 0xff) * 0x100 + (scanRecord[startByte+21] & 0xff);
                //Here is your Minor value
                minor = (scanRecord[startByte+22] & 0xff) * 0x100 + (scanRecord[startByte+23] & 0xff);
                get_uuid = uuid;
                get_rssi = rssi;
                dist = calculateAccuracy(txPower,get_rssi);

                myIbeacon[minor] = new beacon(uuid ,major , minor ,txPower , rssi ,dist);

                Log.v("=====>", "hex_scanRecord:" + hexScanRecord);
                Log.v("=====>", "UUID:"+myIbeacon[minor].get_uuid());
                Log.v("=====>", "minor:"+minor);
                Log.v("=====>", "RSSI:"+myIbeacon[minor].get_rssi());
                Log.v("=====>", "distance:"+myIbeacon[minor].get_dist());
            }
        }
    };

    //找到UI工人的經紀人，這樣才能派遣工作  (找到顯示畫面的UI Thread上的Handler)
    private Handler mUI_Handler = new Handler();

    //宣告特約工人的經紀人
    private Handler mThreadHandler;

    //宣告特約工人
    private HandlerThread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v("=====>", "first scan");
        find_beacon_thread();


        new Handler().postDelayed(new Runnable(){
            public void run() {
                //do something after 4 sec
                Log.v("=====>", "finish wait for 4 sec");
                Log.v("=====>", "second scan");
                find_beacon_thread();
            }
        }, 4000);   //4秒

        new Handler().postDelayed(new Runnable(){
            public void run() {
                //do something after 4 sec
                Log.v("=====>", "finish wait for 8 sec");
                Log.v("=====>", "third scan");
                find_beacon_thread();
            }
        }, 8000);   //4秒



    }


    public void find_beacon_thread()
    {

        //聘請一個特約工人，有其經紀人派遣其工人做事 (另起一個有Handler的Thread)
        mThread = new HandlerThread("find_beacons");
        //讓Worker待命，等待其工作 (開啟Thread)
        mThread.start();
        //找到特約工人的經紀人，這樣才能派遣工作 (找到Thread上的Handler)
        mThreadHandler=new Handler(mThread.getLooper());
        //請經紀人指派工作名稱 r，給工人做
        mThreadHandler.post(r1);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    /**
     * bytesToHex method
     * Found on the internet
     * http://stackoverflow.com/a/9855338
     */
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    //Accuracy here means distance in meters
    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }

    public void find_beacon()
    {
        //final BluetoothAdapter mBluetoothAdapter;

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothAdapter.startLeScan(mLeScanCallback);


        /**
         * Checks if Bluetooth is enabled on device
         * Use this within and Activity
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }

        /**
         * Stop after 3 seconds
         */
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, 3000);
    }


    //工作名稱 r 的工作內容

    private Runnable r1 =new Runnable () {
        public void run() {
            find_beacon();
/*           顯示UI
             http://j796160836.pixnet.net/blog/post/28766165-%5Bandroid%5D-%E5%A4%9A%E5%9F%B7%E8%A1%8C%E7%B7%92-handler%E5%92%8Cthread%E7%9A%84%E9%97%9C%E4%BF%82
            //請經紀人指派工作名稱 r，給工人做
            mUI_Handler.post(r2);*/
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //移除工人上的工作
        if (mThreadHandler != null) {
            mThreadHandler.removeCallbacks(r1);
        }

        //解聘工人 (關閉Thread)
        if (mThread != null) {
            mThread.quit();
        }
    }
}
