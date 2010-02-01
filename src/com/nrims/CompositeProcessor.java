/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims;

import ij.* ;
import java.awt.* ;

/**
 *
 * @author cpoczatek
 */
public class CompositeProcessor  implements Runnable{

    /**
     * Creates a new instance of HSIProcessor
     */
    public CompositeProcessor(MimsPlus compImage) {
        this.compImage = compImage ;
        //compute_hsi_table() ;
    }

    public void finalize() {
        compImage = null ;
        compProps = null ;
    }

    private MimsPlus compImage = null ;
    private CompositeProps compProps = null ;
    private Thread fThread = null ;
/*
    private static float[] rTable = null ;
    private static float[] gTable = null ;
    private static float[] bTable = null ;

    private static final double S6_6 = Math.sqrt(6.0) / 6.0 ;
    private static final double S6_3 = Math.sqrt(6.0) / 3.0 ;
    private static final double S6_2 = Math.sqrt(6.0) / 2.0 ;
    private static final double FULLSCALE = 65535.0 / (2.0 * Math.PI);
    private static final int MAXNUMDEN	=  0 ;
    private static final int NUMERATOR	=  1 ;
    private static final int DENOMINATOR =  2 ;
    private static final int MINNUMDEN	=  3 ;
    private static final int MEANNUMDEN	=  4 ;
    private static final int SUMNUMDEN	=  5 ;
    private static final int RMSNUMDEN	=  6 ;

    private int numSlice = 1 ;
    private int denSlice = 1 ;
*/

    public void setProps(CompositeProps props) {
        if(compImage == null) return ;

        compProps = props;
        start();
    }

    public CompositeProps getProps() { return compProps ; }

    private synchronized void start() {
        if(fThread != null) {
            if(compImage.getUI().getDebug())
                compImage.getUI().updateStatus("HSIProcessor: stop and restart");
            stop();
        }
        try {
        fThread = new Thread(this);
        fThread.setPriority(fThread.NORM_PRIORITY);
        fThread.setContextClassLoader(
                Thread.currentThread().getContextClassLoader());
        if(compImage.getUI().getDebug())
                compImage.getUI().updateStatus("HSIProcessor: start");
        try { fThread.start();}
        catch( IllegalThreadStateException x){ IJ.log(x.toString()); }
        } catch (NullPointerException xn) {}
    }

    private void stop() {
        if(fThread != null) {
            fThread.interrupt();
            fThread = null ;
        }
    }

    public boolean isRunning() {
        if(fThread == null) return false ;
        return fThread.isAlive() ;
    }

    public void run( ) {

       // initialize stuff.
        MimsPlus [] ml = compImage.getUI().getMassImages() ;

        try {

            int [] compPixels;
            try{
                compPixels = (int []) compImage.getProcessor().getPixels() ;
            } catch(Exception e) { return; }

            MimsPlus[] images = this.getProps().getImages();
            int width = compImage.getWidth();
            int height = compImage.getHeight();

            int offset = 0;
            for (int y = 0; y < height && fThread != null; y++) {
                for (int x = 0; x < width && fThread != null; x++) {
                
                    
                    int outValue=0, r=0, g=0, b=0;

                    /*
                    r = 150 << 16;
                    g = 2 << 8;
                    b = 75;
                    */
                    /* actual data value
                    r = ml[indices[0]].getProcessor().getPixel(x, y) << 16;
                    g = ml[indices[1]].getProcessor().getPixel(x, y) << 8;
                    b = ml[indices[2]].getProcessor().getPixel(x, y);
                    */

                    //8 bit grayscale value
                    if(images[0]!=null)
                        r = getPixelLUT(images[0],x,y);
                    if(images[1]!=null)
                        g = getPixelLUT(images[1],x,y);
                    if(images[2]!=null)
                        b = getPixelLUT(images[2],x,y);
                    if(images[3]!=null) {
                        r = java.lang.Math.min(255, r + getPixelLUT(images[3],x,y));
                        g = java.lang.Math.min(255, g + getPixelLUT(images[3],x,y));
                        b = java.lang.Math.min(255, b + getPixelLUT(images[3],x,y));
                    }
                    //bit shifts
                    r = r << 16;
                    g = g << 8;
                    compPixels[offset] = r + g + b;


                    // System.out.print(hsiPixels[offset] + " ");
                    if (fThread == null || fThread.interrupted()) {
                        fThread = null;
                        compImage.unlock();
                        return;
                    }
                    offset++;
                }
            }
            //Scale bar colors
            //snip

            compImage.unlock();
            compImage.updateAndRepaintWindow();
            fThread = null ;

        }
        catch(Exception x) {
            compImage.unlock();
            //if(denominator != null) denominator.unlock();
            //if(numerator != null ) numerator.unlock();
            fThread = null ;
            IJ.log(x.toString());
            x.printStackTrace();
        }

    }

    public int getPixelLUT(MimsPlus img, int x, int y) {
        if(img==null) return 0;
        int val = 0;

        double min = img.getDisplayRangeMin();
        double max = img.getDisplayRangeMax();
        double range = max-min;
        float pix = img.getProcessor().getPixelValue(x, y);

        if(pix<=min)
            return 0;
        else if(pix>=max)
            return 255;

        val = java.lang.Math.round((float)((pix-min)/range)*255);

        return val;
    }



}
