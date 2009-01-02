/*
 * mimsTomography.java
 *
 * Created on December 20, 2007, 3:00 PM
 */

package com.nrims;
import ij.*;


/**
 *
 * @author  cpoczatek
 */
public class MimsTomography extends javax.swing.JPanel {
    
    /** Creates new form mimsTomography */
    public MimsTomography(com.nrims.UI ui, com.nrims.data.Opener im) {
        System.out.println("MimsTomography constructor");
        initComponents();
        this.ui = ui ;
        this.image = im;
        this.images = ui.getMassImages();
        numberMasses = image.nMasses();
        imagestacks = new ImageStack[numberMasses];
        rp = ui.getOpenRatioImages();
                
        for (int i=0; i<=(numberMasses-1); i++) {
            imagestacks[i]=this.images[i].getStack();
        }
        
        tomographyChart = new MimsJFreeChart(ui,im);
        
        //some swing component cleanup, whee...
        lowerSlider.setMaximum(image.nImages());
        upperSlider.setMaximum(image.nImages());
        upperSlider.setValue(upperSlider.getMaximum());
        
        lowerSlider.setMajorTickSpacing((int)(lowerSlider.getMaximum()/8)+1);
        upperSlider.setMajorTickSpacing((int)(upperSlider.getMaximum()/8)+1);
        
        imageJList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = image.getMassNames();
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        
    }
    
    public String squishStrings(String[] strings, String spacer) {
        String temp = spacer;
        for(int i=0; i<strings.length; i++)
            temp = temp + strings[i] + spacer;
        return temp;
    }
    
     
     /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lowerSlider = new javax.swing.JSlider();
        upperSlider = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        statJList = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        imageJList = new javax.swing.JList();
        jLabel4 = new javax.swing.JLabel();
        plotButton = new javax.swing.JButton();

        setToolTipText("");

        lowerSlider.setFont(new java.awt.Font("Dialog", 0, 10));
        lowerSlider.setMinimum(1);
        lowerSlider.setPaintLabels(true);
        lowerSlider.setPaintTicks(true);
        lowerSlider.setValue(1);
        lowerSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                lowerSliderStateChanged(evt);
            }
        });

        upperSlider.setFont(new java.awt.Font("Dialog", 0, 10));
        upperSlider.setMinimum(1);
        upperSlider.setPaintLabels(true);
        upperSlider.setPaintTicks(true);
        upperSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                upperSliderStateChanged(evt);
            }
        });

        jLabel1.setText("Lower limit");

        jLabel2.setText("Upper limit");

        jLabel3.setText("Statistics to plot");

        statJList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "sum", "mean", "stddev", "min", "max", "mode", "area", "xcentroid", "ycentroid", "xcentermass", "ycentermass", "roix", "roiy", "roiwidth", "roiheight", "major", "minor", "angle", "feret", "median", "kurtosis", "areafraction", "perimeter" };
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(30, 30, 30)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1)
                                .addComponent(lowerSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel2)
                                .addComponent(upperSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(plotButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(10, 10, 10)
                        .addComponent(lowerSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(11, 11, 11)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(upperSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4))
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE))))
                .addGap(18, 18, 18)
                .addComponent(plotButton)
                .addContainerGap(69, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    private void lowerSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lowerSliderStateChanged
        // TODO add your handling code here:
        int upperval = upperSlider.getValue();
        jLabel1.setText("Lower limit: "+lowerSlider.getValue());
        if (!upperSlider.getValueIsAdjusting() ) {
            if((upperval<=lowerSlider.getValue())) {
                upperSlider.setValue(java.lang.Math.min(upperSlider.getMaximum(),lowerSlider.getValue()+1));
            }
        }
    }//GEN-LAST:event_lowerSliderStateChanged

    private void upperSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_upperSliderStateChanged
        // TODO add your handling code here:
        int lowerval = lowerSlider.getValue();
        jLabel2.setText("Upper limit: "+upperSlider.getValue());
        if (!lowerSlider.getValueIsAdjusting() ) {
            if((lowerval>=upperSlider.getValue())) {
                lowerSlider.setValue(java.lang.Math.max(lowerSlider.getMinimum(),upperSlider.getValue()-1));
            }
        }
    }//GEN-LAST:event_upperSliderStateChanged

    private void plotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotButtonActionPerformed
        // TODO add your handling code here:
        
        int currentPlane = images[0].getSlice();
        
        MimsRoiManager rm = ui.getRoiManager();
        rm.showFrame();
        ij.gui.Roi[] rois;
        javax.swing.JList rlist = rm.getList() ;
                
        int[] roiIndexes = rlist.getSelectedIndices();
        
        if(!rm.getROIs().isEmpty()) {
            rois = new ij.gui.Roi[roiIndexes.length];
            for(int i = 0 ; i < roiIndexes.length; i++ ) {
                //rois[i] = (ij.gui.Roi)rm.getROIs().get(rlist.getModel().getElementAt(i).toString());
                rois[i] = (ij.gui.Roi)rm.getROIs().get(rlist.getModel().getElementAt(roiIndexes[i]));
            }
        }            
        else {
            rois = new ij.gui.Roi[0];
        }
        
        
        if ( !(statJList.getSelectedIndex()==-1 || imageJList.getSelectedIndex()==-1 || rois.length==0) ) {
            Object[] objs = new Object[statJList.getSelectedValues().length];
            objs = statJList.getSelectedValues();
            String[] statnames = new String[objs.length];
            for(int i=0; i<objs.length; i++) {
                statnames[i]=(String)objs[i];
            }
            
            int[] masses = imageJList.getSelectedIndices();

            tomographyChart.creatNewFrame(rois, image.getName(), statnames, masses, lowerSlider.getValue(), upperSlider.getValue());
                        
        } else {
            ij.IJ.error("Tomography Error", "You must select at least one ROI, statistic, and mass.");
        }
        
        images[0].setSlice(currentPlane);
                
    }//GEN-LAST:event_plotButtonActionPerformed
    
    public void resetBounds() {            
        if (images[0] != null){// to prevent exceptions when no images open 
        lowerSlider.setMaximum(images[0].getImageStackSize());
        upperSlider.setMaximum(images[0].getImageStackSize());
        upperSlider.setValue(upperSlider.getMaximum());
        
        lowerSlider.setMajorTickSpacing((int)(lowerSlider.getMaximum()/8)+1);
        upperSlider.setMajorTickSpacing((int)(upperSlider.getMaximum()/8)+1);
        
        lowerSlider.setLabelTable( lowerSlider.createStandardLabels((int)(lowerSlider.getMaximum()/8)+1) );
        upperSlider.setLabelTable( upperSlider.createStandardLabels((int)(upperSlider.getMaximum()/8)+1) );
    
        }
    }
    public void resetImageNamesList() {
        
        rp = ui.getOpenRatioImages();
        System.out.println("rp length: "+rp.length); 
                        
        java.util.ArrayList<String> strings = new java.util.ArrayList<String>();
        String[] tempstrings = image.getMassNames();
        
        for(int j=0; j<numberMasses; j++) {
            strings.add(tempstrings[j]);
        }
        
        for(int i=0; i<rp.length; i++) {
            String foo;
            foo = rp[i].getTitle();
            foo = foo.substring(foo.indexOf("_m")+1);
            foo = foo.replaceAll("_", "/");
            foo = foo.replaceAll("m", "");
            strings.add(foo);
        }
        
        final String[] str = new String[strings.size()];
        for(int k=0; k<str.length; k++)
            str[k]=strings.get(k);
        
        imageJList.setModel(new javax.swing.AbstractListModel() {
            public int getSize() { return str.length; }
            public Object getElementAt(int i) { return str[i]; }
        });
        
        
    }
    
    private com.nrims.UI ui;
    private com.nrims.data.Opener image;
    private int numberMasses;
    private MimsPlus[] images;
    private ImageStack[] imagestacks;
    private ij.process.ImageStatistics imagestats;
    private MimsPlus[] rp;
    private MimsJFreeChart tomographyChart;
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList imageJList;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSlider lowerSlider;
    private javax.swing.JButton plotButton;
    private javax.swing.JList statJList;
    private javax.swing.JSlider upperSlider;
    // End of variables declaration//GEN-END:variables
    

}
