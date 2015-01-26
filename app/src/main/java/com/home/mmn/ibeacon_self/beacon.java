package com.home.mmn.ibeacon_self;




public class beacon {
    private static final int REQUEST_ENABLE_BT = 2;
    private double dist = 9999;
    private int txPower = -59;
    private String hexScanRecord = "error";
    private int major = -999;
    private int minor =-999;
    private int rssi = -999;
    private String uuid = "error";


    public beacon()
    {
    }

    public beacon(String Uuid , int Major , int Minor , int TxPower , int Rssi ,double Distance)
    {
        this.uuid = Uuid;
        this.major=Major;
        this.minor=Minor;
        this.txPower=TxPower;
        this.rssi=Rssi;
        this.dist=Distance;
    }

    public String get_uuid()
    {
        return this.uuid;
    }

    public int get_major()
    {
        return this.major;
    }

    public int get_minor()
    {
        return this.minor;
    }

    public int get_txPower()
    {
        return this.txPower;
    }

    public int get_rssi()
    {
        return this.rssi;
    }

    public double get_dist()
    {
        return this.dist;
    }


}
