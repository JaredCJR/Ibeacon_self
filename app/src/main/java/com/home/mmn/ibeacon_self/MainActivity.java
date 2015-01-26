package com.home.mmn.ibeacon_self;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements View.OnTouchListener {

    private static int beacon_number = 5;
    private String hexScanRecord = "error";
    private int major = -999;
    private int minor =-999;
    private int get_rssi = -999;
    private String get_uuid = "error";
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 2;
    private double dist = 9999;
    private int txPower = -59;
    private beacon[] myIbeacon = new beacon[(beacon_number+1)];//有5個 beacon
    private TextView show_Coordinate;
    private LinearLayout llLayout;
    private String put_color = "RED";
    private float map_x = 0;
    private float map_y = 0;
    private float map_x_1 = -999;
    private float map_y_1 = -999;
    private float map_x_2 = -999;
    private float map_y_2 = -999;
    private float map_x_3 = -999;
    private float map_y_3 = -999;
    private double user_pos_x = -999;
    private  double user_pos_y = -999;
    private int conut_putted_beacons=0;
    private Paint p = new Paint();
    private DrawView view;
    private beacon_circle circle_1;
    private beacon_circle circle_2;
    private beacon_circle circle_3;
    private positioning_engine engine = new positioning_engine();
    private Button btn_get_position;
    private boolean stop_positioning = true;
    private int loop_count = 1;
    private boolean next_positioning = true;


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

            Log.v("=====>", "Start OnLeScan");
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

        Log.v("=====>", "Start onCreate");
        InitView();

        btn_get_position.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
//do something
                Log.v("=====>", "Start btn");
                stop_positioning = false;
                infinite_positioning();
            }
        });

        llLayout.setOnTouchListener(this);

    }

    public void infinite_positioning()
    {
        while(stop_positioning == false)
        {
            stop_positioning = true;
            Log.v("=====>", "Start infinite_positioning while");

            if(next_positioning)
            {
                Log.v("=====>", "Start infinite_positioning if");
                next_positioning = false;

                new Handler().postDelayed(new Runnable(){
                    public void run() {
                        Log.v("=====>", "Start infinite_positioning postDelayed");
                        find_beacon_thread();
                    }
                }, (1500*(loop_count-1))  );
            }


        }

    }


    public void InitView()
    {
        llLayout = (LinearLayout)findViewById(R.id.map);
        show_Coordinate = (TextView)findViewById(R.id.show_Coordinate);
        btn_get_position = (Button)findViewById(R.id.btn_position);
    }


    public void put_beacon_and_user_on_map()
    {
        Log.v("=====>", "Start put_beacon_and_user_on_map");
        view=new DrawView(this);
        view.setMinimumHeight(1000);
        view.setMinimumWidth(1000);
        //通知view組件重繪
        view.invalidate();
        //view.postInvalidate();
        llLayout.addView(view,0);//(View child, int width, int height)
    }


    public void find_beacon_thread()
    {

        for(int i =0;i<(beacon_number+1);i++) //初始5 beacon
        {
            myIbeacon[i]=new beacon();
        }

        //聘請一個特約工人，有其經紀人派遣其工人做事 (另起一個有Handler的Thread)
        mThread = new HandlerThread("find_beacons");
        //讓Worker待命，等待其工作 (開啟Thread)
        mThread.start();
        //找到特約工人的經紀人，這樣才能派遣工作 (找到Thread上的Handler)
        mThreadHandler=new Handler(mThread.getLooper());
        //請經紀人指派工作名稱 r，給工人做
        mThreadHandler.post(get_user_pos);
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
         * Stop after 1.1 seconds
         */
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, (1100*loop_count) );
    }


    //工作名稱 get_user 的工作內容

    private Runnable get_user_pos =new Runnable () {
        public void run() {
            Log.v("=====>", "Start get_user_pos");
            find_beacon();//find all beacons

/*           顯示UI
             http://j796160836.pixnet.net/blog/post/28766165-%5Bandroid%5D-%E5%A4%9A%E5%9F%B7%E8%A1%8C%E7%B7%92-handler%E5%92%8Cthread%E7%9A%84%E9%97%9C%E4%BF%82
            //請經紀人指派工作名稱 get_user_pos，給工人做
            mUI_Handler.post(r2);*/
            new Handler().postDelayed(new Runnable(){
                public void run() {

                    Log.v("=====>", "wait for 1.25 sec");
                    Log.v("=====>", "show");

                    //after we find  beacons,then we can strat positioning!
                    int find_beacon_number = 1;
                    for(int i =0;i<(beacon_number+1);i++)
                    {

                        Log.v("=====>", "beacon_number:"+beacon_number);

                        Log.v("=====>", "find_beacon_minor:"+myIbeacon[i].get_minor());
                        if(myIbeacon[i].get_minor() != -999)
                        {
                            Log.v("=====>", "find_beacon_number:"+find_beacon_number);

                            find_beacon_number++;
                        }
                    }

                    if(find_beacon_number >2)
                    {
                        engine.start_positioning(myIbeacon);//Strat positioning!
                        Log.v("=====>", "user_pos_x:"+engine.get_user_pos().get_x());
                        Log.v("=====>", "user_pos_y:"+engine.get_user_pos().get_y());
                        user_pos_x = engine.get_user_pos().get_x();
                        user_pos_y = engine.get_user_pos().get_y();


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                                put_beacon_and_user_on_map();//Get user position,the work is done!
                            }
                        });
                        //put_beacon_and_user_on_map();//Get user position,the work is done!
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Can't find more than 2 beacons,abort!", Toast.LENGTH_SHORT).show();
                    }




                     /*
                    for(int i=0;i<myIbeacon.length;i++)
                    {

                        beacon[] sorted_beacon = engine.get_sorted_beacon();//This line just for verification,not for positioning.
                        Log.v("sort=====>", "第 "+(i+1)+"個 beacon");
                        Log.v("sort=====>", "minor:"+sorted_beacon[i].get_minor());
                        Log.v("sort=====>", "RSSI:"+sorted_beacon[i].get_rssi());
                        Log.v("sort=====>", "distance:"+sorted_beacon[i].get_dist());
                    }*/

                    loop_count++;//assign next scan time
                    next_positioning = true;//Allowing the next scan
                    stop_positioning = false;


                }
            }, (1250*(loop_count) ) );   //1.25秒
        }
    };



    @Override
    protected void onDestroy() {
        super.onDestroy();
        //移除工人上的工作
        if (mThreadHandler != null) {
            mThreadHandler.removeCallbacks(get_user_pos);
        }

        //解聘工人 (關閉Thread)
        if (mThread != null) {
            mThread.quit();
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        map_x = event.getX();
        map_y = event.getY();
        conut_putted_beacons++;
        show_Coordinate.setText("X: " + map_x + ", Y: " + map_y);



        if(conut_putted_beacons==1)
        {
            map_x_1=map_x;
            map_y_1=map_y;
        }
        else if(conut_putted_beacons==2)
        {
            map_x_2=map_x;
            map_y_2=map_y;
        }
        else if(conut_putted_beacons==3)
        {
            map_x_3=map_x;
            map_y_3=map_y;
        }
        else
        {
            Toast.makeText(getApplicationContext(), "有n個beacon就要改n個判斷_Touch", Toast.LENGTH_SHORT).show();
        }

        put_beacon_and_user_on_map();
        return false;
    }

    public class DrawView extends View{

        public DrawView(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // 建立初始畫布
            //Paint p = new Paint();								// 創建畫筆
            draw_user_and_beacon_position(put_color,canvas);
        }

        private void draw_user_and_beacon_position(String color,Canvas canvas)
        {

            p.setAntiAlias(true);									// 設置畫筆的鋸齒效果。 true是去除。

            p.setColor(Color.BLUE);								// 設置色

            if(conut_putted_beacons==1)
            {
                canvas.drawCircle(map_x_1,map_y_1,30,p);
            }
            else if(conut_putted_beacons==2)
            {
                canvas.drawCircle(map_x_1,map_y_1,30,p);
                canvas.drawCircle(map_x_2,map_y_2,30,p);
            }
            else if( (conut_putted_beacons==3)  )
            {
                canvas.drawCircle(map_x_1,map_y_1,30,p);
                canvas.drawCircle(map_x_2,map_y_2,30,p);
                canvas.drawCircle(map_x_3,map_y_3,30,p);

                assign_radius_and_set_circles();

                draw_user(canvas);
            }
            else
            {
                canvas.drawCircle(map_x_1,map_y_1,30,p);
                canvas.drawCircle(map_x_2,map_y_2,30,p);
                canvas.drawCircle(map_x_3,map_y_3,30,p);
                Toast.makeText(getApplicationContext(), "有n個beacon就要改n個判斷_draw", Toast.LENGTH_SHORT).show();
                draw_user(canvas);
            }
        }
    }

    public void assign_radius_and_set_circles()
    {
        /**
         * For circle_n
         * Beacons need to be putted in order.
         * Such as   1.minor=3      2.minor=4      3.minor=5  ,etc
         */
        circle_1=new beacon_circle(map_x_1,map_y_1,3 );
        circle_2=new beacon_circle(map_x_2,map_y_2,4 );
        circle_3=new beacon_circle(map_x_3,map_y_3,5 );

        engine.set_circles(circle_1,circle_2,circle_3);
    }

    public void draw_user(Canvas canvas)
    {
        if((user_pos_x!=-999) &&(user_pos_y!=-999))
        {
            p.setColor(Color.RED);
            canvas.drawCircle((float)user_pos_x,(float)user_pos_y,40,p);
        }
    }



}
