/*
 * UI.java
 *
 * Created on May 1, 2006, 12:59 PM
 */
package com.nrims;

import com.nrims.data.*;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.ImageWindow;
import ij.gui.ImageCanvas;
import ij.process.ColorProcessor;
import ij.process.ImageStatistics;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Image;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JOptionPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.TextAnchor;

/**
 * The main user interface of the NRIMS ImageJ plugin.
 */
public class UI extends PlugInJFrame implements WindowListener, MimsUpdateListener {

    public static final long serialVersionUID = 1;
    public static final String NRRD_EXTENSION = ".nrrd";
    public static final String MIMS_EXTENSION = ".im";
    public static final String ROIS_EXTENSION = ".rois.zip";
    public static final String ROI_EXTENSION = ".roi";
    public static final String RATIO_EXTENSION = ".ratio";
    public static final String HSI_EXTENSION = ".hsi";
    public static final String SUM_EXTENSION = ".sum";
    public static final String ACT_EXTIONSION = ".act";

    public static final String SAVE_IMAGE = "Save Image";
    public static final String SAVE_SESSION = "Save Session";

    public int maxMasses = 8;
    private int ratioScaleFactor = 10000;
    private double medianFilterRadius = 1;
    
    private boolean bDebug = false;
    private boolean bSyncStack = true;
    private boolean bSyncROIs = true;
    private boolean bSyncROIsAcrossPlanes = true;
    private boolean bAddROIs = true;
    private boolean bUpdating = false;    
    private boolean currentlyOpeningImages = false;
    private boolean bCloseOldWindows = true;
    private boolean medianFilterRatios = false;
    private boolean isSum = false;
    private boolean isWindow = false;
    private int     windowRange = -1;
    private boolean[] bOpenMass = new boolean[maxMasses];
            
    private String lastFolder = null;      
    public  File   tempActionFile;        
    
            
    private HashMap openers = new HashMap();
    
    private MimsPlus[] massImages = new MimsPlus[maxMasses];
    private MimsPlus[] ratioImages = new MimsPlus[maxMasses];
    public MimsPlus[] hsiImages = new MimsPlus[maxMasses];
    private MimsPlus[] segImages = new MimsPlus[maxMasses];
    private MimsPlus[] sumImages = new MimsPlus[2 * maxMasses];

    private MimsPlus[] compImages = new MimsPlus[2 * maxMasses];

    private MimsData mimsData = null;
    private MimsLog mimsLog = null;   
    private MimsCBControl cbControl = new MimsCBControl(this);
    private MimsStackEditing mimsStackEditing = null;
    private MimsRoiManager roiManager = null;
    private MimsTomography mimsTomography = null;        
    private HSIView hsiControl = null;
    private SegmentationForm segmentation = null;    
    private javax.swing.JRadioButtonMenuItem[] viewMassMenuItems = null;
    private Opener image = null;
    private ij.ImageJ ijapp = null;
    private FileDrop mimsDrop;    
    
    private Point[] windowPositions = null;
    private int[] hiddenWindows = null;
    private double[] windowZooms = null;

    protected MimsLineProfile lineProfile;
    protected MimsAction mimsAction = null; 

    private imageNotes imgNotes;

    private PrefFrame prefs;

    private String revisionNumber = "";

    private static String im_file_path = null;
    /*
     * Private stings for option parsing
     */
    private static final String IMFILE_OPTION = "-imfile";

    /**
     * Constructor
     * Creates a new instance of the OpenMIMS analysis interface.
     * @param fileName path to the OpenMIMS file to analyze
     */
    public UI(String fileName) {
        super("NRIMS Analysis Module");
      
      System.out.println("Ui constructor");
      System.out.println(System.getProperty("java.version") + " : " + System.getProperty("java.vendor"));

      revisionNumber = extractRevisionNumber();

      // Set look and feel to native OS
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         SwingUtilities.updateComponentTreeUI(this);
      } catch (Exception e) {
         IJ.log("Error setting native Look and Feel:\n" + e.toString());
      }

      initComponents();
      initComponentsCustom();
      //read in preferences so values are gettable
      //by various tabs (ie mimsTomography, HSIView, etc.
      //when constructed further down
      prefs = new PrefFrame();

      ijapp = IJ.getInstance();
      if (ijapp == null || (ijapp != null && !ijapp.isShowing())) {
         ijapp = new ij.ImageJ(null);
         setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
      }

      if (image == null) {
         for (int i = 0; i < maxMasses; i++) {
            massImages[i] = null;
            ratioImages[i] = null;
            hsiImages[i] = null;
            segImages[i] = null;
         }
         for (int i = 0; i < 2 * maxMasses; i++) {
            sumImages[i] = null;
         }
      }

      int xloc, yloc = 150;
      if (ijapp != null) {
         xloc = ijapp.getX();
         if (xloc + ijapp.getWidth() + this.getPreferredSize().width + 10 <
                 Toolkit.getDefaultToolkit().getScreenSize().width) {
            xloc += ijapp.getWidth() + 10;
            yloc = ijapp.getY();
         } else {
            yloc = ijapp.getY() + ijapp.getHeight() + 10;
         }
      } else {
         int screenwidth = Toolkit.getDefaultToolkit().getScreenSize().width;
         xloc = (int) (screenwidth > 832 ? screenwidth * 0.8 : screenwidth * 0.9);
         xloc -= this.getPreferredSize().width + 10;
      }

      this.setLocation(xloc, yloc);
      ij.WindowManager.addWindow(this);

      if (bDebug) {
         IJ.log("open UI ok...");
      }
      IJ.showProgress(1.0);

      this.mimsDrop = new FileDrop(null, jTabbedPane1, new FileDrop.Listener() {
         public void filesDropped(File[] files) {
            // Get HSIProps for all open ratio images.
            RatioProps[] rto_props = getOpenRatioProps();

            // Get HSIProps for all open hsi images.
            HSIProps[] hsi_props = getOpenHSIProps();

            // Get SumProps for all open sum images.
            SumProps[] sum_props = getOpenSumProps();

            openFiles(files);

            /*
            getOpenMassImages()[2].getWindow().toFront();
            WindowManager.setCurrentWindow(getOpenMassImages()[2].getWindow());
            WindowManager.setTempCurrentImage(getOpenMassImages()[2]);

            if (getOpenMassImages()[2].getNSlices()>1) {
              ActionEvent ae = new ActionEvent(mimsStackEditing.atManager.okButton, -1, "OK");
              mimsStackEditing.atManager.actionPerformed(ae);
            }

            while(mimsStackEditing.THREAD_STATE == mimsStackEditing.WORKING){
               try {
                  System.out.println("tracking...");
                  Thread.sleep(250);
               } catch (Exception e){}
            }
            
            File file = new File("/nrims/home3/zkaufman/DOCS/TrackingDocuments/TAJBAKHSH", getImageFilePrefix()+NRRD_EXTENSION);
            saveSession(file.getAbsolutePath(), true);
            */

            // Generate all images that were previously open.
            // TODO: a better check than just looking at the first file?
            if( files[0].getAbsolutePath().endsWith(NRRD_EXTENSION) ||
               files[0].getAbsolutePath().endsWith(MIMS_EXTENSION) ) {
                  restoreState(rto_props, hsi_props, sum_props);
            }
            MimsRoiManager rm = getRoiManager();
            if( rm!=null ) {
                rm.resetRoiLocationsLength();
            }

            //Autocontrast mass images.
            //Should add option to apply settings from previous image?
            autoContrastImages(getOpenMassImages());
       }
      });      
      //loadMIMSFile(new File("/nrims/home3/zkaufman/Images/test_file.im"));
   }

    /**
     * Insertion status of the current MimsPlus object
     * @param mp object to be inserted
     * @return success/failure of insertion
     */
   public boolean addToImagesList(MimsPlus mp) {
      int i = 0; int ii = 0; boolean inserted=false;
      while (i < maxMasses) {
         if (mp.getMimsType() == MimsPlus.RATIO_IMAGE && ratioImages[i] == null) {
            inserted = true;
            ratioImages[i] = mp;
            getCBControl().addWindowtoList(mp);
            return true;
         }
         if (mp.getMimsType() == MimsPlus.HSI_IMAGE && hsiImages[i] == null) {
            inserted = true;
            hsiImages[i] = mp;
            return true;
         }
         if (mp.getMimsType() == MimsPlus.SEG_IMAGE && segImages[i] == null) {
            inserted = true;
            segImages[i] = mp;
            return true;
         }
         i++;         
      }     
      
      // Sum and composite images has a larger array size.
      while (ii < 2 * maxMasses) {
         if (mp.getMimsType() == MimsPlus.SUM_IMAGE && sumImages[ii] == null) {
            inserted = true;
            sumImages[ii] = mp;
            getCBControl().addWindowtoList(mp);
            return true;
         }
         if (mp.getMimsType() == MimsPlus.COMPOSITE_IMAGE && compImages[ii] == null) {
            inserted = true;
            compImages[ii] = mp;
            return true;
         }
         ii++;
      }
      if (!inserted) System.out.println("Too many open images");
      return inserted;
   }

   /**
    * Swap crosshairs from hidden to shown or vice versa
    * @param chartpanel GUI element to be affected
    */
   //can these 2 methods be moved to MimsJFreeChart????
   void showHideCrossHairs(ChartPanel chartpanel) {
      Plot plot = chartpanel.getChart().getPlot();
      if (!(plot instanceof XYPlot))
         return;
      
      // Show/Hide XHairs
      XYPlot xyplot = (XYPlot) plot;
      xyplot.setDomainCrosshairVisible(!xyplot.isDomainCrosshairVisible());
      xyplot.setRangeCrosshairVisible(!xyplot.isRangeCrosshairVisible());
      xyplot.showXHairLabel(xyplot.isDomainCrosshairVisible() || xyplot.isDomainCrosshairVisible());
   }

    /**
    * Change y axis from linear to log scale or vice versa
    * @param chartpanel GUI element to be affected
    */
   
   void logLinScale(ChartPanel chartpanel) {
      Plot plot = chartpanel.getChart().getPlot();

      if (!(plot instanceof XYPlot))
         return;


      XYPlot xyplot = (XYPlot) plot;
      org.jfree.chart.axis.ValueAxis axis = xyplot.getRangeAxis();
      String label = axis.getLabel();

       if (!(axis instanceof org.jfree.chart.axis.LogarithmicAxis)) {
           org.jfree.chart.axis.LogarithmicAxis logaxis = new org.jfree.chart.axis.LogarithmicAxis(label);
           logaxis.setRange(axis.getLowerBound(), axis.getUpperBound());
           logaxis.setAutoRange(true);
           logaxis.setStrictValuesFlag(false);
           xyplot.setRangeAxis(logaxis);
       } else {
           org.jfree.chart.axis.NumberAxis linaxis = new org.jfree.chart.axis.NumberAxis(label);
           linaxis.setAutoRange(true);
           xyplot.setRangeAxis(linaxis);
       }

   }

   /**
    * Extract and show table of plot's underlying data
    * @param chartpanel GUI element to be affected
    */

   //Todo, clean up
      public void displayProfileData(ChartPanel chartpanel) {
        org.jfree.chart.plot.XYPlot plot = (XYPlot) chartpanel.getChart().getPlot();
        org.jfree.data.xy.XYDataset data = plot.getDataset();

        ij.measure.ResultsTable table = new ij.measure.ResultsTable();
        table.setHeading(1, "Plane");
        for(int i = 0; i<plot.getLegendItems().getItemCount(); i++) {
            table.setHeading(i+2, plot.getLegendItems().get(i).getLabel() );
        }
        
        //table.incrementCounter();

        //end of table bug?
        for (int i = 0; i < data.getItemCount(0); i++) {
            table.incrementCounter();
            table.addValue(1, data.getXValue(0, i));

            for(int j = 0; j < plot.getLegendItems().getItemCount(); j++) {
                table.addValue(j+1, data.getYValue(j, i));
            }
        }

        table.show("");
    }

    /**
     * Closes the current image and its associated set of windows if the mode is set to close open windows.
     */
    private synchronized void closeCurrentImage() {
        this.windowPositions = gatherWindowPosistions();
        this.hiddenWindows = gatherHiddenWindows();
        this.windowZooms = this.gatherWindowZooms();
        for (int i = 0; i < maxMasses; i++) {
            if (segImages[i] != null) {
                segImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (segImages[i].getWindow() != null) {
                        segImages[i].getWindow().close();
                        segImages[i] = null;
                    }
                }
            }
            if (massImages[i] != null) {
                massImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (massImages[i].getWindow() != null) {
                        massImages[i].getWindow().close();
                        massImages[i] = null;
                    }
                }
            }
            bOpenMass[i] = false;
            if (hsiImages[i] != null) {
                hsiImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (hsiImages[i].getWindow() != null) {
                        hsiImages[i].getWindow().close();
                        hsiImages[i] = null;
                    }
                }
            }
            if (ratioImages[i] != null) {
                ratioImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (ratioImages[i].getWindow() != null) {
                        ratioImages[i].getWindow().close();
                        ratioImages[i] = null;
                    }
                }
            }
        }

        for (int i = 0; i < maxMasses * 2; i++) {
            if (sumImages[i] != null) {
                sumImages[i].removeListener(this);
                if (bCloseOldWindows) {
                    if (sumImages[i].getWindow() != null) {
                        sumImages[i].getWindow().close();
                        sumImages[i] = null;
                    }
                }
            }
        }
    }

    /**
     * Brings up the graphical pane for selecting files to be opened.
     */
    public synchronized void loadMIMSFile() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setMultiSelectionEnabled(true);        
        fc.setPreferredSize(new java.awt.Dimension(650, 500));

        if (lastFolder != null) {
            fc.setCurrentDirectory(new java.io.File(lastFolder));
        } else {
            String ijDir = new ij.io.OpenDialog("", "asdf").getDefaultDirectory();
            if(ijDir != null && !(ijDir.equalsIgnoreCase("")) )
                fc.setCurrentDirectory(new java.io.File(ijDir));
        }

        if (fc.showOpenDialog(this) == JFileChooser.CANCEL_OPTION) {
            lastFolder = fc.getCurrentDirectory().getAbsolutePath();
            return;
        }
        lastFolder = fc.getSelectedFile().getParent();
        setIJDefaultDir(lastFolder);

        File[] files = fc.getSelectedFiles();
        openFiles(files);
    }

    /**
     * Open the file(s) for futher processing.
     * @param files list of files to open
     */
    public void openFiles(File[] files) {

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            boolean onlyShowDraggedFile = true;

            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (i == 0) {
                   lastFolder = file.getParent();
                   setIJDefaultDir(lastFolder);
                }
                if (file.getAbsolutePath().endsWith(NRRD_EXTENSION) ||
                    file.getAbsolutePath().endsWith(MIMS_EXTENSION)) {
                    onlyShowDraggedFile = false;
                    loadMIMSFile(file);
                    break;
                }
            }

            try {
            // Loop thru files.
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (!file.exists()) continue;

                // Load ratio image.
                if (file.getAbsolutePath().endsWith(RATIO_EXTENSION)) {
                    FileInputStream f_in = new FileInputStream(file);
                    ObjectInputStream obj_in = new ObjectInputStream(f_in);
                    Object obj = obj_in.readObject();
                    if (obj instanceof RatioProps) {
                        RatioProps ratioprops = (RatioProps)obj;
                        String dataFileString = ratioprops.getDataFileName();
                        File dataFile = new File(file.getParent(), dataFileString);
                        if (image == null)
                            loadMIMSFile(dataFile);
                        else if (image != null && !dataFileString.matches(image.getImageFile().getName()))
                            loadMIMSFile(dataFile);
                        MimsPlus mp = new MimsPlus(this, ratioprops);
                        mp.showWindow();
                    }
                }

                // Load hsi image.
                if (file.getAbsolutePath().endsWith(HSI_EXTENSION)) {
                    FileInputStream f_in = new FileInputStream(file);
                    ObjectInputStream obj_in = new ObjectInputStream(f_in);
                    Object obj = obj_in.readObject();
                    if (obj instanceof HSIProps) {
                        HSIProps hsiprops = (HSIProps)obj;
                        String dataFileString = hsiprops.getDatFileName();
                        File dataFile = new File(file.getParent(), dataFileString);
                        if (image == null)
                            loadMIMSFile(dataFile);
                        else if (image != null && !dataFileString.matches(image.getImageFile().getName()))
                            loadMIMSFile(dataFile);
                        MimsPlus mp = new MimsPlus(this, hsiprops);
                        mp.showWindow();
                    }
                }

                // Load sum image.
                if (file.getAbsolutePath().endsWith(SUM_EXTENSION)) {
                    FileInputStream f_in = new FileInputStream(file);
                    ObjectInputStream obj_in = new ObjectInputStream(f_in);
                    Object obj = obj_in.readObject();
                    if (obj instanceof SumProps) {
                        SumProps sumprops = (SumProps)obj;
                        String dataFileString = sumprops.getDataFileName();
                        File dataFile = new File(file.getParent(), dataFileString);
                        if (image == null)
                            loadMIMSFile(dataFile);
                        else if (image != null && !dataFileString.matches(image.getImageFile().getName()))
                            loadMIMSFile(dataFile);
                        MimsPlus sp = new MimsPlus(this, sumprops, null);
                        sp.showWindow();
                    }
                }

                // Load Rois.
                if (file.getAbsolutePath().endsWith(ROIS_EXTENSION) ||
                    file.getAbsolutePath().endsWith(ROI_EXTENSION)) {
                   getRoiManager().open(file.getAbsolutePath(), true);
                   updateAllImages();
                   getRoiManager().showFrame();
                }

            }

            //Todo: if a bs file this still gets called and throws?
            
            // This 3 lines solves some strange updating issues.
            // Has to do with syncing images by changing the slice.
            int startslice = massImages[0].getCurrentSlice();
            massImages[0].setSlice(getOpener().getNImages());
            massImages[0].setSlice(startslice);

            if (onlyShowDraggedFile){
               MimsPlus[] mps = getOpenMassImages();
               for (int i = 0; i < mps.length; i++)
                  mps[i].hide();
            }

            } catch (Exception e) {
               e.printStackTrace();
            } finally {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
    }

    public synchronized void loadMIMSFile(File file) throws NullPointerException {
        if (!file.exists()) {
            throw new NullPointerException("File " + file.getAbsolutePath() + " does not exist!");
        }

        try {
            currentlyOpeningImages = true;

            //what is this going to do?
            closeCurrentImage();
            
            // Clear selections in the jlist (prevents exceptions from being thrown).
            getRoiManager().roijlist.clearSelection();

            try {

                //need to add checks
                if (file.getName().endsWith(MIMS_EXTENSION)) {
                   image = new Mims_Reader(file);
                } else if (file.getName().endsWith(NRRD_EXTENSION)) {
                   image = new Nrrd_Reader(file);
                } else {
                   return;
                }
               
            } catch (IOException e) {
                IJ.log("Failed to open " + file + "......  :\n\n");
                logException(e);
                return;
            }

            int nMasses = image.getNMasses();
            int nImages = image.getNImages();

            long memRequired = nMasses * image.getWidth() * image.getHeight() * 2 * nImages;
            //added wiggle room to how big a file can be opened
            //was causing heap size exceptions to be thrown
            long maxMemory = IJ.maxMemory()-(128000000);
            
            for (int i = 0; i < nMasses; i++) {
                bOpenMass[i] = true;
            }
            while (memRequired > maxMemory) {
                ij.gui.GenericDialog gd = new ij.gui.GenericDialog("File Too Large");
                long aMem = memRequired;
                int canOpen = nImages;

                while (aMem > maxMemory) {
                    canOpen--;
                    aMem = nMasses * image.getWidth() * image.getHeight() * 2 * canOpen;
                }

                String[] names = image.getMassNames();
                for (int i = 0; i < image.getNMasses(); i++) {
                    String msg = "Open mass " + names[i];
                    gd.addCheckbox(msg, bOpenMass[i]);
                }
                gd.addNumericField("Open only ", (double) canOpen, 0, 5, " of " + image.getNImages() + " Images");

                gd.showDialog();
                if (gd.wasCanceled()) {
                    image = null;
                    return;
                }

                nMasses = 0;
                for (int i = 0; i < image.getNMasses(); i++) {
                    bOpenMass[i] = gd.getNextBoolean();
                    if (bOpenMass[i]) {
                        nMasses++;
                    }
                }

                nImages = (int) gd.getNextNumber();

                memRequired = nMasses * image.getWidth() * image.getHeight() * 2 * nImages;
            }



            updateStatus("Opening " + file + " ....... " + nMasses + " masses " + nImages + " sections");

            try {
                int n = 0;
                int t = image.getNMasses() * nImages;
                for (int i = 0; i < image.getNMasses(); i++) {
                    IJ.showProgress(++n, t);
                    if (bOpenMass[i]) {
                        MimsPlus mp = new MimsPlus(this, i);
                        mp.setAllowClose(false);
                        massImages[i] = mp;
                        if (mp != null) {
                            massImages[i].getProcessor().setMinAndMax(0, 0);
                            massImages[i].getProcessor().setPixels(image.getPixels(i));                            
                        }
                    }
                }
               
                if (nImages > 1) {
                    // TODO why are we starting from 1 here?
                    for (int i = 1; i < nImages; i++) {
                        image.setStackIndex(i);
                        for (int mass = 0; mass < image.getNMasses(); mass++) {
                            IJ.showProgress(++n, t);
                            if (bOpenMass[mass]) {
                                massImages[mass].appendImage(i);
                            }
                        }
                    }
                }

                for (int i = 0; i < image.getNMasses(); i++) {
                    if (bOpenMass[i]) {
                        if (image.getNImages() > 1) {
                            massImages[i].setSlice(1);
                        }
                        massImages[i].show();
                    }
                }

                if (this.windowPositions != null) {
                    applyWindowPositions(windowPositions);
                } else {
                    //replace with mass image tile
                    ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();

                    wo.run("tile");
                }
                if (this.windowZooms != null) {
                   applyWindowZooms(windowZooms);
                }

            } catch (Exception x) {
                updateStatus(x.toString());
                x.printStackTrace();
            }

            for (int i = 0; i < image.getNMasses(); i++) {
                if (bOpenMass[i]) {
                    massImages[i].addListener(this);
                } else {
                    massImages[i] = null;
                }
            }

            if (mimsData == null) {
                initializeViewMenu();
                mimsData = new com.nrims.MimsData(this, image);
                hsiControl = new HSIView(this);
                mimsLog = new MimsLog(this, image);
                mimsStackEditing = new MimsStackEditing(this, image);
                mimsTomography = new MimsTomography(this);
                mimsAction = new MimsAction(this, image);
                //TODO: throws an exception when opening an image with 2 masses
                segmentation = new SegmentationForm(this);

                jTabbedPane1.setComponentAt(0, mimsData);
                jTabbedPane1.setTitleAt(0, "MIMS Data");
                jTabbedPane1.add("Process", hsiControl);
                jTabbedPane1.add("Contrast", cbControl);
                jTabbedPane1.add("Stack Editing", mimsStackEditing);
                jTabbedPane1.add("Tomography", mimsTomography);
                jTabbedPane1.add("Segmentation", segmentation);
                jTabbedPane1.add("MIMS Log", mimsLog);

            } else {
                resetViewMenu();
                mimsData = new com.nrims.MimsData(this, image);
                cbControl = new MimsCBControl(this);
                mimsStackEditing = new MimsStackEditing(this, image);
                mimsTomography = new MimsTomography(this);
                mimsAction = new MimsAction(this, image);
                //TODO: throws an exception when opening an image with 2 masses
                segmentation = new SegmentationForm(this);
                jTabbedPane1.setComponentAt(0, mimsData);
                jTabbedPane1.setTitleAt(0, "MIMS Data");
                jTabbedPane1.setComponentAt(1, hsiControl);
                jTabbedPane1.setComponentAt(2, cbControl);
                jTabbedPane1.setComponentAt(3, mimsStackEditing);
                jTabbedPane1.setComponentAt(4, mimsTomography);
                jTabbedPane1.setComponentAt(5, segmentation);

                mimsData.setMimsImage(image);
                hsiControl.updateImage();
            }
            
            jTabbedPane1.addChangeListener(new ChangeListener() {
               public void stateChanged(ChangeEvent e){
                  int selected = jTabbedPane1.getSelectedIndex();
                  if (selected == 2) {
                     cbControl.updateHistogram();
                  } 
               }
            });

            this.mimsLog.Log("\n\nNew image: " + getImageFilePrefix() + "\n" + getImageHeader(image));
            this.mimsTomography.resetImageNamesList();
            this.mimsStackEditing.resetSpinners();

            //????????????????
            openers.clear();
            String fName = file.getName();
            openers.put(fName, image);            
            
            // Add the windows to the combobox in CBControl.            
            MimsPlus[] mp = getOpenMassImages();
            for(int i = 0; i < mp.length; i++) {
               cbControl.addWindowtoList(mp[i]);
            }

            //hide mass images if needed
            if (this.hiddenWindows != null) {
                applyHiddenWindows(hiddenWindows);
            }

        } finally {
            currentlyOpeningImages = false;
        }        
    }

    public Point[] gatherWindowPosistions() {
        Point[] positions = new Point[maxMasses];

        MimsPlus[] images = this.getOpenMassImages();
        if(images.length==0) return null;

        for( int i = 0; i < images.length; i++) {
            if( images[i].getWindow() != null)
                positions[i] = images[i].getWindow().getLocation();
            else
                positions[i] = new Point(-9,-9);
        }

        return positions;
    }

     public int[] gatherHiddenWindows() {
        int[] hidden = new int[maxMasses];

        MimsPlus[] images = this.getOpenMassImages();
        if(images.length==0) return null;

        for( int i = 0; i < images.length; i++) {
            if( !images[i].isVisible() )
                hidden[i] = 1;
            else
                hidden[i] = 0;
        }

        return hidden;
    }

     public double[] gatherWindowZooms() {
        double[] zooms = new double[maxMasses];

        for(int i=0; i< zooms.length; i++) { zooms[i] = 1.0; }

        MimsPlus[] images = this.getOpenMassImages();
        if(images.length==0) return null;

        for( int i = 0; i < images.length; i++) {
            if( images[i].getCanvas() != null) {
                zooms[i]=images[i].getCanvas().getMagnification();
            } else {
                zooms[i] = 1.0;
            }
        }

        return zooms;
    }


    public void applyWindowPositions(Point[] positions) {
        for(int i = 0; i < positions.length; i++) {
           if ( positions[i] != null && massImages[i] != null)
               if (massImages[i].getWindow() != null)
                massImages[i].getWindow().setLocation(positions[i]);

        }
    }

    public void applyHiddenWindows(int[] hidden) {
        for(int i = 0; i < hidden.length; i++) {
           if ( massImages[i] != null)
               if( hidden[i] == 1)
                    massImages[i].hide();
        }
    }

    public void applyWindowZooms(double[] zooms) {
        for (int i = 0; i < zooms.length; i++) {
            if (massImages[i] != null) {
                if (massImages[i].getCanvas() != null) {
                   double z = massImages[i].getCanvas().getMagnification();
                   if(z==0.0) continue;
                   if(massImages[i].getCanvas().getMagnification() < zooms[i]) {
                       while(massImages[i].getCanvas().getMagnification() < zooms[i]) {
                           massImages[i].getCanvas().zoomIn(0, 0);
                       }
                   }

                   if(massImages[i].getCanvas().getMagnification() > zooms[i]) {
                       while(massImages[i].getCanvas().getMagnification() > zooms[i]) {
                           massImages[i].getCanvas().zoomOut(0, 0);
                       }
                   }
                }
            }
        }
    }

    private void resetViewMenu() {
        int c=0;
        for(int i = 0; i< windowPositions.length; i++) {
            if(windowPositions[i]!=null) c++;
        }
        //System.out.println("count: "+c);

        for (int i = 0; i < viewMassMenuItems.length; i++) {
            if (i < image.getNMasses()) {
                viewMassMenuItems[i].setText(image.getMassNames()[i]);
                viewMassMenuItems[i].setVisible(true);
                if (i < windowPositions.length && windowPositions[i]!=null) {
                    if (windowPositions[i].x > 0 && windowPositions[i].y > 0) {
                        viewMassMenuItems[i].setSelected(true);
                    } else {
                        viewMassMenuItems[i].setSelected(false);
                    }
                } else {
                    viewMassMenuItems[i].setSelected(true);
                }
            } else {
                viewMassMenuItems[i].setText("foo");
                viewMassMenuItems[i].setVisible(false);
                viewMassMenuItems[i].setSelected(false);
            }

        }
    }

    private void initializeViewMenu() {
        this.viewMassMenuItems = new javax.swing.JRadioButtonMenuItem[this.maxMasses];

        for (int i = 0; i < viewMassMenuItems.length; i++) {
            javax.swing.JRadioButtonMenuItem massRButton = new javax.swing.JRadioButtonMenuItem();

            if (i < image.getNMasses()) {
                massRButton.setVisible(true);
                massRButton.setSelected(true);
                massRButton.setText(image.getMassNames()[i]);
            } else {
                massRButton.setSelected(false);
                massRButton.setText("foo");
                massRButton.setVisible(false);
            }

            massRButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    viewMassChanged(evt);
                }
            });
            viewMassMenuItems[i] = massRButton;

            this.viewMenu.add(massRButton);
        }

    }

    private void viewMassChanged(java.awt.event.ActionEvent evt) {
        if(windowPositions==null) windowPositions = gatherWindowPosistions();
        int index = 0;
        for (int i = 0; i < viewMassMenuItems.length; i++) {
            if (evt.getActionCommand() == viewMassMenuItems[i].getText()) {
                index = i;
            }
        }
        if (massImages[index] != null) {
            if (viewMassMenuItems[index].isSelected() && !massImages[index].isVisible()) {
                //int plane = getVisableMassImages()[0].getSlice();
                massImages[index].show();
                if(windowPositions[index] != null && (windowPositions[index].x > 0 && windowPositions[index].y > 0))
                    massImages[index].getWindow().setLocation(windowPositions[index]);
                massImages[index].setbIgnoreClose(true);
                //massImages[index].setSlice(plane);
                //massImages[index].updateAndDraw();
            } else if( !viewMassMenuItems[index].isSelected() && massImages[index].isVisible()) {
                windowPositions[index] = massImages[index].getWindow().getLocation();
                massImages[index].hide();
            }
        }

        System.out.print(evt.getActionCommand() + " index: " + index);
        System.out.print(" selected: " + viewMassMenuItems[index].isSelected());
        System.out.print(" visable: " + massImages[index].isVisible() + "\n");
    }

    public void massImageClosed(MimsPlus im) {
        if(windowPositions==null) windowPositions = gatherWindowPosistions();
        for (int i = 0; i < massImages.length; i++) {
            if (massImages[i] != null) {
                if (massImages[i].equals(im)) {
                    windowPositions[i] = im.getXYLoc();
                    viewMassMenuItems[i].setSelected(false);
                    //windowZooms[i] = im.getCanvas().getMagnification();

                }
            }
        }
    }

    public int getRatioImageIndex(int numIndex, int denIndex) {        
        for (int i = 0; i < ratioImages.length; i++) {
           if (ratioImages[i] != null) {
               RatioProps rp = ratioImages[i].getRatioProps();
               if (rp.getNumMassIdx() == numIndex && rp.getDenMassIdx() == denIndex) {
                  return i;
               }
           }
        }
        return -1;
    }

    public int getHsiImageIndex(int numIndex, int denIndex) {
        for (int i = 0; i < hsiImages.length; i++) {
           if (hsiImages[i] != null) {
               HSIProps hp = hsiImages[i].getHSIProps();
               if (hp.getNumMassIdx() == numIndex && hp.getDenMassIdx() == denIndex) {
                  return i;
               }
           }
        }
        return -1;
    }

    private String extractRevisionNumber() {
        try {
            InputStream build = getClass().getResourceAsStream("/buildnum.txt");
            InputStreamReader buildr = new InputStreamReader(build);
            BufferedReader br = new BufferedReader(buildr);
            String line;
            line = br.readLine();
            line = line.split(":")[1];

            br.close();
            buildr.close();
            build.close();

            return line;
          }
            catch(Exception v) {
            return "";
          }

    }


    // TODO: Fix Me
    public void openSeg(int[] segImage, String description, int segImageHeight, int segImageWidth) {

        int npixels = segImageWidth * segImageHeight;
        if (segImage.length % npixels != 0) return;
        int nplanes = (int) Math.floor(segImage.length / npixels);

        //TODO: need to unify these, ie fix the multi-plane part
        if (nplanes > 1) {
            ImageStack stack = new ImageStack(segImageWidth, segImageHeight, nplanes);

            for (int offset = 0; offset < nplanes; offset++) {
                int[] pixels = new int[npixels];
                for (int i = 0; i < npixels; i++) {
                    pixels[i] = segImage[i + (npixels * offset)];
                }
                stack.setPixels(pixels, offset + 1);

            }
            ImagePlus img = new ImagePlus("seg", stack);
            img.show();
        } else {
            MimsPlus mp = new MimsPlus(this, segImageWidth, segImageHeight, segImage, description);
            mp.setHSIProcessor(new HSIProcessor(mp));
            boolean bShow = (mp == null);
            // find a slot to save it
            boolean bFound = false;


            bFound = true;
            segImages[0] = mp;
            int segIndex = 0;

            if (!bFound) {
                segIndex = 5;
                segImages[segIndex] = mp;
            }

            mp.addListener(this);
            bShow = true;
            if (bShow) {
                while (mp.getHSIProcessor().isRunning()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException x) {
                    }
                }
                mp.show();
            }
        }
    }

    public static String getImageHeader(Opener im) {
        //String str = "\nHeader: \n";
        //str += "Path: " + im.getImageFile().getAbsolutePath() + "/" + im.getName() + "\n";

        // WE HAVE TO DECIDE WHAT WE WANT.
        String[] names = im.getMassNames();


        String str = "\nHeader: \n";
        str += "Path: " + im.getImageFile().getAbsolutePath() + "\n";
        str += "Masses: ";
        for (int i = 0; i < im.getNMasses(); i++) {str += names[i] + " ";}
        str += "\n";
        str += "Pixels: " + im.getWidth() + "x" + im.getHeight() + "\n";
        
        str += "Raster (nm): " + im.getRaster() + "\n";
        str += "Duration (s): " + im.getDuration() + "\n";
        str += "Dwell time (ms/xy): " + im.getDwellTime() + "\n";
        str += "Stage Position: " + im.getPosition() + "\n";
        str += "Sample name: " + im.getSampleName() + "\n";
        str += "Sample date: " + im.getSampleDate() + "\n";
        str += "Sample hour: " + im.getSampleHour() + "\n";
        str += "Pixel width (nm): " + im.getPixelWidth() + "\n";
        str += "Pixel height (nm): " + im.getPixelHeight() + "\n";
        
        str += "End header.\n\n";
        return str;
    }
    
    public void updateAllImages() {
        for (int i = 0; i < maxMasses; i++) {
            if (segImages[i] != null) {
                segImages[i].updateAndDraw();
                segImages[i].killRoi();
            }
            if (massImages[i] != null) {
                massImages[i].updateAndDraw();
                massImages[i].killRoi();
            }
            if (hsiImages[i] != null) {
                hsiImages[i].updateAndDraw();
                hsiImages[i].killRoi();
            }
            if (ratioImages[i] != null) {
                ratioImages[i].updateAndDraw();
                ratioImages[i].killRoi();
            }
        }

        for (int i = 0; i < maxMasses * 2; i++) {
            if (sumImages[i] != null) {
                sumImages[i].updateAndDraw();
                sumImages[i].killRoi();
            }
        }
    }

    public void recomputeAllRatio() {
        MimsPlus[] openRatio = this.getOpenRatioImages();
        for (int i = 0; i < openRatio.length; i++) {
            openRatio[i].computeRatio();
            openRatio[i].updateAndDraw();            
        }      
        cbControl.updateHistogram();
    }

    public void recomputeAllHSI() {
        MimsPlus[] openHSI = this.getOpenHSIImages();
        for (int i = 0; i < openHSI.length; i++) {
            openHSI[i].computeHSI();
            openHSI[i].updateAndDraw();
        }        
    }                  


    public void recomputeAllComposite() {
        MimsPlus[] openComp = this.getOpenCompositeImages();
        for (int i = 0; i < openComp.length; i++) {
            openComp[i].computeComposite();
            openComp[i].updateAndDraw();
        }
    }


    public void recomputeComposite(MimsPlus img) {
        MimsPlus[] openComp = this.getOpenCompositeImages();
        for (int i = 0; i < openComp.length; i++) {
            CompositeProps props = openComp[i].compProps;
            MimsPlus[] parentImgs = props.getImages();
            for (int j = 0; j < parentImgs.length; j++) {
                if(parentImgs[j]!=null) {
                    if(img.equals(parentImgs[j])) {
                        openComp[i].computeComposite();
                        openComp[i].updateAndDraw();
                    }
                }
            }
        }
    }

    public int getHSIImageIndex(HSIProps props) {
        if (props == null) {
            return -1;
        }
        int numIndex = props.getNumMassIdx();
        int denIndex = props.getDenMassIdx();
        for (int i = 0; i < maxMasses; i++) {
            if (hsiImages[i] != null) {
                if (hsiImages[i].getHSIProps().getNumMassIdx() == numIndex && hsiImages[i].getHSIProps().getDenMassIdx() == denIndex) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Catch events such as changing the slice number of a stack
     * or drawing ROIs and if enabled,  update or synchronize all images
     * @param evt
     */
    @Override
    public synchronized void mimsStateChanged(MimsPlusEvent evt) {

        // Do not call updateStatus() here - causes a race condition..
        if (currentlyOpeningImages || bUpdating)
            return;
        bUpdating = true;

        // Sychronize stack displays.
        if (bSyncStack && evt.getAttribute() == MimsPlusEvent.ATTR_UPDATE_SLICE) {
            MimsPlus mp[] = this.getOpenMassImages();
            MimsPlus rp[] = this.getOpenRatioImages();
            MimsPlus hsi[] = this.getOpenHSIImages();
            MimsPlus sum[] = this.getOpenSumImages();
            MimsPlus comp[] = this.getOpenCompositeImages();

            // Set mass images.
            int nSlice = evt.getSlice();
            boolean updateRatioHSI = evt.getUpdateRatioHSI();
            MimsPlus mplus = evt.getMimsPlus();
            for (int i = 0; i < mp.length; i++) {
               mp[i].setSlice(nSlice);
            }                                                    

            if (!isSum) {
                if (updateRatioHSI) {
                    if (mplus == null) {

                        // Update HSI image slice.
                        for (int i = 0; i < hsi.length; i++) {
                            hsi[i].computeHSI();
                        }

                        // Update ratio images.
                        for (int i = 0; i < rp.length; i++) {
                            rp[i].computeRatio();
                        }

                        // Update composite images.
                        for (int i = 0; i < comp.length; i++) {
                            comp[i].computeComposite();
                        }
                    } else {
                        if (mplus.getMimsType()==MimsPlus.RATIO_IMAGE)
                            mplus.computeRatio();
                        else if (mplus.getMimsType()==MimsPlus.HSI_IMAGE)
                            mplus.computeHSI();
                        else if (mplus.getMimsType()==MimsPlus.COMPOSITE_IMAGE)
                            mplus.computeComposite();
                    }
                }
                // Update rois in sum images
                for (int i = 0; i < sum.length; i++) {
                    // For some reason 1 does not work... any other number does.
                    sum[i].setSlice(2);
                }
            }
            
            autocontrastAllImages();
            cbControl.updateHistogram();
            roiManager.updateSpinners();

        } else if (evt.getAttribute() == MimsPlusEvent.ATTR_SET_ROI || 
                   evt.getAttribute() == MimsPlusEvent.ATTR_MOUSE_RELEASE) {
            // Update all images with a selected ROI 
            // MOUSE_RELEASE catches drawing new ROIs             
            if (bSyncROIs) {
                int i;
                if(evt.getRoi()!= null) evt.getRoi().setStrokeColor(Color.yellow);  // needed to highlight current ROI on all images
                                                                                    // previous code did not highlight ShapeRoi objects
                MimsPlus mp = (MimsPlus) evt.getSource();
                for (i = 0; i < image.getNMasses(); i++) {
                    if (massImages[i] != mp && massImages[i] != null && bOpenMass[i]) {
                        massImages[i].setRoi(evt.getRoi());
                    }
                }
                for (i = 0; i < hsiImages.length; i++) {
                    if (hsiImages[i] != mp && hsiImages[i] != null) {
                        hsiImages[i].setRoi(evt.getRoi());
                    }
                }
                for (i = 0; i < ratioImages.length; i++) {
                    if (ratioImages[i] != mp && ratioImages[i] != null) {
                        ratioImages[i].setRoi(evt.getRoi());
                    }
                }
                for (i = 0; i < segImages.length; i++) {
                    if (segImages[i] != mp && segImages[i] != null) {
                        segImages[i].setRoi(evt.getRoi());
                    }
                }
                for (i = 0; i < sumImages.length; i++) {
                    if (sumImages[i] != mp && sumImages[i] != null) {
                        sumImages[i].setRoi(evt.getRoi());
                    }
                }
            }
            // Automatically appends a drawn ROI to the RoiManager
            // to improve work flow without extra mouse actions.             
            if (bAddROIs && evt.getAttribute() == MimsPlusEvent.ATTR_MOUSE_RELEASE) {
                ij.gui.Roi roi = evt.getRoi();
                if (roi != null && roi.getState() != Roi.CONSTRUCTING) {
                    MimsRoiManager rm = getRoiManager();
                    rm.add();
                    rm.showFrame();
                }
            }

        } else if (evt.getAttribute() == MimsPlusEvent.ATTR_ROI_MOVED) {
            MimsRoiManager rm = getRoiManager();
            rm.move();
        }

        bUpdating = false;

        // had to wait untill not changing....
        // System.out.println("mims state changed...");
        this.mimsStackEditing.resetTrueIndexLabel();
        this.mimsStackEditing.resetSpinners();
    }

   public void imageClosed(MimsPlus mp) {
            int i;
            // TODO: add switch case statement
            for (i = 0; i < sumImages.length; i++) {
               if (sumImages[i] != null)
                  if (sumImages[i].equals(mp))
                     sumImages[i] = null;
            }
            for (i = 0; i < segImages.length; i++) {
               if (segImages[i] != null)
                  if (segImages[i].equals(mp))
                     segImages[i] = null;
                
            }
            for (i = 0; i < hsiImages.length; i++) {
               if (hsiImages[i] != null)
                  if (hsiImages[i].equals(mp))
                     hsiImages[i] = null;
                
            }
            for (i = 0; i < ratioImages.length; i++) {
               if (ratioImages[i] != null)
                  if (ratioImages[i].equals(mp))
                    ratioImages[i] = null;                
            }            
   }

   // This method returns the name of the main image file without the extension.
   public String getImageFilePrefix() {
      String filename = image.getImageFile().getName().toString();
      String prefix = filename.substring(0, filename.lastIndexOf("."));
      return prefix;
   }

   public String getFilePrefix(String fileName) {
      String prefix = fileName.substring(0, fileName.lastIndexOf("."));
      return prefix;
   }
    
   private void initComponentsCustom() {
       //For non netbeans gui initialization....
       this.imgNotes = new imageNotes();
       this.imgNotes.setVisible(false);

       //hide testing
      //TestMenuItem.setVisible(false);
      
       //what is this?
/*
      // Open action.
      jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            try {
               openActionEvent(evt);
            } finally {
               setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
         }
      });
      
      // save action.
      saveMIMSjMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            try {
               saveAction(evt);
            } finally {
               setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
         }
      });
 */
   }
   
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuItem9 = new javax.swing.JMenuItem();
        jPopupMenu1 = new javax.swing.JPopupMenu();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        mainTextField = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openNewMenuItem = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        saveMIMSjMenuItem = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JSeparator();
        aboutMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        utilitiesMenu = new javax.swing.JMenu();
        imageNotesMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        sumAllMenuItem = new javax.swing.JMenuItem();
        importIMListMenuItem = new javax.swing.JMenuItem();
        captureImageMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        exportjMenu = new javax.swing.JMenu();
        exportPNGjMenuItem = new javax.swing.JMenuItem();
        exportHSI_RGBA = new javax.swing.JMenuItem();
        closeMenu = new javax.swing.JMenu();
        closeAllRatioMenuItem = new javax.swing.JMenuItem();
        closeAllHSIMenuItem = new javax.swing.JMenuItem();
        closeAllSumMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        genStackMenuItem = new javax.swing.JMenuItem();
        compositeMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        debugCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        TestMenuItem = new javax.swing.JMenuItem();

        jMenuItem9.setText("Export all images");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("NRIMS Analysis Module");
        setName("NRIMSUI"); // NOI18N

        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        jPanel1.setName("Images"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(703, 428));

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 703, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 333, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Images", jPanel1);

        mainTextField.setEditable(false);
        mainTextField.setText("Ready");
        mainTextField.setToolTipText("Status");

        fileMenu.setText("File");

        openNewMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openNewMenuItem.setMnemonic('o');
        openNewMenuItem.setText("Open MIMS Image");
        openNewMenuItem.setToolTipText("Open a MIMS image from an existing .im file.");
        openNewMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMIMSImageMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openNewMenuItem);
        openNewMenuItem.getAccessibleContext().setAccessibleDescription("Open a MIMS Image");

        jMenuItem1.setText(SAVE_IMAGE);
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMIMSjMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(jMenuItem1);

        saveMIMSjMenuItem.setText(SAVE_SESSION);
        saveMIMSjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMIMSjMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMIMSjMenuItem);
        fileMenu.add(jSeparator7);

        aboutMenuItem.setText("About OpenMIMS");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(aboutMenuItem);
        fileMenu.add(jSeparator2);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.setToolTipText("Quit the NRIMS Application.");
        exitMenuItem.setName("ExitMenuItem"); // NOI18N
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        jMenuBar1.add(fileMenu);

        editMenu.setText("Edit");

        jMenuItem3.setText("Preferences...");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        editMenu.add(jMenuItem3);

        jMenuItem4.setText("Restore MIMS");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        editMenu.add(jMenuItem4);

        jMenuBar1.add(editMenu);

        viewMenu.setText("View");

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.ALT_MASK));
        jMenuItem2.setText("Tile Windows");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        viewMenu.add(jMenuItem2);
        viewMenu.add(jSeparator8);

        jMenuBar1.add(viewMenu);

        utilitiesMenu.setText("Utilities");

        imageNotesMenuItem.setText("Image Notes");
        imageNotesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageNotesMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(imageNotesMenuItem);
        utilitiesMenu.add(jSeparator1);

        sumAllMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK));
        sumAllMenuItem.setText("Sum all Open");
        sumAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sumAllMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(sumAllMenuItem);

        importIMListMenuItem.setText("Import .im List");
        importIMListMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importIMListMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(importIMListMenuItem);

        captureImageMenuItem.setText("Capture current Image");
        captureImageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureImageMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(captureImageMenuItem);
        utilitiesMenu.add(jSeparator3);

        exportjMenu.setText("Export...");

        exportPNGjMenuItem.setText("Export All Derived (png)");
        exportPNGjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPNGjMenuItemActionPerformed(evt);
            }
        });
        exportjMenu.add(exportPNGjMenuItem);

        exportHSI_RGBA.setText("Export HSI (RGBA, QVis)");
        exportHSI_RGBA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportHSI_RGBAActionPerformed(evt);
            }
        });
        exportjMenu.add(exportHSI_RGBA);

        utilitiesMenu.add(exportjMenu);

        closeMenu.setText("Close...");

        closeAllRatioMenuItem.setText("Close All Ratio Images");
        closeAllRatioMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeAllRatioMenuItemActionPerformed(evt);
            }
        });
        closeMenu.add(closeAllRatioMenuItem);

        closeAllHSIMenuItem.setText("Close All HSI Images");
        closeAllHSIMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeAllHSIMenuItemActionPerformed(evt);
            }
        });
        closeMenu.add(closeAllHSIMenuItem);

        closeAllSumMenuItem.setText("Close All Sum Images");
        closeAllSumMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeAllSumMenuItemActionPerformed(evt);
            }
        });
        closeMenu.add(closeAllSumMenuItem);

        utilitiesMenu.add(closeMenu);
        utilitiesMenu.add(jSeparator4);

        genStackMenuItem.setText("Generate Stack");
        genStackMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genStackMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(genStackMenuItem);

        compositeMenuItem.setText("Composite");
        compositeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compositeMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(compositeMenuItem);
        utilitiesMenu.add(jSeparator5);

        debugCheckBoxMenuItem.setText("Debug");
        debugCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugCheckBoxMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(debugCheckBoxMenuItem);

        TestMenuItem.setText("Test");
        TestMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TestMenuItemActionPerformed(evt);
            }
        });
        utilitiesMenu.add(TestMenuItem);

        jMenuBar1.add(utilitiesMenu);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, mainTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                .add(18, 18, 18)
                .add(mainTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(28, 28, 28))
        );

        getAccessibleContext().setAccessibleDescription("NRIMS Analyais Module");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * restores any closed or modified massImages
     */
    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {                                           
        
        int currentSlice = massImages[0].getCurrentSlice();

        mimsStackEditing.uncompressPlanes();

        // concatenate the remaining files.
        int x = mimsAction.getSize();
        for (int i = 1; i <= mimsAction.getSize(); i++) {
           massImages[0].setSlice(i);
           if (mimsAction.isDropped(i)) mimsStackEditing.insertSlice(i);
        }
        mimsStackEditing.untrack();

        mimsStackEditing.resetTrueIndexLabel();
        mimsStackEditing.resetSpinners();

        massImages[0].setSlice(currentSlice);
    }

    public void setIsSum(boolean set) {
        isSum = set;
    }

    public boolean getIsSum() {
        return isSum;
    }

    public void setIsWindow(boolean set) {
        isWindow = set;
    }

    public boolean getIsWindow() {
        return isWindow;
    }

    public void setWindowRange(int range) {
        windowRange = range;
    }

    public int getWindowRange() {
        return windowRange;
    }
    
    public void setMedianFilterRatios(boolean set) {
        medianFilterRatios = set;
    }

    public boolean getMedianFilterRatios() {
        return medianFilterRatios;
    }

    public void autocontrastAllImages() {       
       // All mass images             
       MimsPlus mp[] = getOpenMassImages();
        for (int i = 0; i < mp.length; i++) {
            if (mp[i].getAutoContrastAdjust())
               autoContrastImage(mp[i]);
        }
        
        // All ratio images
        MimsPlus rp[] = getOpenRatioImages();
        for (int i = 0; i < rp.length; i++) {
           if (rp[i].getAutoContrastAdjust())
              autoContrastImage(rp[i]);
        }                
    }      

   public void autoContrastImages(MimsPlus[] imgs) {
       for(int i=0; i<imgs.length; i++) {
           if(imgs[i]!=null) {
               autoContrastImage(imgs[i]);
           }
       }
   }
   
   public void autoContrastImage(MimsPlus img) {                 
      ContrastAdjuster ca = new ContrastAdjuster(img);
      ca.doAutoAdjust = true;
      ca.doUpdate(img);
   }

    public void logException(Exception e) {
        IJ.log(e.toString());

        StackTraceElement[] trace = e.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            IJ.log(trace[i].toString());
        }
    }


    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
      
       if(this.prefs==null) { prefs = new PrefFrame(); }
       prefs.showFrame();
        
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {                                           
        ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
        wo.run("tile");
    }

   // Saves a session.
   private void saveSession(String fileName, boolean saveImageOnly) {

      // Initialize variables.
      File file = new File(fileName);
      String directory = file.getParent();
      String name = file.getName();
      FileOutputStream f_out;
      ObjectOutputStream obj_out;
      String objectFileName;

      try {

        this.getOpener().setNotes(imgNotes.getOutputFormatedText());
        // Save the original .im file to a new file of the .nrrd file type.
        String nrrdFileName = name;
        if (!name.endsWith(NRRD_EXTENSION))
           nrrdFileName = name+NRRD_EXTENSION;        

        ImagePlus[] imp = getOpenMassImages();
		  if (imp == null) return;
		  Nrrd_Writer nw = new Nrrd_Writer(this);
        File dataFile = nw.save(imp, directory, nrrdFileName);

        if (saveImageOnly)
           return;

        // Get the base of the file name.
        String baseFileName = getFilePrefix(dataFile.getAbsolutePath());

        // Save the ROI files to zip.
        String roisFileName = baseFileName+ROIS_EXTENSION;
        Roi[] rois = getRoiManager().getAllROIs();
        if (rois.length > 0)
           getRoiManager().saveMultiple(rois, roisFileName, false);

        // Contruct a unique name for each ratio image and save.
        MimsPlus ratio[] = getOpenRatioImages();
        if (ratio.length > 0){
        for (int i = 0; i < ratio.length; i++) {
           RatioProps ratioprops = ratio[i].getRatioProps();
           ratioprops.setDataFileName(dataFile.getName());
           int numIndex = ratioprops.getNumMassIdx();
           int denIndex = ratioprops.getDenMassIdx();
           int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
           int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
           objectFileName = baseFileName + "_m" + numMass + "_m" + denMass + RATIO_EXTENSION;
           f_out = new FileOutputStream(objectFileName);
           obj_out = new ObjectOutputStream(f_out);
           obj_out.writeObject(ratioprops);
        }}

        // Contruct a unique name for each hsi image and save.
        MimsPlus hsi[] = getOpenHSIImages();
        if (hsi.length > 0){
        for (int i = 0; i < hsi.length; i++) {
           HSIProps hsiprops = hsi[i].getHSIProps();
           hsiprops.setDataFileName(dataFile.getName());
           int numIndex = hsiprops.getNumMassIdx();
           int denIndex = hsiprops.getDenMassIdx();
           int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
           int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
           objectFileName = baseFileName + "_m" + numMass + "_m" + denMass + HSI_EXTENSION;
           f_out = new FileOutputStream(objectFileName);
           obj_out = new ObjectOutputStream(f_out);
           obj_out.writeObject(hsiprops);
        }}

        // Contruct a unique name for each sum image and save.
        MimsPlus sum[] = getOpenSumImages();
        if (sum.length > 0){
        for (int i = 0; i < sum.length; i++) {
           SumProps sumProps = sum[i].getSumProps();
           sumProps.setDataFileName(dataFile.getName());
           if (sumProps.getSumType() == SumProps.RATIO_IMAGE) {
               int numIndex = sumProps.getNumMassIdx();
               int denIndex = sumProps.getDenMassIdx();
               int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
               int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
               objectFileName = baseFileName + "_m" + numMass + "_m" + denMass + SUM_EXTENSION;
           } else if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
              int parentIndex = sumProps.getParentMassIdx();
              int parentMass = Math.round(new Float(getOpener().getMassNames()[parentIndex]));
              objectFileName = baseFileName + "_m" + parentMass + SUM_EXTENSION;
           } else {
              continue;
           }
           f_out = new FileOutputStream(objectFileName);
           obj_out = new ObjectOutputStream(f_out);
           obj_out.writeObject(sumProps);
        }}

        //getmimsAction().writeAction(new File(baseFileName+ACT_EXTIONSION));

      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

    private void openMIMSImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                                      
       
       // Get HSIProps for all open ratio images.
       RatioProps[] rto_props = getOpenRatioProps();
       
       // Get HSIProps for all open hsi images.     
       HSIProps[] hsi_props = getOpenHSIProps();
       
       // Get SumProps for all open sum images.    
       SumProps[] sum_props = getOpenSumProps();
             
       // Load the new file.
       loadMIMSFile();
       
       //Autocontrast mass images.
       //Should add option to apply settings from previous image?
       autoContrastImages(getOpenMassImages());

       // Generate all images that were previously open.
       restoreState(rto_props, hsi_props, sum_props);

        MimsRoiManager rm = getRoiManager();
        if( rm!=null ) {
            rm.resetRoiLocationsLength();
        }


       // Keep the HSIView GUI up to date.
       if (medianFilterRatios) {
           hsiControl.setIsMedianFiltered(true);
           hsiControl.setMedianFilterRadius(medianFilterRadius);
       }
       if (isSum) {
           hsiControl.setIsSum(true);
       }
       if (isWindow) {
           hsiControl.setIsWindow(true);
           hsiControl.setWindowRange(windowRange);
       }

        //Update notes gui
        if(image!=null) {
            imgNotes.setOutputFormatedText(image.getNotes());
        }
}                                                     

    public void restoreState( RatioProps[] rto_props,  HSIProps[] hsi_props, SumProps[] sum_props){

       MimsPlus mp;
       // Generate ratio images.
       for (int i=0; i<rto_props.length; i++){
          if (closeEnough(rto_props[i].getNumMassIdx(), rto_props[i].getNumMassValue()) &&
              closeEnough(rto_props[i].getDenMassIdx(), rto_props[i].getDenMassValue())) {
             mp = new MimsPlus(this, rto_props[i]);
             mp.showWindow();
             mp.setDisplayRange(rto_props[i].getMinLUT(), rto_props[i].getMaxLUT());
          }
       }
       
       // Generate hsi images.
       for (int i=0; i<hsi_props.length; i++){
          if (closeEnough(hsi_props[i].getNumMassIdx(), hsi_props[i].getNumMassValue()) &&
              closeEnough(hsi_props[i].getDenMassIdx(), hsi_props[i].getDenMassValue())) {
              HSIProps tempprops = hsi_props[i].clone();
              mp = new MimsPlus(this, hsi_props[i]);
             mp.showWindow();
             mp.getHSIProcessor().setProps(tempprops);
             mp.hsiProps = tempprops;
          }
       }

       // Generate sum images.
       for (int i=0; i<sum_props.length; i++){
          if (sum_props[i].getSumType() == MimsPlus.RATIO_IMAGE) {
              //boolean foo = closeEnough(sum_props[i].getNumMassIdx(), sum_props[i].getNumMassValue()) && closeEnough(sum_props[i].getDenMassIdx(), sum_props[i].getDenMassValue());
             if (closeEnough(sum_props[i].getNumMassIdx(), sum_props[i].getNumMassValue()) &&
                 closeEnough(sum_props[i].getDenMassIdx(), sum_props[i].getDenMassValue())) {
                mp = new MimsPlus(this, sum_props[i], null);
                mp.showWindow();
                mp.setDisplayRange(sum_props[i].getMinLUT(), sum_props[i].getMaxLUT());
             }
          } else if (sum_props[i].getSumType() == MimsPlus.MASS_IMAGE) {
             if (closeEnough(sum_props[i].getParentMassIdx(), sum_props[i].getParentMassValue())) {
                mp = new MimsPlus(this, sum_props[i], null);
                mp.showWindow();
                mp.setDisplayRange(sum_props[i].getMinLUT(), sum_props[i].getMaxLUT());
             }
          }
       }
       
    }

    String getExportName(MimsPlus img) {
        String name = "";
        name += this.getImageFilePrefix();

        if(img.getMimsType()==img.MASS_IMAGE) {
            int index = img.getMassIndex();
            int mass = Math.round(new Float(getOpener().getMassNames()[index]));
            name += "_m" + mass;
            return name;
        }

        if(img.getMimsType()==img.RATIO_IMAGE) {
            RatioProps ratioprops = img.getRatioProps();
            int numIndex = ratioprops.getNumMassIdx();
            int denIndex = ratioprops.getDenMassIdx();
            int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
            int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
            name += "_m" + numMass + "_m" + denMass + "_ratio";
            return name;
        }

        if(img.getMimsType()==img.HSI_IMAGE) {
            HSIProps hsiprops = img.getHSIProps();

           int numIndex = hsiprops.getNumMassIdx();
           int denIndex = hsiprops.getDenMassIdx();
           int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
           int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
           name += "_m" + numMass + "_m" + denMass + "_hsi";
           return name;
        }

        if (img.getMimsType() == img.SUM_IMAGE) {
            SumProps sumProps = img.getSumProps();
            if (sumProps.getSumType() == SumProps.RATIO_IMAGE) {
                int numIndex = sumProps.getNumMassIdx();
                int denIndex = sumProps.getDenMassIdx();
                int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
                int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
                name += "_m" + numMass + "_m" + denMass + "_sum";
                return name;
            } else if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
                int parentIndex = sumProps.getParentMassIdx();
                int parentMass = Math.round(new Float(getOpener().getMassNames()[parentIndex]));
                name += "_m" + parentMass + "_sum";
                return name;
            }

        }

        if (img.getMimsType() == img.SEG_IMAGE) {
            name += "_seg";
            return name;
        }

        return name;
    }


    
    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        //tab focus changed
        //reset tomography info
        if (this.mimsTomography != null) {
            this.mimsTomography.resetImageNamesList();
        }

        if (this.mimsStackEditing != null) {
            this.mimsStackEditing.resetTrueIndexLabel();
            this.mimsStackEditing.resetSpinners();
        }

    }//GEN-LAST:event_jTabbedPane1StateChanged

private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
    //System.exit(0);
    //TODO doesn't actually close...
    //deleteTempActionFile();
    this.close();
}//GEN-LAST:event_exitMenuItemActionPerformed

private void sumAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sumAllMenuItemActionPerformed

    SumProps sumProps;
    MimsPlus[] openmass = this.getOpenMassImages();
    MimsPlus[] openratio = this.getOpenRatioImages();

    //clear all sum images
    for (int i = 0; i < maxMasses * 2; i++) {
        if (sumImages[i] != null) {
            sumImages[i].close();
            sumImages[i] = null;
        }
    }

    // Open a sum image for each mass image.
    for (int i = 0; i < openmass.length; i++) {
        sumProps = new SumProps(openmass[i].getMassIndex());
        MimsPlus mp = new MimsPlus(this, sumProps, null);
        mp.showWindow();
    }

    // open a sum image for each ratio image.
    for (int i = 0; i < openratio.length; i++) {
        sumProps = new SumProps(openratio[i].getRatioProps().getNumMassIdx(), openratio[i].getRatioProps().getDenMassIdx());
        sumProps.setRatioScaleFactor(openratio[i].getRatioProps().getRatioScaleFactor());
        MimsPlus mp = new MimsPlus(this, sumProps, null);
        mp.showWindow();
    }
}//GEN-LAST:event_sumAllMenuItemActionPerformed

private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed

    String message = "OpenMIMS v0.8 Revision: " + this.revisionNumber + "\n\n";
    message += "OpenMIMS was developed at NRIMS, the National Resource \n";
    message += "for Imaging Mass Spectrometry. \n";
    message += "http://www.nrims.hms.harvard.edu/ \n";
    message += "\nDeveloped by:\n Doug Benson, Collin Poczatek \n ";
    message += "Boris Epstein, Philipp Gormanns\n Stefan Reckow, ";
    message += "Rob Gonzales,\n Zeke Kaufman.";
    message += "\n\nOpenMIMS has modified, uses, or depends upon: \n";
    message += "    TurboReg:  http://bigwww.epfl.ch/thevenaz/turboreg/ \n";
    message += "    libSVM: http://www.csie.ntu.edu.tw/~cjlin/libsvm/ \n";
    message += "    NRRD file format: http://teem.sourceforge.net/nrrd/ \n";
    message += "    nrrd plugins: http://flybrain.stanford.edu/nrrd \n";
    message += "    jFreeChart:  http://www.jfree.org/jfreechart/ \n";
    message += "    FileDrop:  http://iharder.sourceforge.net/current/java/filedrop/ \n";
    message += "\nPlease cite OpenMIMS or any of the \n";
    message += "above projects when applicable. \n";
    
    javax.swing.JFrame frame = new javax.swing.JFrame("About OpenMIMS");
    frame.setSize(400, 300);

    javax.swing.JScrollPane scroll = new javax.swing.JScrollPane();
    frame.add(scroll);
    javax.swing.JTextArea area = new javax.swing.JTextArea();
    area.setEditable(false);
    area.append(message);

    area.setColumns(20);
    area.setRows(5);
    
    scroll.setViewportView(area);
    int x = java.awt.MouseInfo.getPointerInfo().getLocation().x;
    int y =java.awt.MouseInfo.getPointerInfo().getLocation().y;
    frame.setLocation(x,y);
    frame.setVisible(true);

}//GEN-LAST:event_aboutMenuItemActionPerformed

private void captureImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureImageMenuItemActionPerformed
    //testing trying to grab screen pixels from image for rois and annotations

    // Captures the active image window and returns it as an ImagePlus.
    ImagePlus imp = ij.WindowManager.getCurrentImage();
    if (imp == null) {
        IJ.noImage();
        return;
    }

    int p = 0;
    try {
        MimsPlus mp = (MimsPlus) imp;
        if(mp.getMimsType()==MimsPlus.MASS_IMAGE) {
            p = mp.getCurrentSlice();
        }
    } catch (Exception e) {
    }


    ImagePlus imp2 = null;
    try {
        ImageWindow win = imp.getWindow();
        if (win == null) {
            return;
        }
        win.toFront();
        Point loc = win.getLocation();
        ImageCanvas ic = win.getCanvas();
        ic.update(ic.getGraphics());

        Rectangle bounds = ic.getBounds();
        loc.x += bounds.x;
        loc.y += bounds.y;
        Rectangle r = new Rectangle(loc.x, loc.y, bounds.width, bounds.height);
        Robot robot = new Robot();
        robot.delay(100);
        Image img = robot.createScreenCapture(r);
        if (img != null) {
            imp2 = new ImagePlus("Grab of " + imp.getTitle(), img);
            imp2.show();
        }

        //autosave in working directory
        File file = image.getImageFile();

        String dir = file.getParent() + file.separator;
        ij.io.FileSaver saver = new ij.io.FileSaver(imp2);
        String name = imp2.getTitle().replaceAll(" : ", "_");
        name = name.replaceAll(" ", "_");
        name = name.replaceAll("/", "_");
        //double escapte for literal '\'
        name = name.replaceAll("\\\\", "_");
        if(p!=0) {
            name = name + "_p" + p;
        }
        name = name + ".png";

        int n = 1;
        while(new java.io.File(dir+name).exists()) {
            name = name.substring(0, name.length()-4);
            name = name + "_" + n + ".png";
        }

        saver.saveAsPng(dir + name);

    } catch (Exception e) {
        logException(e);
    }

}//GEN-LAST:event_captureImageMenuItemActionPerformed

private void importIMListMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importIMListMenuItemActionPerformed
    com.nrims.data.LoadImageList testLoad = new com.nrims.data.LoadImageList(this);
    boolean read;
    read = testLoad.openList();
    if (!read) {
        return;
    }

    testLoad.printList();
    testLoad.simpleIMImport();
    //this.mimsStackEditing.setConcatGUI(true);
}//GEN-LAST:event_importIMListMenuItemActionPerformed
                                                                              

private void genStackMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_genStackMenuItemActionPerformed

    MimsPlus img;
    //grab current window and try to cast
    try {
        img = (MimsPlus) ij.WindowManager.getCurrentImage();
    } catch (ClassCastException e) {
        //if it's some random image and we can't cast just return
        return;
    }

    generateStack(img);

}//GEN-LAST:event_genStackMenuItemActionPerformed
                                                                                         

private void TestMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TestMenuItemActionPerformed

/*
    MimsRoiManager rm = this.getRoiManager();
    Roi[] rois = rm.getAllROIs();
    ij.gui.ShapeRoi[] shaperois = new ij.gui.ShapeRoi[rois.length];

    for(int i=0; i< rois.length; i++) {
        shaperois[i] = new ij.gui.ShapeRoi(rois[i]);
    }

    for(int i=0; i< shaperois.length; i++) {
        for(int j=0; j< shaperois.length; j++) {
            if(i!=j) {
                ij.gui.ShapeRoi intersection = (ij.gui.ShapeRoi)shaperois[i].clone();
                intersection.not(shaperois[j]);
                rm.add(intersection);
            }
        }
    }
*/

  /*
    MimsRoiManager rm = this.getRoiManager();
    HashMap h = rm.getRoiLocations();

    for (Object key : h.keySet()) {
        ArrayList<Integer[]> xylist = (ArrayList<Integer[]>) h.get(key);
        System.out.println("roi: "+key+" -> ");
        for(int i = 0; i<xylist.size(); i++) {
            System.out.println(xylist.get(i)[0]+","+xylist.get(i)[1]);
        }
    }
*/
     
    //Poking around for roi locations exception

    MimsRoiManager rm = getRoiManager();
    String name = rm.getSelectedROIs()[0].getName();
    HashMap locations = rm.getRoiLocations();
    ArrayList pos = (ArrayList)locations.get(name);

    System.out.println("Roi: " + name);
    System.out.println("locations size : " + locations.size());
    System.out.println("pos size: " + pos.size());
    

     //Random exception testing
    /*
    try{
    int a=0;
    int b =1;
    System.out.println("a: " + a + " b: " + b + " a/b: " + (a/b) + " b/a: " + (b/a) );
    }catch(Exception e) {
        System.out.println(e.getLocalizedMessage());
        System.out.println(e.toString());

        StackTraceElement[] foo = e.getStackTrace();
        for(int i=0; i<foo.length; i++) {
            System.out.println("foo["+i+"]");
            System.out.println(foo[i].toString());
        }
    }
    */
/*     try {
        MimsRoiManager rm = getRoiManager();
        if (rm != null) {
            Roi[] rois = rm.getSelectedROIs();
            boolean foo = rm.roiOverlap(rois[0], rois[1]);
            System.out.println("overlap = " + foo);
        }
    } catch (Exception e) {
    }
*/
    /*String n = this.getOpener().getNotes();
    n = n + " !Notes! ";
    this.getOpener().setNotes(n);*/

     /*
     this.imgNotes.setVisible(true);
     String s = imgNotes.getNoteText();
     
     System.out.println("original\n: "+s);
     System.out.println("--------------------------");

     String s2 = s.replaceAll("\n", "&/&/&");
     System.out.println("encoded\n: "+s2);
     System.out.println("--------------------------");

     String s3 = s2.replaceAll("&/&/&", "\n");
     System.out.println("decoded :\n" + s3);
     System.out.println("--------------------------");
     
     c = c+1;
     

     this.imgNotes.setVisible(true);
     String s = imgNotes.getNoteText() + "blah\r\f\r\fblah";
     String s2 = s.replaceAll("(\r)|(\f)", "\n");
     System.out.println(s2);
     */
     /*
     String s = "mailto:foo@bar.com";
     s +="?subject=Test Subject";
     s += "?body=I am on the meryygoround.... ";
     try{
     ij.plugin.BrowserLauncher.openURL(s);
     } catch(Exception e) {}
     */

    //batch convert to nrrd test
    /*
    try {
        // User sets file prefix name
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        if (lastFolder != null) {
            fc.setCurrentDirectory(new java.io.File(lastFolder));
        }

        int returnVal = fc.showSaveDialog(jTabbedPane1);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = fc.getSelectedFiles();
            System.out.println("Num files: "+files.length);

            for(int i = 0; i < files.length; i++) {
                loadMIMSFile(files[i]);
                //for(int j = 0; j<1000000; j++) {}

                System.out.println("File: "+files[i].getAbsolutePath());

                int planes = this.getOpener().getNImages();
                this.getmimsStackEditing().compressPlanes(planes);
                String newfile = files[i].getParent()+File.separator+this.getImageFilePrefix()+NRRD_EXTENSION;
                System.out.println("New File: "+newfile);
                saveSession(newfile,true);
            }

        }

    } catch (Exception e) {
        e.printStackTrace();
    }
    */

}//GEN-LAST:event_TestMenuItemActionPerformed

private void saveMIMSjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMIMSjMenuItemActionPerformed

     String fileName;
             try {
                 // User sets file prefix name
                 JFileChooser fc = new JFileChooser();
                 if (lastFolder != null) {
                     fc.setCurrentDirectory(new java.io.File(lastFolder));
                 }
                 if( this.getImageFilePrefix() != null ) {
                    fc.setSelectedFile(new java.io.File(this.getImageFilePrefix()+NRRD_EXTENSION));
                 }
                 int returnVal = fc.showSaveDialog(jTabbedPane1);
                 if (returnVal == JFileChooser.APPROVE_OPTION) {
                     fileName = fc.getSelectedFile().getAbsolutePath();
                     File file  = new File(fileName);
                     if (file.exists()){
                        int n = JOptionPane.showConfirmDialog(
                                this,
                                "File already exists.\n" + file.getAbsolutePath() + "\n" + "Overwrite?\n",
                                "Warning",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (n == JOptionPane.NO_OPTION)
                           return;
                     }
                     lastFolder = file.getParent();
                     setIJDefaultDir(lastFolder);
                     setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                     // Determine if we save just the mass images or the entire session.
                     // (e.g. Mass images, Rois, HSIs, ratio image)
                     boolean saveImageOnly = true;
                     if (evt.getActionCommand().equals(SAVE_SESSION))
                        saveImageOnly = false;
                     saveSession(fileName, saveImageOnly);
                 } else {
                     return;
                 }
             } catch(Exception e) {
                 ij.IJ.error("Save Error", "Error saving file.");
             } finally {
                 setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
             }

}//GEN-LAST:event_saveMIMSjMenuItemActionPerformed

private void closeAllRatioMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllRatioMenuItemActionPerformed

    for (int i = 0; i < ratioImages.length; i++) {
        if (ratioImages[i] != null) {
            ratioImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllRatioMenuItemActionPerformed

private void closeAllHSIMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllHSIMenuItemActionPerformed

    for (int i = 0; i < hsiImages.length; i++) {
        if (hsiImages[i] != null) {
            hsiImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllHSIMenuItemActionPerformed

private void closeAllSumMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllSumMenuItemActionPerformed

    for (int i = 0; i < sumImages.length; i++) {
        if (sumImages[i] != null) {
            sumImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllSumMenuItemActionPerformed

private void exportPNGjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPNGjMenuItemActionPerformed

    File file = image.getImageFile();
    System.out.println(file.getParent()+file.separator);
    String dir = file.getParent()+file.separator;

    MimsPlus[] sum = getOpenSumImages();
    for( int i = 0; i < sum.length; i ++) {
        ImagePlus img = (ImagePlus)sum[i];
        ij.io.FileSaver saver = new ij.io.FileSaver(img);
        String name = getExportName(sum[i]) + ".png";
        saver.saveAsPng(dir+name);
    }

    MimsPlus[] hsi = getOpenHSIImages();
    for( int i = 0; i < hsi.length; i ++) {
        ImagePlus img = (ImagePlus)hsi[i];
        ij.io.FileSaver saver = new ij.io.FileSaver(img);
        String name = getExportName(hsi[i]) + ".png";
        saver.saveAsPng(dir+name);
    }

    MimsPlus[] ratios = getOpenRatioImages();
    for( int i = 0; i < ratios.length; i ++) {
        ImagePlus img = (ImagePlus)ratios[i];
        ij.io.FileSaver saver = new ij.io.FileSaver(img);
        String name = getExportName(ratios[i]) + ".png";
        saver.saveAsPng(dir+name);
    }


}//GEN-LAST:event_exportPNGjMenuItemActionPerformed

private void imageNotesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageNotesMenuItemActionPerformed
    this.imgNotes.setVisible(true);
}//GEN-LAST:event_imageNotesMenuItemActionPerformed

private void compositeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compositeMenuItemActionPerformed

    cbControl.showCompositeManager();
   /*
    MimsPlus[] foo = getOpenMassImages();
    MimsPlus[] imgs = {foo[0],foo[1],foo[2]};
    //?
    CompositeProps props = new CompositeProps(imgs);
    MimsPlus comp = new MimsPlus(this, props);
    this.compImages[0] = comp;
    comp.show();
    */
}//GEN-LAST:event_compositeMenuItemActionPerformed

private void debugCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugCheckBoxMenuItemActionPerformed
    this.bDebug = debugCheckBoxMenuItem.isSelected();
    //System.out.println(this.bDebug);
}//GEN-LAST:event_debugCheckBoxMenuItemActionPerformed

private void exportHSI_RGBAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportHSI_RGBAActionPerformed
    // Testing, to be moved to a manager class

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
    MimsPlus denimg = this.getMassImage(props.getDenMassIdx());
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
    int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
    int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));

    java.io.FileOutputStream out = null;
    String dir = this.getImageDir();
    String fileprefix = this.getImageFilePrefix();
    fileprefix += "_m"+numMass+"m"+denMass+"_rgba";

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
        out = new java.io.FileOutputStream(dir+fileprefix+".raw");
    } catch (Exception e) {
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
        double denGain = denMax > denMin ? 255.0 / ( denMax - denMin ) : 1.0 ;
        double denSpan = (denMax-denMin);


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
            /*
            double alpha = java.lang.Math.max(denpix[i], 800);
            alpha = java.lang.Math.min(alpha, 8000);
            alpha = alpha / (8000 - 800);
            plane_rgba[i][3] = (byte) (255 * alpha);
            */


            double scaled = (denpix[i] - denMin)/denSpan;
            //System.out.println(denOut+ " ");
            int outValue = (int) ((double) scaled* rgbGain);
            //System.out.println("rgbgain: "+rgbGain);
            //System.out.println(outValue+ " ");
            if (outValue < 0) {
                outValue = 0;
            } else if (outValue > 255) {
                outValue = 255;
            }

            plane_rgba[i][3] = (byte) (outValue);
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
        //write training data
        bw = new java.io.BufferedWriter(new java.io.FileWriter(new java.io.File(dir + fileprefix + ".dat")));
        /*
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
        bw.write("Resolution: " + x + " " + y + " "+ z + "\n");
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


    ImagePlus iplus = new ImagePlus("test", new ColorProcessor(256, 256, foo));
    iplus.show();
    
}//GEN-LAST:event_exportHSI_RGBAActionPerformed

   // Method for saving action file and writing backup action files.
   public void saveAction(java.awt.event.ActionEvent evt) {

      // Initialize variables.
      File selectedFile = null;

      // Query user where to save action file.
      JFileChooser fc = new JFileChooser(lastFolder);
      String fname = this.getImageFilePrefix();
      fname = fname + ACT_EXTIONSION;
      fc.setSelectedFile(new File(fname));      

      while (true) {
         if (fc.showSaveDialog(this) == JFileChooser.CANCEL_OPTION) {
            return;
         }
         selectedFile = fc.getSelectedFile();

         // Update ImageJs the 'lastFolder' variable.
         lastFolder = selectedFile.getParent();
         setIJDefaultDir(lastFolder);

         // Check for overwriting any existing file.
         String actionFile = selectedFile.getName();
         if (selectedFile.exists()) {
            String[] options = {"Overwrite", "Cancel"};
            int value = JOptionPane.showOptionDialog(this, "File \"" + actionFile + "\" already exists!", null,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (value == JOptionPane.NO_OPTION) {
               return;
            }

         }
         getmimsAction().writeAction(selectedFile);
         break;
      }
   }

   // Open action file.
   private void openActionEvent(java.awt.event.ActionEvent evt) {
      
      // Open JFileChooser and allow user to select action file.
      JFileChooser fc = new JFileChooser();
      if (lastFolder != null) {
         fc.setCurrentDirectory(new java.io.File(lastFolder));
      }
      fc.setPreferredSize(new java.awt.Dimension(650, 500));
      if (fc.showOpenDialog(this) == JFileChooser.CANCEL_OPTION) {
         return;
      }            
      File actionFile = fc.getSelectedFile();
      lastFolder = fc.getSelectedFile().getParent();
      setIJDefaultDir(lastFolder);
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      
      openAction(actionFile);
   }
      
   public void openAction(File actionFile) {   
            
      // initialize variables
      int trueIndex = 1;
      ArrayList<Integer> deleteList = new ArrayList<Integer>();                             

      // more variable assignment and initialization.
      currentlyOpeningImages = true;
      bUpdating = true;      

      try {

         BufferedReader br = new BufferedReader(new FileReader(actionFile));

         // Read action file, and perform actions              
         String line;
         LinkedList actionRowList = new LinkedList();
         LinkedList fileList = new LinkedList();
         while ((line = br.readLine()) != null) {

            // Parse line.
            if (line.equals("")) {
               break;
            }
            String[] row = line.split("\t");

            // Add the actions to the actionList                 
            actionRowList.add(row);

            // Add files to the fileList (only if not already contained).
            if (!fileList.contains(row[5])) {
               fileList.add(row[5]);
            }
         }

         // Now extract all relevant .im files from zip.
         boolean firstImage = true;
         for (int i = 0; i < fileList.size(); i++) {

            // Make sure its exists in the current directory.
            String fileName = ((String) fileList.get(i));
            File imageFile = new File(actionFile.getParent(), fileName);
            if (!imageFile.exists()) {
               System.out.println("can not find image file " + fileName + " in current directory");
               return;
            }

            // Read main image file.
            if (firstImage) {
               loadMIMSFile(imageFile);
            } else {
               UI tempui = new UI(imageFile.getAbsolutePath());
               mimsStackEditing.concatImages(false, false, tempui);
               openers.put(imageFile.getName(), tempui.getOpener());
               for (MimsPlus img : tempui.getMassImages()) {
                  if (img != null) {
                     img.setAllowClose(true);
                     img.close();
                  }
               }
            }

            firstImage = false;
         }

         // Loop over the action row list and perform actions.
         for (int i = 0; i < actionRowList.size(); i++) {

            // Get the action row.
            String[] actionRowString = (String[]) actionRowList.get(i);

            // Get the display index that corresponds to the true index.
            int displayIndex = mimsAction.displayIndex(trueIndex);
            for (int j = 0; j < image.getNMasses(); j++) {
               massImages[j].setSlice(displayIndex);
            }

            // Set the XShift, YShift, dropped val, and image name for this slice.
            mimsStackEditing.XShiftSlice(displayIndex, Double.parseDouble(actionRowString[1]));
            mimsStackEditing.YShiftSlice(displayIndex, Double.parseDouble(actionRowString[2]));
            if (Integer.parseInt(actionRowString[3]) == 1) {
               deleteList.add(trueIndex);
            }
            mimsAction.setSliceImage(displayIndex, new String(actionRowString[5]));

            trueIndex++;
         }
         mimsStackEditing.removeSliceList(deleteList);

      // TODO we need more refined Exception checking here
      } catch (Exception e) {}

      // Set all images to the first slice.
      for (int j = 0; j < image.getNMasses(); j++) {
         massImages[j].setSlice(1);
      }

      bUpdating = false;
      currentlyOpeningImages = false;
   }

//generates a new ImagePlus that's a stack from a ratio or hsi
public void generateStack(MimsPlus img) {
    //do a few checks
    if(img==null)
        return;
    
    //need some reference image that's a stack
    if(this.massImages[0]==null)
        return;

    ImagePlus refimp = this.massImages[0];
    int currentslice = refimp.getSlice();

    //return is there's no stack
    if(refimp.getStackSize()==1)
        return;
    //return if it's not a computed image, ie ratio/hsi
    if( !(img.getMimsType()==MimsPlus.RATIO_IMAGE || img.getMimsType()==MimsPlus.HSI_IMAGE) )
        return;

    ij.ImageStack stack = img.getStack();
    java.awt.image.ColorModel cm = stack.getColorModel();
    ij.ImageStack ims = new ij.ImageStack(stack.getWidth(), stack.getHeight(), cm);
    int numImages = refimp.getStackSize();

    for (int i = 1; i <= numImages; i++) {
        refimp.setSlice(i);
        if(img.getMimsType()==MimsPlus.HSI_IMAGE)
        while(img.getHSIProcessor().isRunning()){}
        
        ims.addSlice(refimp.getStack().getSliceLabel(i), img.getProcessor().duplicate());
    }

    // Create new image
    ImagePlus newimp = new ImagePlus("Stack : "+img.getTitle(), ims);
    newimp.setCalibration(img.getCalibration());

    // Display this new stack
    newimp.show();
    newimp.setSlice(currentslice);
    refimp.setSlice(currentslice);

}


public void updateLineProfile(double[] newdata, String name, int width) {
    if(this.lineProfile==null) {
        return;
    } else {
        lineProfile.updateData(newdata, name, width);
    }
}

    // Returns an instance of the RoiManager.
    public MimsRoiManager getRoiManager() {
        roiManager = MimsRoiManager.getInstance();
        if (roiManager == null) {
            roiManager = new MimsRoiManager(this, image);
        }
        return roiManager;
    }
            
    String getImageDir() {
        //won't work on windows?
        String path = image.getImageFile().getAbsolutePath();
        path = path.substring(0, path.lastIndexOf("/")+1);
        return path;
    }
    
    
    public MimsPlus[] getMassImages() {
        return massImages;
    }

    public MimsPlus getMassImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return massImages[i];
        }
        return null;
    }
    
    // Determine if mass value for a given index (i)
    // is nearly the same as (d).
    public boolean closeEnough(int i, double d) {
       double mass = getMassValue(i);
       double q = d/mass;
       if (q > 0.9 && q < 1.1)
          return true;
       else 
          return false;
    }

    public double getMassValue(int i) {
       double mass = -1.0;
       try {
          mass = new Double(getOpener().getMassNames()[i]);
       } catch (Exception e) {}
       return mass;
    }

    public MimsPlus[] getHSIImages() {
        return hsiImages;
    }

    public MimsPlus getHSIImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return hsiImages[i];
        }
        return null;
    }

    public MimsPlus getRatioImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return ratioImages[i];
        }
        return null;
    }

    public MimsPlus[] getSumImages() {
        return sumImages;
    }

    public MimsPlus getSumImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return sumImages[i];
        }
        return null;
    }

    // Returns only the open mass images as an array.
    public MimsPlus[] getOpenMassImages() {
        int i, nOpen = 0;
        for (i = 0; i < massImages.length; i++) {
            if (massImages[i] != null && bOpenMass[i]) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0        , nOpen = 0; i < massImages.length; i++) {
            if (massImages[i] != null && bOpenMass[i]) {
                mp[nOpen++] = massImages[i];
            }
        }
        return mp;
    }

    public MimsPlus[] getOpenRatioImages() {
        int i, nOpen = 0;
        for (i = 0; i < maxMasses; i++) {
            if (ratioImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0 , nOpen = 0; i < maxMasses; i++) {
            if (ratioImages[i] != null) {
                mp[nOpen++] = ratioImages[i];
            }
        }
        return mp;
    }


    public MimsPlus[] getOpenCompositeImages() {
        int i, nOpen = 0;
        for (i = 0; i < maxMasses; i++) {
            if (compImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0        , nOpen = 0; i < maxMasses; i++) {
            if (compImages[i] != null) {
                mp[nOpen++] = compImages[i];
            }
        }
        return mp;
    }


    public MimsPlus[] getOpenHSIImages() {
        int i, nOpen = 0;
        for (i = 0; i < maxMasses; i++) {
            if (hsiImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0        , nOpen = 0; i < maxMasses; i++) {
            if (hsiImages[i] != null) {
                mp[nOpen++] = hsiImages[i];
            }
        }
        return mp;
    }

    public MimsPlus[] getOpenSegImages() {
        int i, nOpen = 0;
        for (i = 0; i < maxMasses; i++) {
            if (segImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0, nOpen = 0; i < maxMasses; i++) {
            if (segImages[i] != null) {
                mp[nOpen++] = segImages[i];
            }
        }
        return mp;
    }
    
    public MimsPlus[] getOpenSumImages() {
        int i, nOpen = 0;
        for (i = 0; i < 2*maxMasses; i++) {
            if (sumImages[i] != null) {
                nOpen++;
            }
        }
        MimsPlus[] mp = new MimsPlus[nOpen];
        if (nOpen == 0) {
            return mp;
        }
        for (i = 0, nOpen = 0; i < 2*maxMasses; i++) {
            if (sumImages[i] != null) {
                mp[nOpen++] = sumImages[i];
            }
        }
        return mp;
    }

    // Get HSIProps for all open HSI images.
    public HSIProps[] getOpenHSIProps() {
       MimsPlus[] hsi = getOpenHSIImages();
       HSIProps[] hsi_props = new HSIProps[hsi.length];
       for (int i=0; i<hsi.length; i++){
          hsi_props[i] = hsi[i].getHSIProps();
          hsi_props[i].setXWindowLocation(hsi[i].getWindow().getX());
          hsi_props[i].setYWindowLocation(hsi[i].getWindow().getY());
          hsi_props[i].setNumMassValue(getMassValue(hsi_props[i].getNumMassIdx()));
          hsi_props[i].setDenMassValue(getMassValue(hsi_props[i].getDenMassIdx()));

          //should this be set inside getprops?
          //maybe...
          hsi_props[i].setMag(hsi[i].getCanvas().getMagnification());
       }
       return hsi_props;
    }

    // Get RatioProps for all open Ratio images.
    public RatioProps[] getOpenRatioProps() {
       MimsPlus[] rto = getOpenRatioImages();
       RatioProps[] rto_props = new RatioProps[rto.length];
       for (int i=0; i<rto.length; i++){
          rto_props[i] = rto[i].getRatioProps();
          rto_props[i].setXWindowLocation(rto[i].getWindow().getX());
          rto_props[i].setYWindowLocation(rto[i].getWindow().getY());
          rto_props[i].setNumMassValue(getMassValue(rto_props[i].getNumMassIdx()));
          rto_props[i].setDenMassValue(getMassValue(rto_props[i].getDenMassIdx()));

          //should these be set inside getprops?
          //maybe...
          rto_props[i].setMinLUT(rto[i].getDisplayRangeMin());
          rto_props[i].setMaxLUT(rto[i].getDisplayRangeMax());

          rto_props[i].setMag(rto[i].getCanvas().getMagnification());
       }
       return rto_props;
    }

    // Get SumProps for all open Sum images.
    public SumProps[] getOpenSumProps(){
       MimsPlus[] sum = getOpenSumImages();
       SumProps[] sum_props = new SumProps[sum.length];
       for (int i=0; i<sum.length; i++){
          sum_props[i] = sum[i].getSumProps();
          sum_props[i].setXWindowLocation(sum[i].getWindow().getX());
          sum_props[i].setYWindowLocation(sum[i].getWindow().getY());
          if (sum_props[i].getSumType() == SumProps.RATIO_IMAGE) {
             sum_props[i].setNumMassValue(getMassValue(sum_props[i].getNumMassIdx()));
             sum_props[i].setDenMassValue(getMassValue(sum_props[i].getDenMassIdx()));
          } else if (sum_props[i].getSumType() == SumProps.MASS_IMAGE) {
             sum_props[i].setParentMassValue(getMassValue(sum_props[i].getParentMassIdx()));
          }

          //should these be set inside getprops?
          //maybe...
          sum_props[i].setMinLUT(sum[i].getDisplayRangeMin());
          sum_props[i].setMaxLUT(sum[i].getDisplayRangeMax());

          sum_props[i].setMag(sum[i].getCanvas().getMagnification());
       }
       return sum_props;
    }

    public MimsPlus getImageByName(String name) {
        MimsPlus mp = null;
        MimsPlus[] tempimages;

        // Mass images.
        tempimages = getOpenMassImages();        
        for(int i=0; i<tempimages.length; i++){
            if(name.equals(tempimages[i].getTitle())) {
                return tempimages[i];
            }
        }

        // Ratio images.
        tempimages = getOpenRatioImages();        
        for(int i=0; i<tempimages.length; i++){
            if(name.equals(tempimages[i].getTitle())) {
                return tempimages[i];
            }
        }

        // Hsi images.
        tempimages = getOpenHSIImages();
        for(int i=0; i<tempimages.length; i++){
            if(name.equals(tempimages[i].getTitle())) {
                return tempimages[i];
            }
        }

        // Sum images.
        tempimages = getOpenSumImages();
        for(int i=0; i<tempimages.length; i++){
            if(name.equals(tempimages[i].getTitle())) {
                return tempimages[i];
            }
        }

        return mp;
    }
   
    public void setSyncStack(boolean bSync) {
        bSyncStack = bSync;
    }

    public boolean getSyncStack() {
        return bSyncStack;
    }

    public void setSyncROIs(boolean bSync) {
        bSyncROIs = bSync;
    }

    public boolean getSyncROIs() {
        return bSyncROIs;
    }
    
    public void setSyncROIsAcrossPlanes(boolean bSync) {
        bSyncROIsAcrossPlanes = bSync;
    }

    public boolean getSyncROIsAcrossPlanes() {
        return bSyncROIsAcrossPlanes;
    }

    public void setAddROIs(boolean bOnOff) {
        bAddROIs = bOnOff;
    }

    public boolean getAddROIs() {
        return bAddROIs;
    }

    public HSIView getHSIView() {
        return hsiControl;
    }

    public MimsData getMimsData() {
        return mimsData;
    }

    public MimsLog getmimsLog() {
        return mimsLog;
    }
    
    public MimsCBControl getCBControl(){
       return cbControl;
    }

    public MimsStackEditing getmimsStackEditing() {
        return mimsStackEditing;
    }

    public MimsTomography getmimsTomography() {
        return mimsTomography;
    }

    public MimsAction getmimsAction() {
        return mimsAction;
    }

    public boolean isOpening() {
        return currentlyOpeningImages;
    }

    public void setUpdating(boolean bool) {
        bUpdating = bool;
    }
    
    public boolean isUpdating() {
        return bUpdating;
    }

    public PrefFrame getPreferences() {
        return prefs;
    }

    public Opener getOpener() {
        return image;
    }   
    
    public Opener getFromOpenerList(String name){
        Object op = openers.get(name);
        return (Opener)openers.get(name);
    }
    
    public void addToOpenerList(String fileName, Opener opener) {
       openers.put(fileName, opener);
    }

    public void setActiveMimsPlus(MimsPlus mp) {
        if (( mp.getMimsType() != MimsPlus.HSI_IMAGE ) && (mp.getMimsType() != MimsPlus.RATIO_IMAGE) ) {
            return;
        }
        if (mp.getMimsType() == MimsPlus.HSI_IMAGE) {
            int j = getHSIImageIndex(mp.getHSIProps());
            if (j > -1 && j < maxMasses && hsiImages[j].getHSIProps() != null) {
                hsiControl.setProps(hsiImages[j].getHSIProcessor().getHSIProps());
                hsiControl.setImageLabel(hsiImages[j].title);
                hsiControl.setCurrentImage(mp);
            }
        } else if (mp.getMimsType() == MimsPlus.RATIO_IMAGE) {
            int ni = mp.getRatioProps().getNumMassIdx();
            int di = mp.getRatioProps().getDenMassIdx();
            int j = getRatioImageIndex(ni,di);
            if (j > -1 && j < maxMasses && ratioImages[j].getRatioProps() != null) {
                hsiControl.setProps(ratioImages[j].getRatioProps());
                hsiControl.setImageLabel(ratioImages[j].title);
                hsiControl.setCurrentImage(mp);
            }
        }
    }    

    public synchronized void updateStatus(String msg) {
        if (bUpdating) {
            return; // Don't run from other threads...
        } // Don't run from other threads...
        if (!currentlyOpeningImages) {
            mainTextField.setText(msg);
        } else {
            IJ.showStatus(msg);
        }
        if (bDebug) {
            IJ.log(msg);
        }
    }
    
    public int getRatioScaleFactor() {
        return this.ratioScaleFactor;
    }
    
    public int setRatioScaleFactor(int s) {
        this.ratioScaleFactor = s;
        return this.ratioScaleFactor;
    }

    public double getMedianFilterRadius() {
        return this.medianFilterRadius;
    }

    public double setMedianFilterRadius(double r) {
        this.medianFilterRadius = r;
        return this.medianFilterRadius;
    }

    public String getLastFolder() {
        return lastFolder;
    }

    public void setIJDefaultDir(String dir) {
        ij.io.OpenDialog temp = new ij.io.OpenDialog("", "fubar");
        temp.setDefaultDirectory(dir);
        temp = null;
    }

    @Override
    public void run(String cmd) {
        if (cmd.equalsIgnoreCase("open")) {
            super.run(cmd);
        } else {
            super.run("");
        }
        setVisible(true);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        Boolean skip_next = false;
        im_file_path = null;

        for (int i = 0; i < args.length; i++) {
            System.out.println("Arg " + i + " " + args[i]);

            if (!skip_next) {
                if (args[i].startsWith("-ijpath") && i + 1 < args.length) {
                    //Prefs.setHomeDir(args[i+1]);
                    skip_next = true;
                }
                
                if ( (args[i].equals(IMFILE_OPTION)) &&
                       i + 1 < args.length )
                {
                    im_file_path = args[i+1];
                    skip_next = true;
                }

            } else
                skip_next = false;
        }

        if (im_file_path != null) {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    System.out.println("Ui.run called");
                    String temp_path = im_file_path;
                    File[] files_arr = new File[1];
                    UI ui_to_run = new UI(null);
                    ui_to_run.setVisible(true);
                    files_arr[0] = new File(temp_path);
                    ui_to_run.openFiles(files_arr);
                }
            });
        } else {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    System.out.println("Ui.run called");
                    UI ui_to_run = new UI(null);
                    ui_to_run.setVisible(true);
                }
            });
        }

        
    }

    public boolean getDebug() {
        return bDebug;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem TestMenuItem;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem captureImageMenuItem;
    private javax.swing.JMenuItem closeAllHSIMenuItem;
    private javax.swing.JMenuItem closeAllRatioMenuItem;
    private javax.swing.JMenuItem closeAllSumMenuItem;
    private javax.swing.JMenu closeMenu;
    private javax.swing.JMenuItem compositeMenuItem;
    private javax.swing.JCheckBoxMenuItem debugCheckBoxMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportHSI_RGBA;
    private javax.swing.JMenuItem exportPNGjMenuItem;
    private javax.swing.JMenu exportjMenu;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem genStackMenuItem;
    private javax.swing.JMenuItem imageNotesMenuItem;
    private javax.swing.JMenuItem importIMListMenuItem;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField mainTextField;
    private javax.swing.JMenuItem openNewMenuItem;
    private javax.swing.JMenuItem saveMIMSjMenuItem;
    private javax.swing.JMenuItem sumAllMenuItem;
    private javax.swing.JMenu utilitiesMenu;
    private javax.swing.JMenu viewMenu;
    // End of variables declaration//GEN-END:variables

   private File extractFromZipfile(ZipFile zipFile, ZipEntry zipEntry, File destinationFile) {
                  
      // If no destination specified, use temp directory.
      if (destinationFile == null) {
         File destinationDir = new File(System.getProperty("java.io.tmpdir"));
         if (!destinationDir.canRead() || !destinationDir.canWrite())
            return null;
         destinationFile = new File(destinationDir, zipEntry.getName());
      }
                                             
      try {
         // Create input and output streams.
         byte[] buf = new byte[2048]; int n;
         BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
         FileOutputStream fos = new FileOutputStream(destinationFile);
         BufferedOutputStream bos = new BufferedOutputStream(fos, buf.length);
         
         // Write the file.         
         while ((n = bis.read(buf, 0, buf.length)) != -1) {
            bos.write(buf, 0, n);
         }
         
         // Close all streams.
         bos.flush();
         bos.close();
         fos.close();
         bis.close();
                  
      } catch (Exception e) {e.printStackTrace();}      
      
      destinationFile.deleteOnExit();
      return destinationFile;
   }
}
