/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.experimental;

import com.nrims.MimsPlus;
import com.nrims.SumProps;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.ImageStack;
import java.util.ArrayList;
import java.lang.Math;

/**
 *
 * @author cpoczatek
 */
public class massCorrection {

    private com.nrims.UI ui;
    static final float dt = 44 * (float) Math.pow(10, -9);
    static final float csc = (float)((1/1.6)*Math.pow(10, 7));

    /**
     * Generic constructor
     * @param ui
     */
    public massCorrection(com.nrims.UI ui) {
        this.ui = ui;
    }

    /**
     * Performs deadtime correction on all mass images passed using dwelltime
     * read from file header.
     * WARNING: THIS CHANGES DATA.
     * @param massimgs
     * @param dwelltime
     */
    public void performDeadTimeCorr(com.nrims.MimsPlus[] massimgs, float dwelltime) {

        //Assure float images
        if (!(massCorrection.check32bit(massimgs))) {
            this.forceFloatImages(massimgs);
        }

        int nplanes = massimgs[0].getNSlices();
        int nmasses = massimgs.length;

        //loop over all masses
        for (int m = 0; m < nmasses; m++) {
            for (int p = 0; p < nplanes; p++) {
                massimgs[m].setSlice(p + 1);
                //this works, setting below does not
                //float[] pix = (float[])massimgs[m].getStack().getProcessor(p+1).getPixels();
                float[] pix = (float[]) massimgs[m].getProcessor().getPixels();
                float[] newpix = new float[pix.length];

                //compute new pix
                for (int i = 0; i < pix.length; i++) {
                    newpix[i] = dtCorrect(pix[i], dwelltime);
                }


                //set new pix
                //why doesn't this work?
                //massimgs[m].getStack().getProcessor(p+1).setPixels(newpix);
                massimgs[m].getProcessor().setPixels(newpix);
            }
        }


    }

    /**
     * Equation for dt corrected counts of a given pixel.
     * @param counts
     * @param dwelltime
     * @return
     */
    public static float dtCorrect(float counts, float dwelltime) {

        float corCounts = counts / (1 - (counts * dt) / (dwelltime));
        //return the corrected mass counts
        return corCounts;
    }

    /**
     * Perform QSA correction.  Forces float image conversion and dt correction
     * first.
     * @param massimgs
     * @param beta
     * @param dwelltime
     */
    public void performQSACorr(com.nrims.MimsPlus[] massimgs, float[] beta, float dwelltime, float FCObj) {
        //Assure float images
        if (!(massCorrection.check32bit(massimgs))) {
            this.forceFloatImages(massimgs);
        }
        //Do dt correction
        this.performDeadTimeCorr(massimgs, dwelltime);

        int nplanes = massimgs[0].getNSlices();
        int nmasses = massimgs.length;

        //loop over all masses
        for (int m = 0; m < nmasses; m++) {
            for (int p = 0; p < nplanes; p++) {
                massimgs[m].setSlice(p + 1);
                //this works, setting below does not
                //float[] pix = (float[])massimgs[m].getStack().getProcessor(p+1).getPixels();
                float[] pix = (float[]) massimgs[m].getProcessor().getPixels();
                float[] newpix = new float[pix.length];

                //compute new pix
                for (int i = 0; i < pix.length; i++) {
                    if((m==2) && (p==2) && (pix[i]>30)) {
                        int bar = 999;
                    }
                    newpix[i] = QSACorrect(pix[i], beta[m], dwelltime, FCObj);
                }


                //set new pix
                //why doesn't this work?
                //massimgs[m].getStack().getProcessor(p+1).setPixels(newpix);
                massimgs[m].getProcessor().setPixels(newpix);
            }
        }

    }

    /**
     * QSA correct individual dt corrected pixel
     * @param dtcounts
     * @param beta
     * @param dwelltime
     * @param FCObj
     * @return
     */
    public static float QSACorrect(float dtcounts, float beta, float dwelltime, float FCObj) {
        float qsacorr = dtcounts * (1 + beta * yieldCorr(dtcounts, dwelltime, FCObj));
        return qsacorr;
    }

    /**
     * Correct the ion yield based on primary beam current
     * @param dtcounts
     * @param dwelltime
     * @param FCObj
     * @return
     */
    public static float yieldCorr(float dtcounts, float dwelltime, float FCObj) {
        float yieldcorr = yieldExperimental(dtcounts, dwelltime, FCObj) / (1 - (float)(0.5 * yieldExperimental(dtcounts, dwelltime, FCObj)));
        return yieldcorr;
    }

    /**
     * Return experimental ion yield
     * @param dtcounts
     * @param dwelltime
     * @param FCObj
     * @return
     */
    public static float yieldExperimental(float dtcounts, float dwelltime, float FCObj) {
        float yieldexp = dtcounts / CsNumber(dwelltime, FCObj);
        return yieldexp;
    }

    /**
     * Return scaled Cs number based on primary beam current
     * @param dwelltime
     * @param FCobj
     * @return
     */
    public static float CsNumber(float dwelltime, float FCobj) {
        float csn =  (dwelltime * FCobj * csc);
        return csn;
    }

    /**
     * Checks if -all- mass images are float images.
     * @param massimgs
     * @return
     */
    public static boolean check32bit(com.nrims.MimsPlus[] massimgs) {
        boolean is32b = true;
        for (int i = 0; i < massimgs.length; i++) {
            if (!(massimgs[i].getType() == MimsPlus.GRAY32)) {
                is32b = false;
            }
        }
        return is32b;
    }

    /**
     * Forces the conversion of passed mass images to 32bit.
     * Needed to avoid loss of precision before doing corrections.
     * @param massimgs
     */
    public void forceFloatImages(com.nrims.MimsPlus[] massimgs) {
        int nplanes = massimgs[0].getNSlices();
        int nmasses = massimgs.length;
        MimsPlus[][] cp = new MimsPlus[nmasses][nplanes];
        int width = massimgs[0].getWidth();
        int height = massimgs[0].getHeight();

        // Set up the stacks.

        ImageStack[] is = new ImageStack[nmasses];
        for (int mindex = 0; mindex < nmasses; mindex++) {
            ImageStack iss = new ImageStack(width, height);
            is[mindex] = iss;
        }

        for (int idx = 1; idx <= nplanes; idx++) {

            ArrayList sumlist = new ArrayList<Integer>();
            sumlist.add(idx);
            // Generate the "sum" image for the plane
            for (int mindex = 0; mindex < nmasses; mindex++) {
                SumProps sumProps = new SumProps(ui.getMassImage(mindex).getMassIndex());
                cp[mindex][idx - 1] = new MimsPlus(ui, sumProps, sumlist);
            }
        }
        for (int i = 0; i < cp[0].length; i++) {
            for (int mindex = 0; mindex < nmasses; mindex++) {
                ImageProcessor ip = null;
                ip = new FloatProcessor(width, height);
                ip.setPixels(cp[mindex][i].getProcessor().getPixels());

                is[mindex].addSlice(null, ip);
            }
        }

        //set the stacks to new 32bit stacks
        for (int mindex = 0; mindex < nmasses; mindex++) {
            massimgs[mindex].setStack(null, is[mindex]);
        }
    }
}
