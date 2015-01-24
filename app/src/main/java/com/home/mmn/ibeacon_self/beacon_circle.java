package com.home.mmn.ibeacon_self;

/**
 * Created by Jared on 2015/1/25.
 */
public class beacon_circle {
    private double pos_x=-1;
    private double pos_y=-1;
    private double radius=-1;

    beacon_circle()
    {
    }

    beacon_circle(double x,double y,double r)
    {
        this.pos_x=x;
        this.pos_y=y;
        this.radius=r;
    }

    public double get_x()
    {
        return pos_x;
    }

    public double get_y()
    {
        return pos_y;
    }
    public double get_r()
    {
        return radius;
    }

    public void set_x(double x)
    {
        this.pos_x=x;
    }
    public void set_y(double y)
    {
        this.pos_y=y;
    }

    public void set_r(double r)
    {
        this.radius=r;
    }

}
