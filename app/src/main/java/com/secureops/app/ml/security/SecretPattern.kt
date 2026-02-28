package com.secureops.app.ml.security

/**
 * Production-grade secret patterns for detecting exposed credentials
 * 
 * Each pattern includes:
 * - Regex pattern optimized for production use
 * - Severity level
 * - Description
 * - Common variable name patterns
 */
enum class SecretPattern(
    val regex: Regex,
    val severity: ThreatSeverity,
    val description: String,
    val variablePatterns: List<String> = emptyList()
) {
    /**
     * AWS Access Key ID
     * Format: AKIA[0-9A-Z]{16}
     */
    AWS_ACCESS_KEY(
        regex = Regex("""(?i)(AKIA[0-9A-Z]{16})"""),
        severity = ThreatSeverity.CRITICAL,
        description = "AWS Access Key ID",
        variablePatterns = listOf("aws_access_key", "AWS_KEY", "aws_key_id")
    ),
    
    /**
     * AWS Secret Access Key
     * Format: 40 characters of base64
     */
    AWS_SECRET_KEY(
        regex = Regex("""(?i)(?:aws_secret_access_key|aws_secret_key)[\s]*[=:]["']?([A-Za-z0-9/+=]{40})["']?"""),
        severity = ThreatSeverity.CRITICAL,
        description = "AWS Secret Access Key",
        variablePatterns = listOf("aws_secret_access_key", "AWS_SECRET", "aws_secret")
    ),
    
    /**
     * GitHub Personal Access Token
     * Format: ghp_[A-Za-z0-9]{36} (new format)
     */
    GITHUB_TOKEN(
        regex = Regex("""(ghp_[A-Za-z0-9]{36})"""),
        severity = ThreatSeverity.CRITICAL,
        description = "GitHub Personal Access Token",
        variablePatterns = listOf("github_token", "gh_token", "GITHUB_PAT")
    ),
    
    /**
     * GitHub OAuth Token
     * Format: gho_[A-Za-z0-9]{36}
     */
    GITHUB_OAUTH(
        regex = Regex("""(gho_[A-Za-z0-9]{36})"""),
        severity = ThreatSeverity.CRITICAL,
        description = "GitHub OAuth Access Token",
        variablePatterns = listOf("github_oauth", "gh_oauth_token")
    ),
    
    /**
     * Generic GitHub Token (legacy format)
     * Format: 40 hex characters
     */
    GITHUB_TOKEN_LEGACY(
        regex = Regex("""(?i)(?:github_token|gh_token)[\s]*[=:]["']?([a-f0-9]{40})["']?"""),
        severity = ThreatSeverity.HIGH,
        description = "GitHub Token (Legacy Format)",
        variablePatterns = listOf("github_token", "GITHUB_TOKEN")
    ),
    
    /**
     * JSON Web Token (JWT)
     * Format: eyJ[A-Za-z0-9-_=]+\.eyJ[A-Za-z0-9-_=]+\.?[A-Za-z0-9-_.+/=]*
     */
    JWT_TOKEN(
        regex = Regex("""(eyJ[A-Za-z0-9-_=]+\.eyJ[A-Za-z0-9-_=]+\.?[A-Za-z0-9-_.+/=]*)"""),
        severity = ThreatSeverity.HIGH,
        description = "JSON Web Token (JWT)",
        variablePatterns = listOf("jwt", "token", "auth_token", "bearer")
    ),
    
    /**
     * Generic API Key
     * Format: Various patterns with "api_key" or "apikey" context
     */
    API_KEY(
        regex = Regex("""(?i)(?:api_key|apikey|api-key)[\s]*[=:]["']?([A-Za-z0-9_\-]{20,})["']?"""),
        severity = ThreatSeverity.HIGH,
        description = "Generic API Key",
        variablePatterns = listOf("api_key", "apikey", "API_KEY")
    ),
    
    /**
     * Private Key (RSA/SSH)
     * Format: -----BEGIN PRIVATE KEY-----
     */
    PRIVATE_KEY(
        regex = Regex("""(-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)"""),
        severity = ThreatSeverity.CRITICAL,
        description = "RSA/SSH Private Key",
        variablePatterns = listOf("private_key", "ssh_key", "rsa_key")
    ),
    
    /**
     * Slack Token
     * Format: xox[baprs]-[0-9a-zA-Z]{10,48}
     */
    SLACK_TOKEN(
        regex = Regex("""(xox[baprs]-[0-9a-zA-Z]{10,48})"""),
        severity = ThreatSeverity.HIGH,
        description = "Slack Token",
        variablePatterns = listOf("slack_token", "SLACK_TOKEN", "slack_api")
    ),
    
    /**
     * Google API Key
     * Format: AIza[0-9A-Za-z-_]{35}
     */
    GOOGLE_API_KEY(
        regex = Regex("""(AIza[0-9A-Za-z\-_]{35})"""),
        severity = ThreatSeverity.HIGH,
        description = "Google API Key",
        variablePatterns = listOf("google_api_key", "GOOGLE_KEY", "gcp_key")
    ),
    
    /**
     * Generic Password in Code
     * Format: password = "..." or password: "..."
     */
    PASSWORD_IN_CODE(
        regex = Regex("""(?i)(?:password|passwd|pwd)[\s]*[=:]["']([^"'\s]{8,})["']"""),
        severity = ThreatSeverity.HIGH,
        description = "Password in Code",
        variablePatterns = listOf("password", "passwd", "pwd", "PASSWORD")
    ),
    
    /**
     * Database Connection String
     * Format: Contains password in connection string
     */
    DATABASE_URL(
        regex = Regex("""(?i)((?:jdbc|mysql|postgresql|mongodb|redis)://[^:]+:[^@]+@[^\s]+)"""),
        severity = ThreatSeverity.CRITICAL,
        description = "Database Connection String with Credentials",
        variablePatterns = listOf("database_url", "db_url", "connection_string")
    ),
    
    /**
     * Azure Storage Key
     * Format: Base64 string in Azure context
     */
    AZURE_STORAGE_KEY(
        regex = Regex("""(?i)(?:azure_storage|account_key)[\s]*[=:]["']?([A-Za-z0-9+/]{88}==)["']?"""),
        severity = ThreatSeverity.CRITICAL,
        description = "Azure Storage Account Key",
        variablePatterns = listOf("azure_storage_key", "AZURE_KEY", "storage_account_key")
    ),
    
    /**
     * Generic Secret Pattern
     * Format: High-entropy strings in secret context
     */
    GENERIC_SECRET(
        regex = Regex("""(?i)(?:secret|token|key)[\s]*[=:]["']([A-Za-z0-9+/=_\-]{32,})["']"""),
        severity = ThreatSeverity.MEDIUM,
        description = "Generic Secret Pattern",
        variablePatterns = listOf("secret", "token", "key", "SECRET")
    );
    
    companion object {
        /**
         * Get all patterns sorted by severity (highest first)
         */
        fun getAllByPriority(): List<SecretPattern> {
            return values().sortedByDescending { it.severity.level }
        }
        
        /**
         * Get patterns by severity level
         */
        fun getBySeverity(severity: ThreatSeverity): List<SecretPattern> {
            return values().filter { it.severity == severity }
        }
        
        /**
         * Get critical patterns only
         */
        fun getCriticalPatterns(): List<SecretPattern> {
            return values().filter { it.severity == ThreatSeverity.CRITICAL }
        }
    }
}
