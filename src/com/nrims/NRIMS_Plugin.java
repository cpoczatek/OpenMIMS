package com.nrims;

/*
 * NRIMS_Plugin.java
 *
 * Created on May 1, 2006, 12:34 PM
 *
 * @author Douglas Benson
 */

import ij.plugin.PlugIn ;


public class NRIMS_Plugin implements PlugIn {
    
    /** Creates a new instance of NRIMS_Plugin Analysis Module. */
    public NRIMS_Plugin() {
        System.out.println("NRIMS constructor");
        if(ui == null) ui = new com.nrims.UI();
    }

    /** Opens the GUI. */
    @Override
    public void run(String arg) {
        //System.out.println("NRIMS.run");
        String options = ij.Macro.getOptions();
        if(options!=null)
            options = options.trim();
        System.out.println("options="+options+",");

        ui.run(options);
        if(ui.isVisible() == false) ui.setVisible(true);
    }

    private static com.nrims.UI ui = null ;
 }
