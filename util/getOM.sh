#!/bin/bash

#Color defs
red=`tput setaf 1`
green=`tput setaf 2`
cyan=`tput setaf 6`
reset=`tput sgr0`

# TODO:
# - add logic for Linux vs OS X
# - add .desktop file stuff, which has bad paths btw

mkdir -p /tmp/fiji/
cd /tmp/fiji

echo -e "${green} Downloading Fiji... ${reset}"

curl http://jenkins.imagej.net/job/Stable-Fiji/lastSuccessfulBuild/artifact/fiji-linux64.zip \
> fiji-linux64.zip
unzip fiji-linux64.zip

mkdir -p ~/bin/
mv Fiji.app ~/bin/
cd ~/bin/Fiji.app

echo -e "${green} Adding OpenMIMS update site... ${reset}"
./ImageJ-linux64 --update add-update-site OpenMIMS http://nrims.partners.org/OpenMIMS-Fiji/
echo -e "${green} Updating Fiji... ${reset}"
./ImageJ-linux64 --update update

cd ~/bin/
ln -s ./Fiji.app/ImageJ-linux64 ./fiji
ln -s ./Fiji.app/lib/runOpenMIMS.sh ./openmims

# sketch of handling the .desktop file
# cp ./Fiji.app/lib/OpenMIMS.desktop ~/.local/share/applications/

if [[ ":$PATH:" == *":$HOME/bin:"* ]]; then
    echo "~/bin/ is in path"
else
    echo -e "Adding ~/bin/ to path..."
    echo -e "\n\n# Added by OpenMIMS install script" >> ~/.profile
    echo -e "PATH=$PATH:~/opt/bin" >> ~/.profile
fi
