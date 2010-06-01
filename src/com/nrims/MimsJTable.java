package com.nrims;

import ij.IJ;
import ij.gui.Roi;
import ij.process.ImageStatistics;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
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
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
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
   String[] columnNames;
   ArrayList planes;
   JFrame frame;
   boolean roiTable = false;

   static String AREA = "area";
   static String GROUP = "group";

   static String FILENAME = "file";
   static String ROIGROUP = "Roi group";
   static String ROINAME = "Roi name";
   static String SLICE = "Slice";
   
   static String[] SUM_IMAGE_MANDOTORY_COLUMNS = {FILENAME, ROIGROUP, ROINAME};
   static String[] ROIMANAGER_MANDATORY_COLUMNS = {ROINAME, ROIGROUP, SLICE};

   public MimsJTable(UI ui) {
      this.ui = ui;
   }

   // This method is used by the RoiManager "measure"
   // button to create smaller tables with one row per Roi.
   public void createRoiTable(){

      // Set roiTable flag.
      roiTable = true;

      // Get the data.
      data = getRoiDataSet();

      // Setup column headers.
      columnNames = getRoiManagerColumnNames();

      // Generate and display table.
      displayTable(data, columnNames);
   }

   // For sum images.
   public void createSumTable(boolean appendResults){

      // Get the data.
      data = getSumImageDataSet();

      columnNames = getSumImageColumnNames();

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
         columnNames = getColumnNames();

         // Display table.
         displayTable(data, columnNames);

      }
   }

   // Displays a table given data and column headers.
   public void displayTable(Object[][] data, String[] columnNames){

         // Create table and set column width.
         DefaultTableModel tm = new DefaultTableModel(data, columnNames);
         table = new JTable(tm);
         table = autoResizeColWidth(table, tm);
         table.setAutoCreateRowSorter(true);

         //Create the scroll pane and add the table to it.
         JScrollPane scrollPane = new JScrollPane(table);
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
         String title = ui.getImageFilePrefix();
         if (roiTable)
            title += " : " + images[0].getShortTitle();
         frame = new JFrame(title);
         frame.setJMenuBar(menuBar);
         frame.setContentPane(scrollPane);
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
      int nRows = rois.length;
      int nCols = ROIMANAGER_MANDATORY_COLUMNS.length + stats.length;
      Object data[][] = new Object[nRows][nCols];
      int plane = (Integer) planes.get(0);
      MimsPlus image = images[0];
      Roi roi;
      String stat;

      // Set the plane.      
      if (image.getMimsType() == MimsPlus.MASS_IMAGE)
         image.setSlice(plane, false);
      else if (image.getMimsType() == MimsPlus.RATIO_IMAGE)
         image.setSlice(plane, image);

      // Fill in "mandatory" fields... ususally non-numeric file/Roi information.
      for (int row = 0; row < rois.length; row++) {
         roi = rois[row];
         for (int col = 0; col < ROIMANAGER_MANDATORY_COLUMNS.length; col++) {
            stat = ROIMANAGER_MANDATORY_COLUMNS[col];
            if (stat.equals(ROIGROUP)) {
               String group = ui.getRoiManager().getRoiGroup(roi.getName());
               if (group == null)
                  group = "null";
               data[row][col] = group;
            } else if (stat.equals(ROINAME))
               data[row][col] = roi.getName();
            else if (stat.equals(SLICE))
               data[row][col] = images[0].getCurrentSlice();
            else
               data[row][col] = "null";
         }
      }

      // Fill in the data.
      for (int row = 0; row < rois.length; row++) {
         roi = rois[row];
         int colnum = SUM_IMAGE_MANDOTORY_COLUMNS.length;
         
         for (int col = 0; col < stats.length; col++) {
            stat = stats[col];

            // Set decimal precision.
            int precision = 2;

            // Set the ROI location.
            Integer[] xy = ui.getRoiManager().getRoiLocation(rois[row].getName(), plane);
            rois[row].setLocation(xy[0], xy[1]);
            image.setRoi(rois[row]);
            ImageStatistics tempstats = image.getStatistics();

            // "Group" is a mandatory row, so ignore if user selected it.
            if (stat.startsWith(GROUP))
               continue;

            // No decimal for area statistic.
            if (stat.equals(AREA))
               precision = 0;
            data[row][colnum] = IJ.d2s(MimsJFreeChart.getSingleStat(tempstats, stat), precision);

            colnum++;
         }
     }

     return data;
   }

   // Use this method when getting data for single planes.
   // Produces a differently formated table than getDataSet().
   public Object[][] getSumImageDataSet(){

      // Roi check.
      if (rois.length == 0)
         return null;

      // initialize data.
      int nRows = rois.length;
      int nCols = (SUM_IMAGE_MANDOTORY_COLUMNS.length + stats.length) * images.length;
      Object data[][] = new Object[nRows][nCols];
      Roi roi;
      MimsPlus image;
      String stat;
      ImageStatistics tempstats;      

      // Fill in "mandatory" fields... ususally non-numeric file/Roi information.
      for (int row = 0; row < rois.length; row++) {
         roi = rois[row];
         for (int col = 0; col < SUM_IMAGE_MANDOTORY_COLUMNS.length; col++) {
            stat = SUM_IMAGE_MANDOTORY_COLUMNS[col];
            if (stat.equals(FILENAME))
               data[row][col] = ui.getImageFilePrefix();
            else if (stat.equals(ROIGROUP)) {
               String group = ui.getRoiManager().getRoiGroup(roi.getName());
               if (group == null)
                  group = "null";
               data[row][col] = group;
            } else if (stat.equals(ROINAME))
               data[row][col] = roi.getName();
            else
               data[row][col] = "null";
         }
      }

      // Fill in rest of data... statistics.
      for (int row = 0; row < rois.length; row++) {
         roi = rois[row];
         int colnum = SUM_IMAGE_MANDOTORY_COLUMNS.length;

         for (int col1 = 0; col1 < images.length; col1++) {
            image = images[col1];            

               for (int col2 = 0; col2 < stats.length; col2++) {
                  stat = stats[col2];
                  
                  // Set decimal percision.
                  int precision = 2;
                  
                  // Sum images, by definition, are only 1 plane. Since Rois
                  // can have different location on different planes, we will
                  // choose the location for the currently displayed slice.
                  int plane = ui.getMassImages()[0].getCurrentSlice();
                  Integer[] xy = ui.getRoiManager().getRoiLocation(roi.getName(), plane);
                  roi.setLocation(xy[0], xy[1]);
                  image.setRoi(roi);
                  tempstats = image.getStatistics();

                  // "Group" is a mandatory row, so ignore if user selected it.
                  if (stat.startsWith(GROUP))
                     continue;

                  // Some stats we only want to put in once, like "area".
                  if (col1 == 0) {
                     if (stat.equals(AREA))
                        precision = 0;                     
                     data[row][colnum] = IJ.d2s(MimsJFreeChart.getSingleStat(tempstats, stat), precision);
                  } else {
                     if (stat.equals(AREA))
                        continue;
                     else
                        data[row][colnum] = IJ.d2s(MimsJFreeChart.getSingleStat(tempstats, stat), precision);
                  }
                  colnum++;
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
                  int precision = 2;
                  Integer[] xy = ui.getRoiManager().getRoiLocation(rois[i].getName(), plane);
                  rois[i].setLocation(xy[0], xy[1]);
                  image.setRoi(rois[i]);
                  tempstats = image.getStatistics();
                  if (j == 0) {
                     if (stats[k].startsWith("area"))
                        precision = 0;
                     if (stats[k].startsWith("group")) {
                        String group = "null";
                        if (ui.getRoiManager().getRoiGroup(rois[i].getName()) == null)
                           data[ii][col] = group;
                        else
                           data[ii][col] = ui.getRoiManager().getRoiGroup(rois[i].getName());
                     } else
                        data[ii][col] = IJ.d2s(MimsJFreeChart.getSingleStat(tempstats, stats[k]), precision);
                  } else {
                     if ((stats[k].startsWith("group") || stats[k].equalsIgnoreCase("area")))
                        continue;
                     else
                        data[ii][col] = IJ.d2s(MimsJFreeChart.getSingleStat(tempstats, stats[k]), precision);

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
                  if ((stats[k].startsWith(GROUP) || stats[k].equalsIgnoreCase(AREA)))
                     continue;
               }  
               String prefix = "_";
               if (images[j].getType() == MimsPlus.MASS_IMAGE || images[j].getType() == MimsPlus.RATIO_IMAGE)
                  prefix = "_m";
               header = stat + prefix + images[j].getRoundedTitle() + "_r" + (ui.getRoiManager().getIndex(rois[i].getName())+1);
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

   // Setup column headers for sum image table.
   private String[] getSumImageColumnNames() {

      // initialze variables.
      ArrayList<String> columnNamesArray = new ArrayList<String>();
      MimsPlus image;
      String stat;      

      // Mandatory columns first.
      for (int col = 0; col < SUM_IMAGE_MANDOTORY_COLUMNS.length; col++) {
         String header = SUM_IMAGE_MANDOTORY_COLUMNS[col];
         if (header.equals(FILENAME))
            columnNamesArray.add(FILENAME);
         else if (header.equals(ROIGROUP))
            columnNamesArray.add(ROIGROUP);
         else if (header.equals(ROINAME))
            columnNamesArray.add(ROINAME);
         else
            columnNamesArray.add("null");
      }

      // Data column headers.
      for (int col1 = 0; col1 < images.length; col1++) {
         image = images[col1];
         for (int col2 = 0; col2 < stats.length; col2++) {
            stat = stats[col2];
            String label = stat + " " + image.getRoundedTitle();

            if (stat.startsWith(GROUP))
               continue;

            if (col1 > 0 && stat.equals(AREA))
               continue;
            else
               columnNamesArray.add(label);
         }
      }

      // Assemble column headers.
      String[] columnNames = new String[columnNamesArray.size()];
      for (int i = 0; i < columnNames.length; i++) {
         columnNames[i] = columnNamesArray.get(i);
      }

      return columnNames;
   }

   // Setup column headers for sum image table.
   private String[] getRoiManagerColumnNames(){

      // Fill in preliminary mandatory columns headers.
      ArrayList<String> columnNamesArray = new ArrayList<String>();
      int colnum = 0;
      for (int i = 0; i < ROIMANAGER_MANDATORY_COLUMNS.length; i++){
         columnNamesArray.add(ROIMANAGER_MANDATORY_COLUMNS[i]);
         colnum++;
      }

      // Fill in headers for stats.
      for (int i = 0; i < stats.length; i++){
         if (stats[i].startsWith(GROUP))
               continue;
         columnNamesArray.add(stats[i]);
         colnum++;
      }

      // Assemble column headers.
      String[] columnNames = new String[columnNamesArray.size()];
      for (int i = 0; i < columnNames.length; i++) {
         columnNames[i] = columnNamesArray.get(i);
      }

      // Return.
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
                    String value = (String)data[i][j];
                    if (value != null) {
                    out.print((String)data[i][j]);
                    if (col < columnNames.length - 1)
                       out.print("\t");
                    col++;
                    }
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

   public JTable autoResizeColWidth(JTable table, DefaultTableModel model) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(model);

        int margin = 5;

        for (int i = 0; i < table.getColumnCount(); i++) {
            int                     vColIndex = i;
            DefaultTableColumnModel colModel  = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn             col       = colModel.getColumn(vColIndex);
            int                     width     = 0;

            // Get width of column header
            TableCellRenderer renderer = col.getHeaderRenderer();

            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }

            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);

            width = comp.getPreferredSize().width;

            // Get maximum width of column data
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, vColIndex);
                comp     = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false,
                        r, vColIndex);
                width = Math.max(width, comp.getPreferredSize().width);
            }

            // Add margin
            width += 2 * margin;

            // Set the width
            col.setPreferredWidth(width);
        }

        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(
            SwingConstants.CENTER);

        // table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        return table;
    }

}
