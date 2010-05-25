package com.nrims;

import java.awt.Graphics;
import java.awt.geom.PathIterator;
import java.awt.Polygon;
import java.util.Hashtable;
import ij.gui.*;
import ij.io.RoiEncoder;
import ij.io.RoiDecoder;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Extends ij.gui.ImageCanvas with utility to display all ROIs.
 *
 * @author Douglas Benson
 * @author <a href="mailto:rob.gonzalez@gmail.com">Rob Gonzalez</a>
 */
public class MimsCanvas extends ij.gui.ImageCanvas {

    public static final long serialVersionUID = 1;

    public MimsCanvas(MimsPlus imp, UI ui) {
        super(imp);
        this.mImp = imp;
        this.ui = ui;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // Check if the MimsRoiManager is open..
        if (MimsRoiManager.getInstance() != null) {
            drawOverlay(g);
        }
    }

    void drawOverlay(Graphics g) {
        MimsRoiManager roiManager = ui.getRoiManager();
        Hashtable rois = roiManager.getROIs();
        if (rois == null || rois.isEmpty() || roiManager.getHideRois()) {
            return;
        }

        //make color prefernce selectable
        if (mImp.getMimsType() == MimsPlus.HSI_IMAGE) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(Color.RED);
        }
        Roi cRoi = mImp.getRoi();
        javax.swing.JList list = roiManager.getList();

        //to count the rois on a plane if not synced
        int nSyncid = 0;
        //test is ratio/hsi to get correct plane number
        //note getType is from ImagePlus
        boolean isRatio = (mImp.getMimsType() == MimsPlus.RATIO_IMAGE) || (mImp.getMimsType() == MimsPlus.HSI_IMAGE);
        int parentplane = 1;
        
        if (mImp.getMimsType() == MimsPlus.MASS_IMAGE) {
            parentplane = mImp.getCurrentSlice();
        } else if(mImp.getMimsType() == MimsPlus.RATIO_IMAGE) {
            parentplane = ui.getMassImages()[mImp.getRatioProps().getNumMassIdx()].getCurrentSlice();
        } else if(mImp.getMimsType() == MimsPlus.HSI_IMAGE) {
            parentplane = ui.getMassImages()[mImp.getHSIProps().getNumMassIdx()].getCurrentSlice();
        } else if(mImp.getMimsType() == MimsPlus.SUM_IMAGE) {
            if (mImp.getSumProps().getSumType() == SumProps.MASS_IMAGE)
               parentplane = ui.getMassImages()[mImp.getSumProps().getParentMassIdx()].getCurrentSlice();
            else if (mImp.getSumProps().getSumType() == SumProps.RATIO_IMAGE)
               parentplane = ui.getMassImages()[mImp.getSumProps().getNumMassIdx()].getCurrentSlice();
        }


        for (int id = 0; id < list.getModel().getSize(); id++) {
            String label = (list.getModel().getElementAt(id).toString());
            Roi roi = (Roi) rois.get(label);
            Integer[] xy = roiManager.getRoiLocation(label, parentplane);
            if (xy != null)
               roi.setLocation(xy[0], xy[1]);

            boolean bDraw = true;


            // If the current slice is the one which the
            // roi was created then we want to show the roi in red.

            //make color preference selectable
            if(roiManager.isSelected(label)) {
                g.setColor(Color.GREEN);
            } else {
                if (mImp.getMimsType() == MimsPlus.HSI_IMAGE) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(Color.RED);
                }
            }

            String name = "";
            if (ui.getSyncROIsAcrossPlanes()) {
                //name = "" + (id + 1);
                name += label;
                java.awt.Rectangle r = roi.getBounds();
                int x = screenX(r.x + r.width / 2);
                int y = screenY(r.y + r.height / 2);
                if (!(roi instanceof Line))
                   g.drawString(name, x, y);
                bDraw = true;
            } else if (!(ui.getSyncROIsAcrossPlanes()) && (roiManager.getSliceNumber(label) == mImp.getCurrentSlice()) && !isRatio) {
                name = "" + (nSyncid + 1);
                java.awt.Rectangle r = roi.getBounds();
                int x = screenX(r.x + r.width / 2);
                int y = screenY(r.y + r.height / 2);
                if (!(roi instanceof Line))
                   g.drawString(name, x, y);
                bDraw = true;
                nSyncid = nSyncid + 1;
            } else if (!(ui.getSyncROIsAcrossPlanes()) && (roiManager.getSliceNumber(label) == parentplane) && isRatio) {
                name = "" + (nSyncid + 1);
                java.awt.Rectangle r = roi.getBounds();
                int x = screenX(r.x + r.width / 2);
                int y = screenY(r.y + r.height / 2);
                if (!(roi instanceof Line))
                   g.drawString(name, x, y);
                bDraw = true;
                nSyncid = nSyncid + 1;
            } else {
                bDraw = false;
            }

            // We dont want to show the boundry if the mouse is within the roi.
            if (cRoi != null && cRoi.toString().equals(roi.toString())) {
                bDraw = false;
            }



            if (bDraw) {
                switch (roi.getType()) {
                    case Roi.COMPOSITE: {
                        roi.setImage(imp);
                        //make color preference selectable
                        if (mImp.getMimsType() == MimsPlus.HSI_IMAGE) {
                            roi.setInstanceColor(Color.WHITE);
                        } else {
                            roi.setInstanceColor(Color.RED);
                        }
                        roi.draw(g);
                        break; 
                    }
                    case Roi.FREELINE: {
                        int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
                        Polygon p = roi.getPolygon();
                        for (int j = 0; j < p.npoints; j++) {
                            x2 = screenX(p.xpoints[j]);
                            y2 = screenY(p.ypoints[j]);
                            if (j > 0) {
                                g.drawLine(x1, y1, x2, y2);
                            }
                            x1 = x2;
                            y1 = y2;
                        }
                        break;
                    }
                    case Roi.POLYLINE: {
                        int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
                        Polygon p = roi.getPolygon();
                        for (int j = 0; j < p.npoints; j++) {
                            x2 = screenX(p.xpoints[j]);
                            y2 = screenY(p.ypoints[j]);
                            if (j > 0) {
                                g.drawLine(x1, y1, x2, y2);
                            }
                            x1 = x2;
                            y1 = y2;
                        }
                        break;
                    }
                    case Roi.LINE: {
                        Line lroi = (Line) roi;
                        int width = lroi.x2 - lroi.x1;
                        int height = lroi.y2 - lroi.y1;                        
                        int x1, y1, x2, y2;
                        if (width > 0) {
                           x1 = screenX(xy[0]);
                           x2 = screenX(xy[0]+width);
                        } else {
                           x1 = screenX(xy[0]+Math.abs(width));
                           x2 = screenX(xy[0]);
                        }
                        if (height > 0) {
                           y1 = screenY(xy[1]);
                           y2 = screenY(xy[1]+height);
                        } else {
                           y1 = screenY(xy[1]+Math.abs(height));
                           y2 = screenY(xy[1]);
                        }                                                
                        g.drawLine(x1, y1, x2, y2);
                        g.drawString(name, x1+width/2, y1+height/2);
                        break;
                    }
                    case Roi.POINT: {
                            java.awt.Rectangle r = roi.getBounds();
                            int x1 = screenX(r.x);
                            int y1 = screenY(r.y);
                            g.drawLine(x1, y1 - 5, x1, y1 + 5);
                            g.drawLine(x1 - 5, y1, x1 + 5, y1);
                            break;
                    }                    
                    case Roi.OVAL: {
                       
                       // THIS IS A TEMPORARY FIX.
                       // I am not sure why this needs to be done but
                       // otherwise ovals do not appear. They were appearing if they 
                       // were saved and then reloaded (which makes no sense) so this
                       // code mimics that process using a bytestram only. (And solves the problem).
                       // A more permamnent solution should be sought. But I beleive it is an ImageJ bug.
                       
                          ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
                          RoiEncoder re = new RoiEncoder(bos);
                          try {
                             re.write(roi);
                          } catch (Exception e) {
                             e.printStackTrace();
                          }
                                                                              
                          byte[] bytes = bos.toByteArray();
                          RoiDecoder rd = new RoiDecoder(bytes, roi.getName());
                          try {
                             roi = rd.getRoi();
                          } catch (Exception e) {
                             e.printStackTrace();
                          }
                        
                        // END OF TEMPORARY SOLUTION.
                          
                            Shape p = (Shape) roi.getPolygon();
                            PathIterator pi = p.getPathIterator(null);
                            int nc = 0;
                            float[] xys = new float[6];
                            int xn = 0, yn = 0, xp = 0, yp = 0, xi = 0, yi = 0;
                            int ct = 0;
                            while (!pi.isDone()) {
                                ct = pi.currentSegment(xys);
                                xn = screenX(Math.round(xys[0]));
                                yn = screenY(Math.round(xys[1]));
                                if (nc == 0) {
                                    xi = xn;
                                    yi = yn;
                                } else {
                                    g.drawLine(xp, yp, xn, yn);
                                }
                                xp = xn;
                                yp = yn;
                                pi.next();
                                nc++;
                            }
                            if (ct == pi.SEG_CLOSE) {
                                g.drawLine(xn, yn, xi, yi);
                            }
                        }
                    case Roi.FREEROI:
                    case Roi.POLYGON:
                    case Roi.RECTANGLE:
                    default:
                         {
                            Polygon p = roi.getPolygon();
                            PathIterator pi = p.getPathIterator(null);
                            int nc = 0;
                            float[] xys = new float[6];
                            int xn = 0, yn = 0, xp = 0, yp = 0, xi = 0, yi = 0;
                            int ct = 0;
                            while (!pi.isDone()) {
                                ct = pi.currentSegment(xys);
                                xn = screenX(Math.round(xys[0]));
                                yn = screenY(Math.round(xys[1]));
                                if (nc == 0) {
                                    xi = xn;
                                    yi = yn;
                                } else {
                                    g.drawLine(xp, yp, xn, yn);
                                }
                                xp = xn;
                                yp = yn;
                                pi.next();
                                nc++;
                            }
                            if (ct == pi.SEG_CLOSE) {
                                g.drawLine(xn, yn, xi, yi);
                            }
                        }
                        break;

                }
            }
        }
    }

    private int getSegment(float[] array, float[] seg, int index) {
        int len = array.length;
        if (index >= len) {
            return -1;
        }
        seg[0] = array[index++];
        int type = (int) seg[0];
        if (type == PathIterator.SEG_CLOSE) {
            return 1;
        }
        if (index >= len) {
            return -1;
        }
        seg[1] = array[index++];
        if (index >= len) {
            return -1;
        }
        seg[2] = array[index++];
        if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
            return 3;
        }
        if (index >= len) {
            return -1;
        }
        seg[3] = array[index++];
        if (index >= len) {
            return -1;
        }
        seg[4] = array[index++];
        if (type == PathIterator.SEG_QUADTO) {
            return 5;
        }
        if (index >= len) {
            return -1;
        }
        seg[5] = array[index++];
        if (index >= len) {
            return -1;
        }
        seg[6] = array[index++];
        if (type == PathIterator.SEG_CUBICTO) {
            return 7;
        }
        return -1;
    }
    private com.nrims.UI ui = null;
    private com.nrims.MimsPlus mImp;
}
