/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.data;

import com.nrims.MimsPlus;

/**
 *
 * @author wang2
 */
public class ImageDataUtilities {
    public static int getSeriesSize(Opener op){
        String[] massNames = op.getMassNames();
        //ArrayList<MimsPlus> sortedMassImages = new ArrayList<MimsPlus>();
        int row = 0;
        for (int i = 1; i < massNames.length; i++){
            Double cur = new Double(massNames[i]);
            Double prev = new Double(massNames[i-1]);
            if (prev == 0 && i > 1) {
                prev = new Double(massNames[i-2]);
            }
            if (prev > cur){ 
                //check if cur is zero (in which case we may not be at the end of the row)
                //also check if row has already been set
                 if (cur != 0 && row == 0){
                    row = i;
                 }
            }
            if (row == 0 && i+1 == massNames.length) row = i+1;
        }
        return row;
    }
    public static int determineSeries(int index, Opener op){
       int series =  getSeriesSize(op);
       return ((index-(index%series))/series);
    }
}
