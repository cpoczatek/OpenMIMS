package com.nrims;

import ij.IJ;
import ij.WindowManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.swing.*;

public class StartupScript extends Thread {

   private static String FILE_DIRECTORY = "/nrims/home3/zkaufman/DOCS/TrackingDocuments/Girguis_Exp3";
   private static String LIST_FILE_NAME = "filelist.txt";

   UI ui;
   BufferedReader br;
   DataInputStream in;   
   File trackingFile;
   final javax.swing.JDialog dialog;
   JLabel label = new JLabel("");
   File imFile = null;
   File directory = new File(FILE_DIRECTORY);
   RatioProps[] rto_props;
   HSIProps[] hsi_props;
   SumProps[] sum_props;

   public StartupScript(UI ui) {
      this.ui = ui;

      // Get the object of DataInputStream
      trackingFile = new File(FILE_DIRECTORY, LIST_FILE_NAME);
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

      String imfileName = "tempName";
      //while (imfileName != null) {

      // Save nrrd.
      if (imFile != null) {
         File nrrdFile = new File(directory, ui.getImageFilePrefix() + ui.NRRD_EXTENSION);
         System.out.println("Writing nrrd: " + nrrdFile.getName());
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
         imFile = new File(directory, imfileName);
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
         if (ui.getOpener().getNImages() > 1) {
            ui.getOpenMassImages()[0].getWindow().toFront();
            WindowManager.setCurrentWindow(ui.getOpenMassImages()[0].getWindow());
            WindowManager.setTempCurrentImage(ui.getOpenMassImages()[0]);
            ui.getmimsStackEditing().showTrackManager();
            ActionEvent ae = new ActionEvent(ui.getmimsStackEditing().atManager.okButton, -1, "OK");
            ui.getmimsStackEditing().atManager.actionPerformed(ae);
            while (ui.getmimsStackEditing().THREAD_STATE == ui.getmimsStackEditing().WORKING) {
              try {
                  System.out.println("tracking...");
                  Thread.sleep(250);
              } catch (Exception e) {}
            }
         }
         
         // Generate all images that were previously open.
         ui.restoreState(rto_props, hsi_props, sum_props);
         
         // Mass indexes.
         //int m12idx = -1; int m13idx = -1; int m26idx = 0;
         //int m27idx = 1;  int m28idx = -1; int m31idx = 2;
         //int m76idx = 3;  int m80idx = 4;  int m82idx = 5;
         
         // Sum image m12.
         //SumProps sumProps12 = new SumProps(m12idx);
         //sumProps12.setRatioScaleFactor(ui.getRatioScaleFactor());
         //MimsPlus sp12 = new MimsPlus(ui, sumProps12, null);
         //sp12.showWindow();

         // Sum image m26.
         //SumProps sumProps26 = new SumProps(m26idx);
         //sumProps26.setRatioScaleFactor(ui.getRatioScaleFactor());
         //MimsPlus sp26 = new MimsPlus(ui, sumProps26, null);
         //sp26.showWindow();

         // Sum image m28.
         //SumProps sumProps28 = new SumProps(m28idx);
         //sumProps28.setRatioScaleFactor(ui.getRatioScaleFactor());
         //MimsPlus sp28 = new MimsPlus(ui, sumProps28, null);
         //sp28.showWindow();
         
         // Sum image m31.
         //SumProps sumProps31 = new SumProps(m31idx);
         //sumProps31.setRatioScaleFactor(ui.getRatioScaleFactor());
         //MimsPlus sp31 = new MimsPlus(ui, sumProps31, null);
         //sp31.showWindow();

         //MimsPlus hsi[] = ui.getOpenHSIImages();
         //for(int i = 0; i < hsi.length; i++) {
         //   HSIProps hsiprops = hsi[i].getHSIProcessor().getHSIProps();
         //   hsiprops.setDataFileName(imfileName);
         //   MimsPlus mp = new MimsPlus(ui, hsiprops);
         //   mp.showWindow();
         //}

         // HSI image 13/12
         //HSIProps hsiProps1312 = new HSIProps(m13idx, m12idx);
         //hsiProps1312.setMaxRatio(150);
         //hsiProps1312.setMaxRatio(106);
         //hsiProps1312.setRatioScaleFactor(ui.getRatioScaleFactor());
         //hsiProps1312.setMinRatio(106);
         //hsiProps1312.setMaxRatio(150);
         //hsiProps1312.setMaxRGB(58);
         //MimsPlus mp1312 = new MimsPlus(ui, hsiProps1312);
         //mp1312.showWindow();

         /*
         // HSI image 27/26
         HSIProps hsiProps2726 = new HSIProps(m27idx, m26idx);
         hsiProps2726.setRatioScaleFactor(10000);
         hsiProps2726.setMinRatio(37);
         hsiProps2726.setMaxRatio(100);
         hsiProps2726.setMaxRGB(58);
         hsiProps2726.setMinDen(0);
         hsiProps2726.setMinNum(0);
         MimsPlus mp2726 = new MimsPlus(ui, hsiProps2726);
         mp2726.showWindow();

         // HSI image 76/80
         HSIProps hsiProps7680 = new HSIProps(m76idx, m80idx);
         hsiProps7680.setRatioScaleFactor(1);
         hsiProps7680.setMinRatio(0);
         hsiProps7680.setMaxRatio(1);
         hsiProps7680.setMinDen(0);
         hsiProps7680.setMinNum(0);
         hsiProps7680.setMaxRGB(20);
         MimsPlus mp7680 = new MimsPlus(ui, hsiProps7680);
         mp7680.showWindow();

         // HSI image 82/80
         HSIProps hsiProps8280 = new HSIProps(m82idx, m80idx);
         hsiProps8280.setRatioScaleFactor(1);
         hsiProps8280.setMinRatio(0);
         hsiProps8280.setMaxRatio(1);
         hsiProps8280.setMinDen(0);
         hsiProps8280.setMinNum(0);
         hsiProps8280.setMaxRGB(20);
         MimsPlus mp8280 = new MimsPlus(ui, hsiProps8280);
         mp8280.showWindow();
         */
         
         // Tile windows.
         //ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
         //wo.run("tile");

         ui.autocontrastAllImages();

      } catch (Exception e) {
         e.printStackTrace();
      }
      //}
   }
}