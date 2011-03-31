package com.nrims;

import ij.IJ;
import ij.Prefs;

/**
 * This class creates a user preferences interface. It
 * opens as a frame and allows user to enter parameters
 * and settings that control the applications behavior.
 * It is built upon the ImageJ Preferences class.
 * These settings are stored in the ImageJ preferences
 * file unually located in ~HOME_DIR/.imagej/IJ_Prefs.txt.
 * All Open_Mims settings are preceded with the "openmims."
 * string.
 *
 * @author  cpoczatek
 */
public class PrefFrame extends PlugInJFrame {

    //WARNING!!!!
    //correct preferences will --NOT-- be read
    //correctly if running in Netbeans
    //but --WILL-- if running outside

    boolean includeHSI = true;
    boolean includeSum = true;
    boolean includeMass = false;
    boolean includeRatio = false;
    double scaleFactor = 10000;
    double ratioSpan = 1.5;
    boolean ratioReciprocals = false;
    UI ui;
    float reference = (float) 0.0130;
    float background = (float) 0.0037;
    String numerators = "";
    String denominators = "";
    double massDiff = 0.5;

    final String PREFS_KEY = "openmims.";

    /** Instantiates the class and creates the frame.*/
    public PrefFrame(UI ui) {
        super("Preferences");
        this.ui = ui;
        readPreferences();
        initComponents();
        initComponentsCustom();
    }

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      jLabel5 = new javax.swing.JLabel();
      jLabel6 = new javax.swing.JLabel();
      jLabel1 = new javax.swing.JLabel();
      HSIcheckbox = new javax.swing.JCheckBox();
      sumCheckbox = new javax.swing.JCheckBox();
      massCheckbox = new javax.swing.JCheckBox();
      ratioCheckbox = new javax.swing.JCheckBox();
      jLabel2 = new javax.swing.JLabel();
      scaleFactorTextbox = new javax.swing.JTextField();
      jButton1 = new javax.swing.JButton();
      jButton2 = new javax.swing.JButton();
      ratioSpanTextbox = new javax.swing.JTextField();
      jLabel3 = new javax.swing.JLabel();
      ratioReciprocalsCheckBox = new javax.swing.JCheckBox();
      jLabel4 = new javax.swing.JLabel();
      backgroundTextField = new javax.swing.JTextField();
      jLabel7 = new javax.swing.JLabel();
      referenceTextField = new javax.swing.JTextField();

      jLabel5.setText("Percent turnover, background:");

      jLabel6.setText("Percent turnover, background:");

      jLabel1.setText("When exporting images:");

      HSIcheckbox.setText("include HSI images");

      sumCheckbox.setText("include sum images");

      massCheckbox.setText("include mass images");

      ratioCheckbox.setText("include ratio images");

      jLabel2.setText("Ratio scale factor:");

      jButton1.setText("Cancel");
      jButton1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton1ActionPerformed(evt);
         }
      });

      jButton2.setText("Save");
      jButton2.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton2ActionPerformed(evt);
         }
      });

      jLabel3.setText("Ratio span:");

      ratioReciprocalsCheckBox.setText("include reciprocals");

      jLabel4.setText("Percent turnover, background:");

      jLabel7.setText("Percent turnover, maximum:");

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGap(181, 181, 181)
                  .addComponent(jButton1)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addContainerGap()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jLabel1)
                     .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                           .addComponent(sumCheckbox)
                           .addComponent(massCheckbox)
                           .addComponent(HSIcheckbox)
                           .addComponent(ratioCheckbox)))
                     .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                           .addComponent(jLabel2)
                           .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                           .addComponent(ratioSpanTextbox, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                           .addComponent(scaleFactorTextbox, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                     .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(ratioReciprocalsCheckBox))))
               .addGroup(layout.createSequentialGroup()
                  .addContainerGap()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jLabel4)
                     .addComponent(jLabel7))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addComponent(referenceTextField)
                     .addComponent(backgroundTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE))))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel1)
            .addGap(8, 8, 8)
            .addComponent(HSIcheckbox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(sumCheckbox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(massCheckbox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(ratioCheckbox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jLabel2)
               .addComponent(scaleFactorTextbox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jLabel3)
               .addComponent(ratioSpanTextbox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(ratioReciprocalsCheckBox)
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jLabel4)
               .addComponent(backgroundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jLabel7)
               .addComponent(referenceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jButton2)
               .addComponent(jButton1))
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

/** Action method for the "save" button.*/
private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    savePreferences();
}//GEN-LAST:event_jButton2ActionPerformed

/** Action method for the "cancel" button.*/
private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    close();
}//GEN-LAST:event_jButton1ActionPerformed

    private void initComponentsCustom() {

        this.HSIcheckbox.setSelected(includeHSI);
        this.ratioCheckbox.setSelected(includeRatio);
        this.massCheckbox.setSelected(includeMass);
        this.sumCheckbox.setSelected(includeSum);

        this.scaleFactorTextbox.setText(new Double(scaleFactor).toString());
        this.ratioSpanTextbox.setText(new Double(ratioSpan).toString());
        this.ratioReciprocalsCheckBox.setSelected(ratioReciprocals);
        this.backgroundTextField.setText(new Float(background).toString());
        this.referenceTextField.setText(new Float(reference).toString());
    }

    /** Reads the preferences and sets member variables accordingly.*/
    private void readPreferences() {
        includeHSI = (boolean) Prefs.get(PREFS_KEY + "includeHSI", includeHSI);
        includeSum = (boolean) Prefs.get(PREFS_KEY + "includeSum", includeSum);
        includeMass = (boolean) Prefs.get(PREFS_KEY + "includeMass", includeMass);
        includeRatio = (boolean) Prefs.get(PREFS_KEY + "includeRatio", includeRatio);
        scaleFactor = (double) Prefs.get(PREFS_KEY + "ratioScaleFactor", scaleFactor);
        ratioSpan = (double) Prefs.get(PREFS_KEY + "ratioSpan", ratioSpan);
        ratioReciprocals = (boolean) Prefs.get(PREFS_KEY + "ratioReciprocals", ratioReciprocals);
        reference = (float) Prefs.get(PREFS_KEY + "reference", reference);
        background = (float) Prefs.get(PREFS_KEY + "background", background);
        numerators = Prefs.get(PREFS_KEY + "numerators", numerators);
        denominators = Prefs.get(PREFS_KEY + "denominators", denominators);
    }

    /** Saves the preferences file.*/
    public void savePreferences() {
        includeHSI = HSIcheckbox.isSelected();
        includeSum = sumCheckbox.isSelected();
        includeMass = massCheckbox.isSelected();
        includeRatio = ratioCheckbox.isSelected();
        ratioReciprocals = ratioReciprocalsCheckBox.isSelected();

        try {
            scaleFactor = new Double(scaleFactorTextbox.getText());
        } catch (Exception e) {
           IJ.error("Malformed \"scale factor\" value.");
           return;
        }

        if (ui.getHSIView() != null)
           ui.getHSIView().setRatioScaleFactor(scaleFactor);

        try {
           ratioSpan = new Double(ratioSpanTextbox.getText());
        } catch (Exception e) {
           IJ.error("Malformed \"ratio span\" value.");
           return;
        }

        try {
           background = new Float(backgroundTextField.getText());
        } catch (Exception e) {
           IJ.error("Malformed \"background\" value.");
           return;
        }

        try {
           reference = new Float(referenceTextField.getText());
        } catch (Exception e) {
           IJ.error("Malformed \"reference\" value.");
           return;
        }
        
        Prefs.set(PREFS_KEY + "includeHSI", includeHSI);
        Prefs.set(PREFS_KEY + "includeSum", includeSum);
        Prefs.set(PREFS_KEY + "includeMass", includeMass);
        Prefs.set(PREFS_KEY + "includeRatio", includeRatio);
        Prefs.set(PREFS_KEY + "ratioScaleFactor", scaleFactor);
        Prefs.set(PREFS_KEY + "ratioSpan", ratioSpan);
        Prefs.set(PREFS_KEY + "ratioReciprocals", ratioReciprocals);
        Prefs.set(PREFS_KEY + "background", background);
        Prefs.set(PREFS_KEY + "reference", reference);
        Prefs.set(PREFS_KEY + "numerators", numerators);
        Prefs.set(PREFS_KEY + "denominators", denominators);
        Prefs.savePreferences();
        close();
    }

    /** Shows the frame.*/
    public void showFrame() {
        initComponentsCustom();
        setVisible(true);
        toFront();
        setExtendedState(NORMAL);
    }

    @Override
    public void close() {
        setVisible(false);
    }

    /**
     * Include HSI images when exporting images?
     * @return boolean.
     */
    boolean getincludeHSI() {
        return includeHSI;
    }

    /**
     * Include Sum images when exporting images?
     * @return boolean.
     */
    boolean getincludeSum() {
        return includeSum;
    }

    /**
     * Include Mass images when exporting images?
     * @return boolean.
     */
    boolean getincludeMass() {
        return includeMass;
    }

    /**
     * Include Ratio images when exporting images?
     * @return boolean.
     */
    boolean getincludeRatio() {
        return includeRatio;
    }

    /**
     * Gets the scale factor.
     * @return the scale factor.
     */
    double getscaleFactor() {
        return scaleFactor;
    }

    /**
     * Gets the difference allowed between atomic numbers
     * in order to show ratio images in the list.
     * @return double
     */
    double getRatioSpan() {
        return ratioSpan;
    }

    /**
     * Gets the difference in mass allowed between atomic numbers
     * for ratio images saved by the user.
     * @return double
     */
    double getMassDiff() {
        return massDiff;
    }

    /**
     * Include reciprocals in the ratio image list (13/12 and 12/13).
     * @return boolean
     */
    boolean getRatioReciprocals() {
        return ratioReciprocals;
    }

    /**
     * Get the background ratio reference.
     * @return the background ratio
     */
    float getBackgroundRatio(){
       return background;
    }

    /**
     * Get the source ratio reference.
     * @return the background ratio
     */
    float getReferenceRatio(){
       return reference;
    }

    /**
     * Get the list of ratio images preferred by the user.
     */
    String[] getNumerators() {
       String[] numStrArray = new String[0];
       if (numerators != null)
          numStrArray = numerators.split(",");
       return numStrArray;
    }

    /**
     * Get the list of ratio images preferred by the user.
     */
    String[] getDenominators() {
       String[] denStrArray = new String[0];
       if (denominators != null)
          denStrArray = denominators.split(",");
       return denStrArray;
    }
    
    /**
     * Add a ratio image with numerator num and
     * denominator den to the list of default ratio images.     
     */
    void addRatioImage(double num, double den) {
       if (numerators == null || numerators.length() == 0 || numerators.length() != denominators.length()) {
          numerators = "";
          denominators = "";
       } else {
          numerators += ",";
          denominators += ",";
       }
       numerators += Double.toString(num);
       denominators += Double.toString(den);
    }

    /**
     * Add a ratio image with numerator num and
     * denominator den to the list of default ratio images.
     */
    void addRatioImage(String num, String den) {

       Double numValue, denValue;
       try {
          numValue = Double.parseDouble(num);
          denValue = Double.parseDouble(den);
       } catch (NumberFormatException nfe) {
          return;
       }
       addRatioImage(numValue, denValue);
    }

    /**
     * Remove a ratio image with numerator num, and
     * denominator den, to the list of default ratio images.
     */
    void removeRatioImage(double num, double den) {
       if (numerators == null || numerators.length() == 0) {
          return;
       }
       String[] numStrArray = numerators.split(",");
       String[] denStrArray = denominators.split(",");
       if (numStrArray.length != denStrArray.length) {
          return;
       }

       // Clear the numerator and denominator list and 
       // add back those ratio images that are NOT to be removed.
       numerators = null;
       denominators = null;
       Double numValue, denValue, numDiff, denDiff;
       for (int i = 0; i < numStrArray.length; i++) {

          try {
             numValue = Double.parseDouble(numStrArray[i]);
             denValue = Double.parseDouble(denStrArray[i]);
          } catch (NumberFormatException nfe) {
             continue;
          }

          numDiff = Math.abs(num - numValue);
          denDiff = Math.abs(den - denValue);
          System.out.println();
          System.out.println("numValue = " + numValue);
          System.out.println("numDiff = " + numDiff + ", denDiff = " + denDiff);
          System.out.println(numDiff < massDiff && denDiff < massDiff);
          if (numDiff > massDiff && denDiff > massDiff) {
             addRatioImage(numValue, denValue);
          }
       }
       System.out.println();
       System.out.println("numerators = " + numerators);

    }

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JCheckBox HSIcheckbox;
   private javax.swing.JTextField backgroundTextField;
   private javax.swing.JButton jButton1;
   private javax.swing.JButton jButton2;
   private javax.swing.JLabel jLabel1;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JLabel jLabel3;
   private javax.swing.JLabel jLabel4;
   private javax.swing.JLabel jLabel5;
   private javax.swing.JLabel jLabel6;
   private javax.swing.JLabel jLabel7;
   private javax.swing.JCheckBox massCheckbox;
   private javax.swing.JCheckBox ratioCheckbox;
   private javax.swing.JCheckBox ratioReciprocalsCheckBox;
   private javax.swing.JTextField ratioSpanTextbox;
   private javax.swing.JTextField referenceTextField;
   private javax.swing.JTextField scaleFactorTextbox;
   private javax.swing.JCheckBox sumCheckbox;
   // End of variables declaration//GEN-END:variables
}
