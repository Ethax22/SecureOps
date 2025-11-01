package com.secureops.app.domain.model

enum class CIProvider(val displayName: String) {
    GITHUB_ACTIONS("GitHub Actions"),
    GITLAB_CI("GitLab CI"),
    JENKINS("Jenkins"),
    CIRCLE_CI("CircleCI"),
    AZURE_DEVOPS("Azure DevOps")
}
