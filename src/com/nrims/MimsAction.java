package com.nrims;

import com.nrims.data.Opener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MimsAction implements Cloneable {
    
    public ArrayList<double[]> xyTranslationList;
    private ArrayList<Integer> droppedList;
    private ArrayList<Integer> imageIndex;
    private ArrayList<String> imageList;
    double zeros[] = {0.0, 0.0};
    private boolean isCompressed = false;
    private int blockSize =1;

    public MimsAction(UI ui, Opener im) {
        resetAction(ui, im);
    }

    public void resetAction(UI ui, Opener im) {                    
                                     
        // Size of the stack.
        //int size = ui.getMassImage(0).getNSlices();
        int size = im.getNImages();

        // Set size of member variables.
        xyTranslationList = new ArrayList<double[]>();
        droppedList = new ArrayList<Integer>();
        imageIndex = new ArrayList<Integer>();
        imageList = new ArrayList<String>();
        
        // Initialize member variables.
        for (int i = 0; i < size; i++) {         
           xyTranslationList.add(zeros);           
           droppedList.add(0);
           imageIndex.add(i);            
           imageList.add(im.getImageFile().getName());
        }
    }

    public void addPlanes(boolean pre, int n, Opener op) {
        int origSize = imageList.size();
        int startIndex;
        if (pre) {
            startIndex = 0;
        } else {
            startIndex = origSize;
        }       

        // Add planes to the action-ArrayList.
        int openerPlaneNum = 0;
        for (int i = startIndex; i < n + startIndex; i++) {           
           xyTranslationList.add(i, zeros);           
           droppedList.add(i, 0);
           imageIndex.add(i, openerPlaneNum);            
           imageList.add(i, op.getImageFile().getName());
           openerPlaneNum++;
        }
       
    }

    public String getActionRow(int plane) {
        int idx = plane-1;
        return "p:" + plane +
               "\t" + roundTwoDecimals((Double)(xyTranslationList.get(idx)[0])) + 
               "\t" + roundTwoDecimals((Double)(xyTranslationList.get(idx)[1])) +
               "\t" + droppedList.get(idx) + 
               "\t" + imageIndex.get(idx) + 
               "\t" + imageList.get(idx);
    }

    public int getSize() {
        return this.imageList.size();
    }

    public void dropPlane(int displayIndex) {
        int index = trueIndex(displayIndex);
        droppedList.set(index - 1, 1);
    }

    public void undropPlane(int trueIndex) {
        droppedList.set(trueIndex - 1, 0);
    }

    // Apply the given offset (in the x-direction)
    // to the given plane. If image is compressed,
    // apply offset to all planes within that block.
    public void setShiftX(int plane, double offset) {        
        int[] planes = new int[1];
        int tplane = trueIndex(plane);
        planes[0] = tplane;
        double meanTranslation = getXShift(plane);

        if (isCompressed)
           planes = getPlaneNumbersFromBlockNumber(plane);

        for (int i = 0; i < planes.length; i++) {
           tplane = planes[i];
           double y = xyTranslationList.get(tplane-1)[1];
           double x = xyTranslationList.get(tplane-1)[0];
           double xy[] = {offset, y};
           if (isCompressed) {              
              double diff = offset - meanTranslation;
              xy[0] = x+diff;
           }           
           xyTranslationList.set(tplane-1, xy);
        }
    }

    // Apply the given offset (in the y-direction)
    // to the given plane. If image is compressed,
    // apply offset to all planes within that block.
    public void setShiftY(int plane, double offset) {
        int[] planes = new int[1];
        int tplane = trueIndex(plane);
        planes[0] = tplane;
        double meanTranslation = getYShift(plane);

       if (isCompressed)
           planes = getPlaneNumbersFromBlockNumber(plane);

        for (int i = 0; i < planes.length; i++) {
           tplane = planes[i];
           double y = xyTranslationList.get(tplane-1)[1];
           double x = xyTranslationList.get(tplane-1)[0];
           double xy[] = {x, offset};
           if (isCompressed) {
              double diff = offset - meanTranslation;
              xy[1] = y +diff;
           }
           xyTranslationList.set(tplane-1, xy);
        }
    }

    public int getSizeMinusNumberDropped() {
      int nPlanes = 0;
      for (int i = 1; i <= getSize(); i++) {
         if (!isDropped(i))
            nPlanes++;
      }
      return nPlanes;
   }

    public int[] getPlaneNumbersFromBlockNumber(int blockNumber) {

       if (!isCompressed) {
          int[] planes = new int[1];
          planes[0] = blockNumber;
          return planes;
       }

       // Start and End 'display' planes of the blockNumber.
       int blockStart = ((blockNumber-1)*blockSize)+1;
       int blockEnd = (blockNumber*blockSize);
       if (blockEnd > getSizeMinusNumberDropped())
          blockEnd = getSizeMinusNumberDropped();

       // Assemble the array.
       ArrayList<Integer> planesArray = new ArrayList<Integer>();
       for (int i = blockStart; i <= blockEnd; i++) {
          int tplane = trueIndex(i);
          if (tplane <= getSize())
             planesArray.add(tplane);
       }
       // Convert to int[].
       int[] planes = new int[planesArray.size()];
       for (int i = 0; i < planesArray.size(); i++)
          planes[i] = planesArray.get(i);

       return planes;
    }

    // returns the X-shift for this plane.
    // If compressed than returns the mean
    // of the planes in the block.
    public double getXShift(int plane) {
       
       int[] planes = new int[1];       
       int tplane = trueIndex(plane);       
       planes[0] = tplane;

       if (isCompressed)
          planes = getPlaneNumbersFromBlockNumber(plane);

       double sumX = 0;
       for (int i = 0; i < planes.length; i++) {
          tplane = planes[i];          
          sumX += xyTranslationList.get(tplane-1)[0];          
       }       
       double xval = sumX / planes.length;
       return xval;
    }

    // returns the Y-shift for this plane.
    // If compressed than returns the mean
    // of the planes in the block.
    public double getYShift(int plane) {
       int[] planes = new int[1];
       int tplane = trueIndex(plane);
       planes[0] = plane;

       if (isCompressed)
          planes = getPlaneNumbersFromBlockNumber(plane);

       double sumY = 0;
       for (int i = 0; i < planes.length; i++) {
          tplane = planes[i];
          sumY += xyTranslationList.get(tplane-1)[1];
       }
       double yval = sumY / planes.length;
       return yval;
    }

    public int trueIndex(int dispIndex) {
        int index = 0;
        int zeros = 0;
        while (zeros < dispIndex) {
            if (droppedList.get(index) == 0) {
                zeros++;
            }
            index++;
        }
        return index;
    }

    public int displayIndex(int tIndex) {
        int zeros = 0;
        int i = 0;
        while (i < tIndex) {
            if (droppedList.get(i) == 0) {
                zeros++;
            }
            i++;
        }
        if (droppedList.get(tIndex - 1) == 0) {
            return zeros;
        } else {
            return zeros + 1;
        }
    }

    public boolean isDropped(int tIndex) {
        if (droppedList.get(tIndex-1) == 1)
            return true;
        else
            return false;
    }

    public String[] getImageList(){
       String imageListString[] = new String[imageList.size()];
       for (int i=0; i<imageListString.length; i++)
          imageListString[i] = imageList.get(i);
       return imageListString;
    }

    public boolean getIsCompressed() {
       return isCompressed;
    }

    public void setIsCompressed(boolean compressed) {
       isCompressed = compressed;
    }

    public int getBlockSize() {
       return blockSize;
    }

    public void setBlockSize(int size) {
       blockSize = size;
    }
    
    public int getOpenerIndex(int plane) {       
       return imageIndex.get(plane);       
    }
    
    public String getOpenerName(int plane) {
       return imageList.get(plane);
    }
    
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public File writeAction(File file) {
       
        // initialize variable.
        BufferedWriter bw = null;             
        
        try {
            bw = new BufferedWriter(new FileWriter(file));

            // write image state
            for (int i = 1; i <= imageList.size(); i++) {
                bw.append(getActionRow(i));
                bw.newLine();
            }
            bw.close();
            return file.getAbsoluteFile();
        } catch (IOException e) {
            System.out.println(e.getStackTrace().toString());
            return null;
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    return null;
                }
            }
        }
    }

   void setSliceImage(int plane, String file) {
        int tplane = trueIndex(plane);
        if (!imageList.get(tplane - 1).equals(file)) {
            imageList.set(tplane - 1, file);
        }   
   }
   
   double roundTwoDecimals(double d) {
        	DecimalFormat twoDForm = new DecimalFormat("#.##");
		return Double.valueOf(twoDForm.format(d));
   }
}
