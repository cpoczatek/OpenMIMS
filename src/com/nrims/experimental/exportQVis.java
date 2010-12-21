/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims.experimental;

import com.nrims.HSIProcessor;
import com.nrims.HSIProps;
import com.nrims.MimsPlus;
import com.nrims.UI;
import java.io.FileNotFoundException;

/**
 *
 * @author cpoczatek
 */
public class exportQVis {

    public static void exportHSI_RGBA(com.nrims.UI ui) {
        // Testing, to be moved to a manager class???

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

        float[] pix = (float[]) img.internalRatio.getProcessor().getPixels();
        float[] denpix = (float[]) img.internalDenominator.getProcessor().getPixels();
        //for testing, to delete
        int[] foo = new int[pix.length];

        byte[][] plane_rgba = new byte[pix.length][4];

        int numIndex = props.getNumMassIdx();
        int denIndex = props.getDenMassIdx();
        int numMass = Math.round(new Float(ui.getOpener().getMassNames()[numIndex]));
        int denMass = Math.round(new Float(ui.getOpener().getMassNames()[denIndex]));

        java.io.FileOutputStream out = null;
        String dir = ui.getImageDir();
        String fileprefix = ui.getImageFilePrefix();
        fileprefix += "_m" + numMass + "m" + denMass + "_rgba";

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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        for (int j = 1; j < denimg.getStackSize(); j++) {

            img.setSlice(j, img);
            //don't call on internal images?
            //denimg.setSlice(j, true);
            pix = (float[]) img.internalRatio.getProcessor().getPixels();
            denpix = (float[]) img.internalDenominator.getProcessor().getPixels();

            //stupid rgb
            double denMax = img.internalRatio.getProcessor().getMax();
            double denMin = img.internalRatio.getProcessor().getMin();
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

                //alpha should be better...
                //testing hard coded min=800 max=8000;

                int min = 400;
                int max = 4000;
                double alpha = java.lang.Math.max(denpix[i], min);
                alpha = java.lang.Math.min(alpha, max);
                alpha = alpha / (max - min);
                plane_rgba[i][3] = (byte) (255 * alpha);
                 


                //double scaled = (denpix[i] - denMin)/denSpan;
                //System.out.println(denOut+ " ");
                //int outValue = (int) ((double) scaled* rgbGain);

                /*
                double a = (ratioval - props.getMinRatio()) / (props.getMaxRatio() - props.getMinRatio());
                a = 255 * a;
                int outValue = (int) java.lang.Math.round(a);

                //System.out.println("rgbgain: "+rgbGain);
                //System.out.println(outValue+ " ");
                if (outValue < 0) {
                    outValue = 0;
                } else if (outValue > 255) {
                    outValue = 255;
                }

                plane_rgba[i][3] = (byte) (outValue);
                 *
                 */
            }

            try {
                for (int i = 0; i < plane_rgba.length; i++) {
                    out.write(plane_rgba[i]);
                }
                out.flush();

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

        }
        try {
            out.close();
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
             */
            int x = denimg.getWidth();
            int y = denimg.getHeight();
            int z = denimg.getStackSize();
            String name = fileprefix + ".raw";

            bw.write("ObjectFileName: " + name + "\n");
            bw.write("TaggedFileName: ---" + "\n");
            bw.write("Resolution: " + x + " " + y + " " + z + "\n");
            bw.write("SliceThickness: 1 1 1" + "\n");
            bw.write("Format: UCHAR4\n");
            bw.write("NbrTags: 0\n");
            bw.write("ObjectType: TEXTURE_VOLUME_OBJECT\n");
            bw.write("ObjectModel: RGBA\n");
            bw.write("GridType: EQUIDISTANT\n");

            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        //ImagePlus iplus = new ImagePlus("test", new ColorProcessor(256, 256, foo));
        //iplus.show();
    }


}
