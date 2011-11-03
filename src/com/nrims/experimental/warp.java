/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims.experimental;

/**
 *
 * @author cpoczatek
 * Test class for z warping.  Needs much work...
 */
public class warp {

    /*
     * sort of working yeast shifting
     *


    ImagePlus seg = WindowManager.getImage("m26_med3_75-1200.tif");
    System.out.println("shift image: " + seg.getTitle());
    ij.process.ByteProcessor segproc = (ij.process.ByteProcessor)seg.getProcessor();



    int width = getMassImage(0).getProcessor().getWidth();
    int height = getMassImage(0).getProcessor().getHeight();
    int depth = getMassImage(0).getStackSize();
    //stack indexed starting at 1;
    getMassImage(0).setSlice(1);
    short[][] pixels = new short[width][height];
    int[][] shiftpix = new int[width][depth];

    int v = 0;
    //get pixel shift array from shift image
    for (int z = depth; z > 1; z--) {
        seg.setSlice(z);
        segproc = (ij.process.ByteProcessor)seg.getProcessor();
        if(z==500) {
            System.out.println("foo");
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                v = (int) segproc.getPixel(x, y);
                if( ( v!=0 ) && ( shiftpix[x][y]==0 ) ) {
                    //System.out.println("hit:"+x+"-"+y+"-"+z);
                    shiftpix[x][y] = z;
                }
            }
        }
    }

    ShortProcessor shiftproc = new ShortProcessor(width, height);
    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
        shiftproc.set(x, y, shiftpix[x][y]);
        }
    }
    ImagePlus shift = new ImagePlus("shift", shiftproc);
    shift.show();

    //if(true) return;

    //create new stack for shifted images
    //only 1 image this.getOpenMassImages().length
    ImageStack[] stack = new ImageStack[1];

    //for(int i = 0; i<stack.length; i++){ stack[i] = new ImageStack(width, height); }

    //fill stacks with empty planes to the correct length
    //for (int mindex = 0; mindex < stack.length; mindex++) {
      //  for (int sindex = 1; sindex <= depth; sindex++) {
        //    stack[mindex].addSlice("", new ShortProcessor(width, height));
        //}
    //}


    ShortProcessor proc;
    for (int mindex = 0; mindex < getOpenMassImages().length; mindex++) {

        for(int i = 0; i<stack.length; i++){ stack[i] = new ImageStack(width, height); }

    //fill stacks with empty planes to the correct length
    for (int i = 0; i < stack.length; i++) {
        for (int sindex = 1; sindex <= depth; sindex++) {
            stack[i].addSlice("", new ShortProcessor(width, height));
        }
    }

        for (int sindex = 1; sindex <= depth; sindex++) {

            //getMassImage(mindex).setSlice(sindex);
            proc = (ShortProcessor)getMassImage(mindex).getStack().getProcessor(sindex);

            //get mass image pixels
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    pixels[x][y] = (short) proc.getPixel(x, y);
                }
            }

            //decide where those pixels go...
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {

                    //skip 0 depth locations
                    if(shiftpix[x][y]==0) continue;

                    int zindex = sindex + (int)java.lang.Math.floor((depth - shiftpix[x][y])/2);

                    if( (zindex>0)  && (zindex<depth) ) {
                        stack[0].getProcessor(zindex).putPixel(x, y, pixels[x][y]);
                    }

                    if(x==150 && y==150) {
                        System.out.println("m: "+ mindex+ " x: "+ x + " y:" + y + " z: " + sindex + "->" + zindex + " v:" + proc.getPixel(x, y));
                    }

                }//y
            }//x

        }//slice

        //ImagePlus tempimg =  new ImagePlus("m-"+mindex, stack[mindex]);
        //ij


        //create images and show
        ImagePlus img = new ImagePlus("m-"+mindex, stack[0]);

        img.show();

        System.out.println("saving:   /tmp/"+img.getTitle()+".raw");
        new ij.io.FileSaver(img).saveAsRawStack("/tmp/"+img.getTitle()+".raw");
        img.close();
        img = null;
        stack[0]=null;


    }//mass

    end of yeast shift
    */










    //
    //
    //
    //
    //sort of working cell shifting
    /*

    //blah
    MimsPlus maskImage = getMassImage(0);
    ShortProcessor maskproc = (ShortProcessor)maskImage.getProcessor();
    ShortProcessor proc;
    int width = maskImage.getProcessor().getWidth();
    int height = maskImage.getProcessor().getHeight();
    int depth = maskImage.getStackSize();
    //stack indexed starting at 1;
    maskImage.setSlice(1);

    short[][] maskpixels = new short[width][height];
    short[][] pixels = new short[width][height];
    int[][] shift = new int[width][depth];
    short thresh = 125;

    ImageStack[] stack = new ImageStack[4];
    for(int i = 0; i<4; i++){ stack[i] = new ImageStack(width, height); }

    for (int mindex = 0; mindex < 4; mindex++) {

        proc = (ShortProcessor)getMassImage(mindex).getProcessor();
        for (int sindex = 1; sindex <= depth; sindex++) {

            maskImage.setSlice(sindex);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    maskpixels[x][y] = (short) maskproc.getPixel(x, y);
                }
            }

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    pixels[x][y] = (short) proc.getPixel(x, y);
                }
            }

            for (int x = 0; x < width; x++) {
                int s = 0;
                int y = height;
                while (y > 0) {
                    y--;
                    short val = maskpixels[x][y];
                    if (val > thresh) {
                        shift[x][sindex - 1] = height - y;
                        break;
                    }

                }
            }

            //for (int x = 0; x < shift.length; x++) {
            //  System.out.print(shift[x] + ",");
            //}


            //float[][] foo = new float[256][1];
            //for (int i = 0; i < 256; i++) {
            //foo[i][0] = (float) shift[i];
            //}
            //ImagePlus img = new ImagePlus("asdf", new FloatProcessor(foo));
            //img.show();


            ShortProcessor shiftproc = new ShortProcessor(width, height);
            for (int x = 0; x < width; x++) {
                for (int y = height - 1; y >= 0; y--) {
                    short pixval = 0;
                    if ((y - shift[x][sindex - 1] > 0) && (y - shift[x][sindex - 1] < height)) {
                        pixval = pixels[x][y - shift[x][sindex - 1]];
                    }

                    shiftproc.set(x, y, pixval);
                }
            }

            stack[mindex].addSlice("", shiftproc);
            System.out.println("added slice: " + sindex);
        }
    }

    ImagePlus[] imgs = new ImagePlus[4];
    for(int i=0; i<4; i++) {
        imgs[i] = new ImagePlus("m-"+i, stack[i]);
        imgs[i].show();
    }

    */
}
