#!/usr/bin/bash

#
#  DON'T ACTUALLY USE THIS YET!!!!
#
echo "DON'T ACTUALLY USE THIS YET!!!!"
exit

DEPLOY_DIR=/nrims/devel/fiji/Fiji-ReleaseCandidate.app/
PROJ_DIR=/nrims/home3/cpoczatek/NetBeansProjects/OpenMIMS/

ant -f $PROJ_DIR -Dnb.internal.action.name=rebuild clean jar

cp ../dist/Open_MIMS.jar $DEPLOYDIR/plugins/Open_MIMS.jar
cd $DEPLOY_DIR


#cp util/runOpenMIMS.sh /nrims/devel/fiji/Fiji-ReleaseCandidate.app/lib/runOpenMIMS.sh

# Don't like that the files are explicit...
# Remove --simulate to actually do something
./ImageJ-linux64 --update upload --simulate --site OpenMIMS plugins/Open_MIMS.jar lib/*MIMS*
