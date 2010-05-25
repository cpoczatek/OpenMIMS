package com.nrims;

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
import javax.swing.JOptionPane;
import javax.swing.JTextField;


/**
 * This plugin replaces the Analyze/Tools/ROI Manager command.
 * Intent to enable interaction with CustomCanvas to show all rois..
 */
public class MimsRoiManager extends PlugInJFrame implements ActionListener,
        MouseListener {

    JPanel panel;
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
    JCheckBox cbHideAll;
    JCheckBox cbAllPlanes;
    JSpinner xPosSpinner, yPosSpinner, widthSpinner, heightSpinner;
    JLabel xLabel, yLabel, wLabel, hLabel;
    boolean holdUpdate = false;
    private UI ui = null;
    private com.nrims.data.Opener image = null;
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

    public MimsRoiManager(UI ui, com.nrims.data.Opener im) {
        super("MIMS ROI Manager");

        this.ui = ui;
        this.image = im;

        if (instance != null) {
            instance.toFront();
            return;
        }
        instance = this;
        ImageJ ij = IJ.getInstance();
        addKeyListener(ij);
        addMouseListener(this);
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
        groupjlist.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
           public void valueChanged(ListSelectionEvent listSelectionEvent) {
             groupvalueChanged(listSelectionEvent);
           }
        });

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
         JButton rename = new JButton("Rename");
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

    // Creates a new group.
    void createActionPerformed(ActionEvent e) {
        String s = (String)JOptionPane.showInputDialog(this,"Enter new group name:\n","Enter",
                    JOptionPane.PLAIN_MESSAGE,null,null,"");
        if (s == null || s == "")
           return;
        s = s.trim();
        if (groups.contains(s))
           return;
        else {
           groups.add(s);
           groupListModel.addElement(s);
        }
    }

    // Deletes selected groups and all associations.
    void deleteActionPerformed(ActionEvent e) {
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

    void renameActionPerformed(ActionEvent e) {
        int[] idxs = groupjlist.getSelectedIndices();
        if(idxs.length==0) return;
        int index = idxs[0];
        String groupName = (String)groupListModel.get(index);
        if(groupName.equals(DEFAULT_GROUP)) return;

        String newName = (String)JOptionPane.showInputDialog(this,"Enter new name for group "+ groupName +" :\n","Enter",
                    JOptionPane.PLAIN_MESSAGE,null,null,"");
        if (newName == null || newName.equals("") )
           return;
        newName = newName.trim();
        
        //collect all rois of that group
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

        //deassign all rois
        for(int i = 0; i<grprois.size(); i++) {
                groupsMap.remove(grprois.get(i).getName());
        }
        //reassign all rois
        for(int i = 0; i<grprois.size(); i++) {
            groupsMap.put(grprois.get(i).getName(), newName);
        }

        //update groups
        groups.remove(groupName);
        groups.add(newName);

        //update list
        groupListModel.setElementAt(newName, index);
    }

    // Assigns all selected rois to the selected group.
    void deassignActionPerformed(ActionEvent e) {

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

    // Assigns all selected rois to the selected group.
    void assignActionPerformed(ActionEvent e) {

       // Must have at least 1 roi selected to make an assignment.
       int[] Roiidxs = roijlist.getSelectedIndices();
       if (Roiidxs.length < 1)
          return;

       // Get all the possible groups.
       Object[] possibilities = groupListModel.toArray();

       // Construct and display dialog box.
       String roiList = "Assign the following Rois:\n\n";
       for (int i = 0; i < Roiidxs.length; i++)
          roiList += "\t\t" + (String)roiListModel.get(Roiidxs[i]) + "\n";
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

    }

    void setupPosSpinners() {
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

    void setupSizeSpinners() {
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

    void setupPosLabels() {
        xLabel = new JLabel("X Pos.");
        yLabel = new JLabel("Y Pos.");
        xLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        yLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        xLabel.setPreferredSize(new Dimension(90, 15));
        yLabel.setPreferredSize(new Dimension(90, 15));

        panel.add(xLabel);
        panel.add(yLabel);
    }

    void setupSizeLabels() {
        wLabel = new JLabel("Width");
        hLabel = new JLabel("Height");
        wLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        hLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        wLabel.setPreferredSize(new Dimension(90, 15));
        hLabel.setPreferredSize(new Dimension(90, 15));

        panel.add(wLabel);
        panel.add(hLabel);
    }

   void updateRoiLocations(boolean prepend) {

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

   void updateRoiLocations() {

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
             System.out.println("removing 1...");
            xylist.remove(xylist.size()-1);
         }
      }
   }

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

   // Use this method instead of groupListModel.clear() because we do not
   // want the "..." listing to dissapear, because it represents all Rois.
   private void clearGroupListModel() {
      for (int i = groupListModel.getSize()-1; i >= 0; i--) {
         String group = (String)groupListModel.get(i);
         if (!group.equals(DEFAULT_GROUP)) {
            groupListModel.remove(i);
         }
      }
   }

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
      Roi temproi = (Roi) roi.clone();
      temproi.setLocation((Integer) xPosSpinner.getValue(), (Integer) yPosSpinner.getValue());
      imp.setRoi(temproi);

      updatePlots(false);

    }

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
            newroi = new ij.gui.Roi(rect.x, rect.y, (Integer) widthSpinner.getValue(), (Integer) heightSpinner.getValue(), imp);
        } else if (oldroi.getType() == ij.gui.Roi.OVAL) {
            newroi = new ij.gui.OvalRoi(rect.x, rect.y, (Integer) widthSpinner.getValue(), (Integer) heightSpinner.getValue());
        } else {
           return;
        }

        //we give it the old name so that setRoi will
        // know which original roi to delete.
        newroi.setName(oldroi.getName());
        move(imp, newroi);
        updatePlots(false);
    }

    void addCheckbox(String label, boolean bEnabled) {
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
        }
        cb.setSelected(bEnabled);
        cb.addActionListener(this);
        panel.add(cb);
    }

    void addButton(String label) {
        JButton b = new JButton(label);
        b.setMargin( new Insets(0, 0, 0, 0) );
        b.setPreferredSize(new Dimension(90, 30));
        b.setMaximumSize(b.getPreferredSize());
        b.setMinimumSize(b.getPreferredSize());
        b.addActionListener(this);
        b.addKeyListener(IJ.getInstance());
        b.addMouseListener(this);
        if (label.equals("More>>")) {
            moreButton = b;
        }
        panel.add(b);
    }

    void addPopupMenu() {
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
        addPopupItem("Save As");
        add(pm);
    }

    void addPopupItem(String s) {
        JMenuItem mi = new JMenuItem(s);
        mi.addActionListener(this);
        pm.add(mi);
    }

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
            //save(null); //opens with defaul imagej path
            String path = ui.getImageDir();
            save(path);
        } else if (command.equals("Measure")) {
            measure();
        } else if (command.equals("Deselect")) {
            select(-1);
            ui.updateAllImages();
        } else if (command.equals(hideAllRois)) {
            hideAll();
        } else if (command.equals("More>>")) {
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
            if(squaresManager==null) { squaresManager = new SquaresManager(); }
            squaresManager.showFrame();
        } else if (command.equals("Pixel values")) {
            roiPixelvalues();
        } else if (command.equals("Save As")) {
            String path = ui.getImageDir();
            previouslySaved = false;
            save(path);
        } else if (command.equals(moveAllRois)) {
            bAllPlanes = cbAllPlanes.isSelected();
        }
    }

    // Checks to see if the group assigned to roiName is contained within groupNames.
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

    // Get the group of roi with roiName.
    public String getRoiGroup(String roiName) {
       String group = (String) groupsMap.get(roiName);
       return group;
    }

    // Determines what hapens when a group entry is clicked.
    public void groupvalueChanged(ListSelectionEvent e) {
       if (!e.getValueIsAdjusting()) return;
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
              //keep track of numeric and non-numeric names separately
              if(isNumericName(roiName)) {
                  numeric.add(Integer.parseInt(roiName));
              } else {
                  names.add(roiName);
              }

          }
       }
       //add the Rois sorted numerically
       Collections.sort(numeric);
       for(int i=0; i<numeric.size(); i++) {
           roiListModel.addElement(""+numeric.get(i));
       }
       //add the Rois sorted lexigraphically
       Collections.sort(names);
       for(int i=0; i<names.size(); i++) {
           roiListModel.addElement(names.get(i));
       }

       // Disable delete button if Default Group is one of the groups selected.
       //this is why the delete button is a class variable andd the others aren't...
       if (defaultGroupSelected) {
          delete.setEnabled(false);

       } else {
          delete.setEnabled(true);
       }

       ui.updateAllImages();

       holdUpdate = false;
    }

    public void roivalueChanged(ListSelectionEvent e) {

        // DO NOTHING!!  Wait till we are done switching
        if (!e.getValueIsAdjusting()) return;

        boolean setSlice = false;
        holdUpdate = true;

        int[] indices = roijlist.getSelectedIndices();
        if (indices.length == 0) return;

        // Select ROI in the window
        int index = indices[indices.length - 1];
        if (index < 0) index = 0;
        if (ui.getSyncROIsAcrossPlanes()) setSlice = false;
        else setSlice = true;

        restore(index, setSlice);

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
        }

        holdUpdate = false;
    }

    public void selectedRoisStats() {

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

           //widthSpinner.setEnabled(true);
           //heightSpinner.setEnabled(false);

           java.awt.Rectangle rect = roi.getBoundingRect();
           xPosSpinner.setValue(rect.x);
           yPosSpinner.setValue(rect.y);

           //ij.gui.Line lineroi = (Line) roi;
           //widthSpinner.setValue(lineroi.getWidth());
       }
       else {
          disablespinners();
       }
       holdUpdate = false;
    }

    void enablespinners() {
       xPosSpinner.setEnabled(true);
       yPosSpinner.setEnabled(true);
       widthSpinner.setEnabled(true);
       heightSpinner.setEnabled(true);
    }

    void disablespinners() {
       xPosSpinner.setEnabled(false);
       yPosSpinner.setEnabled(false);
       widthSpinner.setEnabled(false);
       heightSpinner.setEnabled(false);
    }

    void enablePosSpinners() {
        xPosSpinner.setEnabled(true);
        yPosSpinner.setEnabled(true);
    }

    void disablePosSpinners() {
        xPosSpinner.setEnabled(false);
        yPosSpinner.setEnabled(false);
    }

    void enableSizeSpinners() {
        widthSpinner.setEnabled(true);
        heightSpinner.setEnabled(true);
    }

    void disableSizeSpinners() {
        widthSpinner.setEnabled(false);
        heightSpinner.setEnabled(false);
    }

    void showall() {
       if (getImage() != null) {
            ui.updateAllImages();
        }
    }

    void hideAll() {
       if (getImage() != null) {
            ui.updateAllImages();
        }
    }

   void setRoi(ImagePlus imp, Roi roi) {

      // ROI old name - based on its old bounding rect
      String label = roi.getName();

      // ROI new name - based on its new bounding rect
      //String newName = getLabel(imp, roi);
      //newName = getUniqueName(newName);
      //if (newName != null) roi.setName(newName);
      //else return;

      // update name in the jlist
      //int i = getIndex(oldName);
      //if (i < 0) return;
      //listModel.set(i, newName);

      // update rois hashtable with new ROI
      //rois.remove(oldName);
      //rois.put(newName, roi);

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

   boolean move(ImagePlus imp, Roi roi) {
      if (imp == null) return false;
      if (roi == null) return false;

       setRoi(imp, roi);

       // Debug
       if (Recorder.record) {
         Recorder.record("mimsRoiManager", "Move");
      }
      return true;
   }

    boolean move() {
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

    boolean add() {
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        Roi roi = imp.getRoi();
        if (roi == null) {
            error("The active image does not have a selection.");
            return false;
        }

        /*
        String name = roi.getName();
        if (isStandardName(name)) {
            name = null;
        }
        String label = name != null ? name : getLabel(imp, roi);
        label = getUniqueName(label);
        if (label == null) {
            return false;
        }
        */

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

        // Assign group.
        int[] indices = groupjlist.getSelectedIndices();
        if (indices.length > 0) {
           String group = (String)groupListModel.getElementAt(indices[0]);
           if (indices.length == 1 && !group.equals(DEFAULT_GROUP))
              groupsMap.put(label, group);
        }

        return true;
    }


    boolean add(Roi roi) {
        //don't really want to be looking at current image???
        //should be refactored along with getlabel()?
        ImagePlus imp = getImage();
        if (imp == null) {
            return false;
        }
        if (roi == null) {
            return false;
        }

        /*
        String name = roi.getName();
        if (isStandardName(name)) {
            name = null;
        }
        String label = name != null ? name : getLabel(imp, roi);
        label = getUniqueName(label);
        if (label == null) {
            return false;
        }
        */
        
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


    boolean isStandardName(String name) {
        if (name == null) {
            return false;
        }
        boolean isStandard = false;
        int len = name.length();
        if (len >= 14 && name.charAt(4) == '-' && name.charAt(9) == '-') {
            isStandard = true;
        } else if (len >= 9 && name.charAt(4) == '-') {
            isStandard = true;
        }
        return isStandard;
    }



    public void renameNumericRois() {
        Roi[] numrois = getNumericRoisSorted();
        int max = numrois.length;

        for(int i = 0; i<max; i++) {
            rename(numrois[i].getName(),""+(i+1));
        }


    }

    String getLabel(ImagePlus imp, Roi roi) {
        /*
        String label = "";
        label += this.getAllROIs().length;
        return label;

        */
        Rectangle r = roi.getBounds();
        int xc = r.x + r.width / 2;
        int yc = r.y + r.height / 2;
        if (xc < 0) {
            xc = 0;
        }
        if (yc < 0) {
            yc = 0;
        }
        int digits = 4;
        String xs = "" + xc;
        if (xs.length() > digits) {
            digits = xs.length();
        }
        String ys = "" + yc;
        if (ys.length() > digits) {
            digits = ys.length();
        }
        xs = "000" + xc;
        ys = "000" + yc;
        String label = ys.substring(ys.length() - digits) + "-" + xs.substring(xs.length() - digits);
/*  Cludgy...  TODO: why is this passed an imageplus and not mimsplus?
        if (imp.getStackSize() > 1) {
            String zs = "000" + imp.getCurrentSlice();
            label = zs.substring(zs.length() - digits) + "-" + label;
        }
*/
        MimsPlus mimsp = ui.getMassImages()[0];
        if(mimsp == null) { return label; }

        if (mimsp.getStackSize() > 1) {
            String zs = "000" + mimsp.getCurrentSlice();
            label = zs.substring(zs.length() - digits) + "-" + label;
        }

        return label;
    }

    void deleteAll(){
       if (roiListModel.getSize() > 0) {
          roijlist.setSelectedIndices(getAllIndexes());

          delete();
       }
    }

    public HashMap getRoiLocations() {
       return locations;
    }

    public Integer[] getRoiLocation(String label, int plane) {
       int index = ui.getmimsAction().trueIndex(plane);
       ArrayList<Integer[]> xylist = (ArrayList<Integer[]>)locations.get(label);
       if (xylist == null)
          return null;
       else {
          return xylist.get(index-1);
       }
    }

    boolean delete() {
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
            //if clearing the whole list assume
            //you're working with a "new" file
            this.previouslySaved = false;
            this.savedpath = "";
            this.resetTitle();

        }

        boolean deletednumeric = false;
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
                if( isNumericName((String)roiListModel.get(i)) ) {
                    deletednumeric = true;
                }
                roiListModel.remove(i);
            }
        }
        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Delete");
        }

        //rename any numericly named rois
        //if(deletednumeric) {
        //    renameNumericRois();
        //}
        
        ui.updateAllImages();

        return true;
    }

    boolean rename(String name2) {
        int index = roijlist.getSelectedIndex();
        if (index < 0) {
            return error("Exactly one item in the list must be selected.");
        }
        String name = roiListModel.get(index).toString();
        if (name2 == null) {
            name2 = promptForName(name);
        }
        if (name2 == null) {
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

        // Is this really necessary?
        // if(isNumericName(name) && !(isNumericName(name2))) {
        //    renameNumericRois();
        // }

        if (Recorder.record) {
            Recorder.record("mimsRoiManager", "Rename", name2);
        }
        return true;
    }


    boolean rename(String name, String name2) {
        
        Roi roi = (Roi) rois.get(name);
        if(roi == null) {
            return false;
        }

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
        //wrong way?
        if(roiListModel.contains(name)) {
            roiListModel.setElementAt(name2, roiListModel.indexOf(name));
            //roiListModel.removeElement(name);
            //roiListModel.addElement(name2);
        }
        return true;
    }

    String promptForName(String name) {
        GenericDialog gd = new GenericDialog("MIMS ROI Manager");
        gd.addStringField("Rename As:", name, 20);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return null;
        }
        String name2 = gd.getNextString();
        name2 = getUniqueName(name2);
        return name2;
    }

    boolean restore(int index, boolean setSlice) {
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

        if (setSlice) {
            int slice = getSliceNumber(label);
            if (slice >= 1 && slice <= imp.getStackSize()) {
                imp.setSlice(slice);
            }
        }

        // Set the selected roi to yellow
        roi.setInstanceColor(java.awt.Color.yellow);
        imp.setRoi(roi);

        return true;
    }

    int getSliceNumber(String label) {
        int slice = -1;
        if (label.length() > 4 && label.charAt(4) == '-' && label.length() >= 14) {
            slice = (int) Tools.parseDouble(label.substring(0, 4), -1);
        }
        return slice;
    }

    void open(String path, boolean force) {

       // Does the user want to overwrite current Roi set?
       if (!force && roiListModel.size() > 0) {
          int n = JOptionPane.showConfirmDialog(
                  this,
                  "Opening an ROI file will delete all current ROIs and ROI groups.\nContinue?\n",
                  "Warning",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.WARNING_MESSAGE);
          if (n == JOptionPane.NO_OPTION)
             return;
        }

        ImagePlus imp = getImage();
        if (imp == null) return;
        Macro.setOptions(null);
        String name = null;
        if (path == null) {
            OpenDialog od = new OpenDialog("Open Selection(s)...", ui.getImageDir(), "");
            String directory = od.getDirectory();
            name = od.getFileName();
            if (name == null) {
                return;
            }
            path = directory + name;
        }
        if (path.endsWith(".zip")) {
            openZip(path);
            return;
        }
        ij.io.Opener o = new ij.io.Opener();
        if (name == null) {
            name = o.getName(path);
        }
        Roi roi = o.openRoi(path);
        if (roi != null) {
            if (name.endsWith(".roi")) {
                name = name.substring(0, name.length() - 4);
            }
            name = getUniqueName(name);
            roiListModel.addElement(name);
            rois.put(name, roi);
        }
        resetRoiLocationsLength();
    }
    // Modified on 2005/11/15 by Ulrik Stervbo to only read .roi files and to not empty the current list
    void openZip(String path) {
       
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
                        name = name.substring(0, name.length() - 4);
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
                        for (int i = 0; i < groups.size(); i++) {
                           groupListModel.addElement((String)groups.get(i));
                        }
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
        resetRoiLocationsLength();
    }

    String getUniqueName(String name) {
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

    public ParticlesManager getParticlesManager() {
        return partManager;
    }

    public SquaresManager getSquaresManager() {
        return squaresManager;
    }

    boolean save(String name) {
        if (roiListModel.size() == 0) {
            return error("The selection list is empty.");
        }
        int[] indexes = getAllIndexes();
        /*
        int[] indexes = jlist.getSelectedIndices();
        if (indexes.length == 0) {
            indexes = getAllIndexes();
        }
        */
        Roi[] tmprois = getAllROIs();
        return saveMultiple(tmprois, name, true);
        //return saveMultiple(indexes, name, true);

        /* Allways save a .zip since we're saving positions as well.
        //only called if one roi selected...
        String path = name;
        name = null;
        String listname = listModel.get(indexes[0]).toString();
        if (name == null) {
            name = listname;
        } else {
            name += "_" + listname;
        }
        Macro.setOptions(null);
        SaveDialog sd = new SaveDialog("Save Selection...", path, name, ".roi");
        String name2 = sd.getFileName();
        if (name2 == null) {
            return false;
        }
        String dir = sd.getDirectory();
        Roi roi = (Roi) rois.get(name);
        rois.remove(listname);
        if (!name2.endsWith(".roi")) {
            name2 = name2 + ".roi";
        }
        String newName = name2.substring(0, name2.length() - 4);
        rois.put(newName, roi);
        roi.setName(newName);
        listModel.set(indexes[0], newName);
        RoiEncoder re = new RoiEncoder(dir + name2);
        try {
            re.write(roi);
        } catch (IOException e) {
            IJ.error("MIMS ROI Manager", e.getMessage());
            System.out.println(e.toString());
        }
        return true;
        */
    }

    boolean saveMultiple(Roi[] rois, String path, boolean bPrompt) {
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
    private void resetTitle() {
        String name = savedpath.substring(savedpath.lastIndexOf("/")+1, savedpath.length());
        this.setTitle("MIMS ROI Manager: "+name);
    }

    void measure() {

       // initialize table.
       MimsJTable table = new MimsJTable(ui);

       // Get current plane.
       ImagePlus imp = getImage();
       ArrayList planes = new ArrayList<Integer>();
       planes.add(ui.getOpenMassImages()[0].getCurrentSlice());
       table.setPlanes(planes);

       // Get selected stats.
       String[] statnames = ui.getmimsTomography().getStatNames();
       table.setStats(statnames);
       
       // Get rois.
       Roi[] rois = getAllListedROIs();
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
       table.createRoiTable();
       table.showFrame();
    }
    
    void duplicate() {
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
        if (isStandardName(name)) {
            name = null;
        }
        String label = name != null ? name : getLabel(imp, roi);
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

    void combine() {
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

    void split() {
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

    int[] getAllIndexes() {
        int count = roiListModel.size();
        int[] indexes = new int[count];
        for (int i = 0; i < count; i++) {
            indexes[i] = i;
        }
        return indexes;
    }

    public void roiThreshold(Roi roi, MimsPlus img, double[] params) {
        //check param size
        if(params.length!=5) return;
        //params = minthreshold, maxthreshold, minsize, maxsize
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

    public void roiThreshold(Roi[] rois, MimsPlus img, double[] params) {
        for(int i = 0; i < rois.length; i++) {
            roiThreshold(rois[i], img, params);
        }
    }

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

    public boolean roiOverlap(Roi r, Roi[] rois) {
        boolean overlap = false;
        for(int i = 0; i < rois.length; i++) {
            overlap = ( overlap || roiOverlap(r, rois[i]) );
        }
        return overlap;
    }

    public void roiSquares(Roi roi, MimsPlus img, double[] params) {

        //check param size
        if(params.length!=3) return;
        //params = size, number, overlap
        int size = (int)Math.round(params[0]);
        int num = (int)Math.round(params[1]);
        boolean allowoverlap = (params[2]==1.0);

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

        //generate rois keyed to mean
        Hashtable<Double, ArrayList<Roi>> roihash = new Hashtable<Double, ArrayList<Roi>>();

        for(int ix=x; ix<=x+width; ix++) {
            for(int iy=y; iy<=y+height; iy++) {
                Roi troi = new Roi(ix, iy, size, size);
                temp_img.setRoi(troi);
                double mean = temp_img.getStatistics().mean;
                //System.out.println("mean = "+mean);
                //System.out.println("Double.isNaN(mean) = "+Double.isNaN(mean));
                //exclude rois outside main roi
                if(Double.isNaN(mean)) continue;
                if(roihash.containsKey(mean)) {
                    roihash.get(mean).add(troi);
                } else {
                    ArrayList<Roi> ar = new ArrayList<Roi>();
                    ar.add(troi);
                    roihash.put(mean, ar);
                }
            }
        }



        //double[] means = (double[])roihash.keySet().toArray();
        Object[] means = roihash.keySet().toArray();
        java.util.Arrays.sort(means);

        MimsRoiManager rm = ui.getRoiManager();
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

        //
        //add to manager
        for (Roi r : keep) {
            rm.add(r);
        }

        //cleanup
        temp_img.close();
        //temp_img.show();
    }

    public void roiSquares(Roi[] rois, MimsPlus img, double[] params) {
        for(int i = 0; i < rois.length; i++) {
            roiSquares(rois[i], img, params);
        }
    }

    //Should this be moved to mimsroicontrol?
    //
    public void roiPixelvalues() {
        MimsPlus img = null;
        try{
            img = (MimsPlus)WindowManager.getCurrentImage();
        } catch(Exception E){
            return;
        }
        roiPixelvalues(img);
    }

    public void roiPixelvalues(MimsPlus img) {
        if(img.getMimsType()==MimsPlus.HSI_IMAGE) {
            roiPixelvalues(img.internalRatio);
            return;
        }

        img.killRoi();
        Roi[] rois = this.getSelectedROIs();
        if(rois==null) return;
        Roi roi = rois[0];
        img.setRoi(roi);
        double[] values = img.getRoiPixels();
        img.killRoi();
        
        ij.measure.ResultsTable rTable = new ij.measure.ResultsTable();
        rTable.addColumns();
        rTable.setHeading(0, "Value");
        
        for(int i = 0; i<values.length; i++) {
            rTable.incrementCounter();
            rTable.addValue(0, values[i]);
        }
        rTable.show(img.getRoundedTitle()+"-roi-"+roi.getName());
    }

    ImagePlus getImage() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            error("There are no images open.");
            return null;
        } else {
            return imp;
        }
    }

    public Roi getRoi() {
        int[] indexes = roijlist.getSelectedIndices();

        if (indexes.length == 0) {
            return null;
        }

        String label = roiListModel.get(indexes[0]).toString();
        Roi roi = (Roi) rois.get(label);
        return roi;
    }

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

        if ((roi.getType() == roi.LINE) || (roi.getType() == roi.POLYLINE) || (roi.getType() == roi.FREELINE)) {
            ij.gui.ProfilePlot profileP = new ij.gui.ProfilePlot(imp);
            ui.updateLineProfile(profileP.getProfile(), imp.getShortTitle() + " : " + roi.getName(), imp.getProcessor().getLineWidth());
        } else {
            String label = imp.getShortTitle() + " ROI: " + roi.getName();
            ui.getmimsTomography().updateHistogram(roipix, label, force);
        }
    }

    boolean error(String msg) {
        new MessageDialog(this, "MIMS ROI Manager", msg);
        Macro.abort();
        return false;
    }

    @Override
    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
//			instance = null;
        }
        ignoreInterrupts = false;
    }

    /** Returns a reference to the MIMS ROI Manager
    or null if it is not open. */
    public static MimsRoiManager getInstance() {
        return (MimsRoiManager) instance;
    }

    /** Returns the ROI Hashtable. */
    public Hashtable getROIs() {
        return rois;
    }

    /** Return roi from hash by name */
    public Roi getRoiByName(String name) {
        return (Roi)rois.get(name);
    }

    /** Gets selected ROIs. */
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
             //rois[i] = (Roi) roi.clone();
             //rois[i].setName("r" + Integer.toString(roiIndexes[i] + 1));
             rois[i] = roi;
          }
       }

       return rois;
    }

    /** Gets all ROIs in rois hash. */
    public Roi[] getAllROIs() {
        Object[] ob = rois.values().toArray();
        Roi[] r = new Roi[ob.length];
        for(int i = 0; i< ob.length; i++) {
            r[i] = (Roi)ob[i];
        }
        return r;
    }

    /** Gets all listed ROIs. */
    public Roi[] getAllListedROIs() {

       // Get size.
       int size = roijlist.getModel().getSize();
       Roi[] rois = new Roi[size];
       for (int i = 0; i < size; i++) {
          rois[i] = (Roi)getROIs().get(roijlist.getModel().getElementAt(i));
       }

       return rois;
    }

    /** Returns the selection list. */
    public JList getList() {
        return roijlist;
    }

    public static String getName(String index) {
        int i = (int) Tools.parseDouble(index, -1);
        MimsRoiManager instance = getInstance();
        if (instance != null && i >= 0 && i < instance.roiListModel.size()) {
            return instance.roiListModel.get(i).toString();
        } else {
            return "null";
        }
    }

    /*
    Call this method to find what index the specified
    ROI label has in the jlist.
    */
   public int getIndex(String label) {
      int count = roiListModel.getSize();
      for (int i = 0; i <= count - 1; i++) {
         String value = roiListModel.get(i).toString();
         if (value.equals(label)) return i;
      }
      return -1;
   }

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

    // Programatically selects all items in the list
    void selectAll() {
        int len = roijlist.getModel().getSize();
        if (len <= 0) {
            return;
        } else {
            roijlist.setSelectionInterval(0, len - 1);
        }
    }

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
//    	super.close();
//    	instance = null;
        this.setVisible(false);
    }

    public void mousePressed(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        if (e.isPopupTrigger() || e.isMetaDown()) {
            pm.show(e.getComponent(), x, y);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public boolean getHideRois() {
        boolean bEnabled = cbHideAll.isSelected();
        return bEnabled;
    }

    public void showFrame() {
        setVisible(true);
        toFront();
        setExtendedState(NORMAL);
    }

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
         setText("(" + idx + ") " + label);

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

//REFACTOR this should be moved to managers package
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

    //REFACTOR this should be moved to managers package
    public class SquaresManager extends com.nrims.PlugInJFrame implements ActionListener {

        Frame instance;

        MimsPlus workingimage;
        JLabel label;
        JTextField sizeField = new JTextField();
        JTextField numberField = new JTextField();
        JCheckBox allowOverlap = new JCheckBox("Allow Overlap", false);

        JButton cancelButton;
        JButton okButton;

        public SquaresManager() {
            super("Squares Manager");

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
            setSize(new Dimension(300, 350));

        }

        // Gray out textfield when "All" images radio button selected.
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Cancel")) {
                closeWindow();
            } else if (e.getActionCommand().equals("OK")) {
                //
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    double size = Double.parseDouble(sizeField.getText());
                    double num = Double.parseDouble(numberField.getText());
                    double overlap = 0.0;
                    if(allowOverlap.isSelected()) { overlap = 1.0; }


                    double[] params = {size, num, overlap};

                    Roi[] rois = getSelectedROIs();
                    MimsPlus img = (MimsPlus)getImage();

                    if(img.getMimsType()==MimsPlus.HSI_IMAGE && img.internalRatio!=null) {
                        roiSquares(rois, img.internalRatio, params);
                    } else {
                        roiSquares(rois, img, params);
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

    public Roi getMaxNumericRoi() {
        Roi[] tmprois = getNumericRoisSorted();
        return tmprois[tmprois.length-1];
    }

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

     public Roi[] getNumericRoisSorted() {
        Roi[] returnrois = getNumericRois();

        Comparator<Roi> byName = new RoiNameComparator(){};
        java.util.Arrays.sort(returnrois, byName);

        return returnrois;
     }

    public boolean hasNumericName(Roi r) {
        try {
            int a = Integer.parseInt(r.getName());
            return true;
        }catch(Exception e){
            return false;
        }
    }

    public boolean isNumericName(String name) {
        try {
            int a = Integer.parseInt(name);
            return true;
        }catch(Exception e){
            return false;
        }
    }

}

