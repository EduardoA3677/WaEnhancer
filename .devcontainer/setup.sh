#!/bin/bash
set -e

# Update and install basic dependencies
ls "$HOME"
cd "$HOME"
sudo apt update
sudo apt install -y software-properties-common curl wget unzip fontconfig openjdk-17-jdk gradle tree fonts-powerline fonts-firacode
sudo add-apt-repository universe -y
sudo apt update

# Install Hack Nerd Font
wget -O "$HOME/Hack.zip" https://github.com/ryanoasis/nerd-fonts/releases/download/v3.1.1/Hack.zip
mkdir -p "$HOME/.local/share/fonts"
unzip "$HOME/Hack.zip" -d "$HOME/.local/share/fonts"
fc-cache -fv

# Install Android SDK
wget -O "$HOME/commandlinetools-linux-13114758_latest.zip" https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
mkdir -p "$HOME/Android/cmdline-tools"
unzip "$HOME/commandlinetools-linux-13114758_latest.zip" -d "$HOME/Android/cmdline-tools"
mv "$HOME/Android/cmdline-tools/cmdline-tools" "$HOME/Android/cmdline-tools/latest"

# Set environment variables
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME="$HOME/Android"
export PATH="${ANDROID_HOME}/emulator:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/cmdline-tools/latest:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:$PATH"

# Add environment variables to zshrc
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> "$HOME/.zshrc"
echo 'export ANDROID_HOME="$HOME/Android"' >> "$HOME/.zshrc"
echo 'export PATH="${ANDROID_HOME}/emulator:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/cmdline-tools/latest:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:$PATH"' >> "$HOME/.zshrc"
source "$HOME/.zshrc"

# Install Android SDK components
yes | sdkmanager --update
sdkmanager --list
sdkmanager 'build-tools;30.0.3' 'platform-tools' 'platforms;android-30' 'tools'
yes | sdkmanager --licenses

# Optional: Clean up zip files
rm -f "$HOME/Hack.zip" "$HOME/commandlinetools-linux-13114758_latest.zip"

echo "Setup completed successfully!"
