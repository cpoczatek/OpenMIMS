/*
 * convertManager.java
 *
 * Created on Jul 5, 2011, 3:54:47 PM
 */

package com.nrims.managers;

import com.nrims.Converter;
import com.nrims.MimsJFileChooser;
import com.nrims.UI;
import java.awt.Cursor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

/**
 *
 * @author zkaufman
 */
public class convertManager extends JFrame implements PropertyChangeListener {

    UI ui;
    File[] files;
    Converter co;

    /** Creates new form convertManager */
    public convertManager(UI ui) {
        this.ui=ui;
        initComponents();
        setLocation(ui.getLocation().x+50, ui.getLocation().y+50);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {
      bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

      trackCheckBox = new javax.swing.JCheckBox();
      massTextField = new javax.swing.JTextField();
      okButton = new javax.swing.JButton();
      cancelButton = new javax.swing.JButton();
      fileListComboBox = new javax.swing.JComboBox();
      selectFilesButton = new javax.swing.JButton();
      jLabel1 = new javax.swing.JLabel();
      progressBar = new javax.swing.JProgressBar();

      setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

      trackCheckBox.setText("Auto track");

      massTextField.setText("mass");

      org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, trackCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), massTextField, org.jdesktop.beansbinding.BeanProperty.create("editable"));
      bindingGroup.addBinding(binding);

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

      selectFilesButton.setText("Select files...");
      selectFilesButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            selectFilesButtonActionPerformed(evt);
         }
      });

      jLabel1.setText("(e.g. 26)");

      binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, trackCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), jLabel1, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
      bindingGroup.addBinding(binding);

      progressBar.setStringPainted(true);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 433, Short.MAX_VALUE)
                  .addContainerGap())
               .addGroup(layout.createSequentialGroup()
                  .addComponent(fileListComboBox, 0, 433, Short.MAX_VALUE)
                  .addContainerGap())
               .addGroup(layout.createSequentialGroup()
                  .addComponent(trackCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(massTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(jLabel1)
                  .addGap(217, 217, 217))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(selectFilesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addContainerGap())
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(cancelButton)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addGap(10, 10, 10))))
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(selectFilesButton)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(fileListComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(trackCheckBox)
               .addComponent(massTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(jLabel1))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(cancelButton)
               .addComponent(okButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      bindingGroup.bind();

      pack();
   }// </editor-fold>//GEN-END:initComponents

    private void selectFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectFilesButtonActionPerformed
       selectFiles();
    }//GEN-LAST:event_selectFilesButtonActionPerformed

    public void selectFiles() {
       MimsJFileChooser mjfc = new MimsJFileChooser(ui);
       mjfc.setMultiSelectionEnabled(true);
       mjfc.setPreferredSize(new java.awt.Dimension(650, 500));
       int returnVal = mjfc.showOpenDialog(this);

       // Open file or return null.
       if (returnVal == JFileChooser.APPROVE_OPTION) {
          files = mjfc.getSelectedFiles();
       }

       for (File file : files) {
          if (file.isFile())
             fileListComboBox.addItem(file.getName());
       }
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
       co.proceed(false);
       setCursor(null);
       close();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

       okButton.setEnabled(false);
       setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

       // Return if file list is empty (user did not select files).
       if (files == null || files.length == 0) {
          ij.IJ.error("No files selected");
          setCursor(null);
          okButton.setEnabled(true);
          return;
       }

       // Add selected files to ArrayList.
       ArrayList<String> fileNames = new ArrayList<String>();
       for (File file : files) {
          fileNames.add(file.getAbsolutePath());
       }

       // Make sure user enters valid mass.
       if (trackCheckBox.isSelected()) {
       try {
          double massString = new Double(massTextField.getText());
       } catch (Exception e) {
          ij.IJ.error("\"" + massTextField.getText() + "\"" + " is not a valid mass value.");
          setCursor(null);
          okButton.setEnabled(true);
          return;
       }
       }

       // Initialize and run Converter object.
       co = new Converter(false, false, trackCheckBox.isSelected(), massTextField.getText(), null);
       co.setFiles(fileNames);
       co.addPropertyChangeListener(this);
       co.execute();

    }//GEN-LAST:event_okButtonActionPerformed
  
   /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
       if ("progress".equals(evt.getPropertyName())) {
          int progress = (Integer) evt.getNewValue();
          progressBar.setValue(progress);
       } else if (evt.getPropertyName().matches("state") && evt.getNewValue().toString().matches("DONE")) {
          setCursor(null);
          close();
       }
    }
    
   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton cancelButton;
   private javax.swing.JComboBox fileListComboBox;
   private javax.swing.JLabel jLabel1;
   private javax.swing.JTextField massTextField;
   private javax.swing.JButton okButton;
   private javax.swing.JProgressBar progressBar;
   private javax.swing.JButton selectFilesButton;
   private javax.swing.JCheckBox trackCheckBox;
   private org.jdesktop.beansbinding.BindingGroup bindingGroup;
   // End of variables declaration//GEN-END:variables

   private void close() {
      setVisible(false);
   }
}
