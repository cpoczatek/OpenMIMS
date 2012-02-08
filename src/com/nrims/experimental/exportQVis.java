/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims.experimental;

import com.nrims.HSIProcessor;
import com.nrims.HSIProps;
import com.nrims.MimsPlus;
import com.nrims.UI;
import com.nrims.MimsJFileChooser;
import java.io.FileNotFoundException;
import java.io.File;


/**
 *
 * @author cpoczatek
 */

/*
    //Example dialog to pass alpha min,max values
    String list = "";
    ij.gui.GenericDialog gd = new ij.gui.GenericDialog("Alpha min,max");
    gd.addStringField("Alpha:", list, 20);
    gd.showDialog();
    if (gd.wasCanceled()) {
        return;
    }
    list = gd.getNextString();

    String[] valstrings = list.split(",");
    if(valstrings.length!=2) return;

    int minA = 0;
    int maxA = 0;

    minA = Integer.parseInt(valstrings[0]);
    maxA = Integer.parseInt(valstrings[1]);

    File file;
    MimsJFileChooser fc = new MimsJFileChooser(this);
    int returnVal = fc.showSaveDialog(this);
    if (returnVal == MimsJFileChooser.CANCEL_OPTION) {
        return;
    }
    String fileName = fc.getSelectedFile().getName();

    //should it grab the current image here or in method?
    com.nrims.experimental.exportQVis.exportHSI_RGBA(this, minA, maxA, fileName);
     */


public class exportQVis {

    /**
     * Write a QVis RGBA version of an HSI image
     * 
     * @param ui
     * @param minA
     * @param maxA
     */
    public static void exportHSI_RGBA(com.nrims.UI ui, int minA, int maxA, String name) {
        // Testing, to be moved to a manager class???
        //this should be passed something else

        MimsPlus img;

        try {
            img = (MimsPlus) ij.WindowManager.getCurrentImage();
        } catch (Exception e) {
            return;
        }

        if (img.getMimsType() != MimsPlus.HSI_IMAGE) {
            return;
        }

        

        HSIProps props = img.getHSIProps();
        MimsPlus denimg = ui.getMassImage(props.getDenMassIdx());
        float[][] hsitables = HSIProcessor.getHsiTables();
        int r, g, b;
        double rScale = 65535.0 / (props.getMaxRatio() - props.getMinRatio());

        float[] pix = (float[]) img.internalRatio_filtered.getProcessor().getPixels();

        //hard coded use of denominator pixels for alpha
        //should be looking at hsiprops for appropriate alpha "channel"
        //may be dependent on non-unitless (ie actual mass counts) alpha min/max

        float[] denpix = (float[]) img.internalDenominator.getProcessor().getPixels();
        //for testing, to delete
        int[] foo = new int[pix.length];

        byte[][] plane_rgba = new byte[pix.length][4];

        int numIndex = props.getNumMassIdx();
        int denIndex = props.getDenMassIdx();
        int numMass = Math.round(new Float(ui.getOpener().getMassNames()[numIndex]));
        int denMass = Math.round(new Float(ui.getOpener().getMassNames()[denIndex]));

        java.io.FileOutputStream out = null;
        //testing each channel
        /*
        java.io.FileOutputStream outr = null;
        java.io.FileOutputStream outg = null;
        java.io.FileOutputStream outb = null;
        java.io.FileOutputStream outa = null;
        */
        String dir = ui.getImageDir();
        

        String fileprefix = name + "_m" + numMass + "m" + denMass + "_rgba";

        //stupid rgb max min crap
        //needs to change to not be unitless
        int rgbMax = props.getMaxRGB();
        int rgbMin = props.getMinRGB();
        if (rgbMax == rgbMin) {
            rgbMax++;
        }
        double rgbGain = 255.0 / (double) (rgbMax - rgbMin);


        try {
            //write rgba data
            out = new java.io.FileOutputStream(dir + java.io.File.separator + fileprefix + ".raw");

            //testing each channel
            /*
            outr = new java.io.FileOutputStream(dir + java.io.File.separator + "r.raw");
            outg = new java.io.FileOutputStream(dir + java.io.File.separator + "g.raw");
            outb = new java.io.FileOutputStream(dir + java.io.File.separator + "b.raw");
            outa = new java.io.FileOutputStream(dir + java.io.File.separator + "a.raw");
            */
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        for (int j = 1; j <= denimg.getStackSize(); j++) {

            img.setSlice(j, img);
            //don't call on internal images?
            //denimg.setSlice(j, true);
            pix = (float[]) img.internalRatio_filtered.getProcessor().getPixels();
            denpix = (float[]) img.internalDenominator.getProcessor().getPixels();

            //stupid rgb
            double denMax = img.internalRatio_filtered.getProcessor().getMax();
            double denMin = img.internalRatio_filtered.getProcessor().getMin();
            //double denSpan = (denMax-denMin);
            //double denGain = denSpan > 0 ? 255.0 / ( denSpan ) : 1.0 ;
            double denSpan = (props.getMaxRatio() - props.getMinRatio());
            double denGain = denMax > denMin ? 255.0 / (denSpan) : 1.0;


            for (int i = 0; i < pix.length; i++) {
                float ratioval = pix[i];

                int iratio = 0;
                if (ratioval > props.getMinRatio()) {
                    if (ratioval < props.getMaxRatio()) {
                        iratio = (int) ((ratioval - props.getMinRatio()) * rScale);
                        if (iratio < 0) {
                            iratio = 0;
                        } else if (iratio > 65535) {
                            iratio = 65535;
                        }
                    } else {
                        iratio = 65535;
                    }
                } else {
                    iratio = 0;
                }

                r = (int) (hsitables[0][iratio] * 255) << 16;
                g = (int) (hsitables[1][iratio] * 255) << 8;
                b = (int) (hsitables[2][iratio] * 255);

                foo[i] = r + g + b;

                plane_rgba[i][0] = (byte) (hsitables[0][iratio] * 255);
                plane_rgba[i][1] = (byte) (hsitables[1][iratio] * 255);
                plane_rgba[i][2] = (byte) (hsitables[2][iratio] * 255);
                if((plane_rgba[i][0]>255) || (plane_rgba[i][1]>255) ||(plane_rgba[i][2]>255)) {
                    System.out.println("Error: "+r+","+g+","+b);
                }

                //alpha should be better...
                //testing hard coded min=800 max=8000;
                //int min = 800;
                //int max = 8000;

                int min = minA;
                int max = maxA;

                double alpha = java.lang.Math.max(denpix[i], min);
                alpha = java.lang.Math.min(alpha, max);
                alpha = (alpha-min) / (max - min);

/*
                //kludge to do ratio value alpha-ing
                alpha = java.lang.Math.max(ratioval, min);
                alpha = java.lang.Math.min(alpha, max);
                alpha = (alpha-min) / (max - min);
                if(denpix[i] < 5000.0) {
                    alpha = (double)0.0;
                }
*/
                plane_rgba[i][3] = (byte) (255 * alpha);

            }

            try {
                for (int i = 0; i < plane_rgba.length; i++) {
                    out.write(plane_rgba[i]);

                    //testing each channel
                    /*
                    outr.write(plane_rgba[i][0]);
                    outg.write(plane_rgba[i][1]);
                    outb.write(plane_rgba[i][2]);
                    outa.write(plane_rgba[i][3]);
                    */
                }
                out.flush();

                //testing each channel
                /*
                outr.flush();
                outg.flush();
                outb.flush();
                outa.flush();
                */

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

        }
        try {
            out.close();

            //testing each channel
            /*
            outr.close();
            outg.close();
            outb.close();
            outa.close();
            */
        } catch (Exception e) {
            e.printStackTrace();
        }

        java.io.BufferedWriter bw = null;
        try {
            //write header file
            bw = new java.io.BufferedWriter(new java.io.FileWriter(new java.io.File(dir + java.io.File.separator + fileprefix + ".dat")));
            /*
             * Example header file content
             * 
            ObjectFileName: 090826-3-4_PT-4x999_concat_rgba.raw
            TaggedFileName: ---
            Resolution: 256 256 449
            SliceThickness: 1 1 1
            Format: UCHAR4
            NbrTags: 0
            ObjectType: TEXTURE_VOLUME_OBJECT
            ObjectModel: RGBA
            GridType: EQUIDISTANT
            # some comment
            # some other comment
             */
            int x = denimg.getWidth();
            int y = denimg.getHeight();
            int z = denimg.getStackSize();
            String rname = fileprefix + ".raw";

            bw.write("ObjectFileName: " + rname + "\n");
            bw.write("TaggedFileName: ---" + "\n");
            bw.write("Resolution: " + x + " " + y + " " + z + "\n");
            bw.write("SliceThickness: 1 1 1" + "\n");
            bw.write("Format: UCHAR4\n");
            bw.write("NbrTags: 0\n");
            bw.write("ObjectType: TEXTURE_VOLUME_OBJECT\n");
            bw.write("ObjectModel: RGBA\n");
            bw.write("GridType: EQUIDISTANT\n");

            //write some metadata as comments
            bw.write("# data_file: " + ui.getOpener().getImageFile().getName() + "\n");
            bw.write("# ratio_min: " + props.getMinRatio() + "\n");
            bw.write("# ratio_max: " + props.getMaxRatio() + "\n");
            bw.write("# alpha_min: " + minA + "\n");
            bw.write("# alpha_max: " + maxA + "\n");
            bw.write("# medianized: " + ui.getMedianFilterRatios() + "\n");
            bw.write("# med_radius: " + ui.getHSIView().getMedianRadius() + "\n");
            bw.write("# window: " + ui.getIsWindow() + "\n");
            bw.write("# window_radius: " + ui.getWindowRange() + "\n");
            
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
