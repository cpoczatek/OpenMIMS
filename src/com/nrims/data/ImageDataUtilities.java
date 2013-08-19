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
    /**
     *
     * @param op
     * @return 
     */
    public static int getSeriesSize(Opener op){
        String[] massNames = op.getMassNames();
        
        //Special case of single mass image files
        if(op.getNMasses()==1) {
            return 1;
        }
        
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
    /**
     * Determines which "series" the mass image at index is from.  If electric
     * peak-switching was not used, should all ways return 0.  An example, if 
     * electric peak-switching was used, 4 detectors were used, and the peaks 
     * switch once, then:
     * - there will be 8 mass images
     * - this method returns 0 for images at indices 0,1,2,3
     * - this method returns 1 for images at indices 4,5,6,7
     * @param index
     * @param op
     * @return
     */
    public static int determineSeries(int index, Opener op){
       int series =  getSeriesSize(op);
       return ((index-(index%series))/series);
    }
}
