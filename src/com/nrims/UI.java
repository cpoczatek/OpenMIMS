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
import ij.gui.Roi;
import ij.gui.ImageWindow;
import ij.gui.ImageCanvas;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Image;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The main user interface of the NRIMS ImageJ plugin.
 * A multitabbed window with a file menu, the UI class
 * serves as the central hub for all classes involved
 * with the user interface.
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
    
    private boolean bSyncStack = true;
    private boolean bUpdating = false;    
    private boolean currentlyOpeningImages = false;
    private boolean bCloseOldWindows = true;
    private boolean medianFilterRatios = false;
    private boolean isSum = false;
    private boolean isWindow = false;
    private int     windowRange = -1;
    private boolean isPercentTurnover = false;
    private boolean isRatio = true;
    private boolean[] bOpenMass = new boolean[maxMasses];
            
    private String lastFolder = null;      
    public  File   tempActionFile;                        
    private HashMap openers = new HashMap();

    private MimsPlus[] massImages = new MimsPlus[maxMasses];
    private MimsPlus[] ratioImages = new MimsPlus[maxMasses];
    private MimsPlus[] hsiImages = new MimsPlus[maxMasses];
    private MimsPlus[] segImages = new MimsPlus[maxMasses];
    private MimsPlus[] sumImages = new MimsPlus[2 * maxMasses];
    private MimsPlus[] compImages = new MimsPlus[2 * maxMasses];

    private MimsData mimsData = null;
    private MimsLog mimsLog = null;   
    private MimsCBControl cbControl = new MimsCBControl(this);
    private MimsStackEditor mimsStackEditing = null;
    private MimsRoiManager roiManager = null;
    private MimsTomography mimsTomography = null;        
    private MimsHSIView hsiControl = null;
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

    /** Creates a new instance of the OpenMIMS analysis interface.*/
    public UI() {
        super("NRIMS Analysis Module");
      
      System.out.println("Ui constructor");
      System.out.println(System.getProperty("java.version") + " : " + System.getProperty("java.vendor"));

      revisionNumber = extractRevisionNumber();

      /*
      // Set look and feel to native OS - this has been giving us issues
      // as getting the "SystemLookAndFeel" sometimes results is very
      // different layouts for the HSIView tab. Leave commentoutr out for now.
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         SwingUtilities.updateComponentTreeUI(this);
      } catch (Exception e) {
         IJ.log("Error setting native Look and Feel:\n" + e.toString());
      }
      */

      initComponents();
      initComponentsCustom();
      
      //read in preferences so values are gettable
      //by various tabs (ie mimsTomography, HSIView, etc.
      //when constructed further down.
      prefs = new PrefFrame(this);

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
      IJ.showProgress(1.0);

      this.mimsDrop = new FileDrop(null, jTabbedPane1, new FileDrop.Listener() {
         public void filesDropped(File[] files) {

            if (files.length > 1) {
               IJ.error("Please drag no more than one file.");
               return;
            }

            File file = files[0];

            // Get HSIProps for all open ratio images.
            RatioProps[] rto_props = getOpenRatioProps();

            // Get HSIProps for all open hsi images.
            HSIProps[] hsi_props = getOpenHSIProps();

            // Get SumProps for all open sum images.
            SumProps[] sum_props = getOpenSumProps();

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            boolean opened = openFile(file);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (opened == false)
               return;
           
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

      // Create and start the thread
      //Thread thread = new StartupScript(this);
      //thread.start();
      //loadMIMSFile(new File("/nrims/home3/zkaufman/Images/test_file.nrrd"));

      // I added this listener because for some reason,
      // neither the windowClosing() method in this class, nor the
      // windowClosing method in PlugInJFrame is registering.
      addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent winEvt) {
            closeCurrentImage();
            close();
         }
      });
   }

    /**
     * Insertion status of the current MimsPlus object
     * @param mp object to be inserted.
     * @return success/failure of insertion.
     */
   public boolean addToImagesList(MimsPlus mp) {
      int i = 0; int ii = 0; boolean inserted=false;
      while (i < maxMasses) {
         if (mp.getMimsType() == MimsPlus.RATIO_IMAGE && ratioImages[i] == null) {
            inserted = true;
            ratioImages[i] = mp;
            getCBControl().addWindowtoList(mp);
            getmimsTomography().resetImageNamesList();
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
            this.mimsTomography.resetImageNamesList();
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
     * Closes the current image and its associated set of
     * windows if the mode is set to close open windows.
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
    public synchronized File loadMIMSFile() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setMultiSelectionEnabled(false);
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
            return null;
        }
        lastFolder = fc.getSelectedFile().getParent();
        setIJDefaultDir(lastFolder);

        File file = fc.getSelectedFile();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        boolean opened = openFile(file);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        if (opened)
           return file;
        else
           return null;
    }

    /**
     * Relegates behavior for opening any of the various MimsPlus file types.
     * @param file to be opened.
     */
    public boolean openFile(File file) {
      
      boolean onlyShowDraggedFile = true;

      String fileName = file.getName();
      if (fileName.endsWith(NRRD_EXTENSION) || fileName.endsWith(MIMS_EXTENSION) ||
              fileName.endsWith(RATIO_EXTENSION) || fileName.endsWith(HSI_EXTENSION) ||
              fileName.endsWith(SUM_EXTENSION) || fileName.endsWith(ROIS_EXTENSION) ||
              fileName.endsWith(ROI_EXTENSION)) {
      } else {
         String fileType;
         int lastIndexOf = fileName.lastIndexOf(".");
         if (lastIndexOf >= 0)
            fileType = fileName.substring(fileName.lastIndexOf("."));
          else
            fileType = fileName;
         IJ.error("Unable to open files of type: " + fileType);
         return false;
      }


      lastFolder = file.getParent();
      setIJDefaultDir(lastFolder);

      try {
         if (file.getAbsolutePath().endsWith(NRRD_EXTENSION) ||
                 file.getAbsolutePath().endsWith(MIMS_EXTENSION)) {
            onlyShowDraggedFile = false;
            loadMIMSFile(file);
         } else if (file.getAbsolutePath().endsWith(RATIO_EXTENSION)) {
            FileInputStream f_in = new FileInputStream(file);
            ObjectInputStream obj_in = new ObjectInputStream(f_in);
            Object obj = obj_in.readObject();
            if (obj instanceof RatioProps) {
               RatioProps ratioprops = (RatioProps) obj;
               String dataFileString = ratioprops.getDataFileName();
               File dataFile = new File(file.getParent(), dataFileString);
               loadMIMSFile(dataFile);
               MimsPlus mp = new MimsPlus(this, ratioprops);
               mp.showWindow();
            }
         } else if (file.getAbsolutePath().endsWith(HSI_EXTENSION)) {
            FileInputStream f_in = new FileInputStream(file);
            ObjectInputStream obj_in = new ObjectInputStream(f_in);
            Object obj = obj_in.readObject();
            if (obj instanceof HSIProps) {
               HSIProps hsiprops = (HSIProps) obj;
               String dataFileString = hsiprops.getDataFileName();
               File dataFile = new File(file.getParent(), dataFileString);
               loadMIMSFile(dataFile);
               MimsPlus mp = new MimsPlus(this, hsiprops);
               mp.showWindow();
            }
         } else if (file.getAbsolutePath().endsWith(SUM_EXTENSION)) {
            FileInputStream f_in = new FileInputStream(file);
            ObjectInputStream obj_in = new ObjectInputStream(f_in);
            Object obj = obj_in.readObject();
            if (obj instanceof SumProps) {
               SumProps sumprops = (SumProps) obj;
               String dataFileString = sumprops.getDataFileName();
               File dataFile = new File(file.getParent(), dataFileString);
               loadMIMSFile(dataFile);
               MimsPlus sp = new MimsPlus(this, sumprops, null);
               sp.showWindow();
            }
         } else if (file.getAbsolutePath().endsWith(ROIS_EXTENSION) ||
                 file.getAbsolutePath().endsWith(ROI_EXTENSION)) {
            onlyShowDraggedFile = false;
            getRoiManager().open(file.getAbsolutePath(), true);
            updateAllImages();
            getRoiManager().showFrame();
         }
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }

       // This 3 lines solves some strange updating issues.
       // Has to do with syncing images by changing the slice.
       int startslice = massImages[0].getCurrentSlice();
       massImages[0].setSlice(getOpener().getNImages());
       massImages[0].setSlice(startslice);

       if (onlyShowDraggedFile) {
          MimsPlus[] mps = getOpenMassImages();
          for (int i = 0; i < mps.length; i++) {
             mps[i].hide();
          }
       }

       return true;
   }

    /**
     * Opens an image file in the .im or .nrrd file format.
     * @param file absolute file path.
     * @throws java.lang.NullPointerException
     */
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
            } catch (Exception e) {
                IJ.error("Failed to open " + file + "\n");
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
                    ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
                    wo.run("tile");
                }
                if (this.windowZooms != null) {
                   applyWindowZooms(windowZooms);
                }

            } catch (Exception x) {
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
                hsiControl = new MimsHSIView(this);
                mimsLog = new MimsLog();
                mimsStackEditing = new MimsStackEditor(this, image);
                mimsTomography = new MimsTomography(this);
                mimsAction = new MimsAction(image);
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
                mimsStackEditing = new MimsStackEditor(this, image);
                mimsTomography = new MimsTomography(this);
                mimsAction = new MimsAction(image);
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

    /**
     * Returns the mass images window positions as an array of Points.
     *
     * @return the mass images window positions.
     */
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

    /**
     * Returns an array of int describing if certain mass
     * images are visible or hidden.
     *
     * @return an array describing if mass images are visible (1) or hidden (0).
     */
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

    /**
     * Returns the mass images window magnifications.
     *
     * @return the mass images window magnifications.
     */
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

    /**
     * Opens the mass image windows at the positions
     * specified in the <code>positions</code> array.
     *
     * @param positions an array of points.
     */
    public void applyWindowPositions(Point[] positions) {
        for(int i = 0; i < positions.length; i++) {
           if ( positions[i] != null && massImages[i] != null)
               if (massImages[i].getWindow() != null)
                massImages[i].getWindow().setLocation(positions[i]);

        }
    }

    /**
     * Keeps visible or hides the mass images based
     * on the contents of the <code>hidden</code> array.
     *
     * @param hidden an array of ints: (1) equals visible, (0) equals hidden.
     */
    public void applyHiddenWindows(int[] hidden) {
        for(int i = 0; i < hidden.length; i++) {
           if ( massImages[i] != null)
               if( hidden[i] == 1)
                    massImages[i].hide();
        }
    }

    /**
     * Applies the magnification factor stored in
     * the <code>zoom</code> array to the mass images.
     *
     * @param zooms an array of zoom values.
     */
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

    /**
     * Resets the "view" menu item to reflect the
     * mass images in the current data file.
     */
    private void resetViewMenu() {
        int c=0;
        for(int i = 0; i< windowPositions.length; i++) {
            if(windowPositions[i]!=null) c++;
        }

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

    /** Initializes the view menu.*/
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

    /** Action method for the "view" menu items. */
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
                massImages[index].show();
                if(windowPositions[index] != null && (windowPositions[index].x > 0 && windowPositions[index].y > 0))
                    massImages[index].getWindow().setLocation(windowPositions[index]);
                massImages[index].setbIgnoreClose(true);
            } else if( !viewMassMenuItems[index].isSelected() && massImages[index].isVisible()) {
                windowPositions[index] = massImages[index].getWindow().getLocation();
                massImages[index].hide();
            }
        }

        System.out.print(evt.getActionCommand() + " index: " + index);
        System.out.print(" selected: " + viewMassMenuItems[index].isSelected());
        System.out.print(" visable: " + massImages[index].isVisible() + "\n");
    }

    /** The behavior for "closing" mass images, when closing is not allowed.*/
    public void massImageClosed(MimsPlus im) {
        if(windowPositions==null) windowPositions = gatherWindowPosistions();
        for (int i = 0; i < massImages.length; i++) {
            if (massImages[i] != null) {
                if (massImages[i].equals(im)) {
                    windowPositions[i] = im.getXYLoc();
                    viewMassMenuItems[i].setSelected(false);
                }
            }
        }
    }

    /**
     * Returns the index of the ratio image with numerator
     * <code>numIndex</code> and denominator <code>denIndex</code>.
     *
     * @param numIndex numerator mass index.
     * @param denIndex denominator mass index.
     * @return index of ratio image, -1 of none exists.
     */
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

    /**
     * Returns the index of the HSI image with numerator
     * <code>numIndex</code> and denominator <code>denIndex</code>.
     *
     * @param numIndex numerator mass index.
     * @param denIndex denominator mass index.
     * @return index of ratio image, -1 of none exists.
     */
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

    /**
     * Extracts the revision number from the file <code>buildnum.txt</code>
     *
     * @return a string containing the build number.
     */
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
    /**
     * Opens a segmented image.
     *
     * @param segImage the data array (width x height x numPlanes).
     * @param description a simple description to be used as a title.
     * @param segImageHeight the height of the image in pixels.
     * @param segImageWidth the width of the image in pixels.
     */
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

    /**
     * Updates all images and kills any active
     * ROIs that might be on those images.
     */
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

    /**
     * Recomputes all ratio images. This needs to be done
     * whenever an action is performed that changes the
     * underlying data of the ratio image. For example,
     * using a median filter.
     */
    public void recomputeAllRatio() {
        MimsPlus[] openRatio = this.getOpenRatioImages();
        for (int i = 0; i < openRatio.length; i++) {
            openRatio[i].computeRatio();
            openRatio[i].updateAndDraw();            
        }      
        cbControl.updateHistogram();
    }

    /**
     * Recomputes all HSI images. This needs to be done
     * whenever an action is performed that changes the
     * underlying data of the HSI image. For example,
     * using a median filter.
     */
    public void recomputeAllHSI() {
        MimsPlus[] openHSI = this.getOpenHSIImages();
        for (int i = 0; i < openHSI.length; i++) {
            openHSI[i].computeHSI();
            openHSI[i].updateAndDraw();
        }        
    }                  

    /**
     * Recomputes a composite image.
     * @param img the parent image.
     */
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

    /**
     * Catch events such as changing the slice number of a stack
     * or drawing ROIs and if enabled,  update or synchronize all images.
     *
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
            // Automatically appends a drawn ROI to the RoiManager
            // to improve work flow without extra mouse actions.             
            if (evt.getAttribute() == MimsPlusEvent.ATTR_MOUSE_RELEASE) {
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

    /**
     * Determines the bahavior when an image is closed. Essentially
     * a bookkeepping method that sets certain member variable to
     * null when corresponding window is closed.
     *
     * @param mp the image being closed.
     */
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

   /**
    * Returns the prefix of the current data files name.
    * For example: test_file. im = test_file
    *
    * @return prefix file name.
    */
   public String getImageFilePrefix() {
      String filename = image.getImageFile().getName().toString();
      String prefix = filename.substring(0, filename.lastIndexOf("."));
      return prefix;
   }

   /**
    * Returns the prefix of any file name.
    * For example: /tmp/test_file.im = /tmp/test_file
    *
    * @return prefix file name.
    */
   public String getFilePrefix(String fileName) {
      String prefix = fileName.substring(0, fileName.lastIndexOf("."));
      return prefix;
   }

   /** Custom actions to be called AFTER initComponent. */
   private void initComponentsCustom() {
       this.imgNotes = new imageNotes();
       this.imgNotes.setVisible(false);
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
      jSeparator6 = new javax.swing.JSeparator();
      jMenuItem5 = new javax.swing.JMenuItem();
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
      closeMenu = new javax.swing.JMenu();
      closeAllRatioMenuItem = new javax.swing.JMenuItem();
      closeAllHSIMenuItem = new javax.swing.JMenuItem();
      closeAllSumMenuItem = new javax.swing.JMenuItem();
      jSeparator4 = new javax.swing.JSeparator();
      genStackMenuItem = new javax.swing.JMenuItem();
      compositeMenuItem = new javax.swing.JMenuItem();

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
         .add(0, 726, Short.MAX_VALUE)
      );
      jPanel1Layout.setVerticalGroup(
         jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(0, 437, Short.MAX_VALUE)
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
      viewMenu.add(jSeparator6);

      jMenuItem5.setText("Roi Manager");
      jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jMenuItem5ActionPerformed(evt);
         }
      });
      viewMenu.add(jMenuItem5);
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

      jMenuBar1.add(utilitiesMenu);

      setJMenuBar(jMenuBar1);

      org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(layout.createSequentialGroup()
            .addContainerGap()
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 731, Short.MAX_VALUE)
               .add(org.jdesktop.layout.GroupLayout.TRAILING, mainTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 731, Short.MAX_VALUE))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
            .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 463, Short.MAX_VALUE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(mainTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
      );

      getAccessibleContext().setAccessibleDescription("NRIMS Analyais Module");

      pack();
   }// </editor-fold>//GEN-END:initComponents

    /**
     * Restores the image to its unmodified state. Undoes
     * and compression, reinserts all deleted planes, and
     * sets all translations back to zero.
     */
    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {                                           
        
        int currentSlice = massImages[0].getCurrentSlice();

        mimsStackEditing.uncompressPlanes();

        // concatenate the remaining files.
        for (int i = 1; i <= mimsAction.getSize(); i++) {
           massImages[0].setSlice(i);
           if (mimsAction.isDropped(i)) mimsStackEditing.insertSlice(i);
        }
        mimsStackEditing.untrack();

        mimsStackEditing.resetTrueIndexLabel();
        mimsStackEditing.resetSpinners();

        massImages[0].setSlice(currentSlice);
    }

    /**
     * Sets isRatio to true, meaning data shown in ratio
     * images are ratio values and NOT percent turnover.
     *
     * @param selected <code>true</code> if generating
     * ratio images with ratio values, otherwise <code>false</code>.
     */
    public void setIsRatio(boolean selected) {
       isRatio = selected;
    }

    /**
     * Returns <code>true</code> if generating ratio images
     * with ratio values and NOT percent turnover.
     *
     * @return boolean.
     */
    public boolean getIsRatio(){
       return isRatio;
    }

    /**
     * Sets isPercentTurnover to true, meaning data shown
     * in ratio images are percent turnover values and NOT
     * ratio values.
     *
     * @param selected <code>true</code> if generating
     * ratio images with percent turnover values, otherwise
     * <code>false</code>.
     */
    public void setIsPercentTurnover(boolean selected) {
       isPercentTurnover = selected;
    }

    /**
     * Returns <code>true</code> if generating ratio images
     * with percent turnover values and NOT ratio values.
     *
     * @return boolean.
     */
    public boolean getIsPercentTurnover(){
       return isPercentTurnover;
    }

    /**
     * Sets the isSum member variable to <code>set</code> telling
     * the application if ratio and HSI images should be generated
     * as sum images, instead of plane by plane.
     *
     * @param set <code>true</code> if generating ratio image
     * and HSI images as sum images, otherwise false.
     */
    public void setIsSum(boolean set) {
        isSum = set;
    }

    /**
     * Returns <code>true</code> if the application is generating ratio and
     * HSI images as sum images.
     *
     * @return <code>true</code> if generating ratio images and
     * HSI images as sum images, otherwise <code>false</code>.
     */
    public boolean getIsSum() {
        return isSum;
    }

    /**
     * Sets the isWindow member vairable to <code>set</code> telling
     * the application if ratio and HSI images should be generated
     * with a window of n planes.
     *
     * @param set <code>true</code> if generating ratio and HSI
     * images with a window of <code>n</code> planes.
     */
    public void setIsWindow(boolean set) {
        isWindow = set;
    }

    /**
     * Returns <code>true</code> if the application is generating
     * ratio and HSI images with a window of <code>n</code> planes.
     *
     * @return <code>true</code> if generating ratio and HSi images
     * with a window of <code>n</code> planes, otherwise <code>false</code>.
     */
    public boolean getIsWindow() {
        return isWindow;
    }

    /**
     * Used to set the number of planes to when generating ratio
     * and HSI images with a sliding window. Actual window size
     * will be 2 times window range plus 1 (The current window is
     * currentplane - windowSize up to currentplane + windowSize).
     *
     * @param range the "radius" of the sliding window.
     */
    public void setWindowRange(int range) {
        windowRange = range;
    }

    /**
     * Returns the size of the sliding window.
     *
     * @return size of sliding window.
     */
    public int getWindowRange() {
        return windowRange;
    }

    /**
     * Tells the application to use the median filter
     * when generating ratio and HSI images. All open
     * ratio and HSI images will be regenerated.
     *
     * @param set <code>true</code> if computing ratio
     * and HSI images with a median filter, othewise <code>false</code>.
     */
    public void setMedianFilterRatios(boolean set) {
        medianFilterRatios = set;
    }

    /**
     * Returns <code>true</code> if the application is using
     * a median filter when generating ratio and HSI images.
     *
     * @return <code>true</code> if using a median filter, otherwise <code>false</code>.
     */
    public boolean getMedianFilterRatios() {
        return medianFilterRatios;
    }

    /**
     * Will perform and autocontrast on all open mass, ratio and sum images.
     */
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

        // All sum images
        MimsPlus sp[] = getOpenSumImages();
        for (int i = 0; i < sp.length; i++) {
           if (sp[i].getAutoContrastAdjust())
              autoContrastImage(sp[i]);
        }
    }      

    /**
     * Autocontrasts an images according to ImageJ's autocontrasting algorithm.
     *
     * @param imgs an array of images to autocontrast.
     */
   public void autoContrastImages(MimsPlus[] imgs) {
       for(int i=0; i<imgs.length; i++) {
           if(imgs[i]!=null) {
               autoContrastImage(imgs[i]);
           }
       }
   }

   /**
    * Autocontrast an image according to ImageJ's autocontrasting algorithm.
    *
    * @param img an image to autocontrast.
    */
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

    /** An action method for the Edit>Preferences... menu item.*/
    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
      
       if(this.prefs==null) { prefs = new PrefFrame(this); }
       prefs.showFrame();
        
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    /** Action method for the View>Tile Windows menu item.*/
    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {                                           
        ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
        wo.run("tile");
    }

    /**
     * Saves the current analysis session. If <code>saveImageOnly</code> is
     * set to <code>true</code> than only the mass image data will be save
     * to a file with name <code>fileName</code> (always ending with .nrrd).
     * If <code>saveImageOnly</code> is set to <code>false</code> than
     * mass image data will be save ALONG with a roi zip file containing
     * all ROIs, all ratio, HSI and sum images. All files will have the same
     * prefix name, but will slightly differ in their endings as well as
     * their extensions.
     *
     * @param fileName the name of the file to be saved.
     * @param saveImageOnly <code>true</code> if saving only the mass
     * image data, <code>false</code> if saving Rois and derived images.
     */
    public void saveSession(String fileName, boolean saveImageOnly) {

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

    /**
     * Action method for File>Open Mims Image menu item. If opening
     */
    private void openMIMSImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {                                                      
       
       // Get HSIProps for all open ratio images.
       RatioProps[] rto_props = getOpenRatioProps();
       
       // Get HSIProps for all open hsi images.     
       HSIProps[] hsi_props = getOpenHSIProps();
       
       // Get SumProps for all open sum images.    
       SumProps[] sum_props = getOpenSumProps();
             
       // Load the new file.
       File file = loadMIMSFile();
       if (file == null)
          return;

       // Restores state if image file.
       if( file.getAbsolutePath().endsWith(NRRD_EXTENSION) ||
           file.getAbsolutePath().endsWith(MIMS_EXTENSION) ) {
                  restoreState(rto_props, hsi_props, sum_props);
       }
       
       //Autocontrast mass images.
       //Should add option to apply settings from previous image?
       autoContrastImages(getOpenMassImages());

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

    /**
     * Regenerates the ratio images, hsi images and sum images with
     * the properties specified in the arrays. Used when opening
     * a new data file and the user wishes to generate all the same
     * derived images that were open with the previous data file.
     *
     * @param rto_props array of <code>RatioProps</code> objects.
     * @param hsi_props array of <code>HSIProps</code> objects.
     * @param sum_props array of <code>SumProps</code> objects.
     */
    public void restoreState( RatioProps[] rto_props,  HSIProps[] hsi_props, SumProps[] sum_props){

       if (rto_props == null)
          rto_props = new RatioProps[0];
       
       if (hsi_props == null)
          hsi_props = new HSIProps[0];
       
       if (sum_props == null)
          sum_props = new SumProps[0];

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

    /**
     * Determines the default name to assign ratio, HSi and sum images
     * when being saved. Generally they will all have the same prefix name
     * followed by some detail about the image along with an extension that
     * corresponds to the type of image.
     * <p>
     * For example, if the current open file is called test_file.nrrd and
     * the user wished to save all derived images, the following are possible
     * names of the saved images depending on its type:
     * <ul>
     * <li>test_file_m13.sum
     * <li>test_file_m13_m12.sum
     * <li>test_file_m13_m12.ratio
     * <li>test_file_m13_m12.hsi
     * </ul>
     *
     * @param img the image to be saved.
     * @return the default name of the file.
     */
    String getExportName(MimsPlus img) {
        String name = "";
        name += this.getImageFilePrefix();

           if(img.getMimsType()==MimsPlus.MASS_IMAGE) {
               int index = img.getMassIndex();
               int mass = Math.round(new Float(getOpener().getMassNames()[index]));
               name += "_m" + mass;
               return name;
           }

        if(img.getMimsType()==MimsPlus.RATIO_IMAGE) {
            RatioProps ratioprops = img.getRatioProps();
            int numIndex = ratioprops.getNumMassIdx();
            int denIndex = ratioprops.getDenMassIdx();
            int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
            int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
            name += "_m" + numMass + "_m" + denMass + "_ratio";
            return name;
        }

        if(img.getMimsType()==MimsPlus.HSI_IMAGE) {
            HSIProps hsiprops = img.getHSIProps();

           int numIndex = hsiprops.getNumMassIdx();
           int denIndex = hsiprops.getDenMassIdx();
           int numMass = Math.round(new Float(getOpener().getMassNames()[numIndex]));
           int denMass = Math.round(new Float(getOpener().getMassNames()[denIndex]));
           name += "_m" + numMass + "_m" + denMass + "_hsi";
           return name;
        }

        if (img.getMimsType() == MimsPlus.SUM_IMAGE) {
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

        if (img.getMimsType() == MimsPlus.SEG_IMAGE) {
            name += "_seg";
            return name;
        }

        if (img.getMimsType() == img.COMPOSITE_IMAGE) {
            name = img.getTitle();
            return name;
        }

        return name;
    }

    /**
     * Action method for changing tabs. Im not sure but I dont think
     * this should be needed. Possible future delete.
     */
    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged

        if (this.mimsTomography != null) {
            this.mimsTomography.resetImageNamesList();
        }

        if (this.mimsStackEditing != null) {
            this.mimsStackEditing.resetTrueIndexLabel();
            this.mimsStackEditing.resetSpinners();
        }

    }//GEN-LAST:event_jTabbedPane1StateChanged

    /** Action method File>Exit menu item. Closes the application. */
private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
   closeCurrentImage();
   this.close();
}//GEN-LAST:event_exitMenuItemActionPerformed

   /** Action method for Utilities>Sum all Open menu item. Generates sum images
    for all open mass and ration images.*/
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

/** Action method for File>About menu item. Displays basic information about the Open Mims plugins. */
private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed

    String message = "OpenMIMS v1.0 Revision: " + this.revisionNumber + "\n\n";
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

/**
 * Action method for Utilities>Capture Current Image menu item. Generates
 * a .png screen capture for the most recently clicked image.
 */
private void captureImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureImageMenuItemActionPerformed

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

        String dir = file.getParent() + File.separator;
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

/**
 * Action method for the Utilities>Import .im  List menu item. Loads
 * a list of images caontained in a text file and concatenates them.
 */
private void importIMListMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importIMListMenuItemActionPerformed
    com.nrims.data.LoadImageList testLoad = new com.nrims.data.LoadImageList(this);
    boolean read;
    read = testLoad.openList();
    if (!read) {
        return;
    }

    testLoad.printList();
    testLoad.simpleIMImport();
}//GEN-LAST:event_importIMListMenuItemActionPerformed
                                                                              
/**
 * Action method for the Utilities>Generate Stack menu item. Turns
 * a ratio or HSI image into a stack.
 */
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
                                                                                         
/** Action method Utilities>Test menu item. Reserved for testing code. */
/**
 * Action method for File>Save Image and File>Save Session menu items.
 * Brings up a JFileChooser for saving the current image (or session).
 */
private void saveMIMSjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMIMSjMenuItemActionPerformed

   String fileName;
   try {
      // User sets file prefix name
      JFileChooser fc = new JFileChooser();
      if (lastFolder != null) {
         fc.setCurrentDirectory(new java.io.File(lastFolder));
      }
      if (this.getImageFilePrefix() != null) {
         fc.setSelectedFile(new java.io.File(this.getImageFilePrefix() + NRRD_EXTENSION));
      }
      int returnVal = fc.showSaveDialog(jTabbedPane1);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         fileName = fc.getSelectedFile().getAbsolutePath();
         File file = new File(fileName);
         if (file.exists()) {
            int n = JOptionPane.showConfirmDialog(
                    this,
                    "File already exists.\n" + file.getAbsolutePath() + "\n" + "Overwrite?\n",
                    "Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (n == JOptionPane.NO_OPTION) {
               return;
            }
         }
         lastFolder = file.getParent();
         setIJDefaultDir(lastFolder);
         setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

         // Determine if we save just the mass images or the entire session.
         // (e.g. Mass images, Rois, HSIs, ratio image)
         boolean saveImageOnly = true;
         if (evt.getActionCommand().equals(SAVE_SESSION)) {
            saveImageOnly = false;
         }
         saveSession(fileName, saveImageOnly);
      } else {
         return;
      }
   } catch (Exception e) {
      ij.IJ.error("Save Error", "Error saving file.");
   } finally {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
   }
}//GEN-LAST:event_saveMIMSjMenuItemActionPerformed

/**
 * Action method for the Utilities>Close>Close All Ratio Images.
 * Closes all ratio images.
 */
private void closeAllRatioMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllRatioMenuItemActionPerformed

    for (int i = 0; i < ratioImages.length; i++) {
        if (ratioImages[i] != null) {
            ratioImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllRatioMenuItemActionPerformed

/**
 * Action method for the Utilities>Close>Close All HSI Images.
 * Closes all HSI images.
 */
private void closeAllHSIMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllHSIMenuItemActionPerformed

    for (int i = 0; i < hsiImages.length; i++) {
        if (hsiImages[i] != null) {
            hsiImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllHSIMenuItemActionPerformed

/**
 * Action method for the Utilities>Close>Close All Sum Images.
 * Closes all sum images.
 */
private void closeAllSumMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAllSumMenuItemActionPerformed

    for (int i = 0; i < sumImages.length; i++) {
        if (sumImages[i] != null) {
            sumImages[i].close();
        }
    }
}//GEN-LAST:event_closeAllSumMenuItemActionPerformed

/**
 * Exports all open sum images, HSI images, ratio images and composite images as .png files.
 */
public void exportPNGs(){
   File file = image.getImageFile();
    System.out.println(file.getParent()+File.separator);
    String dir = file.getParent()+File.separator;

    MimsPlus[] sum = getOpenSumImages();
    for( int i = 0; i < sum.length; i ++) {
        ImagePlus img = (ImagePlus)sum[i];
        ij.io.FileSaver saver = new ij.io.FileSaver(img);
        String name = getExportName(sum[i]) + ".png";
        File saveName = new File(dir+name);
        if (saveName.exists()) {
           for (int j = 1; j < 1000; j++){
              name = getExportName(sum[i]) + "_" + j + ".png";
              saveName = new File(dir+name);
              if (!saveName.exists())
                 break;
           }
        }
        saver.saveAsPng(dir+name);
    }

    MimsPlus[] hsi = getOpenHSIImages();
    for( int i = 0; i < hsi.length; i ++) {
        ImagePlus img = (ImagePlus)hsi[i];
        ij.io.FileSaver saver = new ij.io.FileSaver(img);
        String name = getExportName(hsi[i]) + ".png";
        File saveName = new File(dir+name);
        if (saveName.exists()) {
           for (int j = 1; j < 1000; j++){
              name = getExportName(hsi[i]) + "_" + j + ".png";
              saveName = new File(dir+name);
              if (!saveName.exists())
                 break;
           }
        }
        saver.saveAsPng(dir+name);
    }

    MimsPlus[] ratios = getOpenRatioImages();
    for( int i = 0; i < ratios.length; i ++) {
        ImagePlus img = (ImagePlus)ratios[i];
        ij.io.FileSaver saver = new ij.io.FileSaver(img);
        String name = getExportName(ratios[i]) + ".png";
        File saveName = new File(dir+name);
        if (saveName.exists()) {
           for (int j = 1; j < 1000; j++){
              name = getExportName(ratios[i]) + "_" + j + ".png";
              saveName = new File(dir+name);
              if (!saveName.exists())
                 break;
           }
        }
        saver.saveAsPng(dir+name);
    }


    MimsPlus[] comp = getOpenCompositeImages();
    for( int i = 0; i < comp.length; i ++) {
        ImagePlus img = (ImagePlus)comp[i];
        ij.io.FileSaver saver = new ij.io.FileSaver(img);
        String name = getExportName(comp[i]) + ".png";
        File saveName = new File(dir+name);
        if (saveName.exists()) {
           for (int j = 1; j < 1000; j++){
              name = getExportName(comp[i]) + "_" + j + ".png";
              saveName = new File(dir+name);
              if (!saveName.exists())
                 break;
           }
        }
        saver.saveAsPng(dir+name);
    }

}

/** Action method for Utilities>Export>Export All Derived. Exports all derived images and .png's. */
private void exportPNGjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPNGjMenuItemActionPerformed
   exportPNGs();
}//GEN-LAST:event_exportPNGjMenuItemActionPerformed

/** Action method for Utilities>Image Notes. Opens a text area for adding notes. */
private void imageNotesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageNotesMenuItemActionPerformed
    this.imgNotes.setVisible(true);
}//GEN-LAST:event_imageNotesMenuItemActionPerformed

/** Action method for Utilities>Composite menu item. Shows the compisite manager interface. */
private void compositeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compositeMenuItemActionPerformed
    cbControl.showCompositeManager();
}//GEN-LAST:event_compositeMenuItemActionPerformed

/** Action method for Utilities>Debug menu item. */
/** Not used. Unable to delete. Stupid netbeans. */
private void exportHSI_RGBAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportHSI_RGBAActionPerformed
    // Empty
}//GEN-LAST:event_exportHSI_RGBAActionPerformed

/** Action method for View>Roi Manager menu item. Display the Roi Manager. */
private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
   getRoiManager().showFrame();
}//GEN-LAST:event_jMenuItem5ActionPerformed

/**
 * Generates a new MimsPlus image that is a stack. Whereas ratio
 * image ans HSI images are single plane images by design, this method
 * will turn it into a scrollable stack.
 *
 * @param img the image (ratio or HSI images only)/
 */
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

/**
 * Updates the line profile to reflect the data stored in <code>newdata</code>.
 *
 * @param newdata the new data.
 * @param name the name to be on the legend.
 * @param width width of the line roi.
 */
public void updateLineProfile(double[] newdata, String name, int width) {
    if(this.lineProfile==null) {
        return;
    } else {
        lineProfile.updateData(newdata, name, width);
    }
}

   /**
    * Returns an instance of the RoiManager.
    *
    * @return an instance of the RoiManager.
    */
    public MimsRoiManager getRoiManager() {
        roiManager = MimsRoiManager.getInstance();
        if (roiManager == null) {
            roiManager = new MimsRoiManager(this);
        }
        return roiManager;
    }

    /**
     * Returns the directory of the image currently opened.
     *
     * @return the directory of the current image file.
     */
    String getImageDir() {
        String path = image.getImageFile().getParent();
        return path;
    }
    
    /**
     * Returns all the mass images, regardless if they are open or not.
     *
     * @return MimsPlus array of mass images.
     */
    public MimsPlus[] getMassImages() {
        return massImages;
    }

    /**
     * Returns the mass image with index <code>i</code>.
     *
     * @param i the index.
     * @return the mass image.
     */
    public MimsPlus getMassImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return massImages[i];
        }
        return null;
    }

    /**
     * Determines if the mass value for a given index <code>i</code>
     * is close enough to the mass value <code>d</code> to be considered
     * equal. The current range is hard coded as being .9 to 1.1 of the
     * same value to be considered equal, these parameters should be
     * made as preferences.
     *
     * @param i the index for a current mass value.
     * @param d the original mass value.
     * @return <code>true</code> if the mass falls within the specified range, otherwise <code>false</code>.
     */
    public boolean closeEnough(int i, double d) {
       double mass = getMassValue(i);
       double q = d/mass;
       if (q > 0.9 && q < 1.1)
          return true;
       else 
          return false;
    }

    /**
     * Gets the mass value for mass image with index <code>i</code>.
     *
     * @param i the index.
     * @return the mass value.
     */
    public double getMassValue(int i) {
       double mass = -1.0;
       try {
          mass = new Double(getOpener().getMassNames()[i]);
       } catch (Exception e) {}
       return mass;
    }

    /**
     * Gets the ratio image with index <code>i</code>.
     *
     * @param i the index
     * @return the image.
     */
    public MimsPlus getRatioImage(int i) {
        if (i >= 0 && i < maxMasses) {
            return ratioImages[i];
        }
        return null;
    }

    /**
     * Returns the open mass images as an array.
     *
     * @return array of images.
     */
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

    /**
     * Returns the open ratio images as an array.
     *
     * @return array of images.
     */
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

    /**
     * Returns the open composite images as an array.
     *
     * @return array of images.
     */
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

    /**
     * Returns the open HSI images as an array.
     *
     * @return array of images.
     */
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

    /**
     * Returns the open segmented images as an array.
     *
     * @return array of images.
     */
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

    /**
     * Returns the open sum images as an array.
     *
     * @return array of images.
     */
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

    /**
     * Returns the <code>HSIProps</code> object for all open HSI images.
     *
     * @return array of <code>HSIProps</code> objects.
     */
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

    /**
     * Returns the <code>RatioProps</code> object for all open ratio images.
     *
     * @return array of <code>RatioProps</code> objects.
     */
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

    /**
     * Returns the <code>SumProps</code> object for all open sum images.
     *
     * @return array of <code>SumProps</code> objects.
     */
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

    /**
     * Returns the mass, ratio, HSI or sum image with the name <code>name</code>.
     *
     * @param name the name
     * @return an image.
     */
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

    /**
     * Sets if all mass images should croll through
     * the stack in unison (synched).
     *
     * @param bSync Set to <code>true</code> if mass
     * images should scroll in sych, otherwise <code>false</code>.
     */
    public void setSyncStack(boolean bSync) {
        bSyncStack = bSync;
    }

    /**
     * Returns a pointer to the <code>MimsData</code> object
     * which controls the "Data" tab.
     *
     * @return the <code>MimsData</code> object.
     */
    public MimsData getMimsData() {
        return mimsData;
    }

    /**
     * Returns a pointer to the <code>MimsHSIView</code> object
     * which controls the "Process" tab.
     * 
     * @return the <code>MimsHSIView</code> object.
     */
    public MimsHSIView getHSIView() {
        return hsiControl;
    }

    /**
     * Returns a pointer to the <code>MimsLog</code> object
     * which controls the "MIMS Log" tab.
     *
     * @return the <code>MimsLog</code> object.
     */
    public MimsLog getmimsLog() {
        return mimsLog;
    }

    /**
     * Returns a pointer to the <code>MimsCBControl</code> object
     * which controls the "Contrast" tab.
     *
     * @return the <code>MimsCBControl</code> object.
     */
    public MimsCBControl getCBControl(){
       return cbControl;
    }

    /**
     * Returns a pointer to the <code>MimsStackEditor</code> object
     * which controls the "Stack Editing" tab.
     *
     * @return the <code>MimsStackEditor</code> object.
     */
    public MimsStackEditor getmimsStackEditing() {
        return mimsStackEditing;
    }

    /**
     * Returns a pointer to the <code>MimsTomography</code> object
     * which controls the "Tomography" tab.
     *
     * @return the <code>MimsTomography</code> object.
     */
    public MimsTomography getmimsTomography() {
        return mimsTomography;
    }

    /**
     * Returns a pointer to the <code>MimsAction</code> object.
     * The <code>MimsAction</code> object stores all the modifications
     * made to the current image (translations, plane deletions, etc).
     *
     * @return the <code>MimsAction</code> object.
     */
    public MimsAction getmimsAction() {
        return mimsAction;
    }

    /**
     * Sets the flag indicating the plugin is in the process of updating.
     *
     * @param bool <code>true</code> if updating, otherwise false.
     */
    public void setUpdating(boolean bool) {
        bUpdating = bool;
    }

    /**
     * Return <code>true</code> if the plugin is in the process of opening
     * an image.
     *
     * @return <code>true</code> if the plugin is in the process of opening
     * an image, otherwise <code>false</code>.
     */
    public boolean isOpening() {
        return currentlyOpeningImages;
    }

    /**
     * Gets the flag indicating if the plugin is in the process of updating.
     *
     * @return <code>true</code> if updating, otherwise false.
     */
    public boolean isUpdating() {
        return bUpdating;
    }

    /**
     * Returns a link the <code>PrefFrame</code> object for referencing user preferences.
     *
     * @return the <code>PrefFrame</code> object.
     */
    public PrefFrame getPreferences() {
        return prefs;
    }

    /**
     * Returns a link the <code>Opener</code> object for getting image data and metadata.
     * The UI class stores a list of opener objects in the case that the image was
     * concatenated or derived from multiple files. This method returns the member variable
     * <code>image</code> which will correspond to the FIRST image opened.
     *
     * @return the <code>Opener</code> object.
     */
    public Opener getOpener() {
        return image;
    }   

    /**
     * Gets <code>Opener</code> with name <code>name</code> from the list of opener objects.
     *
     * @param name the name of the opener object.
     * @return the <code>Opener</code>.
     */
    public Opener getFromOpenerList(String name){
        return (Opener)openers.get(name);
    }

    /**
     * Adds the <code>Opener</code> object with name <code>name</code>
     * to the list of openers.
     *
     * @param fileName name of file.
     * @param opener opener object.
     */
    public void addToOpenerList(String fileName, Opener opener) {
       openers.put(fileName, opener);
    }

   /**
    * Determines the bahavior when an image window is made active.
    *
    * @param mp the image.
    */
   public void setActiveMimsPlus(MimsPlus mp) {
      if (mp == null)
         return;
      
      if (mp.getMimsType() == MimsPlus.HSI_IMAGE) {
         hsiControl.setCurrentImage(mp);
         hsiControl.setProps(mp.getHSIProcessor().getHSIProps());
      } else if (mp.getMimsType() == MimsPlus.RATIO_IMAGE) {
         hsiControl.setCurrentImage(mp);
         hsiControl.setProps(mp.getRatioProps());
      }
   }

    /**
     * Displays the String <code>msg</code> in the status bar.
     *
     * @param msg the message to display.
     */
    public synchronized void updateStatus(String msg) {
        if (bUpdating) {
            return;
        }
        if (!currentlyOpeningImages) {
            mainTextField.setText(msg);
        } else {
            IJ.showStatus(msg);
        }
    }

    /**
     * Sets the radius of the radius of the median filter.
     *
     * @param r the radius.
     */
    public void setMedianFilterRadius(double r) {
        this.medianFilterRadius = r;        
    }

    /**
     * Returns the directory of the last location used by
     * the user for loading or saving image data.
     *
     * @return the last folder.
     */
    public String getLastFolder() {
        return lastFolder;
    }

    /**
     * Sets the ImageJ default directory so that when imageJ
     * file choosers are opened, they are pointing to the
     * directory <code>dir</code>.
     *
     * @param dir the directory.
     */
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
                    UI ui_to_run = new UI();
                    ui_to_run.setVisible(true);
                    files_arr[0] = new File(temp_path);
                    File file_to_open = files_arr[0];
                    ui_to_run.openFile(file_to_open);
                    
                }
            });
        } else {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    System.out.println("Ui.run called");
                    UI ui_to_run = new UI();
                    ui_to_run.setVisible(true);
                }
            });
        }
    }

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JMenuItem aboutMenuItem;
   private javax.swing.JMenuItem captureImageMenuItem;
   private javax.swing.JMenuItem closeAllHSIMenuItem;
   private javax.swing.JMenuItem closeAllRatioMenuItem;
   private javax.swing.JMenuItem closeAllSumMenuItem;
   private javax.swing.JMenu closeMenu;
   private javax.swing.JMenuItem compositeMenuItem;
   private javax.swing.JMenu editMenu;
   private javax.swing.JMenuItem exitMenuItem;
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
   private javax.swing.JMenuItem jMenuItem5;
   private javax.swing.JMenuItem jMenuItem9;
   private javax.swing.JPanel jPanel1;
   private javax.swing.JPopupMenu jPopupMenu1;
   private javax.swing.JSeparator jSeparator1;
   private javax.swing.JSeparator jSeparator2;
   private javax.swing.JSeparator jSeparator3;
   private javax.swing.JSeparator jSeparator4;
   private javax.swing.JSeparator jSeparator6;
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
}