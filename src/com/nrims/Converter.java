package com.nrims;

import ij.ImagePlus;
import ij.io.FileSaver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import javax.swing.SwingWorker;

public class Converter extends SwingWorker<Void, Void> {

   // Properties file strings.
   Properties defaultProps;
   public static final String  PROPERTIES_PNGS          = "PNGS";
   public static final String  PROPERTIES_PNG_DIRECTORY = "PNG_DIRECTORY";
   public static final String  PROPERTIES_PNG_OVERWRITE = "PNG_OVERWRITE";
   public static final String  PROPERTIES_TRACK         = "TRACK";
   public static final String  PROPERTIES_TRACK_MASS    = "TRACK_MASS";
   public static final String  PROPERTIES_HSI           = "HSI";
   public static final String  PROPERTIES_THRESH_UPPER  = "HSI_THRESH_UPPER";
   public static final String  PROPERTIES_THRESH_LOWER  = "HSI_THRESH_LOWER";
   public static final String  PROPERTIES_RGB_MAX       = "HSI_RGB_MAX";
   public static final String  PROPERTIES_RGB_MIN       = "HSI_RGB_MIN";
   public static final String  PROPERTIES_USE_SUM       = "USE_SUM";
   public static final String  PROPERTIES_MEDIANIZE     = "MEDIANIZE";
   public static final String  PROPERTIES_MEDIANIZATION_RADIUS = "THRESH_LOWER";

   // Default values.
   public static final boolean PNGS_ONLY_DEFAULT        = false;
   public static final boolean TRACK_DEFAULT            = false;
   public static final boolean PNG_DEFAULT              = false;
   public static final boolean PNG_OVERWRITE_DEFAULT    = false;
   public static final boolean USE_SUM_DEFAULT          = false;
   public static final boolean MEDIANIZE_DEFAULT        = false;
   public static final double  MEDIANIZE_RADIUS_DEFAULT = 1.5;
   public static final String  TRACK_MASS_DEFAULT       = "0";
   public static final String  HSI_DEFAULT              = "";
   public static final String  THRESH_UPPER_DEFAULT     = "";
   public static final String  THRESH_LOWER_DEFAULT     = "";
   public static final String  RGB_MAX_DEFAULT          = "";
   public static final String  RGB_MIN_DEFAULT          = "";
   public static final int     RGB_MAX_INT_DEFAULT      = 51;
   public static final int     RGB_MIN_INT_DEFAULT      = 1;
   public static final String  PNG_DIRECTORY_DEFAULT    = null;
   public static final String  NRRD_EXTENSION           = ".nrrd";

   UI ui;
   boolean pngs_only       = PNGS_ONLY_DEFAULT;
   boolean track           = TRACK_DEFAULT;
   boolean pngs            = PNG_DEFAULT;
   boolean overwrite_pngs  = PNG_OVERWRITE_DEFAULT;
   boolean useSum          = USE_SUM_DEFAULT;
   boolean medianize       = MEDIANIZE_DEFAULT;
   String massToTrack      = TRACK_MASS_DEFAULT;
   String[] HSIs           = new String[0];
   String[] threshUppers   = new String[0];
   String[] threshLowers   = new String[0];
   String[] rgbMaxes       = new String[0];
   String[] rgbMins        = new String[0];
   int trackIndex          = 0;
   double medianizeRadius  = MEDIANIZE_RADIUS_DEFAULT;
   String pngDir           = PNG_DIRECTORY_DEFAULT;
   ArrayList<String> files = new ArrayList<String>();
   boolean proceed         = true;
   
   public Converter(boolean readProps, boolean bpngs_only, boolean bTrack,
           String sMassToTrack, String propertiesFileString) {

      ui = new UI(true);
      pngs_only = bpngs_only;
      track = bTrack;
      massToTrack = sMassToTrack;

      if (readProps) {
         try {
            readProperties(propertiesFileString);
            setUISettings();
         } catch (FileNotFoundException fnfe) {
            System.out.println("Can not find Properties file: " + propertiesFileString);
            return;
         } catch (IOException ioe) {
            System.out.println("Trouble reading Properties file: " + propertiesFileString);
            return;
         }
      }      
   }

   private void readProperties(String propertiesFileString) throws FileNotFoundException, IOException {

      // create and load default properties
      defaultProps = new Properties();
      FileInputStream in = new FileInputStream(propertiesFileString);
      defaultProps.load(in);
      defaultProps.list(System.out);
      in.close();

      // Track.
      track = Boolean.parseBoolean(defaultProps.getProperty(PROPERTIES_TRACK, Boolean.toString(track)));
      if (track)
         massToTrack = defaultProps.getProperty(PROPERTIES_TRACK_MASS, massToTrack);

      // Pngs
      pngs = Boolean.parseBoolean(defaultProps.getProperty(PROPERTIES_PNGS, Boolean.toString(pngs)));
      if (pngs) {
         pngDir = defaultProps.getProperty(PROPERTIES_PNG_DIRECTORY, PNG_DIRECTORY_DEFAULT);        
      } else {
         return;
      }
      overwrite_pngs = Boolean.parseBoolean(defaultProps.getProperty(PROPERTIES_PNG_OVERWRITE, Boolean.toString(overwrite_pngs)));

      // Hsi's
      String HSI = defaultProps.getProperty(PROPERTIES_HSI, HSI_DEFAULT);
      HSI = HSI.replace("\"", "");
      HSIs = HSI.split(" ");
      if (HSIs.length == 0)
         return;

      // Upper threshold.
      String threshUpper = defaultProps.getProperty(PROPERTIES_THRESH_UPPER, THRESH_UPPER_DEFAULT);
      threshUpper = threshUpper.replaceAll("\"", "");
      threshUppers = threshUpper.split(" ");

      // Lower threshold
      String threshLower = defaultProps.getProperty(PROPERTIES_THRESH_LOWER, THRESH_LOWER_DEFAULT);
      threshLower = threshLower.replaceAll("\"", "");
      threshLowers = threshLower.split(" ");

      // RGB Max
      String rgbMax = defaultProps.getProperty(PROPERTIES_RGB_MAX, RGB_MAX_DEFAULT);
      rgbMax = rgbMax.replaceAll("\"", "");
      rgbMaxes = rgbMax.split(" ");

      // RGB Min
      String rgbMin = defaultProps.getProperty(PROPERTIES_RGB_MIN, RGB_MIN_DEFAULT);
      rgbMin = rgbMin.replaceAll("\"", "");
      rgbMins = rgbMin.split(" ");

      // Use sum.
      useSum = Boolean.parseBoolean(defaultProps.getProperty(PROPERTIES_USE_SUM, Boolean.toString(USE_SUM_DEFAULT)));

      // Medianize
      medianize = Boolean.parseBoolean(defaultProps.getProperty(PROPERTIES_MEDIANIZE, Boolean.toString(MEDIANIZE_DEFAULT)));

      // Medianization radius
      medianizeRadius = Double.parseDouble(defaultProps.getProperty(PROPERTIES_MEDIANIZATION_RADIUS, Double.toString(MEDIANIZE_RADIUS_DEFAULT)));

   }

   private void setUISettings() {
      if (useSum)
         ui.setIsSum(true);

      if (medianize) {
         ui.setMedianFilterRatios(true);
         ui.setMedianFilterRadius(medianizeRadius);
      }
   }

   public void setFiles(ArrayList<String> filesArrayList) {
      files = filesArrayList;
   }

   public void proceed(boolean proceed) {
      this.proceed = proceed;
   }

   private boolean writeNrrd(){

      // Initialize variables.
      boolean written = false;
      File imFile = ui.getOpener().getImageFile().getAbsoluteFile();
      String imDirectory = imFile.getParent();

      // Save the original .im file to a new file of the .nrrd file type.
      String nrrdFileName = ui.getImageFilePrefix() + NRRD_EXTENSION;
      File saveFile = new File(imDirectory, nrrdFileName);
      saveFile.setWritable(true, false);
      if (saveFile.getParentFile().canWrite()) {
         System.out.println("          Saving... " + saveFile.getAbsolutePath());
         ui.saveSession(saveFile.getAbsolutePath(), true);
         written = true;
      } else {
         written = false;
      }
      return written;
   }

   private void generate_pngs() {

      String pngDirectory= new File(ui.getOpener().getImageFile().getParent()).getAbsolutePath();
      if (pngDir != null)
         pngDirectory = (new File(ui.getOpener().getImageFile().getParent(), pngDir)).getAbsolutePath();

      File pngDirFile = new File(pngDirectory);

      if (!pngDirFile.exists()) {
         pngDirFile.mkdir();
         pngDirFile.setWritable(true, false);
      }

      if (!pngDirFile.canWrite()) {
         System.out.println("WARNING: Can not create or write to directory: " + pngDir);
         return;
      }
      generateMassImagePNGs(pngDirFile);
      generateHSIImagePNGs(pngDirFile);
   }

   private void generateMassImagePNGs(File pngDirFile) {

      // Generate mass images.
      String name;
      FileSaver saver;
      MimsPlus img;
      File saveName;
      MimsPlus[] mp = ui.getOpenMassImages();
      SumProps sp;
      for (int i = 0; i < mp.length; i++) {
         sp = new SumProps(i);
         img = new MimsPlus(ui, sp, null);
         name = ui.getExportName(img) + ".png";
         saveName = new File(pngDirFile, name);         
         System.out.println("       PNG-ing... " + saveName.getAbsolutePath());
         ui.autoContrastImage(img);
         saver = new ij.io.FileSaver(img);
         saver.saveAsPng(saveName.getAbsolutePath());
         saveName.setWritable(true, false);
      }
   }

   private void generateHSIImagePNGs(File pngDirFile) {

      // Generate hsi images.
      int numIdx, denIdx;
      double numMass, denMass;
      double upperThresh, lowerThresh;
      int rgbMax, rgbMin;
      String numerator, denominator;
      int counter = 0;
      MimsPlus hsi_mp;
      FileSaver saver;
      File saveName;
      for (String hsi : HSIs) {
         numerator = hsi.substring(0, hsi.indexOf("/"));
         denominator = hsi.substring(hsi.indexOf("/")+1, hsi.length());
         try {
            numMass = (new Double(numerator)).doubleValue();
            denMass = (new Double(denominator)).doubleValue();
            numIdx = ui.getClosestMassIndices(numMass, 0.49);
            denIdx = ui.getClosestMassIndices(denMass, 0.49);
         } catch (Exception e) {
            System.out.println("Skipping \"" + hsi + "\".");
            continue;
         }

         HSIProps hsiprops;
         if (numIdx >= 0 && denIdx >= 0)
            hsiprops = new HSIProps(numIdx, denIdx);
         else
            continue;

         if (counter < threshUppers.length) {
            try {
               upperThresh = (new Double(threshUppers[counter])).doubleValue();
               hsiprops.setMaxRatio(upperThresh);
            } catch (NumberFormatException nfe) {
               System.out.println("WARNING: Bad format for upper threshold: " + threshUppers[counter] + ". Auto thresholding");
            }
         }

         if (counter < threshLowers.length) {
            try {
               lowerThresh = (new Double(threshLowers[counter])).doubleValue();
               hsiprops.setMinRatio(lowerThresh);
            } catch (NumberFormatException nfe) {
               System.out.println("WARNING: Bad format for lower threshold: " + threshLowers[counter] + ". Auto thresholding");
            }
         }
         
         if (counter < rgbMaxes.length) {
            try {
               rgbMax = (new Integer(rgbMaxes[counter])).intValue();
               hsiprops.setMaxRGB(rgbMax);
            } catch (NumberFormatException nfe) {
               hsiprops.setMaxRGB(RGB_MAX_INT_DEFAULT);
               System.out.println("WARNING: Bad format for max RGB: " + rgbMaxes[counter] + ". Auto setting");
            }
         }

         if (counter < rgbMins.length) {
            try {
               rgbMin = (new Integer(rgbMins[counter])).intValue();
               hsiprops.setMinRGB(rgbMin);
            } catch (NumberFormatException nfe) {
               hsiprops.setMinRGB(RGB_MIN_INT_DEFAULT);
               System.out.println("WARNING: Bad format for min RGB: " + rgbMins[counter] + ". Auto setting");
            }
         }

         hsi_mp = new MimsPlus(ui, hsiprops);
         String name = ui.getExportName(hsi_mp) + ".png";
         saveName = new File(pngDirFile,name);
         System.out.println("       PNG-ing... " + saveName.getAbsolutePath());
         while (hsi_mp.getHSIProcessor().isRunning()) {
            try {
               Thread.sleep(100);
            } catch (InterruptedException ie) {
               // do nothing
            }
         }
         saver = new FileSaver(hsi_mp);
         saver.saveAsPng(saveName.getAbsolutePath());
         saveName.setWritable(true, false);
         counter++;
      }

   }

   private boolean trackFile(){

      try {
         double massString = new Double(massToTrack);
         trackIndex = ui.getClosestMassIndices(massString, 0.5);
         if (trackIndex < 0)
            trackIndex = 0;
      } catch (Exception e) {
         System.out.println(massToTrack + " must be a number.");
         trackIndex = 0;
      }

      try {
         System.out.println("   Tracking... (using index " + trackIndex + ")");
         ImagePlus mp = (ImagePlus)ui.getMassImage(trackIndex).clone();
         AutoTrack autoTrack = new AutoTrack(ui, mp);
         ArrayList<Integer> includeList = new ArrayList<Integer>();
         for (int i = 0; i < mp.getNSlices(); i++) {
            includeList.add(i, i + 1);
         }
         autoTrack.setIncludeList(includeList);
         double[][] trans = autoTrack.track(mp);
         ui.getmimsStackEditing().applyTranslations(trans, includeList);
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
      return true;
      
   }

   private boolean openFile(String fileString) {      
      boolean opened = false;
      File file = new File(fileString);

      if (file.exists() && file.canRead()) {
         System.out.println("Opening... " + fileString);        
         opened = ui.openFile(file);
      } else {
         System.out.println("Can not find, or can not read " + fileString);
         opened = false;
      }
      return opened;
   }

    public static void main(String[] args) {

       // Properties defaults.
       boolean readProps = false;
       String propertiesFileString = "";

        // Tracking defaults.
        boolean track = TRACK_DEFAULT;
        String massToTrack = TRACK_MASS_DEFAULT;

        boolean lpngs_only = PNGS_ONLY_DEFAULT;

        // File list
        ArrayList filesArrayList = new ArrayList<String>();

        // Collect input arguments.
        String arg = "";
        for (int i = 0; i < args.length; i++) {
            arg = args[i].trim();
            System.out.println("arg = " + arg);
            if(arg.equals("-t")) {
               i++;
               massToTrack = args[i].trim();
               track = true;
            }
            else if (arg.startsWith("-properties")) {
               i++;
               propertiesFileString = args[i].trim();
               readProps = true;
            }
            else if (arg.startsWith("-pngs_only")) {
               lpngs_only = true;
            }
            else {
               if (args[i].endsWith(".im") || args[i].endsWith(".nrrd")) {
                  filesArrayList.add(args[i]);
               }
            }
        }

        Converter mn = new Converter(readProps, lpngs_only, track, massToTrack, propertiesFileString);
        mn.setFiles(filesArrayList);
        mn.doInBackground();
        System.exit(0);
   }

   @Override
   protected Void doInBackground() {
      
      int percentComplete = 0;
      setProgress(percentComplete);

      // Make sure we have files.
      if (files.isEmpty())
         System.out.println("No files specified.");
           
      // Open the file, track and save.
      int counter = 0;
      int total = files.size();
      for (String fileString : files) {

         if (!proceed) {
            break;
         }

            // Open File.
            boolean opened = openFile(fileString);
            if (!opened) {
               System.out.println("Failed to open " + fileString);
               continue;
            }

            percentComplete = Math.round(100*((float)counter+(float)0.25)/(float)total);
            setProgress(percentComplete);

            // Track File.
            if (track && !pngs_only) {
               boolean tracked = trackFile();
               if (!tracked) {
                  System.out.println("Failed to track " + fileString);
                  continue;
               }
            }

            percentComplete = Math.round(100*((float)counter+(float)0.5)/(float)total);
            setProgress(percentComplete);

            // Generate Pngs.
            if (pngs) {
               generate_pngs();
            }

            percentComplete = Math.round(100*((float)counter+(float)0.75)/(float)total);
            setProgress(percentComplete);

            // Save File.
            if (!pngs_only) {
               boolean wrote = writeNrrd();
               if (!wrote) {
                  System.out.println("Failed to convert " + fileString);
                  continue;
               }
            }

            percentComplete = Math.min(Math.round(100*((float)counter+(float)1.0)/(float)total), 100);
            setProgress(percentComplete);            

            ui.closeCurrentImage();
            counter++;
      }

      return null;
   }

}
