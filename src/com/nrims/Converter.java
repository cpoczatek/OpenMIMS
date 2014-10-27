package com.nrims;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;
//import sun.misc.BASE64Encoder; // DJ: to encode the png images into a string.
import org.apache.commons.codec.binary.Base64; // DJ: to encode png image into a string.
import java.io.ByteArrayOutputStream; // DJ:
import java.io.PrintWriter;
import java.util.HashMap;
import javax.imageio.ImageIO; // DJ
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
//import org.jfree.xml.util.Base64;


// DJ: 09/06/2014 : libraries to be used to copy the 
// generated html file's path to clipboard so the user
// could just paste within the browser to doaplay the page.
import java.awt.datatransfer.*;
import java.awt.Toolkit;

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
   public static final String  PROPERTIES_RATIO_SCALE_FACTOR  = "RATIO_SCALE_FACTOR";
   public static final String  PROPERTIES_USE_SUM       = "USE_SUM";
   public static final String  PROPERTIES_MEDIANIZE     = "MEDIANIZE";
   public static final String  PROPERTIES_MEDIANIZATION_RADIUS = "MEDIANIZATION_RADIUS";

   // Default values.
   public static final boolean PNGS_ONLY_DEFAULT        = false;
   public static final boolean TRACK_DEFAULT            = false;
   public static final boolean PNG_DEFAULT              = false;
   public static final boolean PNG_OVERWRITE_DEFAULT    = false;
   public static final boolean USE_SUM_DEFAULT          = false;
   public static final boolean MEDIANIZE_DEFAULT        = false;
   public static final boolean INSERT_KV_PAIRS_DEFAULT  = false;
   public static final String  KEYS_DEFAULT             = "";
   public static final String  VALUES_DEFAULT           = "";
   public static final String  TRACK_MASS_DEFAULT       = "0";
   public static final String  HSI_DEFAULT              = "";
   public static final String  THRESH_UPPER_DEFAULT     = "";
   public static final String  THRESH_LOWER_DEFAULT     = "";
   public static final String  RGB_MAX_DEFAULT          = "";
   public static final String  RGB_MIN_DEFAULT          = "";
   public static final double  SCALE_FACTOR_DEFAULT     = 10000;
   public static final String  PNG_DIRECTORY_DEFAULT    = null;
   public static final String  NRRD_EXTENSION           = ".nrrd";
   public static final int     RGB_MAX_INT_DEFAULT      = 51;
   public static final int     RGB_MIN_INT_DEFAULT      = 1;
   public static final double  MEDIANIZE_RADIUS_DEFAULT = 1.5;

   UI ui;
   boolean pngs_only       = PNGS_ONLY_DEFAULT;
   boolean track           = TRACK_DEFAULT;
   boolean pngs            = PNG_DEFAULT;
   boolean overwrite_pngs  = PNG_OVERWRITE_DEFAULT;
   boolean useSum          = USE_SUM_DEFAULT;
   boolean medianize       = MEDIANIZE_DEFAULT;
   boolean insert_kv_pairs = INSERT_KV_PAIRS_DEFAULT;
   String keys             = KEYS_DEFAULT;
   String values           = VALUES_DEFAULT;
   String massToTrack      = TRACK_MASS_DEFAULT;
   String[] HSIs           = new String[0];
   String[] threshUppers   = new String[0];
   String[] threshLowers   = new String[0];
   String[] rgbMaxes       = new String[0];
   String[] rgbMins        = new String[0];
   String[] scaleFactors    = new String[0];
   int      trackIndex     = 0;
   double medianizeRadius  = MEDIANIZE_RADIUS_DEFAULT;
   String pngDir           = PNG_DIRECTORY_DEFAULT;
   ArrayList<String> files = new ArrayList<String>();
   boolean proceed         = true;
   
   //DJ:
   boolean isForHTMLManager = false;
   String htmlTables = ""; 
   String htmlFile = "";
   boolean configSpecs = false; // if we used a config filr, it would be true
   
   public Converter(boolean readProps, boolean bpngs_only, boolean bTrack,
           String sMassToTrack, String propertiesFileString, String keys, String values) {

      ui = new UI(true);
      pngs_only = bpngs_only;
      track = bTrack;
      massToTrack = sMassToTrack;
      if (!keys.isEmpty()) {
         insert_kv_pairs = true;
         this.keys = keys;
         this.values = values;
      }

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
      this.pngs = Boolean.parseBoolean(defaultProps.getProperty(PROPERTIES_PNGS, Boolean.toString(pngs)));
      if (this.pngs) {
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

      //ratio scale factor
      String factors = defaultProps.getProperty(PROPERTIES_RATIO_SCALE_FACTOR, Double.toString(SCALE_FACTOR_DEFAULT));
      factors = factors.replaceAll("\"", "");
      scaleFactors = factors.split(" ");

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
   
   //DJ
   // to setup the png directory for the convertManger to use it 
   // in order to generate html page.
   public void hsiImageSpecsForHTML(String htmlFilePath,
                            String pngFolderPath, 
                            String[] HSIs,
                            String[] numThreshs,
                            String[] denThreshs,
                            String[] ratioScaleFactors,
                            String[] maxRGBs,
                            String[] minRGBs){
       this.htmlFile = htmlFilePath;
       this.isForHTMLManager = true;
       this.pngDir = pngFolderPath;
       this.pngs = true;
      // this.pngs_only = false;
       this.HSIs = HSIs; 
       this.threshUppers = numThreshs;
       this.threshLowers = denThreshs;
       this.scaleFactors = ratioScaleFactors;
       this.rgbMaxes = maxRGBs;
       this.rgbMins = minRGBs;
   }
   //DJ
   // To be called when we're using a config file to retrieve the
   // specs from.
   public void specsForHtmlThruConfigFile(String htmlFilePath){
       this.htmlFile = htmlFilePath;
       this.isForHTMLManager = true;
       configSpecs = true;
   }
   // DJ
   // to be mainly called from a script
   public String getPngsDirectoryPath(){
        String pngDirectory = "";
        
        // case that applies just on: 
        // creating a web page where the specs are collected
        // from OpenMIMS
        if (this.isForHTMLManager && configSpecs == false) {
            pngDirectory = pngDir;
            
        // in case the specs are collected from a "CONFIG" file
        } else {
            pngDirectory = new File(ui.getOpener().getImageFile().getParent()).getAbsolutePath();
            if (pngDir != null) // pngDirectory = pngDir;
            {
                pngDirectory = (new File(ui.getOpener().getImageFile().getParent(), pngDir)).getAbsolutePath();
            }
        }
        //System.out.println("pngDirectory is: " + pngDirectory);
        return pngDirectory;
   }
   // DJ
   public String generateHTML(){
       String html = "";
       html += "<!DOCTYPE html>";
       html += "<html lang=\"en\">";
       html += "  <head><title>OpenMIMS - HTML GENERATOR </title></head>";
       html += "  <body BGCOLOR=\"#D8D8D8\"><BR><BR><BR>";
       
       html += this.htmlTables;
       
       html += "  </body>";
       html += "</html>";
       
       return html;
   }
    private void generate_pngs() {
        String pngDirectory = getPngsDirectoryPath();

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
       // generateRatioImagePNGs(pngDirFile); // Dj: 10/27/2014 just fe testing as of now.
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
      double rfactor;
      int rgbMax, rgbMin;
      String numerator, denominator;
      int counter = 0;
      MimsPlus hsi_mp;
      FileSaver saver;
      File saveName;
      for (String hsi : HSIs) {
         try {
            numerator = hsi.substring(0, hsi.indexOf("/"));
            denominator = hsi.substring(hsi.indexOf("/")+1, hsi.length());
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

         if (counter < scaleFactors.length) {
            try {
               rfactor = new Double(scaleFactors[counter]);
               hsiprops.setRatioScaleFactor(rfactor);
            } catch (NumberFormatException nfe) {
               hsiprops.setRatioScaleFactor(SCALE_FACTOR_DEFAULT);
               System.out.println("WARNING: Bad format for scale factor: " + scaleFactors[counter] + ". Auto setting");
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
         
         // Get the image to track.
         ImagePlus img = (ImagePlus)ui.getMassImage(trackIndex);

         // Build the include list
         ArrayList<Integer> includeList = new ArrayList<Integer>();
         for (int i = 0; i < img.getNSlices(); i++) {
            includeList.add(i, i + 1);
         }
         
         // Build a copy of the image.
         ImageStack imgStack = img.getImageStack();
         ImageStack tempStack = new ImageStack(img.getWidth(), img.getHeight());
         for (int i = 0; i < imgStack.getSize(); i++) {
               tempStack.addSlice(Integer.toString(i + 1), imgStack.getProcessor(i + 1));
         }
         ImagePlus mp = new ImagePlus("img", tempStack);
         
         // Auto track on the copy.
         AutoTrack autoTrack = new AutoTrack(ui, mp);
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

   private void insert_kv_pairs() {

      // Check if keys or values is empty
      if (keys.isEmpty() || values.isEmpty()) {
         System.out.println("      Can not insert key/value pairs: -k or -v is empty");
         return;
      }

      // Split the keys.
      keys = keys.replaceAll("\"", "");
      String[] k = keys.split(" ");

      // Split the values
      values = values.replaceAll("\"", "");
      String[] v = values.split(" ");

      // Make sure same number of keys as values.
      if (k.length != v.length) {
         System.out.println("      Can not insert key/value pairs: number of keys not equal to number of values");
         return;
      }

      // Insert into hashmap.
      System.out.println("      Inserting key value pairs...");
      for(int i = 0; i < k.length; i++) {         
         ui.insertMetaData(k[i], v[i]);
      }
      
   }

    public static void main(String[] args) {

       // Properties defaults.
       boolean readProps = false;
       String propertiesFileString = "";

        // Tracking defaults.
        boolean track = TRACK_DEFAULT;
        String massToTrack = TRACK_MASS_DEFAULT;

        // Generate PNGS only.
        boolean lpngs_only = PNGS_ONLY_DEFAULT;

        // Insert key value pairs.
        String keys = "";
        String values = "";

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
            else if (arg.equals("-k")) {
               i++;
               keys = args[i].trim();
            }
            else if (arg.equals("-v")) {
               i++;
               values = args[i].trim();
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

        Converter mn = new Converter(readProps, lpngs_only, track, massToTrack, propertiesFileString, keys, values);
        mn.setFiles(filesArrayList);
        mn.doInBackground();
        System.exit(0);
   }

   @Override
   public Void doInBackground() {
      
      int percentComplete = 0;
      setProgress(percentComplete);

      // Make sure we have files.
      if (files.isEmpty())
         System.out.println("No files specified.");
      
      //DJ:
      String[][] tables_Not_sorted = new String[files.size()][2];
      // Open the file, track and save.
      int counter = 0;
      int total = files.size();
      //for (String fileString : files) {
      
      // System.out.println("Number of files to print is: " + files.size());
      
      for (int index = 0; index < files.size(); index++){

         if (!proceed) {
            break;
         }

            // Open File.
            boolean opened = openFile(files.get(index));
            if (!opened) {
               System.out.println("Failed to open " + files.get(index));
               continue;
            }

            percentComplete = Math.round(100*((float)counter+(float)0.25)/(float)total);
            setProgress(percentComplete);

            // Track File.
            if (track && !pngs_only) {
               boolean tracked = trackFile();
               if (!tracked) {
                  System.out.println("Failed to track " + files.get(index));
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

            if (insert_kv_pairs) {
               insert_kv_pairs();
            }

            // Save File.
            if (!pngs_only) {
               boolean wrote = writeNrrd();
               if (!wrote) {
                  System.out.println("Failed to convert " + files.get(index));
                  continue;
               }
            }

            percentComplete = Math.min(Math.round(100*((float)counter+(float)1.0)/(float)total), 100);
            setProgress(percentComplete);   
            

          //DJ: generate Table =========================================
          if (this.isForHTMLManager) {

              String nrrdFileWholePathAndName = files.get(index);
              
              if(files.get(index).endsWith(".im")){
                  nrrdFileWholePathAndName = nrrdFileWholePathAndName.substring(0, nrrdFileWholePathAndName.length()-3) + ".nrrd";
              }
              
              String[] imOrNrrdFilePathAndName_split = files.get(index).split(java.util.regex.Pattern.quote("/"));
              String imOrNrrdFileName = imOrNrrdFilePathAndName_split[imOrNrrdFilePathAndName_split.length - 1];
              
              String tableString = "";
              tableString += "<TABLE ALIGN=\"LEFT\" BORDER=\"5\" CELLPADDING=\"5\" >";
              tableString += "  <TR>";
              tableString += "    <TD>";
              tableString += "       <TABLE WIDTH=\"400\" height=\"100%\">";
              tableString += "         <TR>";
              tableString += "             <TH COLSPAN=\"3\" HEIGHT=\"25\"><FONT SIZE=\"4\">";
              tableString += "                <a ALIGN=\"CENTER\" href=" + nrrdFileWholePathAndName + ">";
              tableString +=                     nrrdFileWholePathAndName.substring(nrrdFileWholePathAndName.lastIndexOf("/")+1);
              tableString += "                </a><FONT></TH>";
              tableString += "         </TR>";

              int i = 5;
              String header = com.nrims.data.ImageDataUtilities.getImageHeader(this.ui.getOpener());
              String[] headerList = header.split("\\r?\\n");
              while (i < headerList.length - 4) {
                  String[] infos = headerList[i].split(java.util.regex.Pattern.quote(":"));

                  tableString += "     <TR>";
                  tableString += "          <TD style=\"color:red\" COLOR=\"RED\" ALIGN=\"RIGHT\" LINEHEIGHT=\"5px\" WIDTH=\"50%\">";
                  tableString += "                <FONT SIZE=\"2\"> " + infos[0] + "</FONT>";
                  tableString += "          </TD>";
                  tableString += "          <TD style=\"LINE-HEIGHT:5px\" ALIGN=\"LEFT\">";
                  tableString += "                <FONT SIZE=\"2\">";

                  String details = "";
                  int idx = 1;
                  while (idx < infos.length) {
                      details += infos[idx];
                      if (idx + 1 < infos.length) {
                          details += ":";
                      }
                      idx += 1;
                  }
                  tableString += details;
                  tableString += "                </FONT>";
                  tableString += "          </TD>";
                  tableString += "     </TR>";
                  i += 1;
                  
                  // this "if" singles out the date in which the file was created.
                  if(infos[0].equals("Sample date")){
                      
                      details = details.trim();
                      String[] day_month_year = details.split("\\.");
                      
                      String day = day_month_year[0];
                      String month = day_month_year[1];
                      int year = Integer.parseInt(day_month_year[2]);

                      // DJ: fixing the two year digits date to be 4 digits for comparison purposes
                      // exp: date1: 00  and date2 = 99 ; in comparison 00 is less than 99 
                      // so we need to make 00 to be represtented as year 2000 
                      // and 99 to be represented as year 1999 so the comparison
                      // between for example year 2000 and 1999 would hold the correct result.
                      if (year >= 0 && year < 80)
                          year += 2000;
                      else 
                          year += 1900;
                      
                      tables_Not_sorted[index][0] = Integer.toString(year) + month + day;
                  }
       
                  // this "if" singles out the date in which the file was created.
                  if(infos[0].equals("Sample hour")){
                      
                      details = details.trim();
                      String[] hour_minutes = details.split(":");
                      
                      String hour    = hour_minutes[0];
                      String minutes = hour_minutes[1];
              
                      tables_Not_sorted[index][0] += "." + hour + minutes;
                  }
              }
              tableString += "      </TABLE>";
              tableString += "    </TD>";
              
              
              ArrayList<String> pngFileNames = new ArrayList<String>(); // As a reference. to be used later on for comparison and sorting. 
              HashMap<String, String> pngFileName_to_encoding_MAP = new HashMap<String, String>();
              
              final File folder = new File(getPngsDirectoryPath()); // was: pngDir
              for (final File pngFile : folder.listFiles()){
                  
                  String[] pngFilePathAndName_split = pngFile.getName().split(java.util.regex.Pattern.quote("/"));
                  String pngfilename = pngFilePathAndName_split[pngFilePathAndName_split.length-1];
                               // just to help us get the filename without extension
                  int extension_length = 0;
                  if (files.get(index).endsWith(".im")) {
                      extension_length = 3;
                  } else if (files.get(index).endsWith(".nrrd")) {
                      extension_length = 5;
                  }

                  String imOrNrrdFileName_No_Extention = imOrNrrdFileName.substring(0, imOrNrrdFileName.length() - extension_length);

                  
                  if (pngfilename.contains(imOrNrrdFileName_No_Extention)){

                      pngFileNames.add(pngfilename);
                      String png_string = "";
                      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                      try {
                          /*
                          BufferedImage pngImage = ImageIO.read(pngFile);
                          ImageIO.write(pngImage, "png", byteArrayOutputStream);
                          byteArrayOutputStream.flush();
                          
                          byte[] imageBytes = byteArrayOutputStream.toByteArray();
                          
                          
                          // using the first library
                          //BASE64Encoder encoder = new BASE64Encoder();
                          //png_string += encoder.encode(imageBytes);
                          */
                          
                          // Reading a Image file from file system
                          FileInputStream imageInFile = new FileInputStream(pngFile);
                          byte imageData[] = new byte[(int) pngFile.length()];
                          imageInFile.read(imageData);

                          // Converting Image byte array into Base64 String
                          png_string += Base64.encodeBase64String(imageData);
                          
                          //alternative base64
                          //png_string += Base64.encodeBase64URLSafeString(byteArrayOutputStream.toByteArray());
                          
                          pngFileName_to_encoding_MAP.put(pngfilename, png_string);
                          
                          byteArrayOutputStream.close();
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
/*
                      tableString += "<TD ALIGN=\"CENTER\">";
                      tableString += " <img src=\"data:image/png;base64," + png_string + "\" alt=\"" + pngFile.getName() + "\">";
                      tableString += "<BR>";
                      tableString += pngfilename.substring(imOrNrrdFileName_No_Extention.length()+1, pngfilename.length()-4);
                      tableString += "<BR>&nbsp";
                      tableString += "</TD>";
*/
                  }
              }/*
               // at this step, we reposition the hsi's to be last in the list
              String[] pngFileNamesArray = pngFileNames.toArray(new String[pngFileNames.size()]);
              
              Arrays.sort(pngFileNamesArray);
              ArrayList<String> pngFileNames_sorted = new ArrayList<String>();
              ArrayList<String> png_hsis = new ArrayList<String>();
              
              String the_zero_mass_image = null;
              
              for(int m = 0 ; m < pngFileNamesArray.length; m++){
                  if(pngFileNamesArray[m].contains("hsi") == false){
                      if(pngFileNamesArray[m].contains("_m0_") == false){
                        pngFileNames_sorted.add(pngFileNamesArray[m]);
                      } else{
                          the_zero_mass_image = pngFileNamesArray[m];
                      }
                      
                  }else
                      png_hsis.add(pngFileNamesArray[m]);
              }
              if(the_zero_mass_image != null)
                pngFileNames_sorted.add(the_zero_mass_image);
              // now, we add the png_hsis to the end.
              pngFileNames_sorted.addAll(png_hsis);
              */
              
              // at this step, we re-position the hsi's to be first in the list of images we display
              String[] pngFileNamesArray = pngFileNames.toArray(new String[pngFileNames.size()]);
              
              Arrays.sort(pngFileNamesArray);
              ArrayList<String> pngFileNames_sorted = new ArrayList<String>();
              ArrayList<String> png_masses = new ArrayList<String>();
              
              String the_zero_mass_image = null;
              
              for(int m = 0 ; m < pngFileNamesArray.length; m++){
                  if(pngFileNamesArray[m].contains("hsi") == true){
                      pngFileNames_sorted.add(pngFileNamesArray[m]);
                  }else
                      if(pngFileNamesArray[m].contains("_m0_") == false){
                        png_masses.add(pngFileNamesArray[m]);
                      } else{
                        the_zero_mass_image = pngFileNamesArray[m];
                      }
                      
              }
              // now, we add the png_hsis to the end.
              pngFileNames_sorted.addAll(png_masses);
              // now, we add the zero mass if it exists
              if(the_zero_mass_image != null)
                pngFileNames_sorted.add(the_zero_mass_image);
              
              
              // embedding the pngs into the html web page
              for(int k=0 ; k<pngFileNames_sorted.size() ; k++){
                      tableString += "<TD ALIGN=\"CENTER\">";
                      tableString += " <img src=\"data:image/png;base64," + pngFileName_to_encoding_MAP.get(pngFileNames_sorted.get(k)) + "\" alt=\"" + pngFileNames_sorted.get(k) + "\">";
                      tableString += "<BR>";
                      tableString += pngFileNames_sorted.get(k).substring(pngFileNames_sorted.get(k).indexOf("_m")+1, pngFileNames_sorted.get(k).length()-4);
                      tableString += "<BR>&nbsp";
                      tableString += "</TD>";
              }
              
              tableString += "  </TR>";
              tableString += "</TABLE><BR><BR>";
              
              tables_Not_sorted[index][1] = tableString;
              
             // this.htmlTables += tableString;
          } //============================================================
           
            ui.closeCurrentImage();
            counter++;
      }
      
      //DJ: just for debugging purposes:
      //for (int index = 0; index < files.size(); index++){
      //    System.out.println(tables_Not_sorted[index][1]);
      //}

      
      // DJ: enhancement - sorting by the date and time of creation.
      // Most recent to the most old.
      
      double[] datesTimesArray = new double[tables_Not_sorted.length];
      for(int x = 0 ; x<tables_Not_sorted.length; x++){
          datesTimesArray[x] = Double.parseDouble(tables_Not_sorted[x][0]);
      }
      Arrays.sort(datesTimesArray);
      
       for (int dt = datesTimesArray.length - 1; dt >= 0; dt--) {
           for (int j = 0; j < tables_Not_sorted.length; j++) {
               double d1 = datesTimesArray[dt];
               double d2 = Double.parseDouble(tables_Not_sorted[j][0]);
               int retval = Double.compare(d1, d2);

               if (retval == 0) {
                   this.htmlTables += tables_Not_sorted[j][1];
               }
           }
       }
       
       //DJ just for debugging:
       //System.out.println(htmlTables);

       if (com.nrims.managers.convertManager.getInstance() != null) {

           StringBuilder builder = new StringBuilder(this.generateHTML());

           String html = builder.toString();
           try {
               PrintWriter writer = new PrintWriter(new File(htmlFile));
               writer.println(html);
               writer.close();

           } catch (IOException ex) {
               Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
           }
           
           
           
           // DJ: 09/06/2014
           
           // copy the web page link to clipboard:
           StringSelection stringSelection = new StringSelection(htmlFile);
           Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
           clipboard.setContents(stringSelection, null);
           
           // Make a new small frame to inform the user that the link was copied to
           // his/her clipboard and ask weither to open it in a browser or not.
           ij.gui.GenericDialog messageBox = new ij.gui.GenericDialog("DONE");
           String message = "SUCCESS...WEB PAGE CREATED (link is copied to your clipboard)";
           message += "\n\nWOULD YOU LIKE TO OPEN THE LINK IN A WEB BROWSER ?";
           messageBox.addMessage(message);
           messageBox.setOKLabel("YES");
           messageBox.setCancelLabel("NO");
           messageBox.centerDialog(true);

           messageBox.showDialog();
           
           // In case the user agrees to open the link in a browser
           if (messageBox.wasOKed()) {
               java.awt.Desktop desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
               try {
                   if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                       try {
                           //java.net.URI uri = new java.net.URI("http://www.google.com"); // test
                           java.net.URI uri = new java.net.URI("file://" + htmlFile);
                           desktop.browse(uri);
                       } catch (Exception e) {
                           e.printStackTrace();
                       }
                   }
               } catch (Exception e) {
                   e.printStackTrace();
               }
           }
  
           // in case we collected the specs from OpenMIMS,
           // we just remove the pngs and their folder which are located
           // in the "tmp" directory.
           if(configSpecs == false){
               // delete the tmp directory that has the pngs
               File directory = new File(pngDir);
               for (File pngFile : directory.listFiles()) {
                   pngFile.delete();
               }
               directory.delete();
           }
       }

      return null;
   }
}
