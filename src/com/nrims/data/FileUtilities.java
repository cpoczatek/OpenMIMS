/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims.data;

import com.nrims.HSIProps;
import com.nrims.MimsPlus;
import com.nrims.RatioProps;
import com.nrims.SumProps;
import com.nrims.UI;
import static com.nrims.UI.HSI_EXTENSION;
import static com.nrims.UI.MIMS_EXTENSION;
import static com.nrims.UI.NRRD_EXTENSION;
import static com.nrims.UI.RATIO_EXTENSION;
import static com.nrims.UI.ROIS_EXTENSION;
import static com.nrims.UI.SESSIONS_EXTENSION;
import static com.nrims.UI.SUM_EXTENSION;
import ij.gui.Roi;
import java.beans.DefaultPersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author wang2
 */
public class FileUtilities {
    /**
     * getNext: modified version of getNext function from NextImageOpener plugin in ImageJ, modified for OpenMIMS
     * @param path
     * @param imageName
     * @param forward
     * @return 
     */
    public static String getNext(String path, String imageName, boolean forward){
        File dir = new File(path);
        if (!dir.isDirectory()) return null;
        String[] names = dir.list();
        ij.util.StringSorter.sort(names);
        int thisfile = -1;
        for (int i=0; i<names.length; i++) {
            if (names[i].equals(imageName)) {
                thisfile = i;
                break;
            }
        }
         //System.out.println("OpenNext.thisfile:" + thisfile);
        if(thisfile == -1) return null;// can't find current image
        
        // make candidate the index of the next file
        int candidate = thisfile + 1;
        if (!forward) candidate = thisfile - 1;
        if (candidate<0) candidate = names.length - 1;
        if (candidate==names.length) candidate = 0;
        // keep on going until an image file is found or we get back to beginning
        while (candidate!=thisfile) {
            String nextPath = path + "/" + names[candidate];
             //System.out.println("OpenNext: "+ candidate + "  " + names[candidate]);
            File nextFile = new File(nextPath);
            boolean canOpen = true;
            if (names[candidate].startsWith(".") || nextFile.isDirectory())
                canOpen = false;
            if (canOpen) {
                 String fileName = nextFile.getName();
                    if (fileName.endsWith(NRRD_EXTENSION) || fileName.endsWith(MIMS_EXTENSION)) {
                    }else{
                        canOpen = false;
                    }
            }
            if (canOpen)
                    return nextPath;
            else {// increment again
                if (forward)
                    candidate = candidate + 1;
                else
                    candidate = candidate - 1;
                if (candidate<0) candidate = names.length - 1;
                if (candidate == names.length) candidate = 0;
            }
            
        }
        //System.out.println("OpenNext: Search failed");
        return null;
    }
     /**
     * Helper function for reading .sum/.ratio/.hsi files from xml.
     * @param file the file to read
     * @return the object read from the file
     */
    public static Object readObjectFromXML(File file){
        Object obj = null;
        try {
            XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
            obj = xmlDecoder.readObject();
            xmlDecoder.close();
        } catch (Exception e) {
            try{
            FileInputStream f_in = new FileInputStream(file);
            ObjectInputStream obj_in = new ObjectInputStream(f_in);
            obj = obj_in.readObject();
            }catch(Exception ex){
                obj = null;
            }
        }finally {
            return obj;
        }
    }
    /**
    * Takes a props object and adds it to a zip.
    * Used in Save Session.
    * depends on no globals, can probably move to new helper class
    * @param zos the ZipOutputStream for the final zip
    * @param toWrite the props object to be written
    * @param cls the class of the props object
    * @param params the parameters for the constructor of the props object
    * @param filenames the filenames of similar props objects
    * @param extension the extension for the props object
    * @param numName mass string for the numerator (or if a sum of a mass image, the parent)
    * @param denName mass string for the numerator (or if a sum of a mass image, -1)
    * @param filename the name of the .nrrd file
    * @param i index of the object in filenames
    * @return true if save succeeded, or false if an error was thrown
    */
    
    public static boolean saveToXML(ZipOutputStream zos, Object toWrite, Class<?> cls, String[] params, String[] filenames, String extension, String numName, String denName, String filename, int i){
        try {
            int numMass = Math.round(new Float(numName));
            int denMass = Math.round(new Float(denName));
            if (denMass != -1) filenames[i] = filename + "_m" + numMass + "_m" + denMass;
            else filenames[i] = filename + "_m" + numMass;
            int numBefore = 0;
            for (int j = 0; j < i; j++) if (filenames[i].equals(filenames[j])) numBefore++;
            String post = "";
            if (numBefore > 0) post = "(" + numBefore + ")";
            zos.putNextEntry(new ZipEntry(filenames[i] + post + extension));
            XMLEncoder e = new XMLEncoder(zos);
            //need to modify persistance delegate to deal with constructor in SumProps which takes parameters
            e.setPersistenceDelegate(cls, new DefaultPersistenceDelegate(params));
            e.writeObject(toWrite);
            e.flush();
            //need to append "</java>" to the end because flushing doesn't "complete" the file like close does
            //but we cannot close or else the entire ZipOutputstream closes
            DataOutputStream d_out = new DataOutputStream(zos);
            d_out.writeBytes("</java>");
            d_out.flush();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
        /**
     * Given a zip file, extracts XML files from within and converts to Objects.
     * Used in opening .session.zip files
     * Depends on no globals, can probably move to new helper class
     * @param file absolute file path
     * @return array list containing Objects.
     */
    public static ArrayList openXMLfromZip(File file) {
        ArrayList entries = new ArrayList();
        Object obj;
        try {
            ZipFile z_file = new ZipFile(file);
            ZipInputStream z_in = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry;
            XMLDecoder xmlDecoder;
            while ((entry = z_in.getNextEntry()) != null) {
                xmlDecoder = new XMLDecoder(z_file.getInputStream(entry));
                obj = xmlDecoder.readObject();
                entries.add(obj);
                xmlDecoder.close();
            }
            z_in.close();
        } catch (Exception e) {
            return null;
        } finally {
            return entries;
        }
    }
    /**
    * AutoSaveROI is the thread which is responsible for autosaving the ROI's
    */
    public static class AutoSaveROI implements Runnable {
        UI ui;
        public AutoSaveROI(UI ui){
            this.ui = ui;
        }
        public void run(){
            for (;;){
                try {
                    // Save the ROI files to zip.
                    String roisFileName = System.getProperty("java.io.tmpdir")+"/"+ui.getImageFilePrefix();
                    Roi[] rois = ui.getRoiManager().getAllROIs();
                    if (rois.length > 0){
                       checkSave(roisFileName + ROIS_EXTENSION, roisFileName, 1);
                       ui.getRoiManager().saveMultiple(rois, roisFileName + ROIS_EXTENSION, false);
                       //threadMessage("Autosaved at "+ roisFileName + ROIS_EXTENSION);
                    }else{
                        //threadMessage("Nothing to autosave");
                    }
                    Thread.sleep(ui.getInterval());    
                } catch (InterruptedException e){
                    //ui.threadMessage("Autosave thread interrupted");
                    break;
                }
            }
        }
    }
    public static boolean checkSave(String toSave, String filename, int n) {
        File file = new File(toSave);
        String newFilename = filename + "(" + n + ")" + ROIS_EXTENSION;
        // File (or directory) with new name
        File file2 = new File(newFilename);
        if (file2.exists() && n < 10) {
            checkSave(newFilename, filename, n + 1);
        }
        if (file.exists()) {
            boolean success = file.renameTo(file2);
            if (success) {
                file = new File(toSave);
                file.delete();
            }
        }
        return true;
    }
    public static String getMachineName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }
       /**
    * Returns the prefix of any file name.
    * For example: /tmp/test_file.im = /tmp/test_file
    *
    * @return prefix file name.
    */
   public static String getFilePrefix(String fileName) {
      String prefix = fileName.substring(0, fileName.lastIndexOf("."));
      return prefix;
   }
   /* public static boolean saveAdditionalData(String dataFileName, Opener image, MimsRoiManager roiM, MimsPlus[] ratio, MimsPlus[] hsi, MimsPlus[] sum){
          File sessionFile = null;
          String[] names = image.getMassNames();
          // Save the ROI files to zip.
          if (rois.length > 0) {
              System.out.println("ROIS exist, going to open window");
              if ((sessionFile = checkForExistingFiles(ROIS_EXTENSION, baseFileName, "Mims roi zips"))!= null) {
                  baseFileName = getFilePrefix(getFilePrefix(sessionFile.getAbsolutePath()));
                  getRoiManager().saveMultiple(rois, baseFileName + ROIS_EXTENSION, false);
              } else {
                  System.out.println("Saving roi.zip canceled.");
              }
          }
          if (ratio.length + hsi.length + sum.length > 0){
              if ((sessionFile = checkForExistingFiles(SESSIONS_EXTENSION, baseFileName, "Mims session files")) != null) {
                  baseFileName = getFilePrefix(getFilePrefix(sessionFile.getAbsolutePath()));
                  onlyFileName = getFilePrefix(getFilePrefix(sessionFile.getName()));
              } else {
                  System.out.println("Saving session.zip canceled.");
              }
              // Contruct a unique name for each ratio image and save into a ratios.zip file
              ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(baseFileName + SESSIONS_EXTENSION)));

              if (ratio.length > 0) {
                  String[] filenames = new String[ratio.length];
                  for (int i = 0; i < ratio.length; i++) {
                      RatioProps ratioprops = ratio[i].getRatioProps();
                      ratioprops.setDataFileName(dataFileName);
                      if (!FileUtilities.saveToXML(zos, ratioprops, RatioProps.class, new String[]{"numMassIdx", "denMassIdx"}, filenames,
                              RATIO_EXTENSION, names[ratioprops.getNumMassIdx()], names[ratioprops.getDenMassIdx()], onlyFileName, i))
                          return false;
                  }
              }

              // Contruct a unique name for each hsi image and save.
              if (hsi.length > 0) {
                  String[] filenames = new String[hsi.length];
                  for (int i = 0; i < hsi.length; i++) {
                      HSIProps hsiprops = hsi[i].getHSIProps();
                      hsiprops.setDataFileName(dataFileName);
                      if (!FileUtilities.saveToXML(zos, hsiprops, HSIProps.class, new String[]{"numMassIdx", "denMassIdx"}, filenames,
                              HSI_EXTENSION, names[hsiprops.getNumMassIdx()], names[hsiprops.getDenMassIdx()], onlyFileName, i))
                          return false;
                  }
              }

              // Contruct a unique name for each sum image and save.
              if (sum.length > 0) {
                  String[] filenames = new String[sum.length];
                  for (int i = 0; i < sum.length; i++) {
                      SumProps sumProps = sum[i].getSumProps();
                      sumProps.setDataFileName(dataFileName);
                      if (sumProps.getSumType() == SumProps.RATIO_IMAGE) {
                          if (!FileUtilities.saveToXML(zos, sumProps, SumProps.class, new String[]{"numMassIdx", "denMassIdx"}, filenames,
                                  SUM_EXTENSION, names[sumProps.getNumMassIdx()], names[sumProps.getDenMassIdx()], onlyFileName, i))
                              return false;
                      } else if (sumProps.getSumType() == SumProps.MASS_IMAGE) {
                          if (!FileUtilities.saveToXML(zos, sumProps, SumProps.class, new String[]{"parentMassIdx"}, filenames,
                                  SUM_EXTENSION, names[sumProps.getParentMassIdx()], "-1", onlyFileName, i))
                              return false;
                      } else continue;
                  }

              }
              zos.flush();
              zos.close();
          }
    }*/
}
