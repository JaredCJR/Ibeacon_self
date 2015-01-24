package com.home.mmn.ibeacon_self;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.HandlerThread;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements View.OnTouchListener {

    private static int beacon_number = 5;
    private String hexScanRecord = "error";
    private int major = -999;
    private int minor =-999;
    private int get_rssi = -999;
    private String get_uuid = "error";
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 2;
    private double dist = -999;
    private int txPower = -59;
    private beacon[] myIbeacon = new beacon[(beacon_number+1)];//有5個 beacon
    private TextView show_Coordinate;
    private LinearLayout llLayout;
    private LinearLayout put_beacon_layout;
    public String strTag = "GetFingerTouchPos";
    private String put_color = "RED";
    private float map_x = 0;
    private float map_y = 0;
    private float map_x_1 = -999;
    private float map_y_1 = -999;
    private float map_x_2 = -999;
    private float map_y_2 = -999;
    private float map_x_3 = -999;
    private float map_y_3 = -999;
    private int conut_putted_beacons=0;
    private Paint p = new Paint();
    private DrawView view;
    private beacon_circle circle_1;
    private beacon_circle circle_2;
    private beacon_circle circle_3;
    private circle_intersection_pos sect_pos[]= new circle_intersection_pos[6];
    private circle_intersection_pos pos_user;


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
                //beacon_list.add(myIbeacon[minor]);

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

        llLayout = (LinearLayout)findViewById(R.id.map);
        show_Coordinate = (TextView)findViewById(R.id.show_Coordinate);
        llLayout.setOnTouchListener(this);

        put_beacon_layout= (LinearLayout)findViewById(R.id.map_canvas);


        Log.v("=====>", "first scan");
        find_beacon_thread();

        new Handler().postDelayed(new Runnable(){
            public void run() {
                //do something after 1 sec
                Log.v("=====>", "finish wait for 2.5 sec");
                Log.v("=====>", "second scan");
                find_beacon_thread();
            }
        }, 1400);

        new Handler().postDelayed(new Runnable(){
            public void run() {
                //do something after 1 sec
                Log.v("=====>", "finish wait for 2.5 sec");
                Log.v("=====>", "second scan");
                find_beacon_thread();
            }
        }, 2800);

        new Handler().postDelayed(new Runnable(){
            public void run() {
                //do something after 1 sec
                Log.v("=====>", "finish wait for 2.5 sec");
                Log.v("=====>", "second scan");
                find_beacon_thread();
            }
        }, 4200);

        new Handler().postDelayed(new Runnable(){
            public void run() {
                //do something after 1 sec
                Log.v("=====>", "finish wait for 2.5 sec");
                Log.v("=====>", "second scan");
                find_beacon_thread();
            }
        }, 5600);



        


    }

    public void put_beacon_on_map()
    {

        view=new DrawView(this);
        view.setMinimumHeight(1000);
        view.setMinimumWidth(1000);
        //通知view組件重繪
        view.invalidate();
        llLayout.addView(view,0);//(View child, int width, int height)
    }


    public void find_beacon_thread()
    {

        for(int i =0;i<(beacon_number+1);i++) //初始5beacon
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
         * Stop after 1.1 seconds
         */
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, 1100);
    }


    //工作名稱 r 的工作內容

    private Runnable r1 =new Runnable () {
        public void run() {
            find_beacon();
/*           顯示UI
             http://j796160836.pixnet.net/blog/post/28766165-%5Bandroid%5D-%E5%A4%9A%E5%9F%B7%E8%A1%8C%E7%B7%92-handler%E5%92%8Cthread%E7%9A%84%E9%97%9C%E4%BF%82
            //請經紀人指派工作名稱 r，給工人做
            mUI_Handler.post(r2);*/


            new Handler().postDelayed(new Runnable(){
                public void run() {

                    Log.v("=====>", "finish wait for 1.25 sec");
                    Log.v("=====>", "show");
                    for(int i=0;i<myIbeacon.length;i++)
                    {
                        beacon_sort(myIbeacon);
                        Log.v("sort=====>", "第 "+(i+1)+"個 beacon");
                        Log.v("sort=====>", "minor:"+sorted_beacon[i].get_minor());
                        Log.v("sort=====>", "RSSI:"+sorted_beacon[i].get_rssi());
                        Log.v("sort=====>", "distance:"+sorted_beacon[i].get_dist());
                    }
                }
            }, 1250);   //1.25秒
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


    private beacon[] sorted_beacon;
    private int sorted_beacon_array_length;

    public void beacon_sort(beacon[] values) {
        // check for empty or null array
        if (values ==null || values.length==0){
            return;
        }
        sorted_beacon = values;
        sorted_beacon_array_length = values.length;
        quicksort(0, sorted_beacon_array_length - 1);
    }

    private void quicksort(int low, int high) {
        int i = low, j = high;
        // Get the pivot element from the middle of the list
        beacon pivot = sorted_beacon[low + (high-low)/2];

        // Divide into two lists
        while (i <= j) {
            // If the current value from the left list is smaller then the pivot
            // element then get the next element from the left list
            while (sorted_beacon[i].get_dist() < pivot.get_dist()) {
                i++;
            }
            // If the current value from the right list is larger then the pivot
            // element then get the next element from the right list
            while (sorted_beacon[j].get_dist() > pivot.get_dist()) {
                j--;
            }

            // If we have found a values in the left list which is larger then
            // the pivot element and if we have found a value in the right list
            // which is smaller then the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                beacon_swap(sorted_beacon,i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksort(low, j);
        if (i < high)
            quicksort(i, high);
    }



    public void beacon_swap(beacon array[], int index1, int index2)
// pre: array is full and index1, index2 < array.length
// post: the values at indices 1 and 2 have been swapped
    {
       beacon temp = array[index1];           // store the first value in a temp
        array[index1] = array[index2];      // copy the value of the second into the first
        array[index2] = temp;               // copy the value of the temp into the second
    }

    /*@Override
    public boolean OnTouchEvent(MotionEvent event){
        float x = event.getX();
        float y = event.getY();
        int iEventType = event.getAction();

        if(iEventType == MotionEvent.ACTION_DOWN){
            show_Coordinate.setText("Down");
        }
        else if(iEventType == MotionEvent.ACTION_UP){
            show_Coordinate.setText("Up");
        }

        show_Coordinate.setText("X: " + x + ", Y: " + y);
        Log.v(strTag, String.format("(x,y) = (%d,%d)", x, y));
        //return super.onTouchEvent(event);
        return true;
    }*/

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

        put_beacon_on_map();
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
            put_color = "BLUE";
            draw_beacons(put_color,canvas);
        }

        private void draw_beacons(String color,Canvas canvas)
        {

            p.setAntiAlias(true);									// 設置畫筆的鋸齒效果。 true是去除。

            if(color.equals("BLUE"))
            {
                p.setColor(Color.BLUE);								// 設置色
            }
            else if(color.equals("RED"))
            {
                p.setColor(Color.RED);								// 設置色
            }

            if(conut_putted_beacons==1)
            {
                canvas.drawCircle(map_x_1,map_y_1,30,p);
            }
            else if(conut_putted_beacons==2)
            {
                canvas.drawCircle(map_x_1,map_y_1,30,p);
                canvas.drawCircle(map_x_2,map_y_2,30,p);
            }
            else if(conut_putted_beacons==3)
            {
                canvas.drawCircle(map_x_1,map_y_1,30,p);
                canvas.drawCircle(map_x_2,map_y_2,30,p);
                canvas.drawCircle(map_x_3,map_y_3,30,p);
                circle_1=new beacon_circle(map_x_1,map_y_1,sorted_beacon[0].get_dist());
                circle_2=new beacon_circle(map_x_2,map_y_2,sorted_beacon[1].get_dist());
                circle_3=new beacon_circle(map_x_3,map_y_3,sorted_beacon[2].get_dist());
            }
            else
            {
                canvas.drawCircle(map_x_1,map_y_1,30,p);
                canvas.drawCircle(map_x_2,map_y_2,30,p);
                canvas.drawCircle(map_x_3,map_y_3,30,p);
                Toast.makeText(getApplicationContext(), "有n個beacon就要改n個判斷_draw", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void calc_two_circle(beacon_circle A,beacon_circle B)
    {
        //http://blog.xuite.net/andy19890411/Orz/17302463-%E3%80%90%E6%95%99%E5%AD%B8%E3%80%91%E4%BD%9C%E6%A5%AD%E5%85%AB+-+%E5%B0%8B%E6%89%BE%E5%85%A9%E5%9C%93%E4%BA%A4%E9%BB%9E%E3%80%82(2008.05.29%E5%8E%9F%E5%A7%8B%E6%AA%94OK)
        double x1=A.get_x();
        double y1=A.get_y();
        double r1=A.get_r();

        double x2=B.get_x();
        double y2=B.get_y();
        double r2=B.get_r();

        double sect_x1=-1;//intersection solution position
        double sect_y1=-1;
        double sect_x2=-1;
        double sect_y2=-1;

        if(y1!=y2)//兩圓圓心Y值不同時
        {//m= y=mx+k的x項系數、k= y=mx+k的k項常數、 a、b、c= x=(-b±√(b^2-4ac))/2a的係數
            double m=(x1-y2)/(y2-y1),k=(Math.pow(r1,2)-Math.pow(r2,2)+Math.pow(y2,2)-Math.pow(x1,2)+Math.pow(y2,2)-Math.pow(y1,2))/(2*(y2-y1));
            double a=1+Math.pow(m,2),b=2*(k*m-y2-m*y2),c=Math.pow(y2,2)+Math.pow(y2,2)+Math.pow(k,2)-2*k*y2-Math.pow(r2,2);

            if(b*b-4*a*c>=0)//有交點時
            {
                sect_x1=((-b)+Math.sqrt(b*b-4*a*c))/(2*a);//x=(-b+√(b^2-4ac))/2a
                sect_y1=m*sect_x1+k;//y=mx+k
                sect_x2=((-b)-Math.sqrt(b*b-4*a*c))/(2*a);//x=(-b-√(b^2-4ac))/2a
                sect_y2=m*sect_x2+k;//y=mx+k
                if(b*b-4*a*c>0)//兩交點
                    printf("The cross points are (%.2lf,%.2lf) and (%.2lf,%.2lf).\n",sect_x1,sect_y1,sect_x2,sect_y2);
                else//一交點
                    printf("The cross points are (%.2lf,%.2lf).\n",sect_x1,sect_y1);
            }
            else//沒有交點時
                printf("No cross points.\n");
        }
        else if((y1==y2))//兩圓圓心Y值相同時
        {//sect_x1= 兩交點的x值、 a、b、c= x=(-b±√(b^2-4ac))/2a的係數
            sect_x1=-(Math.pow(x1,2)-Math.pow(y2,2)-Math.pow(r1,2)+Math.pow(r2,2))/(2*y2-2*x1);
            double a=1,b=-2*y1,c=Math.pow(sect_x1,2)+Math.pow(x1,2)-2*x1*sect_x1+Math.pow(y1,2)-Math.pow(r1,2);
            if(b*b-4*a*c>=0)
            {
                sect_y1=((-b)+Math.sqrt(b*b-4*a*c))/(2*a);//y=(-b+√(b^2-4ac))/2a
                sect_y2=((-b)-Math.sqrt(b*b-4*a*c))/(2*a);//y=(-b-√(b^2-4ac))/2a
                if(b*b-4*a*c>0)//兩交點
                    printf("The cross points are (%.2lf,%.2lf) and (%.2lf,%.2lf).\n",sect_x1,sect_y1,sect_x1,sect_y2);
                else//一交點
                    printf("The cross points are (%.2lf,%.2lf).\n",sect_x1,sect_y1);
            }
            else//沒有交點時
                printf("No cross points.\n");
        }
    }

}
