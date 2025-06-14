# Logging in Forge

## Logging Philosophy

### Logging Objectives

Some high-level objectives of logging include:

- Assessing runtime health
- Providing troubleshooting diagnostics
- Improving system legibility

By default, only emit log messages sufficient to provide a good sense of the health and normal operation of the application, but no more. Default logging should help someone new to the project understand what is going on without overwhelming them with excessive details, and to help a regular user assess the health of the application.

More granular logs ought to be available to someone working on specific areas of functionality. These more granular logs could also help someone to better understand the overall interoperation of the system as a whole.

### Log Level Definitions

The log level associated with any given logging statement will determine how well that log statement meets these broader objectives.

- `ERROR`: Provides indications of adverse operations. Typically there is no clear and immediate remedy for the problems reported in these messages, e.g. network failure, missing resource, unhandled exception
- `WARN`: Provides indications of atypical operations for which there are known remedies. e.g. experimental feature enabled, fallback to a secondary implementation, codepath that could result in performance degradation, initiation of an expensive operation that may take a long time to complete
- `INFO`: Provides status information about normal operations. Should be low-volume and relatively high-level.
- `DEBUG`: Provides status information of interest to someone specifically focused on the subsystem from which the message originates, or someone looking to better understand the overall interoperation of the app. These can be higher-volume than INFO, but should still be infrequent enough that they can be left on without being overwhelming.
- `TRACE`: Provides low-level details likely of interest only to someone actively working on the area of functionality from which the message originates. These can be higher-volume than DEBUG, since they are not intended to be left on indefinitely.

Consistent log level assignments throughout the application enable each user to easily adjust log output to their liking with package or class-specific logger configuration settings.


## Logging Framework Considerations

### SLF4J API with Logback Implementation

The SLF4J API is a lightweight logging interface specification that allows for different conforming implementations to be supplied and configured at runtime, providing a great deal of flexibility in runtime logging while maintaining a simple and consistent code-level experience.

`logback` is the successor to the venerable (and now retired) `log4j` 1.x logging framework. Some advantages offered by `logback`:
  - Native implementation of the SLF4J API: no adapter layer, resulting in high-performance, low-latency logging.
  - Parameterized logging: if a log message will not be emitted, no time is wasted generating the log string.
  - Mapped Diagnostic Context (MDC) support: facilities for managing per-thread metadata; can be useful for e.g. tracking an individual event through various subsystems.
  - Tuned for performance and stability, and widely used.
  - Automatic reloading of configuration files.
  - Flexible configuration.

### Cross-Platform

Our logging platform must support all deployment targets.

### Low-Configuration

The default logging should require very little, if any, configuration. But it must also be simple and straightforward to configure when adjustments are desired.


## Logging Configuration

### Configuration Files

Logging is configured primarily by a `logback.xml` file found in `forge-gui/src/main/resources/logback.xml`. The default settings are intended to provide a reasonable volume of high-level logging messages useful for observing the behavior of the application under normal circumstances. Errors should appear readily, and not be drowned out by excessively verbose output.

When working on a specific functional area of the code, logging thresholds can be adjusted to provide more granular logging or to attenuate distracting messages. This is done by adjusting the contents of `forge-gui/src/main/resources/logging.properties`. That properties file does not exist by default (but logback _is_ configured to look for it); so to make adjustments, copy the example properties file to this location:

```
cp forge-gui/src/main/resources/logging.properties.example forge-gui/src/main/resources/logging.properties
```

`logging.properties` does not exist by default because each developer's day-to-day logging needs vary greatly, and we want to have a consistent baseline logging experience. Leaving this potentially volatile and variable file out of version control prevents inadvertent commits that could dramatically alter logging for everyone, and should reduce contention with respect to our defaults. However, changes to `logging.properties.example` that provide settings recommendations for specific feature areas are welcome.

### Configuration Overrides

The `logback.xml` configuration is designed to allow overriding key aspects of the logging subsystem by passing properties as commandline arguments. Here are some examples:

```
-DloggingFilename="${HOME}/forge-dev.log"
-DloggingIsFileEnabled=true
-DloggingIsConsoleEnabled=false
-DloggingThreshold=ERROR
```

Inspect the `logback.xml` file for other settings that can be overridden, and to see the default values for these settings.

To fully silence logback status messages (make sure the `logback.xml` configuration element has `debug=false` as well), you can use this commandline option:

```
-Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener
```

### Log Pattern

The default log pattern is:

```
%d [%-5p][%-25c{5}] %m%n
```

This is: datestamp, log level, classname with the package portions abbreviated, log message, and newline, with some formatting to help align elements more neatly across lines, improving scan-ability and readability. Here are some sample log lines demonstrating this pattern:

```
2025-06-13 15:51:13,082 [INFO ][f.u.SwingImageFetcher    ] Saved image to: /Users/forge-dev/Library/Caches/Forge/pics/cards/MKM/Extract a Confession.fullborder.jpg
2025-06-13 15:51:13,082 [TRACE][f.u.ImageFetcher         ] Removed: /Users/forge-dev/Library/Caches/Forge/pics/cards/MKM/Extract a Confession.full.jpg [queueSize: 8]
2025-06-13 15:51:13,148 [INFO ][f.u.SwingImageFetcher    ] Saved image to: /Users/forge-dev/Library/Caches/Forge/pics/cards/ARB/Glassdust Hulk.fullborder.jpg
2025-06-13 15:51:13,149 [TRACE][f.u.ImageFetcher         ] Removed: /Users/forge-dev/Library/Caches/Forge/pics/cards/ARB/Glassdust Hulk.full.jpg [queueSize: 7]
```


## Logging Techniques

### Parameterized Log Messages

When writing logging statements, use parameterized logging. Parameterized log strings are not evaluated unless the log message will actually be emitted, so there is minimal overhead for any log statements below the configured logging threshold. For example, if we have an object whose `.toString()` is computationally expensive, if we were to do:

```
log.trace("Current object value: " + object);
```
we will _always_ build the string, always incurring the expense of that `object.toString()`, even if our current logging threshold is above `TRACE`.

With a simple change to parameterization, the string is created only if the log message will be emitted:

```
log.trace("Current object value: {}", object);
```

Any number of placeholders can be used:

```
log.trace("Current object value for context {}: {} [isSomeConditionTrue: {}]", context, object, condition);
```

Note that parameterized placeholders do not provide the richness of `printf`-style `Formatter` placeholders; this is one of the trade-offs of using parameterized logging.

## Other Logging In Forge

- There are 802 usages of rudimentary `System.(out|err).println` logging, 72 of which are commented out.
- There are 10 usages of `minlog` logging.
- There are 8 usages of `android.util.Log` logging.
- There is a game event logging subsystem, which is orthogonal to the development-centric logging under discussion here.
- Hanmac reports "I think right now, the logging is mostly done via Sentry, and then Sentry writes the log file." The `sentry.properties` configuration files all reference a remote data source name (dsn), so this functionality does not _appear_ to be directly relevant to the adoption of runtime development-centric logging under discussion here. However, it _is_ important that we do not inadvertently break this Sentry logging.
- The `logback` configuration file initially added to the repo configures logging for some dependencies whose logging conforms to the `slf4j` API, including the above-mentioned Sentry log aggregation. It appears that configuration for these dependencies are also satisfied and subsumed under the expanded `logback` configuration introduced for general runtime logging.
