package com.secureops.app.ml.security

/**
 * Severity levels for detected security threats
 * 
 * Used to prioritize and classify security findings in code and logs
 */
enum class ThreatSeverity(val level: Int, val displayName: String) {
    /**
     * Critical - Immediate action required
     * Examples: Exposed private keys, production credentials
     */
    CRITICAL(4, "Critical"),
    
    /**
     * High - Should be addressed urgently
     * Examples: API keys, database passwords
     */
    HIGH(3, "High"),
    
    /**
     * Medium - Should be addressed soon
     * Examples: Suspicious patterns, potential secrets
     */
    MEDIUM(2, "Medium"),
    
    /**
     * Low - Informational, review recommended
     * Examples: Weak patterns, possible false positives
     */
    LOW(1, "Low"),
    
    /**
     * Info - Informational only
     * Examples: Security best practice violations
     */
    INFO(0, "Info");
    
    companion object {
        /**
         * Get severity from level integer
         */
        fun fromLevel(level: Int): ThreatSeverity {
            return values().find { it.level == level } ?: INFO
        }
    }
}
