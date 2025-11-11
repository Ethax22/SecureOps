# Phase 3: Multi-Provider Testing Guide 🧪

**Date:** November 9, 2025  
**Purpose:** Test GitHub Actions, GitLab CI, CircleCI, and Azure DevOps integrations

---

## 🎯 **Overview**

All 5 CI/CD providers are **fully implemented** in code:

- ✅ Jenkins (Tested & Working)
- ⚠️ GitHub Actions (Code exists, not tested)
- ⚠️ GitLab CI (Code exists, not tested)
- ⚠️ CircleCI (Code exists, not tested)
- ⚠️ Azure DevOps (Code exists, not tested)

**Goal:** Verify each provider works with real accounts

---

## 📋 **Pre-Testing Checklist**

### What You Need:

- [ ] GitHub account with a repository that has Actions
- [ ] GitLab account with a project that has CI/CD
- [ ] CircleCI account connected to a repository
- [ ] Azure DevOps account with pipelines
- [ ] Personal Access Tokens (PAT) for each platform

---

## 🔧 **Test 1: GitHub Actions**

### Step 1: Get GitHub Personal Access Token

1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Select scopes:
    - ✅ `repo` (Full control of private repositories)
    - ✅ `workflow` (Update GitHub Action workflows)
4. Generate and copy token

### Step 2: Prepare Repository URL

Format: `https://github.com/USERNAME/REPO_NAME`

Example: `https://github.com/Ethax22/Vibestate`

### Step 3: Add Account in App

1. Open SecureOps app
2. Go to **Settings** → **Add Account**
3. Select **GitHub Actions**
4. Enter:
    - **Account Name**: "My GitHub"
    - **Base URL**: `https://github.com/USERNAME/REPO_NAME`
    - **Token**: (paste your PAT)
5. Save

### Step 4: Verify

- [ ] Account added successfully
- [ ] Dashboard shows GitHub workflows
- [ ] Build numbers visible
- [ ] Status indicators correct (Success/Failure/Running)
- [ ] Can tap to view details
- [ ] Can rerun workflows

### Expected Behavior:

- ✅ App fetches workflow runs from GitHub Actions API
- ✅ Shows last 50 runs
- ✅ Displays commit info, branch, author
- ✅ Rerun button works

### Troubleshooting:

- **Error: 401 Unauthorized** → Check token has correct scopes
- **Error: 404 Not Found** → Verify repository URL format
- **No workflows shown** → Check repository has Actions enabled

---

## 🔧 **Test 2: GitLab CI**

### Step 1: Get GitLab Personal Access Token

1. Go to GitLab → Preferences → Access Tokens
2. Create token with scopes:
    - ✅ `read_api`
    - ✅ `read_repository`
    - ✅ `write_repository`
3. Copy token

### Step 2: Get Project ID

1. Go to your GitLab project
2. Look under project name (Project ID: 12345678)
3. Or go to Settings → General → Project ID

Example: `gitlab.com/username/project-name` → Project ID: `12345678`

### Step 3: Prepare URL

Format: `https://gitlab.com/api/v4/projects/PROJECT_ID`

Example: `https://gitlab.com/api/v4/projects/12345678`

### Step 4: Add Account in App

1. Settings → Add Account → **GitLab CI**
2. Enter:
    - **Account Name**: "My GitLab"
    - **Base URL**: `https://gitlab.com/api/v4/projects/12345678`
    - **Token**: (paste your PAT)
3. Save

### Step 5: Verify

- [ ] Account added
- [ ] Pipelines visible in dashboard
- [ ] Can view pipeline details
- [ ] Can retry pipelines
- [ ] Can cancel running pipelines

### Expected Behavior:

- ✅ Fetches pipelines from GitLab API
- ✅ Shows pipeline status
- ✅ Displays branch and commit SHA

### Troubleshooting:

- **Error: 401** → Check token scopes
- **Error: 404** → Verify project ID is correct
- **No pipelines** → Check project has CI/CD enabled

---

## 🔧 **Test 3: CircleCI**

### Step 1: Get CircleCI API Token

1. Go to CircleCI → User Settings → Personal API Tokens
2. Create new token
3. Copy token

### Step 2: Prepare Project Info

Format: `github/ORG_NAME/REPO_NAME` or `bitbucket/ORG_NAME/REPO_NAME`

Example: `github/Ethax22/Vibestate`

### Step 3: Add Account in App

1. Settings → Add Account → **CircleCI**
2. Enter:
    - **Account Name**: "My CircleCI"
    - **Base URL**: `https://circleci.com/api/v2/project/github/USERNAME/REPO`
    - **Token**: (paste your API token)
3. Save

### Step 4: Verify

- [ ] Account added
- [ ] Pipelines visible
- [ ] Build status correct
- [ ] Can rerun workflows

### Expected Behavior:

- ✅ Fetches pipeline data from CircleCI API v2
- ✅ Shows recent runs
- ✅ Displays commit info

### Troubleshooting:

- **Error: Unauthorized** → Verify token
- **No data** → Check project slug format (github/org/repo)

---

## 🔧 **Test 4: Azure DevOps**

### Step 1: Get Azure Personal Access Token

1. Go to Azure DevOps → User Settings → Personal Access Tokens
2. Create token with scopes:
    - ✅ Build (Read & Execute)
    - ✅ Code (Read)
3. Copy token

### Step 2: Prepare Organization and Project

Format: `https://dev.azure.com/ORGANIZATION/PROJECT`

Example: `https://dev.azure.com/mycompany/MyProject`

### Step 3: Add Account in App

1. Settings → Add Account → **Azure DevOps**
2. Enter:
    - **Account Name**: "My Azure DevOps"
    - **Base URL**: `https://dev.azure.com/ORG/PROJECT`
    - **Token**: (paste your PAT)
3. Save

### Step 4: Verify

- [ ] Account added
- [ ] Builds visible in dashboard
- [ ] Can view build details
- [ ] Can rerun builds

### Expected Behavior:

- ✅ Fetches builds from Azure DevOps API
- ✅ Shows build definitions
- ✅ Displays source branch and version

### Troubleshooting:

- **Error: 401** → Check token scopes
- **Error: 404** → Verify organization and project names

---

## 📊 **Testing Checklist**

### For Each Provider:

#### Basic Functionality:

- [ ] Add account successfully
- [ ] Account appears in Manage Accounts
- [ ] Initial sync completes
- [ ] Pipelines appear on Dashboard
- [ ] Build numbers visible
- [ ] Status indicators working (colors)
- [ ] Commit messages displayed
- [ ] Author names shown
- [ ] Branch names visible
- [ ] Timestamps correct

#### Advanced Functionality:

- [ ] Tap pipeline to view details
- [ ] Build details screen loads
- [ ] Root cause analysis shown (for failures)
- [ ] Rerun button works
- [ ] Cancel button works (if running)
- [ ] Logs load (if implemented for provider)
- [ ] Background sync works (after 15 min)
- [ ] Notifications work for failures

#### Voice Assistant:

- [ ] "Show my [provider] builds"
- [ ] "What's the status of [provider]?"
- [ ] "Any failed [provider] pipelines?"

---

## 🐛 **Common Issues & Solutions**

### Issue: "Failed to sync pipelines"

**Possible Causes:**

1. Invalid token
2. Incorrect base URL format
3. Network connectivity
4. API rate limiting
5. Provider-specific permissions

**Solution:**

1. Verify token has correct scopes
2. Check URL format matches examples
3. Check internet connection
4. Wait a few minutes (rate limit)
5. Ensure account has access to repositories

### Issue: "No pipelines showing"

**Possible Causes:**

1. Repository has no CI/CD configured
2. No recent builds
3. Permissions issue

**Solution:**

1. Trigger a build on the platform
2. Check repository has workflows/pipelines
3. Verify token has read access

### Issue: "Rerun not working"

**Possible Causes:**

1. Token missing write permissions
2. Build cannot be rerun (already running)
3. Provider-specific restrictions

**Solution:**

1. Regenerate token with execute permissions
2. Check build is in a rerunable state
3. Check provider documentation

---

## 📝 **Testing Results Template**

Use this template to document your testing:

```markdown
## GitHub Actions Testing Results

**Date:** [Date]
**Account:** [Account name]
**Repository:** [Repo URL]

### ✅ Working:
- [ ] Account addition
- [ ] Pipeline fetching
- [ ] Dashboard display
- [ ] Build details
- [ ] Rerun functionality

### ❌ Issues Found:
1. [Issue description]
   - Error: [Error message]
   - Steps to reproduce: [Steps]
   - Expected: [Expected behavior]
   - Actual: [Actual behavior]

### 📊 Summary:
- Total pipelines fetched: X
- Status: Working / Needs Fix
- Notes: [Additional notes]
```

---

## 🎯 **Success Criteria**

### Minimum for Production:

- ✅ Jenkins (already working)
- ✅ GitHub Actions (basic functionality)
- ✅ GitLab CI (basic functionality)

### Nice to Have:

- ⚠️ CircleCI (if you use it)
- ⚠️ Azure DevOps (if you use it)

---

## 🔍 **Code Locations for Debugging**

If you find issues during testing:

### Provider Implementations:

- `PipelineRepository.kt` - Lines 88-308 (all fetch methods)
- `RemediationExecutor.kt` - Lines 60-180 (rerun/cancel actions)

### API Services:

- `GitHubService.kt` - GitHub Actions endpoints
- `GitLabService.kt` - GitLab CI endpoints
- `CircleCIService.kt` - CircleCI endpoints
- `AzureDevOpsService.kt` - Azure DevOps endpoints

### NetworkModule:

- `NetworkModule.kt` - Retrofit configuration and interceptors

---

## 🚀 **Next Steps After Testing**

1. Document any issues found
2. Create fix tickets for problems
3. Update documentation with working examples
4. Consider adding provider-specific features
5. Implement OAuth2 if needed

---

## 💡 **Tips for Successful Testing**

1. **Start with GitHub** - Most popular, easiest to test
2. **Use test repositories** - Don't use production repos
3. **Test with both** - Success and failure scenarios
4. **Check logs** - Use Logcat to see API responses
5. **Test offline** - Verify cached data works
6. **Test notifications** - Trigger failures to test alerts

---

## 📚 **API Documentation References**

- **GitHub Actions**: https://docs.github.com/en/rest/actions
- **GitLab CI**: https://docs.gitlab.com/ee/api/pipelines.html
- **CircleCI**: https://circleci.com/docs/api/v2
- **Azure DevOps**: https://learn.microsoft.com/en-us/rest/api/azure/devops/build

---

**🎊 Ready to test! Start with GitHub Actions and report any issues you find!**
