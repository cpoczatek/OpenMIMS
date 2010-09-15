package com.nrims;

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
    int scaleFactor = 10000;
    double ratioSpan = 1.5;
    boolean ratioReciprocals = false;
    
    final String PREFS_KEY = "openmims.";

    /** Instantiates the class and creates the frame.*/
    public PrefFrame() {
        super("Preferences");
        readPreferences();
        initComponents();
        initComponentsCustom();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

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
                                .addComponent(ratioReciprocalsCheckBox)))))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 96, Short.MAX_VALUE)
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

        this.scaleFactorTextbox.setText(new Integer(scaleFactor).toString());
        this.ratioSpanTextbox.setText(new Double(ratioSpan).toString());
        this.ratioReciprocalsCheckBox.setSelected(ratioReciprocals);

    }

    /** Reads the preferences and sets member variables accordingly.*/
    private void readPreferences() {
        includeHSI = (boolean) Prefs.get(PREFS_KEY + "includeHSI", includeHSI);
        includeSum = (boolean) Prefs.get(PREFS_KEY + "includeSum", includeSum);
        includeMass = (boolean) Prefs.get(PREFS_KEY + "includeMass", includeMass);
        includeRatio = (boolean) Prefs.get(PREFS_KEY + "includeRatio", includeRatio);
        scaleFactor = (int) Prefs.get(PREFS_KEY + "ratioScaleFactor", scaleFactor);
        ratioSpan = (double) Prefs.get(PREFS_KEY + "ratioSpan", ratioSpan);
        ratioReciprocals = (boolean) Prefs.get(PREFS_KEY + "ratioReciprocals", ratioReciprocals);
    }

    /** Saves the preferences file.*/
    private void savePreferences() {
        includeHSI = HSIcheckbox.isSelected();
        includeSum = sumCheckbox.isSelected();
        includeMass = massCheckbox.isSelected();
        includeRatio = ratioCheckbox.isSelected();
        try {
            scaleFactor = new Integer(scaleFactorTextbox.getText());
            ratioSpan = new Double(ratioSpanTextbox.getText());
        } catch (Exception e) {
        }

        ratioReciprocals = ratioReciprocalsCheckBox.isSelected();

        Prefs.set(PREFS_KEY + "includeHSI", includeHSI);
        Prefs.set(PREFS_KEY + "includeSum", includeSum);
        Prefs.set(PREFS_KEY + "includeMass", includeMass);
        Prefs.set(PREFS_KEY + "includeRatio", includeRatio);
        Prefs.set(PREFS_KEY + "ratioScaleFactor", scaleFactor);
        Prefs.set(PREFS_KEY + "ratioSpan", ratioSpan);
        Prefs.set(PREFS_KEY + "ratioReciprocals", ratioReciprocals);
        Prefs.savePreferences();
        close();
    }

    /** Shows the frame.*/
    public void showFrame() {
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
    int getscaleFactor() {
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
     * Include reciprocals in the ratio image list (13/12 and 12/13).
     * @return boolean
     */
    boolean getRatioReciprocals() {
        return ratioReciprocals;
    }

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox HSIcheckbox;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JCheckBox massCheckbox;
    private javax.swing.JCheckBox ratioCheckbox;
    private javax.swing.JCheckBox ratioReciprocalsCheckBox;
    private javax.swing.JTextField ratioSpanTextbox;
    private javax.swing.JTextField scaleFactorTextbox;
    private javax.swing.JCheckBox sumCheckbox;
    // End of variables declaration//GEN-END:variables
}
