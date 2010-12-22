package com.nrims;

import com.nrims.data.MIMSFileFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.filter.*;
import ij.util.Tools;
import ij.measure.Calibration;
import ij.plugin.frame.*;
import ij.plugin.filter.ParticleAnalyzer;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;


/**
 * The MimsRoiManager provides the user with a graphical interface for
 * performing various operations with ROIs.
 */
public class MimsRoiManager extends PlugInJFrame implements ActionListener {

    JPanel panel;
    MimsJTable table;
    static Frame instance;
    static final String DEFAULT_GROUP = "...";
    static final String GROUP_FILE_NAME = "group";
    static final String GROUP_MAP_FILE_NAME = "group_map";
    JList roijlist;
    JList groupjlist;
    DefaultListModel roiListModel;
    DefaultListModel groupListModel;
    Hashtable rois = new Hashtable();
    boolean canceled;
    boolean macro;
    boolean ignoreInterrupts;
    JPopupMenu pm;
    JButton moreButton;
    JButton delete;
    JButton rename;
    JCheckBox cbHideAll;
    JCheckBox cbAllPlanes;
    JCheckBox cbHideLabels;
    JSpinner xPosSpinner, yPosSpinner, widthSpinner, heightSpinner;
    JLabel xLabel, yLabel, wLabel, hLabel;
    boolean holdUpdate = false;
    private UI ui = null;
    private String savedpath = "";
    boolean previouslySaved = false;
    boolean bAllPlanes = true;
    HashMap locations = new HashMap<String, ArrayList<Integer[]>>();
    HashMap groupsMap = new HashMap<String, String>();
    ArrayList groups = new ArrayList<String>();
    ParticlesManager partManager;
    SquaresManager squaresManager;
    String hideAllRois = new String("Hide All Rois");
    String moveAllRois = new String("Move All");
    String hideAllLabels = new String("Hide Labels");
    ListSelectionListener groupSelectionListener;

   /**
    * Creates a new instance of MimsRoiManager.
    *
    * @param ui a pointer to the UI.
    */
    public MimsRoiManager(UI ui) {
        super("MIMS ROI Manager");

        this.ui = ui;

        if (instance != null) {
            instance.toFront();
            return;
        }
        instance = this;
        ImageJ ij = IJ.getInstance();
        addKeyListener(ij);
        //addMouseListener(this);
        WindowManager.addWindow(this);
        setLayout(new FlowLayout());

        // JList stuff - for ROIs
        roiListModel = new DefaultListModel();
        roijlist = new JList(roiListModel) {
           protected void processMouseEvent(MouseEvent e) {
              if (e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == MouseEvent.MOUSE_DRAGGED) {
                 if (roijlist.getCellBounds(0, roiListModel.size() - 1).contains(e.getPoint()) == false) {
                    roijlist.clearSelection();
                    e.consume();
                 } else {
                    super.processMouseEvent(e);
                 }
              }
           }
        };
        roijlist.setCellRenderer(new ComboBoxRenderer());
        roijlist.addKeyListener(ij);
        roijlist.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
           public void valueChanged(ListSelectionEvent listSelectionEvent) {
              roivalueChanged(listSelectionEvent);
           }
        });

        // JList stuff - for Groups
        groupListModel = new DefaultListModel();
        groupListModel.addElement(DEFAULT_GROUP);
        groupjlist = new JList(groupListModel);
        groupSelectionListener = new ListSelectionListener() {
           public void valueChanged(ListSelectionEvent listSelectionEvent) {
             groupvalueChanged(listSelectionEvent);
           }
        };
        groupjlist.addListSelectionListener(groupSelectionListener);

        // Group scrollpane.
        Dimension d2 = new Dimension(230, 450);
        JScrollPane groupscrollpane = new JScrollPane(groupjlist);
        groupscrollpane.setPreferredSize(d2);
        groupscrollpane.setMinimumSize(d2);
        groupscrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Roi scrollpane.
        JScrollPane roiscrollpane = new JScrollPane(roijlist);
        roiscrollpane.setPreferredSize(d2);
        roiscrollpane.setMinimumSize(d2);
        roiscrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Create, Delete Button.
        JButton create = new JButton("New");
        create.setMargin( new Insets(0, 0, 0, 0) );
        create.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
            createActionPerformed(evt);
         }});
        delete = new JButton("Delete");
        delete.setMargin( new Insets(0, 0, 0, 0) );
        delete.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
            deleteActionPerformed(evt);
         }});
         rename = new JButton("Rename");
         rename.setMargin( new Insets(0, 0, 0, 0) );
         rename.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
            renameActionPerformed(evt);
         }});


        // Assign, Deassign Button.
        JButton assign = new JButton("Assign");
        assign.setMargin( new Insets(0, 0, 0, 0) );
        assign.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
            assignActionPerformed(evt);
         }});
        JButton dassign = new JButton("Deassign");
        dassign.setMargin( new Insets(0, 0, 0, 0) );
        dassign.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
            deassignActionPerformed(evt);
         }});

        // Assemble
        Dimension d1 = new Dimension(200, 350);
        JPanel leftPanel    = new JPanel(new BorderLayout());
        JPanel rightPanel   = new JPanel(new BorderLayout());
        JPanel centerPanel1 = new JPanel();
        JPanel eastPanel1   = new JPanel();
        JPanel westPanel1   = new JPanel();
        JPanel southPanel1  = new JPanel();
        JPanel northPanel1  = new JPanel();
        JPanel centerPanel2 = new JPanel();
        JPanel westPanel2   = new JPanel();
        JPanel southPanel2  = new JPanel();
        JPanel northPanel2  = new JPanel();

        // Left pane - south panel
        southPanel1.setLayout(new GridLayout(2,2));
        southPanel1.add(create);
        southPanel1.add(delete);
        southPanel1.add(rename);

        // Left pane - north panel
        northPanel1.add(new JLabel("Groups"));

        // Left pane
        Dimension dLeft = new Dimension(165, 350);
        leftPanel.setPreferredSize(dLeft);
        leftPanel.add(groupscrollpane, BorderLayout.CENTER);
        leftPanel.add(southPanel1, BorderLayout.SOUTH);
        leftPanel.add(northPanel1, BorderLayout.NORTH);

        // Right pane - south panel
        southPanel2.setLayout(new GridLayout(2,2));
        southPanel2.add(assign);
        southPanel2.add(dassign);
        southPanel2.add(new JLabel(""));

        // Right pane - north panel
        northPanel2.add(new JLabel("Rois"));

        // Right pane - center panel
        Dimension dRight = new Dimension(165, 350);
        rightPanel.setPreferredSize(dRight);
        rightPanel.setMaximumSize(dRight);
        rightPanel.setMinimumSize(dRight);

        //Right pane - east panel
        panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.setPreferredSize(new Dimension(200, 350));

        // Placeholders.
        JLabel emptySpace1 = new JLabel("");
        emptySpace1.setBorder(BorderFactory.createEmptyBorder(5, 70, 5, 70));
        JLabel emptySpace2 = new JLabel("");
        emptySpace2.setBorder(BorderFactory.createEmptyBorder(5, 70, 5, 70));
        JLabel emptySpace3 = new JLabel("");
        emptySpace3.setBorder(BorderFactory.createEmptyBorder(0, 70, 5, 70));
        JLabel emptySpace4 = new JLabel("");
        emptySpace4.setBorder(BorderFactory.createEmptyBorder(5, 70, 5, 70));

        panel.add(emptySpace1);
        addButton("Delete");
        addButton("Rename");
        addButton("Open");
        addButton("Save");
        addButton("Measure");
        addButton("More>>");
        addPopupMenu();
        
        //order of these calls determines position...
        panel.add(emptySpace2);
        setupPosLabels();
        setupPosSpinners();
        panel.add(emptySpace3);
        setupSizeLabels();
        setupSizeSpinners();
        
        // Add checkboxes.
        panel.add(emptySpace4);
        addCheckbox(moveAllRois, true);
        addCheckbox(hideAllRois, false);
        addCheckbox(hideAllLabels, false);
        
        //rightPanel.add(panel, BorderLayout.EAST);
        rightPanel.add(roiscrollpane, BorderLayout.CENTER);
        rightPanel.add(southPanel2, BorderLayout.SOUTH);
        rightPanel.add(northPanel2, BorderLayout.NORTH);

        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        add(leftPanel);
        add(rightPanel);
        add(panel);
        pack();
        GUI.center(this);
    }

   /**
    * Action Method for the "new" group button.
    */
    private void createActionPerformed(ActionEvent e) {
        String s = (String)JOptionPane.showInputDialog(this,"Enter new group name:\n","Enter",
                    JOptionPane.PLAIN_MESSAGE,null,null,"");
        addGroup(s);
    }

   /**
    * Adds a new group.
    *
    * @param s the name of the group.
    * @return <code>true</code> if successful, otherwise <code>false</code>.
    */
    private boolean addGroup(String s) {
        
       if (s == null)
          return false;

       if (s.equals(DEFAULT_GROUP))
          return false;

       s = s.trim();
       if (s.equals(""))
          return false;

       if (groups.contains(s))
          return false;
       else {
          groups.add(s);
          clearGroupListModel();
          Object[] groupsArray = groups.toArray();
          String[] groupsStringArray = new String[groupsArray.length];
          for (int i = 0; i < groupsArray.length; i++)
            groupsStringArray[i] = (String)groupsArray[i];
          java.util.Arrays.sort(groupsStringArray);
          for (int i = 0; i < groupsStringArray.length; i++)
            groupListModel.addElement(groupsStringArray[i]);
       }

       return true;
    }

   /**
    * Deletes selected groups and all associations.
    */
    private void deleteActionPerformed(ActionEvent e) {
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
    }

   /**
    * Renames the selected group.
    */
    private void renameActionPerformed(ActionEvent e) {
        int[] idxs = groupjlist.getSelectedIndices();
        if(idxs.length==0)
           return;
        int index = idxs[0];
        String groupName = (String)groupListModel.get(index);

        String newName = (String)JOptionPane.showInputDialog(this,"Enter new name for group "+ groupName +" :\n","Enter",
                    JOptionPane.PLAIN_MESSAGE,null,null,"");
        newName = newName.trim();
        if (newName == null || newName.equals("") || newName.equals(DEFAULT_GROUP))
           return;
                               
        if (addGroup(newName)) {

          // Collect all rois of that group
          ArrayList<Roi> grprois = new ArrayList<Roi>();
          for (int id = 0; id < roijlist.getModel().getSize(); id++) {
             String roiName = roijlist.getModel().getElementAt(id).toString();
             String roigroupName = (String) groupsMap.get(roiName);
             if (roigroupName != null) {
                if (roigroupName.equals(groupName)) {
                   grprois.add(this.getRoiByName(roiName));
                }
             }
          }

          //update groups
          groups.remove(groupName);

          //update list
          groupListModel.removeElement(groupName);

          //deassign all rois
          for (int i = 0; i < grprois.size(); i++)
             groupsMap.remove(grprois.get(i).getName());

          //reassign all rois
          for (int i = 0; i < grprois.size(); i++)
             groupsMap.put(grprois.get(i).getName(), newName);
       }
    }

   /**
    * Assigns all selected rois to the selected group.
    */      
    private void deassignActionPerformed(ActionEvent e) {

       // Must have at least 1 roi selected to make an assignment.
       int[] Roiidxs = roijlist.getSelectedIndices();
       if (Roiidxs.length < 1)
          return;

       // Deassign all selected Rois.
       for (int i = 0; i < Roiidxs.length; i++) {
          String roiName = (String)roiListModel.get(Roiidxs[i]);
          groupsMap.remove(roiName);
       }

    }


   /**
    * Assigns all selected rois to the selected group.
    */
    private void assignActionPerformed(ActionEvent e) {

       int MAX_LIST_LENGTH = 13;

       // Must have at least 1 roi selected to make an assignment.
       int[] Roiidxs = roijlist.getSelectedIndices();
       if (Roiidxs.length < 1)
          return;

       // Get all the possible groups.
       Object[] possibilities = groupListModel.toArray();

       // Construct and display dialog box.
       String roiList = "Assign the following Rois:\n\n";
       int listLength = Roiidxs.length;
       if (listLength > MAX_LIST_LENGTH)
          listLength = MAX_LIST_LENGTH-3;
       for (int i = 0; i < listLength; i++)
          roiList += "\t\t" + (String)roiListModel.get(Roiidxs[i]) + "\n";
       if (Roiidxs.length > MAX_LIST_LENGTH) {
          roiList += ".\n";
          roiList += ".\n";
          roiList += ".\n";
          roiList += "\t\t" + (String)roiListModel.get(Roiidxs[Roiidxs.length-2]) + "\n";
          roiList += "\t\t" + (String)roiListModel.get(Roiidxs[Roiidxs.length-1]) + "\n";
       }
       roiList += "\n";
       String s = (String)JOptionPane.showInputDialog(this, roiList, "Customized Dialog",
                    JOptionPane.PLAIN_MESSAGE, null, possibilities, DEFAULT_GROUP);

       // User hit cancel;
       if (s == null)
          return;

       // Assign all rois to selected group.
       for(int i = 0; i < Roiidxs.length; i++) {
          String roiName = (String)roiListModel.get(Roiidxs[i]);
          groupsMap.put(roiName, s);
       }
       groupjlist.setSelectedValue(s, true);

    }

   /**
    * Setup the position spinners for controlling ROI location.
    */
    private void setupPosSpinners() {
        xPosSpinner = new JSpinner();
        yPosSpinner = new JSpinner();

        xPosSpinner.setPreferredSize(new Dimension(90, 30));
        yPosSpinner.setPreferredSize(new Dimension(90, 30));

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

        panel.add(xPosSpinner);
        panel.add(yPosSpinner);
    }

   /**
    * Setup the size spinners for controlling ROI size.
    * This can only be used with certain types of symmetric ROIs
    * (squares, oval etc.)
    */
    private void setupSizeSpinners() {
        widthSpinner = new JSpinner();
        heightSpinner = new JSpinner();

        widthSpinner.setPreferredSize(new Dimension(90, 30));
        heightSpinner.setPreferredSize(new Dimension(90, 30));

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

        panel.add(widthSpinner);
        panel.add(heightSpinner);

    }

   /** More setting up of the layout.*/
    private void setupPosLabels() {
        xLabel = new JLabel("X Pos.");
        yLabel = new JLabel("Y Pos.");
        xLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        yLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        xLabel.setPreferredSize(new Dimension(90, 15));
        yLabel.setPreferredSize(new Dimension(90, 15));

        panel.add(xLabel);
        panel.add(yLabel);
    }

    /** More setting up of the layout.*/
    private void setupSizeLabels() {
        wLabel = new JLabel("Width");
        hLabel = new JLabel("Height");
        wLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        hLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        wLabel.setPreferredSize(new Dimension(90, 15));
        hLabel.setPreferredSize(new Dimension(90, 15));

        panel.add(wLabel);
        panel.add(hLabel);
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
         int size_new = ui.getmimsAction().getSize();

         // Difference in sizes
         int size_orig = xylist.size();
         int size_diff = size_new - size_orig;

         // If prepending use FIRST element.
         // If appending use LAST element.
         Integer[] xy = new Integer[2];
         if (prepend) {
            xy = xylist.get(0);
         } else {
            xy = xylist.get(xylist.size()-1);
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
   }

   /** Updates the locations array for all ROIs.*/
   private void updateRoiLocations() {

      // Loop over rios.
      for (Object key : locations.keySet()) {

         // Get roi location size.
         ArrayList<Integer[]> xylist = (ArrayList<Integer[]>) locations.get(key);

         // Current image size
         int size_new = ui.getmimsAction().getSize();

         // Difference in sizes
         int size_orig = xylist.size();
         int size_diff = size_new - size_orig;
         if(size_diff>0) updateRoiLocations(false);

         // Remove positions
         // size_diff must be negative here so
         // decrimenting instead of incrementing
         for (int i = 0; i > size_diff; i--) {
            xylist.remove(xylist.size()-1);
         }
      }
   }

   /**
    * Updates the locations array for all ROIs. To
    * be used when opening a new image.
    */
   public void resetRoiLocationsLength() {

       for (Object key : rois.keySet()) {
         // Get roi location size.
         Roi roi = (Roi)rois.get(key);
         Rectangle rec = roi.getBoundingRect();
         ArrayList<Integer[]> xylist = (ArrayList<Integer[]>) locations.get(key);

         // If no entry, create one.
         if (xylist == null) {
            int stacksize = ui.getmimsAction().getSize();
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
            int img_size = ui.getmimsAction().getSize();
            int diff = locations_size - img_size;
            if(diff < 0) {
               //grow locations arraylist
               updateRoiLocations(false);
            } else if(diff > 0) {
               //shrink locations arraylist
               updateRoiLocations();
            }
         }
       }
   }

   /** Updates the position spinners.*/
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

      if (!label.equals(""))
         xylist = (ArrayList<Integer[]>)locations.get(label);
      else
         return;

      if (xylist != null)
         xy = (Integer[])xylist.get(ui.getOpenMassImages()[0].getCurrentSlice()-1);
      else
         return;

      if (xy != null) {
         holdUpdate = true;
         xPosSpinner.setValue(xy[0]);
         yPosSpinner.setValue(xy[1]);
         holdUpdate = false;
      } else {
         return;
      }

   }

   /**
    * Use this method instead of groupListModel.clear() because we do not
    * want the "..." listing to dissapear, because it represents all Rois.
    */
   private void clearGroupListModel() {
      for (int i = groupListModel.getSize()-1; i >= 0; i--) {
         String group = (String)groupListModel.get(i);
         if (!group.equals(DEFAULT_GROUP)) {
            groupListModel.remove(i);
         }
      }
   }

   /** Action method for changing position spinners.*/
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

      int plane = imp.getCurrentSlice();
      int trueplane = ui.getmimsAction().trueIndex(plane);
      ArrayList xylist = (ArrayList<Integer[]>)locations.get(label);
      xylist.set(trueplane-1, new Integer[] {(Integer) xPosSpinner.getValue(), (Integer) yPosSpinner.getValue()});
      locations.put(label, xylist);

      // For display purposes.
      Roi roi = (Roi)rois.get(label);
      roi.setLocation((Integer) xPosSpinner.getValue(), (Integer) yPosSpinner.getValue());
      imp.setRoi(roi);
      move();

      updatePlots(false);

    }

    /** Action method for changing size spinners.*/
     private void hwSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {

        String label = "";

        if (holdUpdate) return;

        if (roijlist.getSelectedIndices().length != 1) {
           error("Exactly one item in the list must be selected.");
           return;
        } else {
           label = roijlist.getSelectedValue().toString();
        }

        // Make sure we have an image
        ImagePlus imp = getImage();
        if (imp == null) return;

        // Make sure we have a ROI
        Roi oldroi = (Roi)rois.get(label);
        if (oldroi == null) return;

        // There is no setWidth or  setHeight method for a ROI
        // so essentially we have to create a new one, setRoi
        // will delete the old one from the rois hashtable.
        Roi newroi = null;
        java.awt.Rectangle rect = oldroi.getBoundingRect();

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
        updatePlots(false);
    }

    /** layout method.*/
    private void addCheckbox(String label, boolean bEnabled) {
        JCheckBox cb = new JCheckBox(label);
        cb.setPreferredSize(new Dimension(130, 20));
        cb.setMaximumSize(cb.getPreferredSize());
        cb.setMinimumSize(cb.getPreferredSize());
        if (label.equals(hideAllRois)) {
            cbHideAll = cb;
        } else if (label.equals(moveAllRois)) {
            cb.setPreferredSize(new Dimension(175, 20));
            cb.setMaximumSize(cb.getPreferredSize());
            cb.setMinimumSize(cb.getPreferredSize());
            cbAllPlanes = cb;
        } else if (label.equals(hideAllLabels)) {
           cb.setPreferredSize(new Dimension(175, 20));
           cb.setMaximumSize(cb.getPreferredSize());
           cb.setMinimumSize(cb.getPreferredSize());
           cbHideLabels = cb;
        }
        cb.setSelected(bEnabled);
        cb.addActionListener(this);
        panel.add(cb);
    }

    /** layout method.*/
    private void addButton(String label) {
        JButton b = new JButton(label);
        b.setMargin( new Insets(0, 0, 0, 0) );
        b.setPreferredSize(new Dimension(90, 30));
        b.setMaximumSize(b.getPreferredSize());
        b.setMinimumSize(b.getPreferredSize());
        b.addActionListener(this);
        b.addKeyListener(IJ.getInstance());
        //b.addMouseListener(this);
        if (label.equals("More>>")) {
            moreButton = b;
        }
        panel.add(b);
    }

    /** layout method.*/
    private void addPopupMenu() {
        pm = new JPopupMenu();
        pm.setBorderPainted(true);
        pm.setBorder(new javax.swing.border.LineBorder(Color.BLACK));
        addPopupItem("Duplicate");
        addPopupItem("Combine");
        addPopupItem("Split");
        addPopupItem("Particles");
        addPopupItem("Squares");
        addPopupItem("Pixel values");
        addPopupItem("Add [t]");
        addPopupItem("Reorder all");
        add(pm);
    }

    /** layout method.*/
    private void addPopupItem(String s) {
        JMenuItem mi = new JMenuItem(s);
        mi.addActionListener(this);
        pm.add(mi);
    }

    /** Action event handler.*/
    public void actionPerformed(ActionEvent e) {

        int modifiers = e.getModifiers();
        boolean altKeyDown = (modifiers & ActionEvent.ALT_MASK) != 0 || IJ.altKeyDown();
        boolean shiftKeyDown = (modifiers & ActionEvent.SHIFT_MASK) != 0 || IJ.shiftKeyDown();
        IJ.setKeyUp(KeyEvent.VK_ALT);
        IJ.setKeyUp(KeyEvent.VK_SHIFT);
        String label = e.getActionCommand();
        if (label == null) {
            return;
        }
        String command = label;
        if (command.equals("Add [t]")) {
            add();
        } else if (command.equals("Delete")) {
            delete();
        } else if (command.equals("Rename")) {
            rename(null);
        } else if (command.equals("Open")) {
            open(null, false);
        } else if (command.equals("Save")) {
            save();
        } else if (command.equals("Measure")) {
            measure();
        } else if (command.equals("Deselect")) {
            select(-1);
            ui.updateAllImages();
        } else if (command.equals(hideAllRois)) {
            hideAll();
        } else if (command.equals(hideAllLabels)) {
            hideLabels();
        } else if (command.equals("More>>")) {
            ImagePlus imp = getImage();
            if (imp == null)
               return;
            Point ploc = panel.getLocation();
            Point bloc = moreButton.getLocation();
            pm.show(this, ploc.x, bloc.y);
        } else if (command.equals("Duplicate")) {
            duplicate();
        } else if (command.equals("Combine")) {
            combine();
        } else if (command.equals("Split")) {
            split();
        } else if (command.equals("Particles")) {
            if(partManager==null) { partManager = new ParticlesManager(); }
            partManager.showFrame();
        } else if (command.equals("Squares")) {
            if(squaresManager==null) { squaresManager = new SquaresManager(this); }
            squaresManager.showFrame();
        } else if (command.equals("Pixel values")) {
            roiPixelvalues();
        } else if (command.equals(moveAllRois)) {
            bAllPlanes = cbAllPlanes.isSelected();
        } else if (command.equals("Reorder all")) {
           reorderAll();
        }
    }

    /**
     * Checks to see if the group assigned to roiName is contained within groupNames.
     */
    private boolean containsRoi(String[] groupNames, String roiName){
       for (String groupName : groupNames) {
          if (groupName.equals(DEFAULT_GROUP))
             return true;
          if (groupsMap.get(roiName) == null)
             return false;
          if (((String)groupsMap.get(roiName)).equals(groupName))
             return true;
       }
       return false;
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

    /**
     * Determines the behavior when a group entry is clicked.
     */
    private void groupvalueChanged(ListSelectionEvent e) {

       if (e == null)
          return;

       boolean adjust = e.getValueIsAdjusting();

       if (!adjust){
          holdUpdate = true;
          boolean defaultGroupSelected = false;

          // Get the selected groups.
          int[] indices = groupjlist.getSelectedIndices();
          String[] groupNames = new String[indices.length];
          for (int i = 0; i < indices.length; i++){
             groupNames[i] = (String)groupListModel.getElementAt(indices[i]);
             if (groupNames[i].equals(DEFAULT_GROUP))
                defaultGroupSelected = true;
          }

          // Show only Rois that are part of the selected groups.
          roiListModel.removeAllElements();
          ArrayList<String> names = new ArrayList<String>();
          ArrayList<Integer> numeric = new ArrayList<Integer>();
          for (Object object : rois.keySet()) {
             String roiName = (String)object;
             boolean contains = containsRoi(groupNames, roiName);
             if (contains) {
                 roiListModel.addElement(roiName);
             }
          }

          sortROIList();

          // Disable delete button if Default Group is one of the groups selected.
          //this is why the delete button is a class variable andd the others aren't...
          if (defaultGroupSelected) {
             delete.setEnabled(false);
             rename.setEnabled(false);
          } else {
             delete.setEnabled(true);
             rename.setEnabled(true);
          }

          ui.updateAllImages();

          holdUpdate = false;
       }
    }

    /**
     * Determines the behavior when a ROI entry is clicked.
     */
    public void roivalueChanged(ListSelectionEvent e) {

        // DO NOTHING!!  Wait till we are done switching
        if (!e.getValueIsAdjusting()) return;

        holdUpdate = true;

        int[] indices = roijlist.getSelectedIndices();
        if (indices.length == 0) return;

        // Select ROI in the window
        int index = indices[indices.length - 1];
        if (index < 0) index = 0;

        restore(index);

        // Do spinner stuff
        if (indices.length == 1) {
            ImagePlus imp = getImage();
            if (imp == null) return;
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
           String roiName = (String)roiListModel.getElementAt(indices[0]);
           String groupName = (String)groupsMap.get(roiName);

           // This is my attempt at setting the selection of the
           // groupjlist WITHOUT triggering the ListSelectionListener.
           // The only way I have been able to accomplish this is by
           // first removing the listener, setting the selectedvalue,
           // then re-adding the listener. If you know a better way,
           // let me know.
           groupjlist.removeListSelectionListener(groupSelectionListener);
           if (groupName == null)
              groupName = DEFAULT_GROUP;
           groupjlist.setSelectedValue(groupName, true);
           groupjlist.addListSelectionListener(groupSelectionListener);
        }

        holdUpdate = false;
    }

    /**
     * If multiple ROIs are selected, statistical
     * information will be displayed in the user interface.
     * (User requestd feature).
     */
    private void selectedRoisStats() {

        // Get the group of selected rois. Ignore Line type rois.
        Roi[] rois = getSelectedROIs();
        ArrayList<Roi> roilist = new ArrayList<Roi>();
        for (int i = 0; i < rois.length; i++) {
            if (rois[i].getType() != Roi.LINE && rois[i].getType() != Roi.FREELINE && rois[i].getType() != Roi.POLYLINE) {
                roilist.add(rois[i]);
            }
        }
        if (roilist.size() == 0) {
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
        if (mp.getMimsType() == MimsPlus.HSI_IMAGE) {
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
     * Update the spinner to reflect the parameters of the passed ROI.
     *
     * @param roi A roi.
     */
    public void resetSpinners(Roi roi) {

       if (roi == null) return;
       holdUpdate = true;

       // get the type of ROI we are dealing with
       int roiType = roi.getType();

       // Not sure if all ROIs have a width-height value that can be adjusted... test
       if (roiType == Roi.RECTANGLE || roiType == Roi.OVAL) {
          enablespinners();
          java.awt.Rectangle rect = roi.getBoundingRect();
          xPosSpinner.setValue(rect.x);
          yPosSpinner.setValue(rect.y);
          wLabel.setText("Width");
          hLabel.setText("Height");
          widthSpinner.setValue(rect.width);
          heightSpinner.setValue(rect.height);
       } else if (roiType == Roi.POLYGON || roiType == Roi.FREEROI) {
          enablePosSpinners();
          disableSizeSpinners();
          java.awt.Rectangle rect = roi.getBoundingRect();
          xPosSpinner.setValue(rect.x);
          yPosSpinner.setValue(rect.y);
       } else if (roiType == Roi.LINE || roiType == Roi.POLYLINE || roiType == Roi.FREELINE) {
           enablePosSpinners();
           disableSizeSpinners();
           java.awt.Rectangle rect = roi.getBoundingRect();
           xPosSpinner.setValue(rect.x);
           yPosSpinner.setValue(rect.y);
       }
       else {
          disablespinners();
       }
       holdUpdate = false;
    }

    /** layout method. */
    private void enablespinners() {
       xPosSpinner.setEnabled(true);
       yPosSpinner.setEnabled(true);
       widthSpinner.setEnabled(true);
       heightSpinner.setEnabled(true);
    }

    /** layout method. */
    private void disablespinners() {
       xPosSpinner.setEnabled(false);
       yPosSpinner.setEnabled(false);
       widthSpinner.setEnabled(false);
       heightSpinner.setEnabled(false);
    }

    /** layout method. */
    private void enablePosSpinners() {
        xPosSpinner.setEnabled(true);
        yPosSpinner.setEnabled(true);
    }

    /** layout method. */
    private void disableSizeSpinners() {
        widthSpinner.setEnabled(false);
        heightSpinner.setEnabled(false);
    }

    /** updates the images so that ROI are not shown. */
    private void hideAll() {
       if (getImage() != null) {
            ui.updateAllImages();
        }
    }

    /** updates the images so that ROI labels are not shown. */
    private void hideLabels() {
       if (getImage() != null) {
            ui.updateAllImages();
        }
    }

    /** Sets the Roi in its current location. */
    private void setRoi(ImagePlus imp, Roi roi) {

      // ROI old name - based on its old bounding rect
      String label = roi.getName();

      Rectangle rec = roi.getBounds();
      MimsPlus mp = null;
      try{
          mp = (MimsPlus)imp;
      } catch(Exception e){
          return;
      }
      int plane = 1;
      int t = mp.getMimsType();
      if( t==mp.RATIO_IMAGE ) {
          plane = ui.getMassImage(mp.getRatioProps().getNumMassIdx()).getCurrentSlice();
      }else if( t==mp.HSI_IMAGE ) {
          plane = ui.getMassImage(mp.getHSIProps().getNumMassIdx()).getCurrentSlice();
      }else if( t==mp.SUM_IMAGE ) {
          plane = ui.getMassImage(mp.getSumProps().getParentMassIdx()).getCurrentSlice();
      }else if(t==mp.MASS_IMAGE) {
          plane = mp.getCurrentSlice();
      }
      int trueplane = ui.getmimsAction().trueIndex(plane);

      ArrayList xylist = (ArrayList<Integer[]>)locations.get(label);
      xylist.set(trueplane-1, new Integer[] {rec.x, rec.y});
      locations.put(label, xylist);

      imp.setRoi(roi);

      imp.updateAndRepaintWindow();
   }

   /** Sets the Roi in its current location. */
   private boolean move(ImagePlus imp, Roi roi) {
      if (imp == null) return false;
      if (roi == null) return false;

       setRoi(imp, roi);

       // Debug
       if (Recorder.record) {
         Recorder.record("mimsRoiManager", "Move");
      }
      return true;
   }

   /** Sets the Roi in its current location. */
   public boolean move() {
        // Get the image and the roi
        ImagePlus imp = getImage();
        Roi roi = imp.getRoi();
        boolean b = true;

        if( bAllPlanes==false) {
            b = move(imp, roi);
        }

        if( bAllPlanes==true ) {
            String label = roi.getName();
            Rectangle rec = roi.getBounds();
            ArrayList xylist = (ArrayList<Integer[]>)locations.get(label);
            if (xylist == null) {
              int stacksize = ui.getmimsAction().getSize();
              xylist = new ArrayList<Integer[]>();
              Integer[] xy = new Integer[2];
              for (int i = 0; i < stacksize; i++) {
                 xy = new Integer[]{rec.x, rec.y};
                 xylist.add(i, xy);
              }
              locations.put(label, xylist);
           }

            int size = ui.getmimsAction().getSize();

            for(int p = 1; p <= size; p ++) {
                xylist.set(p-1, new Integer[] {rec.x, rec.y});
            }
            locations.put(label, xylist);
        }

        return b;
    }

   /** Adds the currently drawn ROI to the manager. */
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
        if(rois.isEmpty()) {
            label += 1;
        } else {
           String maxname = new String("0");
           Roi maxroi = getMaxNumericRoi();
           if (maxroi != null)
                maxname = maxroi.getName();
            int m = Integer.parseInt(maxname);
            m = m +1;
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
        int stacksize = ui.getmimsAction().getSize();
        ArrayList xypositions = new ArrayList<Integer[]>();
        Integer[] xy = new Integer[2];
        for (int i = 0; i < stacksize; i++) {
           xy = new Integer[] {r.x, r.y};
           xypositions.add(i, xy);
        }
        locations.put(label, xypositions);

        // Add roi to list.
        rois.put(label, roi);

        // Assign group.
        int[] indices = groupjlist.getSelectedIndices();
        if (indices.length > 0) {
           String group = (String)groupListModel.getElementAt(indices[0]);
           if (indices.length == 1 && !group.equals(DEFAULT_GROUP))
              groupsMap.put(label, group);
        }

        return true;
    }

    /** Add a ROI to the manager. */
    public boolean add(Roi roi) {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        if (roi == null) {
            return false;
        }
        
        String label = "";
        if(rois.isEmpty()) {
            label += 1;
        } else {
            String maxname = getMaxNumericRoi().getName();
            int m = Integer.parseInt(maxname);
            m = m +1;
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
        int stacksize = ui.getmimsAction().getSize();
        ArrayList xypositions = new ArrayList<Integer[]>();
        Integer[] xy = new Integer[2];
        for (int i = 0; i < stacksize; i++) {
           xy = new Integer[] {r.x, r.y};
           xypositions.add(i, xy);
        }
        locations.put(label, xypositions);

        // Add roi to list.
        rois.put(label, roi);

        return true;
    }

    /** Sets an array of Rois to the manager. */
    public boolean add(Roi[] roiarr) {
        boolean val = true;
        for(int i = 0; i < roiarr.length; i++) {
            val = val && this.add(roiarr[i]);
        }
        return val;
    }

    /** Sorts the list of ROIs.*/
    private void sortROIList() {
        String[] roinames = new String[roiListModel.size()];
        for (int i = 0; i < roinames.length; i++) {
            roinames[i] = (String) roiListModel.getElementAt(i);
        }
        roiListModel.removeAllElements();
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Integer> numeric = new ArrayList<Integer>();
        for(int i = 0; i < roinames.length; i++) {
            //keep track of numeric and non-numeric names separately
            if (isNumericName(roinames[i]) && !roinames[i].startsWith("0") ) {
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
       int index = ui.getmimsAction().trueIndex(plane);
       ArrayList<Integer[]> xylist = (ArrayList<Integer[]>)locations.get(label);
       if (xylist == null)
          return null;
       else {
          return xylist.get(index-1);
       }
    }

    /**
     * Deletes currently selected ROIs. Deletes all if none selected.
     *
     * @return <code>true</code> if successful, otherwise <code>false</code>.
     */
    public boolean delete() {
        int count = roiListModel.getSize();
        if (count == 0) {
            return error("The list is empty.");
        }
        int index[] = roijlist.getSelectedIndices();
        if (index.length == 0) {
            String msg = "Delete all items on the list?";
            canceled = false;
            if (!IJ.macroRunning() && !macro) {
                YesNoCancelDialog d = new YesNoCancelDialog(this, "MIMS ROI Manager", msg);
                if (d.cancelPressed()) {
                    canceled = true;
                    return false;
                }
                if (!d.yesPressed()) {
                    return false;
                }
            }
            index = getAllIndexes();
            this.previouslySaved = false;
            this.savedpath = "";
            this.resetTitle();
        }

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
                roiListModel.remove(i);
            }
        }
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Delete");
        }        
        ui.updateAllImages();
        return true;
    }
    
    /**
     * Renames the selected ROI in an ordered sequence
     * starting with 1. If a Roi already exists with that
     * name, it will be adjusted until a unique name is found.
     * (e.g. 1-1, 1-2, etc)
     */
    public void renameGroup(int[] indices){

       String newName, oldName;
       int idx = 1;
       for (int i = 0; i < indices.length; i++) {
          oldName = roiListModel.get(indices[i]).toString();          

          // update rois hashtable
          Roi roi = (Roi) rois.get(oldName);          
          rois.remove(oldName);
          newName = getUniqueName(Integer.toString(idx));
          roi.setName(newName);
          rois.put(newName, roi);

          // update locations array.
          locations.put(newName, locations.get(oldName));
          locations.remove(oldName);

          // update groups map.
          String group = (String) groupsMap.remove(oldName);
          if (group != null)
             groupsMap.put(newName, group);

          // update the list display.
          roiListModel.set(indices[i], newName);

          idx++;
       }
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
        if (group != null)
           groupsMap.put(name2, group);

        // update the list display.
        roiListModel.set(index, name2);
        roijlist.setSelectedIndex(index);

        sortROIList();
        return true;
    }

    /** Displays a prompt asking the user for a new name for the selected ROI.*/
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

    /** Restores the ROI.*/
    private boolean restore(int index) {
        String label = roiListModel.get(index).toString();
        Roi roi = (Roi) rois.get(label);
        MimsPlus imp;
        try{
            imp = (MimsPlus)getImage();
        } catch(ClassCastException e) {
            imp = ui.getOpenMassImages()[0];
        }
        if (imp == null || roi == null) {
            return false;
        }

        // Set the selected roi to yellow
        roi.setInstanceColor(java.awt.Color.yellow);
        imp.setRoi(roi);

        return true;
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
          if (n == JOptionPane.NO_OPTION)
             return;
        }

        ImagePlus imp = getImage();
        if (imp == null) return;
        Macro.setOptions(null);
        File file = new File(abspath);
        if (abspath == null) {

           MimsJFileChooser fc = new MimsJFileChooser(ui);
           MIMSFileFilter mff_rois = new MIMSFileFilter(UI.ROI_EXTENSION.substring(1, UI.ROI_EXTENSION.length()));
           mff_rois.addExtension("zip");
           mff_rois.setDescription("Mims rois");
           fc.addChoosableFileFilter(mff_rois);
           fc.setFileFilter(mff_rois);
           fc.setMultiSelectionEnabled(false);
           fc.setPreferredSize(new java.awt.Dimension(650, 500));
           if (fc.showOpenDialog(this) == JFileChooser.CANCEL_OPTION)
               return;
           file = fc.getSelectedFile();
           if (file == null)
              return;
           abspath = file.getAbsolutePath();
        }

       if (file.getAbsolutePath().endsWith(".zip")) {
          openZip(abspath);
       } else {
          String name = file.getName();
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

    /** Opens a zip file containing saved ROIs.*/
    private void openZip(String path) {
       
       // Delete rois.
       roiListModel.clear();
       rois = new Hashtable();
       locations = new HashMap<String, ArrayList<Integer[]>>();

       // Delete groups.
       clearGroupListModel();
       groups = new ArrayList<String>();
       groupsMap = new HashMap<String, String>();

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
                }
                else if (name.endsWith(".pos")) {
                    ois = new ObjectInputStream(in);
                    HashMap temp_loc = new HashMap<String, ArrayList<Integer[]>>();
                    try {
                        temp_loc = (HashMap<String, ArrayList<Integer[]>>)ois.readObject();
                        this.locations = temp_loc;
                    } catch(ClassNotFoundException e) {
                        error(e.toString());
                        System.out.println(e.toString());
                    }
                }
                else if (name.equals(GROUP_FILE_NAME)) {
                    ois = new ObjectInputStream(in);
                    try {                        
                        this.groups = (ArrayList<String>)ois.readObject();
                        clearGroupListModel();
                        Object[] groupsArray = groups.toArray();
                        String[] groupsStringArray = new String[groupsArray.length];
                        for (int i = 0; i < groupsArray.length; i++)
                           groupsStringArray[i] = (String)groupsArray[i];
                        java.util.Arrays.sort(groupsStringArray);
                        for (int i = 0; i < groupsStringArray.length; i++)
                           groupListModel.addElement(groupsStringArray[i]);
                    } catch(ClassNotFoundException e) {
                        error(e.toString());
                        System.out.println(e.toString());
                    }
                }
                else if (name.equals(GROUP_MAP_FILE_NAME)) {
                    ois = new ObjectInputStream(in);
                    try {
                        this.groupsMap = (HashMap<String, String>)ois.readObject();
                    } catch(ClassNotFoundException e) {
                        error(e.toString());
                        System.out.println(e.toString());
                    }
                }

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
        groupvalueChanged(null);
    }

    /**
     * Returns a unique name for an ROI. If the name
     * already exists in the list of ROIs, the method
     * will append a "-1" to the name so that it is unique.
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

    /** Returns the ParticlesManager.*/
    public ParticlesManager getParticlesManager() {
        return partManager;
    }

    /** Returns the SquaresManager.*/
    public SquaresManager getSquaresManager() {
        return squaresManager;
    }

    /**
     * Saves all ROIs.
     *
     * @return <code>true</code> if successful, otherwise <code>false</code>.
     */
    public boolean save() {
        ImagePlus imp = getImage();
        if (imp == null)
           return false;

        if (rois.size() == 0) {
            return error("The selection list is empty.");
        }

        String path = ui.getImageDir();

        Roi[] tmprois = getAllROIs();
        boolean success = saveMultiple(tmprois, path, true);
        if (success)
           ui.setLastFolder(path);

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
            SaveDialog sd = new SaveDialog("Save ROIs...", path,
                    defaultname,
                    ".zip");
            String name = sd.getFileName();
            if (name == null) {
                return false;
            }
            if (!(name.endsWith(UI.ROIS_EXTENSION))) {
                name = name + UI.ROIS_EXTENSION;
            }
            String dir = sd.getDirectory();
            path = (new File(dir, name)).getAbsolutePath();
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

            out.close();
            savedpath = path;
            previouslySaved = true;
            resetTitle();
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

    /** Resets the title of the manager.*/
    private void resetTitle() {
        String name = savedpath.substring(savedpath.lastIndexOf("/")+1, savedpath.length());
        this.setTitle("MIMS ROI Manager: "+name);
    }

    /**
     * The action method for the "mesure" button. Takes some basic
     * measurements of the currentImage and generates a table for display.
     */
    private void measure() {

       // initialize table.
       if (table == null)
         table = new MimsJTable(ui);

       // Get current plane.
       ImagePlus imp = getImage();
       if (imp == null)
          return;

       ArrayList planes = new ArrayList<Integer>();
       planes.add(ui.getOpenMassImages()[0].getCurrentSlice());
       table.setPlanes(planes);

       // Get selected stats.
       String[] statnames = ui.getmimsTomography().getStatNames();
       table.setStats(statnames);
       
       // Get rois.
       Roi[] rois = getSelectedROIs();
       if (rois.length >= 1) {
          table.setRois(rois);
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
     * The action method for the "duplicate" button.
     * Duplicates the current ROI.
     */
    private void duplicate() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return;
        }

        Roi roi = imp.getRoi();

        if (roi == null) {
            error("The active image does not have a selection.");
            return;
        }

        Roi roi2 = (Roi)roi.clone();

        String name = roi2.getName();
        String label = name;
        label = getUniqueName(label);
        if (label == null) {
            return;
        }
        roiListModel.addElement(label);
        roi2.setName(label);
        Calibration cal = imp.getCalibration();
        if (cal.xOrigin != 0.0 || cal.yOrigin != 0.0) {
            Rectangle r = roi2.getBounds();
            roi2.setLocation(r.x - (int) cal.xOrigin, r.y - (int) cal.yOrigin);
        }
        rois.put(label, roi2);
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Add");
        }
        return;
    }


    private void reorderAll() {       
       int[] idxs = roijlist.getSelectedIndices();
       for (int i = 0; i <  idxs.length; i++) {
          int j = idxs[i];
          roijlist.setSelectedIndex(j);
          int count = 1;
          while (true) {
             boolean success = rename(Integer.toString(count));
             count++;
             if (success)               
                break;
          }
       }
    }

    /**
     * The action method for the "combine" button.
     * Combines several ROIs into one.
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
                if (roi instanceof ShapeRoi) {
                    s1 = (ShapeRoi) roi;
                } else {
                    s1 = new ShapeRoi(roi);
                }
                if (s1 == null) {
                    return;
                }
            } else {
                if (roi instanceof ShapeRoi) {
                    s2 = (ShapeRoi) roi;
                } else {
                    s2 = new ShapeRoi(roi);
                }
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
     * The action method for the "split" button.
     * Must be a ROI of composite type.
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
        Roi[] rois = ((ShapeRoi) roi).getRois();
        for (int i = 0; i < rois.length; i++) {
            imp.setRoi(rois[i]);
            add();
        }
    }

    /** The the indices for all ROIS. (Not sure why this is needed, leaving for now). */
    private int[] getAllIndexes() {
        int count = roiListModel.size();
        int[] indexes = new int[count];
        for (int i = 0; i < count; i++) {
            indexes[i] = i;
        }
        return indexes;
    }

    /** Documentation Required. */
    public void roiThreshold(Roi roi, MimsPlus img, double[] params) {
        //check param size
        if(params.length!=5) return;
        double mint = params[0];
        double maxt = params[1];
        double mins = params[2];
        double maxs = params[3];
        int diag = (int)params[4];

        img.setRoi(roi, true);
        ImageProcessor imgproc = img.getProcessor();

        int imgwidth = img.getWidth();
        int imgheight =img.getHeight();
        float[][] pix = new float[imgwidth][imgheight];

        //get pixel values
        for(int i = 0; i < imgwidth; i++) {
            for(int j = 0; j < imgheight; j++) {
                pix[i][j] = (float)imgproc.getPixelValue(i, j);
            }
        }

        //apply "mask"
        for (int j = 0; j < imgheight; j++) {
            for (int i = 0; i < imgwidth; i++) {
                if (!roi.contains(i, j)) {
                    pix[i][j] = 0;
                }
            }
        }

        //threshold image
        FloatProcessor proc = new FloatProcessor(pix);
        proc.setThreshold(mint, maxt, FloatProcessor.NO_LUT_UPDATE);

        ImagePlus temp_img = new ImagePlus("temp_img", proc);
        //generate rois
        int options = ParticleAnalyzer.ADD_TO_MANAGER;
        if(diag!=0) {
            options = options | diag;
        }
        //options = options | ParticleAnalyzer.INCLUDE_HOLES;
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

    }

    /** Documentation Required. */
    public void roiThreshold(Roi[] rois, MimsPlus img, double[] params) {
        for(int i = 0; i < rois.length; i++) {
            roiThreshold(rois[i], img, params);
        }
    }

    /** Documentation Required. */
    public boolean roiOverlap(Roi r, Roi q) {
        boolean overlap = false;
        if((r.getType()==Roi.RECTANGLE) && (q.getType()==Roi.RECTANGLE)) {
            overlap = r.getBoundingRect().intersects(q.getBoundingRect());
        }
        if(!(r.getType()==Roi.RECTANGLE) || !(q.getType()==Roi.RECTANGLE)) {
            int x = r.getBoundingRect().x;
            int y = r.getBoundingRect().y;
            int w = r.getBoundingRect().width;
            int h = r.getBoundingRect().height;

            for (int ix = x; ix <= x + w; ix++) {
                for (int iy = y; iy <= y + h; iy++) {
                    overlap = overlap || q.contains(ix, iy);
                    if(overlap) break;
                }
                if(overlap) break;
            }
        }

        return overlap;
    }

    /** Documentation Required. */
    public boolean roiOverlap(Roi r, Roi[] rois) {
        boolean overlap = false;
        for(int i = 0; i < rois.length; i++) {
            overlap = ( overlap || roiOverlap(r, rois[i]) );
        }
        return overlap;
    }

    /** Documentation Required. */
    public Roi[] roiSquares(Roi roi, MimsPlus img, double[] params) {
        //check param size
        if(params.length!=3) return null;
        //params = size, number, overlap
        int size = (int)Math.round(params[0]);
        int num = (int)Math.round(params[1]);
        boolean allowoverlap = (params[2]==1.0);

        img.setRoi(roi, true);

        //roi bounding box
        int x = roi.getBoundingRect().x;
        int y = roi.getBoundingRect().y;
        int width = roi.getBoundingRect().width;
        int height = roi.getBoundingRect().height;
        float[][] pix = new float[width][height];

        //get pixel values
        //using x,y offsets
        for(int i = 0; i < width; i++) {
            for(int j = 0; j < height; j++) {
                pix[i][j] = (float)img.getProcessor().getPixelValue(i+x, j+y);
            }
        }

        //apply "mask" which is a somewhat arbbitrary value
        // of -1.0E35, very negative but "far" from Float.min_value
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (!roi.contains(i+x, j+y)) {
                    //pix[i][j] = 0;
                    pix[i][j] = (float)-1.0E35;
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
        for(int ix=0; ix<=width-size; ix++) {
            for(int iy=0; iy<=height-size; iy++) {
                Roi troi = new Roi(ix, iy, size, size);
                temp_img.setRoi(troi);
                nrois++;
                
                 //throw out any roi containing pixels outside
                //the original roi by checking if min is "close" to mask value
                double min = temp_img.getStatistics().min;
                if(min < -1.0E30) {
                    //System.out.println("min == "+min);
                    rejected++;
                    continue;
                }

                //get mean value and add to hash
                double mean = temp_img.getStatistics().mean;
                //System.out.println("mean = "+mean);
                
                //exclude rois outside main roi
                if(roihash.containsKey(mean)) {
                    roihash.get(mean).add(troi);
                } else {
                    ArrayList<Roi> ar = new ArrayList<Roi>();
                    ar.add(troi);
                    roihash.put(mean, ar);
                }
            }
        }

        System.out.println("nrois = "+nrois+" | rejected = "+rejected);
        
        Object[] means = roihash.keySet().toArray();
        java.util.Arrays.sort(means);

        ArrayList<Roi> keep = new ArrayList<Roi>();
        //find highest mean rois
        if(allowoverlap) {
            int meanindex = means.length-1;
            while(keep.size()<num) {
                //if((Double)means[meanindex]<0) continue;
                ArrayList fromhash = roihash.get(means[meanindex]);
                for(Object r : fromhash) {
                    keep.add((Roi)r);
                    if(keep.size()==num)
                        break;
                }
                meanindex--;
                if(meanindex<0) break;
            }

        } else {
            //find highest mean rois checking for overlap
            if(means.length==0) {
                System.out.println("No rois found for roi: "+roi.getName());
                return null;
            }

            int meanindex = means.length-1;
            Roi lastadded = roihash.get(means[meanindex]).get(0);
            keep.add(lastadded);

            while(keep.size()<num) {
                ArrayList fromhash = roihash.get(means[meanindex]);
                for (Object r : fromhash) {
                    Roi[] keeparray = new Roi[keep.size()];
                    keeparray = keep.toArray(keeparray);

                    if (!roiOverlap((Roi)r, keeparray)) {
                        keep.add((Roi)r);
                        lastadded = (Roi)r;
                    }

                    if (keep.size() == num) {
                        break;
                    }
                }
                meanindex--;
                if(meanindex<0) break;
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
        for(int i = 0; i < rrois.length; i++) {
            int rx = rrois[i].getBoundingRect().x;
            int ry = rrois[i].getBoundingRect().y;
            rrois[i].setLocation(x+rx, y+ry);
        }
        keep = null;
        return rrois;

    }

    /** Documentation Required. */
    public Roi[] roiSquares(Roi[] rois, MimsPlus img, double[] params) {
        ArrayList<Roi> temprois = new ArrayList<Roi>();
        for(int i = 0; i < rois.length; i++) {
            Roi[] r = roiSquares(rois[i], img, params);
            if(r==null) {
                System.out.println("roiSquares returned null.");
                continue;
            }
            for(int j = 0; j < r.length; j++) {
                temprois.add(r[j]);
            }
        }

        Roi[] rrois = new Roi[temprois.size()];
        temprois.toArray(rrois);
        temprois = null;

        System.out.println("input length: "+rois.length+" output length: "+rrois.length);

        return rrois;
    }

    //not used yet
    //still testing
    /** Documentation Required. */
    public Roi[] roiSquaresZ(Roi roi, MimsPlus img, double[] params) {


        //check param size
        if(params.length!=3) return null;
        //params = size, number, overlap
        int size = (int)Math.round(params[0]);
        int num = (int)Math.round(params[1]);
        boolean allowoverlap = (params[2] == 1.0);

        
        int stacksize = 1;
        if (img.getMimsType() == MimsPlus.MASS_IMAGE) {
            stacksize = img.getStackSize();
        } else if (img.getMimsType() == MimsPlus.RATIO_IMAGE) {
            stacksize = ui.getMassImage(img.getRatioProps().getNumMassIdx()).getStackSize();
        } else if (img.getMimsType() == MimsPlus.HSI_IMAGE) {
            stacksize = ui.getMassImage(img.getHSIProps().getNumMassIdx()).getStackSize();
        }

        
        img.setRoi(roi, true);
        ImageProcessor imgproc = img.getProcessor();

        int imgwidth = img.getWidth();
        int imgheight =img.getHeight();

        //generate rois keyed to mean
        Hashtable<Double, ArrayList<Roi>> roihash = new Hashtable<Double, ArrayList<Roi>>();

        for (int p = 1; p <= stacksize; p++) {
            //same setslice code as MimsJFreeChart.getdataset()
            if (img.getMimsType() == MimsPlus.MASS_IMAGE) {
               ui.getOpenMassImages()[0].setSlice(p, false);
            } else if (img.getMimsType() == MimsPlus.RATIO_IMAGE) {
               ui.getOpenMassImages()[0].setSlice(p, img);
            }

            float[][] pix = new float[imgwidth][imgheight];

            //get pixel values
            for (int i = 0; i < imgwidth; i++) {
                for (int j = 0; j < imgheight; j++) {
                    pix[i][j] = (float) imgproc.getPixelValue(i, j);
                }
            }


            //apply "mask"
            for (int j = 0; j < imgheight; j++) {
                for (int i = 0; i < imgwidth; i++) {
                    if (!roi.contains(i, j)) {
                        //pix[i][j] = 0;
                        pix[i][j] = Float.NaN;
                    }

                }
            }
            

            //generate temp image
            FloatProcessor proc = new FloatProcessor(pix);
            ImagePlus temp_img = new ImagePlus("temp_img", proc);

            //roi bounding box
            int x = roi.getBoundingRect().x;
            int y = roi.getBoundingRect().y;
            int width = roi.getBoundingRect().width;
            int height = roi.getBoundingRect().height;


            for (int ix = x; ix <= x + width; ix++) {
                for (int iy = y; iy <= y + height; iy++) {
                    Roi troi = new Roi(ix, iy, size, size);
                    temp_img.setRoi(troi);
                    double mean = temp_img.getStatistics().mean;
                    //System.out.println("mean = "+mean);
                    //System.out.println("Double.isNaN(mean) = "+Double.isNaN(mean));
                    //exclude rois outside main roi
                    if (Double.isNaN(mean)) {
                        continue;
                    }
                    if (roihash.containsKey(mean)) {
                        roihash.get(mean).add(troi);
                    } else {
                        ArrayList<Roi> ar = new ArrayList<Roi>();
                        ar.add(troi);
                        roihash.put(mean, ar);
                    }
                }
            }



            //cleanup
            temp_img.close();
            temp_img = null;
            proc = null;
            pix = null;

            //int start=0;
            if(roihash.size()>(num*num)) {
                //start = roihash.size();
                //System.out.println("hash.size: " + roihash.size());
                Object[] means = roihash.keySet().toArray();
                java.util.Arrays.sort(means);
                int index = 0;
                while(roihash.size()>(num*num)) {
                    roihash.remove(means[index]);
                    index++;
                }
            }
            //int end = roihash.size();
            //System.out.println("end size: "+end);
            //System.out.println("delta: "+(start-end));
            
        }

        //double[] means = (double[])roihash.keySet().toArray();
        Object[] means = roihash.keySet().toArray();
        java.util.Arrays.sort(means);

        ArrayList<Roi> keep = new ArrayList<Roi>();
        //find highest mean rois
        if(allowoverlap) {
            int meanindex = means.length-1;
            while(keep.size()<num) {
                //if((Double)means[meanindex]<0) continue;
                ArrayList fromhash = roihash.get(means[meanindex]);
                for(Object r : fromhash) {
                    keep.add((Roi)r);
                    if(keep.size()==num)
                        break;
                }
                meanindex--;
                if(meanindex<0) break;
            }

        } else {
            //find highest mean rois checking for overlap
            int meanindex = means.length-1;
            Roi lastadded = roihash.get(means[meanindex]).get(0);
            keep.add(lastadded);

            while(keep.size()<num) {
                ArrayList fromhash = roihash.get(means[meanindex]);
                for (Object r : fromhash) {
                    Roi[] keeparray = new Roi[keep.size()];
                    keeparray = keep.toArray(keeparray);

                    if (!roiOverlap((Roi)r, keeparray)) {
                        keep.add((Roi)r);
                        lastadded = (Roi)r;
                    }

                    if (keep.size() == num) {
                        break;
                    }
                }
                meanindex--;
                if(meanindex<0) break;
            }

        }


        Roi[] rrois = new Roi[keep.size()];
        keep.toArray(rrois);

        //more cleanup
        means = null;
        keep = null;
        roihash = null;

        return rrois;

    }

    /** Documentation Required. */
    public Roi[] roiSquaresZ(Roi[] rois, MimsPlus img, double[] params) {
        ArrayList<Roi> temprois = new ArrayList<Roi>();
        for(int i = 0; i < rois.length; i++) {
            Roi[] r = roiSquaresZ(rois[i], img, params);
            for(int j = 0; j < r.length; j++) {
                temprois.add(r[j]);
            }
        }

        Roi[] rrois = new Roi[temprois.size()];
        temprois.toArray(rrois);
        return rrois;

    }

    /**
     * Generates a table of pixel values for a
     * ROI for the current image and plane.
     */
    private void roiPixelvalues() {
        MimsPlus img = null;
        try{
            img = (MimsPlus)WindowManager.getCurrentImage();
        } catch(Exception E){
            return;
        }
        roiPixelvalues(img);
    }

    /**
     * Generates a table of pixel values for a
     * ROI for the passed image and current plane.
     *
     * @param img the image.
     */
    public void roiPixelvalues(MimsPlus img) {
        if(img.getMimsType()==MimsPlus.HSI_IMAGE) {
            roiPixelvalues(img.internalRatio);
            return;
        }

        img.killRoi();
        Roi[] rois = this.getSelectedROIs();
        if(rois==null) return;
        if(rois.length == 0) return;

        // Collect all the pixels within the highlighted rois.
        ArrayList<Double> values = new ArrayList<Double>();
        ArrayList<String> groups = new ArrayList<String>();
        for (Roi roi : rois) {
            img.setRoi(roi);
            double[] roipixels = img.getRoiPixels();
            String group = getRoiGroup(roi.getName());
            for (double pixel : roipixels) {
                values.add(pixel);
                groups.add(group);
            }
            img.killRoi();
        }

        // Setup table.
        ij.measure.ResultsTable rTable = new ij.measure.ResultsTable();
        rTable.addColumns();
        rTable.setHeading(0, "Group");
        rTable.setHeading(1, "Value");

        // Fill in the table.
        for(int i = 0; i<values.size(); i++) {
            rTable.incrementCounter();
            String group = groups.get(i);
            if (group == null)
               group = "null";
            rTable.setLabel(group, i);
            rTable.addValue(1, values.get(i));
        }

        // Set some parameters in case user wants to save table.
        Prefs.set("options.ext", ".txt");
        ui.setIJDefaultDir(ui.getImageDir());

        // Set title and show table.
        String title = "";
        if (img.getMimsType() == MimsPlus.MASS_IMAGE)
           title = ui.getImageFilePrefix() + "_m"+img.getRoundedTitle();
        if (img.getMimsType() == MimsPlus.RATIO_IMAGE)
           title = ui.getImageFilePrefix() + "_m"+img.getNumeratorImage().getRoundedTitle()+"_m"+img.getDenominatorImage().getRoundedTitle();
        if (rois.length == 1)
           title += "_roi"+rois[0].getName();
        rTable.show(title);
        
    }

    /** Gets the current image. */
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
     * Updates plots representing data for an ROI.
     * Should be called in cases where pixels contained
     * within an ROI changes (for example, location change
     * or size change).
     *
     * @param force forces update.
     */
    void updatePlots(boolean force) {
        MimsPlus imp;
        try{
            imp = (MimsPlus)this.getImage();
        } catch(ClassCastException e) {
            return;
        }
        if(imp == null) return;

        Roi roi = imp.getRoi();
        if(roi == null) return;
        double[] roipix = imp.getRoiPixels();

        if ((roi.getType() == Roi.LINE) || (roi.getType() == Roi.POLYLINE) || (roi.getType() == Roi.FREELINE)) {
            ij.gui.ProfilePlot profileP = new ij.gui.ProfilePlot(imp);
            ui.updateLineProfile(profileP.getProfile(), imp.getShortTitle() + " : " + roi.getName(), imp.getProcessor().getLineWidth());
        } else {
            String label = imp.getShortTitle() + " ROI: " + roi.getName();
            ui.getmimsTomography().updateHistogram(roipix, label, force);
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

    @Override
    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        ignoreInterrupts = false;
    }

    /**
     * Returns a reference to the MIMS ROI Manager
     * or null if it is not open.
     */
    public static MimsRoiManager getInstance() {
        return (MimsRoiManager) instance;
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
     * Returns  the ROI with name <code>name</code>
     *
     * @return the ROI.
     */
    public Roi getRoiByName(String name) {
        return (Roi)rois.get(name);
    }

    /** 
     * Gets all the selected ROIs.
     *
     * @return ROI array.
     */
    public Roi[] getSelectedROIs() {

       // initialize variables.
       Roi roi;
       Roi[] rois;

       // get selected indexes.
       int[] roiIndexes = roijlist.getSelectedIndices();
       if (roiIndexes.length == 0) {
          rois = new Roi[0];
       } else {
          rois = new ij.gui.Roi[roiIndexes.length];
          for (int i = 0; i < roiIndexes.length; i++) {
             roi = (ij.gui.Roi) getROIs().get(roijlist.getModel().getElementAt(roiIndexes[i]));
             rois[i] = roi;
          }
       }

       return rois;
    }

    /** 
     * Gets all ROIs regardless if they're selected or not.
     *
     * @return ROI array.
     */
    public Roi[] getAllROIs() {
        Object[] ob = rois.values().toArray();
        Roi[] r = new Roi[ob.length];
        for(int i = 0; i< ob.length; i++) {
            r[i] = (Roi)ob[i];
        }
        return r;
    }

    /** Returns the selection list. */
    public JList getList() {
        return roijlist;
    }

   /**
    * Call this method to find what index the specified
    * ROI label has in the jlist.
    *
    * @param label the ROI name.
    * @return the index.
    */
   public int getIndex(String label) {
      int count = roiListModel.getSize();
      for (int i = 0; i <= count - 1; i++) {
         String value = roiListModel.get(i).toString();
         if (value.equals(label)) return i;
      }
      return -1;
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
           roijlist.setSelectedIndex(index);
        }


        String label = roijlist.getSelectedValue().toString();

        // Make sure we have a ROI
        Roi roi = (Roi)rois.get(label);
        if (roi == null) return;
        else resetSpinners(roi);
    }

   /** Programmatically selects all ROIs in the list.*/
    public void selectAll() {
        int len = roijlist.getModel().getSize();
        if (len <= 0) {
            return;
        } else {
            roijlist.setSelectionInterval(0, len - 1);
        }
    }

   /**
    * Programmatically adds the ROI with index <code>index</code>
    * to the list of already selected ROIs
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
            for(int i = 0; i<selected.length; i++) {
                newselected.add(selected[i]);
            }
            if(newselected.contains(index)) {
                newselected.remove((Object)index);
            } else {
                newselected.add(index);
            }
            int[] ns = new int[newselected.size()];
            for(int i = 0; i<ns.length; i++) {
                ns[i]=(Integer)newselected.get(i);
            }
            roijlist.setSelectedIndices(ns);
        }

        selectedRoisStats();

    }

   /**
    * Determines if the ROI with name <code>name</name> is
    * already selected in the manager.
    *
    * @param name the ROI name.
    * @return <code>true</code> if selected, otherwise <code>false</false>
    */
    public boolean isSelected(String name) {
        boolean b = false;
        Object[] selectednames = roijlist.getSelectedValues();

        for(int i=0; i<selectednames.length; i++) {
            String sname = (String)selectednames[i];
            if(name.equals(sname)) {
                b = true;
                return b;
            }
        }

        return b;
    }
    
    /** Overrides PlugInFrame.close(). */
    @Override
    public void close() {
        this.setVisible(false);
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

    /**Shows the ROI manager frame.*/
    public void showFrame() {
        setVisible(true);
        toFront();
        setExtendedState(NORMAL);
    }

    /**
     * This class controls how ROIs are listed and displayed in
     * the jlist. It uses html so that an index is displayed in
     * light grey followed by the name of the ROI in black. Prefereably
     * the jlist would have the index built into it but I can
     * not find any implementation of the jlist that does that.
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
         int idx = index + 1;

         // My attempt at creating a numbered list...
         /*
         Color bg = rm.getBackground();        
         String hexStr = Integer.toHexString( bg.getRGB() );
         hexStr = hexStr.substring(2, hexStr.length());
         
         System.out.println(hexStr);
         String idxStr = Integer.toString(idx);
         if (idxStr.length()==1)
            idxStr = "0"+idxStr;
         setText("<html> <BGCOLOR="+"#"+hexStr+"> <font color=gray>"+idxStr+"</font> <font color=black>"+" "+label+"</font></html>");
         */
         // Attempt failed.
         setText("<html> <font color=gray>"+"("+idx+")"+"</font> <font color=black>"+" "+label+"</font></html>");


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

    /** Documentation Required. */
    public class ParticlesManager extends com.nrims.PlugInJFrame implements ActionListener {

        Frame instance;

        MimsPlus workingimage;

        JLabel label;
        JTextField threshMinField = new JTextField();
        JTextField threshMaxField = new JTextField();
        JTextField sizeMinField = new JTextField();
        JTextField sizeMaxField = new JTextField();
        JCheckBox allowDiagonal = new JCheckBox("Allow Diagonal Connections", false);
        JButton cancelButton;
        JButton okButton;

        public ParticlesManager() {
            super("Particles Manager");

            if (instance != null) {
                instance.toFront();
                return;
            }
            instance = this;

            // Setup panel.
            JPanel jPanel = new JPanel();
            jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

            try{
                workingimage = (MimsPlus)getImage();
            } catch(Exception e){ return; }

            String imagename = workingimage.getTitle();
            label = new JLabel("Image:   " + imagename);
            jPanel.add(label);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            //add textfields
            JLabel label2 = new JLabel("Threshold min");
            jPanel.add(label2);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(threshMinField);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            JLabel label3 = new JLabel("Threshold max");
            jPanel.add(label3);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(threshMaxField);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            JLabel label4 = new JLabel("Size min");
            jPanel.add(label4);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(sizeMinField);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            JLabel label5 = new JLabel("Size max");
            jPanel.add(label5);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(sizeMaxField);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            jPanel.add(allowDiagonal);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));


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
            setSize(new Dimension(300, 375));

        }

        // Gray out textfield when "All" images radio button selected.
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Cancel")) {
                closeWindow();
            } else if (e.getActionCommand().equals("OK")) {
                //
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    double mint = Double.parseDouble(threshMinField.getText());
                    double maxt = Double.parseDouble(threshMaxField.getText());
                    double mins = Double.parseDouble(sizeMinField.getText());
                    double maxs = Double.parseDouble(sizeMaxField.getText());
                    double diag = 0;
                    if(allowDiagonal.isSelected()){ diag = 0; } else { diag = ij.plugin.filter.ParticleAnalyzer.FOUR_CONNECTED; }

                    double[] params = {mint, maxt, mins, maxs, diag};

                    Roi[] rois = getSelectedROIs();
                    MimsPlus img = (MimsPlus)getImage();
                    if(img.getMimsType()==MimsPlus.HSI_IMAGE && img.internalRatio!=null) {
                        roiThreshold(rois, img.internalRatio, params);
                    } else {
                        roiThreshold(rois, img, params);
                    }

                } catch(Exception x) {
                    ij.IJ.error("Error", "Not a number.");
                      return;
                } finally {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }

                ui.updateAllImages();
                closeWindow();
            }

        }

        public void resetImage() {
            try{
                workingimage = (MimsPlus)getImage();
            } catch(Exception e){ return; }

            label.setText(workingimage.getTitle());
        }

        public void resetImage(MimsPlus img) {
            workingimage = img;
            label.setText(workingimage.getTitle());
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

    /** Documentation Required. */
    public class SquaresManager extends com.nrims.PlugInJFrame implements ActionListener {

        Frame instance;
        MimsRoiManager rm;

        MimsPlus workingimage;
        JLabel label;
        JTextField sizeField = new JTextField();
        JTextField numberField = new JTextField();
        JTextField rangeField = new JTextField();
        JCheckBox allowOverlap = new JCheckBox("Allow Overlap", false);

        JButton cancelButton;
        JButton SButton;
        JButton ZButton;

        public SquaresManager(MimsRoiManager rm) {
            super("Squares Manager");
            this.rm = rm;
            
            if (instance != null) {
                instance.toFront();
                return;
            }
            instance = this;

            // Setup panel.
            JPanel jPanel = new JPanel();
            jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

            try{
                workingimage = (MimsPlus)getImage();
            } catch(Exception e){ return; }

            String imagename = workingimage.getTitle();
            label = new JLabel("Image:   " + imagename);
            jPanel.add(label);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            //add textfields
            JLabel label2 = new JLabel("Square size");
            jPanel.add(label2);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(sizeField);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            JLabel label3 = new JLabel("Number of squares");
            jPanel.add(label3);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(numberField);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            jPanel.add(allowOverlap);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            /*
            JLabel label4 = new JLabel("Range:");
            jPanel.add(label4);
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            jPanel.add(rangeField);
            jPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            jPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            */

            // Set up "OK" and "Cancel" buttons.
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

            buttonPanel.add(cancelButton);
            buttonPanel.add(SButton);
            buttonPanel.add(ZButton);
            //disabled till it actually works
            ZButton.setEnabled(false);

            // Add elements.
            setLayout(new BorderLayout());
            add(jPanel, BorderLayout.PAGE_START);
            add(buttonPanel, BorderLayout.PAGE_END);
            setSize(new Dimension(300, 350));

        }

        
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Cancel")) {
                closeWindow();
            } else if (e.getActionCommand().equals("Squares")) {
                //
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    double size = Double.parseDouble(sizeField.getText());
                    double num = Double.parseDouble(numberField.getText());
                    double overlap = 0.0;
                    if(allowOverlap.isSelected()) { overlap = 1.0; }


                    double[] params = {size, num, overlap};

                    Roi[] rois = rm.getSelectedROIs();

                    if(rois.length==0) {
                        ij.IJ.error("Error", "No rois selected.");
                        return;
                    }

                    MimsPlus img = (MimsPlus)getImage();

                    if(img.getMimsType()==MimsPlus.HSI_IMAGE && img.internalRatio!=null) {
                        rm.add(roiSquares(rois, img.internalRatio, params));
                    } else {
                        rm.add(roiSquares(rois, img, params));
                    }

                } catch(Exception x) {
                    x.printStackTrace();
                    ij.IJ.error("Error", "Not a number.");
                    return;
                } finally {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }

                ui.updateAllImages();
                closeWindow();
            } else if (e.getActionCommand().equals("SquaresZ")) {

                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    double size = Double.parseDouble(sizeField.getText());
                    double num = Double.parseDouble(numberField.getText());
                    double overlap = 0.0;
                    if(allowOverlap.isSelected()) { overlap = 1.0; }


                    double[] params = {size, num, overlap};

                    Roi[] rois = rm.getSelectedROIs();

                    if(rois.length==0) {
                        ij.IJ.error("Error", "No rois selected.");
                        return;
                    }

                    MimsPlus img = (MimsPlus)getImage();

                    if(img.getMimsType()==MimsPlus.HSI_IMAGE && img.internalRatio!=null) {
                        rm.add(roiSquaresZ(rois, img.internalRatio, params));
                    } else {
                        rm.add(roiSquaresZ(rois, img, params));
                    }

                } catch(Exception x) {
                    x.printStackTrace();
                    ij.IJ.error("Error", "Not a number.");
                    return;
                } finally {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }

                ui.updateAllImages();
                closeWindow();

            }

        }

        public void resetImage() {
            try{
                workingimage = (MimsPlus)getImage();
            } catch(Exception e){ return; }

            label.setText(workingimage.getTitle());
        }

        public void resetImage(MimsPlus img) {
            workingimage = img;
            label.setText(workingimage.getTitle());
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


    //Various methods to deal with transitionsing to
    //simple numeric roi names

    //Compares roi's by name for sorting.
    //Roi's with int-castable names are compared as ints
    //Roi's with non-int-castable names are ??????????????????????????
    public abstract class RoiNameComparator implements Comparator<Roi> {
        public int compare(Roi roia, Roi roib) {
            
            //this should never actually happen
            //since roi names are forced to be unique
            if(roia.getName().equals(roib.getName())) {
                return 0;
            }
            int a=0, b=0;
            try{
                a = Integer.parseInt(roia.getName());
                b = Integer.parseInt(roib.getName());
            }catch(Exception e){
                return 0;

            }

            if(a<b) return -1;
            if(a>b) return 1;
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
        if (tmprois.length == 0)
           return null;
        else
           return tmprois[tmprois.length-1];
    }

    /**
     * Returns an array of ROIs that have numeric names.
     *
     * @return the ROI array with numeric names.
     */
     public Roi[] getNumericRois() {
        Roi[] tmprois = getAllROIs();
        ArrayList<Roi> numrois = new ArrayList<Roi>();

        for(int i = 0; i< tmprois.length; i++) {
            if(hasNumericName(tmprois[i]))
                numrois.add(tmprois[i]);
        }

        Roi[] returnrois = new Roi[numrois.size()];

        for(int i = 0; i< returnrois.length; i++) {
            returnrois[i]=(Roi)numrois.get(i);
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

        Comparator<Roi> byName = new RoiNameComparator(){};
        java.util.Arrays.sort(returnrois, byName);

        return returnrois;
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
        }catch(Exception e){
            return false;
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
        }catch(Exception e){
            return false;
        }
    }

}

