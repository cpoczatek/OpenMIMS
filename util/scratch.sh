#!/bin/bash

red=`tput setaf 1`
green=`tput setaf 2`
reset=`tput sgr0`
echo "${red}red text ${green}green text${reset}"

echo -e "${green}Downloading Fiji... ${reset}"

if [ "$(uname)" == "Darwin" ]; then
    # Do something under Mac OS X platform
    echo "I'm a Mac!"
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    # Do something under GNU/Linux platform
    echo "I'm a Linux!"
elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW32_NT" ]; then
    # Do something under Windows NT platform
    echo "I'm a Windows!"
fi

if [[ ":$PATH:" == *":$HOME/bin:"* ]]; then
    echo "~/bin/ is in path"
else
    echo -e "Adding ~/bin/ to path..."
    echo -e "\n\n# Added by OpenMIMS install script" >> ~/.profile
    echo -e "PATH=$PATH:~/opt/bin" >> ~/.profile
fi
