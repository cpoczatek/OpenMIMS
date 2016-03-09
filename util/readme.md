Short notes about scripts in this directory.  Most scripts are self documented.
--------------------------------------------------------------------------------

runOpenMIMS.sh - bash script to start Fiji and run OpenMIMS passing args to OpenMIMS.
Also runs a macro to do the toolbar setup and logs stdout/stderr to a file.

--------------------------------------------------------------------------------

runOpenMIMS_singleInstance.sh - same as above, but with -single_instance flag added.

--------------------------------------------------------------------------------

OpenMIMS.desktop - a .desktop file to allow filetype-application association. Copy
to ~/.local/share/applications/ or similar

--------------------------------------------------------------------------------

OpenMIMS.app.zip - an OSX app wrapper around a 1 line bash script, written in Automator.
To install, copy .zip somewhere, unzip, copy OpenMIMS.app to /Applications/

The script being wrapped is:
open -n -a Fiji --args --allow-multiple -eval "run('Open MIMS Image', '$@'); run('Install...', 'install=/Applications/Fiji.app/macros/openmims_tools.fiji.ijm');"

Details of above command:
open : general OSX command
-n : start new instance
-a Fiji : use application 'Fiji', assumes Fiji.app in in /Applications/
--args : following are args to application (Fiji)
--allow-multiple : allow multiple Fiji instances (difference from -n)
-eval : evaluate following macro commands, there are 2 commands between the "quotes"
"run('Open ...[snip]... ); : 1st macro, run OpenMIMS and pass it the arguments to this script, eg filename.
run('Install ...[snip]...);" : 2nd macro, install the custom OpenMIMS toosls in the toolbar, assumes Fiji is in /Applications/


--------------------------------------------------------------------------------






