#!/bin/bash
# Launch script for Forge Desktop GUI

echo "Starting Forge Desktop GUI..."
echo ""

cd "$(dirname "$0")"

# Find Java 17 installation
if [ -x "/opt/homebrew/opt/openjdk@17/bin/java" ]; then
    # Homebrew installation (Apple Silicon)
    JAVA_CMD="/opt/homebrew/opt/openjdk@17/bin/java"
    echo "Using Homebrew Java 17: $JAVA_CMD"
elif [ -x "/usr/local/opt/openjdk@17/bin/java" ]; then
    # Homebrew installation (Intel)
    JAVA_CMD="/usr/local/opt/openjdk@17/bin/java"
    echo "Using Homebrew Java 17: $JAVA_CMD"
elif [ -d "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home" ]; then
    # System-linked installation
    JAVA_CMD="/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home/bin/java"
    echo "Using system Java 17: $JAVA_CMD"
else
    # Fallback to system java
    JAVA_CMD="java"
    echo "WARNING: Java 17 not found, using system java (may fail)"
fi

# Check if JAR exists
JAR_PATH="forge-gui-desktop/target/forge-gui-desktop-2.0.08-SNAPSHOT-jar-with-dependencies.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: JAR file not found at $JAR_PATH"
    echo "Please build the project first with: mvn clean package -DskipTests"
    exit 1
fi

echo "Launching Forge Desktop GUI..."
echo ""

# Change to target directory where JAR and resources (via symlink) are located
cd forge-gui-desktop/target

# Launch the desktop GUI
$JAVA_CMD -Xmx4096m -Dio.netty.tryReflectionSetAccessible=true -Dfile.encoding=UTF-8 -jar forge-gui-desktop-2.0.08-SNAPSHOT-jar-with-dependencies.jar
