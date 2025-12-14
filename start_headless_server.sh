#!/bin/bash
# Startup script for ForgeHeadlessServer (port 8080)

echo "Starting ForgeHeadlessServer on port 8080..."
echo "Press Ctrl+C to stop the server"
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

$JAVA_CMD -cp forge-gui-desktop/target/forge-gui-desktop-2.0.08-SNAPSHOT-jar-with-dependencies.jar forge.view.ForgeHeadless "$@"
