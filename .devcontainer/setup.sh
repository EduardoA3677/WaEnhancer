#!/bin/bash
set -e

cd "$HOME"

# Archivos de configuración posibles
CONFIG_FILES=("$HOME/.bashrc" "$HOME/.zshrc" "$HOME/.profile")

# Actualiza e instala dependencias
sudo apt update
sudo apt install -y software-properties-common
sudo add-apt-repository universe -y
sudo apt update
sudo apt install -y tree fonts-powerline fonts-firacode curl wget unzip openjdk-17-jdk openjdk-17-jre

# Configura alternativas para Java y Javac
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-17-openjdk-amd64/bin/java 1
sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-17-openjdk-amd64/bin/javac 1
sudo update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java
sudo update-alternatives --set javac /usr/lib/jvm/java-17-openjdk-amd64/bin/javac

# Descargar y extraer SDK de Android
SDK_ZIP="commandlinetools-linux-13114758_latest.zip"
SDK_URL="https://dl.google.com/android/repository/$SDK_ZIP"
wget -O "$SDK_ZIP" "$SDK_URL"

mkdir -p "$HOME/Android/cmdline-tools"
unzip -q "$SDK_ZIP" -d "$HOME/Android/cmdline-tools"
mv "$HOME/Android/cmdline-tools/cmdline-tools" "$HOME/Android/cmdline-tools/latest"

# Variables de entorno
JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
ANDROID_HOME="$HOME/Android"
CMDLINE_BIN="$ANDROID_HOME/cmdline-tools/latest/bin"
PATH_UPDATE="$ANDROID_HOME/emulator:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$CMDLINE_BIN:$ANDROID_HOME/platform-tools"

# Exportar para sesión actual
export JAVA_HOME
export ANDROID_HOME
export PATH="$PATH_UPDATE:$PATH"

# Añadir a archivos de configuración
for FILE in "${CONFIG_FILES[@]}"; do
  {
    echo ""
    echo "# Android SDK & Java environment variables"
    echo "export JAVA_HOME=$JAVA_HOME"
    echo "export ANDROID_HOME=$ANDROID_HOME"
    echo "export PATH=$PATH_UPDATE:\$PATH"
  } >> "$FILE"
done

# Detectar shell actual
if [ -n "$ZSH_VERSION" ]; then
  CURRENT_SHELL_RC="$HOME/.zshrc"
elif [ -n "$BASH_VERSION" ]; then
  CURRENT_SHELL_RC="$HOME/.bashrc"
else
  CURRENT_SHELL_RC="$HOME/.profile"
fi

# Aplicar configuración a la sesión actual si es interactiva
if [[ $- == *i* ]]; then
  echo "🔄 Ejecutando: source $CURRENT_SHELL_RC"
  # shellcheck disable=SC1090
  source "$CURRENT_SHELL_RC"
else
  echo "⚠️ La sesión no es interactiva. Por favor, ejecuta manualmente: source $CURRENT_SHELL_RC"
fi

# Instalar componentes SDK
yes | "$CMDLINE_BIN/sdkmanager" --update
yes | "$CMDLINE_BIN/sdkmanager" "build-tools;30.0.3" "platform-tools" "platforms;android-30" "tools"
yes | "$CMDLINE_BIN/sdkmanager" --licenses

# Limpiar archivo descargado
rm -f "$SDK_ZIP"

echo "✅ Entorno de desarrollo Android configurado correctamente."
