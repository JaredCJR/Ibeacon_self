package com.home.mmn.ibeacon_self;


import android.util.Log;


public class positioning_engine {

    private static int beacon_number = 5;
    private beacon[] myIbeacon = new beacon[(beacon_number+1)];//有5個 beacon
    private beacon_circle circle_1;
    private beacon_circle circle_2;
    private beacon_circle circle_3;
    private beacon_circle[] circles = new beacon_circle[5];
    private circle_intersection_pos sect_pos[]= new circle_intersection_pos[6];
    private circle_intersection_pos last_time_sect_pos[] = new circle_intersection_pos[6];
    private circle_intersection_pos pos_user;
    private cross_pos_and_dist[] pos_dist_combine = new cross_pos_and_dist[15];
    private cross_pos_and_dist[] nearest_combine = new cross_pos_and_dist[3];
    private circle_intersection_pos critical_cross_point[] = new circle_intersection_pos[3];
    private double[] mapping_px_radius = new double[5];
    /*private double mapping_px_radius_2 = -1;
    private double mapping_px_radius_3 = -1;
    private double mapping_px_radius_4 = -1;
    private double mapping_px_radius_5 = -1;*/

    positioning_engine()//constructor
    {


    }

    public void start_positioning(beacon[] Ibeacon)//All work is done in here!
    {
        this.myIbeacon = Ibeacon;
        put_correct_radius_to_circles();
        //beacon_sort(myIbeacon);//Useful?
        beacon_circle_sort(circles);//Get the circles array according to their radius(px) in increasing
        circle_1 = sorted_circle[0];
        circle_2 = sorted_circle[1];
        circle_3 = sorted_circle[2];
        calc_all_cross_points(circle_1,circle_2,circle_3);
        get_critical_3_cross_points_and_user_position();
    }

    public void put_correct_radius_to_circles()
    {
        //Assign radius to corresponding beacons(according to minor number).
        for(int i=0;i<beacon_number;i++)
        {
            if(circles[0].get_minor()==myIbeacon[i].get_minor())
            {
                circles[0].set_r(myIbeacon[i].get_dist());
            }
            else if(circles[1].get_minor()==myIbeacon[i].get_minor())
            {
                circles[1].set_r(myIbeacon[i].get_dist());
            }
            else if(circles[2].get_minor()==myIbeacon[i].get_minor())
            {
                circles[2].set_r(myIbeacon[i].get_dist());
            }
            else if(circles[3].get_minor()==myIbeacon[i].get_minor())
            {
                circles[3].set_r(myIbeacon[i].get_dist());
            }
            else if(circles[4].get_minor()==myIbeacon[i].get_minor())
            {
                circles[4].set_r(myIbeacon[i].get_dist());
            }
        }
        mapping_meter_to_px();
    }

    public beacon[] get_sorted_beacon()
    {
        return sorted_beacon;
    }


    public void set_circles(beacon_circle circle1,beacon_circle circle2,beacon_circle circle3,beacon_circle circle4,beacon_circle circle5)
    {
        this.circles[0]=circle1;
        this.circles[1]=circle2;
        this.circles[2]=circle3;
        this.circles[3]=circle4;
        this.circles[4]=circle5;
    }

    public void mapping_meter_to_px()//mapping: 1 meter = 200px
    {
        for(int i =0;i<beacon_number;i++)
            this.circles[i].set_r( ( circles[i].get_r()*200 ) );
    }


    //http://www.vogella.com/tutorials/JavaAlgorithmsQuicksort/article.html
    private beacon[] sorted_beacon;
    private int sorted_beacon_array_length;

    public void beacon_sort(beacon[] values) {
        // check for empty or null array
        if (values ==null || values.length==0){
            return;
        }
        sorted_beacon = values;
        sorted_beacon_array_length = values.length;
        quicksort_beacon(0, sorted_beacon_array_length - 1);
    }

    private void quicksort_beacon(int low, int high) {
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
            quicksort_beacon(low, j);
        if (i < high)
            quicksort_beacon(i, high);
    }



    public void beacon_swap(beacon array[], int index1, int index2)
// pre: array is full and index1, index2 < array.length
// post: the values at indices 1 and 2 have been swapped
    {
        beacon temp = array[index1];           // store the first value in a temp
        array[index1] = array[index2];      // copy the value of the second into the first
        array[index2] = temp;               // copy the value of the temp into the second
    }


    public void calc_all_cross_points(beacon_circle A,beacon_circle B, beacon_circle C)
    {
        calc_cirlce_cross_points(A,B,0);
        calc_cirlce_cross_points(B,C,2);
        calc_cirlce_cross_points(A,C,4);
    }


    public void calc_cirlce_cross_points(beacon_circle A,beacon_circle B,int store_index)
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
                {
                    sect_pos[store_index] = new circle_intersection_pos(sect_x1,sect_y1);
                    sect_pos[(store_index+1)] = new circle_intersection_pos(sect_x2,sect_y2);

                    //save for next scan if the signal is not stable
                    last_time_sect_pos[store_index] = new circle_intersection_pos(sect_x1,sect_y1);
                    last_time_sect_pos[(store_index+1)] = new circle_intersection_pos(sect_x2,sect_y2);

                    Log.v("=====>", "sect_pos" + store_index + ": ( " + sect_pos[store_index].get_x() + " , " + sect_pos[store_index].get_y() + " )");
                    Log.v("=====>", "sect_pos"+(store_index+1)+": ( "+sect_pos[(store_index+1)].get_x()+" , "+sect_pos[(store_index+1)].get_y()+" )");
                }
                else//一交點
                {
                    sect_pos[store_index] = new circle_intersection_pos(sect_x1,sect_y1);
                    sect_pos[(store_index+1)] = new circle_intersection_pos(sect_x1,sect_y1);
                    //Toast.makeText(getApplicationContext(), "只有1個交點！!!!!!!!", Toast.LENGTH_SHORT).show();
                    Log.v("=====>", "只有1個交點！!!!!!!!");
                    //Log.v("=====>", "sect_pos"+store_index": ( "+sect_pos[store_index].get_x()+" , "+sect_pos[store_index].get_y()+" )");
                    //Log.v("=====>", "sect_pos"+(store_index+1)": ( "+sect_pos[(store_index+1)].get_x()+" , "+sect_pos[(store_index+1)].get_y()+" )");
                }
            }
            else//沒有交點時
            {
                sect_pos[store_index] = new circle_intersection_pos();
                sect_pos[(store_index+1)] = new circle_intersection_pos();
                    //Toast.makeText(getApplicationContext(), "沒有交點!!!!!", Toast.LENGTH_SHORT).show();
                    Log.v("=====>", "沒有交點！!!!!!!!");

            }
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
                {
                    sect_pos[store_index] = new circle_intersection_pos(sect_x1,sect_y1);
                    sect_pos[(store_index+1)]= new circle_intersection_pos(sect_x1,sect_y2);

                    //save for next scan if the signal is not stable
                    last_time_sect_pos[store_index] = new circle_intersection_pos(sect_x1,sect_y1);
                    last_time_sect_pos[(store_index+1)] = new circle_intersection_pos(sect_x1,sect_y2);

                    Log.v("=====>", "sect_pos"+store_index+": ( "+sect_pos[store_index].get_x()+" , "+sect_pos[store_index].get_y()+" )");
                    Log.v("=====>", "sect_pos"+(store_index+1)+": ( "+sect_pos[(store_index+1)].get_x()+" , "+sect_pos[(store_index+1)].get_y()+" )");
                }
                else//一交點
                {
                    sect_pos[store_index] = new circle_intersection_pos(sect_x1,sect_y1);
                    sect_pos[(store_index+1)] = new circle_intersection_pos(sect_x1,sect_y1);
                    //Toast.makeText(getApplicationContext(), "only one cross point,remain the same 2 cross point!", Toast.LENGTH_SHORT).show();
                    Log.v("=====>","only one cross point,remain the same 2 cross point!");
                    //Log.v("=====>", "sect_pos" + store_index": ( " + sect_pos[store_index].get_x() + " , " + sect_pos[store_index].get_y() + " )");
                    //Log.v("=====>", "sect_pos" + (store_index + 1)": ( " + sect_pos[(store_index + 1)].get_x() + " , " + sect_pos[(store_index + 1)].get_y() + " )");
                }
            }
            else//沒有交點時
            {
                sect_pos[store_index] = new circle_intersection_pos();
                sect_pos[(store_index+1)] = new circle_intersection_pos();
                //Toast.makeText(getApplicationContext(), "沒有交點!!!!!", Toast.LENGTH_SHORT).show();
                Log.v("=====>", "沒有交點！!!!!!!!");
            }
        }
    }

    public void get_critical_3_cross_points_and_user_position()
    {
        for(int i=0;i<6;i++)
        {
            try
            {
                if( ( sect_pos[i].get_x()==-9999 ) && ( sect_pos[i].get_y()==-9999  ) )
                {

                    sect_pos[i].set_x(last_time_sect_pos[i].get_x());
                    sect_pos[i].set_y(last_time_sect_pos[i].get_y());
                }
            }
            catch (Exception ex)
            {
                Log.v("=====>", "The cross point is too less!");
                Log.v("=====>", "Prediction,due to the available data is too less");
                //Prediction,due to the available data is too less
                double predict_x = (( sorted_circle[0].get_x() + sorted_circle[1].get_x() + sorted_circle[2].get_x()  )/3);
                double predict_y =( ( sorted_circle[0].get_y() + sorted_circle[1].get_y() + sorted_circle[2].get_y()  )/3);
                sect_pos[i]=new circle_intersection_pos(predict_x,predict_y);

            }
        }


        pos_dist_combine[0] = new cross_pos_and_dist(sect_pos[0],sect_pos[1]);// 15 combinations(length) for 6 cross ponits
        pos_dist_combine[1] = new cross_pos_and_dist(sect_pos[0],sect_pos[2]);
        pos_dist_combine[2] = new cross_pos_and_dist(sect_pos[0],sect_pos[3]);
        pos_dist_combine[3] = new cross_pos_and_dist(sect_pos[0],sect_pos[4]);
        pos_dist_combine[4] = new cross_pos_and_dist(sect_pos[0],sect_pos[5]);
        pos_dist_combine[5] = new cross_pos_and_dist(sect_pos[1],sect_pos[2]);
        pos_dist_combine[6] = new cross_pos_and_dist(sect_pos[1],sect_pos[3]);
        pos_dist_combine[7] = new cross_pos_and_dist(sect_pos[1],sect_pos[4]);
        pos_dist_combine[8] = new cross_pos_and_dist(sect_pos[1],sect_pos[5]);
        pos_dist_combine[9] = new cross_pos_and_dist(sect_pos[2],sect_pos[3]);
        pos_dist_combine[10] = new cross_pos_and_dist(sect_pos[2],sect_pos[4]);
        pos_dist_combine[11] = new cross_pos_and_dist(sect_pos[2],sect_pos[5]);
        pos_dist_combine[12] = new cross_pos_and_dist(sect_pos[3],sect_pos[4]);
        pos_dist_combine[13] = new cross_pos_and_dist(sect_pos[3],sect_pos[5]);
        pos_dist_combine[14] = new cross_pos_and_dist(sect_pos[4],sect_pos[5]);

        cross_pos_and_dist_sort(pos_dist_combine);//Sorting it according to their distance in increasing.

        for(int i =0;i<3;i++)
        {
            nearest_combine[i]=pos_dist_combine[i];//Get the 3 most shorted distance combination
        }

        unique_pos(nearest_combine);//get critical_cross_point[3] combination
        calc_where_is_user(critical_cross_point);//get user position:pos_user

    }

    private cross_pos_and_dist[] sorted_combination;
    private int sorted_combination_length;

    public void cross_pos_and_dist_sort(cross_pos_and_dist[] values) {
        // check for empty or null array
        if (values ==null || values.length==0){
            return;
        }
        sorted_combination = values;
        sorted_combination_length = values.length;
        quicksort_cross_pos_and_dist(0, sorted_combination_length - 1);
    }



    private void quicksort_cross_pos_and_dist(int low, int high) {
        int i = low, j = high;
        // Get the pivot element from the middle of the list
        cross_pos_and_dist pivot = sorted_combination[low + (high-low)/2];

        // Divide into two lists
        while (i <= j) {
            // If the current value from the left list is smaller then the pivot
            // element then get the next element from the left list
            while (sorted_combination[i].get_dist() < pivot.get_dist()) {
                i++;
            }
            // If the current value from the right list is larger then the pivot
            // element then get the next element from the right list
            while (sorted_combination[j].get_dist() > pivot.get_dist()) {
                j--;
            }

            // If we have found a values in the left list which is larger then
            // the pivot element and if we have found a value in the right list
            // which is smaller then the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                cross_pos_and_dist_swap(sorted_combination, i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksort_cross_pos_and_dist(low, j);
        if (i < high)
            quicksort_cross_pos_and_dist(i, high);
    }

    public void cross_pos_and_dist_swap(cross_pos_and_dist array[], int index1, int index2)
// pre: array is full and index1, index2 < array.length
// post: the values at indices 1 and 2 have been swapped
    {
        cross_pos_and_dist temp = array[index1];           // store the first value in a temp
        array[index1] = array[index2];      // copy the value of the second into the first
        array[index2] = temp;               // copy the value of the temp into the second
    }

    public void unique_pos(cross_pos_and_dist[] A)  //choose 3 unique position from 6 position made of 3 same combination
    {
        circle_intersection_pos B[] = new circle_intersection_pos[6];
        B[0] = new circle_intersection_pos(A[0].get_x1() , A[0].get_y1());
        B[1] = new circle_intersection_pos(A[0].get_x2() , A[0].get_y2());
        B[2] = new circle_intersection_pos(A[1].get_x1() , A[1].get_y1());
        B[3] = new circle_intersection_pos(A[1].get_x2() , A[1].get_y2());
        B[4] = new circle_intersection_pos(A[2].get_x1() , A[2].get_y1());
        B[5] = new circle_intersection_pos(A[2].get_x2() , A[2].get_y2());


        int w =0;

        for(int i =0;i<6;i++)
        {
            int z = 1;
            while( ((i+z)<6) )
            {
                if(  ( B[i].get_x() == B[i+z].get_x() )  &&  (B[i].get_y() == B[i+z].get_y() )  )
                {
                    critical_cross_point[w] = B[i];//copy the A[i] reference to critical_cross_point[w]  (meaning point to same memory)
                    w++;
                    z++;
                    break;

                }
                else
                {
                    z++;
                }
            }

        }
    }

    public void calc_where_is_user(circle_intersection_pos[] A)
    {

        Log.v("=====>", "Start calc_where_is_user:");
        //http://yu-li-liang.blogspot.tw/2012/10/blog-post_24.html
        try{
            pos_user=new circle_intersection_pos(  ( (A[0].get_x()+A[1].get_x()+A[2].get_x()) /3),    ( (A[0].get_y()+A[1].get_y()+A[2].get_y()) /3)       );
        }

        catch(Exception ex)
        {
        Log.v("=====>", ex.getMessage());
        }
    }

    public circle_intersection_pos get_user_pos()
    {
        return pos_user;
    }
//=================================================================================================
    private beacon_circle[] sorted_circle;
    private int sorted_circle_array_length;

    public void beacon_circle_sort(beacon_circle[] values) {
        // check for empty or null array
        if (values ==null || values.length==0){
            return;
        }
        sorted_circle = values;
        sorted_circle_array_length = values.length;
        quicksort_beacon(0, sorted_circle_array_length - 1);
    }

    private void quicksort_circles(int low, int high) {
        int i = low, j = high;
        // Get the pivot element from the middle of the list
        beacon_circle pivot = sorted_circle[low + (high-low)/2];

        // Divide into two lists
        while (i <= j) {
            // If the current value from the left list is smaller then the pivot
            // element then get the next element from the left list
            while (sorted_circle[i].get_r() < pivot.get_r()) {
                i++;
            }
            // If the current value from the right list is larger then the pivot
            // element then get the next element from the right list
            while (sorted_circle[i].get_r() > pivot.get_r()) {
                j--;
            }

            // If we have found a values in the left list which is larger then
            // the pivot element and if we have found a value in the right list
            // which is smaller then the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                beacon_swap(sorted_circle,i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksort_beacon(low, j);
        if (i < high)
            quicksort_beacon(i, high);
    }



    public void beacon_swap(beacon_circle array[], int index1, int index2)
// pre: array is full and index1, index2 < array.length
// post: the values at indices 1 and 2 have been swapped
    {
        beacon_circle temp = array[index1];           // store the first value in a temp
        array[index1] = array[index2];      // copy the value of the second into the first
        array[index2] = temp;               // copy the value of the temp into the second
    }


}
