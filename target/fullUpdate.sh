#!/bin/sh

java -jar MCModSync.jar -d -f
echo "Running Forge Installer, follow the instuctions to install the forge client there..."
java -jar ForgeInstaller.jar
rm ForgeInstaller.jar
