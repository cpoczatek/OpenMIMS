/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.data;

import com.nrims.MimsPlus;
import com.nrims.UI;
import ij.ImagePlus;

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
    public static boolean isPeakSwitching(Opener op){
        int series =  getSeriesSize(op);
        if (series == op.getNMasses()) return false;
        else return true;
                
    }
     /**
     * Gets the information stored in the header of the image file
     * and returns it as a string. Currently used as debug data
     * output only.
     *
     * @param im a pointer to the <code>Opener</code>.
     * @return a String containing the metadata.
     */
    public static String getImageHeader(Opener im) {

        // WE HAVE TO DECIDE WHAT WE WANT.
        String[] names = im.getMassNames();
        String[] symbols = im.getMassSymbols();

        String str = "\nHeader: \n";
        str += "Path: " + im.getImageFile().getAbsolutePath() + "\n";
        str += "Masses: ";
        for (int i = 0; i < im.getNMasses(); i++) {str += names[i] + " ";}
        str += "\n";

        str += "Symbols: ";
        if(symbols!=null) {
            for (int i = 0; i < im.getNMasses(); i++) {str += symbols[i] + " ";}
        }
        str += "\n";

        str += "Pixels: " + im.getWidth() + "x" + im.getHeight() + "\n";
        
        str += "Raster (nm): " + im.getRaster() + "\n";
        str += "Duration (s): " + im.getDuration() + "\n";
        str += "Dwell time (ms/xy): " + im.getDwellTime() + "\n";
        str += "Stage Position: " + im.getPosition() + "\n";
        str += "Z Position: " + im.getZPosition() + "\n";
        str += "Sample date: " + im.getSampleDate() + "\n";
        str += "Sample hour: " + im.getSampleHour() + "\n";
        str += "Pixel width (nm): " + im.getPixelWidth() + "\n";
        str += "Pixel height (nm): " + im.getPixelHeight() + "\n";

        str += "Dead time Corrected: " + im.isDTCorrected() + "\n";
        str += "QSA Corrected: " + im.isQSACorrected() + "\n";
        if (im.isQSACorrected()) {
           if (im.getBetas() != null) {
              str += "\tBetas: ";
              for (int i = 0; i < im.getBetas().length; i++) {
                 str += im.getBetas()[i];
                 if (i < im.getBetas().length -1)
                    str += ", ";
              }
              str += "\n";
           }

           if (im.getFCObjective() > 0)
              str += "\tFC Objective: " + im.getFCObjective() + "\n";
           
        }

        
        str += "End header.\n\n";
        return str;
    }
    /**
     * Method to return title for a single image based on formatString in preferences
     * @param index the index of the image/parent of image you want title for
     * @param extension whether or not to include the file extension in the name
     * @return a formatted title string according to user preferences
     */
    public static String formatTitle(int index, boolean extension, String formatString, Opener image){
        char[] formatArray = formatString.toCharArray();
        String curString = "";
        String name = image.getImageFile().getName().toString();
        for (int i = 0; i < formatArray.length; i++) {
            char curChar = formatArray[i];
            if (curChar == 'M') {
                curString+= String.valueOf(image.getMassNames()[index]);
            } else if (curChar == 'F') {
                if (!extension){
                    curString+= name.substring(0, name.lastIndexOf("."));
                }else{
                    curString += name;
                }
            } else if (curChar == 'S') {
                if (image.getMassSymbols() != null) {
                    curString += String.valueOf(image.getMassSymbols()[index]);
                }
            }else {
                curString+= String.valueOf(curChar);
            }
        }
        int numBefore;
        if (isPeakSwitching(image)) {
            numBefore = determineSeries(index, image) + 1;
            curString = "(" + numBefore + ") " + curString;
            
        }
        return curString;
    }
    /**
     * Method to return title for a double image (ie ratio, hsi) based on formatString in preferences
     * @param numIndex index of the numerator
     * @param denIndex index of the denominator
     * @param extension whether or not to include the file extension in the name
     * @return a formatted title string according to user preferences
     */
    public static String formatTitle(int numIndex, int denIndex, boolean extension, String formatString, Opener image){
        char[] formatArray = formatString.toCharArray();
        String[] names = image.getMassNames();
        String[] symbols = image.getMassSymbols();
        String curString = "";
        String name = image.getImageFile().getName().toString();
        int numBefore;
        boolean isPeakSwitching = isPeakSwitching(image);
        for (int i = 0; i < formatArray.length; i++) {
            char curChar = formatArray[i];
            if (curChar == 'M') {
                if (isPeakSwitching) {
                    numBefore = determineSeries(numIndex, image) + 1;
                    curString = "(" + (numBefore + 1) + ")" + curString;
                }
                curString += String.valueOf(names[numIndex]) + "/";
                if (isPeakSwitching) {
                    numBefore = determineSeries(denIndex, image) + 1;
                    curString = "(" + (numBefore + 1) + ")" + curString;
                }
                curString += String.valueOf(names[denIndex]);
            } else if (curChar == 'F') {
                if (!extension){
                    curString+= name.substring(0, name.lastIndexOf("."));
                }else{
                    curString += name;
                }
            } else if (curChar == 'S') {
                if (symbols != null) {
                    curString += String.valueOf(symbols[numIndex]) + "/" + String.valueOf(symbols[denIndex]);
                }
            }else {
                curString+= String.valueOf(curChar);
            }
        }
        return curString;
    }
    /**
     * Generates a new MimsPlus image that is a stack. Whereas ratio image and
     * HSI images are single plane images by design, this method will turn it
     * into a scrollable stack.
     *
     * @param img the image (ratio or HSI images only)/
     */
    public static void generateStack(MimsPlus img, UI ui) {
        //do a few checks
        if (img == null) {
            return;
        }

        //need some reference image that's a stack
        if (ui.getMassImages()[0] == null) {
            return;
        }

        ImagePlus refimp = ui.getMassImages()[0];
        int currentslice = refimp.getSlice();

        //return is there's no stack
        if (refimp.getStackSize() == 1) {
            return;
        }
        //return if it's not a computed image, ie ratio/hsi
        if (!(img.getMimsType() == MimsPlus.RATIO_IMAGE || img.getMimsType() == MimsPlus.HSI_IMAGE)) {
            return;
        }

        ij.ImageStack stack = img.getStack();
        java.awt.image.ColorModel cm = stack.getColorModel();
        ij.ImageStack ims = new ij.ImageStack(stack.getWidth(), stack.getHeight(), cm);
        int numImages = refimp.getStackSize();

        for (int i = 1; i <= numImages; i++) {
            refimp.setSlice(i);
            if (img.getMimsType() == MimsPlus.HSI_IMAGE) {
                while (img.getHSIProcessor().isRunning()) {
                }
            }

            ims.addSlice(refimp.getStack().getSliceLabel(i), img.getProcessor().duplicate());
        }

        // Create new image
        ImagePlus newimp = new ImagePlus("Stack : " + img.getTitle(), ims);
        newimp.setCalibration(img.getCalibration());

        // Display this new stack
        newimp.show();
        newimp.setSlice(currentslice);
        refimp.setSlice(currentslice);

    }
}
