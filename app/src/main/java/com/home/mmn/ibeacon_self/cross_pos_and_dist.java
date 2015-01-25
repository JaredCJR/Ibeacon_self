package com.home.mmn.ibeacon_self;



public class cross_pos_and_dist {//combination of calculated distance for two cross points and the cross points position

    private double pos_x1=-1;
    private double pos_y1=-1;
    private double pos_x2=-1;
    private double pos_y2=-1;
    private double distance=-1;

    cross_pos_and_dist()
    {

    }

    cross_pos_and_dist(circle_intersection_pos A,circle_intersection_pos B)
    {
        pos_x1=A.get_x();
        pos_y1=A.get_y();
        pos_x2=B.get_x();
        pos_y2=B.get_y();

        this.distance=get_2_points_distance(A,B);

    }

    private double get_2_points_distance(circle_intersection_pos A, circle_intersection_pos B)
    {
        double x1=A.get_x();
        double y1=A.get_y();
        double x2=B.get_x();
        double y2=B.get_y();

        double dist = Math.sqrt(  (  (Math.pow((x1-x2),2))  +  (Math.pow((y1-y2),2))  )  );
        return dist;
    }

    public double get_x1()
    {
        return  pos_x1;
    }

    public double get_y1()
    {
        return  pos_y1;
    }
    public double get_x2()
    {
        return  pos_x2;
    }

    public double get_y2()
    {
        return  pos_y2;
    }

    public double get_dist()
    {
        return  distance;
    }

}
