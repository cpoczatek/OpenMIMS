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
        //MimsPlus curZero = null;
        /*if (massImages[0].getMassValue() != 0) 
            sortedMassImages.add(massImages[0]);
        else
            curZero = massImages[0];*/
        for (int i = 1; i < massNames.length; i++){
            Double cur = new Double(massNames[i]);
            Double prev = new Double(massNames[i-1]);
            //if (prev.getMassValue() == 0 && i > 1) prev = massImages[i-2];
            if (prev > cur){ 
                 if (cur != 0){
                    //f (curZero != null) sortedMassImages.add(curZero);
                    if (row == 0) row = i;
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
