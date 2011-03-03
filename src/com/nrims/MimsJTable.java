package com.nrims;

import com.nrims.data.MIMSFileFilter;
import ij.IJ;
import ij.gui.Roi;
import ij.process.ImageStatistics;
import java.awt.Component;
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
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * MimsJTable class creates a frame containing a <code>JTable</code>.
 * This class is used to generate frame that contain data, usually
 * statistical data associated with images.
 *
 * @author zkaufman
 */
public class MimsJTable {

   UI ui;
   JTable table;
   String[] stats;
   MimsPlus images[];
   Roi[] rois;
   ArrayList planes;
   JFrame frame;

   static String DEFAULT_TABLE_NAME = "_data.txt";
   static String AREA = "area";
   static String GROUP = "group";

   static String FILENAME = "file";
   static String ROIGROUP = "Roi group";
   static String ROINAME = "Roi name";
   static String SLICE = "Slice";
   
   static String[] SUM_IMAGE_MANDOTORY_COLUMNS = {FILENAME, ROIGROUP, ROINAME};
   static String[] ROIMANAGER_MANDATORY_COLUMNS = {ROINAME, ROIGROUP, SLICE};

   public static final int mOptions = ImageStatistics.AREA+ImageStatistics.MEAN+ImageStatistics.STD_DEV +
                 ImageStatistics.MODE+ImageStatistics.MIN_MAX+ImageStatistics.CENTROID +
                 ImageStatistics.CENTER_OF_MASS+ImageStatistics.PERIMETER+ImageStatistics.LIMIT +
                 ImageStatistics.RECT+ImageStatistics.LABELS+ImageStatistics.ELLIPSE +
                 ImageStatistics.INVERT_Y+ImageStatistics.CIRCULARITY+ImageStatistics.SHAPE_DESCRIPTORS +
                 ImageStatistics.INTEGRATED_DENSITY+ImageStatistics.MEDIAN +
                 ImageStatistics.SKEWNESS+ImageStatistics.KURTOSIS + ImageStatistics.SLICE +
                 ImageStatistics.STACK_POSITION+ImageStatistics.SCIENTIFIC_NOTATION;

   public MimsJTable(UI ui) {
      this.ui = ui;
   }

  /**
   * Used by the RoiManager "measure" button to generate a table.
   * When used in this way, only statistics for the currently
   * selected image will be shown.
   *
   * @param appendData <code>true</code> if appending a plot
   * to an existing frame, otherwise <code>false</code>.
   */
   public void createRoiTable(boolean appendData){

      // Get the data and column headers.
      Object[][] data = getRoiDataSet();
      String[] columnNames = getRoiManagerColumnNames();

      // Generate and display table.
      if (appendData && ableToAppendData(columnNames))
         appendDataToTable(data, columnNames);
      else
         displayTable(data, columnNames);
   }

  /**
   * Generates a table for Sum images. Because sum images only have
   * one plane, a sum table will differ from the default table in
   * that each row corresponds to an ROI (rather than to a plane).
   *
   * @param appendData <code>true</code> if appending a plot
   * to an existing frame, otherwise <code>false</code>.
   */
   public void createSumTable(boolean appendData){

      // Get the data and column headers.
      Object[][] data = getSumImageDataSet();
      String[] columnNames = getSumImageColumnNames();

      // Generate and display table.
      if (appendData && ableToAppendData(columnNames))
         appendDataToTable(data, columnNames);
      else
         displayTable(data, columnNames);
   }

  /**
   * Generates a table for images. Each row corresponds to
   * a plane and each column to a statisitical field (or meta data String).
   *
   * @param appendData <code>true</code> if appending a plot
   * to an existing frame, otherwise <code>false</code>.
   */
   public void createTable(boolean appendData) {

      // Get data.
      Object[][] data = getDataSet();
      String[] columnNames = getColumnNames();

      // Generate and display table.
      if (appendData && ableToAppendData(columnNames))
         appendDataToTable(data, columnNames);
      else
         displayTable(data, columnNames);
   }

  /**
   * Generates a table for listing Roi info (name and group) and their pixel values.
   * Correct order is required and ArrayLists must have the same length.
   * Output table will look something like the following:
   *
   * Group  | Name  | Pixel Value
   * ------------------------------
   * group1 | name1 | pixel value 1
   * group1 | name1 | pixel value 2
   * group1 | name2 | pixel value 3
   * group2 | name1 | pixel value 1
   * group2 | name2 | pixel value 2
   * group3 | name1 | pixel value 1
   * group3 | name2 | pixel value 2
   * group3 | name3 | pixel value 3
   * group3 | name3 | pixel value 4
   *
   * @param groups ArrayList of roi names. Repeats expected.
   * @param groups ArrayList of groups. Repeats expected.
   * @param groups ArrayList of pixel values.
   */
   void createPixelTable(String file, ArrayList<String> names, ArrayList<String> groups, ArrayList<Double> values) {
      
      // Input checks.
      if (names == null || groups == null || values == null)
         return;
      if (names.size() == 0 || groups.size() == 0 || values.size() == 0)
         return;
      if ((groups.size() != values.size()) || (names.size() != values.size()))
         return;

      // Get data.
      Object[][] data = new Object[values.size()][4];
      String group, name = "";
      for(int i = 0; i < values.size(); i++) {
         name = (String)names.get(i);
         group = (String)groups.get(i);
         if (group == null)
            group = "null";
         if (name == null)
            name = "null";
         if (group.trim().length() == 0)
            group = "null";
         if (name.trim().length() == 0)
            name = "null";
         data[i][0] = file;
         data[i][1] = group;
         data[i][2] = name;
         data[i][3] = (Double)values.get(i);
      }
      String[] columnNames = {"File", "Roi Group", "Roi Name", "Pixel value"};

      displayTable(data, columnNames);
   }

  /**
   * Does the actual displaying of the table and frame.
   */
   private void displayTable(Object[][] data, String[] columnNames){

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
         for (int i = 0; i < images.length; i++)
            title += " : " + images[i].getShortTitle();
         frame = new JFrame(title);
         frame.setJMenuBar(menuBar);
         frame.setContentPane(scrollPane);
         frame.pack();
   }

  /**
   * Determines the best way to go about appending data.
   */
   private void appendDataToTable(Object[][] data, String[] columnNames) {
      TableModel tm = table.getModel();
      DefaultTableModel model = (DefaultTableModel) tm;
      for (int i = 0; i < data.length; i++) {
         model.addRow(data[i]);
         model.setColumnIdentifiers(columnNames);
      }
      autoResizeColWidth(table, model);

      // Update title.
      String title = ui.getImageFilePrefix();
      for (int i = 0; i < images.length; i++)
         title += " : " + images[i].getShortTitle();

      frame.setTitle(title);
   }

  /**
   * Determines if it is possible to append data.
   */
   private boolean ableToAppendData(String[] columnNames) {

      // Cant append if any of the following conditions are satisfied.
      if (frame == null)
         return false;
      else if (!frame.isVisible())
         return false;
      else if (table == null)
         return false;
      else if (!tableColumnsMatch(columnNames))
         return false;

      return true;
   }

  /**
   * Use this method when getting data for single planes.
   * Produces a differently formated table than getDataSet().
   */
   private Object[][] getRoiDataSet(){

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
      if (image.getMimsType() == MimsPlus.HSI_IMAGE)
         image = image.internalRatio;
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
               data[row][col] = (new Integer(images[0].getCurrentSlice())).toString();
            else
               data[row][col] = "null";
         }
      }

      // Fill in the data.
      ImageStatistics imageStats = null;
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
            imageStats = image.getStatistics(mOptions);

            // "Group" is a mandatory row, so ignore if user selected it.
            if (stat.startsWith(GROUP))
               continue;

            // No decimal for area statistic.
            if (stat.equals(AREA))
               precision = 0;
            data[row][colnum] = IJ.d2s(MimsJFreeChart.getSingleStat(imageStats, stat), precision);

            colnum++;
         }
     }

     return data;
   }

  /**
   * Use this method when getting data for single planes.
   * Produces a differently formated table than getDataSet().
   */
   private Object[][] getSumImageDataSet(){

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
      ImageStatistics imageStats;

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
                  imageStats = image.getStatistics(mOptions);

                  // "Group" is a mandatory row, so ignore if user selected it.
                  if (stat.startsWith(GROUP))
                     continue;

                  // Some stats we only want to put in once, like "area".
                  if (col1 == 0) {
                     if (stat.equals(AREA))
                        precision = 0;                     
                     data[row][colnum] = IJ.d2s(MimsJFreeChart.getSingleStat(imageStats, stat), precision);
                  } else {
                     if (stat.equals(AREA))
                        continue;
                     else
                        data[row][colnum] = IJ.d2s(MimsJFreeChart.getSingleStat(imageStats, stat), precision);
                  }
                  colnum++;
               }
           }
       }

      return data;
   }

  /**
   * Use this method when getting data for multiple planes.
   */
   private Object[][] getDataSet() {

      // initialize variables.
      ImageStatistics imageStats = null;
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
                  imageStats = image.getStatistics(mOptions);                  
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
                        data[ii][col] = IJ.d2s(MimsJFreeChart.getSingleStat(imageStats, stats[k]), precision);
                  } else {
                     if ((stats[k].startsWith("group") || stats[k].equalsIgnoreCase("area")))
                        continue;
                     else
                        data[ii][col] = IJ.d2s(MimsJFreeChart.getSingleStat(imageStats, stats[k]), precision);

                  }
                  col++;
               }
            }
         }
      }

      ui.getOpenMassImages()[0].setSlice(currentSlice);

      return data;
   }

  /**
   * Returns the column names.
   */
   private String[] getColumnNames(){

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
               header = stat + prefix + images[j].getRoundedTitle() + "_r" + rois[i].getName();
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

  /**
   * Setup column headers for sum image table.
   */
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

  /**
   * Setup column headers for sum image table.
   */
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

  /**
   * Determines the behavior of the "Save" action.
   */
   private void saveActionPerformed(ActionEvent evt) {
      MimsJFileChooser fc = new MimsJFileChooser(ui);
      MIMSFileFilter mff_txt = new MIMSFileFilter("txt");
      mff_txt.setDescription("Text file");
      fc.addChoosableFileFilter(mff_txt);
      fc.setFileFilter(mff_txt);
      fc.setPreferredSize(new java.awt.Dimension(650, 500));
      String lastFolder = ui.getLastFolder();

      try {
         fc.setSelectedFile(new File(lastFolder, ui.getImageFilePrefix()+DEFAULT_TABLE_NAME));

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

  /**
   * Writes the actual data.
   */
   private void writeData(File file) {
      try {
              PrintWriter out = new PrintWriter(new FileWriter(file));
              DefaultTableModel dtm = (DefaultTableModel)table.getModel();

              // Write column headers              
              for (int i = 0; i < dtm.getColumnCount(); i++) {
                 out.print(dtm.getColumnName(i));
                 if (i < dtm.getColumnCount() - 1)
                     out.print("\t");                 
              }
              out.println();

              // Write data
              String value = "";
              for (int i = 0; i < dtm.getRowCount(); i++) {
                 for (int j = 0; j < dtm.getColumnCount(); j++) {
                    Object objVal = dtm.getValueAt(i, j);
                    if (value == null)
                       value = "null";
                    else
                       value = objVal.toString();
                    out.print(value);
                    if (j < dtm.getColumnCount() - 1)
                       out.print("\t");                                     
                 }
                 out.println();
              }

              // Close file
              out.close();

          } catch (IOException e) {
              e.printStackTrace();
          }
   }

  /**
   * Determines if the number of columns is the same.
   */
   private boolean tableColumnsMatch(String[] columnNames) {

      int numCol1 = columnNames.length;
      int numCol2 = ((DefaultTableModel)table.getModel()).getColumnCount();

      if (numCol1 != numCol2)
         return false;

      return true;
   }

   /**
    * Sets the images to be included in the table.
    *
    * @param images a set of MimsPlus images.
    */
   public void setImages(MimsPlus[] images){
      this.images = images;
   }

   /**
    * Sets the statistics to be included in the table.
    *
    * @param stats a set of statistics.
    */
   public void setStats(String[] stats) {
      this.stats = stats;
   }

   /**
    * Sets the ROIs to be included in the table.
    *
    * @param rois a set of ROIs.
    */
   public void setRois(Roi[] rois) {
      this.rois = rois;
   }

   /**
    * Displays the frame (with table).
    */
   public void showFrame() {
      if (frame != null) {
         frame.setVisible(true);
         frame.toFront();
      }
   }

   /**
    * Nulls the table and sets the frame to not visible.
    */
   public void close() {
    	table = null;
      if (frame != null)
         frame.setVisible(false);
   }

   /**
    * Sets the planes to be included in the table.
    *
    * @param planes an arraylist of planes.
    */
   void setPlanes(ArrayList planes) {
      this.planes = planes;
   }

   /**
    * Adjust the size of the columns correctly.
    *
    * @param table the JTable.
    * @param model the table model.
    */
   private JTable autoResizeColWidth(JTable table, DefaultTableModel model) {


        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(model);

        int margin = 5;
        int minWidth = 75;

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
            if (width < minWidth)
               width = minWidth;

            // Set the width
            col.setPreferredWidth(width);
        }

        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(
            SwingConstants.CENTER);

        return table;
    }

}
