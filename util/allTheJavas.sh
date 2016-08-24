#!/bin/bash

sudo add-apt-repository -y ppa:webupd8team/java
sudo apt-get update

# The -y doesn't matter because you're prompted for a EULA! 
# Yay Oracle.
sudo apt-get -y install oracle-java{6,7,8,9}-installer 
