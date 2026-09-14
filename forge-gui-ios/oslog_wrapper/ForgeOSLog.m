// ForgeOSLog.m - Simple os_log wrapper for RoboVM
// This wraps os_log since it's a C macro that can't be called directly from Java

#import <os/log.h>
#import <Foundation/Foundation.h>

// Simple function to log a public message using os_log
void forge_os_log_public(const char *message) {
    os_log_with_type(OS_LOG_DEFAULT, OS_LOG_TYPE_DEFAULT, "%{public}s", message);
}

// Log with different levels
void forge_os_log_info(const char *message) {
    os_log_with_type(OS_LOG_DEFAULT, OS_LOG_TYPE_INFO, "%{public}s", message);
}

void forge_os_log_debug(const char *message) {
    os_log_with_type(OS_LOG_DEFAULT, OS_LOG_TYPE_DEBUG, "%{public}s", message);
}

void forge_os_log_error(const char *message) {
    os_log_with_type(OS_LOG_DEFAULT, OS_LOG_TYPE_ERROR, "%{public}s", message);
}
