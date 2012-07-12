/*
 * ReportGenerator.java
 *
 * Created on May 11, 2011, 9:15:26 AM
 */
package com.nrims;

import com.nrims.data.MIMSFileFilter;
import com.tutego.jrtf.Rtf;
import static com.tutego.jrtf.Rtf.rtf;
import com.tutego.jrtf.RtfPara;
import static com.tutego.jrtf.RtfPara.*;
import com.tutego.jrtf.RtfPicture;
import com.tutego.jrtf.RtfText;
import com.tutego.jrtf.RtfUnit;
import static com.tutego.jrtf.RtfText.*;
import ij.IJ;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * ReportGenerator class displays a pop-up window that contains
 * a capture of the current image and allows the user to type
 * in some notes into a text area. This information is then
 * inserted into an RTF document that is editable outside the plugin.
 *
 * @author zkaufman
 */
public class ReportGenerator extends javax.swing.JFrame implements MouseListener {

   UI ui;
   static File reportFile;
   Date date;
   MimsJFreeChart jfc;
   MimsJTable jt;
   JLabel[] jlabelArray;
   Image[] imageArray;
   Font font = new Font(Font.SERIF, Font.PLAIN, 12);
   DecimalFormat formatter = new DecimalFormat("0.00");
   JPopupMenu jp;
   JLabel currentLabel = null;

   private int reportType = 0;
   public static final String REPORT_EXTENSION = "_report.rtf";
   public static final int IMAGE = 1;
   public static final int TABLE = 3;

   public static final int HEIGHT_TWIPS = 2490;
   public int WIDTH_TWIPS = 2490;
   
   public static final int ICON_HEIGHT = 128;
   public int ICON_WIDTH = 128;

   /**
    * ReportGenerator constructor for images.
    *
    * @param ui
    */
   public ReportGenerator(UI ui) {
      this.ui = ui;
      this.date = new Date();
      this.reportType = IMAGE;
      initComponents();
      initComponentsCustom();
   }

   /**
    * ReportGenerator constructor for tables.
    *
    * @param ui
    * @param mimsjtable
    */
   public ReportGenerator(UI ui, MimsJTable jt) {
      this.ui = ui;
      this.date = new Date();
      this.jt = jt;
      this.reportType = TABLE;
      initComponents();
      initComponentsCustom();
   }

   /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
   @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      jScrollPane2 = new javax.swing.JScrollPane();
      jScrollPane1 = new javax.swing.JScrollPane();
      notesTextArea = new javax.swing.JTextArea();
      okButton = new javax.swing.JButton();
      cancelButton = new javax.swing.JButton();
      browseButton = new javax.swing.JButton();
      reportJLabel = new javax.swing.JLabel();
      jPanel2 = new javax.swing.JPanel();
      imageIcon = new javax.swing.JLabel();
      imageIcon1 = new javax.swing.JLabel();
      imageIcon2 = new javax.swing.JLabel();
      imageIcon3 = new javax.swing.JLabel();
      imageIcon4 = new javax.swing.JLabel();
      imageIcon5 = new javax.swing.JLabel();
      imageIcon6 = new javax.swing.JLabel();
      imageIcon7 = new javax.swing.JLabel();
      dateJlabel = new javax.swing.JLabel();
      imageFileJlabel = new javax.swing.JLabel();
      clickLabel = new javax.swing.JLabel();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      notesTextArea.setColumns(20);
      notesTextArea.setRows(5);
      jScrollPane1.setViewportView(notesTextArea);

      okButton.setText("OK");
      okButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            okButtonActionPerformed(evt);
         }
      });

      cancelButton.setText("Cancel");
      cancelButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            cancelButtonActionPerformed(evt);
         }
      });

      browseButton.setText("Browse...");
      browseButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            browseButtonActionPerformed(evt);
         }
      });

      reportJLabel.setText("reportfile");

      javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
      jPanel2.setLayout(jPanel2Layout);
      jPanel2Layout.setHorizontalGroup(
         jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 522, Short.MAX_VALUE)
      );
      jPanel2Layout.setVerticalGroup(
         jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 141, Short.MAX_VALUE)
      );

      dateJlabel.setText("date");

      imageFileJlabel.setText("imageFile");

      clickLabel.setText("Click");

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addContainerGap()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                     .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(dateJlabel, javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(imageFileJlabel, javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(browseButton)
                        .addGap(6, 6, 6)
                        .addComponent(reportJLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE))
                     .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                           .addGap(419, 419, 419)
                           .addComponent(cancelButton)
                           .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                           .addComponent(okButton)))
                     .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(imageIcon)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageIcon1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageIcon2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageIcon3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageIcon5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageIcon4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageIcon6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageIcon7)
                        .addGap(480, 480, 480))))
               .addGroup(layout.createSequentialGroup()
                  .addGap(150, 150, 150)
                  .addComponent(clickLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(imageIcon)
                     .addComponent(imageIcon1)
                     .addComponent(imageIcon3)
                     .addComponent(imageIcon5)
                     .addComponent(imageIcon4)
                     .addComponent(imageIcon6)
                     .addComponent(imageIcon7)
                     .addComponent(imageIcon2))
                  .addGap(22, 22, 22))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(clickLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(imageFileJlabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(dateJlabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(browseButton)
               .addComponent(reportJLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(okButton)
               .addComponent(cancelButton))
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
       MimsJFileChooser fc = new MimsJFileChooser(ui);
       MIMSFileFilter mff_txt = new MIMSFileFilter("rtf");
       mff_txt.setDescription("RTF file");
       fc.addChoosableFileFilter(mff_txt);
       fc.setFileFilter(mff_txt);
       fc.setPreferredSize(new java.awt.Dimension(650, 500));
       fc.setSelectedFile(reportFile);
       int returnVal = fc.showSaveDialog(this);
       if (returnVal == MimsJFileChooser.APPROVE_OPTION) {
          reportFile = fc.getSelectedFile();
          reportJLabel.setText(reportFile.getAbsolutePath());
       } else {
          return;
       }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
       setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
       writeReport();
    }//GEN-LAST:event_okButtonActionPerformed

   /**
    * @param args the command line arguments
    */
   public static void main(String args[]) {

   }
   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton browseButton;
   private javax.swing.JButton cancelButton;
   private javax.swing.JLabel clickLabel;
   private javax.swing.JLabel dateJlabel;
   private javax.swing.JLabel imageFileJlabel;
   private javax.swing.JLabel imageIcon;
   private javax.swing.JLabel imageIcon1;
   private javax.swing.JLabel imageIcon2;
   private javax.swing.JLabel imageIcon3;
   private javax.swing.JLabel imageIcon4;
   private javax.swing.JLabel imageIcon5;
   private javax.swing.JLabel imageIcon6;
   private javax.swing.JLabel imageIcon7;
   private javax.swing.JPanel jPanel2;
   private javax.swing.JScrollPane jScrollPane1;
   private javax.swing.JScrollPane jScrollPane2;
   private javax.swing.JTextArea notesTextArea;
   private javax.swing.JButton okButton;
   private javax.swing.JLabel reportJLabel;
   // End of variables declaration//GEN-END:variables

   private void initComponentsCustom() {

      // JPopupMenu
      jp = new JPopupMenu();
      JMenuItem menuItem = new JMenuItem("remove");
      menuItem.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String currentLabelText = currentLabel.getText();
            for (int i = 0; i < jlabelArray.length; i++) {
               String labelText = jlabelArray[i].getText();
               if (labelText != null && labelText.matches(currentLabelText)) {
                  jlabelArray[i].setText(null);
                  jlabelArray[i].setIcon(null);
                  imageArray[i] = null;
               }
            }
         }
      });
      jp.add(menuItem);

      // Set the report file label.
      if (reportFile == null) {
         File imageDir = ui.getOpener().getImageFile().getParentFile();
         String dirName = imageDir.getName();
         String reportName = dirName + REPORT_EXTENSION;
         reportFile = new File(imageDir, reportName);
      }
      reportJLabel.setFont(font);
      reportJLabel.setText(reportFile.getAbsolutePath());

      // Set the image label.
      String imageFileText = "<html><B>File:</B> ";
      imageFileText += ui.getOpener().getImageFile().getName();
      imageFileJlabel.setFont(font);
      imageFileJlabel.setText(imageFileText);

      // Set date label.
      String dateText = "<html><B>Date:</B> ";
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
      dateText += dateFormat.format(date);
      dateJlabel.setFont(font);
      dateJlabel.setText(dateText);

      // Specifics depending on the type of report.
      if (reportType == IMAGE) {

         // Set up the icon array.
         jlabelArray = new JLabel[8];
         jlabelArray[0] = imageIcon;
         jlabelArray[1] = imageIcon1;
         jlabelArray[2] = imageIcon2;
         jlabelArray[3] = imageIcon3;
         jlabelArray[4] = imageIcon4;
         jlabelArray[5] = imageIcon5;
         jlabelArray[6] = imageIcon6;
         jlabelArray[7] = imageIcon7;
         for (JLabel label : jlabelArray) {
            label.setVerticalTextPosition(JLabel.BOTTOM);
            label.setHorizontalTextPosition(JLabel.CENTER);
            label.addMouseListener(this);
         }
         imageArray = new Image[jlabelArray.length];

         // Set the place holder text
         clickLabel.setFont(font);
         clickLabel.setText("<html><I>Ctrl+Click the image you want to add</I>");

      } else if (reportType == TABLE) {
         remove(clickLabel);
         addTable();
      }
   }   
   
   public void addImage(Image img, String text) {

      // Validate
      if (reportType != IMAGE)
         return;
      
      // Set the image.
      if (img == null)
         return;

      if (text == null || text.length() == 0)
         return;

      // Remove the space reserved for the table.
      remove(jPanel2);

      // Get rid of "click to add" instruction.
      clickLabel.setText("");

      // Get the icon and scale.
      ImageIcon icon = new ImageIcon(img);
      double icon_scale = (double)ICON_HEIGHT/(double)icon.getIconHeight();
      ICON_WIDTH = Math.round((float)icon_scale*(float)icon.getIconWidth());
      double ratio = (double)icon.getIconWidth()/(double)icon.getIconHeight();
      WIDTH_TWIPS = Math.round((float)ratio*(float)HEIGHT_TWIPS);
      icon = new ImageIcon(getScaledImage(icon.getImage(), ICON_WIDTH, ICON_HEIGHT));

      // Fill in image.
      for (int i = 0; i < jlabelArray.length; i++) {
         if (jlabelArray[i].getText() != null && jlabelArray[i].getText().matches(text)) {
            return;
         }
      }
      for (int i = 0; i < jlabelArray.length; i++) {
        if (jlabelArray[i].getText() == null || jlabelArray[i].getText().length() == 0) {
            imageArray[i] = img;
            jlabelArray[i].setIcon(icon);
            jlabelArray[i].setText(text);
            jlabelArray[i].setName(Integer.toString(i));
            break;
         }
      }
      
   }

   public void addTable() {

      if (reportType != TABLE)
         return;

      // Set the image label.
      JTable table1 = jt.getJTable();      
      JTable table = new JTable(table1.getModel());
      JScrollPane jsp = new JScrollPane(table);
      jsp.setPreferredSize(jPanel2.getPreferredSize());
      jPanel2.setLayout(new BorderLayout());
      jPanel2.add(jsp);

      for(int i = 0; i < table.getColumnCount(); i++) {
          table.getColumnModel().getColumn(i).setCellRenderer(table1.getColumnModel().getColumn(i).getCellRenderer());
      }
   }

   /**
    * Resizes an image using a Graphics2D object backed by a BufferedImage.
    * @param srcImg - source image to scale
    * @param w - desired width
    * @param h - desired height
    * @return - the new resized image
    */
   private Image getScaledImage(Image srcImg, int w, int h) {
      BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2 = resizedImg.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2.drawImage(srcImg, 0, 0, w, h, null);
      g2.dispose();
      return resizedImg;
   }

   /**
    * Writes the report.
    */
   private void writeReport() {

      Rtf newcontent = null;
      try {
         if (reportType == IMAGE) {
            newcontent = getImageRtfContent();
         } else if (reportType == TABLE) {
            newcontent = getTableRtfContent();
         }

         if (reportFile.exists()) {
            boolean isRTF = isValidRTF();
            if (isRTF) {
               boolean success = appendToExistingReport(newcontent);
               if (!success) {
                  IJ.error("Unable to append to report file \"" + reportFile.getName() + "\".\n"
                          + "Please select a new file or give a new name.");
               } else {
                  close();
               }
            } else {
               IJ.error("Report file \"" + reportFile.getName() + "\" is not "
                          + "recognized as a valid RTF file. Please select a new file or give a new name.");
            }
         } else {
            newcontent.out(new FileWriter(reportFile));
            close();
         }
      } catch (Exception x) {
         x.printStackTrace();
         IJ.error("Error writing " + reportFile.getName());
      }
   }

   /**
    * Performs minimal amount of checking to see if the report
    * being appended to is a valid RTF file.
    * First it checks if the file begins with <code>{\rtf</code>.
    * Secondly it checks that the file end with <code>}</code>.
    *
    * @return <code>true</code> if valid rtf, otherwise <code>false</code>.
    */
   private boolean isValidRTF() {

      try {
         // Make sure document begins with {\rtf
         BufferedReader buffer = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(reportFile))));
         String first_line = buffer.readLine();
         if (!first_line.startsWith("{\\rtf"))
            return false;
         buffer.close();

         // Make sure document ends with closing brace.
         long last_cb_position = getLastPosition('}');
         long counter = getLastPosition(' ', false);
         if (counter - last_cb_position > 10)
            return false;

      } catch (IOException ioe) {
         return false;
      }

      return true;
   }

   /**
    * Appends newcontent to report file.
    *
    * @param newcontent - the new content
    * @return <code>true</code> if successfull
    */
   private boolean appendToExistingReport(Rtf newcontent) {
      try {
         long last_cb_position = getLastPosition('}');
         String s = newcontent.toString();
         RandomAccessFile out = new RandomAccessFile(reportFile, "rw");
         out.seek(last_cb_position);
         out.writeBytes(s);
         out.writeBytes("}");
         out.close();
      } catch (IOException ioe) {
         return false;
      }

      return true;
    }

    /**
     * Closes the report generator window and removes reference from main UI.
     */
    public void close() {
       dispose();
       
       //Check might be unnecessary, but first check if this is referenced
       //by the main UI. If so, delete reference.
       if(ui.getReportGenerator() == this) {
           ui.setReportGenerator(null);
       }
    }

    /**
    * Scans report for for the last occurance of the passed character <code>cb</code>.
    * If <code>null</code> is passed as an arguement, the method return the index of
    * the last character in the file.
    *
    * @param cb - the character
    * @returns the index of the last position of the character.
    */
   public long getLastPosition(char cb) {
      return getLastPosition(cb, true);
   }

   /**
    * Scans report for for the last occurance of the passed character <code>cb</code>.
    * If <code>lookingForSpecificCharacter</code> is false, the method return the index of
    * the last character in the file.
    *
    * @param cb - the character
    * @returns the index of the last position of the character.
    */
   private long getLastPosition(char cb, boolean lookingForSpecificCharacter) {

      long BAD_VALUE = -1;
      long counter = 0;
      long last_cb_position = 0;

      try {
      Reader buffer;
      InputStream in = new FileInputStream(reportFile);
      Reader reader = new InputStreamReader(in);
      buffer = new BufferedReader(reader);

      int c = 0;
      while ((c = buffer.read()) != -1) {
         char character = (char) c;
         if (lookingForSpecificCharacter) {
            if (character == cb) {
               last_cb_position = counter;
            }
         }
         counter++;
         }
      } catch(Exception e) {
         return BAD_VALUE;
      }

      if (!lookingForSpecificCharacter)
         last_cb_position = counter;

      if (last_cb_position < 5)
         return BAD_VALUE;

      return last_cb_position;
   }

   private Rtf getImageRtfContent() {

      // Build the ArrayList of RtfPictures.
      String namesString = "";      
      ByteArrayOutputStream baos;
      ArrayList<RtfPicture> rtfpicArray = new ArrayList<RtfPicture>();
      for (int i = 0; i < imageArray.length; i++) {
         Image img = imageArray[i];
         if (img != null) {
            namesString += jlabelArray[i].getText();
            if (i < imageArray.length - 1)
               namesString += ", ";            
            try {
               baos = new ByteArrayOutputStream();
               ImageIO.write((RenderedImage) img, "PNG", baos);
               rtfpicArray.add(picture(new ByteArrayInputStream(baos.toByteArray())).size(WIDTH_TWIPS,HEIGHT_TWIPS,RtfUnit.TWIPS));
            } catch (IOException ex) {
               return null;
            }
         }
      }

      // Convert to Array of RtfText
      RtfText[] rtfpics = new RtfText[rtfpicArray.size()];
      for (int i = 0; i < rtfpics.length; i++)
         rtfpics[i] = rtfpicArray.get(i).type(RtfPicture.PictureType.AUTOMATIC);

      // Build the content.
      Rtf newcontent = rtf().section(
              p(bold("DATE: "), getDate()),
              p((Object[])rtfpics),
              p(bold("Images: "), namesString),
              p(bold("Image File: "), ui.getOpener().getImageFile().getAbsolutePath()),
              p(bold("Raster: "), getRaster()),
              p(bold("Roi File: "), getRoiFileString()),
              p(bold("Notes: "), notesTextArea.getText()),
              p(""),
              p(""));    

      return newcontent;
   }

   private Rtf getTableRtfContent() throws IOException {

      // initialize
      DefaultTableModel dft = (DefaultTableModel) jt.getJTable().getModel();      
      int cols = dft.getColumnCount();
      int rows = dft.getRowCount();
      Object[] objs = new Object[cols];
      ArrayList rtfs_arr = new ArrayList<RtfPara>();

      // Date
      rtfs_arr.add(p(bold("DATE: "), getDate()));

      // Column headers
      for (int i = 0; i < cols; i++)
         objs[i] = dft.getColumnName(i);
      rtfs_arr.add(row(objs).bottomCellBorder().leftCellBorder().rightCellBorder());

      // Data
      for (int row = 0; row < rows; row++) {
         objs = new Object[cols];
         for (int col = 0; col < cols; col++) {
            Object value = dft.getValueAt(row, col);
            if (value instanceof Double) {
               objs[col] = formatter.format((Number) value);
            } else {
               objs[col] = value;
            }             
         }
         rtfs_arr.add(row(objs).leftCellBorder().rightCellBorder());
      }

      // Other
      rtfs_arr.add(p(bold("Image File: "), ui.getOpener().getImageFile().getAbsolutePath()));
      rtfs_arr.add(p(bold("Roi File: "), getRoiFileString()));
      rtfs_arr.add(p(bold("Notes: "), notesTextArea.getText()));
      rtfs_arr.add(p(""));
      rtfs_arr.add(p(""));


      RtfPara[] rtf_para = new RtfPara[rtfs_arr.size()];
      for (int index = 0; index < rtfs_arr.size(); index++) {
         rtf_para[index] = (RtfPara)rtfs_arr.get(index);
      }
      Rtf newcontent = rtf().section(rtf_para);

      return newcontent;
   }


   private String getDate() {
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
      String dateText = dateFormat.format(date);
      return dateText;
   }

   private String getRaster() {
      String raster = ui.getOpener().getRaster();
      if (!raster.contains(",")) {
         int ras = Math.round((Float.parseFloat(raster))/1000.0f);
         raster = String.valueOf(ras);
      }
      return raster;
   }

   private String getRoiFileString() {
            // roi file
      String roiFileString = "null";
      File roiFile = ui.getRoiManager().getRoiFile();
      if (roiFile != null)
         roiFileString = roiFile.getAbsolutePath();
      return roiFileString;
   }

   public void mouseClicked(MouseEvent e) {
      
   }

   public void mousePressed(MouseEvent e) {
      if (e.isPopupTrigger()) {
         currentLabel = (JLabel)e.getSource();
         jp.show(currentLabel, e.getX(), e.getY());
      } 
   }

   public void mouseEntered(MouseEvent e) {
      
   }

   public void mouseExited(MouseEvent e) {
      
   }

   @Override
   public void mouseReleased(MouseEvent e) {

   }
}