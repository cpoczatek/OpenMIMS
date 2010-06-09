package com.nrims;

import com.nrims.data.Opener;
import ij.*;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JLabel;

/**
 *
 * @author  cpoczatek
 */
public class MimsStackEditing extends javax.swing.JPanel {

    public static final long serialVersionUID = 1;
    public static final int OK = 2;
    public static final int CANCEL = 3;
    public static final int WORKING = 4;
    public static final int DONE = 5;
    public int STATE = -1;
    public int THREAD_STATE = -1;
    private UI ui = null;
    private Opener image = null;
    private int numberMasses = -1;
    private MimsPlus[] images = null;
    private ImageStack[] imagestacks = null;
    private boolean holdupdate = false;
    public AutoTrackManager atManager;
    public ArrayList<Integer> includeList = new ArrayList<Integer>();
    public int startSlice = -1;
    
    public MimsStackEditing(UI ui, Opener im) {

        initComponents();

        this.ui = ui;
        this.image = im;

        this.images = ui.getMassImages();
        numberMasses = image.getNMasses();
        imagestacks = new ImageStack[numberMasses];

        resetImageStacks();
    }

    public void resetImageStacks() {
        for (int i = 0; i < numberMasses; i++) {
            if(images[i]!=null)
                imagestacks[i] = this.images[i].getStack();
        }
    }

    public String removeSliceList(ArrayList<Integer> remList) {
        String liststr = "";
        int length = remList.size();
        int count = 0;
        for (int i = 0; i < length; i++) {
            removeSlice(remList.get(i) - count);
            count++;
            liststr += remList.get(i);
            if (i < (length - 1)) {
                liststr += ", ";
            }
        }

        return liststr;
    }

    public void removeSlice(int plane) {

        for (int k = 0; k <= (numberMasses - 1); k++) {

            imagestacks[k].deleteSlice(plane);

            this.images[k].setStack(null, imagestacks[k]);
        }
        this.ui.mimsAction.dropPlane(plane);
    }

    public void XShiftSlice(int plane, double xval) {
        for (int k = 0; k <= (numberMasses - 1); k++) {
            //make sure there is no roi
            this.images[k].killRoi();
            this.images[k].getProcessor().translate(xval, 0.0);
            images[k].updateAndDraw();
        }
        ui.mimsAction.setShiftX(plane, xval);
    }

    public void YShiftSlice(int plane, double yval) {
        for (int k = 0; k <= (numberMasses - 1); k++) {
            //make sure there is no roi
            this.images[k].killRoi();
            this.images[k].getProcessor().translate(0.0, yval);
            images[k].updateAndDraw();
        }
        ui.mimsAction.setShiftY(plane, yval);
    }

    public void restoreSlice(int plane) {
        int restoreIndex = ui.mimsAction.trueIndex(plane);
        try {
           
            int openerIndex = ui.mimsAction.getOpenerIndex(restoreIndex - 1);
            String openerName = ui.mimsAction.getOpenerName(restoreIndex - 1);
            Opener op = ui.getFromOpenerList(openerName);

            op.setStackIndex(openerIndex);
            for (int k = 0; k <= (numberMasses - 1); k++) {
                images[k].setSlice(plane);
                /*ImagePlus imp = new ImagePlus();
                ij.process.ShortProcessor p = new ij.process.ShortProcessor(256, 256);
                imp.setProcessor("foo", p);
                imp.getProcessor().setPixels(op.getPixels(k));
                imp.show();*/
                images[k].getProcessor().setPixels(op.getPixels(k));
                images[k].updateAndDraw();
            }
        } catch (Exception e) {
            System.err.println("Error re-reading plane " + restoreIndex);
            System.err.println(e.toString());
            e.printStackTrace();
        }
    }

    public void restoreMIMS() {
        untrack();
        MimsPlus[] massImages = ui.getMassImages();
        int currentSlice = massImages[0].getCurrentSlice();

        // concatenate the remaining files.
        int x = ui.mimsAction.getSize();
        for (int i = 1; i <= ui.mimsAction.getSize(); i++) {
           massImages[0].setSlice(i);
           if (ui.mimsAction.isDropped(i)) insertSlice(i);
        }

        resetTrueIndexLabel();
        resetSpinners();

        massImages[0].setSlice(currentSlice);
    }
    public void insertSlice(int plane) {

        System.out.println("inside insertSlice...");

        if (!ui.mimsAction.isDropped(plane)) {
            System.out.println("already there...");
            return;
        }

        // Get some properties about the image we are trying to restore.
        int restoreIndex = ui.mimsAction.displayIndex(plane);
        int displaysize = images[0].getNSlices();
        System.out.println("try to add at: " + restoreIndex);
        try {
            if (restoreIndex < displaysize) {
                int currentPlane = images[0].getCurrentSlice();
                
                int openerIndex = ui.mimsAction.getOpenerIndex(plane - 1);
                String openerName = ui.mimsAction.getOpenerName(plane - 1);
                Opener op = ui.getFromOpenerList(openerName);
                                
                op.setStackIndex(openerIndex);
                for (int i = 0; i < op.getNMasses(); i++) {
                    images[i].setSlice(restoreIndex);
                    imagestacks[i].addSlice("", images[i].getProcessor(), restoreIndex);
                    images[i].getProcessor().setPixels(op.getPixels(i));
                    images[i].updateAndDraw();
                }
                ui.mimsAction.undropPlane(plane);
                images[0].setSlice(currentPlane);
            }
            if (restoreIndex >= displaysize) {

                this.holdupdate = true;

                int currentPlane = images[0].getCurrentSlice();
                
                int openerIndex = ui.mimsAction.getOpenerIndex(plane  - 1);
                String openerName = ui.mimsAction.getOpenerName(plane - 1);
                Opener op = ui.getFromOpenerList(openerName);
                
                op.setStackIndex(openerIndex);
                for (int i = 0; i < op.getNMasses(); i++) {
                    images[i].setSlice(displaysize);
                    imagestacks[i].addSlice("", images[i].getProcessor());
                    images[i].setStack(null, imagestacks[i]);
                    images[i].setSlice(restoreIndex);
                    images[i].getProcessor().setPixels(op.getPixels(i));
                    images[i].updateAndDraw();
                }

                ui.mimsAction.undropPlane(plane);
                images[0].setSlice(currentPlane);
            }

        } catch (Exception e) {
            System.err.println("Error re-reading plane " + restoreIndex + "from file.");
            System.err.println(e.toString());
            e.printStackTrace();
        }
        this.holdupdate = false;
        this.resetTrueIndexLabel();
    }

    // Set the user selected options on to the tempImage.
    public ImagePlus setOptions(ImagePlus tempImage, String options) {

         // Crop image to size of roi.
         if (options.contains("roi")) {
            tempImage = cropImage(tempImage);
         } else {
            ImageStack tempStack = tempImage.getImageStack();
            ImageProcessor tempProcessor = tempImage.getProcessor();
            StackProcessor stackproc = new StackProcessor(tempStack, tempProcessor);
            tempImage.setStack("cropped", stackproc.crop(0, 0, tempImage.getProcessor().getWidth(), tempImage.getProcessor().getHeight()));
         }

          // Enhance tracking image contrast.
          if(options.contains("normalize") || options.contains("equalize")) {
             WindowManager.setTempCurrentImage(tempImage);
             String tmpstring = "";
             if(options.contains("normalize")) { tmpstring += "normalize "; }
             if(options.contains("equalize")) { tmpstring += "equalize "; }
             IJ.run("Enhance Contrast", "saturated=0.5 " + tmpstring + " normalize_all");
          }

          return tempImage;
      }

    // Get the user selected options.
     public String getOptions() {

         String options = " ";
             if (atManager.sub.isSelected()) {
                 options += "roi ";
             }

              //check for roi
              if (options.contains("roi")) {
                  MimsRoiManager roimanager = ui.getRoiManager();
                  if (roimanager == null) {
                      ij.IJ.error("Error", "No ROI selected.");
                      return null;
                  }
                  ij.gui.Roi roi = roimanager.getRoi();
                  if (roi == null) {
                      ij.IJ.error("Error", "No ROI selected.");
                      return null;
                  }
              }

             if (atManager.norm.isSelected()) {
                 options += "normailize ";
             }
             if (atManager.eq.isSelected()) {
                 options += "equalize ";
             }

         return options;
      }

    // Apply translations onto the images.
    public void applyTranslations(double[][] translations, ArrayList<Integer> includeList) {

          double deltax, deltay;
          int plane;

           for (int i = 0; i < translations.length; i++) {
              
               if (STATE == CANCEL)
                  return;

               plane = includeList.get(i);
               //possible loss of precision...
               //notice the negative....
               deltax = (-1.0) * translations[i][0];
               deltay = (-1.0) * translations[i][1];
               images[0].setSlice(plane);
               double actx = ui.mimsAction.getXShift(plane);
               double acty = ui.mimsAction.getYShift(plane);

               boolean redraw = ((deltax * actx < 0) || (deltay * acty < 0));

               if (!holdupdate && (!ui.isUpdating())) {
                   if (redraw) {
                       restoreSlice(plane);
                       XShiftSlice(plane, actx + deltax);
                       YShiftSlice(plane, acty + deltay);
                   } else {
                       XShiftSlice(plane, deltax);
                       YShiftSlice(plane, deltay);
                   }
                   ui.mimsAction.setShiftX(plane, actx + deltax);
                   ui.mimsAction.setShiftY(plane, acty + deltay);
               }
           }
      }

    public void concatImages(boolean pre, boolean labeloriginal, UI tempui) {

        ui.setUpdating(true);
        
        Opener tempImage = tempui.getOpener();
        MimsPlus[] tempimage = tempui.getMassImages();
        ImageStack[] tempstacks = new ImageStack[numberMasses];

        for (int i = 0; i < image.getNMasses(); i++) {
            if (images[i] != null) {
                images[i].setIsStack(true);
            }
        }

        //label all slices of original image
        //if (labeloriginal) {
        //    for (int mass = 0; mass <= (numberMasses - 1); mass++) {
        //        for (int i = 1; i <= imagestacks[mass].getSize(); i++) {
        //            imagestacks[mass].setSliceLabel(images[mass].getTitle(), i);
        //        }
        //    }
        //}
        
        //increase action size
        ui.mimsAction.addPlanes(pre, tempimage[0].getNSlices(), tempImage);
        
        for (int i = 0; i <= (numberMasses - 1); i++) {
            tempstacks[i] = tempimage[i].getStack();
        }
        //append slices, include labels
        if (pre) {
            for (int mass = 0; mass <= (numberMasses - 1); mass++) {
                for (int i = tempstacks[mass].getSize(); i >= 1; i--) {
                    imagestacks[mass].addSlice(tempimage[mass].getTitle(), tempstacks[mass].getProcessor(i), 0);
                }
            }
            ui.getmimsLog().Log("Concat: " + tempui.getImageFilePrefix() + " + " + ui.getImageFilePrefix());
        } else {
            for (int mass = 0; mass <= (numberMasses - 1); mass++) {
                for (int i = 1; i <= tempstacks[mass].getSize(); i++) {
                    imagestacks[mass].addSlice(tempimage[mass].getTitle(), tempstacks[mass].getProcessor(i));
                }
            }
            ui.getmimsLog().Log("Concat: " + ui.getImageFilePrefix() + " + " + tempui.getImageFilePrefix());
        }


        for (int i = 0; i <= (numberMasses - 1); i++) {
            this.images[i].setStack(null, imagestacks[i]);
            this.images[i].updateImage();
        }

        

        ui.getmimsLog().Log("New size: " + images[0].getNSlices() + " planes");

        resetImageStacks();
        for (int i = 0; i < tempimage.length; i++) {
            if (tempimage[i] != null) {
                tempimage[i].setAllowClose(true);
                tempimage[i].close();
                tempimage[i] = null;
            }
        }
        for (int i = 0; i < tempstacks.length; i++) {
            tempstacks[i] = null;
        }

        ui.addToOpenerList(tempui.getOpener().getImageFile().getName(), tempui.getOpener());
        ui.setUpdating(false);
    }

    public boolean sameResolution(Opener im, Opener ij) {
        return ((im.getWidth() == ij.getWidth()) && (im.getHeight() == ij.getHeight()));
    }

    public boolean sameSpotSize(Opener im, Opener ij) {
        return ((im.getPixelWidth() == ij.getPixelWidth()) && (im.getPixelHeight() == ij.getPixelHeight()));
    }
    
    public static ArrayList<Integer> parseList(String liststr, int lb, int ub) {
        ArrayList<Integer> deletelist = new ArrayList<Integer>();
        ArrayList<Integer> checklist = new ArrayList<Integer>();
        boolean badlist = false;

        check:
        try {
            if (liststr.equals("")) {
                badlist = true;
                break check;
            }
            liststr = liststr.replaceAll("[^0-9,-]", "");
            String[] splitstr = liststr.split(",");
            int l = splitstr.length;

            for (int i = 0; i < l; i++) {

                if (!splitstr[i].contains("-")) {
                    deletelist.add(Integer.parseInt(splitstr[i]));
                } else {
                    String[] resplitstr = splitstr[i].split("-");

                    if (resplitstr.length > 2) {
                        ij.IJ.error("List Error", "Malformed range in list.");
                        break check;
                    }

                    int low = Integer.parseInt(resplitstr[0]);
                    int high = Integer.parseInt(resplitstr[1]);

                    if (low >= high) {
                        ij.IJ.error("List Error", "Malformed range bounds in list.");
                        break check;
                    }

                    for (int j = low; j <= high; j++) {
                        deletelist.add(j);
                    }
                }
            }
            java.util.Collections.sort(deletelist);

            int length = deletelist.size();

            for (int i = 0; i < length; i++) {
                if (deletelist.get(i) > ub || deletelist.get(i) < lb) {
                    badlist = true;
                    ij.IJ.error("List Error", "Out of range element in list.");
                    break check;
                }
            }

            for (int i = 0; i < length; i++) {
                int plane = deletelist.get(i);
                if (!checklist.contains(plane)) {
                    checklist.add(plane);
                }
            }


        } catch (Exception e) {
            ij.IJ.error("List Error", "Exception, malformed list.");
            badlist = true;
        }


        if (badlist) {
            ArrayList<Integer> bad = new ArrayList<Integer>(0);
            return bad;
        } else {
            return checklist;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
   private void initComponents() {

      buttonGroup1 = new javax.swing.ButtonGroup();
      jSeparator1 = new javax.swing.JSeparator();
      concatButton = new javax.swing.JButton();
      jLabel5 = new javax.swing.JLabel();
      deleteListTextField = new javax.swing.JTextField();
      deleteListButton = new javax.swing.JButton();
      trueIndexLabel = new javax.swing.JLabel();
      reinsertButton = new javax.swing.JButton();
      jLabel1 = new javax.swing.JLabel();
      reinsertListTextField = new javax.swing.JTextField();
      displayActionButton = new javax.swing.JButton();
      translateXSpinner = new javax.swing.JSpinner();
      jLabel2 = new javax.swing.JLabel();
      jLabel3 = new javax.swing.JLabel();
      translateYSpinner = new javax.swing.JSpinner();
      autoTrackButton = new javax.swing.JButton();
      untrackButton = new javax.swing.JButton();
      jLabel4 = new javax.swing.JLabel();
      sumButton = new javax.swing.JButton();
      compressButton = new javax.swing.JButton();
      compressTextField = new javax.swing.JTextField();
      sumTextField = new javax.swing.JTextField();
      jButton1 = new javax.swing.JButton();

      concatButton.setText("Concatenate");
      concatButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            concatButtonActionPerformed(evt);
         }
      });

      jLabel5.setText("Delete List (eg: 2,4,8-25,45...)");

      deleteListButton.setText("Delete");
      deleteListButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            deleteListButtonActionPerformed(evt);
         }
      });

      trueIndexLabel.setText("True Index: 999   Display Index: 999");

      reinsertButton.setText("Reinsert");
      reinsertButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            reinsertButtonActionPerformed(evt);
         }
      });

      jLabel1.setText("Reinsert List (True plane numbers)");

      displayActionButton.setText("Display Action");
      displayActionButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            displayActionButtonActionPerformed(evt);
         }
      });

      translateXSpinner.setModel(new javax.swing.SpinnerNumberModel(0.0d, -999.0d, 999.0d, 0.01d));
      translateXSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
         public void stateChanged(javax.swing.event.ChangeEvent evt) {
            translateXSpinnerStateChanged(evt);
         }
      });

      jLabel2.setText("Translate X");

      jLabel3.setText("Translate Y");

      translateYSpinner.setModel(new javax.swing.SpinnerNumberModel(0.0d, -999.0d, 999.0d, 0.01d));
      translateYSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
         public void stateChanged(javax.swing.event.ChangeEvent evt) {
            translateYSpinnerStateChanged(evt);
         }
      });

      autoTrackButton.setText("Autotrack");
      autoTrackButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            autoTrackButtonActionPerformed(evt);
         }
      });

      untrackButton.setText("Untrack");
      untrackButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            untrackButtonActionPerformed(evt);
         }
      });

      sumButton.setText("Sum");
      sumButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            sumButtonActionPerformed(evt);
         }
      });

      compressButton.setText("Compress");
      compressButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            compressButtonActionPerformed(evt);
         }
      });

      jButton1.setText("Uncompress");
      jButton1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButton1ActionPerformed(evt);
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
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                     .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                           .addGroup(layout.createSequentialGroup()
                              .addComponent(reinsertListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                           .addGroup(layout.createSequentialGroup()
                              .addComponent(reinsertButton)
                              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                           .addGroup(layout.createSequentialGroup()
                              .addComponent(jLabel1)
                              .addGap(87, 87, 87))
                           .addGroup(layout.createSequentialGroup()
                              .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                 .addComponent(deleteListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                                 .addComponent(jLabel5)
                                 .addComponent(trueIndexLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                                 .addComponent(deleteListButton, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                           .addGroup(layout.createSequentialGroup()
                              .addGap(12, 12, 12)
                              .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                 .addComponent(translateXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                 .addComponent(jLabel2))
                              .addGap(8, 8, 8))
                           .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                              .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                 .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                                 .addComponent(compressButton, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)))))
                     .addComponent(sumButton, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(23, 23, 23))
                     .addComponent(translateYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                     .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(sumTextField, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(compressTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE))))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(displayActionButton)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(concatButton)
                  .addGap(77, 77, 77)
                  .addComponent(autoTrackButton)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(untrackButton)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGap(53, 53, 53)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(jLabel3)
                     .addComponent(jLabel2))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(translateXSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(translateYSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)))
               .addGroup(layout.createSequentialGroup()
                  .addGap(25, 25, 25)
                  .addComponent(trueIndexLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(jLabel5)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(deleteListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(deleteListButton))
               .addGroup(layout.createSequentialGroup()
                  .addGap(50, 50, 50)
                  .addComponent(jLabel4)))
            .addGap(50, 50, 50)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(compressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(compressButton))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(jButton1))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(jLabel1)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(reinsertListTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(reinsertButton)))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(sumTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(sumButton))
            .addGap(46, 46, 46)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(displayActionButton)
               .addComponent(concatButton)
               .addComponent(autoTrackButton)
               .addComponent(untrackButton))
            .addContainerGap())
      );
   }// </editor-fold>                        

    private void deleteListButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                 

        String liststr = deleteListTextField.getText();
        ArrayList<Integer> checklist = parseList(liststr, 1, images[0].getStackSize());

        if (checklist.size() != 0) {
            liststr = removeSliceList(checklist);
            ui.getmimsLog().Log("Deleted list: " + liststr);
            ui.getmimsLog().Log("New size: " + images[0].getNSlices() + " planes");
        }

        this.resetTrueIndexLabel();
        this.resetSpinners();
        deleteListTextField.setText("");
}                                                
      
    private void concatButtonActionPerformed(java.awt.event.ActionEvent evt) {                                             
        UI tempUi = new UI(ui.getImageDir());
        tempUi.loadMIMSFile();
        Opener tempImage = tempUi.getOpener();
        if (tempImage == null) {
            return; // if the FileChooser dialog was canceled
        }
        if (ui.getOpener().getNMasses() == tempImage.getNMasses()) {
            if (sameResolution(image, tempImage)) {
                if (sameSpotSize(image, tempImage)) {
                    Object[] options = {"Append images", "Prepend images", "Cancel"};
                    int value = JOptionPane.showOptionDialog(this, tempUi.getImageFilePrefix(), "Concatenate",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
                    if (value != JOptionPane.CANCEL_OPTION) {
                        // store action to reapply it after restoring
                        // (shallow copy in mimsAction is enough as 'restoreMims' creates a new 'actionList' object
                        concatImages(value != JOptionPane.YES_OPTION, true, tempUi);
                        ui.getRoiManager().updateRoiLocations(value != JOptionPane.YES_OPTION);
                    }
                } else {
                    IJ.error("Images do not have the same spot size.");
                }
            } else {
                IJ.error("Images are not the same resolution.");
            }
        } else {
            IJ.error("Two images with the same\nnumber of masses must be open.");
        }

        // close the temporary windows
        for (MimsPlus image : tempUi.getMassImages()) {
            if (image != null) {
                image.setAllowClose(true);
                image.close();

            }
        }
        tempUi = null;
        
        ui.getMimsData().setHasStack(true);
        ui.setSyncStack(true);

        ij.plugin.WindowOrganizer wo = new ij.plugin.WindowOrganizer();
        //wo.run("tile");
        ui.updateStatus("");
        ij.WindowManager.repaintImageWindows();
}                                            

    private void reinsertButtonActionPerformed(java.awt.event.ActionEvent evt) {                                               
        // TODO add your handling code here:
        int current = images[0].getCurrentSlice();
        String liststr = reinsertListTextField.getText();
        int trueSize = ui.mimsAction.getSize();
        ArrayList<Integer> checklist = parseList(liststr, 1, trueSize);
        int length = checklist.size();

        System.out.println("insert button...");
        System.out.println("length = " + length);

        for (int i = 0; i < length; i++) {
            System.out.println("i = " + i);
            this.insertSlice(checklist.get(i));
        }

        images[0].setSlice(current);
        this.resetTrueIndexLabel();
        this.resetSpinners();
        reinsertListTextField.setText("");
}                                              

    private void displayActionButtonActionPerformed(java.awt.event.ActionEvent evt) {                                                    
        // TODO add your handling code here:
        ij.text.TextWindow actionWindow = new ij.text.TextWindow("Current Action State", "plane\tx\ty\tdrop\timage index\timage", "", 300, 400);

        int n = ui.mimsAction.getSize();
        String tempstr = "";
        for (int i = 1; i <= n; i++) {
            tempstr = ui.mimsAction.getActionRow(i);
            actionWindow.append(tempstr);
        }
    }                                                   

    private void translateXSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {                                               
        // TODO add your handling code here:
        int plane = images[0].getCurrentSlice();

        double xval = (Double) translateXSpinner.getValue();
        double yval = (Double) translateYSpinner.getValue();

        double actx = ui.mimsAction.getXShift(plane);
        double acty = ui.mimsAction.getYShift(plane);

        double deltax = xval - actx;
        double deltay = yval - acty;

        boolean redraw = ((deltax * actx < 0) || (deltay * acty < 0));

        if (!holdupdate && ((actx != xval) || (acty != yval)) && (!ui.isUpdating())) {
            if (redraw) {
                this.restoreSlice(plane);
                this.XShiftSlice(plane, xval);
                this.YShiftSlice(plane, yval);
            } else {
                this.XShiftSlice(plane, deltax);
                this.YShiftSlice(plane, deltay);
            }
            ui.mimsAction.setShiftX(plane, xval);
            ui.mimsAction.setShiftY(plane, yval);
        }
    }                                              

    private void translateYSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {                                               
        // TODO add your handling code here:
        int plane = images[0].getCurrentSlice();

        double xval = (Double) translateXSpinner.getValue();
        double yval = (Double) translateYSpinner.getValue();

        double actx = ui.mimsAction.getXShift(plane);
        double acty = ui.mimsAction.getYShift(plane);

        double deltax = xval - actx;
        double deltay = yval - acty;

        boolean redraw = ((deltax * actx < 0) || (deltay * acty < 0));

        if (!holdupdate && ((actx != xval) || (acty != yval)) && (!ui.isUpdating())) {
            if (redraw) {
                this.restoreSlice(plane);
                this.XShiftSlice(plane, xval);
                this.YShiftSlice(plane, yval);
            } else {
                this.XShiftSlice(plane, deltax);
                this.YShiftSlice(plane, deltay);
            }
            ui.mimsAction.setShiftX(plane, xval);
            ui.mimsAction.setShiftY(plane, yval);
        }
    }                                              



    public void autoTrackButtonActionPerformed(java.awt.event.ActionEvent evt) {

        atManager = new AutoTrackManager();
        atManager.showFrame();              
}                                               
   
    private void untrackButtonActionPerformed(java.awt.event.ActionEvent evt) {                                              
        untrack();
}                                             

public void untrack() {

    int startPlane = ui.getOpenMassImages()[0].getCurrentSlice();

        double xval = 0.0;
        double yval = 0.0;

        double actx = 0.0;
        double acty = 0.0;

        double deltax = 0.0;
        double deltay = 0.0;

        boolean redraw;

        for (int plane = 1; plane <= images[0].getStackSize(); plane++) {
            xval = 0;
            yval = 0;

            actx = ui.mimsAction.getXShift(plane);
            acty = ui.mimsAction.getYShift(plane);

            deltax = xval - actx;
            deltay = yval - acty;

            redraw = ((deltax * actx < 0) || (deltay * acty < 0));

            if (!holdupdate && ((actx != xval) || (acty != yval)) && (!ui.isUpdating())) {
                if (redraw) {
                    this.restoreSlice(plane);
                    this.XShiftSlice(plane, xval);
                    this.YShiftSlice(plane, yval);
                } else {
                    this.XShiftSlice(plane, deltax);
                    this.YShiftSlice(plane, deltay);
                }
                ui.mimsAction.setShiftX(plane, xval);
                ui.mimsAction.setShiftY(plane, yval);
            }
        }
        autoTrackButton.setEnabled(true);
        images[0].setSlice(startPlane);
        resetSpinners();
        ui.getmimsLog().Log("Untracked.");
}

private void sumButtonActionPerformed(java.awt.event.ActionEvent evt) {                                          

    // Get the window title.
    String name = WindowManager.getCurrentImage().getTitle();
    String sumTextFieldString = sumTextField.getText().trim();

    // initialize varaibles.
    SumProps sumProps = null;
    MimsPlus mp = ui.getImageByName(name);
    if (mp == null) return;
    MimsPlus sp;

    // Generate a SumProps object
    if (mp.getMimsType() == MimsPlus.MASS_IMAGE) {
       int parentIdx = mp.getMassIndex();
       if (parentIdx > -1) sumProps = new SumProps(parentIdx);
    }
    if (mp.getMimsType() == MimsPlus.RATIO_IMAGE) {
       RatioProps rp = mp.getRatioProps();
       int numIdx = rp.getNumMassIdx();
       int denIdx = rp.getDenMassIdx();
       if (numIdx > -1 && denIdx > -1) sumProps = new SumProps(numIdx, denIdx);
       sumProps.setRatioScaleFactor(mp.getRatioProps().getRatioScaleFactor());
    }

    // Get list from field box.
    if (sumTextFieldString.isEmpty()) {
       sp = new MimsPlus(ui, sumProps, null);
    } else {
       ArrayList<Integer> sumlist = parseList(sumTextFieldString, 1, ui.mimsAction.getSize());
       if (sumlist.size()==0) return;
       sp = new MimsPlus(ui, sumProps, sumlist);
    }

    // Show sum image.
    sp.showWindow();
}                                         

private void compressButtonActionPerformed(java.awt.event.ActionEvent evt) {                                               

   // Get the block size from the text box.
   String comptext = compressTextField.getText();
   int blockSize = 1;
   try {
       blockSize = Integer.parseInt(comptext);
   } catch(Exception e) {
       ij.IJ.error("Invalid compress value.");
       compressTextField.setText("");
       return;
   }

   // Do the compression.
   boolean done = compressPlanes(blockSize);

   // Do some autocontrasting stuff.
    if (done) {
        int nmasses = image.getNMasses();
        for (int mindex = 0; mindex < nmasses; mindex++) {
            images[mindex].setSlice(1);
            images[mindex].updateAndDraw();
            ui.autoContrastImage(images[mindex]);
        }
    }

    compressTextField.setText("");
    ui.getmimsLog().Log("Compressed with blocksize: " + blockSize);
}                                              

private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        uncompressPlanes();
}                                        

public void uncompressPlanes() {
   
        // This uncompresses the image.
        removeSlice(1);
        insertSlice(1);

        // This resizes the scrollbar correctly.
        images[0].setSlice(2);
        images[0].setSlice(1);

        this.resetTrueIndexLabel();
        this.resetSpinners();   
}

public boolean compressPlanes(int blockSize) {

        // initializing stuff.
        int nmasses = image.getNMasses();
        int currentplane = images[0].getSlice();
        int templength = images[0].getProcessor().getPixelCount();
        float[][] sumPixels = new float[nmasses][templength];

        // Set up the stacks.
        int size = images[0].getNSlices();
        ImageStack[] is = new ImageStack[nmasses];
        for (int mindex = 0; mindex < nmasses; mindex++) {
            ImageStack iss = new ImageStack(images[0].getWidth(), images[0].getHeight());
            is[mindex] = iss;
        }

        // Begin looping over block size.
        for (int i = 1; i <= size; i = i + blockSize) {

            // Create a sum list of individual images to be in the block.
            ArrayList sumlist = new ArrayList<Integer>();
            for (int j = i; j <= i + blockSize - 1; j++) {
                if (j > 0 && j <= images[0].getNSlices()) {
                    sumlist.add(j);
                }
            }

            // Generate the sum image for the block.
            MimsPlus cp;
            for (int mindex = 0; mindex < nmasses; mindex++) {

                SumProps sumProps = new SumProps(images[mindex].getMassIndex());
                cp = new MimsPlus(ui, sumProps, sumlist);

                // Check for bad data.
                double m = cp.getProcessor().getMax();
                if (m > 65535) {
                    IJ.error("Cannot compress. Over 16 bit limit.");
                    System.out.println("ERROR! over limit of 16 bit processing!");
                    return false;
                }

                // Build up the stacks
                sumPixels[mindex] = (float[]) cp.getProcessor().getPixels();
                short[] t = new short[cp.getProcessor().getPixelCount()];
                for (int k = 0; k < sumPixels[mindex].length; k++) {
                    t[k] = (short) sumPixels[mindex][k];
                }
                String label = sumlist.get(0) + " - " + sumlist.get(sumlist.size() - 1);
                is[mindex].addSlice(label, t);

            }
        }

        //a little cleanup
        //multiple calls to setSlice are to get image scroll bars triggered right
        for (int mindex = 0; mindex < image.getNMasses(); mindex++) {
            images[mindex].setStack(null, is[mindex]);
            if (images[mindex].getNSlices() > 1) {
                images[mindex].setIsStack(true);
                images[mindex].setSlice(1);
                images[mindex].setSlice(images[mindex].getNSlices());
                images[mindex].setSlice(currentplane);

            } else {
                images[mindex].setIsStack(false);
            }
            images[mindex].updateAndDraw();
        }
        return true;
    }

    protected void resetSpinners() {
        if (this.images != null && (!holdupdate) && (images[0] != null) && (!ui.isUpdating())) {
            if (THREAD_STATE == this.WORKING)
               return;
            holdupdate = true;
            int plane = images[0].getCurrentSlice();
            double xval = ui.mimsAction.getXShift(plane);
            double yval = ui.mimsAction.getYShift(plane);
            this.translateXSpinner.setValue(xval);
            this.translateYSpinner.setValue(yval);
            holdupdate = false;
        }
    }

    protected void resetTrueIndexLabel() {

        if (this.images != null && (!holdupdate) && (images[0] != null)) {
            String label = "True index: ";
            int p = ui.mimsAction.trueIndex(this.images[0].getCurrentSlice());

            label = label + java.lang.Integer.toString(p);
            p = this.images[0].getCurrentSlice();
            label = label + "   Display index: " + java.lang.Integer.toString(p);

            this.trueIndexLabel.setText(label);
        }
    }
   // Variables declaration - do not modify                     
   private javax.swing.JButton autoTrackButton;
   private javax.swing.ButtonGroup buttonGroup1;
   private javax.swing.JButton compressButton;
   private javax.swing.JTextField compressTextField;
   private javax.swing.JButton concatButton;
   private javax.swing.JButton deleteListButton;
   private javax.swing.JTextField deleteListTextField;
   private javax.swing.JButton displayActionButton;
   private javax.swing.JButton jButton1;
   private javax.swing.JLabel jLabel1;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JLabel jLabel3;
   private javax.swing.JLabel jLabel4;
   private javax.swing.JLabel jLabel5;
   private javax.swing.JSeparator jSeparator1;
   private javax.swing.JButton reinsertButton;
   private javax.swing.JTextField reinsertListTextField;
   private javax.swing.JButton sumButton;
   private javax.swing.JTextField sumTextField;
   private javax.swing.JSpinner translateXSpinner;
   private javax.swing.JSpinner translateYSpinner;
   private javax.swing.JLabel trueIndexLabel;
   private javax.swing.JButton untrackButton;
   // End of variables declaration                   
   
   // Crops the image to the Roi selected in RoiManager.
   // Otherwise calls crop (without actually cropping).
      public ImagePlus cropImage(ImagePlus img) {

         ImageStack tempStack = img.getImageStack();
         ImageProcessor tempProcessor = img.getProcessor();
         ij.process.StackProcessor stackproc = new ij.process.StackProcessor(tempStack, tempProcessor);

               MimsRoiManager roimanager = ui.getRoiManager();
               if (roimanager == null) { return null; }
               ij.gui.Roi roi = roimanager.getRoi();
               if (roi == null) { return null; }
               int width = roi.getBoundingRect().width;
               int height = roi.getBoundingRect().height;
               int x = roi.getBoundingRect().x;
               int y = roi.getBoundingRect().y;
               img.getProcessor().setRoi(roi);
               img.setStack("cropped", stackproc.crop(x, y, width, height));
               img.killRoi();

               return img;
      }      

      // Return a new image minus those planes specified in includeList.
      public ImagePlus getSubStack(ImagePlus img, ArrayList<Integer> includeList) {
         ImageStack imgStack = img.getImageStack();
         ImageStack tempStack = new ImageStack(img.getHeight(), img.getWidth());
          for (int i = 0; i < imgStack.getSize(); i++) {
             if (includeList.contains(i+1)) {
                tempStack.addSlice(Integer.toString(i+1), imgStack.getProcessor(i+1));
             }
          }
         ImagePlus tempImg = new ImagePlus("img", tempStack);
         return tempImg;
      }

    public void notifyComplete(double[][] trans) {

       boolean nullTrans = false;
       atManager.cancelButton.setEnabled(false);
       if (trans == null) {
          nullTrans = true;
       } else {
          applyTranslations(trans, includeList);
       }
       atManager.cancelButton.setEnabled(true);

       THREAD_STATE = DONE;
       ui.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
       atManager.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
       atManager.closeWindow();
       if (startSlice > -1)
             images[0].setSlice(startSlice);

       if (nullTrans == true && STATE == MimsStackEditing.OK)
          throw new NullPointerException("translations is null: AutoTrack has failed.");
    }
   
public class AutoTrackManager extends com.nrims.PlugInJFrame implements ActionListener{
   
   Frame instance;
   ButtonGroup buttonGroup = new ButtonGroup();
   JTextField txtField = new JTextField();
   JRadioButton all;
   JRadioButton some;
   JRadioButton sub;
   JRadioButton norm;
   JRadioButton eq;
   JButton cancelButton;
   JButton okButton;            
   
   public AutoTrackManager(){
      super("Auto Track Manager");
      
      if (instance != null) {
         instance.toFront();
         return;
      }
      instance = this;
      
      // Setup radiobutton panel.
      JPanel jPanel = new JPanel();
      jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

      String imagename = WindowManager.getCurrentImage().getTitle();
      JLabel label = new JLabel("Image:   "+imagename);
      jPanel.add(label);
      jPanel.add(Box.createRigidArea(new Dimension(0,10)));

      // Radio buttons.
      all  = new JRadioButton("Autotrack all images.");
      all.setActionCommand("All");
      all.addActionListener(this);            
      all.setSelected(true);
      
      some = new JRadioButton("Autotrack subset of images. (eg: 2,4,8-25,45...)");            
      some.setActionCommand("Subset");                                                   
      some.addActionListener(this);
      txtField.setEditable(false);

      sub = new JRadioButton("Use subregion (Roi)");
      sub.setSelected(false);

      norm = new JRadioButton("Normalize tracking image");
      norm.setSelected(true);
      eq = new JRadioButton("Equalize tracking image");
      eq.setSelected(true);

      buttonGroup.add(all);
      buttonGroup.add(some);      
     

      // Add to container.
      jPanel.add(all);
      jPanel.add(Box.createRigidArea(new Dimension(0,10)));
      jPanel.add(some);   
      jPanel.add(Box.createRigidArea(new Dimension(0,10)));
      jPanel.add(txtField);
      jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
      jPanel.add(Box.createRigidArea(new Dimension(0,10)));
      jPanel.add(sub);
      jPanel.add(Box.createRigidArea(new Dimension(0,10)));
      jPanel.add(norm);
      jPanel.add(Box.createRigidArea(new Dimension(0,10)));
      jPanel.add(eq);
      jPanel.add(Box.createRigidArea(new Dimension(0,10)));
      
      // Set up "OK" and "Cancel" buttons.      
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));            
      cancelButton = new JButton("Cancel");   
      cancelButton.setActionCommand("Cancel");
      cancelButton.addActionListener(this);
      okButton = new JButton("OK");           
      okButton.setActionCommand("OK");
      okButton.addActionListener(this);
      buttonPanel.add(cancelButton);
      buttonPanel.add(okButton);
      
      // Add elements.
      setLayout(new BorderLayout());            
      add(jPanel, BorderLayout.PAGE_START);
      add(buttonPanel, BorderLayout.PAGE_END);            
      setSize(new Dimension(375, 300));
  
   }
   
   // Gray out textfield when "All" images radio button selected.
    public void actionPerformed(ActionEvent e) {
       if (e.getActionCommand().equals("Subset"))
          txtField.setEditable(true);       
       else if (e.getActionCommand().equals("All"))   
          txtField.setEditable(false); 
       else if (e.getActionCommand().equals("Cancel")) {
          STATE = CANCEL;
          THREAD_STATE = DONE;
          closeWindow();
          if (startSlice > -1) 
             images[0].setSlice(startSlice);
       } else if (e.getActionCommand().equals("OK")) {
            STATE = OK;

            ui.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            atManager.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // Get optional parameters for auto tracking algorithm.
            String options = getOptions();
            if (options == null){
               STATE = CANCEL;
               notifyComplete(null);
               return;
            }

            // Get the list of included images and create substack.
            ImagePlus currentImage = WindowManager.getCurrentImage();
            startSlice = currentImage.getCurrentSlice();
            if (atManager.getSelection(atManager.buttonGroup).getActionCommand().equals("Subset")) {
               includeList = parseList(atManager.txtField.getText(), 1, ui.mimsAction.getSize());
            } else {
               for (int i = 0; i < images[0].getNSlices(); i++)
                  includeList.add(i, i+1);
            }
            ImagePlus tempImage = getSubStack(currentImage, includeList);

            // Set options.
            tempImage = setOptions(tempImage, options);

            // Call Autotrack algorithm.
            THREAD_STATE = WORKING;
            AutoTrack temptrack = new AutoTrack(ui, tempImage);
            Thread thread = new Thread(temptrack);
            thread.start();

            tempImage.close();
       }
    }

    // Show the frame.
    public void showFrame() {
        setLocation(400, 400);
        setVisible(true);
        toFront();
        setExtendedState(NORMAL);
    }
    
    public void closeWindow() {
      super.close();
      instance = null;
      this.setVisible(false);       
   }
    
    // Returns a reference to the MimsRatioManager
    // or null if it is not open.
    public AutoTrackManager getInstance() {
        return (AutoTrackManager)instance;
    }
    
    // Returns all numbers between min and max NOT in listA.
    public ArrayList<Integer> getInverseList(ArrayList<Integer> listA, int min, int max){
       ArrayList<Integer> listB = new ArrayList<Integer>();
       
       for (int i=min; i <= max; i++) {
          if (!listA.contains(i))
             listB.add(i);          
       }
       
       return listB;
    }
        
    // This method returns the selected radio button in a button group
    public JRadioButton getSelection(ButtonGroup group) {
        for (Enumeration e=group.getElements(); e.hasMoreElements(); ) {
            JRadioButton b = (JRadioButton)e.nextElement();
            if (b.getModel() == group.getSelection()) {
                return b;
            }
        }
        return null;
    }

}

}


