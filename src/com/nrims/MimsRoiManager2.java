/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*  This is the newer version of MimsRoiManager */
package com.nrims;

import com.nrims.data.MIMSFileFilter;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.MessageDialog;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.RoiDecoder;
import ij.io.RoiEncoder;
import ij.measure.Calibration;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.BorderLayout;
import java.awt.Component;
import static java.awt.Component.LEFT_ALIGNMENT;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import static java.awt.Frame.ICONIFIED;
import static java.awt.Frame.NORMAL;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.SwingConstants.LEFT;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author djsia
 */
public class MimsRoiManager2 extends javax.swing.JFrame implements ActionListener {

    private static UI ui = null;
    javax.swing.JFrame instance;

    MimsJTable table;
    File roiFile = null;

    static final String DEFAULT_GROUP = "...";
    static final String GROUP_FILE_NAME = "group";
    static final String GROUP_MAP_FILE_NAME = "group_map";

    // DJ: 12/08/2014
    static final String DEFAULT_TAG = "...";  // might not be used at all.
    static final String TAG_FILE_NAME = "tag";
    static final String TAG_MAP_FILE_NAME = "tag_map";

    public DefaultListModel roiListModel;
    public DefaultListModel groupListModel;
    public DefaultListModel tagListModel;

    ListSelectionListener groupSelectionListener;
    ListSelectionListener roiSelectionListener;
    ListSelectionListener tagSelectionListener;

    boolean holdUpdate = false;
    boolean needsToBeSaved = false;
    JPopupMenu pm;

    Hashtable rois = new Hashtable();

    boolean canceled;
    boolean macro;
    private String savedpath = "";
    boolean previouslySaved = false;

    boolean bAllPlanes = true;

    //add to something
    HashMap locations = new HashMap<String, ArrayList<Integer[]>>();
    HashMap groupsMap = new HashMap<String, String>();
    ArrayList groups = new ArrayList<String>();

    //Dj: 12/05/2014
    HashMap tagsMap = new HashMap<String, ArrayList<String[]>>();
    ArrayList tags = new ArrayList<String>();

    ParticlesManager partManager;
    SquaresManager squaresManager;

    // ImageJ ij;
    /**
     * Creates new form MimsRoiManager2
     */
    public MimsRoiManager2(UI ui) {
        super("MIMS ROI Manager ver 2");

        initComponents();

        this.ui = ui;

        if (instance != null) {
            instance.toFront();
            return;
        }
        instance = this;
        ImageJ ij = IJ.getInstance();
        addKeyListener(ij);

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        //  ROIS
        //=============
        roisHandler(ij);

        //  GROUPS
        //=============
        groupsHandler();

        //  TAGS
        //=============
        tagsHandler();

        //  SPINNERS
        //=============
        spinnersHandler();

        //  CHECKBOXES:
        //=============
        checkBoxesHandler();

    } // end of constructor

    private void groupsHandler() {

        // JList stuff - for Groups
        groupListModel = new DefaultListModel();
        groupListModel.addElement(DEFAULT_GROUP);
        groupjlist.setModel(groupListModel);
        groupjlist.setCellRenderer(new ComboBoxRenderer());

        //groupjlist = new JList(groupListModel);
        groupSelectionListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                groupValueChanged(listSelectionEvent);
            }
        };
        groupjlist.addListSelectionListener(groupSelectionListener);
        groupjlist.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                check(e);
            }

            public void mouseReleased(MouseEvent e) {
                check(e);
            }

            public void check(MouseEvent e) {

                // construct the menu for each item
                final javax.swing.JPopupMenu jPopupMenu = new javax.swing.JPopupMenu();

                if (groupjlist.getSelectedIndices().length == 1) {

                    javax.swing.JMenuItem newGroupItem = new javax.swing.JMenuItem("New group");
                    newGroupItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            createNewGroupActionPerformed(e);
                        }
                    });

                    javax.swing.JMenuItem renameGroupItem = new javax.swing.JMenuItem("Rename group");
                    renameGroupItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            renameGroupActionPerformed(e);
                        }
                    });

                    javax.swing.JMenuItem deleteGroupItem = new javax.swing.JMenuItem("Delete group");
                    deleteGroupItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (groupjlist.getSelectedIndex() == 0) {
                                IJ.error("Cannot delete the Defaut Group ");
                            } else {
                                deleteGroupActionPerformed(e);
                            }
                        }
                    });

                    //  item.addActionListener(actionListener);
                    jPopupMenu.add(newGroupItem);
                    jPopupMenu.add(renameGroupItem);
                    jPopupMenu.add(deleteGroupItem);

                    if (e.isPopupTrigger()) { //if the event shows the menu
                        groupjlist.setSelectedIndex(groupjlist.locationToIndex(e.getPoint())); //select the item
                        jPopupMenu.show(groupjlist, e.getX(), e.getY()); //and show the menu
                    }
                } else if (groupjlist.getSelectedIndices().length > 1) {
                    javax.swing.JMenuItem deleteSelectedGroups = new javax.swing.JMenuItem("Delete Selected Groups");
                    deleteSelectedGroups.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            // int[] Roiidxs = groupjlist.getSelectedIndices();
                            //for (int i = 0; i < Roiidxs.length; i++) {

                            //String roiName = (String) roiListModel.get(Roiidxs[i]);
                            //groupsMap.remove(roiName);
                            // System.out.println("index is = " + Roiidxs[i] );
                            // if(Roiidxs[i] != 0) {  // we don't delete the default group.
                            deleteGroupActionPerformed(e);
                            // }
                            //}
                            needsToBeSaved = true;
                            groupjlist.setSelectedValue(DEFAULT_GROUP, true);
                        }
                    });

                    jPopupMenu.add(deleteSelectedGroups);

                    if (e.isPopupTrigger()) { //if the event shows the menu
                        jPopupMenu.show(groupjlist, e.getX(), e.getY()); //and show the menu
                    }

                }

            } // end of check
        });

        // Group scrollpane.
        //JScrollPane groupscrollpane = new JScrollPane(groupjlist);
        //groupscrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    }

    private void tagsHandler() {

        // JList stuff - for Groups
        tagListModel = new DefaultListModel();
        tagListModel.addElement(DEFAULT_TAG);
        tagjlist.setModel(tagListModel);
        tagjlist.setCellRenderer(new ComboBoxRenderer());

        //groupjlist = new JList(groupListModel);
        tagSelectionListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                tagValueChanged(listSelectionEvent);
            }
        };
        tagjlist.addListSelectionListener(tagSelectionListener);
        tagjlist.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                check(e);
            }

            public void mouseReleased(MouseEvent e) {
                check(e);
            }

            public void check(MouseEvent e) {

                // construct the menu for each item
                final javax.swing.JPopupMenu jPopupMenu = new javax.swing.JPopupMenu();

                javax.swing.JMenuItem newTagItem = new javax.swing.JMenuItem("New tag");
                newTagItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        createNewTagActionPerformed(e);
                    }
                });

                javax.swing.JMenuItem renameTagItem = new javax.swing.JMenuItem("Rename tag");
                renameTagItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        renameTagActionPerformed(e);
                    }
                });

                javax.swing.JMenuItem deleteTagItem = new javax.swing.JMenuItem("Delete tag");
                deleteTagItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (tagjlist.getSelectedIndex() == 0) {
                            IJ.error("Cannot delete the Defaut Tag ");
                        } else {
                            deleteTagActionPerformed(e);
                        }
                    }
                });

                //  item.addActionListener(actionListener);
                jPopupMenu.add(newTagItem);
                jPopupMenu.add(renameTagItem);
                jPopupMenu.add(deleteTagItem);

                if (e.isPopupTrigger()) { //if the event shows the menu
                    tagjlist.setSelectedIndex(tagjlist.locationToIndex(e.getPoint())); //select the item
                    jPopupMenu.show(tagjlist, e.getX(), e.getY()); //and show the menu
                }
            }
        });

    }

    private void spinnersHandler() {

        xPosSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));
        yPosSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));

        xPosSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                posSpinnerStateChanged(evt);
            }
        });
        yPosSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                posSpinnerStateChanged(evt);
            }
        });

        widthSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));
        heightSpinner.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));

        widthSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                hwSpinnerStateChanged(evt);
            }
        });
        heightSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                hwSpinnerStateChanged(evt);
            }
        });

    }

    private void checkBoxesHandler() {

        cbAllPlanes.setSelected(true);

        cbAllPlanes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bAllPlanes = cbAllPlanes.isSelected();
            }
        });

        cbHideAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hideAll();
            }
        });

        cbHideLabels.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hideLabels();
            }
        });

    }

    /**
     * Returns if the user has selected the "hide Rois" checkbox.
     *
     * @return <code>true</code> if checked, otherwise <code>false>/code>.
     */
    public boolean getHideRois() {
        boolean bEnabled = cbHideAll.isSelected();
        return bEnabled;
    }

    /**
     * Returns if the user has selected the "hide Roi labels" checkbox.
     *
     * @return <code>true</code> if checked, otherwise <code>false>/code>.
     */
    public boolean getHideLabel() {
        return cbHideLabels.isSelected();
    }

    /**
     * updates the images so that ROI are not shown.
     */
    private void hideAll() {
        if (getImage() != null) {
            ui.updateAllImages();
        }
    }

    /**
     * updates the images so that ROI labels are not shown.
     */
    private void hideLabels() {
        if (getImage() != null) {
            ui.updateAllImages();
        }
    }

    // DJ: 12/10/2014
    // to force the focus of the any component within a frame.
    // to be used with create new group/tag JOptionPane
    private class RequestFocusListener implements java.awt.event.HierarchyListener {

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            final Component c = e.getComponent();
            if (c.isShowing() && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                java.awt.Window toplevel = javax.swing.SwingUtilities.getWindowAncestor(c);
                toplevel.addWindowFocusListener(new java.awt.event.WindowAdapter() {
                    //@Override
                    public void windowGainedFocus(WindowEvent e) {
                        c.requestFocus();
                    }
                });
            }
        }
    }

    /**
     * Action Method for the "new" group menu item.
     */
    private void createNewGroupActionPerformed(ActionEvent e) {
        javax.swing.JTextArea textArea = new javax.swing.JTextArea();
        textArea.setEditable(true);
        textArea.requestFocusInWindow();
        textArea.addHierarchyListener(new RequestFocusListener());

        //Dimension d = new Dimension(textArea.getWidth(), textArea.getHeight()*2);
        // textArea.setSize(d);
        JScrollPane scrollPane = new JScrollPane(textArea) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(320, 70);
            }

            @Override
            public void setFocusable(boolean focusable) {
                super.setFocusable(true); //To change body of generated methods, choose Tools | Templates.
            }
        };

        java.io.StringWriter writer = new java.io.StringWriter();

        textArea.setText(writer.toString());
        JOptionPane.showMessageDialog(this, scrollPane, "Enter new group(s) name(s):", JOptionPane.PLAIN_MESSAGE);

        // String s = (String)JOptionPane.showInputDialog(this,"Enter new group name:\n","Enter",
        //            JOptionPane.PLAIN_MESSAGE,null,null,"");
        String[] names = textArea.getText().split("\n");

        for (String name : names) {
            addGroup(name);
        }

        if (partManager != null) {
            partManager.updateGroups();
        }
        if (squaresManager != null) {
            squaresManager.updateGroups();
        }
    }

    /**
     * Renames the selected group.
     */
    private void renameGroupActionPerformed(ActionEvent e) {

        // Make sure only 1 group is selected.
        int[] idxs = groupjlist.getSelectedIndices();
        if (idxs.length == 0) {
            return;
        }
        if (idxs.length > 1) {
            error("Please select only one group to rename.");
            return;
        }

        // Prompt user for new name and validate.
        int index = idxs[0];
        String groupName = (String) groupListModel.get(index);
        String newName = (String) JOptionPane.showInputDialog(this, "Enter new name for group " + groupName + " :\n", "Enter",
                JOptionPane.PLAIN_MESSAGE, null, null, groupName);
        if (newName == null) {
            return;
        }
        newName = newName.trim();
        if (newName.equals("") || newName.equals(DEFAULT_GROUP)) {
            return;
        }

        // Get all rois that belong to that group before we actually rename it.
        Roi[] g_rois = getAllROIsInList();

        if (addGroup(newName)) {

            // Remove old group entry.
            groups.remove(groupName);
            groupListModel.removeElement(groupName);

            // Overwrite all previous maps.
            for (int i = 0; i < g_rois.length; i++) {
                groupsMap.put(g_rois[i].getName(), newName);
            }

            // Select new group.
            groupjlist.setSelectedValue(newName, true);
        }
        if (partManager != null) {
            partManager.updateGroups();
        }
        if (squaresManager != null) {
            squaresManager.updateGroups();
        }
    }

    private void roisHandler(ImageJ ij) {

        // JList stuff - for ROIs
        roiListModel = new DefaultListModel();

        roijlist.setModel(roiListModel);

        roijlist.setCellRenderer(new ComboBoxRenderer());
        roijlist.addKeyListener(ij);
        roiSelectionListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                roiValueChanged(listSelectionEvent);
            }
        };
        roijlist.addListSelectionListener(roiSelectionListener);

        roijlist.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                check(e);
            }

            public void mouseReleased(MouseEvent e) {
                check(e);
            }

            public void check(MouseEvent e) {

                // construct the menu for each item
                final javax.swing.JPopupMenu jPopupMenu = new javax.swing.JPopupMenu();

                if (roijlist.getSelectedIndices().length == 1) {

                    javax.swing.JMenuItem renameItem = new javax.swing.JMenuItem("Rename");
                    renameItem.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            rename(null);
                            needsToBeSaved = true;
                        }
                    });

                    //================
                    //   ROI ASSIGN TAGS
                    //================
                    javax.swing.JMenu assignTagsMenu = new javax.swing.JMenu("Assign tags");

                    //====== ROI ASSIGN TO ALL TAGS  ==========
                    javax.swing.JMenuItem assignToAllTags = new javax.swing.JMenuItem("Assign to all tags");
                    assignToAllTags.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            final String roiName = (roijlist.getSelectedValue() == null) ? "-1" : roijlist.getSelectedValue().toString();
                            ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(roiName);

                            if (associatedTags == null) {
                                associatedTags = new ArrayList<String>();
                            }

                            ArrayList<Integer> allValidIndicesToBeSetToSelected = new ArrayList<Integer>();

                            for (int indx = 0; indx < tagListModel.size(); indx++) {
                                String tagName = (String) (tagListModel.getElementAt(indx));

                                if (!tagName.equals(DEFAULT_TAG)) {

                                    allValidIndicesToBeSetToSelected.add(indx);

                                    if (!associatedTags.contains(tagName)) {
                                        associatedTags.add(tagName);

                                        /*
                                        int[] newSelectedIndices = new int[tagjlist.getSelectedIndices().length+1];
                                        if(newSelectedIndices == null)
                                            newSelectedIndices = new int[1];
                                        newSelectedIndices[newSelectedIndices.length-1] = tagListModel.indexOf(tagName);
                                        tagjlist.setSelectedIndices(newSelectedIndices);
                                         */
                                    }
                                }
                            }
                            tagsMap.put(roiName, associatedTags); // we update the roi's associated tags.

                            int[] toBeSelected = new int[allValidIndicesToBeSetToSelected.size()];
                            for (int iddx = 0; iddx < allValidIndicesToBeSetToSelected.size(); iddx++) {
                                toBeSelected[iddx] = allValidIndicesToBeSetToSelected.get(iddx).intValue();

                            }
                            tagjlist.setSelectedIndices(toBeSelected);
                            needsToBeSaved = true;

                        }
                    });
                    assignTagsMenu.add(assignToAllTags);

                    //====== ROI ASSIGN TO TAGS INDIVIDUALLY ==========
                    for (int i = 0; i < tags.size(); i++) {
                        final String roiName = (roijlist.getSelectedValue() == null) ? "-1" : roijlist.getSelectedValue().toString();
                        javax.swing.JMenuItem tagItem = new javax.swing.JMenuItem((String) (tags.get(i)));
                        //tagItem.setName((String) (tags.get(i)));
                        //tagItem.getText()
                        tagItem.addActionListener(new ActionListener() {

                            String ItemName = "";

                            public void actionPerformed(ActionEvent e) {
                                String tagName = e.getActionCommand();
                                ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(roiName);
                                if (associatedTags == null) {
                                    associatedTags = new ArrayList<String>();
                                }
                                associatedTags.add(tagName);
                                tagsMap.put(roiName, associatedTags);
                                needsToBeSaved = true;
                                tagjlist.setSelectedValue(tagName, true);
                                ItemName = tagName;

                            }
                        });
                        // we add the submenu items while deactivating any tag item that is already associated with 
                        // the selected ROI.
                        assignTagsMenu.add(tagItem);

                        ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(roiName);

                        if (associatedTags != null) {
                            for (String tag : associatedTags) {
                                if (tag.equals(tagItem.getText())) {
                                    tagItem.setEnabled(false);
                                }
                            }
                        }
                    }

                    //================
                    //   ROI REMOVE TAGS
                    //================
                    //====== ROI REMOVE ALL ASSOCIATED TAGS  ==========
                    javax.swing.JMenu removeTagsMenu = new javax.swing.JMenu("Remove tags");
                    javax.swing.JMenuItem removeAllTags = new javax.swing.JMenuItem("Remove all tags");
                    removeAllTags.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            final String roiName = (roijlist.getSelectedValue() == null) ? "-1" : roijlist.getSelectedValue().toString();

                            ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(roiName);
                            if (associatedTags != null) {
                                tagsMap.remove(roiName);
                                needsToBeSaved = true;
                                tagjlist.setSelectedValue(DEFAULT_TAG, true);
                            }
                        }
                    });
                    removeTagsMenu.add(removeAllTags);

                    //====== ROI REMOVE TAGS INDIVIDUALLY ==========
                    for (int i = 0; i < tags.size(); i++) {
                        final String roiName = (roijlist.getSelectedValue() == null) ? "-1" : roijlist.getSelectedValue().toString();
                        javax.swing.JMenuItem tagItem = new javax.swing.JMenuItem((String) (tags.get(i)));
                        tagItem.setName((String) (tags.get(i)));
                        tagItem.addActionListener(new ActionListener() {

                            String ItemName = "";

                            public void actionPerformed(ActionEvent e) {
                                String tagName = e.getActionCommand();
                                //Collin, use hashset?
                                //tagsMap.get(roiName).add(tagName);

                                ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(roiName);
                                if (associatedTags != null && !associatedTags.isEmpty()) {
                                    //associatedTags = new ArrayList<String>();

                                    associatedTags.remove(tagName);

                                    tagsMap.put(roiName, associatedTags);
                                    //tagsMap.remove(roiName);
                                    needsToBeSaved = true;
                                    tagjlist.setSelectedValue(DEFAULT_TAG, true);

                                    // ItemName = tagName;
                                }

                            }
                        });
                        // we add the submenu items while deactivating any tag item that is
                        // not already associated with the selected ROI.
                        removeTagsMenu.add(tagItem);

                        ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(roiName);

                        if (associatedTags == null || associatedTags.isEmpty()) {
                            tagItem.setEnabled(false);

                        } else {
                            for (String tag : associatedTags) {
                                tagItem.setEnabled(false);
                                if (tag.equals(tagItem.getText())) {
                                    tagItem.setEnabled(true);
                                    break;
                                }
                            }
                        }
                    }

                    //================
                    //   ROI DEASSIGN GROUP
                    //================
                    javax.swing.JMenuItem deAssign = new javax.swing.JMenuItem("Deassign");

                    // if the ROI selected is not assigned, we disable the "deAssign menu option"
                    if (groupjlist.getSelectedIndex() == 0) {
                        deAssign.setEnabled(false);
                    }
                    deAssign.addActionListener(new ActionListener() {
                        final String roiName = (roijlist.getSelectedValue() == null) ? "-1" : roijlist.getSelectedValue().toString();

                        public void actionPerformed(ActionEvent e) {
                            groupsMap.remove(roiName);
                            needsToBeSaved = true;
                            groupjlist.setSelectedValue(DEFAULT_GROUP, true);
                        }
                    });

                    //================
                    //   ROI CHANGE GROUP
                    //================
                    javax.swing.JMenu changeGroupMenu = new javax.swing.JMenu("Change group");
                    for (int i = 0; i < groups.size(); i++) {
                        final String roiName = (roijlist.getSelectedValue() == null) ? "-1" : roijlist.getSelectedValue().toString();
                        javax.swing.JMenuItem groupItem = new javax.swing.JMenuItem((String) (groups.get(i)));
                        groupItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                // Assign Roi to Group chosen from the submenu
                                String groupName = e.getActionCommand();
                                groupsMap.put(roiName, groupName);
                                needsToBeSaved = true;
                                groupjlist.setSelectedValue(groupName, true);
                            }
                        });

                        // quick check to exclude displaying the group where itis already assigned to
                        if (groupsMap.get(roiName) == null) {
                            changeGroupMenu.add(groupItem);
                        } else if (!groupsMap.get(roiName).toString().equals((String) (groups.get(i)))) {
                            changeGroupMenu.add(groupItem);
                        }
                    }

                    //  item.addActionListener(actionListener);
                    jPopupMenu.add(renameItem);
                    jPopupMenu.add(assignTagsMenu);
                    jPopupMenu.add(removeTagsMenu);
                    jPopupMenu.add(changeGroupMenu);
                    jPopupMenu.add(deAssign);

                    if (e.isPopupTrigger()) { //if the event shows the menu
                        roijlist.setSelectedIndex(roijlist.locationToIndex(e.getPoint())); //select the item
                        jPopupMenu.show(roijlist, e.getX(), e.getY()); //and show the menu
                    }
                } // In case more than one roi are selected.
                else if (roijlist.getSelectedIndices().length > 1) {

                    javax.swing.JMenu assignAllToGroup = new javax.swing.JMenu("Assign all to group ");
                    for (int i = 0; i < groups.size(); i++) {

                        //final String roiName = (roijlist.getSelectedValue() == null) ? "-1" : roijlist.getSelectedValue().toString();
                        javax.swing.JMenuItem groupItem = new javax.swing.JMenuItem((String) (groups.get(i)));
                        groupItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                // Assign Roi to Group chosen from the submenu
                                String groupName = e.getActionCommand();
                                for (int selectedIndex : roijlist.getSelectedIndices()) {
                                    String roiName = (roiListModel.elementAt(selectedIndex)).toString();
                                    groupsMap.put(roiName, groupName);
                                }
                                needsToBeSaved = true;
                                groupjlist.setSelectedValue(groupName, true);
                            }
                        });
                        // here
                        //if(groupjlist.getSelectedIndex() == -1 || groupjlist.getSelectedIndex() == 0)
                        assignAllToGroup.add(groupItem);
                    }

                    javax.swing.JMenuItem deAssignAllGroups = new javax.swing.JMenuItem("Deassign all groups");
                    deAssignAllGroups.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            int[] Roiidxs = roijlist.getSelectedIndices();
                            for (int i = 0; i < Roiidxs.length; i++) {
                                String roiName = (String) roiListModel.get(Roiidxs[i]);
                                groupsMap.remove(roiName);
                            }
                            needsToBeSaved = true;
                            groupjlist.setSelectedValue(DEFAULT_GROUP, true);
                        }
                    });

                    javax.swing.JMenu assignAllToTag = new javax.swing.JMenu("Assign all to Tag ");
                    for (int i = 0; i < tags.size(); i++) {

                        //final String roiName = (roijlist.getSelectedValue() == null) ? "-1" : roijlist.getSelectedValue().toString();
                        javax.swing.JMenuItem tagItem = new javax.swing.JMenuItem((String) (tags.get(i)));
                        tagItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                // Assign Roi to Group chosen from the submenu
                                String tagName = e.getActionCommand();
                                for (int selectedIndex : roijlist.getSelectedIndices()) {
                                    String roiName = (roiListModel.elementAt(selectedIndex)).toString();

                                    ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(roiName);
                                    if (associatedTags == null) {
                                        associatedTags = new ArrayList<String>();
                                    }
                                    associatedTags.add(tagName);
                                    tagsMap.put(roiName, associatedTags);
                                    //needsToBeSaved = true;
                                    //tagjlist.setSelectedValue(tagName, true);
                                }
                                needsToBeSaved = true;
                                tagjlist.setSelectedValue(tagName, true);
                            }
                        });
                        // here
                        //if(groupjlist.getSelectedIndex() == -1 || groupjlist.getSelectedIndex() == 0)
                        assignAllToTag.add(tagItem);
                    }

                    // javax.swing.JMenu deAssignTags = new javax.swing.JMenu("Deassign tags");
                    javax.swing.JMenuItem deAssignAllTags = new javax.swing.JMenuItem("Deassign all tags");
                    deAssignAllTags.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            int[] Roiidxs = roijlist.getSelectedIndices();
                            for (int i = 0; i < Roiidxs.length; i++) {
                                String roiName = (String) roiListModel.get(Roiidxs[i]);

                                tagsMap.remove(roiName);

                            }
                            needsToBeSaved = true;
                            tagjlist.setSelectedValue(DEFAULT_TAG, true);
                        }
                    });
                    //deAssignTags.add(deAssignAllTags);

                    /*
                    for(int idx = 0; idx< tags.size(); idx++){
                         javax.swing.JMenuItem tagItem = new javax.swing.JMenuItem(tags.get(idx).toString());
                         tagItem.addActionListener(new ActionListener() {

                             public void actionPerformed(ActionEvent e) {
                                 
                                String tagName = e.getActionCommand();
                                for(int selectedIndex : roijlist.getSelectedIndices()){
                                    String roiName = (roiListModel.elementAt(selectedIndex)).toString();
                                    
                                    ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(roiName);
                                    
                                    if(associatedTags != null &&  associatedTags.contains(tagName)){
                                        associatedTags.remove(tagName);
                                        tagsMap.put(roiName, associatedTags);
                                    }
                                    
                                    
                                }
                                needsToBeSaved = true;
                                
                                if(tagjlist.getSelectedValue().equals(DEFAULT_TAG)){
                                    System.out.println("DEF_TAG_WAS_HIGHLIGHTED");
                                    tagjlist.setSelectedValue(tagName , true);
                                } else {
                                    System.out.println("DEF_TAG_WAS_NOT_HIGHLIGHTED");
                                    tagjlist.setSelectedValue(DEFAULT_TAG, true);
                                }
                                groupjlist.setSelectedValue(DEFAULT_GROUP, true);
                                 
                             }
                         });
                         
                         deAssignTags.add(tagItem);
                         
                    }
                     */
                    jPopupMenu.add(assignAllToGroup);
                    jPopupMenu.add(deAssignAllGroups);
                    jPopupMenu.add(assignAllToTag);
                    jPopupMenu.add(deAssignAllTags);

                    if (e.isPopupTrigger()) { //if the event shows the menu
                        jPopupMenu.show(roijlist, e.getX(), e.getY()); //and show the menu
                    }
                }
            }

        });

    }

    // DJ: 12/05/2014
    /**
     * Renames the selected group.
     */
    private void renameTagActionPerformed(ActionEvent e) {

        // Make sure only 1 group is selected.
        int[] idxs = tagjlist.getSelectedIndices();
        if (idxs.length == 0) {
            return;
        }
        if (idxs.length > 1) {
            error("Please select only one tag to rename.");
            return;
        }

        // Prompt user for new name and validate.
        int index = idxs[0];
        String tagName = (String) tagListModel.get(index);
        String newName = (String) JOptionPane.showInputDialog(this, "Enter new name for tag " + tagName + " :\n", "Enter",
                JOptionPane.PLAIN_MESSAGE, null, null, tagName);
        if (newName == null) {
            return;
        }
        newName = newName.trim();
        if (newName.equals("") || newName.equals(DEFAULT_TAG)) {
            return;
        }

        // Get all rois that belong to that tag before we actually rename it.
        Roi[] t_rois = getAllROIsInList();

        System.out.println("number of rois in list is = " + t_rois.length);

        if (addTag(newName)) {

            // Remove old group entry.
            tags.remove(tagName);
            tagListModel.removeElement(tagName);

            // Overwrite all previous maps.
            for (int i = 0; i < t_rois.length; i++) {
                ArrayList<String> associated_tags = (ArrayList<String>) tagsMap.get(t_rois);
                if (associated_tags == null) {
                    associated_tags = new ArrayList<String>();
                }
                associated_tags.add(newName);
                tagsMap.put(t_rois[i].getName(), associated_tags);
            }

            // Select new tag.
            tagjlist.setSelectedValue(newName, true);
        }
        //if(partManager != null) partManager.updateGroups();
        //if(squaresManager != null) squaresManager.updateGroups();
    }

    /**
     * Deletes selected groups and all associations.
     */
    private void deleteGroupActionPerformed(ActionEvent e) {
        int[] idxs = groupjlist.getSelectedIndices();
        for (int i = idxs.length - 1; i >= 0; i--) { //DJ: original : i>=0
            int indexToDelete = idxs[i];
            String groupNameToDelete = (String) groupListModel.get(indexToDelete);
            for (int id = 0; id < roijlist.getModel().getSize(); id++) {
                String roiName = roijlist.getModel().getElementAt(id).toString();
                String groupName = (String) groupsMap.get(roiName);
                if (groupName != null) {
                    if (groupName.equals(groupNameToDelete)) {
                        groupsMap.remove(roiName);
                    }
                }
            }
            groupListModel.removeElementAt(indexToDelete);
            groups.remove(groupNameToDelete);
        }
        if (partManager != null) {
            partManager.updateGroups();
        }
        if (squaresManager != null) {
            squaresManager.updateGroups();
        }
        needsToBeSaved = true;
    }

    // DJ: 12/05/2014
    /**
     * Action Method for the "new" Tag(s) menu item.
     */
    private void createNewTagActionPerformed(ActionEvent e) {
        /*
        String t = (String)JOptionPane.showInputDialog(this,"Enter new tag name:\n","Enter",
                    JOptionPane.PLAIN_MESSAGE,null,null,"");
        addTag(t);
        //if(partManager != null) partManager.updateGroups();
        //if(squaresManager != null) squaresManager.updateGroups();
         */

        javax.swing.JTextArea textArea = new javax.swing.JTextArea();
        textArea.requestFocusInWindow();
        textArea.addHierarchyListener(new RequestFocusListener());

        //Dimension d = new Dimension(textArea.getWidth(), textArea.getHeight()*2);
        // textArea.setSize(d);
        JScrollPane scrollPane = new JScrollPane(textArea) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(320, 70);
            }

            @Override
            public void setFocusable(boolean focusable) {
                super.setFocusable(true); //To change body of generated methods, choose Tools | Templates.
            }
        };

        java.io.StringWriter writer = new java.io.StringWriter();

        textArea.setText(writer.toString());
        JOptionPane.showMessageDialog(this, scrollPane, "Enter new tag(s) name(s):", JOptionPane.PLAIN_MESSAGE);
        // String s = (String)JOptionPane.showInputDialog(this,"Enter new group name:\n","Enter",
        //            JOptionPane.PLAIN_MESSAGE,null,null,"");

        String[] names = textArea.getText().split("\n");

        for (String name : names) {
            addTag(name);
        }

    }

    // DJ: 12/05/2014
    /**
     * Deletes selected Tags and all associations.
     */
    private void deleteTagActionPerformed(ActionEvent e) {
        /*
       int[] idxs = groupjlist.getSelectedIndices();
       for(int i = idxs.length-1; i >=0; i--) {
          int indexToDelete = idxs[i];
          String groupNameToDelete = (String)groupListModel.get(indexToDelete);
          for (int id = 0; id < roijlist.getModel().getSize(); id++) {
             String roiName = roijlist.getModel().getElementAt(id).toString();
             String groupName = (String)groupsMap.get(roiName);
             if (groupName != null) {
                if (groupName.equals(groupNameToDelete))
                   groupsMap.remove(roiName);
             }
          }
          groupListModel.removeElementAt(indexToDelete);
          groups.remove(groupNameToDelete);
       }
       if(partManager != null) partManager.updateGroups();
        if(squaresManager != null) squaresManager.updateGroups();
        needsToBeSaved = true;
         */
    }

    /**
     * Action method for changing position spinners.
     */
    private void posSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {
        String label = "";

        if (holdUpdate) {
            return;
        }
        if (roijlist.getSelectedIndices().length != 1) {
            error("Exactly one item in the list must be selected.");
            return;
        } else {
            label = roijlist.getSelectedValue().toString();
        }

        // Make sure we have an image
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }

        // Update the
        int plane = imp.getCurrentSlice();
        int trueplane = ui.getMimsAction().trueIndex(plane);
        ArrayList xylist = (ArrayList<Integer[]>) locations.get(label);
        xylist.set(trueplane - 1, new Integer[]{(Integer) xPosSpinner.getValue(), (Integer) yPosSpinner.getValue()});
        locations.put(label, xylist);

        // 
        Roi roi = (Roi) rois.get(label);
        if (roi.getType() == Roi.LINE) {
            moveLine(roi, (Integer) xPosSpinner.getValue(), (Integer) yPosSpinner.getValue());
            roi = (Roi) rois.get(roi.getName());
        } else {
            roi.setLocation((Integer) xPosSpinner.getValue(), (Integer) yPosSpinner.getValue());
        }
        imp.setRoi(roi);
        move();
        needsToBeSaved = true;
        updatePlots(false);

    }

    /**
     * Action method for changing size spinners.
     */
    private void hwSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {

        String label = "";

        if (holdUpdate) {
            return;
        }

        if (roijlist.getSelectedIndices().length != 1) {
            error("Exactly one item in the list must be selected.");
            return;
        } else {
            label = roijlist.getSelectedValue().toString();
        }

        // Make sure we have an image
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }

        // Make sure we have a ROI
        Roi oldroi = (Roi) rois.get(label);
        if (oldroi == null) {
            return;
        }

        // There is no setWidth or  setHeight method for a ROI
        // so essentially we have to create a new one, setRoi
        // will delete the old one from the rois hashtable.
        Roi newroi = null;
        java.awt.Rectangle rect = oldroi.getBounds();

        // Dont do anything if the values are changing
        // only because user selected a different ROI.
        if (oldroi.getType() == ij.gui.Roi.RECTANGLE) {
            newroi = new Roi(rect.x, rect.y, (Integer) widthSpinner.getValue(), (Integer) heightSpinner.getValue(), imp);
        } else if (oldroi.getType() == ij.gui.Roi.OVAL) {
            newroi = new OvalRoi(rect.x, rect.y, (Integer) widthSpinner.getValue(), (Integer) heightSpinner.getValue());
        } else {
            return;
        }

        //we give it the old name so that setRoi will
        // know which original roi to delete.
        newroi.setName(oldroi.getName());
        rois.put(newroi.getName(), newroi);
        imp.setRoi(newroi);
        imp.updateAndRepaintWindow();
        needsToBeSaved = true;
        updatePlots(false);
    }

    /*
    HashMap locations = new HashMap<String, ArrayList<Integer[]>>();
    HashMap groupsMap = new HashMap<String, String>();
    ArrayList groups = new ArrayList<String>();
     */
    // DJ: 08/22/2014
    public HashMap<String, ArrayList<Integer[]>> getLocations() {
        return locations;
    }

    public HashMap<String, String> getGroupMap() {
        return groupsMap;
    }

    public ArrayList<String> getGroups() {
        return (ArrayList<String>) groups;
    }

    public void setLocations(HashMap<String, ArrayList<Integer[]>> locations) {
        this.locations = locations;
    }

    public void setGroupMap(HashMap<String, String> gMap) {
        this.groupsMap = gMap;
    }

    public void setGroups(ArrayList<String> groups) {
        this.groups = groups;
    }

    public boolean needsToBeSaved() {
        return needsToBeSaved;
    }

    public void setNeedsToBeSaved(boolean bool) {
        needsToBeSaved = bool;
    }

    /**
     * Overrides PlugInFrame.close().
     */
    //@Override
    public void close() {
        this.setVisible(false);
    }

    //DJ: 12/05/2014
    public void clearRoiJList() {
        roijlist.clearSelection();
    }

    /**
     * Returns the ParticlesManager.
     */
    public ParticlesManager getParticlesManager() {
        return partManager;
    }

    /**
     * Returns the SquaresManager.
     */
    public SquaresManager getSquaresManager() {
        return squaresManager;
    }

    /**
     * Updates the position spinners.
     */
    void updateSpinners() {

        String label = "";
        Roi roi = null;
        ArrayList xylist;
        Integer[] xy = new Integer[2];

        if (roijlist.getSelectedIndices().length != 1) {
            return;
        } else {
            label = roijlist.getSelectedValue().toString();
        }

        if (!label.equals("")) {
            xylist = (ArrayList<Integer[]>) locations.get(label);
        } else {
            return;
        }

        if (xylist != null) {
            xy = (Integer[]) xylist.get(ui.getOpenMassImages()[0].getCurrentSlice() - 1);
        } else {
            return;
        }

        if (xy != null) {
            holdUpdate = true;
            xPosSpinner.setValue(xy[0]);
            widthSpinner.setValue(xy[1]);
            holdUpdate = false;
        } else {
            return;
        }

    }

    /**
     * Shows the ROI manager frame.
     */
    public void viewManager() {
        if (super.getExtendedState() == ICONIFIED) {
            showFrame();
        } else {
            setVisible(false);
            setVisible(true);
        }
    }

    /**
     * Shows the ROI manager frame.
     */
    public void showFrame() {
        setVisible(true);
        toFront();
        setExtendedState(NORMAL);
    }

    /**
     * Sets the Roi in its current location.
     */
    private void setRoi(ImagePlus imp, Roi roi) {

        // ROI old name - based on its old bounding rect
        String label = roi.getName();

        Rectangle rec = roi.getBounds();
        MimsPlus mp = null;
        try {
            mp = (MimsPlus) imp;
        } catch (Exception e) {
            return;
        }
        int plane = 1;
        int t = mp.getMimsType();
        if (t == MimsPlus.RATIO_IMAGE) {
            plane = ui.getMassImage(mp.getRatioProps().getNumMassIdx()).getCurrentSlice();
        } else if (t == MimsPlus.HSI_IMAGE) {
            plane = ui.getMassImage(mp.getHSIProps().getNumMassIdx()).getCurrentSlice();
        } else if (t == MimsPlus.SUM_IMAGE) {
            plane = ui.getMassImage(mp.getSumProps().getParentMassIdx()).getCurrentSlice();
        } else if (t == MimsPlus.MASS_IMAGE) {
            plane = mp.getCurrentSlice();
        }
        int trueplane = ui.getMimsAction().trueIndex(plane);

        ArrayList xylist = (ArrayList<Integer[]>) locations.get(label);
        xylist.set(trueplane - 1, new Integer[]{rec.x, rec.y});
        locations.put(label, xylist);

        imp.setRoi(roi);

        imp.updateAndRepaintWindow();
    }

    /**
     * Returns the ROI with name <code>name</code>
     *
     * @return the ROI.
     */
    public Roi getRoiByName(String name) {
        return (Roi) rois.get(name);
    }

    /**
     * Sets the Roi in its current location.
     */
    private boolean move(ImagePlus imp, Roi roi) {
        if (imp == null) {
            return false;
        }
        if (roi == null) {
            return false;
        }

        setRoi(imp, roi);

        // Debug
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Move");
        }
        return true;
    }

    /**
     * Line rois require a special method for moving because the <code>setlocation()</code> method does not work for
     * line Rois (at least not the way you would expact, and not the ways it works for shape Rois).
     */
    public void moveLine(Roi oldRoi, int newX, int newY) {
        ImagePlus imp = getImage();

        if (oldRoi.isLine()) {
            Rectangle rec = oldRoi.getBounds();
            int oldx = rec.x;
            int oldy = rec.y;
            int deltax = newX - oldx;
            int deltay = newY - oldy;
            Line lineroi = (Line) oldRoi;
            Line newline = new Line(lineroi.x1 + deltax, lineroi.y1 + deltay, lineroi.x2 + deltax, lineroi.y2 + deltay, imp);
            newline.setName(oldRoi.getName());
            moveLine(oldRoi, newline);
        }
    }

    /**
     * Line rois require a special method for moving because the <code>setlocation()</code> method does not work for
     * line Rois (at least not the way you would expact, and not the ways it works for shape Rois).
     */
    public void moveLine(Roi oldRoi, Roi newRoi) {

        if (!oldRoi.getName().equals(newRoi.getName())) {
            System.out.println("WARNING: Rois should have the same name.");
        }

        if (!oldRoi.isLine() || !newRoi.isLine()) {
            System.out.println("WARNING: Both Rois should be Line.");
        }

        rois.remove(oldRoi.getName());
        rois.put(newRoi.getName(), newRoi);
        needsToBeSaved = true;
    }

    /**
     * Sets the Roi in its current location.
     */
    public boolean move() {
        // Get the image and the roi
        ImagePlus imp = getImage();
        Roi roi = imp.getRoi();
        boolean b = true;

        if (bAllPlanes == false) {
            b = move(imp, roi);
        }

        if (bAllPlanes == true) {
            String label = roi.getName();
            Rectangle rec = roi.getBounds();
            ArrayList xylist = (ArrayList<Integer[]>) locations.get(label);
            if (xylist == null) {
                int stacksize = ui.getMimsAction().getSize();
                xylist = new ArrayList<Integer[]>();
                Integer[] xy = new Integer[2];
                for (int i = 0; i < stacksize; i++) {
                    xy = new Integer[]{rec.x, rec.y};
                    xylist.add(i, xy);
                }
                locations.put(label, xylist);
            }

            int size = ui.getMimsAction().getSize();

            for (int p = 1; p <= size; p++) {
                xylist.set(p - 1, new Integer[]{rec.x, rec.y});
            }
            locations.put(label, xylist);
        }
        needsToBeSaved = true;
        return b;
    }

    /**
     * Determines the behavior when a group entry is clicked.
     */
    private void groupValueChanged(ListSelectionEvent e) {

        if (e == null) {
            return;
        }

        boolean adjust = e.getValueIsAdjusting();

        if (!adjust) {
            holdUpdate = true;
            boolean defaultGroupSelected = false;

            boolean defaultTagSelected = false; // DJ

            // Get the selected groups.
            int[] indices = groupjlist.getSelectedIndices();
            String[] groupNames = new String[indices.length];
            for (int i = 0; i < indices.length; i++) {
                groupNames[i] = (String) groupListModel.getElementAt(indices[i]);
                if (groupNames[i].equals(DEFAULT_GROUP)) {
                    defaultGroupSelected = true;
                }
            }
            // DJ:
            // Get the selected tags.
            int[] tagIndices = tagjlist.getSelectedIndices();
            if (tagIndices == null || tagIndices.length == 0) {
                tagjlist.setSelectedValue(DEFAULT_TAG, true);
                tagIndices = tagjlist.getSelectedIndices();
                //tagIndices = new int[1];
                //tagIndices[0]=0;
            }

            String[] tagNames = new String[tagIndices.length];
            for (int i = 0; i < tagIndices.length; i++) {
                tagNames[i] = (String) tagListModel.getElementAt(tagIndices[i]);
                if (tagNames[i].equals(DEFAULT_TAG)) {
                    defaultTagSelected = true;
                }
            }

            // Show only Rois that are part of the selected groups.
            String roiName;
            roiListModel.clear();
            for (Object object : rois.keySet()) {
                roiName = (String) object;
                boolean isGroupContainsRoi = groupContainsRoi(groupNames, roiName);
                boolean isTagContainsRoi = tagContainsRoi(tagNames, roiName);            // DJ
                if (isGroupContainsRoi && isTagContainsRoi) {                                        // DJ
                    roiListModel.addElement(roiName);
                }
            }
            sortROIList();

            /*
          
          // Disable delete button if Default Group is one of the groups selected.
          //this is why the delete button is a class variable andd the others aren't...
          if (defaultGroupSelected) {
             deleteButton.setEnabled(false);
             rename.setEnabled(false);
          } else {
             deleteButton.setEnabled(true);
             rename.setEnabled(true);
          }
             */
            ui.updateAllImages();

            holdUpdate = false;
        }
    }

    /**
     * Determines the behavior when a tag entry is clicked.
     */
    private void tagValueChanged(ListSelectionEvent e) {

        if (e == null) {
            return;
        }

        boolean adjust = e.getValueIsAdjusting();

        if (!adjust) {
            holdUpdate = true;
            boolean defaultTagSelected = false;
            boolean defaultGroupSelected = false;

            // Get the selected tags.
            int[] indices = tagjlist.getSelectedIndices();
            String[] tagNames = new String[indices.length];
            for (int i = 0; i < indices.length; i++) {
                tagNames[i] = (String) tagListModel.getElementAt(indices[i]);
                // System.out.println(tagNames[i]);
                if (tagNames[i].equals(DEFAULT_TAG)) {
                    defaultTagSelected = true;
                }
            }

            // DJ:
            // Get the selected groups.
            int[] grIndices = groupjlist.getSelectedIndices();

            if (grIndices == null || grIndices.length == 0) {
                groupjlist.setSelectedValue(DEFAULT_GROUP, true);
                grIndices = groupjlist.getSelectedIndices();
                //tagIndices = new int[1];
                //tagIndices[0]=0;
            }

            String[] groupNames = new String[grIndices.length];
            for (int i = 0; i < grIndices.length; i++) {
                groupNames[i] = (String) groupListModel.getElementAt(grIndices[i]);
                if (groupNames[i].equals(DEFAULT_GROUP)) {
                    defaultGroupSelected = true;
                }
            }

            // Show only Rois that are part of the selected groups.
            String roiName;
            roiListModel.clear();
            for (Object object : rois.keySet()) {
                roiName = (String) object;
                boolean isTagContainsRoi = tagContainsRoi(tagNames, roiName);
                boolean isGroupContainsRoi = groupContainsRoi(groupNames, roiName); //DJ
                if ((isTagContainsRoi && isGroupContainsRoi)) {
                    roiListModel.addElement(roiName);
                }
            }
            sortROIList();

            /*
          
          // Disable delete button if Default Group is one of the groups selected.
          //this is why the delete button is a class variable andd the others aren't...
          if (defaultGroupSelected) {
             deleteButton.setEnabled(false);
             rename.setEnabled(false);
          } else {
             deleteButton.setEnabled(true);
             rename.setEnabled(true);
          }
             */
            ui.updateAllImages();

            holdUpdate = false;
        }
    }

    /**
     * Checks to see if the group assigned to roiName is contained within groupNames.
     */
    private boolean groupContainsRoi(String[] groupNames, String roiName) {
        for (String groupName : groupNames) {
            if (groupName.equals(DEFAULT_GROUP)) {
                return true;
            }
            if (groupsMap.get(roiName) == null) {
                return false;
            }
            if (((String) groupsMap.get(roiName)).equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    // DJ: 12/08/2014
    /**
     * Checks to see if the tag assigned to roiName is contained within tagNames.
     */
    private boolean tagContainsRoi(String[] tagNames, String roiName) {

        for (String tagName : tagNames) {
            if (tagName.equals(DEFAULT_TAG)) {
                return true;
            }
            if (tagsMap.get(roiName) == null) {
                return false;
            }

            ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(roiName);
            for (int i = 0; i < associatedTags.size(); i++) {
                if (associatedTags.get(i).equals(tagName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Prompts the user for a new name for the selected ROI.
     *
     * @param name2 ROI name.
     * @return <code>true</code> if successful, otherwise <code>/false</code>
     */
    boolean rename(String name2) {
        int[] indices = roijlist.getSelectedIndices();
        if (indices.length != 1) {
            return error("Please select only one item in the list.");
        }

        int index = indices[0];
        String name = roiListModel.get(index).toString();

        if (name2 == null) {
            name2 = promptForName(name);
        }
        if (name2 == null) {
            return false;
        }
        if (name2.trim().length() == 0) {
            return false;
        }
        if (rois.get(name2) != null) {
            System.out.println("A Roi with name " + name + "already exists.");
            return false;
        }
        Roi roi = (Roi) rois.get(name);

        // update rois hashtable
        rois.remove(name);
        roi.setName(name2);
        rois.put(name2, roi);

        // update locations array.
        locations.put(name2, locations.get(name));
        locations.remove(name);

        // update groups map.
        String group = (String) groupsMap.remove(name);
        if (group != null) {
            groupsMap.put(name2, group);
        }

        // update the list display.
        roiListModel.set(index, name2);
        roijlist.setSelectedIndex(index);

        sortROIList();
        needsToBeSaved = true;
        return true;
    }

    /**
     * Displays a prompt asking the user for a new name for the selected ROI.
     */
    private String promptForName(String name) {
        GenericDialog gd = new GenericDialog("MIMS ROI Manager");
        gd.addStringField("Rename As:", name, 20);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return null;
        }
        String name2 = gd.getNextString();
        return name2;
    }

    /**
     * Sorts the list of ROIs.
     */
    private void sortROIList() {
        String[] roinames = new String[roiListModel.size()];
        for (int i = 0; i < roinames.length; i++) {
            roinames[i] = (String) roiListModel.getElementAt(i);
        }
        roiListModel.removeAllElements();
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Integer> numeric = new ArrayList<Integer>();
        for (int i = 0; i < roinames.length; i++) {
            //keep track of numeric and non-numeric names separately
            if (isNumericName(roinames[i]) && !roinames[i].startsWith("0")) {
                numeric.add(Integer.parseInt(roinames[i]));
            } else {
                names.add(roinames[i]);
            }
        }
        //add the Rois sorted numerically
        Collections.sort(numeric);
        for (int i = 0; i < numeric.size(); i++) {
            roiListModel.addElement("" + numeric.get(i));
        }
        //add the Rois sorted lexigraphically
        Collections.sort(names);
        for (int i = 0; i < names.size(); i++) {
            roiListModel.addElement(names.get(i));
        }
    }

    /**
     * Tests if String <code>name</code> is numeric (for example, "3")
     *
     * @return <code>true</code> if name is numeric, otherwise <code>false</code>.
     */
    public boolean isNumericName(String name) {
        try {
            int a = Integer.parseInt(name);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines the behavior when a ROI entry is clicked.
     */
    public void roiValueChanged(ListSelectionEvent e) {

        // DO NOTHING!!  Wait till we are done switching        
        if (!e.getValueIsAdjusting()) {
            return;
        }

        holdUpdate = true;

        int[] indices = roijlist.getSelectedIndices();
        if (indices.length == 0) {
            return;
        }

        // Select ROI in the window
        int index = indices[indices.length - 1];
        if (index < 0) {
            index = 0;
        }

        restore(index);

        // Do spinner stuff
        if (indices.length == 1) {
            ImagePlus imp = getImage();
            if (imp == null) {
                return;
            }
            Roi roi = imp.getRoi();
            resetSpinners(roi);
            updatePlots(true);
        } else {
            disablespinners();
        }

        // Display data for a group of rois.
        if (indices.length > 1) {
            selectedRoisStats();
        } else if (indices.length == 1) {
            String roiName = (String) roiListModel.getElementAt(indices[0]);
            String groupName = (String) groupsMap.get(roiName);
            ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(roiName); // DJ: 12/08/2014

            // This is my attempt at setting the selection of the
            // groupjlist WITHOUT triggering the ListSelectionListener.
            // The only way I have been able to accomplish this is by
            // first removing the listener, setting the selectedvalue,
            // then re-adding the listener. If you know a better way,
            // let me know.
            groupjlist.removeListSelectionListener(groupSelectionListener);
            if (groupName == null) {
                groupName = DEFAULT_GROUP;
            }
            groupjlist.setSelectedValue(groupName, true);
            groupjlist.addListSelectionListener(groupSelectionListener);

            tagjlist.removeListSelectionListener(tagSelectionListener); // DJ: 12/08/2014
            ArrayList<Integer> ArrayListAssociatedTagsIndices = new ArrayList<Integer>();
            if (associatedTags == null || associatedTags.isEmpty()) {
                tagjlist.setSelectedValue(DEFAULT_TAG, true);

            } else {
                for (int ii = 0; ii < associatedTags.size(); ii++) {
                    String tagName = associatedTags.get(ii);
                    ArrayListAssociatedTagsIndices.add(new Integer(tagListModel.indexOf(tagName)));
                    //tagjlist.setSelectedValue(tagName, true);
                }
                int[] AssociatedTagsIndices = new int[ArrayListAssociatedTagsIndices.size()];
                for (int indx = 0; indx < ArrayListAssociatedTagsIndices.size(); indx++) {
                    AssociatedTagsIndices[indx] = ArrayListAssociatedTagsIndices.get(indx).intValue();
                }
                tagjlist.setSelectedIndices(AssociatedTagsIndices);
            }

            tagjlist.addListSelectionListener(tagSelectionListener);
        }

        holdUpdate = false;
    }

    /**
     * If multiple ROIs are selected, statistical information will be displayed in the user interface. (User requestd
     * feature).
     */
    private void selectedRoisStats() {

        // Get the group of selected rois. Ignore Line type rois.
        Roi[] lrois = getSelectedROIs();
        ArrayList<Roi> roilist = new ArrayList<Roi>();
        for (int i = 0; i < lrois.length; i++) {
            if (lrois[i].getType() != Roi.LINE && lrois[i].getType() != Roi.FREELINE && lrois[i].getType() != Roi.POLYLINE) {
                roilist.add(lrois[i]);
            }
        }
        if (roilist.isEmpty()) {
            return;
        }

        // Get last selected window.
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }

        // get the MimsPlus version.
        MimsPlus mp = ui.getImageByName(imp.getTitle());
        if (mp == null) {
            return;
        }
        Roi originalroi = mp.getRoi();
        if (mp.getMimsType() == MimsPlus.HSI_IMAGE || mp.getMimsType() == MimsPlus.RATIO_IMAGE) {
            mp = mp.internalRatio;
        }
        if (mp == null) {
            return;
        }

        // Collect all the pixels within the highlighted rois.
        ArrayList<Double> pixelvals = new ArrayList<Double>();
        for (Roi roi : roilist) {
            mp.setRoi(roi);
            double[] roipixels = mp.getRoiPixels();
            for (double pixel : roipixels) {
                pixelvals.add(pixel);
            }
        }

        // Calculate the mean.
        double mean = 0;
        double stdev;
        int n = pixelvals.size();
        for (int i = 0; i < n; i++) {
            mean += pixelvals.get(i);
        }
        mean /= n;

        // Calculate the standard deviation.
        double sum = 0;
        for (int i = 0; i < n; i++) {
            double v = pixelvals.get(i) - mean;
            sum += v * v;
        }
        stdev = Math.sqrt(sum / (n - 1));

        mp.setRoi(originalroi);
        ui.updateStatus("\t\tA = " + n + ", M = " + IJ.d2s(mean) + ", SD = " + IJ.d2s(stdev));

        holdUpdate = false;
    }

    /**
     * Gets all the selected ROIs.
     *
     * @return ROI array.
     */
    public Roi[] getSelectedROIs() {

        // initialize variables.
        Roi roi;
        Roi[] lrois;

        // get selected indexes.
        int[] roiIndexes = roijlist.getSelectedIndices();
        if (roiIndexes.length == 0) {
            lrois = new Roi[0];
        } else {
            lrois = new ij.gui.Roi[roiIndexes.length];
            for (int i = 0; i < roiIndexes.length; i++) {
                roi = (ij.gui.Roi) getROIs().get(roijlist.getModel().getElementAt(roiIndexes[i]));
                lrois[i] = roi;
            }
        }

        return lrois;
    }

    /**
     * Returns a reference to the MIMS ROI Manager2 or null if it is not open.
     */
    public MimsRoiManager2 getInstance() {
        return (MimsRoiManager2) instance;
    }

    /**
     * Returns the ROI Hashtable.
     *
     * @return ROI Hashtable.
     */
    public Hashtable getROIs() {
        return rois;
    }

    /**
     * Programmatically selects an ROI.
     *
     * @param index the ROI index..
     */
    public void select(int index) {
        int n = roiListModel.size();
        if (index < 0) {
            roijlist.clearSelection();
            return;
        } else if (index > -1 && index < n) {
            roijlist.removeListSelectionListener(roiSelectionListener);
            roijlist.setSelectedIndex(index);
            roijlist.addListSelectionListener(roiSelectionListener);
        }

        String label = roijlist.getSelectedValue().toString();

        // Make sure we have a ROI
        Roi roi = (Roi) rois.get(label);
        if (roi == null) {
            return;
        } else {
            resetSpinners(roi);
        }
    }

    /**
     * Programmatically selects all ROIs in the list.
     */
    public void selectAll() {
        int len = roijlist.getModel().getSize();
        if (len <= 0) {
            return;
        } else {
            roijlist.setSelectionInterval(0, len - 1);
        }
    }

    /**
     * Programmatically adds the ROI with index <code>index</code> to the list of already selected ROIs
     *
     * @param index the ROI index.
     */
    public void selectAdd(int index) {
        int n = roijlist.getModel().getSize();
        if (index < 0) {
            roijlist.clearSelection();
            return;
        } else if (index > -1 && index < n) {
            int[] selected = roijlist.getSelectedIndices();
            int s = selected.length + 1;
            ArrayList newselected = new ArrayList();
            for (int i = 0; i < selected.length; i++) {
                newselected.add(selected[i]);
            }
            if (newselected.contains(index)) {
                newselected.remove((Object) index);
            } else {
                newselected.add(index);
            }
            int[] ns = new int[newselected.size()];
            for (int i = 0; i < ns.length; i++) {
                ns[i] = (Integer) newselected.get(i);
            }
            roijlist.setSelectedIndices(ns);
        }

        selectedRoisStats();

    }

    /**
     * Returns the selection list.
     */
    public JList getList() {
        return roijlist;
    }

    /**
     * Call this method to find what index the specified ROI label has in the jlist.
     *
     * @param label the ROI name.
     * @return the index.
     */
    public int getIndex(String label) {
        int count = roiListModel.getSize();
        for (int i = 0; i <= count - 1; i++) {
            String value = roiListModel.get(i).toString();
            if (value.equals(label)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the HashMap of ROI locations indexed by ROI name.
     *
     * @return a HashMap of ROI locations.
     */
    public HashMap getRoiLocations() {
        return locations;
    }

    /**
     * Gets the ROI location for a given plane.
     *
     * @param label the ROI name.
     * @param plane the plane number.
     * @return the x-y location of the ROI for the given plane.
     */
    public Integer[] getRoiLocation(String label, int plane) {
        int index = ui.getMimsAction().trueIndex(plane);
        ArrayList<Integer[]> xylist = (ArrayList<Integer[]>) locations.get(label);
        if (xylist == null) {
            return null;
        } else {
            return xylist.get(index - 1);
        }
    }

    /**
     * Determines if the ROI with name <code>name</name> is already selected in the manager.
     *
     * @param name the ROI name.
     * @return <code>true</code> if selected, otherwise <code>false</false>
     */
    public boolean isSelected(String name) {
        boolean b = false;
        //Object[] selectednames = roijlist.getSelectedValues();
        List<String> selectednames = roijlist.getSelectedValuesList();

        for (int i = 0; i < selectednames.size(); i++) {
            String sname = selectednames.get(i);
            if (name.equals(sname)) {
                b = true;
                return b;
            }
        }

        return b;
    }

    /**
     * Get the group of roi with roiName.
     *
     * @param roiName name of ROI
     * @return the group name to which it belongs.
     */
    public String getRoiGroup(String roiName) {
        String group = (String) groupsMap.get(roiName);
        return group;
    }

    // DJ: 12/08/2014
    /**
     * Get the Strings array of tags associated to the roiName.
     *
     * @param roiName name of ROI
     * @return the ArrayList of tags names associated with the roiName.
     */
    public ArrayList<String> getRoiTags(String roiName) {
        ArrayList<String> tags = (ArrayList<String>) tagsMap.get(roiName);

        if (tags != null) {
            String[] tagsArray = new String[tags.size()];
            for (int indx = 0; indx < tags.size(); indx++) {
                tagsArray[indx] = tags.get(indx);
                // System.out.println("Original at index = " + indx + "--> " + tagsArray[indx]);
            }
            //System.out.println("--------------------------------");

            Arrays.sort(tagsArray);
            //System.out.println(tagsArray);

            //System.out.println("--------------------------------");
            ArrayList<String> tagsArrayList = new ArrayList<String>();
            for (int indx = 0; indx < tagsArray.length; indx++) {
                tagsArrayList.add(tagsArray[indx]);
                //System.out.println("Original at index = " + indx + "--> " + tagsArray[indx]);
            }

            return tagsArrayList;
        }

        return tags;

    }

    /**
     * Gets the current ROI.
     *
     * @return an ROI.
     */
    public Roi getRoi() {
        int[] indexes = roijlist.getSelectedIndices();

        if (indexes.length == 0) {
            return null;
        }

        String label = roiListModel.get(indexes[0]).toString();
        Roi roi = (Roi) rois.get(label);
        return roi;
    }

    /**
     * Gets the name of the current roi file
     *
     * @return the roi File, <code>null</code> if not open or not yet saved.
     */
    public File getRoiFile() {
        return roiFile;
    }

    // DJ: 12/05/2014
    public JList getRoiList() {
        return roijlist;
    }

    // DJ: 08/25/2014
    // Mainly used in com.nrims.managers.FileManager
    // to restore the state of the MimsRoiManager. 
    /**
     * Adds an Roi to a group without renaming the Roi provided
     *
     * @param roi which will be added to the group.
     * @param group as <code>String</code> of the group where the Roi should be added to.
     * @return <code>true</code> if added, <code>false</code> otherwise.
     */
    public boolean addToGroupWithoutRenaming(Roi roi, String group) {
        boolean val = true;

        roiListModel.addElement(roi.getName());

        Rectangle r = roi.getBounds();

        // Create positions arraylist.
        int stacksize = ui.getMimsAction().getSize();
        ArrayList xypositions = new ArrayList<Integer[]>();
        Integer[] xy = new Integer[2];
        for (int i = 0; i < stacksize; i++) {
            xy = new Integer[]{r.x, r.y};
            xypositions.add(i, xy);
        }
        locations.put(roi.getName(), xypositions);

        // Add roi to list.
        rois.put(roi.getName(), roi);

        //add group to list if not there
        if (!groups.contains(group)) {
            addGroup(group);
        }

        // Assign group.
        groupsMap.put(roi.getName(), group);
        needsToBeSaved = true;
        return val;
    }

    /**
     * Update the spinner to reflect the parameters of the passed ROI.
     *
     * @param roi A roi.
     */
    public void resetSpinners(Roi roi) {

        if (roi == null) {
            return;
        }
        holdUpdate = true;

        // get the type of ROI we are dealing with
        int roiType = roi.getType();

        // Not sure if all ROIs have a width-height value that can be adjusted... test
        if (roiType == Roi.RECTANGLE || roiType == Roi.OVAL) {
            enablespinners();
            java.awt.Rectangle rect = roi.getBounds();
            xPosSpinner.setValue(rect.x);
            yPosSpinner.setValue(rect.y);
            //widthLabel.setText("Width");    // DJ: done thru the GUI
            //heightLabel.setText("Height");  // DJ: done thru the GUI
            widthSpinner.setValue(rect.width);
            heightSpinner.setValue(rect.height);
            //} else if (roiType == Roi.POLYGON || roiType == Roi.FREEROI) {
        } else if (roi.isArea() && !(roiType == Roi.RECTANGLE || roiType == Roi.OVAL)) {
            enablePosSpinners();
            disableSizeSpinners();
            java.awt.Rectangle rect = roi.getBounds();
            xPosSpinner.setValue(rect.x);
            yPosSpinner.setValue(rect.y);
        } else if (roiType == Roi.LINE || roiType == Roi.POLYLINE || roiType == Roi.FREELINE || roiType == Roi.POINT) {
            enablePosSpinners();
            disableSizeSpinners();
            java.awt.Rectangle rect = roi.getBounds();
            xPosSpinner.setValue(rect.x);
            yPosSpinner.setValue(rect.y);
        } else {
            disablespinners();
        }
        holdUpdate = false;
    }

    /**
     * layout method.
     */
    private void enablespinners() {
        xPosSpinner.setEnabled(true);
        yPosSpinner.setEnabled(true);
        widthSpinner.setEnabled(true);
        heightSpinner.setEnabled(true);
    }

    /**
     * layout method.
     */
    private void disablespinners() {
        xPosSpinner.setEnabled(false);
        yPosSpinner.setEnabled(false);
        widthSpinner.setEnabled(false);
        heightSpinner.setEnabled(false);
    }

    /**
     * layout method.
     */
    private void enablePosSpinners() {
        xPosSpinner.setEnabled(true);
        yPosSpinner.setEnabled(true);
    }

    /**
     * layout method.
     */
    private void disableSizeSpinners() {
        widthSpinner.setEnabled(false);
        heightSpinner.setEnabled(false);
    }

    /**
     * Restores the ROI.
     */
    private boolean restore(int index) {
        String label = roiListModel.get(index).toString();
        Roi roi = (Roi) rois.get(label);
        MimsPlus imp;
        try {
            imp = (MimsPlus) getImage();
        } catch (ClassCastException e) {
            imp = ui.getOpenMassImages()[0];
        }
        if (imp == null || roi == null) {
            return false;
        }

        // Set the selected roi to yellow
        //roi.setInstanceColor(java.awt.Color.yellow);   // setInstanceColor is deprecated.  Use setStrokeColor
        roi.setStrokeColor(java.awt.Color.yellow);
        imp.setRoi(roi);
        needsToBeSaved = true;
        return true;
    }

    /**
     * Gets the current image.
     */
    public ImagePlus getImage() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            error("There are no images open.");
            return null;
        } else {
            return imp;
        }
    }

    /**
     * Updates plots representing data for an ROI. Should be called in cases where pixels contained within an ROI
     * changes (for example, location change or size change).
     *
     * @param force forces update.
     */
    void updatePlots(boolean force) {
        MimsPlus imp;
        try {
            imp = (MimsPlus) this.getImage();
        } catch (ClassCastException e) {
            return;
        }
        if (imp == null) {
            return;
        }

        Roi roi = imp.getRoi();
        if (roi == null) {
            return;
        }

        // If this is an HSI or ratio image replace imp with internalRatio which
        // has the correct un-medianized ratio values, then set roi.
        if ((imp.getMimsType() == MimsPlus.HSI_IMAGE) || (imp.getMimsType() == MimsPlus.RATIO_IMAGE)) {
            imp = imp.internalRatio;
            imp.setRoi(roi);
        }

        if ((roi.getType() == Roi.LINE) || (roi.getType() == Roi.POLYLINE) || (roi.getType() == Roi.FREELINE)) {
            ij.gui.ProfilePlot profileP = new ij.gui.ProfilePlot(imp);
            ui.updateLineProfile(profileP.getProfile(), imp.getShortTitle() + " : " + roi.getName(), imp.getProcessor().getLineWidth(), imp);
        } else {
            double[] roipix = imp.getRoiPixels();
            String label = imp.getShortTitle() + " ROI: " + roi.getName();
            ui.getMimsTomography().updateHistogram(roipix, label, force);
        }
    }

    /**
     * Displays an error message. Always returns false.
     *
     * @param msg The message to be displayed.
     * @return always false.
     */
    private boolean error(String msg) {
        new MessageDialog(this, "MIMS ROI Manager", msg);
        Macro.abort();
        return false;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        groupjlist = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        roijlist = new javax.swing.JList();
        jScrollPane3 = new javax.swing.JScrollPane();
        tagjlist = new javax.swing.JList();
        groupsLabel = new javax.swing.JLabel();
        tagsLabel = new javax.swing.JLabel();
        deleteButton = new javax.swing.JButton();
        measureButton = new javax.swing.JButton();
        pixelValuesButton = new javax.swing.JButton();
        moreButton = new javax.swing.JButton();
        xPosSpinner = new javax.swing.JSpinner();
        yPosLabel = new javax.swing.JLabel();
        heightSpinner = new javax.swing.JSpinner();
        xPosLabel = new javax.swing.JLabel();
        yPosSpinner = new javax.swing.JSpinner();
        heightLabel = new javax.swing.JLabel();
        widthSpinner = new javax.swing.JSpinner();
        widthLabel = new javax.swing.JLabel();
        saveButton = new javax.swing.JButton();
        openButton = new javax.swing.JButton();
        roisLabel = new javax.swing.JLabel();
        cbAllPlanes = new javax.swing.JCheckBox();
        cbHideAll = new javax.swing.JCheckBox();
        cbHideLabels = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        groupjlist.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(groupjlist);

        roijlist.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        roijlist.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                roijlistMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(roijlist);

        tagjlist.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane3.setViewportView(tagjlist);

        groupsLabel.setText("Groups");

        tagsLabel.setText("Tags");

        deleteButton.setText("Delete");
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });

        measureButton.setText("Measure");
        measureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                measureButtonActionPerformed(evt);
            }
        });

        pixelValuesButton.setText("Pixel Values");
        pixelValuesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pixelValuesButtonActionPerformed(evt);
            }
        });

        moreButton.setText("More>>");
        moreButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                moreButtonMouseClicked(evt);
            }
        });
        moreButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moreButtonActionPerformed(evt);
            }
        });

        yPosLabel.setText("Y Pos");

        xPosLabel.setText("X Pos");

        heightLabel.setText("Height");

        widthLabel.setText("Width");

        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        openButton.setText("Open");
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });

        roisLabel.setText("Rois");

        cbAllPlanes.setText("Move All");

        cbHideAll.setText("Hide All Rois");

        cbHideLabels.setText("Hide Labels");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(46, 46, 46)
                        .addComponent(groupsLabel)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(tagsLabel)
                        .addGap(68, 68, 68))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addComponent(roisLabel))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(69, 69, 69)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(yPosLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(xPosLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(pixelValuesButton, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
                                    .addComponent(deleteButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(saveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(122, 122, 122)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(widthLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(heightLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbAllPlanes)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(openButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(measureButton, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
                                .addComponent(moreButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(xPosSpinner))
                            .addComponent(cbHideAll)
                            .addComponent(cbHideLabels)
                            .addComponent(heightSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(yPosSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(widthSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(54, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(roisLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(openButton)
                                    .addComponent(saveButton))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(deleteButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(measureButton))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(moreButton)
                                    .addComponent(pixelValuesButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(30, 30, 30)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(xPosSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(xPosLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(yPosLabel)
                                    .addComponent(yPosSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(widthLabel)
                                    .addComponent(widthSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(heightLabel)
                                    .addComponent(heightSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(cbAllPlanes)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbHideAll)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbHideLabels))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tagsLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(groupsLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3)
                            .addComponent(jScrollPane1))))
                .addGap(36, 36, 36))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void roijlistMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_roijlistMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_roijlistMouseClicked

    private void moreButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreButtonActionPerformed

    }//GEN-LAST:event_moreButtonActionPerformed

    private void moreButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_moreButtonMouseClicked
        pm = new JPopupMenu();

        //pm.setBorderPainted(true);
        //pm.setBorder(new javax.swing.border.LineBorder(Color.BLACK));
        JMenuItem duplicate = new JMenuItem("Duplicate");
        duplicate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                duplicate();
            }
        });

        JMenuItem intersection = new JMenuItem("Intersection (and)");
        intersection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                intersection();
            }
        });

        JMenuItem combine = new JMenuItem("Combine (or)");
        combine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                combine();
            }
        });

        JMenuItem complement = new JMenuItem("Complement (not)");
        complement.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                complement();
            }
        });

        JMenuItem exclusivePixels = new JMenuItem("Exclusive pixels");
        exclusivePixels.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exclusiveToRoi();
            }
        });

        JMenuItem split = new JMenuItem("Split");
        split.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                split();
            }
        });

        JMenuItem particles = new JMenuItem("Particles");
        particles.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (partManager == null) {
                    partManager = new ParticlesManager(getInstance());
                }
                partManager.showFrame();
            }
        });

        JMenuItem squares = new JMenuItem("Squares");
        squares.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (squaresManager == null) {
                    squaresManager = new SquaresManager(getInstance());
                }
                squaresManager.showFrame();
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });

        JMenuItem add = new JMenuItem("Add");
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                add();
            }
        });

        JMenuItem reorderAll = new JMenuItem("Reorder all");
        reorderAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reorderAll();
            }
        });

        JMenuItem pushToIJRoiManager = new JMenuItem("Push to IJ Roi Manager");
        pushToIJRoiManager.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pushRoiToIJ();
            }
        });

        JMenuItem pullFromIJRoiManager = new JMenuItem("Pull from IJ Roi Manager");
        pullFromIJRoiManager.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pullRoiFromIJ();
            }
        });

        JMenuItem SplitROI = new JMenuItem("Split ROI");
        SplitROI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                roiSplit();
            }
        });

        pm.add(duplicate);
        pm.add(intersection);
        pm.add(combine);
        pm.add(complement);
        pm.add(exclusivePixels);
        pm.add(split);
        pm.add(particles);
        pm.add(squares);
        pm.add(add);
        pm.add(reorderAll);
        pm.add(pushToIJRoiManager);
        pm.add(pullFromIJRoiManager);
        pm.add(SplitROI);

        pm.show(evt.getComponent(), evt.getX(), evt.getY());;
    }//GEN-LAST:event_moreButtonMouseClicked

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        save();
    }//GEN-LAST:event_saveButtonActionPerformed

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        open(null, false);
    }//GEN-LAST:event_openButtonActionPerformed

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        delete(true);
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void measureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_measureButtonActionPerformed
        measure();
    }//GEN-LAST:event_measureButtonActionPerformed

    private void pixelValuesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pixelValuesButtonActionPerformed
        roiPixelvalues();
    }//GEN-LAST:event_pixelValuesButtonActionPerformed

    /**
     * Generates a table of pixel values for a ROI for the current image and plane.
     */
    private void roiPixelvalues() {
        MimsPlus img = null;
        try {
            img = (MimsPlus) WindowManager.getCurrentImage();
        } catch (Exception E) {
            return;
        }
        roiPixelvalues(img);
    }

    public void roiPixelvalues(MimsPlus img) {
        MimsPlus numImg = new MimsPlus(ui);
        MimsPlus denImg = new MimsPlus(ui);
        // Make sure we have an image.
        if (img == null) {
            IJ.error("No image has been selected.");
            return;
        }

        // make sure we have Rois.
        img.killRoi();
        Roi[] lrois = this.getSelectedROIs();
        if (lrois == null || lrois.length == 0) {
            lrois = getAllROIsInList();
            if (lrois == null || lrois.length == 0) {
                IJ.error("No rois in list.");
                return;
            }
        }

        // Ratio or HSIs pixel data is stored in the internal ratio image.
        if (img.getMimsType() == MimsPlus.HSI_IMAGE || img.getMimsType() == MimsPlus.RATIO_IMAGE) {
            img = img.internalRatio;
            numImg = ui.getMassImage(img.getRatioProps().getNumMassIdx());
            denImg = ui.getMassImage(img.getRatioProps().getDenMassIdx());
        }

        // Collect all the pixels within the highlighted rois.
        ArrayList<Double> values = new ArrayList<Double>();
        ArrayList<Double> numValues = new ArrayList<Double>();
        ArrayList<Double> denValues = new ArrayList<Double>();
        ArrayList<String> lgroups = new ArrayList<String>();
        ArrayList<String> ltags = new ArrayList<String>();  // DJ: 12/08/2014
        ArrayList<String> names = new ArrayList<String>();
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        for (Roi roi : lrois) {
            img.setRoi(roi);
            double[] roipixels = img.getRoiPixels();
            String group = getRoiGroup(roi.getName());
            ArrayList<String> tags = getRoiTags(roi.getName()); // DJ: 12/08/2014
            for (double pixel : roipixels) {
                //TODO, clean. Is this if=else missing braces? Based on indent, yes
                // Based on logic, no? This is not python
                //Quick and dirty fix
                if (img.getMimsType() == MimsPlus.RATIO_IMAGE) {
                    values.add(Double.valueOf(twoDForm.format(pixel)));
                } else {
                    values.add(pixel);
                }
                lgroups.add(group);
                names.add(roi.getName());
                // DJ: 12/08/2014
                String commaSeparatedTagsString = "";
                // was mising null check
                if (tags != null) {
                    for (int j = 0; j < tags.size(); j++) {
                        if (j == tags.size() - 1) {
                            commaSeparatedTagsString += tags.get(j);
                        } else {
                            commaSeparatedTagsString += tags.get(j) + ", ";
                        }
                    }
                }
                ltags.add(commaSeparatedTagsString);

            }
            if (img.getMimsType() == MimsPlus.RATIO_IMAGE || img.getMimsType() == MimsPlus.HSI_IMAGE) {
                roipixels = numImg.getRoiPixels();
                for (double pixel : roipixels) {
                    numValues.add(Double.valueOf(twoDForm.format(pixel)));
                }
                roipixels = denImg.getRoiPixels();
                for (double pixel : roipixels) {
                    denValues.add(Double.valueOf(twoDForm.format(pixel)));
                }
            }
            img.killRoi();
        }

        if (values == null) {
            return;
        }
        if (values.isEmpty()) {
            return;
        }

        // Create table.
        MimsJTable tbl = new MimsJTable(ui);
        MimsPlus[] imgs = new MimsPlus[1];
        imgs[0] = img;
        tbl.setImages(imgs);
        if (img.getMimsType() == MimsPlus.RATIO_IMAGE || img.getMimsType() == MimsPlus.HSI_IMAGE) {
            // DJ: 12/08/2014
            //tbl.createPixelTableNumDen(ui.getImageFilePrefix(), names, lgroups, values, numValues, denValues);
            tbl.createPixelTableNumDen(ui.getImageFilePrefix(), names, lgroups, ltags, values, numValues, denValues);
        } else {
            //tbl.createPixelTable(ui.getImageFilePrefix(), names, lgroups, values);
            tbl.createPixelTable(ui.getImageFilePrefix(), names, lgroups, ltags, values);
        }
        tbl.showFrame();
    }

    /**
     * Opens a file containing saved ROIs.
     *
     * @param path a string containing the absolute path.
     * @param force forces overwrite of current ROIs.
     */
    public void open(String abspath, boolean force) {

        // Does the user want to overwrite current Roi set?
        if (!force && roiListModel.size() > 0) {
            int n = JOptionPane.showConfirmDialog(
                    this,
                    "Opening a roi zip file will delete all current ROIs and ROI groups.\n\t\tContinue?\n",
                    "Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (n == JOptionPane.NO_OPTION) {
                return;
            }
        }

        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        Macro.setOptions(null);
        if (abspath != null) {
            roiFile = new File(abspath);
        } else {

            MimsJFileChooser fc = new MimsJFileChooser(ui);
            MIMSFileFilter mff_rois = new MIMSFileFilter("rois.zip");
            mff_rois.addExtension("zip");
            mff_rois.setDescription("Roi file");
            fc.addChoosableFileFilter(mff_rois);
            fc.setFileFilter(mff_rois);
            fc.setMultiSelectionEnabled(false);
            fc.setPreferredSize(new java.awt.Dimension(650, 500));
            if (fc.showOpenDialog(this) == JFileChooser.CANCEL_OPTION) {
                return;
            }
            roiFile = fc.getSelectedFile();
            if (roiFile == null) {
                return;
            }
            abspath = roiFile.getAbsolutePath();
        }

        if (roiFile.getAbsolutePath().endsWith(".zip")) {
            openZip(abspath);
        } else {
            String name = roiFile.getName();
            ij.io.Opener o = new ij.io.Opener();
            Roi roi = o.openRoi(abspath);
            if (roi != null) {
                if (abspath.endsWith(".roi")) {
                    name = name.substring(0, name.length() - 4);
                }
                name = getUniqueName(name);
                roi.setName(name);
                roiListModel.addElement(name);
                rois.put(name, roi);
            }
            resetRoiLocationsLength();
        }
    }

    /**
     * Opens a zip file containing saved ROIs.
     */
    private void openZip(String path) {

        // Delete rois.
        roiListModel.clear();
        rois = new Hashtable();
        locations = new HashMap<String, ArrayList<Integer[]>>();

        // Delete groups.
        clearGroupListModel();
        groups = new ArrayList<String>();
        groupsMap = new HashMap<String, String>();

        // DJ: 12/08/2014: Delete Tags.
        clearTagListModel();
        tags = new ArrayList<String>();
        tagsMap = new HashMap<String, String>();

        ZipInputStream in = null;
        ByteArrayOutputStream out;
        ObjectInputStream ois;
        int nRois = 0;
        try {
            in = new ZipInputStream(new FileInputStream(path));
            byte[] buf = new byte[1024];
            int len;
            ZipEntry entry = in.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                if (name.endsWith(".roi")) {
                    out = new ByteArrayOutputStream();
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                    byte[] bytes = out.toByteArray();
                    RoiDecoder rd = new RoiDecoder(bytes, name);
                    Roi roi = rd.getRoi();
                    if (roi != null) {
                        int idx = name.length() - 4;
                        name = name.substring(0, idx);
                        name = getUniqueName(name);
                        roiListModel.addElement(name);
                        rois.put(name, roi);
                        getImage().setRoi(roi);
                        nRois++;
                    }
                } else if (name.endsWith(".pos")) {
                    ois = new ObjectInputStream(in);
                    HashMap temp_loc = new HashMap<String, ArrayList<Integer[]>>();
                    try {
                        temp_loc = (HashMap<String, ArrayList<Integer[]>>) ois.readObject();
                        this.locations = temp_loc;
                    } catch (ClassNotFoundException e) {
                        error(e.toString());
                        System.out.println(e.toString());
                    }
                } else if (name.equals(GROUP_FILE_NAME)) {
                    ois = new ObjectInputStream(in);
                    try {
                        this.groups = (ArrayList<String>) ois.readObject();
                        clearGroupListModel();
                        Object[] groupsArray = groups.toArray();
                        String[] groupsStringArray = new String[groupsArray.length];
                        for (int i = 0; i < groupsArray.length; i++) {
                            groupsStringArray[i] = (String) groupsArray[i];
                        }
                        java.util.Arrays.sort(groupsStringArray);
                        for (int i = 0; i < groupsStringArray.length; i++) {
                            groupListModel.addElement(groupsStringArray[i]);
                        }
                    } catch (ClassNotFoundException e) {
                        error(e.toString());
                        System.out.println(e.toString());
                    }
                } else if (name.equals(GROUP_MAP_FILE_NAME)) {
                    ois = new ObjectInputStream(in);
                    try {
                        this.groupsMap = (HashMap<String, String>) ois.readObject();
                    } catch (ClassNotFoundException e) {
                        error(e.toString());
                        System.out.println(e.toString());
                    }
                } //----------------------------------------------------
                // DJ: 12/08/2014
                else if (name.equals(TAG_FILE_NAME)) {
                    ois = new ObjectInputStream(in);
                    try {
                        this.tags = (ArrayList<String>) ois.readObject();
                        clearTagListModel();
                        Object[] tagsArray = tags.toArray();
                        String[] tagsStringArray = new String[tagsArray.length];
                        for (int i = 0; i < tagsArray.length; i++) {
                            tagsStringArray[i] = (String) tagsArray[i];
                        }
                        java.util.Arrays.sort(tagsStringArray);
                        for (int i = 0; i < tagsStringArray.length; i++) {
                            tagListModel.addElement(tagsStringArray[i]);
                        }
                    } catch (ClassNotFoundException e) {
                        error(e.toString());
                        System.out.println(e.toString());
                    }
                } else if (name.equals(TAG_MAP_FILE_NAME)) {
                    ois = new ObjectInputStream(in);
                    try {
                        this.tagsMap = (HashMap<String, ArrayList<String>>) ois.readObject();
                    } catch (ClassNotFoundException e) {
                        error(e.toString());
                        System.out.println(e.toString());
                    }
                }

                //----------------------------------------------------
                entry = in.getNextEntry();
            }
            in.close();
            savedpath = path;
            previouslySaved = true;
            resetTitle();
        } catch (IOException e) {
            error(e.toString());
            System.out.println(e.toString());
        }
        if (nRois == 0) {
            error("This ZIP archive does not appear to contain \".roi\" files");
        }
        sortROIList();
        resetRoiLocationsLength();

        // Do this so that all added Rois get sorted.
        groupjlist.setSelectedValue(DEFAULT_GROUP, true);
        groupValueChanged(null);

        //DJ: 12/08/2014
        tagjlist.setSelectedValue(DEFAULT_TAG, true);
        tagValueChanged(null);

    }

    /**
     * Updates the locations array for all ROIs. To be used when opening a new image.
     */
    public void resetRoiLocationsLength() {

        for (Object key : rois.keySet()) {
            // Get roi location size.
            Roi roi = (Roi) rois.get(key);
            Rectangle rec = roi.getBounds();
            ArrayList<Integer[]> xylist = (ArrayList<Integer[]>) locations.get(key);

            // If no entry, create one.
            if (xylist == null) {
                int stacksize = ui.getMimsAction().getSize();
                xylist = new ArrayList<Integer[]>();
                Integer[] xy = new Integer[2];
                for (int i = 0; i < stacksize; i++) {
                    xy = new Integer[]{rec.x, rec.y};
                    xylist.add(i, xy);
                }
                locations.put(roi.getName(), xylist);
                // If exist but is not proper length, fix.
            } else {
                int locations_size = xylist.size();
                int img_size = ui.getMimsAction().getSize();
                int diff = locations_size - img_size;
                if (diff < 0) {
                    //grow locations arraylist
                    updateRoiLocations(false);
                } else if (diff > 0) {
                    //shrink locations arraylist
                    updateRoiLocations();
                }
            }
        }
    }

    /**
     * Updates the locations array for all ROIs.
     */
    private void updateRoiLocations() {

        // Loop over rios.
        for (Object key : locations.keySet()) {

            // Get roi location size.
            ArrayList<Integer[]> xylist = (ArrayList<Integer[]>) locations.get(key);

            // Current image size
            int size_new = ui.getMimsAction().getSize();

            // Difference in sizes
            int size_orig = xylist.size();
            int size_diff = size_new - size_orig;
            if (size_diff > 0) {
                updateRoiLocations(false);
            }

            // Remove positions
            // size_diff must be negative here so
            // decrimenting instead of incrementing
            for (int i = 0; i > size_diff; i--) {
                xylist.remove(xylist.size() - 1);
            }
        }
        needsToBeSaved = true;
    }

    /**
     * Updates the locations array for all ROIs.
     *
     * @param prepend <code>true</code> if prepending data, otherwise <code>false</code>.
     */
    public void updateRoiLocations(boolean prepend) {

        // Loop over rios.
        for (Object key : locations.keySet()) {

            // Get roi location size.
            ArrayList<Integer[]> xylist = (ArrayList<Integer[]>) locations.get(key);

            // Current image size
            int size_new = ui.getMimsAction().getSize();

            // Difference in sizes
            int size_orig = xylist.size();
            int size_diff = size_new - size_orig;

            // If prepending use FIRST element.
            // If appending use LAST element.
            Integer[] xy = new Integer[2];
            if (prepend) {
                xy = xylist.get(0);
            } else {
                xy = xylist.get(xylist.size() - 1);
            }

            // Create prepend/append array.
            ArrayList<Integer[]> xylist_new = new ArrayList<Integer[]>();
            for (int i = 0; i < size_diff; i++) {
                xylist_new.add(i, xy);
            }

            // Combine lists.
            if (prepend) {
                xylist_new.addAll(xylist);
                locations.put(key, xylist_new);
            } else {
                xylist.addAll(xylist_new);
                locations.put(key, xylist);
            }
        }
        needsToBeSaved = true;
    }

    /**
     * Saves all ROIs.
     *
     * @return <code>true</code> if successful, otherwise <code>false</code>.
     */
    public boolean save() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }

        if (rois.isEmpty()) {
            return error("The selection list is empty.");
        }

        String path = ui.getImageDir();

        Roi[] tmprois = getAllROIs();
        boolean success = saveMultiple(tmprois, path, true);
        if (success) {
            ui.setLastFolder(path);
            ui.setIJDefaultDir(path);
        }

        return success;
    }

    /**
     * Does the actual saving.
     *
     * @param rois a list of ROIs.
     * @param path the absolute path .
     * @param bPrompt prompt the user for the save location.
     *
     * @return <code>true</code> if save successful, otherwise <code>false</code>.
     */
    public boolean saveMultiple(Roi[] rois, String path, boolean bPrompt) {
        Macro.setOptions(null);
        if (bPrompt) {
            String defaultname = ui.getImageFilePrefix();
            defaultname += UI.ROIS_EXTENSION;

            MimsJFileChooser mfc = new MimsJFileChooser(ui);
            mfc.setSelectedFile(new File(defaultname));
            MIMSFileFilter mff_rois = new MIMSFileFilter("rois.zip");
            mff_rois.addExtension("rois.zip");
            mff_rois.setDescription("Roi archive");
            mfc.addChoosableFileFilter(mff_rois);
            mfc.setFileFilter(mff_rois);
            int returnVal = mfc.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                path = mfc.getSelectedFile().getAbsolutePath();
            } else {
                return false;
            }
        }

        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            for (int i = 0; i < rois.length; i++) {
                String label = rois[i].getName();
                if (!label.endsWith(".roi")) {
                    label += ".roi";
                }
                zos.putNextEntry(new ZipEntry(label));
                re.write(rois[i]);
                out.flush();
            }

            //save locations hash
            String label = "locations.pos";
            zos.putNextEntry(new ZipEntry(label));
            ObjectOutputStream obj_out = new ObjectOutputStream(zos);
            obj_out.writeObject(locations);
            obj_out.flush();

            // save groups
            label = GROUP_FILE_NAME;
            zos.putNextEntry(new ZipEntry(label));
            ObjectOutputStream obj_out1 = new ObjectOutputStream(zos);
            obj_out1.writeObject(groups);
            obj_out1.flush();

            // save group mapping
            label = GROUP_MAP_FILE_NAME;
            zos.putNextEntry(new ZipEntry(label));
            ObjectOutputStream obj_out2 = new ObjectOutputStream(zos);
            obj_out2.writeObject(groupsMap);
            obj_out2.flush();

            //--------------------------------------------------------
            // DJ: 12/05/2014.
            // save tags
            label = TAG_FILE_NAME;
            zos.putNextEntry(new ZipEntry(label));
            ObjectOutputStream obj_out3 = new ObjectOutputStream(zos);
            obj_out3.writeObject(tags);
            obj_out3.flush();

            // save tag mapping
            label = TAG_MAP_FILE_NAME;
            zos.putNextEntry(new ZipEntry(label));
            ObjectOutputStream obj_out4 = new ObjectOutputStream(zos);
            obj_out4.writeObject(tagsMap);
            obj_out4.flush();
            //--------------------------------------------------------

            out.close();
            savedpath = path;
            previouslySaved = true;
            resetTitle();
            roiFile = new File(path);
        } catch (IOException e) {
            error("" + e);
            System.out.println(e.toString());
            return false;
        }
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Save", path);
        }
        return true;
    }

    /**
     * Resets the title of the manager.
     */
    private void resetTitle() {
        String name = savedpath.substring(savedpath.lastIndexOf("/") + 1, savedpath.length());
        this.setTitle("MIMS ROI Manager: " + name);
    }

    /**
     * Deletes currently selected ROIs. Deletes all if none selected.
     *
     * @return <code>true</code> if successful, otherwise <code>false</code>.
     */
    public boolean delete(boolean prompt) {
        int count = roiListModel.getSize();
        if (count == 0 && prompt) {
            return error("The list is empty.");
        }
        int index[] = roijlist.getSelectedIndices();
        if (index.length == 0) {
            String msg = "Delete all items on the list?";
            canceled = false;
            if (!IJ.macroRunning() && !macro && prompt) {
                int d = JOptionPane.showConfirmDialog(this, msg, "MIMS ROI Manager", JOptionPane.YES_NO_OPTION);
                if (d == JOptionPane.NO_OPTION) {
                    return false;
                }
            }
            index = getAllIndexes();
            this.previouslySaved = false;
            this.savedpath = "";
            this.resetTitle();
        }

        //remove listener to avoid unneeded events
        roijlist.removeListSelectionListener(roiSelectionListener);
        for (int i = count - 1; i >= 0; i--) {
            boolean delete = false;
            for (int j = 0; j < index.length; j++) {
                if (index[j] == i) {
                    delete = true;
                }
            }
            if (delete) {
                locations.remove(roiListModel.get(i));
                rois.remove(roiListModel.get(i));
                groupsMap.remove(roiListModel.get(i));
                tagsMap.remove(roiListModel.get(i)); // DJ: 12/08/2014
                roiListModel.remove(i);
            }
        }
        //add listener back
        roijlist.addListSelectionListener(roiSelectionListener);

        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Delete");
        }
        ui.updateAllImages();
        needsToBeSaved = true;
        return true;
    }

    /**
     * The action method for the "mesure" button. Takes some basic measurements of the currentImage and generates a
     * table for display.
     */
    private void measure() {

        // initialize table.
        if (table == null) {
            table = new MimsJTable(ui);
        }

        // Get current plane.
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }

        ArrayList planes = new ArrayList<Integer>();
        planes.add(ui.getOpenMassImages()[0].getCurrentSlice());
        table.setPlanes(planes);

        // Get selected stats.
        String[] statnames = ui.getMimsTomography().getStatNames();
        table.setStats(statnames);

        // Get rois.
        Roi[] lrois = getSelectedROIs();
        if (lrois.length >= 1) {
            table.setRois(lrois);
        } else {
            System.out.println("No rois");
            return;
        }

        // Get image.
        MimsPlus[] images = new MimsPlus[1];
        images[0] = (MimsPlus) WindowManager.getCurrentImage();
        table.setImages(images);

        // Generate table.
        table.createRoiTable(true);
        table.showFrame();
    }

    /**
     * The action method for the "duplicate" button. Duplicates the current ROI.
     */
    private void duplicate() {

        // Get the selected rois.
        Roi[] selected_rois = getSelectedROIs();
        if (selected_rois == null || selected_rois.length == 0) {
            error("You must select rois to duplicate.");
            return;
        }

        // Duplicate each roi selected making sure to preserve
        // group and position information.
        for (Roi roi : selected_rois) {

            // Get unique name.
            Roi roi2 = (Roi) roi.clone();
            String name = roi2.getName();
            String label = name;
            label = getUniqueName(label);
            if (label == null) {
                return;
            }
            roiListModel.addElement(label);
            roi2.setName(label);

            // Duplicate positions.
            ArrayList xypositions = new ArrayList<Integer[]>();
            ArrayList xypositions_orig = (ArrayList<Integer[]>) locations.get(name);
            for (int i = 0; i < xypositions_orig.size(); i++) {
                xypositions.add(i, xypositions_orig.get(i));
            }
            locations.put(label, xypositions);

            // Duplicate group assignment.
            String group = (String) groupsMap.get(name);
            if (group != null) {
                groupsMap.put(label, group);
            }

            // DJ: 12/08/2014
            // Duplicate tag assignment.
            ArrayList<String> tags = (ArrayList<String>) tagsMap.get(name);
            if (tags != null) {
                tagsMap.put(label, tags);
            }

            // Add the roi the the hashmap.
            rois.put(label, roi2);
        }
        needsToBeSaved = true;
        return;
    }

    /**
     * pushRoitoIJ: push all ROI's in OpenMIMS Roi manager and imports them to ImageJ Roi manager. Note: nothing will
     * happen if the ImageJ ROI manager has not been opened at least once or there are no ROI's to push
     */
    public void pushRoiToIJ() {
        Roi[] lrois = getAllROIsInList();
        RoiManager manager = RoiManager.getInstance();
        //open RoiManager if it's null
        if (manager == null) {
            IJ.run("ROI Manager...");
            manager = RoiManager.getInstance();
        }
        if (manager != null) {
            for (int i = 0; i < lrois.length; i++) {
                manager.addRoi(lrois[i]);
            }
            if (lrois.length > 0) {
                ui.updateAllImages();
            }
        }
    }

    /**
     * pullRoiFromIJ: pull all ROI's from ImageJ ROI manager and imports them to OpenMIMS Manager. Note: nothing will
     * happen if the ImageJ ROI manager has not been opened at least once or there are no ROI's to pull
     */
    public void pullRoiFromIJ() {
        RoiManager manager = RoiManager.getInstance();
        Roi[] lrois = manager.getSelectedRoisAsArray();
        if (manager != null) {
            for (int i = 0; i < lrois.length; i++) {
                add(lrois[i]);
            }
            if (lrois.length > 0) {
                ui.updateAllImages();
            }
        }
    }

    private void reorderAll() {
        int[] idxs = roijlist.getSelectedIndices();
        for (int i = 0; i < idxs.length; i++) {
            int j = idxs[i];
            roijlist.setSelectedIndex(j);
            int count = 1;
            while (true) {
                boolean success = rename(Integer.toString(count));
                count++;
                if (success) {
                    break;
                }
            }
        }
    }

    /**
     * The action method for the "combine" button. Combines several ROIs into one.
     */
    private void combine() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        int[] indexes = roijlist.getSelectedIndices();
        if (indexes.length == 1) {
            error("More than one item must be selected, or none");
            return;
        }
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        ShapeRoi s1 = null, s2 = null;
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois.get(roiListModel.get(indexes[i]).toString());
            if (roi.isLine() || roi.getType() == Roi.POINT) {
                continue;
            }
            Calibration cal = imp.getCalibration();
            if (cal.xOrigin != 0.0 || cal.yOrigin != 0.0) {
                roi = (Roi) roi.clone();
                Rectangle r = roi.getBounds();
                roi.setLocation(r.x + (int) cal.xOrigin, r.y + (int) cal.yOrigin);
            }
            if (s1 == null) {
                s1 = new ShapeRoi(roi);
                if (s1 == null) {
                    return;
                }
            } else {
                s2 = new ShapeRoi(roi);
                if (s2 == null) {
                    continue;
                }
                if (roi.isArea()) {
                    s1.or(s2);
                }
            }
        }
        if (s1 != null) {
            //imp.setRoi(s1);
            this.add(s1);
        }
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Combine");
        }
    }

    /**
     * The action method for the "intersection" button. Finds the intersection of several ROI's.
     */
    private void intersection() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        int[] indexes = roijlist.getSelectedIndices();

        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }

        //loop over selected rois
        for (int i = 0; i < 1; i++) {
            Roi roi = (Roi) rois.get(roiListModel.get(indexes[i]).toString());

            //skip 0 area rois
            if (roi.isLine() || roi.getType() == Roi.POINT) {
                continue;
            }

            ShapeRoi sroi = new ShapeRoi(roi);
            sroi.setName(roi.getName());

            //loop over all other rois
            boolean unchanged = true;
            for (Object key : rois.keySet()) {
                Roi subtractroi = (Roi) rois.get(key);
                //don't subract self
                if (roi == subtractroi) {
                    continue;
                }

                ShapeRoi subshaperoi = new ShapeRoi(subtractroi);

                //if intersection is empty do nothing
                ShapeRoi intersect = (ShapeRoi) sroi.clone();
                intersect.and(subshaperoi);
                if (intersect.getBounds().width == 0 || intersect.getBounds().height == 0) {
                    continue;
                }

                sroi.and(subshaperoi);
                unchanged = false;
            }

            //skip if 0 area/empty
            Rectangle r = sroi.getBounds();
            System.out.println("Roi: " + sroi.getName() + " w: " + r.width + " h: " + r.height + " unchanged: " + unchanged);
            if (r.width == 0 || r.height == 0) {
                continue;
            }
            if (unchanged) {
                continue;
            }

            //finally add roi
            add(sroi);
        }
    }

    /**
     * The action method for the "complement" button. Finds the complement of several ROI's.
     */
    private void complement() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        int[] indexes = roijlist.getSelectedIndices();

        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        Roi fullroi = new Roi(0, 0, imp.getWidth(), imp.getHeight());
        //loop over selected rois
        ShapeRoi froi = new ShapeRoi(fullroi);
        froi.setName(froi.getName());
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois.get(roiListModel.get(indexes[i]).toString());

            //skip 0 area rois
            if (roi.isLine() || roi.getType() == Roi.POINT) {
                continue;
            }

            ShapeRoi sroi = new ShapeRoi(roi);
            froi.not(sroi);
        }
        Rectangle r = froi.getBounds();
        System.out.println("Roi: " + froi.getName() + " w: " + r.width + " h: " + r.height + " unchanged: ");
        if (r.width != 0 && r.height != 0) {
            add(froi);
        }

    }

    /**
     * roiSplit(): Takes one or more ROI's, combines them, filling the roi and outside with white and black
     * respectively, thresholding, watersheding, then running Analyze Particles, resulting in a split ROI
     */
    private void roiSplit() {
        ImagePlus imp = getImage();
        imp.killRoi();
        ImagePlus workImp = imp.duplicate();
        if (imp == null) {
            return;
        }
        //combine all selected ROI's
        int[] indexes = roijlist.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        ShapeRoi s1 = null, s2 = null;
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois.get(roiListModel.get(indexes[i]).toString());
            if (roi.isLine() || roi.getType() == Roi.POINT) {
                continue;
            }
            Calibration cal = imp.getCalibration();
            if (cal.xOrigin != 0.0 || cal.yOrigin != 0.0) {
                roi = (Roi) roi.clone();
                Rectangle r = roi.getBounds();
                roi.setLocation(r.x + (int) cal.xOrigin, r.y + (int) cal.yOrigin);
            }
            if (s1 == null) {
                s1 = new ShapeRoi(roi);
                if (s1 == null) {
                    return;
                }
            } else {
                s2 = new ShapeRoi(roi);
                if (s2 == null) {
                    continue;
                }
                if (roi.isArea()) {
                    s1.or(s2);
                }
            }
        }

        if (s1 != null) {
            Roi roi = s1.shapeToRoi();
            ImageStack stack = workImp.getStack();
            int stackSize = stack.getSize();
            for (int i = 0; i < stackSize - 1; i++) {
                stack.deleteLastSlice();
            }
            workImp.setStack(stack);
            ij.WindowManager.setTempCurrentImage(workImp);
            ImageProcessor ip = workImp.getProcessor();
            //fill inside and outside
            ip.setValue(255);
            ip.fill(roi);
            ip.setValue(0);
            ip.fillOutside(roi);
            //run IJ effects
            IJ.run("Convert to Mask", " ");
            //IJ.run("Dilate", "");
            //IJ.run("Erode", "");
            IJ.run("Watershed", "");
            IJ.run("Analyze Particles...", "size=0-Infinity circularity=0.00-1.00 show=Nothing clear add slice");
            //resulting roi's from IJ operation creates the ROIs in the IJ Roi manager, so we want to pull them
            pullRoiFromIJNoUpdate();
            //for convenience's sake, we close the IJ ROI window.
            RoiManager manager = RoiManager.getInstance();
            manager.close();
        }
    }

    private void exclusiveToRoi() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        int[] indexes = roijlist.getSelectedIndices();

        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }

        //loop over selected rois
        for (int i = 0; i < indexes.length; i++) {
            Roi roi = (Roi) rois.get(roiListModel.get(indexes[i]).toString());

            //skip 0 area rois
            if (roi.isLine() || roi.getType() == Roi.POINT) {
                continue;
            }

            ShapeRoi sroi = new ShapeRoi(roi);
            sroi.setName(roi.getName());

            //loop over all other rois
            boolean unchanged = true;
            for (Object key : rois.keySet()) {
                Roi subtractroi = (Roi) rois.get(key);
                //don't subract self
                if (roi == subtractroi) {
                    continue;
                }

                ShapeRoi subshaperoi = new ShapeRoi(subtractroi);

                //if intersection is empty do nothing
                ShapeRoi intersect = (ShapeRoi) sroi.clone();
                intersect.and(subshaperoi);
                if (intersect.getBounds().width == 0 || intersect.getBounds().height == 0) {
                    continue;
                }

                sroi.not(subshaperoi);
                unchanged = false;
            }

            //skip if 0 area/empty
            Rectangle r = sroi.getBounds();
            System.out.println("Roi: " + sroi.getName() + " w: " + r.width + " h: " + r.height + " unchanged: " + unchanged);
            if (r.width == 0 || r.height == 0) {
                continue;
            }
            if (unchanged) {
                continue;
            }

            //finally add roi
            add(sroi);
        }

    }

    /**
     * The action method for the "split" button. Must be a ROI of composite type.
     */
    private void split() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }
        Roi roi = imp.getRoi();
        if (roi == null || roi.getType() != Roi.COMPOSITE) {
            error("Image with composite selection required");
            return;
        }
        Roi[] lrois = ((ShapeRoi) roi).getRois();
        for (int i = 0; i < lrois.length; i++) {
            imp.setRoi(lrois[i]);
            add();
        }
    }

    /**
     * Adds the currently drawn ROI to the manager.
     */
    public boolean add() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        Roi roi = imp.getRoi();
        if (roi == null) {
            error("The active image does not have a selection.");
            return false;
        }

        String label = "";
        if (rois.isEmpty()) {
            label += 1;
        } else {
            String maxname = "0";
            Roi maxroi = getMaxNumericRoi();
            if (maxroi != null) {
                maxname = maxroi.getName();
            }
            int m = Integer.parseInt(maxname);
            m = m + 1;
            label += m;
        }

        roiListModel.addElement(label);
        roi.setName(label);
        Calibration cal = imp.getCalibration();
        Rectangle r = roi.getBounds();
        if (cal.xOrigin != 0.0 || cal.yOrigin != 0.0) {
            roi.setLocation(r.x - (int) cal.xOrigin, r.y - (int) cal.yOrigin);
        }

        // Create positions arraylist.
        int stacksize = ui.getMimsAction().getSize();
        ArrayList xypositions = new ArrayList<Integer[]>();
        Integer[] xy = new Integer[2];
        for (int i = 0; i < stacksize; i++) {
            xy = new Integer[]{r.x, r.y};
            xypositions.add(i, xy);
        }
        locations.put(label, xypositions);

        // Add roi to list.
        rois.put(label, roi);

        // Assign group.
        int[] indices = groupjlist.getSelectedIndices();
        if (indices.length > 0) {
            String group = (String) groupListModel.getElementAt(indices[0]);
            if (indices.length == 1 && !group.equals(DEFAULT_GROUP)) {
                groupsMap.put(label, group);
            }
        }

        // DJ: 12/08/2014
        // Assign tags.
        int[] tagsIndices = tagjlist.getSelectedIndices();
        if (tagsIndices.length > 0) {
            String tag = (String) tagListModel.getElementAt(tagsIndices[0]);
            if (tagsIndices.length == 1 && !tag.equals(DEFAULT_TAG)) {
                ArrayList<String> tagss = new ArrayList<String>();
                tagss.add(tag);
                tagsMap.put(label, tagss);
            }
        }

        needsToBeSaved = true;
        return true;
    }

    /**
     * Add a ROI to the manager.
     */
    public boolean add(Roi roi) {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        if (roi == null) {
            return false;
        }

        String label = "";
        Roi maxRoi = getMaxNumericRoi();
        if (rois.isEmpty() || maxRoi == null) {
            label += 1;
        } else {
            String maxname = maxRoi.getName();
            int m = Integer.parseInt(maxname);
            m = m + 1;
            label += m;
        }

        roiListModel.addElement(label);
        roi.setName(label);
        Calibration cal = imp.getCalibration();
        Rectangle r = roi.getBounds();
        if (cal.xOrigin != 0.0 || cal.yOrigin != 0.0) {
            roi.setLocation(r.x - (int) cal.xOrigin, r.y - (int) cal.yOrigin);
        }

        // Create positions arraylist.
        int stacksize = ui.getMimsAction().getSize();
        ArrayList xypositions = new ArrayList<Integer[]>();
        Integer[] xy = new Integer[2];
        for (int i = 0; i < stacksize; i++) {
            xy = new Integer[]{r.x, r.y};
            xypositions.add(i, xy);
        }
        locations.put(label, xypositions);

        // Add roi to list.
        rois.put(label, roi);
        needsToBeSaved = true;
        return true;
    }

    // DJ: 08/25/2014
    // Mainly used in com.nrims.managers.FileManager
    // to restore the state of the MimsRoiManager. 
    /**
     * Adds a Roi to the manager without renaming the provided Roi
     *
     * @param roi
     * @return <code>true</code> if added, <code>false</code> otherwise.
     */
    public boolean addWithoutRenaming(Roi roi) {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        if (roi == null) {
            return false;
        }
        roiListModel.addElement(roi.getName());

        Calibration cal = imp.getCalibration();
        Rectangle r = roi.getBounds();
        if (cal.xOrigin != 0.0 || cal.yOrigin != 0.0) {
            roi.setLocation(r.x - (int) cal.xOrigin, r.y - (int) cal.yOrigin);
        }

        // Create positions arraylist.
        int stacksize = ui.getMimsAction().getSize();
        ArrayList xypositions = new ArrayList<Integer[]>();
        Integer[] xy = new Integer[2];
        for (int i = 0; i < stacksize; i++) {
            xy = new Integer[]{r.x, r.y};
            xypositions.add(i, xy);
        }
        locations.put(roi.getName(), xypositions);

        // Add roi to list.
        rois.put(roi.getName(), roi);
        needsToBeSaved = true;
        return true;
    }

    /**
     * The the indices for all ROIS. (Not sure why this is needed, leaving for now).
     */
    private int[] getAllIndexes() {
        int count = roiListModel.size();
        int[] indexes = new int[count];
        for (int i = 0; i < count; i++) {
            indexes[i] = i;
        }
        return indexes;
    }

    /**
     * pullRoiFromIJNoUpdate: pull all ROI's from ImageJ ROI manager and imports them to OpenMIMS Manager, without
     * updating images. Note: used specifically for roiSplit due to weird issue with redrawing.
     */
    public void pullRoiFromIJNoUpdate() {
        RoiManager manager = RoiManager.getInstance();
        Roi[] lrois = manager.getSelectedRoisAsArray();
        if (manager != null) {
            for (int i = 0; i < lrois.length; i++) {
                add(lrois[i]);
            }
        }
    }

    /**
     * Finds particles like IJ's ParticleAnalyzer (which has been exhibiting odd behavior) and adds them to
     * MimsRoiManager. Uses com.nrims.segmentation.NrimsParticleAnalyzer.
     *
     * @param roi Bounding Roi
     * @param img Image to work on
     * @param params min/max threshold and size
     */
    public Roi[] roiThreshold(Roi roi, MimsPlus img, double[] params) {
        //check param size
        if (params.length != 5) {
            return null;
        }
        double mint = params[0];
        double maxt = params[1];
        double mins = params[2];
        double maxs = params[3];
        //at the moment diag is unused because of switch to NrimsParticleAnalyzer
        int diag = (int) params[4];

        img.setRoi(roi, true);
        ImageProcessor imgproc = img.getProcessor();

        int imgwidth = img.getWidth();
        int imgheight = img.getHeight();
        float[][] pix = new float[imgwidth][imgheight];

        //get pixel values
        for (int i = 0; i < imgwidth; i++) {
            for (int j = 0; j < imgheight; j++) {
                pix[i][j] = (float) imgproc.getPixelValue(i, j);
            }
        }

        //apply roi "mask"
        //need to do this regardless of cropping
        //for non-rectangular rois
        for (int j = 0; j < imgheight; j++) {
            for (int i = 0; i < imgwidth; i++) {
                if (!roi.contains(i, j)) {
                    pix[i][j] = Float.NaN;
                }
            }
        }

        //threshold image
        FloatProcessor proc = new FloatProcessor(pix);

        //crop
        Roi bounds = new ij.gui.Roi(roi.getBounds());
        //need ofsets because of crop
        int xoffset = roi.getBounds().x;
        int yoffset = roi.getBounds().y;
        proc.setRoi(bounds);
        proc = (FloatProcessor) proc.crop();

        ImagePlus temp_img = new ImagePlus("temp_img", proc);
        //temp_img.show();

        ij.process.ByteProcessor byteproc = new ij.process.ByteProcessor(temp_img.getImage());
        //do explicit setting of bytes since thresholding wasn't working
        float[] temppix = (float[]) proc.getPixels();
        byte[] bytepix = new byte[temppix.length];
        for (int i = 0; i < temppix.length; i++) {
            if ((mint <= temppix[i]) && (temppix[i] <= maxt)) {
                bytepix[i] = (byte) 255;
            } else {
                bytepix[i] = (byte) 0;
            }
        }
        byteproc.setPixels(bytepix);

        ImagePlus byte_img = new ImagePlus("bin", byteproc);
        //byte_img.show();

        //unsure about arguments...
        //this is probably bad....
        int s = new Long(new Double(mins).longValue()).intValue();
        com.nrims.segmentation.NrimsParticleAnalyzer nPA = new com.nrims.segmentation.NrimsParticleAnalyzer(byte_img, 255, 0, s);
        ArrayList<Roi> rs = nPA.analyze();

        //Throw out rois that are too large.
        //This is apparently the proper way to modify a collection
        //while iterating over it.  The loop below throws java.util.ConcurrentModificationException
        for (Iterator<Roi> it = rs.iterator(); it.hasNext();) {
            Roi r = it.next();
            temp_img.setRoi(r);
            double a = temp_img.getProcessor().getStatistics().area;
            //System.out.println("roi: " + rs.get(i) + " area: " + a);
            if (a > maxs) {
                it.remove();
            }
        }

        //temp_img.close();
        for (Roi r : rs) {
            int xpos = r.getBounds().x;
            int ypos = r.getBounds().y;
            r.setLocation(xoffset + xpos, yoffset + ypos);
            //r.setLocation(s, s);
        }

        Roi[] returnarr = new Roi[rs.size()];
        return rs.toArray(returnarr);

        //old code using IJ particle analyzer
        //This DOES NOT WORK.  Rois are missed and not using INCLUDE_HOLES
        //seems to do nothing.  Left as a warning for those that come later...
        /*
        //generate rois
        int options = ParticleAnalyzer.ADD_TO_MANAGER;
        if(diag!=0) {
            options = options | diag;
        }
        options = options | ParticleAnalyzer.INCLUDE_HOLES;
        ParticleAnalyzer pa = new ParticleAnalyzer(options, 0, Analyzer.getResultsTable(), mins, maxs);
        pa.analyze(temp_img);
        RoiManager rm = (RoiManager) WindowManager.getFrame("ROI Manager");
        if (rm == null) {
            return;
        }
        //add to mims roi manager with needed shift in location
        for (Roi calcroi : rm.getRoisAsArray()) {
            ui.getRoiManager().add(calcroi);
        }
        rm.close();
        temp_img.close();
         */
    }

    /**
     * Iteravley calls roiThreshold for each Roi in rois
     *
     * @param rois Array of rois to be passed to roiThreshold
     * @param img Working image
     * @param params min/max threshold and size
     */
    public Roi[] roiThreshold(Roi[] rois, MimsPlus img, double[] params) {
        ArrayList<Roi> returnlist = new ArrayList<Roi>();
        if (rois.length == 0) {
            Roi[] temparr = roiThreshold(new ij.gui.Roi(0, 0, img.getWidth(), img.getHeight()), img, params);
            returnlist.addAll(Arrays.asList(temparr));
        } else {
            for (int i = 0; i < rois.length; i++) {
                Roi[] temparr = roiThreshold(rois[i], img, params);
                returnlist.addAll(Arrays.asList(temparr));
            }
        }
        Roi[] returnarr = new Roi[returnlist.size()];
        return returnlist.toArray(returnarr);
    }

    //Various methods to deal with transitionsing to
    //simple numeric roi names
    //Compares roi's by name for sorting.
    //Roi's with int-castable names are compared as ints
    //Roi's with non-int-castable names are ??????????????????????????
    public abstract class RoiNameComparator implements Comparator<Roi> {

        public int compare(Roi roia, Roi roib) {

            //this should never actually happen
            //since roi names are forced to be unique
            if (roia.getName().equals(roib.getName())) {
                return 0;
            }
            int a = 0, b = 0;
            try {
                a = Integer.parseInt(roia.getName());
                b = Integer.parseInt(roib.getName());
            } catch (Exception e) {
                return 0;

            }

            if (a < b) {
                return -1;
            }
            if (a > b) {
                return 1;
            }
            return 0;
        }
    }

    /**
     * Returns the ROI with the maximum name value.
     *
     * @return the ROI with the maximum integer label.
     */
    public Roi getMaxNumericRoi() {
        Roi[] tmprois = getNumericRoisSorted();
        if (tmprois.length == 0) {
            return null;
        } else {
            return tmprois[tmprois.length - 1];
        }
    }

    /**
     * Gets all ROIs regardless if they're selected or not.
     *
     * @return ROI array.
     */
    public Roi[] getAllROIs() {
        Object[] ob = rois.values().toArray();
        Roi[] r = new Roi[ob.length];
        for (int i = 0; i < ob.length; i++) {
            r[i] = (Roi) ob[i];
        }
        return r;
    }

    /**
     * Tests if the ROI has a numeric name (for example, "3")
     *
     * @return <code>true</code> if name is numeric, otherwise <code>false</code>.
     */
    public boolean hasNumericName(Roi r) {
        try {
            int a = Integer.parseInt(r.getName());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns an array of ROIs that have numeric names.
     *
     * @return the ROI array with numeric names.
     */
    public Roi[] getNumericRois() {
        Roi[] tmprois = getAllROIs();
        ArrayList<Roi> numrois = new ArrayList<Roi>();

        for (int i = 0; i < tmprois.length; i++) {
            if (hasNumericName(tmprois[i])) {
                numrois.add(tmprois[i]);
            }
        }

        Roi[] returnrois = new Roi[numrois.size()];

        for (int i = 0; i < returnrois.length; i++) {
            returnrois[i] = (Roi) numrois.get(i);
        }

        return returnrois;
    }

    /**
     * Returns an array of ROIs that have numeric names in sorted order.
     *
     * @return the ROI array with numeric names, sorted.
     */
    public Roi[] getNumericRoisSorted() {
        Roi[] returnrois = getNumericRois();

        Comparator<Roi> byName = new RoiNameComparator() {
        };
        java.util.Arrays.sort(returnrois, byName);

        return returnrois;
    }

    /**
     * Sets an array of Rois to the manager.
     */
    public boolean add(Roi[] roiarr) {
        boolean val = true;
        for (int i = 0; i < roiarr.length; i++) {
            val = val && this.add(roiarr[i]);
        }
        return val;
    }

    public boolean addToGroup(Roi[] roiarr, String group) {
        boolean val = true;
        for (int i = 0; i < roiarr.length; i++) {
            val = val && this.addToGroup(roiarr[i], group);
        }
        return val;
    }

    public boolean addToGroup(Roi roi, String group) {
        boolean val = true;

        String label = "";
        if (rois.isEmpty()) {
            label += 1;
        } else {
            String maxname = getMaxNumericRoi().getName();
            int m = Integer.parseInt(maxname);
            m = m + 1;
            label += m;
        }

        roiListModel.addElement(label);
        roi.setName(label);

        Rectangle r = roi.getBounds();

        // Create positions arraylist.
        int stacksize = ui.getMimsAction().getSize();
        ArrayList xypositions = new ArrayList<Integer[]>();
        Integer[] xy = new Integer[2];
        for (int i = 0; i < stacksize; i++) {
            xy = new Integer[]{r.x, r.y};
            xypositions.add(i, xy);
        }
        locations.put(label, xypositions);

        // Add roi to list.
        rois.put(label, roi);

        //add group to list if not there
        if (!groups.contains(group)) {
            addGroup(group);
        }

        // Assign group.
        groupsMap.put(label, group);
        needsToBeSaved = true;
        return val;
    }

    // DJ: 12/08/2014
    public boolean addToTag(Roi[] roiarr, String tag) {
        boolean val = true;
        for (int i = 0; i < roiarr.length; i++) {
            val = val && this.addToTag(roiarr[i], tag);
        }
        return val;
    }

    // DJ: 12/08/2014
    public boolean addToTag(Roi roi, String tag) {
        boolean val = true;

        String label = "";
        if (rois.isEmpty()) {
            label += 1;
        } else {
            String maxname = getMaxNumericRoi().getName();
            int m = Integer.parseInt(maxname);
            m = m + 1;
            label += m;
        }

        roiListModel.addElement(label);
        roi.setName(label);

        Rectangle r = roi.getBounds();

        /*
        // Create positions arraylist.
        int stacksize = ui.getMimsAction().getSize();
        ArrayList xypositions = new ArrayList<Integer[]>();
        Integer[] xy = new Integer[2];
        for (int i = 0; i < stacksize; i++) {
            xy = new Integer[]{r.x, r.y};
            xypositions.add(i, xy);
        }
        locations.put(label, xypositions);

        // Add roi to list.
        rois.put(label, roi);
         */
        //add tag to list if not there
        if (!tags.contains(tag)) {
            addTag(tag);
        }

        // Assign Tag.
        ArrayList<String> associatedTags = (ArrayList<String>) tagsMap.get(label);
        if (associatedTags == null) {
            associatedTags = new ArrayList<String>();
        }

        associatedTags.add(tag);
        groupsMap.put(label, associatedTags);

        needsToBeSaved = true;
        return val;
    }

    /**
     * Adds a new group.
     *
     * @param s the name of the group.
     * @return <code>true</code> if successful, otherwise <code>false</code>.
     */
    public boolean addGroup(String s) {

        if (s == null) {
            return false;
        }

        if (s.equals(DEFAULT_GROUP)) {
            return false;
        }

        s = s.trim();
        if (s.equals("")) {
            return false;
        }

        if (groups.contains(s)) {
            return false;
        } else {
            groups.add(s);
            clearGroupListModel();
            Object[] groupsArray = groups.toArray();
            String[] groupsStringArray = new String[groupsArray.length];
            for (int i = 0; i < groupsArray.length; i++) {
                groupsStringArray[i] = (String) groupsArray[i];
            }
            java.util.Arrays.sort(groupsStringArray);
            for (int i = 0; i < groupsStringArray.length; i++) {
                groupListModel.addElement(groupsStringArray[i]);
            }
        }
        needsToBeSaved = true;
        return true;
    }

    /**
     * Adds a new tag.
     *
     * @param s the name of the tag.
     * @return <code>true</code> if successful, otherwise <code>false</code>.
     */
    public boolean addTag(String t) {

        if (t == null) {
            return false;
        }

        if (t.equals(DEFAULT_TAG)) {
            return false;
        }

        t = t.trim();
        if (t.equals("")) {
            return false;
        }

        if (tags.contains(t)) {
            return false;
        } else {
            tags.add(t);
            clearTagListModel();
            Object[] tagssArray = tags.toArray();
            String[] tagsStringArray = new String[tagssArray.length];
            for (int i = 0; i < tagssArray.length; i++) {
                tagsStringArray[i] = (String) tagssArray[i];
            }
            java.util.Arrays.sort(tagsStringArray);
            for (int i = 0; i < tagsStringArray.length; i++) {
                tagListModel.addElement(tagsStringArray[i]);
            }
        }
        needsToBeSaved = true;
        return true;
    }

    /**
     * Use this method instead of groupListModel.clear() because we do not want the "..." listing to dissapear, because
     * it represents all Rois.
     */
    private void clearGroupListModel() {
        for (int i = groupListModel.getSize() - 1; i >= 0; i--) {
            String group = (String) groupListModel.get(i);
            if (!group.equals(DEFAULT_GROUP)) {
                groupListModel.remove(i);
            }
        }
    }

    // DJ: 12/08/2014
    /**
     * Use this method instead of tagListModel.clear() because we do not want the "..." listing to dissapear, because it
     * represents all Rois.
     */
    private void clearTagListModel() {
        for (int i = tagListModel.getSize() - 1; i >= 0; i--) {
            String tag = (String) tagListModel.get(i);
            if (!tag.equals(DEFAULT_TAG)) {
                tagListModel.remove(i);
            }
        }
    }

    /**
     * Returns a unique name for an ROI. If the name already exists in the list of ROIs, the method will append a "-1"
     * to the name so that it is unique.
     *
     * @return a unique name.
     */
    public String getUniqueName(String name) {
        String name2 = name;
        int n = 1;
        Roi roi2 = (Roi) rois.get(name2);
        while (roi2 != null) {
            roi2 = (Roi) rois.get(name2);
            if (roi2 != null) {
                int lastDash = name2.lastIndexOf("-");
                if (lastDash != -1 && name2.length() - lastDash < 5) {
                    name2 = name2.substring(0, lastDash);
                }
                name2 = name2 + "-" + n;
                n++;
            }
            roi2 = (Roi) rois.get(name2);
        }
        return name2;
    }

    /**
     * Gets all the selected ROIs.
     *
     * @return ROI array.
     */
    public Roi[] getAllROIsInList() {

        // Loop over all Rois in list.
        int listLength = roijlist.getModel().getSize();
        Roi[] lrois = new Roi[listLength];
        for (int i = 0; i < listLength; i++) {
            lrois[i] = ((Roi) getROIs().get(roijlist.getModel().getElementAt(i)));
        }

        return lrois;
    }

    /**
     * Documentation Required.
     */
    public Roi[] roiSquares(Roi roi, MimsPlus img, double[] params) {
        //check param size
        if (params.length != 3) {
            return null;
        }
        //params = size, number, overlap
        int size = (int) Math.round(params[0]);
        int num = (int) Math.round(params[1]);
        boolean allowoverlap = (params[2] == 1.0);

        img.setRoi(roi, true);

        //roi bounding box
        int x = roi.getBounds().x;
        int y = roi.getBounds().y;
        int width = roi.getBounds().width;
        int height = roi.getBounds().height;
        float[][] pix = new float[width][height];

        //get pixel values
        //using x,y offsets
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pix[i][j] = (float) img.getProcessor().getPixelValue(i + x, j + y);
            }
        }

        //apply "mask" which is a somewhat arbbitrary value
        // of -1.0E35, very negative but "far" from Float.min_value
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (!roi.contains(i + x, j + y)) {
                    //pix[i][j] = 0;
                    pix[i][j] = (float) -1.0E35;
                }

            }
        }

        //generate temp image
        FloatProcessor proc = new FloatProcessor(pix);
        ImagePlus temp_img = new ImagePlus("temp_img", proc);
        //temp_img.show();

        //generate rois keyed to mean
        Hashtable<Double, ArrayList<Roi>> roihash = new Hashtable<Double, ArrayList<Roi>>();

        int nrois = 0;
        int rejected = 0;
        for (int ix = 0; ix <= width - size; ix++) {
            for (int iy = 0; iy <= height - size; iy++) {
                Roi troi = new Roi(ix, iy, size, size);
                temp_img.setRoi(troi);
                nrois++;

                //throw out any roi containing pixels outside
                //the original roi by checking if min is "close" to mask value
                double min = temp_img.getStatistics().min;
                if (min < -1.0E30) {
                    //System.out.println("min == "+min);
                    rejected++;
                    continue;
                }

                //get mean value and add to hash
                double mean = temp_img.getStatistics().mean;
                //System.out.println("mean = "+mean);

                //exclude rois outside main roi
                if (roihash.containsKey(mean)) {
                    roihash.get(mean).add(troi);
                } else {
                    ArrayList<Roi> ar = new ArrayList<Roi>();
                    ar.add(troi);
                    roihash.put(mean, ar);
                }
            }
        }

        System.out.println("nrois = " + nrois + " | rejected = " + rejected);

        Object[] means = roihash.keySet().toArray();
        java.util.Arrays.sort(means);

        ArrayList<Roi> keep = new ArrayList<Roi>();
        //find highest mean rois
        if (allowoverlap) {
            int meanindex = means.length - 1;
            while (keep.size() < num) {
                //if((Double)means[meanindex]<0) continue;
                ArrayList fromhash = roihash.get(means[meanindex]);
                for (Object r : fromhash) {
                    keep.add((Roi) r);
                    if (keep.size() == num) {
                        break;
                    }
                }
                meanindex--;
                if (meanindex < 0) {
                    break;
                }
            }

        } else {
            //find highest mean rois checking for overlap
            if (means.length == 0) {
                System.out.println("No rois found for roi: " + roi.getName());
                return null;
            }

            int meanindex = means.length - 1;
            Roi lastadded = roihash.get(means[meanindex]).get(0);
            keep.add(lastadded);

            while (keep.size() < num) {
                ArrayList fromhash = roihash.get(means[meanindex]);
                for (Object r : fromhash) {
                    Roi[] keeparray = new Roi[keep.size()];
                    keeparray = keep.toArray(keeparray);

                    if (!roiOverlap((Roi) r, keeparray)) {
                        keep.add((Roi) r);
                        lastadded = (Roi) r;
                    }

                    if (keep.size() == num) {
                        break;
                    }
                }
                meanindex--;
                if (meanindex < 0) {
                    break;
                }
            }

        }

        //cleanup
        temp_img.close();
        temp_img = null;
        proc = null;
        means = null;
        roihash = null;
        pix = null;

        Roi[] rrois = new Roi[keep.size()];
        keep.toArray(rrois);

        //add back x,y offset
        for (int i = 0; i < rrois.length; i++) {
            int rx = rrois[i].getBounds().x;
            int ry = rrois[i].getBounds().y;
            rrois[i].setLocation(x + rx, y + ry);
        }
        keep = null;
        return rrois;

    }

    /**
     * Documentation Required.
     */
    public Roi[] roiSquares(Roi[] rois, MimsPlus img, double[] params) {
        ArrayList<Roi> temprois = new ArrayList<Roi>();
        for (int i = 0; i < rois.length; i++) {
            Roi[] r = roiSquares(rois[i], img, params);
            if (r == null) {
                System.out.println("roiSquares returned null.");
                continue;
            }
            for (int j = 0; j < r.length; j++) {
                temprois.add(r[j]);
            }
        }

        Roi[] rrois = new Roi[temprois.size()];
        temprois.toArray(rrois);
        temprois = null;

        System.out.println("input length: " + rois.length + " output length: " + rrois.length);

        return rrois;
    }

    /**
     * Checks for Roi overlap. Returns true iff and pixel in Roi r is also in Roi q
     *
     * @param r Roi
     * @param q Roi
     * @return boolean
     */
    public boolean roiOverlap(Roi r, Roi q) {
        boolean overlap = false;
        if ((r.getType() == Roi.RECTANGLE) && (q.getType() == Roi.RECTANGLE)) {
            overlap = r.getBounds().intersects(q.getBounds());
        }
        if (!(r.getType() == Roi.RECTANGLE) || !(q.getType() == Roi.RECTANGLE)) {
            int x = r.getBounds().x;
            int y = r.getBounds().y;
            int w = r.getBounds().width;
            int h = r.getBounds().height;

            for (int ix = x; ix <= x + w; ix++) {
                for (int iy = y; iy <= y + h; iy++) {
                    overlap = overlap || q.contains(ix, iy);
                    if (overlap) {
                        break;
                    }
                }
                if (overlap) {
                    break;
                }
            }
        }

        return overlap;
    }

    /**
     * Checks for Roi overlap. Returns true iff an pixel in Roi r is also in any roi in rois[]
     *
     * @param r Roi
     * @param rois Roi[]
     * @return boolean
     */
    public boolean roiOverlap(Roi r, Roi[] rois) {
        boolean overlap = false;
        for (int i = 0; i < rois.length; i++) {
            overlap = (overlap || roiOverlap(r, rois[i]));
        }
        return overlap;
    }

    /**
     * layout method.
     */
    private void addPopupItem(String s) {
        JMenuItem mi = new JMenuItem(s);
        mi.addActionListener(this);
        pm.add(mi);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MimsRoiManager2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MimsRoiManager2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MimsRoiManager2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MimsRoiManager2.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MimsRoiManager2(ui).setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox cbAllPlanes;
    private javax.swing.JCheckBox cbHideAll;
    private javax.swing.JCheckBox cbHideLabels;
    private javax.swing.JButton deleteButton;
    private javax.swing.JList groupjlist;
    private javax.swing.JLabel groupsLabel;
    private javax.swing.JLabel heightLabel;
    private javax.swing.JSpinner heightSpinner;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JButton measureButton;
    private javax.swing.JButton moreButton;
    private javax.swing.JButton openButton;
    private javax.swing.JButton pixelValuesButton;
    private javax.swing.JList roijlist;
    private javax.swing.JLabel roisLabel;
    private javax.swing.JButton saveButton;
    private javax.swing.JList tagjlist;
    private javax.swing.JLabel tagsLabel;
    private javax.swing.JLabel widthLabel;
    private javax.swing.JSpinner widthSpinner;
    private javax.swing.JLabel xPosLabel;
    private javax.swing.JSpinner xPosSpinner;
    private javax.swing.JLabel yPosLabel;
    private javax.swing.JSpinner yPosSpinner;
    // End of variables declaration//GEN-END:variables

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * This class controls how ROIs are listed and displayed in the jlist. It uses html so that an index is displayed in
     * light grey followed by the name of the ROI in black. Prefereably the jlist would have the index built into it but
     * I can not find any implementation of the jlist that does that.
     */
    class ComboBoxRenderer extends JLabel implements ListCellRenderer {

        public ComboBoxRenderer() {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(CENTER);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            // Prepend label of Roi on the image into the name in the jlist.
            String label = (String) value;
            setText(label);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    public class ParticlesManager extends com.nrims.PlugInJFrame implements ActionListener {

        Frame instance;
        MimsPlus workingimage;
        MimsRoiManager2 rm;
        JTextField threshMinField = new JTextField(10);
        JTextField threshMaxField = new JTextField(10);
        JTextField sizeMinField = new JTextField(10);
        JTextField sizeMaxField = new JTextField(10);
        JCheckBox useFilteredCheckbox = new JCheckBox("Use filtered image");
        MimsRoiManager2.GroupAssignmentPanel groupAssignmentPanel;
        JButton cancelButton;
        JButton okButton;

        public ParticlesManager(MimsRoiManager2 rm) {
            super(null);
            this.rm = rm;
            Dimension d = new Dimension(180, 15);

            if (instance != null) {
                instance.toFront();
                return;
            }
            instance = this;

            try {
                workingimage = (MimsPlus) getImage();
            } catch (Exception e) {
                return;
            }

            if (workingimage == null) {
                return;
            }

            setTitle(workingimage.getTitle());

            // Setup panel.
            JPanel jPanel = new JPanel();
            jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

            // Threshold Min text field.
            JLabel label2 = new JLabel("Threshold min");
            label2.setAlignmentX(LEFT_ALIGNMENT);
            jPanel.add(label2);
            threshMinField.setAlignmentX(LEFT_ALIGNMENT);
            threshMinField.setMaximumSize(d);
            jPanel.add(threshMinField);
            jPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            // Threshold Max text field
            JLabel label3 = new JLabel("Threshold max");
            label3.setAlignmentX(LEFT_ALIGNMENT);
            jPanel.add(label3);
            threshMaxField.setAlignmentX(LEFT_ALIGNMENT);
            threshMaxField.setMaximumSize(d);
            jPanel.add(threshMaxField);
            jPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            // Min Size text field.
            JLabel label4 = new JLabel("Size min");
            label4.setAlignmentX(LEFT_ALIGNMENT);
            jPanel.add(label4);
            sizeMinField.setAlignmentX(LEFT_ALIGNMENT);
            sizeMinField.setMaximumSize(d);
            jPanel.add(sizeMinField);
            jPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            // Max size text field.
            JLabel label5 = new JLabel("Size max");
            label5.setAlignmentX(LEFT_ALIGNMENT);
            jPanel.add(label5);
            sizeMaxField.setAlignmentX(LEFT_ALIGNMENT);
            sizeMaxField.setMaximumSize(d);
            jPanel.add(sizeMaxField);
            jPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            //use filtered checkbox
            useFilteredCheckbox.setAlignmentX(LEFT_ALIGNMENT);
            jPanel.add(useFilteredCheckbox);

            jPanel.add(Box.createRigidArea(new Dimension(0, 30)));

            // Group Assignment Panel.
            groupAssignmentPanel = new MimsRoiManager2.GroupAssignmentPanel(groupListModel.toArray());
            groupAssignmentPanel.setAlignmentX(LEFT_ALIGNMENT);
            jPanel.add(groupAssignmentPanel);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

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
            setSize(new Dimension(480, 700));

        }

        public void updateGroups() {
            groupAssignmentPanel.updateJList(groupListModel.toArray());
        }
        // Gray out textfield when "All" images radio button selected.

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Cancel")) {
                closeWindow();
            } else if (e.getActionCommand().equals("OK")) {

                //
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    Roi[] rois = getSelectedROIs();

                    double mint = 0, maxt = 0, mins = 0, maxs = 0.0;
                    double diag = 0;
                    if (!threshMinField.getText().isEmpty()) {
                        mint = Double.parseDouble(threshMinField.getText());
                    }
                    if (!threshMaxField.getText().isEmpty()) {
                        maxt = Double.parseDouble(threshMaxField.getText());
                    }
                    if (!sizeMinField.getText().isEmpty()) {
                        mins = Double.parseDouble(sizeMinField.getText());
                    }
                    if (!sizeMaxField.getText().isEmpty()) {
                        maxs = Double.parseDouble(sizeMaxField.getText());
                    }

                    double[] params = {mint, maxt, mins, maxs, diag};

                    MimsPlus img = workingimage;
                    if ((img.getMimsType() == MimsPlus.HSI_IMAGE || img.getMimsType() == MimsPlus.RATIO_IMAGE) && img.internalRatio != null) {
                        if (useFilteredCheckbox.isSelected() && img.internalRatio_filtered != null) {
                            img = img.internalRatio_filtered;
                        } else {
                            img = img.internalRatio;
                        }

                    }
                    for (int r = 0; r < rois.length; r++) {
                        rm.addToGroup(roiThreshold(rois[r], img, params), groupAssignmentPanel.getGroupName());
                    }
                } catch (Exception x) {
                    ij.IJ.error("Error", "Not a number.");
                    x.printStackTrace();
                    return;
                } finally {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
                ui.updateAllImages();
                closeWindow();
            }
        }

        /**
         * Sets the image to perform the calculation.
         */
        public void resetImage() {
            try {
                MimsPlus mp = (MimsPlus) getImage();
                resetImage(mp);
            } catch (Exception e) {
                return;
            }
        }

        /**
         * Sets the image to perform the calculation.
         *
         * @param img the image.
         */
        public void resetImage(MimsPlus img) {
            workingimage = img;
            setTitle(workingimage.getTitle());
        }

        // Show the frame.
        public void showFrame() {
            setLocation(400, 400);
            resetImage();
            setVisible(true);
            toFront();
            setExtendedState(NORMAL);
        }

        public void closeWindow() {
            super.close();
            instance = null;
            this.setVisible(false);
        }
    }

    /**
     * Creates a JPanel that can be used for frames that do some work with ROIS that results in the creation of multiple
     * Rois. This panel allows the user to select from the list of already created groups or a rtextfield to create a
     * new one.
     */
    public class GroupAssignmentPanel extends JPanel implements ActionListener {

        static final String ASSIGN_GROUP = "Assign to existing group";
        static final String CREATE_GROUP = "Create new group";

        public JRadioButton assignGroupButton = null;
        public JRadioButton createGroupButton = null;
        JTextField createGroupTextField;
        JList groupList;

        public GroupAssignmentPanel(Object[] elements) {

            // ScrollPane
            groupList = new JList(elements);
            groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            groupList.setSelectedValue(DEFAULT_GROUP, true);
            Dimension d2 = new Dimension(230, 320);
            JScrollPane groupscrollpane = new JScrollPane(this.groupList);
            groupscrollpane.setPreferredSize(d2);
            groupscrollpane.setMinimumSize(d2);
            groupscrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            // Create the radio buttons.
            assignGroupButton = new JRadioButton(ASSIGN_GROUP);
            assignGroupButton.setActionCommand(ASSIGN_GROUP);
            assignGroupButton.addActionListener(this);
            assignGroupButton.setSelected(true);

            createGroupButton = new JRadioButton(CREATE_GROUP);
            createGroupButton.setActionCommand(CREATE_GROUP);
            createGroupButton.addActionListener(this);

            // Group the radio buttons.
            ButtonGroup group = new ButtonGroup();
            group.add(assignGroupButton);
            group.add(createGroupButton);

            // Textfield to allow user to create group.
            createGroupTextField = new JTextField(15);
            createGroupTextField.setEnabled(false);
            JPanel createGroupTextFieldJPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            createGroupTextFieldJPanel.add(createGroupTextField);

            // Left Panel
            Dimension dl = d2;
            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
            leftPanel.setPreferredSize(dl);
            leftPanel.setMinimumSize(dl);
            leftPanel.add(assignGroupButton, BorderLayout.NORTH);
            leftPanel.add(groupscrollpane, BorderLayout.CENTER);

            // Right Panel
            Dimension dr = new Dimension(180, 320);
            JPanel rightPanel = new JPanel(new BorderLayout());
            rightPanel.setPreferredSize(dr);
            rightPanel.setMinimumSize(dr);
            rightPanel.add(createGroupButton, BorderLayout.NORTH);
            rightPanel.add(createGroupTextFieldJPanel, BorderLayout.CENTER);

            setLayout(new FlowLayout(FlowLayout.LEFT));
            add(leftPanel);
            add(rightPanel);

            //setBorder(BorderFactory.createRaisedBevelBorder());
            setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        }

        public void updateJList(Object[] elements) {
            groupList.setListData(elements);
        }

        /**
         * Action methods for radio button clicks.
         *
         * @param e Action Event
         */
        public void actionPerformed(ActionEvent e) {
            // Assign Group
            if (e.getActionCommand().equals(ASSIGN_GROUP)) {
                createGroupTextField.setEnabled(false);
                groupList.setEnabled(true);

                // Create Group
            } else if (e.getActionCommand().equals(CREATE_GROUP)) {
                createGroupTextField.setEnabled(true);
                groupList.setEnabled(false);
            }
        }

        /**
         * Sets the image to perform the calculation.
         *
         * @return the group the be used for the assignment.
         */
        public String getGroupName() {
            String returnVal = DEFAULT_GROUP;
            if (assignGroupButton.isSelected()) {
                returnVal = (String) groupList.getSelectedValue();
            } else if (createGroupButton.isSelected()) {
                String field = createGroupTextField.getText().trim();
                if (field.length() != 0) {
                    returnVal = field;
                }
            }
            return returnVal;
        }

    }

    /**
     * Documentation Required.
     */
    public class SquaresManager extends com.nrims.PlugInJFrame implements ActionListener {

        Frame instance;
        MimsRoiManager2 rm;

        MimsPlus workingimage;
        JTextField sizeField = new JTextField(10);
        JTextField numberField = new JTextField(10);
        JTextField rangeField = new JTextField(10);
        JCheckBox allowOverlap = new JCheckBox("Allow Overlap", false);
        GroupAssignmentPanel groupAssignmentPanel;

        JButton cancelButton;
        JButton SButton;
        JButton ZButton;

        public SquaresManager(MimsRoiManager2 rm) {
            super(null);
            this.rm = rm;
            Dimension d = new Dimension(180, 15);
            if (instance != null) {
                instance.toFront();
                return;
            }

            instance = this;

            try {
                workingimage = (MimsPlus) getImage();
            } catch (Exception e) {
                return;
            }

            if (workingimage == null) {
                return;
            }

            setTitle(workingimage.getTitle());

            // Setup panel.
            JPanel jPanel = new JPanel();
            jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

            // Square Size text field.
            JLabel label2 = new JLabel("Square size (n x n pixels)");
            label2.setAlignmentX(LEFT_ALIGNMENT);
            jPanel.add(label2);
            sizeField.setAlignmentX(LEFT_ALIGNMENT);
            sizeField.setMaximumSize(d);
            jPanel.add(sizeField);
            jPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            // Number of Squares text field.
            JLabel label3 = new JLabel("Number of squares");
            label3.setAlignmentX(LEFT_ALIGNMENT);
            jPanel.add(label3);
            numberField.setAlignmentX(LEFT_ALIGNMENT);
            numberField.setMaximumSize(d);
            jPanel.add(numberField);
            jPanel.add(Box.createRigidArea(new Dimension(0, 20)));

            // Overlap checkbox.
            allowOverlap.setAlignmentX(LEFT_ALIGNMENT);
            jPanel.add(allowOverlap);
            jPanel.add(Box.createRigidArea(new Dimension(0, 30)));

            // Group Assignment Panel.
            groupAssignmentPanel = new GroupAssignmentPanel(groupListModel.toArray());
            groupAssignmentPanel.setAlignmentX(LEFT_ALIGNMENT);
            jPanel.add(groupAssignmentPanel);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Button Panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            cancelButton = new JButton("Cancel");
            cancelButton.setActionCommand("Cancel");
            cancelButton.addActionListener(this);
            SButton = new JButton("Squares");
            SButton.setActionCommand("Squares");
            SButton.addActionListener(this);
            ZButton = new JButton("SquaresZ");
            ZButton.setActionCommand("SquaresZ");
            ZButton.addActionListener(this);
            ZButton.setEnabled(false);
            buttonPanel.add(cancelButton);
            buttonPanel.add(SButton);
            buttonPanel.add(ZButton);

            // Assemble.
            setLayout(new BorderLayout());
            add(jPanel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.PAGE_END);
            setSize(new Dimension(480, 590));
        }

        public void updateGroups() {
            groupAssignmentPanel.updateJList(groupListModel.toArray());
        }

        public void actionPerformed(ActionEvent e) {

            // Cancel
            if (e.getActionCommand().equals("Cancel")) {
                closeWindow();

                // Squares
            } else if (e.getActionCommand().equals("Squares")) {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    double size = Double.parseDouble(sizeField.getText());
                    double num = Double.parseDouble(numberField.getText());
                    double overlap = 0.0;
                    if (allowOverlap.isSelected()) {
                        overlap = 1.0;
                    }
                    double[] params = {size, num, overlap};
                    Roi[] rois = rm.getSelectedROIs();
                    if (rois.length == 0) {
                        ij.IJ.error("Error", "No rois selected.");
                        return;
                    }
                    MimsPlus img = workingimage;
                    if ((img.getMimsType() == MimsPlus.HSI_IMAGE || img.getMimsType() == MimsPlus.RATIO_IMAGE) && img.internalRatio != null) {
                        img = img.internalRatio;
                    }
                    for (int r = 0; r < rois.length; r++) {
                        rm.addToGroup(roiSquares(rois[r], img, params), groupAssignmentPanel.getGroupName());
                    }
                } catch (Exception x) {
                    ij.IJ.error("Error", "Not a number.");
                    return;
                } finally {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }

                ui.updateAllImages();
                closeWindow();

                // SquaresZ
            }
        }

        /**
         * Sets the image to perform the calculation.
         */
        public void resetImage() {
            try {
                MimsPlus mp = (MimsPlus) getImage();
                resetImage(mp);
            } catch (Exception e) {
                return;
            }
        }

        /**
         * Sets the image to perform the calculation.
         *
         * @param img the image.
         */
        public void resetImage(MimsPlus img) {
            workingimage = img;
            setTitle(workingimage.getTitle());
        }

        // Show the frame.
        public void showFrame() {
            setLocation(400, 400);
            resetImage();
            setVisible(true);
            toFront();
            setExtendedState(NORMAL);
        }

        public void closeWindow() {
            super.close();
            instance = null;
            this.setVisible(false);
        }
    }

}
