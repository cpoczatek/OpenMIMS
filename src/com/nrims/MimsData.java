package com.nrims;

/**
 * The MimsData class creates the "Mims Data" tabbed panel.
 * It sole purpose is displaying meta-data associated with
 * the current image. If additional images are opened in the
 * same session (e.g via concatenate) this data will not change.
 * It will continue to reflect metadata associated with the
 * first image opened in the current session.
 *
 * @author  Douglas Benson
 */
public class MimsData extends javax.swing.JPanel {
    
    /**
     * Creates new form MimsData
     */
    public MimsData(com.nrims.UI ui, com.nrims.data.Opener image) {
        initComponents();
        this.ui = ui ;
        setMimsImage(image);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      jLabel1 = new javax.swing.JLabel();
      jLabel2 = new javax.swing.JLabel();
      jLabel3 = new javax.swing.JLabel();
      jLabel4 = new javax.swing.JLabel();
      jLabel5 = new javax.swing.JLabel();
      jLabel6 = new javax.swing.JLabel();
      jLabel7 = new javax.swing.JLabel();
      jLabel8 = new javax.swing.JLabel();
      jLabel9 = new javax.swing.JLabel();
      jLabel10 = new javax.swing.JLabel();
      jLabel11 = new javax.swing.JLabel();
      jLabel12 = new javax.swing.JLabel();
      jLabel13 = new javax.swing.JLabel();
      jLabel14 = new javax.swing.JLabel();
      jLabel15 = new javax.swing.JLabel();
      jLabel16 = new javax.swing.JLabel();
      syncjCheckBox = new javax.swing.JCheckBox();
      jLabel17 = new javax.swing.JLabel();
      jLabel18 = new javax.swing.JLabel();
      jLabel19 = new javax.swing.JLabel();
      jLabel20 = new javax.swing.JLabel();
      jLabel21 = new javax.swing.JLabel();
      jLabel22 = new javax.swing.JLabel();
      jLabel23 = new javax.swing.JLabel();
      jLabel24 = new javax.swing.JLabel();
      jLabel25 = new javax.swing.JLabel();
      jLabel26 = new javax.swing.JLabel();
      jLabel27 = new javax.swing.JLabel();
      jLabel28 = new javax.swing.JLabel();
      jLabel29 = new javax.swing.JLabel();
      jLabel30 = new javax.swing.JLabel();

      jLabel1.setText("File");

      jLabel2.setText("Images/Mass");

      jLabel3.setText("Position");

      jLabel4.setText("Date");

      jLabel5.setText("User");

      jLabel6.setText("Z position");

      jLabel7.setText("Dwell Time");

      jLabel8.setText("Raster");

      jLabel9.setText("jLabel9");

      jLabel10.setText("jLabel10");

      jLabel11.setText("jLabel11");

      jLabel12.setText("jLabel12");

      jLabel13.setText("jLabel13");

      jLabel14.setText("jLabel14");

      jLabel15.setText("jLabel15");

      jLabel16.setText("jLabel16");

      syncjCheckBox.setSelected(true);
      syncjCheckBox.setText("Synchronize Stacks");
      syncjCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
      syncjCheckBox.addItemListener(new java.awt.event.ItemListener() {
         public void itemStateChanged(java.awt.event.ItemEvent evt) {
            syncjCheckBoxItemStateChanged(evt);
         }
      });

      jLabel17.setText("No. of Masses");

      jLabel18.setText("jLabel18");

      jLabel19.setText("Duration");

      jLabel20.setText("jLabel20");

      jLabel21.setText("Pixels");

      jLabel22.setText("jLabel22");

      jLabel23.setText("Path");

      jLabel24.setText("jLabel24");

      jLabel25.setText("Symbols");

      jLabel26.setText("jLabel26");

      jLabel27.setText("DT Corrected");

      jLabel28.setText("jLabel27");

      jLabel29.setText("QSA Corrected");

      jLabel30.setText("jLabel30");

      org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(layout.createSequentialGroup()
            .addContainerGap()
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(jLabel17)
               .add(jLabel1)
               .add(jLabel23)
               .add(jLabel25)
               .add(jLabel2)
               .add(jLabel3)
               .add(jLabel7)
               .add(jLabel19)
               .add(jLabel8)
               .add(jLabel21)
               .add(jLabel27)
               .add(jLabel29)
               .add(jLabel4)
               .add(jLabel6)
               .add(jLabel5))
            .add(21, 21, 21)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
               .add(jLabel28)
               .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                  .add(jLabel26, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
                  .add(165, 165, 165))
               .add(jLabel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(jLabel11)
               .add(jLabel15)
               .add(jLabel20)
               .add(jLabel16)
               .add(jLabel22)
               .add(jLabel18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 467, Short.MAX_VALUE)
               .add(jLabel13)
               .add(jLabel12)
               .add(jLabel14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
               .add(layout.createSequentialGroup()
                  .add(jLabel30)
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 175, Short.MAX_VALUE)
                  .add(syncjCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 240, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
               .add(jLabel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 467, Short.MAX_VALUE)
               .add(jLabel24, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 467, Short.MAX_VALUE))
            .addContainerGap())
      );

      layout.linkSize(new java.awt.Component[] {jLabel11, jLabel12, jLabel13, jLabel15, jLabel16}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

      layout.setVerticalGroup(
         layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(layout.createSequentialGroup()
            .addContainerGap()
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel1)
               .add(jLabel9))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel23)
               .add(jLabel24))
            .add(11, 11, 11)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel17)
               .add(jLabel18))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel25)
               .add(jLabel26))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel2)
               .add(jLabel10))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel3)
               .add(jLabel11))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel6)
               .add(jLabel14))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel4)
               .add(jLabel12))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel5)
               .add(jLabel13))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel7)
               .add(jLabel15))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel19)
               .add(jLabel20))
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
               .add(jLabel8)
               .add(jLabel16))
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
               .add(layout.createSequentialGroup()
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(jLabel21)
                     .add(jLabel22))
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(jLabel27)
                     .add(jLabel28))
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                     .add(jLabel29)
                     .add(jLabel30))
                  .addContainerGap(27, Short.MAX_VALUE))
               .add(layout.createSequentialGroup()
                  .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                  .add(syncjCheckBox)
                  .addContainerGap())))
      );
   }// </editor-fold>//GEN-END:initComponents

    private void syncjCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_syncjCheckBoxItemStateChanged
        ui.setSyncStack(syncjCheckBox.isSelected());
}//GEN-LAST:event_syncjCheckBoxItemStateChanged

    /**
     * Enables the sync checkbox. Only needs to be
     * enabled when more than 1 plane.
     * @param bOn more than 1 plane.
     */
    public void setHasStack(boolean bOn) {
        syncjCheckBox.setEnabled(bOn);
    }

    /**
     * Set the meta -data fields based on the contents
     * of the <code>Opener</code> object.
     * @param image the <code>Opener</code> object. 
     */
    public void setMimsImage( com.nrims.data.Opener image ) {
        if(image == null) {
            jLabel9.setText("");
            jLabel24.setText("");
            jLabel10.setText("0");
            jLabel11.setText("");
            jLabel12.setText("");
            jLabel13.setText("");
            jLabel14.setText("");
            jLabel15.setText("");
            jLabel16.setText("");
            jLabel18.setText("0");
            jLabel26.setText("");
            jLabel22.setText("0");
            jLabel28.setText("false");
            jLabel30.setText("false");
            syncjCheckBox.setEnabled(false);        
        } else {
            String tempstring = "";
            tempstring = image.getImageFile().getName();
            jLabel9.setText(tempstring);

            tempstring = image.getImageFile().getParent();
            jLabel24.setText(tempstring);

            tempstring = "" + image.getNImages();
            jLabel10.setText(tempstring);
            
            tempstring = image.getPosition();
            jLabel11.setText(tempstring);
            
            tempstring = image.getSampleDate() + " " + image.getSampleHour();
            jLabel12.setText(tempstring);
            
            tempstring = image.getUserName();
            jLabel13.setText(tempstring);
            
            tempstring = image.getZPosition();
            jLabel14.setText(tempstring);
            
            tempstring = image.getDwellTime() + " ms/px    " + Double.toString(image.getCountTime()) + " s/plane";
            jLabel15.setText(tempstring);
            
            tempstring = image.getDuration() + " s";
            jLabel20.setText(tempstring);
            
            jLabel28.setText(Boolean.toString(image.isDTCorrected()));

            jLabel30.setText(Boolean.toString(image.isQSACorrected()));
            

            //TODO
            //THIS IS A BUG
            //if getRaster returns null things throw
            //there should be ZERO required k/v pairs in a nrrd header
            String raster = image.getRaster();
            if (!raster.contains(",")) {
               int ras = Math.round((Float.parseFloat(raster))/1000.0f);
               raster = String.valueOf(ras);
            }
            tempstring = raster + " um";
            jLabel16.setText(tempstring);
            
            int i, nMasses = image.getNMasses();
            String massNames = "" + nMasses + " [" ;
            String massSymbols = "" + nMasses + " [";
            
            //TODO
            //THIS IS A BUG
            //if this k/v pair is bad (ie length<nMass)
            //throws, should fail gracefully
            for(i=0;i<nMasses;i++) {
                massNames += image.getMassNames()[i];
                if( i+1 != nMasses ) massNames += ", ";
                else massNames += "]";
            }
            if (image.getMassSymbols() == null) {
                massSymbols += " ]";
            } else {
                //TODO
                //THIS IS A BUG
                //if this k/v pair is bad (ie length<nMass)
                //throws, should fail gracefully
                for (i = 0; i < nMasses; i++) {
                    massSymbols += image.getMassSymbols()[i];
                    if (i + 1 != nMasses) {
                        massSymbols += ", ";
                    } else {
                        massSymbols += "]";
                    }
                }
            }
            jLabel18.setText(massNames);
            jLabel26.setText(massSymbols);
            jLabel22.setText(image.getWidth() +" x " + image.getHeight());
            
            syncjCheckBox.setEnabled(image.getNImages() > 1);
        }
    }
    
    private com.nrims.UI ui = null ;
   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel jLabel1;
   private javax.swing.JLabel jLabel10;
   private javax.swing.JLabel jLabel11;
   private javax.swing.JLabel jLabel12;
   private javax.swing.JLabel jLabel13;
   private javax.swing.JLabel jLabel14;
   private javax.swing.JLabel jLabel15;
   private javax.swing.JLabel jLabel16;
   private javax.swing.JLabel jLabel17;
   private javax.swing.JLabel jLabel18;
   private javax.swing.JLabel jLabel19;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JLabel jLabel20;
   private javax.swing.JLabel jLabel21;
   private javax.swing.JLabel jLabel22;
   private javax.swing.JLabel jLabel23;
   private javax.swing.JLabel jLabel24;
   private javax.swing.JLabel jLabel25;
   private javax.swing.JLabel jLabel26;
   private javax.swing.JLabel jLabel27;
   private javax.swing.JLabel jLabel28;
   private javax.swing.JLabel jLabel29;
   private javax.swing.JLabel jLabel3;
   private javax.swing.JLabel jLabel30;
   private javax.swing.JLabel jLabel4;
   private javax.swing.JLabel jLabel5;
   private javax.swing.JLabel jLabel6;
   private javax.swing.JLabel jLabel7;
   private javax.swing.JLabel jLabel8;
   private javax.swing.JLabel jLabel9;
   private javax.swing.JCheckBox syncjCheckBox;
   // End of variables declaration//GEN-END:variables
    
}
