# Setup on OVH

Chose an Ubuntu 16.04 VM.


## Installing OS dependencies

sudo apt-get update
sudo apt-get -y install curl
sudo apt-get -y install git-core

sudo apt-get -y install openjdk-8-jdk
sudo apt-get -y install maven
sudo apt-get -y install rlwrap
curl -O https://download.clojure.org/install/linux-install-1.10.1.536.sh
chmod +x linux-install-1.10.1.536.sh
sudo ./linux-install-1.10.1.536.sh


