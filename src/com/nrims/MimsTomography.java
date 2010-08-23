package com.nrims;

/*
 * mimsTomography.java
 *
 * Created on December 20, 2007, 3:00 PM
 */

import com.nrims.data.Opener;
import com.nrims.plot.MimsChartFactory;
import com.nrims.plot.MimsChartPanel;
import com.nrims.plot.MimsXYPlot;

import ij.gui.Roi;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;

/**
 * @author  cpoczatek
 */
public class MimsTomography extends javax.swing.JPanel {

    private UI ui;
    private Opener image;
    MimsJFreeChart tomoChart = null;
    MimsJTable table = null;
    private JFreeChart chart;
    private MimsChartPanel chartPanel;
    
    /** Creates new form mimsTomography */
    public MimsTomography(UI ui) {
        initComponents();
        setupHistogram();
        
        this.ui = ui;
        this.image = ui.getOpener();
        tomoChart = null;

        imageJList.setModel(new DefaultListModel());
        imageJList.setCellRenderer(new ImageListRenderer());
    }
    
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      jLabel3 = new javax.swing.JLabel();
      jScrollPane1 = new javax.swing.JScrollPane();
      statJList = new javax.swing.JList();
      jScrollPane2 = new javax.swing.JScrollPane();
      imageJList = new javax.swing.JList();
      jLabel4 = new javax.swing.JLabel();
      plotButton = new javax.swing.JButton();
      appendCheckBox = new javax.swing.JCheckBox();
      jButton1 = new javax.swing.JButton();
      jTextField1 = new javax.swing.JTextField();
      jLabel5 = new javax.swing.JLabel();
      currentPlaneCheckBox = new javax.swing.JCheckBox();
      jSeparator1 = new javax.swing.JSeparator();
      histogramjPanel = new javax.swing.JPanel();
      histogramUpdatejCheckBox = new javax.swing.JCheckBox();
      profilejButton = new javax.swing.JButton();

      setToolTipText("");

      jLabel3.setText("Statistics to plot");

      statJList.setModel(new javax.swing.AbstractListModel() {
         String[] strings = { "mean", "stddev", "min", "max", "sum", "mode", "area", "group (table only)", "xcentroid", "ycentroid", "xcentermass", "ycentermass", "roix", "roiy", "roiwidth", "roiheight", "major", "minor", "angle", "feret", "median", "kurtosis", "areafraction", "perimeter" };
         public int getSize() { return strings.length; }
         public Object getElementAt(int i) { return strings[i]; }
      });
      jScrollPane1.setViewportView(statJList);

      jScrollPane2.setViewportView(imageJList);

      jLabel4.setText("Masses");

      plotButton.setText("Plot");
      plotButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            plotButtonActionPerformed(evt);
         }
      });

      appendCheckBox.setText("Append");

      jButton1.setText("Table");
      jButton1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton1ActionPerformed(evt);
         }
      });

      jLabel5.setText("Planes (eg: 2,4,8-25,45...)");

      currentPlaneCheckBox.setText("Current plane only");

      jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

      histogramjPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

      javax.swing.GroupLayout histogramjPanelLayout = new javax.swing.GroupLayout(histogramjPanel);
      histogramjPanel.setLayout(histogramjPanelLayout);
      histogramjPanelLayout.setHorizontalGroup(
         histogramjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 364, Short.MAX_VALUE)
      );
      histogramjPanelLayout.setVerticalGroup(
         histogramjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 258, Short.MAX_VALUE)
      );

      histogramUpdatejCheckBox.setText("AutoUpdate Histogram");

      profilejButton.setText("Line Profile");
      profilejButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            profilejButtonActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addComponent(jLabel3))
                     .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel5)
                     .addComponent(currentPlaneCheckBox)
                     .addGroup(layout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addComponent(jLabel4))
                     .addGroup(layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(jTextField1))))
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(plotButton, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(appendCheckBox)))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(histogramUpdatejCheckBox)
               .addComponent(histogramjPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addComponent(profilejButton, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 377, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addGap(22, 22, 22)
                  .addComponent(histogramjPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(histogramUpdatejCheckBox)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(profilejButton))
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(jLabel4)
                     .addComponent(jLabel3))
                  .addGap(10, 10, 10)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(currentPlaneCheckBox))
                     .addComponent(jScrollPane1))
                  .addGap(20, 20, 20)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(plotButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                     .addGroup(layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(appendCheckBox)))))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

       private void setupHistogram() {
        // Create arbitrary dataset
        HistogramDataset dataset = new HistogramDataset();

        // Create chart using the ChartFactory
        chart = MimsChartFactory.createMimsHistogram("", "Pixel Value", "", dataset, PlotOrientation.VERTICAL, true, true, false);
        chart.setBackgroundPaint(this.getBackground());

        MimsXYPlot plot = (MimsXYPlot) chart.getPlot();
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardXYBarPainter());

        // Listen for key pressed events.
       KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
          public boolean dispatchKeyEvent(KeyEvent e) {
             if (e.getID() == KeyEvent.KEY_PRESSED) {
                chartPanel.keyPressed(e);
             }
             return false;
          }
       });

        // Movable range and domain.
        plot.setDomainPannable(true);
        plot.setRangePannable(true);

        chartPanel = new MimsChartPanel(chart);
        chartPanel.setSize(350, 250);
        histogramjPanel.add(chartPanel);
    }

    public void updateHistogram(double[] pixelvalues, String label, boolean forceupdate) {
       if(pixelvalues == null) {
          return;
       } else if (pixelvalues.length == 0) {
          return;
       }
       if (forceupdate || histogramUpdatejCheckBox.isSelected()) {
          HistogramDataset dataset = new HistogramDataset();

          dataset.addSeries(label, pixelvalues, 100);

          MimsXYPlot plot = (MimsXYPlot) chart.getPlot();
          plot.setDataset(dataset);

          chart.fireChartChanged();
       }
    }

    private void plotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotButtonActionPerformed

       // Initialize chart.
       if (!appendCheckBox.isSelected() || tomoChart == null) {
          tomoChart = new MimsJFreeChart(ui);
       }

       // Determine planes.
       ArrayList planes = getPlanes();
       if (planes.size() >= 1) {
          tomoChart.setPlanes(planes);
       } else {
          System.out.println("Undetermined planes");
          return;
       }

       // Get selected stats.
       String[] statnames = getStatNames();
       if (statnames.length >= 1) {
          tomoChart.setStats(statnames);
       } else {
          System.out.println("No stats selected");
          return;
       }

       // Get selected rois.
       MimsRoiManager rm = ui.getRoiManager();
       Roi[] rois = rm.getSelectedROIs();
       if (rois.length == 0)
          rois = rm.getAllListedROIs();
       if (rois.length >= 1) {
          tomoChart.setRois(rois);
       } else {
          System.out.println("No rois selected");
          return;
       }

       // images
       MimsPlus[] images = getImages();
       if (images.length >= 1) {
          tomoChart.setImages(images);
       } else {
          System.out.println("No images selected");
          return;
       }

       int currentPlane = ui.getMassImage(0).getSlice();
       tomoChart.plotData(appendCheckBox.isSelected());
       
       // Fast forward stack by one slice if we are appending current plane.
       if((currentPlaneCheckBox.isSelected()) && ((currentPlane+1) <= ui.getMassImage(0).getStackSize())) {
           ui.getMassImage(0).setSlice(currentPlane + 1);
       }
    }//GEN-LAST:event_plotButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

       // initialize variables.
       MimsRoiManager rm = ui.getRoiManager();

       if (!appendCheckBox.isSelected() || table == null) {
          table = new MimsJTable(ui);
       }

       // Determine planes.
       ArrayList planes = getPlanes();
       if (planes.size() >= 1) {
          table.setPlanes(planes);
       } else {
          System.out.println("Undetermined planes");
          return;
       }

       // Get selected stats.
       String[] statnames = getStatNames();
       if (statnames.length >= 1) {
          table.setStats(statnames);
       } else {
          System.out.println("No stats selected");
          return;
       }

       // Get selected rois.
       Roi[] rois = rm.getSelectedROIs();
       if (rois.length == 0)
          rois = rm.getAllListedROIs();
       if (rois.length >= 1) {
          table.setRois(rois);
       } else {
          System.out.println("No rois selected");
          return;
       }

       // Get selected images.
       MimsPlus[] images = getImages();
       if (images.length >= 1) {
          table.setImages(images);
       } else {
          System.out.println("No images selected");
          return;
       }

       // Is at least 1 mass or ratio image present.
       boolean isOneMassOrRatioImagePresent = false;
       for (int i = 0; i < images.length; i++)
          if (images[i].getMimsType() == MimsPlus.MASS_IMAGE || images[i].getMimsType() == MimsPlus.RATIO_IMAGE)
             isOneMassOrRatioImagePresent = true;
       
       // Show Roi formatted table if only dealing with one plane
       if (isOneMassOrRatioImagePresent && planes.size() != 1)
         table.createTable(appendCheckBox.isSelected());
       else
         table.createSumTable(appendCheckBox.isSelected());

       // Show table
       table.showFrame();
    }//GEN-LAST:event_jButton1ActionPerformed

    // When the button is pressed a new window is opened
    // which contains a line plot, if a Line Roi is drawn.
    private void profilejButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_profilejButtonActionPerformed
       if (ui.lineProfile == null) {
          ui.lineProfile = new MimsLineProfile(ui);
          double[] foo = new double[100];
          for (int i = 0; i < 100; i++) {
             foo[i] = 10;
          }
          ui.updateLineProfile(foo, "line", 1);
       } else {
          ui.lineProfile.setVisible(true);
       }
    }//GEN-LAST:event_profilejButtonActionPerformed
    
    public void resetImageNamesList() {

        // Get all the open images we want in the list.
        MimsPlus[] rp = ui.getOpenRatioImages();
        MimsPlus[] mp = ui.getOpenMassImages();
        MimsPlus[] sp = ui.getOpenSumImages();

        // Build array of image names.
        java.util.ArrayList<MimsPlus> images = new java.util.ArrayList<MimsPlus>();
        for(int j=0; j<mp.length; j++) 
            images.add(mp[j]);
        for(int j=0; j<rp.length; j++)
            images.add(rp[j]);
        for(int j=0; j<sp.length; j++)
            images.add(sp[j]);

        // Insert into list.
        final MimsPlus[] img = new MimsPlus[images.size()];
        for(int k=0; k<images.size(); k++)
            img[k]=images.get(k);
        
        imageJList.setModel(new javax.swing.AbstractListModel() {
            public int getSize() { return img.length; }
            public Object getElementAt(int i) { return img[i]; }
        });
    }
    
    private MimsPlus[] getImages(){
       
       // Get selected images.
       int[] num = imageJList.getSelectedIndices();

       // Get all open mass and ratio images.
       MimsPlus[] mp = ui.getOpenMassImages();
       MimsPlus[] rp = ui.getOpenRatioImages();
       
       // Build list of images.
       MimsPlus[] images = new MimsPlus[num.length];
       for (int i = 0; i < num.length; i++) {
          images[i] = (MimsPlus)imageJList.getModel().getElementAt(num[i]);
       }
       
       return images;
    }

    // Get the selected statistics.
    public String[] getStatNames(){

       // initialize array and get selected statistics.
       Object[] objs = new Object[statJList.getSelectedValues().length];
       objs = statJList.getSelectedValues();
       
       // If no statistics selected, use Area, Mean, and StdDev by default.
       String[] statnames;
       if (objs.length == 0) {
          statnames = new String[3];
          statnames[0] = "area";
          statnames[1] = "mean";
          statnames[2] = "stddev";
       } else {
         statnames = new String[objs.length];
         for (int i = 0; i < objs.length; i++) {
            statnames[i] = (String) objs[i];
         }
       }

       return statnames;
    }

    private ArrayList<Integer> getPlanes(){

       // initialize
       ArrayList planes = new ArrayList<Integer>();

       // Get text.
       String list = jTextField1.getText().trim();

       // Parse text, generat list.
       if (currentPlaneCheckBox.isSelected()) {
          planes.add(ui.getOpenMassImages()[0].getCurrentSlice());
       } else if (list.matches("") || list.length() == 0) {
          for(int i = 1; i <= ui.getOpenMassImages()[0].getNSlices(); i++) {
             planes.add(i);
          }
       } else {
          planes = MimsStackEditor.parseList(jTextField1.getText(), 1, ui.getOpenMassImages()[0].getNSlices());
       }

       return planes;
    }

    class ImageListRenderer extends JLabel implements ListCellRenderer {

      public ImageListRenderer() {
         setOpaque(true);
         setHorizontalAlignment(LEFT);
         setVerticalAlignment(CENTER);
      }

      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

         // Extract simple title from value.
         MimsPlus image = (MimsPlus) value;
         String label = image.getRoundedTitle();
         setText(label);

         if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
         } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
         }

         return this;
      }
   }

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JCheckBox appendCheckBox;
   private javax.swing.JCheckBox currentPlaneCheckBox;
   private javax.swing.JCheckBox histogramUpdatejCheckBox;
   private javax.swing.JPanel histogramjPanel;
   private javax.swing.JList imageJList;
   private javax.swing.JButton jButton1;
   private javax.swing.JLabel jLabel3;
   private javax.swing.JLabel jLabel4;
   private javax.swing.JLabel jLabel5;
   private javax.swing.JScrollPane jScrollPane1;
   private javax.swing.JScrollPane jScrollPane2;
   private javax.swing.JSeparator jSeparator1;
   private javax.swing.JTextField jTextField1;
   private javax.swing.JButton plotButton;
   private javax.swing.JButton profilejButton;
   private javax.swing.JList statJList;
   // End of variables declaration//GEN-END:variables
}
