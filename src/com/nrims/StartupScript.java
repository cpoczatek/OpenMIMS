package com.nrims;

import ij.IJ;
import ij.WindowManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.swing.*;

public class StartupScript extends Thread {

   private static String LIST_FILE_NAME = "/nrims/home3/zkaufman/TrackingDocuments/LEE/EXP19/filelist.txt";

   UI ui;
   BufferedReader br;
   DataInputStream in;   
   File trackingFile;
   final javax.swing.JDialog dialog;
   JLabel label = new JLabel("");
   File imFile = null;
   RatioProps[] rto_props;
   HSIProps[] hsi_props;
   SumProps[] sum_props;

   public StartupScript(UI ui) {
      this.ui = ui;

      // Get the object of DataInputStream
      //trackingFile = new File(FILE_DIRECTORY, LIST_FILE_NAME);
      trackingFile = new File(LIST_FILE_NAME);
      if (trackingFile.exists()) {
         try {
            FileInputStream fstream = new FileInputStream(trackingFile);
            in = new DataInputStream(fstream);
            br = new BufferedReader(new InputStreamReader(in));
         } catch (Exception e) {
         }
      } else {
         System.out.println(trackingFile.getAbsolutePath() + " does not exist.");
         System.exit(0);
      }

      //Create the dialog.
      dialog = new javax.swing.JDialog(ui, "A Non-Modal Dialog");
      label.setHorizontalAlignment(JLabel.CENTER);
      Font font = label.getFont();
      label.setFont(label.getFont().deriveFont(font.PLAIN, 14.0f));

      JButton closeButton = new JButton("Next");
      closeButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            next();
         }
      });
      JPanel closePanel = new JPanel();
      closePanel.setLayout(new BoxLayout(closePanel, BoxLayout.LINE_AXIS));
      closePanel.add(Box.createHorizontalGlue());
      closePanel.add(closeButton);
      closePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));

      JPanel contentPane = new JPanel(new BorderLayout());
      contentPane.add(label, BorderLayout.CENTER);
      contentPane.add(closePanel, BorderLayout.PAGE_END);
      contentPane.setOpaque(true);
      dialog.setContentPane(contentPane);
      dialog.setSize(new Dimension(300, 150));
   }

   // This method is called when the thread runs
   public void run() {
      dialog.setVisible(true);
   }

   public void next() {

      ui.setIsSum(true);
      ui.setMedianFilterRatios(true);
      ui.setMedianFilterRadius(1);

      String imfileName = "tempName";

      while (imfileName != null) {

      // Save nrrd.
      if (imFile != null) {
         File nrrdFile = new File(imFile.getParent(), ui.getImageFilePrefix() + UI.NRRD_EXTENSION);
         System.out.println("Writing nrrd: " + nrrdFile.getAbsolutePath());
         ui.saveSession(nrrdFile.getAbsolutePath(), true);
         if (!nrrdFile.exists())
            IJ.error(nrrdFile.getAbsolutePath() + " does not exit.");
         ui.exportPNGs() ;
      }
         
      try {

         //Read File Line By Line
         imfileName = br.readLine();
         if (imfileName == null || imfileName.length() == 0) {
            System.out.println("Possibly EOF.");
            System.exit(0);
         }
         imFile = new File(imfileName);
         label.setText(imfileName);

         // If file does not exist, print and continue.
         if (!imFile.exists()) {
            System.out.println(imFile.getAbsolutePath() + " does not exist.");
            return;
         }
         System.out.println(imFile.getAbsolutePath());

         rto_props = ui.getOpenRatioProps();
         hsi_props = ui.getOpenHSIProps();
         sum_props = ui.getOpenSumProps();
         
         // Open.
         ui.loadMIMSFile(imFile);               
         
         // Track.
         boolean track = false;
         if (track) {
         int indexToTrack = 2;
         if (ui.getOpener().getNImages() > 1) {
            ui.getOpenMassImages()[indexToTrack].getWindow().toFront();
            WindowManager.setCurrentWindow(ui.getOpenMassImages()[indexToTrack].getWindow());
            WindowManager.setTempCurrentImage(ui.getOpenMassImages()[indexToTrack]);
            ui.getmimsStackEditing().showTrackManager();
            ActionEvent ae = new ActionEvent(ui.getmimsStackEditing().atManager.okButton, -1, "OK");
            ui.getmimsStackEditing().atManager.actionPerformed(ae);
            while (ui.getmimsStackEditing().THREAD_STATE == ui.getmimsStackEditing().WORKING) {
              try {
                 System.out.println("tracking...");
                 Thread.sleep(1000);
              } catch (Exception e) {}
            }
         }
         }
         
         // Generate all images that were previously open.
         //ui.restoreState(rto_props, hsi_props, sum_props);

         // Mass indexes.
         int m12idx = 0; int m13idx = 1; int m26idx = 2;
         int m27idx = 3;  int m31idx = 4; int m32idx = 5;
         int m81idx = 6;  //int m80idx = 4;  int m82idx = 5;
         
         // Sum image m26.
         SumProps sumProps26 = new SumProps(m26idx);
         sumProps26.setRatioScaleFactor(10000);
         MimsPlus sp26 = new MimsPlus(ui, sumProps26, null);
         sp26.showWindow();
         
         // Sum image m31.
         SumProps sumProps31 = new SumProps(m31idx);
         sumProps31.setRatioScaleFactor(10000);
         MimsPlus sp31 = new MimsPlus(ui, sumProps31, null);
         sp31.showWindow();

         // Sum image m32.
         SumProps sumProps32 = new SumProps(m32idx);
         sumProps32.setRatioScaleFactor(10000);
         MimsPlus sp32 = new MimsPlus(ui, sumProps32, null);
         sp32.showWindow();

         // Sum image m81.
         SumProps sumProps81 = new SumProps(m81idx);
         sumProps81.setRatioScaleFactor(10000);
         MimsPlus sp81 = new MimsPlus(ui, sumProps81, null);
         sp81.showWindow();
         
         // HSI image 27/26
         HSIProps hsiProps2726 = new HSIProps(m27idx, m26idx);
         hsiProps2726.setRatioScaleFactor(10000);
         hsiProps2726.setMinRatio(37);
         hsiProps2726.setMaxRatio(100);
         hsiProps2726.setMaxRGB(44);
         hsiProps2726.setMinRGB(0);
         hsiProps2726.setNumThreshold(0);
         hsiProps2726.setDenThreshold(0);
         MimsPlus mp2726 = new MimsPlus(ui, hsiProps2726);
         mp2726.showWindow();

         MimsPlus[] sp = ui.getOpenSumImages();
         for (int i = 0; i < sp.length; i++)
            ui.autoContrastImage(sp[i]);

      } catch (Exception e) {
         e.printStackTrace();
      }
      }
   }
}