# Finder starts apps without the shell's PATH, and /usr/bin/java without a JDK opens Apple's install prompt.

forge_java_home() {
  # Without -F, java_home returns the default JVM when none is 17 or newer.
  /usr/libexec/java_home -F -v 17+ 2>/dev/null && return 0
  # Homebrew's openjdk is not registered with java_home.
  for h in /opt/homebrew/opt/openjdk /usr/local/opt/openjdk; do
    if [ -x "$h/bin/java" ]; then
      echo "$h"
      return 0
    fi
  done
  return 1
}

forge_require_java() {
  if java_home=$(forge_java_home); then
    export PATH="$java_home/bin:$PATH"
    return 0
  fi
  if osascript -e 'display dialog "Forge needs Java 17 or newer." buttons {"Cancel", "Download Java"} default button "Download Java" with icon caution' >/dev/null 2>&1; then
    open "https://adoptium.net/temurin/releases/?os=mac&package=jre&version=17"
  fi
  exit 1
}
