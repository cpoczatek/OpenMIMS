package com.nrims;

import ij.IJ;
import ij.gui.Roi;
import ij.process.ImageStatistics;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * @author zkaufman
 */
public class MimsJTable {

   UI ui;
   JTable table;
   String[] stats;
   MimsPlus images[];
   Roi[] rois;
   Object[][] data;
   ArrayList planes;
   JFrame frame;

   public MimsJTable(UI ui) {
      this.ui = ui;
   }

   // This method is used by the RoiManager "measure"
   // button to create smaller tables with one row per Roi.
   public void createRoiTable(){

      // Get the data.
      Object[][] data = getRoiDataSet();

      // Setup column headers.
      String[] columnNames = new String[stats.length+2];
      columnNames[0] = "Image : Roi Label";
      columnNames[1] = "Slice";
      for (int i = 0; i < stats.length; i++){
         columnNames[i+2] = stats[i];
      }

      // Generate and display table.
      displayTable(data, columnNames);
   }

   // This method is used by the Tomography tab "table"
   // button to create larger tables with one row per plane.
   public void createTable(boolean appendResults) {

      // Cant append if frame doesnt exist
      if (frame == null) {
         appendResults = false;
      } else if (!frame.isVisible()) {
         appendResults = false;
      }

      // If attempting to append, make sure number of column match.
      if (appendResults && data != null) {
         appendResults = tableColumnsMatch();
      }

      // Appending results.
      if (appendResults && data != null) {
         Object[][] newData = getDataSet();
         for (int i = 0; i < newData.length; i++) {
            TableModel tm = table.getModel();
            DefaultTableModel model = (DefaultTableModel) tm;
            model.addRow(newData[i]);
            model.setColumnIdentifiers(getColumnNames());
            int width = 100;
            for (int ii = 0; ii < getColumnNames().length; ii++) {
               TableColumn col = table.getColumnModel().getColumn(ii);
               col.setMinWidth(width);
               col.setPreferredWidth(width);
            }
         }
      } else if (rois.length == 0 || images.length == 0 || stats.length == 0) {
         return;
      } else {

         // Get data.
         data = getDataSet();

         // Get columns.
         String[] columnNames = getColumnNames();

         // Display table.
         displayTable(data, columnNames);

      }
   }

   // Displays a table given data and column headers.
   public void displayTable(Object[][] data, String[] columnNames){
      
         int width = 110;
         DefaultTableModel tm = new DefaultTableModel(data, columnNames);
         table = new JTable(tm);
         for (int i = 0; i < columnNames.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setMinWidth(width);
            col.setPreferredWidth(width);
         }

         //Create the scroll pane and add the table to it.
         JScrollPane scrollPane = new JScrollPane(table);
         table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
         scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

         // Create the menu bar.
         JMenuBar menuBar = new JMenuBar();
         JMenu menu;
         JMenuItem menuItem;
         menu = new JMenu("File");
         menuBar.add(menu);
         menuItem = new JMenuItem("Save");
         menuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveActionPerformed(evt);
         }});
         menu.add(menuItem);

         // Generate frame.
         frame = new JFrame("Measure");
         frame.setJMenuBar(menuBar);
         frame.setContentPane(scrollPane);
         frame.setSize(600, 400);
         frame.pack();
   }

   // Use this method when getting data for single planes.
   // Produces a differently formated table than getDataSet().
   public Object[][] getRoiDataSet(){

      // Image check.
      if (images.length != 1)
         return null;

      // Slice check.
      if (planes.size() != 1)
         return null;

      // Roi check.
      if (rois.length == 0)
         return null;

      // initialize data.
      Object data[][] = new Object[rois.length][stats.length+2];

      // Set the plane.
      int plane = (Integer) planes.get(0);
      MimsPlus image = images[0];
      if (image.getMimsType() == MimsPlus.MASS_IMAGE)
         image.setSlice(plane, false);
      else if (image.getMimsType() == MimsPlus.RATIO_IMAGE)
         image.setSlice(plane, image);

      // Fill in the data.
      for (int row = 0; row < rois.length; row++) {
         for (int col = 0; col < stats.length+2; col++) {
            Integer[] xy = ui.getRoiManager().getRoiLocation(rois[row].getName(), plane);
            rois[row].setLocation(xy[0], xy[1]);
            image.setRoi(rois[row]);
            ImageStatistics tempstats = image.getStatistics();
            if (col == 0)
               data[row][col] = images[0].getShortTitle() + " : (" + rois[row].getName() + ")";
            else if (col == 1)
               data[row][col] = plane;
            else {
               double value = MimsJFreeChart.getSingleStat(tempstats, stats[col-2]);
               int precision = 2;
               if (stats[col-2] == "area")
                  precision = 0;
               data[row][col] = IJ.d2s(MimsJFreeChart.getSingleStat(tempstats, stats[col-2]), precision);
            }
          }
       }

      return data;
   }


   // Use this method when getting data for multiple planes.
   public Object[][] getDataSet() {

      // initialize variables.
      ImageStatistics tempstats = null;
      int currentSlice = ui.getOpenMassImages()[0].getCurrentSlice();
      Object[][] data = new Object[planes.size()][rois.length * images.length * stats.length + 1];

      // Fill in "slice" field.
      for (int ii = 0; ii < planes.size(); ii++) {
         data[ii][0] = ((Integer)planes.get(ii)).toString();
      }

      // Fill in data.
      for (int ii = 0; ii < planes.size(); ii++) {
         int col = 1;
         int plane = (Integer)planes.get(ii);
         for (int j = 0; j < images.length; j++) {
            MimsPlus image = images[j];
            if (image.getMimsType() == MimsPlus.MASS_IMAGE)
               image.setSlice(plane, false);
            else if (image.getMimsType() == MimsPlus.RATIO_IMAGE)
               image.setSlice(plane, image);
            for (int i = 0; i < rois.length; i++) {
               for (int k = 0; k < stats.length; k++) {
                  Integer[] xy = ui.getRoiManager().getRoiLocation(rois[i].getName(), plane);
                  rois[i].setLocation(xy[0], xy[1]);
                  image.setRoi(rois[i]);
                  tempstats = image.getStatistics();
                  if (j == 0) {
                     if (stats[k].startsWith("group")) {
                        String group = "null";
                        if (ui.getRoiManager().getRoiGroup(rois[i].getName()) == null)
                           data[ii][col] = group;
                        else
                           data[ii][col] = ui.getRoiManager().getRoiGroup(rois[i].getName());
                     } else
                        data[ii][col] = IJ.d2s(MimsJFreeChart.getSingleStat(tempstats, stats[k]), 2);
                  } else {
                     if ((stats[k].startsWith("group") || stats[k].equalsIgnoreCase("area")))
                        continue;
                     else
                        data[ii][col] = IJ.d2s(MimsJFreeChart.getSingleStat(tempstats, stats[k]), 2);

                  }
                  col++;
               }
            }
         }
      }

      ui.getOpenMassImages()[0].setSlice(currentSlice);

      return data;
   }

   public String[] getColumnNames(){

      // initialze variables.
      ArrayList<String> columnNamesArray = new ArrayList<String>();
      String header = "";
      columnNamesArray.add("slice");
      String tableOnly = "(table only)";

      // Generate header based on image, roi, stat.
      int col = 1;
      for (int j = 0; j < images.length; j++) {
         for (int i = 0; i < rois.length; i++) {
            for (int k = 0; k < stats.length; k++) {
               String stat = stats[k];
               if (j == 0) {
                  if (stats[k].endsWith(tableOnly))
                     stat = stats[k].substring(0, stats[k].indexOf(tableOnly) - 1);
               } else {
                  if ((stats[k].startsWith("group") || stats[k].equalsIgnoreCase("area")))
                     continue;
               }
               header = stat + "_m" + images[j].getRoundedTitle() + "_r" + (ui.getRoiManager().getIndex(rois[i].getName())+1);
               columnNamesArray.add(header);
               col++;
            }
         }
      }

      // Fill in columnNames array.
      String[] columnNames = new String[columnNamesArray.size()];
      for (int i = 0; i < columnNames.length; i++) {
         columnNames[i] = columnNamesArray.get(i);
      }

      return columnNames;
   }

   private void saveActionPerformed(ActionEvent evt) {
      javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
      fc.setPreferredSize(new java.awt.Dimension(650, 500));
      String lastFolder = ui.getLastFolder();

      try {
         if (lastFolder != null) {
            fc.setCurrentDirectory(new java.io.File(lastFolder));
         }

         int returnVal = fc.showSaveDialog(frame);
         if (returnVal == JFileChooser.APPROVE_OPTION) {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            writeData(fc.getSelectedFile());
         } else {
            return;
         }
      } catch (Exception e) {
         ij.IJ.error("Save Error", "Error saving file.");
      } finally {
         frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
   }

   public void writeData(File file) {
      try {
              PrintWriter out = new PrintWriter(new FileWriter(file));

              // Write column headers
              int col = 0;
              String[] columnNames = getColumnNames();
              for (String name: columnNames) {
                 out.print(name);
                 if (col < columnNames.length - 1)
                     out.print("\t");
                 col++;
              }
              out.println();

              // Write data
              for (int i = 0; i < data.length; i++) {
                 col = 0;
                 for (int j = 0; j < data[i].length; j++) {
                    out.print((String)data[i][j]);
                    if (col < columnNames.length - 1)
                       out.print("\t");
                    col++;
                 }
                 out.println();
              }

              // Close file
              out.close();

          } catch (IOException e) {
              e.printStackTrace();
          }
   }

   public boolean tableColumnsMatch() {

      String[] columnNames = getColumnNames();

      int numCol1 = columnNames.length;
      int numCol2 = table.getColumnCount();

      if (numCol1 != numCol2) {
         return false;
      }

      return true;
   }

   public void setImages(MimsPlus[] images){
      this.images = images;
   }

   public void setStats(String[] stats) {
      this.stats = stats;
   }

   public void setRois(Roi[] rois) {
      this.rois = rois;
   }

   public void showFrame() {
      if (frame != null) {
         frame.setVisible(true);
         frame.toFront();
      }
   }

   public void close() {
    	data = null;
      if (frame != null)
         frame.setVisible(false);
   }

   void setPlanes(ArrayList planes) {
      this.planes = planes;
   }

}
