package com.nrims;

import com.nrims.data.Opener;

import ij.IJ ;
import ij.ImagePlus;
import ij.gui.* ;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.filter.RankFilters;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Rectangle;
import java.awt.event.WindowEvent ;
import java.awt.event.WindowListener ;
import java.awt.event.MouseListener ;
import java.awt.event.MouseMotionListener ;
import java.awt.event.MouseEvent ;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.event.EventListenerList;

/**
 * Extends ImagePlus with methods to synchronize display of multiple stacks
 * and drawing ROIs in each windows
 */
public class MimsPlus extends ImagePlus implements WindowListener, MouseListener, MouseMotionListener, MouseWheelListener {

    /* Public constants */
    static final public int MASS_IMAGE = 0 ;
    static final public int RATIO_IMAGE = 1 ;
    static final public int HSI_IMAGE  =  2 ;
    static final public int SEG_IMAGE = 3 ;
    static final public int SUM_IMAGE = 4 ;
    static final public int COMPOSITE_IMAGE = 5 ;

    // Internal images for test data display.
    public MimsPlus internalRatio;
    public MimsPlus internalNumerator;
    public MimsPlus internalDenominator;

    // Window position.
    private int xloc = -1;
    private int yloc = -1;

    // Props objects.
    public SumProps sumProps = null;
    public RatioProps ratioProps = null;
    public HSIProps hsiProps = null;
    public CompositeProps compProps = null;

    // Lut
    public String lut = "Grays";

    // Other member variables.
    public String title = "";
    private boolean allowClose =true;
    private boolean bIgnoreClose = false ;
    boolean bMoving = false;
    boolean autoAdjustContrast = false;
    static boolean bStateChanging = false ;
    private int massIndex = 0 ;
    private int nType = 0 ;
    private int x1, x2, y1, y2, w1, w2, h1, h2;
    private boolean bIsStack = false ;
    private HSIProcessor hsiProcessor = null ;
    private CompositeProcessor compProcessor = null ;
    private com.nrims.UI ui = null;
    private EventListenerList fStateListeners = null ;

    /**
     * Generic constructor
     * @param ui user interface to be used in the MimsPlus object
     */
    public MimsPlus(UI ui) {
        super();
        this.ui = ui;
        fStateListeners = new EventListenerList() ;
    }

    /**
     * Constructor to use for mass images.
     * @param ui
     * @param index
     */
    public MimsPlus(UI ui, int index ) {
        super();
        this.ui = ui;
        this.massIndex = index ;
        this.nType = MASS_IMAGE;

        // Get a copy of the opener and setup image parameters.
        Opener op = ui.getOpener();
        int width = op.getWidth();
        int height = op.getHeight();        
        op.setStackIndex(0);

        // Set processor.
        ij.process.ImageProcessor ip = null;
        if (ui.getOpener().getFileType() == FileInfo.GRAY16_UNSIGNED) {
           short[] pixels = new short[width * height];
           ip = new ij.process.ShortProcessor(width, height, pixels, null);
        } else if (ui.getOpener().getFileType() == FileInfo.GRAY32_FLOAT) {
           float[] pixels = new float[width * height];
           ip = new ij.process.FloatProcessor(width, height, pixels, null);
        }

        //Don't do this, massNames should allready have the correctly formated string
        //Double massNumber = new Double(op.getMassNames()[index]);
        String title = "m" + op.getMassNames()[index] + " : " + ui.getImageFilePrefix();
        setProcessor(title, ip);
        fStateListeners = new EventListenerList() ;
    }

    /**
     * Constructor to use for segmented images.
     * @param ui
     * @param width
     * @param height
     * @param pixels
     * @param name
     */
    public MimsPlus(UI ui,int width, int height, int[] pixels, String name) {
        super();
        this.ui=ui;
        this.nType = SEG_IMAGE ;

        try {
            ij.process.ImageProcessor ipp = new ij.process.ColorProcessor(
                width,
                height,
                pixels);

            setProcessor(name, ipp);
            fStateListeners = new EventListenerList() ;
        } catch (Exception x) { IJ.log(x.toString());}
    }

    /**
     * Constructor for sum images.
     * @param ui
     * @param sumProps
     * @param sumlist
     */
   public MimsPlus(UI ui, SumProps sumProps, ArrayList<Integer> sumlist) {
      super();
      this.ui = ui;
      this.sumProps = sumProps;
      this.nType = SUM_IMAGE;
      this.xloc = sumProps.getXWindowLocation();
      this.yloc = sumProps.getYWindowLocation();

      // Setup image.
      Opener op = ui.getOpener();
      int width = op.getWidth();
      int height = op.getHeight();
      double sumPixels[] = new double[width*height];
      ImageProcessor ipp = new FloatProcessor(width, height, sumPixels);            
      title = "Sum : ";
      if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
         if (sumProps.getParentMassIdx() == ui.getOpenMassImages().length)
            title += "1";
         else 
            title += "m" + op.getMassNames()[sumProps.getParentMassIdx()] + " : " + op.getImageFile().getName();
      } else if (sumProps.getSumType() == SumProps.RATIO_IMAGE) {
         String numString, denString;
         if (sumProps.getNumMassIdx() == ui.getOpenMassImages().length)
            numString = "1";
         else
            numString = "m" + op.getMassNames()[sumProps.getNumMassIdx()];
         if (sumProps.getDenMassIdx() == ui.getOpenMassImages().length)
            denString = "1";
         else
            denString = "m" + op.getMassNames()[sumProps.getDenMassIdx()];
         title += numString + "/" + denString + " : " + op.getImageFile().getName();
      }
      setProcessor(title, ipp);
      fStateListeners = new EventListenerList();
      //before listeners were added in compute...
      addListener(ui);

      // Setup sumlist.
      if (sumlist == null) {
         sumlist = new ArrayList<Integer>();
         for(int i = 1; i <= ui.getmimsAction().getSize(); i++)
            sumlist.add(i);
      }

      // Compute pixels values.
      computeSum(sumlist);
   }

   /**
    * Constructor for ratio images.
    * @param ui
    * @param props
    */
   public MimsPlus(UI ui, RatioProps props) {
      this(ui, props, false);
   }

   /**
    * Constructor for ratio images.
    * @param ui
    * @param props
    * @param forHSI
    */
   public MimsPlus(UI ui, RatioProps props, boolean forHSI) {
      super();
      this.ui = ui;
      this.ratioProps = props;
      this.nType = RATIO_IMAGE;
      this.xloc = props.getXWindowLocation();
      this.yloc = props.getYWindowLocation();

      // Setup image.
      Opener op = ui.getOpener();
      int width = op.getWidth();
      int height = op.getHeight();
      float[] pixels = new float[width * height];
      ImageProcessor ip = new FloatProcessor(width, height, pixels, null);
      ip.setMinAndMax(0, 1.0);
      String numName, denName;
      if (props.getNumMassIdx() == ui.getOpenMassImages().length)
         numName = "1";
      else
         numName = "m" + ui.getOpener().getMassNames()[props.getNumMassIdx()];
      if (props.getDenMassIdx() == ui.getOpenMassImages().length)
         denName = "1";
      else
         denName = "m" + ui.getOpener().getMassNames()[props.getDenMassIdx()];
      title = numName + "/" + denName + " : " + ui.getImageFilePrefix();
      setProcessor(title, ip);
      fStateListeners = new EventListenerList();
      //before listeners were added in compute...
      addListener(ui);

      // Compute pixel values.
      computeRatio(forHSI);
    }



   /**
    * Constructor for composite images.
    * @param ui
    * @param compprops
    */
   public MimsPlus(UI ui, CompositeProps compprops) {
       super();
       this.ui = ui;
       this.compProps = compprops;
       this.nType = MimsPlus.COMPOSITE_IMAGE;
       fStateListeners = new EventListenerList() ;
       setupCompositeImage(compprops);
   }

    /**
     * Constructor for HSI images.
     * @param ui
     * @param props
     */
    public MimsPlus(UI ui, HSIProps props) {
      super();
      this.ui = ui;
      this.hsiProps = props;
      this.nType = HSI_IMAGE;
      this.xloc = props.getXWindowLocation();
      this.yloc = props.getYWindowLocation();

      setupHSIImage(props);
    }

    /**
    * Initialization for composite image graphics.
    * @param compprops
    */
   public void setupCompositeImage(CompositeProps compprops) {
       compProps = compprops;
       MimsPlus[] imgs = compprops.getImages();

       Opener op = ui.getOpener();
       int width = op.getWidth();
       int height = op.getHeight();
       int[] rgbPixels = new int[width * height];
       ImageProcessor ip = new ColorProcessor(width, height, rgbPixels);
       title = ui.getImageFilePrefix();
       for (int i = 0; i < imgs.length; i++) {
           if (imgs[i] != null) {
               title += "_" + imgs[i].getRoundedTitle().replace(" ", "-");
           } else {
               title += "_n";
           }
       }
       title += "_comp";
       setProcessor(title, ip);

       //fill in pixels
       computeComposite();
   }

    /**
    * Composite image readiness status
    * @return status (true for success, false for failure)
    */
    public synchronized boolean computeComposite()
    {
        setCompositeProcessor(new CompositeProcessor(this));
        try {
            getCompositeProcessor().setProps(compProps);
        } catch (Exception e) {
            ui.updateStatus("Failed computing Composite image");
        }
        return true;
    }

   /**
    * Sets up HSI image
    * @param props HSI properties to use
    */
    public void setupHSIImage(HSIProps props) {

      // Set props incase changes
      hsiProps = props;

      // Setup image.
      Opener op = ui.getOpener();
      int width = op.getWidth();
      int height = op.getHeight();
      if(props.getLabelMethod() > 0)
         height += 16;
      int [] rgbPixels = new int[width*height];
      ImageProcessor ip = new ColorProcessor(width, height, rgbPixels);
      String numName = ui.getOpener().getMassNames()[props.getNumMassIdx()];
      String denName = ui.getOpener().getMassNames()[props.getDenMassIdx()];
      title = "HSI : m" + numName +"/m"+ denName + " : " + ui.getImageFilePrefix();
      setProcessor(title, ip);
      getProcessor().setMinAndMax(0, 255);
      fStateListeners = new EventListenerList();
      addListener(ui);

      // Fill in pixel values.
      computeHSI();
    }

    /**
     * Compute HSI image; return true for success
     * @return true for success, false for failure
     */
    public synchronized boolean computeHSI()
    {

      // Set up internal images for data display.
        RatioProps rProps = new RatioProps(hsiProps.getNumMassIdx(), hsiProps.getDenMassIdx());
        rProps.setRatioScaleFactor(hsiProps.getRatioScaleFactor());
        rProps.setNumThreshold(hsiProps.getNumThreshold());
        rProps.setDenThreshold(hsiProps.getDenThreshold());
        internalRatio = new MimsPlus(ui, rProps, true);
        internalNumerator = internalRatio.internalNumerator;
        internalDenominator = internalRatio.internalDenominator;
        setHSIProcessor(new HSIProcessor(this));
        try {
         getHSIProcessor().setProps(hsiProps);
      } catch (Exception e) {
         ui.updateStatus("Failed computing HSI image");
      }
      return true;
   }

   /**
    * Computes ratios values.
    */
    public synchronized void computeRatio() {
      computeRatio(false);
    }

   /**
    * Computes ratios values.
    * @param forHSI
    */
    private synchronized void computeRatio(boolean forHSI) {

       // Get numerator and denominator mass indexes.
       int numIndex = ratioProps.getNumMassIdx();
       int denIndex = ratioProps.getDenMassIdx();

       if (numIndex > ui.getOpener().getNMasses() - 1 || denIndex > ui.getOpener().getNMasses())
          return;

        // Get the numerator and denominator mass images.
        MimsPlus parentNum = ui.getMassImage( numIndex );
        MimsPlus parentDen = ui.getMassImage( denIndex );

        // Setup list for sliding window, entire image, or single plane.
        java.util.ArrayList<Integer> list = new java.util.ArrayList<Integer>();
        int currentplane = parentNum.getCurrentSlice();
        if (ui.getIsSum()) {
           for (int i = 1; i <= parentNum.getNSlices(); i++) {
              list.add(i);
           }
        } else if (ui.getIsWindow()) {
           int windowSize = ui.getWindowRange();
           int lb = currentplane - windowSize;
           int ub = currentplane + windowSize;
           for (int i = lb; i <= ub; i++) {
              list.add(i);
           }
        } else {
           list.add(currentplane);
        }

        // Compute the sum of the numerator and denominator mass images.
        SumProps numProps = new SumProps(numIndex);
        SumProps denProps = new SumProps(denIndex);
        internalNumerator = new MimsPlus(ui, numProps, list);
        internalDenominator = new MimsPlus(ui, denProps, list);

        // Fill in the data.
        float[] nPixels = (float[]) internalNumerator.getProcessor().getPixels();
        float[] dPixels = (float[]) internalDenominator.getProcessor().getPixels();
        float[] rPixels = new float[getWidth() * getHeight()];
        float rMax = 0.0f;
        float rMin = 1000000.0f;
        float rSF = ui.getHSIView().getRatioScaleFactor();
        int numThreshold = ratioProps.getNumThreshold();
        int denThreshold = ratioProps.getDenThreshold();

        if( this.ratioProps.getRatioScaleFactor()>0 ) {
            rSF = ((Double)this.ratioProps.getRatioScaleFactor()).floatValue();
        }

        // If we are dealing with a ratio image whose numerator or denominator
        // is "1", we DO NOT need to multiply the ratio by the ratioscalefactor.
        if (denIndex == ui.getOpenMassImages().length)
            rSF = (float) 1.0;

        for (int i = 0; i < rPixels.length; i++) {
            if( dPixels[i] != 0 && nPixels[i] > numThreshold && dPixels[i] > denThreshold) {
                rPixels[i] = rSF * ((float) nPixels[i] / (float) dPixels[i]);
            } else {
                rPixels[i]=0;
            }
          if (rPixels[i] > rMax) {
             rMax = rPixels[i];
          } else if (rPixels[i] < rMin) {
             rMin = rPixels[i];
       }
       }

       if (ui.getIsPercentTurnover() && forHSI) {
          float reference = ui.getPreferences().getReferenceRatio();
          float background = ui.getPreferences().getBackgroundRatio();
          rPixels = HSIProcessor.turnoverTransform(rPixels, reference, background, (float)(ratioProps.getRatioScaleFactor()));
       }

       // Set processor.
       ImageProcessor ip = new FloatProcessor(getWidth(), getHeight(), rPixels, getProcessor().getColorModel());
       ip.setMinAndMax(getProcessor().getMin(), getProcessor().getMax());
       setProcessor(title, ip);
       internalRatio = this;

       // Do median filter if set to true.
       if (ui.getMedianFilterRatios()) {
          Roi temproi = getRoi();
          killRoi();
          RankFilters rfilter = new RankFilters();
          double r = ui.getHSIView().getMedianRadius();
          rfilter.rank(getProcessor(), r, RankFilters.MEDIAN);
          rfilter = null;
          setRoi(temproi);
       }
    }

   /**
    * Computes sum values.
    * @param sumlist
    */
    private synchronized void computeSum(ArrayList<Integer> sumlist) {

       // initialize variables.
       double[] sumPixels = null;
       int parentIdx, numIdx, denIdx;
       MimsPlus parentImage, numImage, denImage;

       // Sum-mass image.
       if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
          parentIdx = sumProps.getParentMassIdx();
          parentImage = ui.getMassImage(parentIdx);
          if (parentIdx == ui.getOpenMassImages().length) {
             int width = getWidth();
             int height = getHeight();
             int area = width * height;
             sumPixels = new double[area];
             for (int i = 0; i < area; i++)
                sumPixels[i] = 1;
          } else {
             int templength = parentImage.getProcessor().getPixelCount();
             sumPixels = new double[templength];

             Object[] o = parentImage.getStack().getImageArray();
             int numSlices = parentImage.getNSlices();
             if (o[0] instanceof float[]) {
               float[] tempPixels = new float[templength];
               for (int i = 0; i < sumlist.size(); i++) {
                   if (sumlist.get(i) < 1 || sumlist.get(i) > numSlices) continue;
                   tempPixels = (float[])o[sumlist.get(i)-1];
                   for (int j = 0; j < sumPixels.length; j++) {
                       sumPixels[j] += ((int) ( tempPixels[j] ) );
                   }
               }
             } else if (o[0] instanceof short[]) {
                short[] tempPixels = new short[templength];
                for (int i = 0; i < sumlist.size(); i++) {
                   if (sumlist.get(i) < 1 || sumlist.get(i) > numSlices) continue;
                   tempPixels = (short[])o[sumlist.get(i)-1];
                   for (int j = 0; j < sumPixels.length; j++) {
                       sumPixels[j] += ((int) ( tempPixels[j] & 0xffff) );
                   }
               }
             }
          }
       }
       // Sum-ratio image
       else if (sumProps.getSumType() == SumProps.RATIO_IMAGE) {
          numIdx = sumProps.getNumMassIdx();
          denIdx = sumProps.getDenMassIdx();

          SumProps nProps = new SumProps(numIdx);
          SumProps dProps = new SumProps(denIdx);

          UI tempui = ui;
          tempui.setMedianFilterRatios(false);

          numImage = new MimsPlus(tempui, nProps, sumlist);
          denImage = new MimsPlus(tempui, dProps, sumlist);
          internalNumerator = numImage;
          internalDenominator = denImage;

          float[] numPixels = (float[]) numImage.getProcessor().getPixels();
          float[] denPixels = (float[]) denImage.getProcessor().getPixels();
          sumPixels = new double[numImage.getProcessor().getPixelCount()];


          float rSF = ui.getHSIView().getRatioScaleFactor();
          if( this.sumProps.getRatioScaleFactor()>0 )
            rSF = ((Double)this.sumProps.getRatioScaleFactor()).floatValue();
          if (denIdx == ui.getOpenMassImages().length)
            rSF = (float) 1.0;
          for (int i = 0; i < sumPixels.length; i++) {
             if (denPixels[i] != 0) {
                 sumPixels[i] = rSF * (numPixels[i] / denPixels[i]);
             } else {
                 sumPixels[i] = 0;
             }
          }
       }

       // Set processor.
       ImageProcessor ip = new FloatProcessor(getWidth(), getHeight(), sumPixels);
       setProcessor(title, ip);

    }

    /**
     * Shows the current window.
     */
    public void showWindow() {
        show();

        // Set window location.
        if (xloc > -1 & yloc > -1) {
            getWindow().setLocation(xloc, yloc);
        }

        // Add image to list og images in UI.
        ui.addToImagesList(this);

        // Autocontrast image by default.
        if ((this.getMimsType() == MimsPlus.MASS_IMAGE) || (this.getMimsType() == MimsPlus.RATIO_IMAGE) || (this.getMimsType() == MimsPlus.SUM_IMAGE)) {
            ui.autoContrastImage(this);
        }

        this.restoreMag();
    }

    /**
     * Restores previous magnification.
     */
    public void restoreMag() {
        if(this.getCanvas()==null) return;
        double mag = 1.0;

        if (this.getMimsType() == MimsPlus.HSI_IMAGE) {
            mag = this.getHSIProps().getMag();
        }
        if (this.getMimsType() == MimsPlus.RATIO_IMAGE) {
            mag = this.getRatioProps().getMag();
        }
        if (this.getMimsType() == MimsPlus.SUM_IMAGE) {
            mag = this.getSumProps().getMag();
        }

        double z = this.getCanvas().getMagnification();

        if (this.getCanvas().getMagnification() < mag) {
            while (this.getCanvas().getMagnification() < mag) {
                this.getCanvas().zoomIn(0, 0);
            }
        }

        if (this.getCanvas().getMagnification() > mag) {
            while (this.getCanvas().getMagnification() > mag) {
                this.getCanvas().zoomOut(0, 0);
            }
        }
        
    }

    /**
     * Returns the width of the image in pixels.
     * @return the width in pixels.
     */
    @Override
    public int getWidth() {
      if (getProcessor() != null)
         return getProcessor().getWidth();
      else
         return ui.getOpener().getWidth();
    }

   /**
    * Returns the height of the image in pixels.
    * @return the height in pixels.
    */
   @Override
    public int getHeight() {
      if (getProcessor() != null)
         return getProcessor().getHeight();
      else
         return ui.getOpener().getHeight();
    }

    /**
     * Appends image to stack. Not hit from MimsStackEditing.concatImages().
     * Only used when opening a multiplane image file.
     * @param nImage
     * @throws Exception
     */
    public void appendImage(int nImage) throws Exception {
        if (ui.getOpener() == null) {
            throw new Exception("No image opened?");
        }
        if (nImage >= ui.getOpener().getNImages()) {
            throw new Exception("Out of Range");
        }
        ij.ImageStack stack = getStack();
        ui.getOpener().setStackIndex(nImage);
        stack.addSlice(null, ui.getOpener().getPixels(massIndex));
        setStack(null, stack);
        setSlice(nImage + 1);
        //setProperty("Info", srcImage.getInfo());
        bIgnoreClose = true;
        bIsStack = true;
    }

    /**
     * Returns a short title in the form "m13.01" or "m26.05".
     * @return The title (e.g. "m13.01").
     */
    @Override
    public String getShortTitle() {
        String tempstring = this.getTitle();
        int colonindex = tempstring.indexOf(":");
        if (getMimsType() == SUM_IMAGE)
           colonindex = tempstring.indexOf(":", colonindex+1);
        if(colonindex>0)
           return tempstring.substring(0, colonindex-1);
        else
           return "";
    }



    /**
     * Similar to {@link #getShortTitle() getShortTitle} but uses the rounded mass and excludes the "m".
     *
     * @return Text string containing the rounded mass value. For example:
     * <ul>
     * <li>"13" for Mass images.
     * <li>"13/12" for Ratio images.
     * <li>"sum 13" for Sum images.
     * <li>"0" by default.
     * </ul>
     */
    public String getRoundedTitle() {
        if (this.getMimsType() == MimsPlus.MASS_IMAGE) {
            String tempstring = this.getTitle();
            int colonindex = tempstring.indexOf(":");
            tempstring = tempstring.substring(1, colonindex - 1);
            int massint = java.lang.Math.round(Float.parseFloat(tempstring));
            return Integer.toString(massint);
        }
        if (this.getMimsType() == MimsPlus.RATIO_IMAGE) {
            String tempstring = this.getTitle();
            int colonindex = tempstring.indexOf(":");
            int slashindex = tempstring.indexOf("/");
            String neumstring = tempstring.substring(1, slashindex);
            String denstring = tempstring.substring(slashindex + 2, colonindex - 1);
            if (neumstring == null || neumstring.trim().length() == 0)
               neumstring = "1";
            if (denstring == null || denstring.trim().length() == 0)
               denstring = "1";
            int nint = java.lang.Math.round(Float.parseFloat(neumstring));
            int dint = java.lang.Math.round(Float.parseFloat(denstring));
            return Integer.toString(nint) + "/" + Integer.toString(dint);
        }
        if (this.getMimsType() == MimsPlus.SUM_IMAGE) {
            SumProps props = this.getSumProps();
            if (props.getSumType() == SumProps.MASS_IMAGE) {
                int ind = props.getParentMassIdx();
                String title;
                if (ind == ui.getOpenMassImages().length)
                   title = "1";
                else
                   title = "sum " + ui.getMassImage(ind).getRoundedTitle();
                return title;
            } else if (props.getSumType() == SumProps.RATIO_IMAGE) {
                int den = props.getDenMassIdx();
                int num = props.getNumMassIdx();
                String numString, denString = "";
                if (den == ui.getOpenMassImages().length)
                   denString = "1";
                else
                   denString = ui.getMassImage(den).getRoundedTitle();
                if (num == ui.getOpenMassImages().length)
                   numString = "1";
                else
                   numString = ui.getMassImage(num).getRoundedTitle();
                return "sum " + numString + "/" + denString;
            }
        }
        return "0";
    }

    /**
     * Returns numerator image if such exists.
     * @return numerator image
     */
    public MimsPlus getNumeratorImage() {
        if (this.getMimsType() == this.RATIO_IMAGE) {
            return this.ui.getMassImage(this.getRatioProps().getNumMassIdx());
        } else if (this.getMimsType() == this.HSI_IMAGE) {
            return this.ui.getMassImage(this.getHSIProps().getNumMassIdx());
        } else {
            return null;
        }
    }

    /**
     * Returns denominator image if such exists.
     * @return denominator image
     */
    public MimsPlus getDenominatorImage() {
        if (this.getMimsType() == this.RATIO_IMAGE) {
            return this.ui.getMassImage(this.getRatioProps().getDenMassIdx());
        } else if (this.getMimsType() == this.HSI_IMAGE) {
            return this.ui.getMassImage(this.getHSIProps().getDenMassIdx());
        } else {
            return null;
        }
    }

    /**
     * Sets the "ignore close" flag to the value of b
     * @param b "ignore close" value
     */
    public void setbIgnoreClose(boolean b) {
        this.bIgnoreClose = b;
    }

    /**
     * Gets current location
     * @return the Point object containing the current location
     */
    public java.awt.Point getXYLoc() {
        return new java.awt.Point(this.xloc, this.yloc);
    }

    /**
     * Shows the window and add various mouse mouse listeners.
     */
    @Override
    public void show() {
        //this is a little weird...
        super.show() ;
        ij.gui.ImageWindow win = getWindow();
        if(win != null || getWindow() != null) {
            if(!(getWindow().getCanvas() instanceof MimsCanvas )) {
                if (getStackSize() > 1) {
                    new StackWindow(this, new MimsCanvas(this, ui));
                } else {
                    new ImageWindow(this, new MimsCanvas(this, ui));
                }
            }
            getWindow().addWindowListener(this);
            getWindow().getCanvas().addMouseListener(this);
            getWindow().getCanvas().addMouseMotionListener(this);
            getWindow().getCanvas().addMouseWheelListener(this);
        }
    }

    /**
     * Compares the input MimsPlus object (mp) to the current class.
     * @param mp
     * @return true if a match, false otherwise
     */
    public boolean equals(MimsPlus mp) {

        if (mp.getMimsType() != getMimsType()) {
            return false;
        }

        if (mp.getMimsType() == MASS_IMAGE) {
            if (mp.getMassIndex() == getMassIndex()) {
                return true;
            }
        } else if (mp.getMimsType() == RATIO_IMAGE) {
            if (mp.getRatioProps().equals(getRatioProps())) {
                return true;
            }
        } else if (mp.getMimsType() == SUM_IMAGE) {
            if (mp.getSumProps().equals(getSumProps())) {
                return true;
            }
        } else if (mp.getMimsType() == HSI_IMAGE) {
            if (mp.getHSIProps().equals(getHSIProps())) {
                return true;
            }
        } else if (mp.getMimsType() == COMPOSITE_IMAGE) {
            if (mp.getCompositeProps().equals(getCompositeProps())) {
                return true;
            }
        } else if (mp.getMimsType() == SEG_IMAGE) {
            // TODO: Not sure what to add here
        }

        return false;
    }

    /**
     * This method sets the image to actually be the image
     * enclosed by the Roi.
     *
     * @param roi
     */
    @Override
    public void setRoi(ij.gui.Roi roi) {
        if(roi == null)  {
           super.killRoi();
        } else {
           super.setRoi(roi);
        }
        stateChanged(roi,MimsPlusEvent.ATTR_SET_ROI);
    }

    @Override
    public void windowClosing(WindowEvent e) {
        java.awt.Point p = this.getWindow().getLocation();
        this.xloc = p.x;
        this.yloc = p.y;
    }

    @Override
    public void windowClosed(WindowEvent e) {
        // When opening a stack, we get a close event
        // from the original ImageWindow, and ignore this event
        if(bIgnoreClose) {
            bIgnoreClose = false ;
            return ;
        }
        //ui.imageClosed(this);
        //ui.getCBControl().removeWindowfromList(this);
    }

    public void windowStateChanged(WindowEvent e) {}

    @Override
    public void windowDeactivated(WindowEvent e) {}

    @Override
    public void windowActivated(WindowEvent e) {
        ui.setActiveMimsPlus(this);
        ui.getCBControl().setWindowlistCombobox(getTitle());
        ui.getCBControl().setLUT(lut);

        MimsRoiManager rm = ui.getRoiManager();
        if(rm==null) return;

        MimsRoiManager.ParticlesManager pm = rm.getParticlesManager();
        MimsRoiManager.SquaresManager sm = rm.getSquaresManager();

        if(pm!=null) pm.resetImage(this);
        if(sm!=null) sm.resetImage(this);
    }

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowOpened(WindowEvent e) {
        java.awt.Window w = getWindow();
        if(w==null) return;
        WindowListener [] wl = w.getWindowListeners();
        boolean bFound = false ;
        int i ;
        for(i = 0 ; i < wl.length ; i++ ) {
            if(wl[i] == this) {
                bFound = true;
            }
        }
        if(!bFound) {
            getWindow().addWindowListener(this);
        }
        bFound = false ;
        MouseListener [] ml = getWindow().getCanvas().getMouseListeners();
        for(i=0;i<ml.length;i++) {
            if(ml[i] == this) {
                bFound = true;
            }
        }
        if(!bFound) {
            getWindow().getCanvas().addMouseListener(this);
            getWindow().getCanvas().addMouseMotionListener(this);
        }
    }

    @Override
    public void mouseExited(MouseEvent e){ ui.updateStatus(" "); }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {

      if(getRoi() != null ) {

         // Set the moving flag so we know if user is attempting to move a roi.
         // Line Rois have to be treated differently because their state is never MOVING .
         int roiState = getRoi().getState();
         int roiType = getRoi().getType();

         if (roiState == Roi.MOVING) bMoving = true;
         else if (roiType == Roi.LINE && roiState == Roi.MOVING_HANDLE) bMoving = true;
         else bMoving = false;

         // Highlight the roi in the jlist that the user is selecting
          if (roi.getName() != null) {
              int i = ui.getRoiManager().getIndex(roi.getName());
              if (!(ij.IJ.controlKeyDown())) {
                  ui.getRoiManager().select(i);
              }
              if (ij.IJ.controlKeyDown()) {
                  bStateChanging = true;
                  ui.getRoiManager().selectAdd(i);
              }
          }

         // Get the location so that if the user simply declicks without
         // moving, a duplicate roi is not created at the same location.
         Rectangle r = getRoi().getBounds();
         x1 = r.x; y1 = r.y; w1 = r.width; h1 = r.height;

      } else if(getRoi() != null && ( ij.IJ.shiftKeyDown() && ij.IJ.controlKeyDown() ) ) {
          this.killRoi();
          bMoving = false;

      }
      bStateChanging = false;
      
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        if(bStateChanging) return;

         float[] pix;
         if (this.nType == HSI_IMAGE ) {
            internalRatio.setRoi(getRoi());
            pix = (float[])internalRatio.getProcessor().getPixels();
            internalRatio.killRoi();
         } else if(this.nType == RATIO_IMAGE || this.nType == SUM_IMAGE) {
             pix = (float[])getProcessor().getPixels();
         } else if( (this.nType == SEG_IMAGE) || (this.nType == COMPOSITE_IMAGE)) {
             return;
         } else {
            if (getProcessor() instanceof ShortProcessor) {
               short[] spix = (short[])getProcessor().getPixels();
               pix = new float[spix.length];
               for(int i = 0; i < spix.length; i++)
                  pix[i] = (new Short(spix[i])).floatValue();
            } else {
               pix = (float[])getProcessor().getPixels();
            }
         }
         if (pix != null) {
            double[] dpix = new double[pix.length];
            for (int i = 0; i < pix.length; i++) {
                dpix[i] = (new Float(pix[i])).doubleValue();
            }
            ui.getmimsTomography().updateHistogram(dpix, getShortTitle(), true);

            //TODO: this should be somewhere else
             if (this.nType == HSI_IMAGE) {
                 String stats = "";
                 stats += this.getShortTitle() + ": ";
                 stats += "mean = " + IJ.d2s(this.internalRatio.getStatistics().mean, 2) + " ";
                 stats += "sd = " + IJ.d2s(this.internalRatio.getStatistics().stdDev, 2);
                 ui.updateStatus(stats);
             } else {
                String stats = "";
                 stats += this.getShortTitle() + ": ";
                 stats += "mean = " + IJ.d2s(this.getStatistics().mean, 2) + " ";
                 stats += "sd = " + IJ.d2s(this.getStatistics().stdDev, 2);
                 ui.updateStatus(stats);
             }
         }

         //check magnification and update
         //ignoring tool type to start
         if (this.getCanvas() != null) {
             double mag = this.getCanvas().getMagnification();
            if (this.getMimsType() == MimsPlus.HSI_IMAGE) {
                this.getHSIProps().setMag(mag);
            }
            if (this.getMimsType() == MimsPlus.RATIO_IMAGE) {
                this.getRatioProps().setMag(mag);
            }
            if (this.getMimsType() == MimsPlus.SUM_IMAGE) {
                this.getSumProps().setMag(mag);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (bStateChanging) {
         return;
      }
      if (bMoving) {
        Roi thisroi = getRoi();
        if(thisroi==null) return;
        // Prevent duplicate roi at same location
        Rectangle r = thisroi.getBounds();
        x2 = r.x; y2 = r.y; w2 = r.width; h2 = r.height;
        if (x1 == x2 && y1 == y2 && w1 == w2 && h1 == h2) return;

        stateChanged(getRoi(), MimsPlusEvent.ATTR_ROI_MOVED);

        ui.getRoiManager().resetSpinners(thisroi);
        updateHistogram(true);
        bMoving = false;
        return;
      }

      switch (Toolbar.getToolId()) {
         case Toolbar.RECTANGLE:
         case Toolbar.OVAL:
         case Toolbar.LINE:
         case Toolbar.FREELINE:
         case Toolbar.FREEROI:
         case Toolbar.POINT:
         case Toolbar.POLYGON:
         case Toolbar.POLYLINE:
            stateChanged(getRoi(), MimsPlusEvent.ATTR_MOUSE_RELEASE);
            break;
         case Toolbar.WAND:
            if (getRoi() != null) {
               stateChanged(getRoi(), MimsPlusEvent.ATTR_MOUSE_RELEASE);
            }
            break;
      }
   }

    /**
     * Handles a mouse move event. A fairly in depth method
     * that displays data in the status bar of the application.
     * The data that is displayed is dependant on the type of image
     * (Mass, HSI, Sum, etc.) This methos also controls various
     * aspects of ROI behavior regarding mouse events.
     *
     * @param e mouse move event
     */
    public void mouseMoved(MouseEvent e) {

        if(this.ui.isOpening()) {
            return;
        }

        // Get the X and Y position of the mouse.
        int x = (int) e.getPoint().getX();
        int y = (int) e.getPoint().getY();
        int mX = getWindow().getCanvas().offScreenX(x);
        int mY = getWindow().getCanvas().offScreenY(y);
        String msg = "" + mX + "," + mY ;

        // Get the slice number.
        int cslice = getCurrentSlice();
        boolean stacktest = isStack();
        if (this.nType == RATIO_IMAGE || this.nType == HSI_IMAGE) {
            stacktest = stacktest || this.getNumeratorImage().isStack();
            if(stacktest) cslice = this.getNumeratorImage().getCurrentSlice();
        }
        if (stacktest)
            msg += "," + cslice + " = ";
        else
            msg += " = ";

        // Get pixel data for the mouse location.
        if((this.nType == RATIO_IMAGE || this.nType == HSI_IMAGE) && (this.internalDenominator!=null && this.internalNumerator!=null) ) {
            float ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
            float dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
            double ratio = internalRatio.getProcessor().getPixelValue(mX, mY);
            String opstring = "";
            if(ui.getMedianFilterRatios()) { opstring = "-med->"; } else { opstring = "=";}
            msg += "S (" + (int)ngl + " / " + (int)dgl + ") " + opstring + " " + IJ.d2s(ratio, 4);
        } else if(this.nType == SUM_IMAGE) {
            float ngl, dgl;
            if (internalNumerator != null && internalDenominator != null) {
               ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
               dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
               msg += " S (" + ngl + " / " + dgl + ") = ";
            }
            int[] gl = getPixel(mX, mY);
            float s = Float.intBitsToFloat(gl[0]);
            msg += IJ.d2s(s,0);
        } else {
            MimsPlus[] ml = ui.getOpenMassImages() ;
            for(int i = 0 ; i < ml.length ; i++ ) {
                //int [] gl = ml[i].getPixel(mX,mY);
                //float s = Float.intBitsToFloat(gl[0]);
                msg += ml[i].getValueAsString(mX, mY);
                //msg += IJ.d2s(s,0);
                if( i+1 < ml.length )
                    msg += ", ";
            }
        }

        // Loop over all Rois, determine which one to highlight.
        int displayDigits = 2;
        java.util.Hashtable rois = ui.getRoiManager().getROIs();

        Roi smallestRoi = null;
        double smallestRoiArea = 0.0;
        ij.process.ImageStatistics stats = null;
        ij.process.ImageStatistics smallestRoiStats = null;
        ij.process.ImageStatistics numeratorStats = null;
        ij.process.ImageStatistics denominatorStats = null;

        for(Object key:rois.keySet()) {
            Roi loopRoi = (Roi)rois.get(key);

            if (!((DefaultListModel)ui.getRoiManager().getList().getModel()).contains(loopRoi.getName()))
               continue;            

            boolean linecheck = false;            
            int c = -1;
            if( (loopRoi.getType() == Roi.LINE) || (loopRoi.getType() == Roi.POLYLINE) || (loopRoi.getType() == Roi.FREELINE) ) {
                c = loopRoi.isHandle(x, y);
                if(c != -1) linecheck=true;
            }

            if(loopRoi.contains(mX, mY) || linecheck) {

                  if (this.getMimsType()==HSI_IMAGE && internalRatio!=null) {
                      internalRatio.setRoi(loopRoi);
                      stats = internalRatio.getStatistics();
                      internalRatio.killRoi();
                  } else {
                      setRoi(loopRoi);
                      stats = this.getStatistics();
                      killRoi();
                  }

                  // Set as smallest Roi that the mouse is within and save stats
                  if (smallestRoi == null) {
                     smallestRoi = loopRoi;
                     smallestRoiStats = stats;
                     smallestRoiArea = smallestRoiStats.area;
                     if (linecheck)
                        smallestRoiArea = 0;
                  } else {
                     if (stats.area < smallestRoiArea || linecheck) {
                        smallestRoi = loopRoi;
                        smallestRoiArea = stats.area;
                        smallestRoiStats = stats;
                     }
                  }
            }
        }

       //get numerator and denominator stats
       if ((this.getMimsType() == HSI_IMAGE || this.getMimsType() == RATIO_IMAGE)
                && internalNumerator != null && internalDenominator != null) {
          internalNumerator.setRoi(smallestRoi);
          numeratorStats = internalNumerator.getStatistics();
          internalNumerator.killRoi();

          internalDenominator.setRoi(smallestRoi);
          denominatorStats = internalDenominator.getStatistics();
          internalDenominator.killRoi();
       }

        double sf=1.0;
        if(this.getMimsType()==HSI_IMAGE) {
            sf=this.getHSIProps().getRatioScaleFactor();
        }
        if(this.getMimsType()==RATIO_IMAGE) {
            sf=this.getRatioProps().getRatioScaleFactor();
        }
        // Highlight the "inner most" Roi.
        if(smallestRoi!=null) {
            smallestRoi.setInstanceColor(java.awt.Color.YELLOW);
        }
        //set image roi for vizualization
        if (smallestRoi != null) {
           setRoi(smallestRoi);
           if (roi.getType() == Roi.LINE || roi.getType() == Roi.FREELINE || roi.getType() == Roi.POLYLINE)
              msg += "\t ROI " + roi.getName() + ": L=" + IJ.d2s(roi.getLength(), 0);
           else
              msg += "\t ROI " + roi.getName() + ": A=" + IJ.d2s(smallestRoiStats.area, 0) + ", M=" + IJ.d2s(smallestRoiStats.mean, displayDigits) + ", Sd=" + IJ.d2s(smallestRoiStats.stdDev, displayDigits);
           updateHistogram(true);
           updateLineProfile();
           if((this.getMimsType()==HSI_IMAGE || this.getMimsType()==RATIO_IMAGE) && numeratorStats!=null && denominatorStats!=null) {
               double ratio_means = sf*(numeratorStats.mean/denominatorStats.mean);
               if (this.getMimsType()==HSI_IMAGE && ui.getIsPercentTurnover()) {
                  float reference = ui.getPreferences().getReferenceRatio();
                  float background = ui.getPreferences().getBackgroundRatio();
                  float ratio_means_fl = HSIProcessor.turnoverTransform((float)ratio_means, reference, background, (float)sf);
                  ratio_means = (double)ratio_means_fl;
               }
               msg += ", N/D=" + IJ.d2s(ratio_means, displayDigits);
           }
        }
        ui.updateStatus(msg);
    }

    private String getValueAsString(int x, int y) {
    	if (win!=null && win instanceof PlotWindow)
    		return "";
		Calibration cal = getCalibration();
    	int[] v = getPixel(x, y);
    	int type = getType();
		switch (type) {
			case GRAY8: case GRAY16: case COLOR_256:
				if (type==COLOR_256) {
					if (cal.getCValue(v[3])==v[3]) // not calibrated
						return(", index=" + v[3] + "," + v[0] + "," + v[1] + "," + v[2]);
					else
						v[0] = v[3];
				}
				double cValue = cal.getCValue(v[0]);
				if (cValue==v[0])
    				return("" + v[0]);
    			else
    				return("" + IJ.d2s(cValue) + " ("+v[0]+")");
    		case GRAY32:
            DecimalFormat dec = new DecimalFormat();
            dec.setMaximumFractionDigits(0);
    			return("" + dec.format(Float.intBitsToFloat(v[0])));
			case COLOR_RGB:
    			return("" + v[0] + "," + v[1] + "," + v[2]);
    		default: return("");
		}
    }

    @Override
    // Display statistics while dragging or creating ROIs.
    public void mouseDragged(MouseEvent e) {

        if( Toolbar.getBrushSize() != 0 && Toolbar.getToolId()==Toolbar.OVAL) return;

       // get mouse poistion
       int x = (int) e.getPoint().getX();
       int y = (int) e.getPoint().getY();
       int mX = getWindow().getCanvas().offScreenX(x);
       int mY = getWindow().getCanvas().offScreenY(y);
       String msg = "" + mX + "," + mY;

       int cslice = getCurrentSlice();
        boolean stacktest = isStack();
        if (this.nType == RATIO_IMAGE || this.nType == HSI_IMAGE) {
            stacktest = stacktest || this.getNumeratorImage().isStack();
            if(stacktest) cslice = this.getNumeratorImage().getCurrentSlice();
        }
        if (stacktest) {
            msg += "," + cslice + " = ";
        } else {
            msg += " = ";
        }

       if((this.nType == RATIO_IMAGE || this.nType == HSI_IMAGE) && (this.internalDenominator!=null && this.internalNumerator!=null) ) {
            float ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
            float dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
            double ratio = internalRatio.getProcessor().getPixelValue(mX, mY);
            String opstring = "";
            if(ui.getMedianFilterRatios()) { opstring = "-med->"; } else { opstring = "=";}
            msg += "S (" + (int)ngl + " / " + (int)dgl + ") " + opstring + " " + IJ.d2s(ratio, 4);
        }
        else if(this.nType == SUM_IMAGE) {
            float ngl, dgl;
            if (internalNumerator != null && internalDenominator != null) {
               ngl = internalNumerator.getProcessor().getPixelValue(mX, mY);
               dgl = internalDenominator.getProcessor().getPixelValue(mX, mY);
               msg += " S (" + ngl + " / " + dgl + ") = ";
            }
            int[] gl = getPixel(mX, mY);
            float s = Float.intBitsToFloat(gl[0]);
            msg += IJ.d2s(s,0);
        }
        else {
            MimsPlus[] ml = ui.getOpenMassImages() ;
            for(int i = 0 ; i < ml.length ; i++ ) {
                int [] gl = ml[i].getPixel(mX,mY);
                msg += gl[0] ;
                if( i+1 < ml.length ) {
                    msg += ", ";
                }
            }
        }

       // precision
       int displayDigits = 3;

       // Get the ROI, (the area in yellow).
       Roi roi = getRoi();

        double sf = 1.0;
        if (this.getMimsType() == HSI_IMAGE) {
            sf = this.getHSIProps().getRatioScaleFactor();
        }
        if (this.getMimsType() == RATIO_IMAGE) {
            sf = this.getRatioProps().getRatioScaleFactor();
        }

      // Display stats in the message bar.
      if (roi != null) {
          ij.process.ImageStatistics stats = this.getStatistics();
          ij.process.ImageStatistics numeratorStats = null;
          ij.process.ImageStatistics denominatorStats = null;

         if (this.getMimsType() == MimsPlus.HSI_IMAGE) {
            this.internalRatio.setRoi(roi);
             stats =  this.internalRatio.getStatistics();
            msg += "\t ROI " + roi.getName() + ": A=" + IJ.d2s(stats.area, 0) + ", M=" + IJ.d2s(stats.mean, displayDigits) + ", Sd=" + IJ.d2s(stats.stdDev, displayDigits);
         } else {
            msg += "\t ROI " + roi.getName() + ": A=" + IJ.d2s(stats.area, 0) + ", M=" + IJ.d2s(stats.mean, displayDigits) + ", Sd=" + IJ.d2s(stats.stdDev, displayDigits);
         }

          //get numerator denominator stats
          if ((this.getMimsType() == HSI_IMAGE || this.getMimsType() == RATIO_IMAGE) && internalNumerator != null && internalDenominator != null) {
              internalNumerator.setRoi(roi);
              numeratorStats = internalNumerator.getStatistics();
              internalNumerator.killRoi();

              internalDenominator.setRoi(roi);
              denominatorStats = internalDenominator.getStatistics();
              internalDenominator.killRoi();

              double ratio_means = sf*numeratorStats.mean/denominatorStats.mean;
              msg += ", N/D=" + IJ.d2s(ratio_means, displayDigits);
          }
         ui.updateStatus(msg);

         setRoi(roi);

         updateHistogram(false);
         updateLineProfile();

      }
   }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e){
        int plane = 1;
        int size = 1;
        MimsPlus mp = null;
        if(IJ.controlKeyDown()) {
            this.getWindow().mouseWheelMoved(e);
            return;
        }

        if(this.nType == MimsPlus.MASS_IMAGE) {
            this.getWindow().mouseWheelMoved(e);
            return;
        }else if (this.nType == MimsPlus.HSI_IMAGE) {
            mp = ui.getMassImage(this.getHSIProps().getNumMassIdx());
            plane = mp.getSlice();
            size = mp.getStackSize();
        } else if(this.nType == MimsPlus.RATIO_IMAGE) {
            mp = ui.getMassImage(this.getRatioProps().getNumMassIdx());
            plane = mp.getSlice();
            size = mp.getStackSize();
        }else if( this.nType == MimsPlus.SUM_IMAGE ){ return; }
        if(mp==null) return;

        int d = e.getWheelRotation();
        if( ( (plane + d)<=size ) && ( (plane+d)>=1 ) ) {
            mp.setSlice(plane+d);
        }
    }

    /**
     * Updates historgram values.
     * @param force force update (true or false)
     */
    private void updateHistogram(boolean force) {
        if (roi == null) {
            return;
        }
        // Update histogram (area Rois only).
        if ((roi.getType() == Roi.FREEROI) || (roi.getType() == Roi.OVAL) ||
                (roi.getType() == Roi.POLYGON) || (roi.getType() == Roi.RECTANGLE)) {
            int imageLabel = ui.getRoiManager().getIndex(roi.getName()) + 1;
            String label = getShortTitle() + " Roi: (" + imageLabel + ")";
            double[] roiPix;
            if (this.nType == HSI_IMAGE) {
                internalRatio.setRoi(getRoi());
                roiPix = internalRatio.getRoiPixels();
                internalRatio.killRoi();
            } else {
                roiPix = this.getRoiPixels();
            }
            if (roiPix != null) {
                ui.getmimsTomography().updateHistogram(roiPix, label, force);
            }
        }

    }

   /**
    * Update image line profile. Line profiles for ratio images and
    * HSI images should be identical.
    */
   private void updateLineProfile() {
      if (roi == null)
         return;

      if (!roi.isLine())
         return;

      // Line profiles for ratio images and HSI images should be identical.
      if (this.nType == HSI_IMAGE) {
         internalRatio.setRoi(getRoi());
         ij.gui.ProfilePlot profileP = new ij.gui.ProfilePlot(internalRatio);
         internalRatio.killRoi();
         ui.updateLineProfile(profileP.getProfile(), this.getShortTitle() + " : " + roi.getName(), this.getProcessor().getLineWidth());
      } else {
         ij.gui.ProfilePlot profileP = new ij.gui.ProfilePlot(this);
         ui.updateLineProfile(profileP.getProfile(), this.getShortTitle() + " : " + roi.getName(), this.getProcessor().getLineWidth());
      }

   }

   /**
    * Obtain ROI pixel values.
    * @return Array of pixel values
    */
    public double[] getRoiPixels() {
        if (this.getRoi()==null) return null;

        Rectangle rect = roi.getBoundingRect();
        ij.process.ImageProcessor imp = this.getProcessor();
        ij.process.ImageStatistics stats = this.getStatistics();

        byte[] mask = imp.getMaskArray();
        int i, mi;

        if (mask == null) {
            double[] pixels = new double[rect.width * rect.height];
            i = 0;

            for (int y = rect.y; y < (rect.y + rect.height); y++) {
                for (int x = rect.x; x < (rect.x + rect.width); x++) {
                    pixels[i] = imp.getPixelValue(x, y);
                    i++;
                }
            }

            return pixels;
        } else {
            java.util.ArrayList<Double> pixellist = new java.util.ArrayList<Double>();
            for (int y = rect.y, my = 0; y < (rect.y + rect.height); y++, my++) {
                i = y * width + rect.x;
                mi = my * rect.width;
                for (int x = rect.x; x < (rect.x + rect.width); x++) {

                    // I had to add this line because oval Rois were generating
                    // an OutOfBounds exception when being dragged off the canvas.
                    if (mi >= mask.length) break;

                    // mask should never be null here.
                    if (mask == null || mask[mi++] != 0) {
                        pixellist.add((double)imp.getPixelValue(x, y));
                    }
                    i++;
                }
            }
            double[] foo = new double[pixellist.size()];
            for(int j =0; j< foo.length; j++)
                foo[j] = pixellist.get(j);
            return foo;
        }
    }

    /**
     * Adds listener tp the object.
     * @param inListener Listener to add
     */
    public void addListener(MimsUpdateListener inListener) {
        fStateListeners.add(MimsUpdateListener.class, inListener);
    }

    /**
     * Removes listener from the object.
     * @param inListener Listener to remove
     */
    public void removeListener(MimsUpdateListener inListener) {
        fStateListeners.remove(MimsUpdateListener.class, inListener);
    }

    /**
     * Set LUT
     * @param label Label for LUT
     */
    public void setLut(String label) {
        lut = label;
    }

   /**
    * extends setSlice to notify listeners when the frame updates
    * enabling synchronization with other windows
    * @param slice
    * @param attr
    */
    private void stateChanged(int slice, int attr) {
        bStateChanging = true ;
        MimsPlusEvent event = new MimsPlusEvent(this, slice, attr);
        Object[] listeners = fStateListeners.getListenerList();
        for(int i=listeners.length-2; i >= 0; i -= 2){
            if(listeners[i] == MimsUpdateListener.class ){
                    ((MimsUpdateListener)listeners[i+1])
					.mimsStateChanged(event);
            }
        }
        bStateChanging = false ;
    }

    private void stateChanged(int slice, int attr, boolean updateRatioHSI) {
        bStateChanging = true ;
        MimsPlusEvent event = new MimsPlusEvent(this, slice, attr, updateRatioHSI);
        Object[] listeners = fStateListeners.getListenerList();
        for(int i=listeners.length-2; i >= 0; i -= 2){
            if(listeners[i] == MimsUpdateListener.class ){
                    ((MimsUpdateListener)listeners[i+1])
					.mimsStateChanged(event);
            }
        }
        bStateChanging = false ;
    }

    private void stateChanged(int slice, int attr, MimsPlus mplus) {
        bStateChanging = true ;
        MimsPlusEvent event = new MimsPlusEvent(this, slice, attr, mplus);
        Object[] listeners = fStateListeners.getListenerList();
        for(int i=listeners.length-2; i >= 0; i -= 2){
            if(listeners[i] == MimsUpdateListener.class ){
                    ((MimsUpdateListener)listeners[i+1])
					.mimsStateChanged(event);
            }
        }
        bStateChanging = false ;
    }

    private void stateChanged(ij.gui.Roi roi, int attr) {
        MimsPlusEvent event = new MimsPlusEvent(this, roi, attr);
        Object[] listeners = fStateListeners.getListenerList();
        for(int i=listeners.length-2; i >= 0; i -= 2){
            if(listeners[i] == MimsUpdateListener.class ){
                    ((MimsUpdateListener)listeners[i+1])
					.mimsStateChanged(event);
            }
        }
    }

    /**
     * Sets the slice.
     * @param index the plane number (starts with 1).
     */
    @Override
    public synchronized void setSlice(int index) {
        if(getCurrentSlice() == index) {
            return;
        }
        super.setSlice(index);
        if(bStateChanging) {
            return;
        }
        stateChanged(index,MimsPlusEvent.ATTR_UPDATE_SLICE);
    }

    /**
     * Set slice as current
     * @param index
     * @param updateRatioHSI
     */
    public synchronized void setSlice(int index, boolean updateRatioHSI) {
        if (getCurrentSlice() == index) {
            return;
        }
        super.setSlice(index);
        if (bStateChanging) {
            return;
        }
        stateChanged(index, MimsPlusEvent.ATTR_UPDATE_SLICE, false);
    }

    /**
     * Set slice as current
     * @param index
     * @param mplus
     */
    public synchronized void setSlice(int index, MimsPlus mplus) {
        if(this.getCurrentSlice() == index) {
            return;
        }
        super.setSlice(index);
        if(bStateChanging) {
            return;
        }
        stateChanged(index,MimsPlusEvent.ATTR_UPDATE_SLICE, mplus);
    }

    /**
     * Set the allow close flag
     * @param allowClose
     */
    public void setAllowClose(boolean allowClose){
        this.allowClose = allowClose;
    }

    /**
     *
     */
    @Override
    public void close() {
        if (allowClose) {
            ui.imageClosed(this);
            ui.getCBControl().removeWindowfromList(this);
            ui.getmimsTomography().resetImageNamesList();
            super.close();
        } else {
            this.hide();
            ui.massImageClosed(this);
        }
    }

    /**
     * Set HSI processor
     * @param processor
     */
    public void setHSIProcessor( HSIProcessor processor ) { this.hsiProcessor = processor ; }

    /**
     *
     * @return HSI processor
     */
    public HSIProcessor getHSIProcessor() { return hsiProcessor ; }

    /**
     * Sets Composite processor
     * @param processor
     */
    public void setCompositeProcessor( CompositeProcessor processor ) { this.compProcessor = processor ; }

    /**
     *
     * @return composite processor
     */
    public CompositeProcessor getCompositeProcessor() { return compProcessor ; }

    /**
     * Sets auto comtrast adjust flag (true/false)
     * @param auto auto comtrast adjust flag
     */
    public void setAutoContrastAdjust( boolean auto ) { this.autoAdjustContrast = auto ; }

    /**
     *
     * @return auto comtrast adjust flag
     */
    public boolean getAutoContrastAdjust() { return autoAdjustContrast ; }

    /**
     *
     * @return "is stack" flag
     */
    public boolean isStack() { return bIsStack ; }

    /**
     * Sets "is stack" flag
     * @param isS
     */
    public void setIsStack(boolean isS) { bIsStack = isS; }

    /**
     *
     * @return mass index
     */
    public int getMassIndex() { return massIndex; }

    /**
     *
     * @return MIMS type
     */
    public int getMimsType() { return nType ; }

    /**
     *
     * @return sum properties
     */
    public SumProps getSumProps() { return sumProps; }

    /**
     *
     * @return ratio properties
     */
    public RatioProps getRatioProps() { return ratioProps; }

    /**
     *
     * @return HSI properties
     */
    public HSIProps getHSIProps() { return getHSIProcessor().getHSIProps(); }

    /**
     *
     * @return ratio properties
     */
    public CompositeProps getCompositeProps() { return compProps; }

    /**
     *
     * @return UI object
     */
    public UI getUI() { return ui ; }

}
