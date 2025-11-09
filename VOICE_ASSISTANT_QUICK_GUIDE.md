# Voice Assistant Quick Reference Guide

## How to Use

1. Open the **Voice** tab in the app
2. Tap the **microphone button**
3. Speak your query clearly
4. Wait for the response (text + speech)

## What You Can Ask

### 📊 Build Status

```
"Show me my builds"
"What's the status?"
"How are my pipelines doing?"
"What's currently running?"
"List all builds"
```

### ❌ Failures & Errors

```
"Why did build 123 fail?"
"What went wrong?"
"Explain the failure"
"What's the root cause?"
"Tell me about the error"
```

### ⚠️ Risk & Safety

```
"Any risky deployments?"
"Show high risk builds"
"Check for unstable projects"
"Which builds are dangerous?"
```

### 📈 Analytics & Stats

```
"Show me analytics"
"What are my statistics?"
"How many failed builds?"
"What's my success rate?"
"Average build time"
"Show trends"
```

### 📁 Repositories

```
"List my repositories"
"Show all repos"
"Which projects do I have?"
"Tell me about repository [name]"
```

### 👥 Accounts

```
"List my accounts"
"Show connected providers"
"What CI/CD accounts do I have?"
"Tell me about my GitHub account"
"How do I add an account?"
```

### 🚀 Deployments

```
"When was the last deployment?"
"Show recent deployments"
"What's the deployment status?"
```

### 🌿 Branches

```
"Show me the main branch"
"How is branch [name] doing?"
"List builds for branch develop"
```

### ⏱️ Duration & Performance

```
"How long do builds take?"
"What's the average build time?"
"Show fastest build"
"Which build took the longest?"
```

### 📝 Commits

```
"Show last commit"
"Who made the recent commit?"
"What was the last commit message?"
```

### 🔧 Actions

```
"Rerun the last failed build"
"Retry build 456"
"Restart the pipeline"
"Rollback the deployment"
"Notify the team"
```

### ❓ Help & Conversation

```
"Help"
"What can you do?"
"Show me commands"
"Hello" / "Hi" / "Hey"
"Thank you"
```

## Tips

- **Be Natural**: Speak naturally, the assistant understands variations
- **Be Specific**: Include build numbers, repo names, or time ranges for better results
- **Use Suggestion Chips**: Tap the quick suggestion buttons for common queries
- **Check Visually**: All responses are shown in text and spoken aloud

## Examples

### Example 1: Quick Status Check

```
You: "How are my builds?"
App: "You have 3 failed builds out of 25 total. 2 currently running. The rest are successful."
```

### Example 2: Get Statistics

```
You: "Show me statistics"
App: "Analytics for all time: You have 25 total builds. The overall failure rate is 12.0%. Average build duration is 8 minutes."
```

### Example 3: List Repositories

```
You: "What repositories do I have?"
App: "You have 5 repositories: backend-api, frontend-web, mobile-app, data-pipeline, ml-service"
```

### Example 4: Find Failures

```
You: "Why did the build fail?"
App: "Build 123 in backend-api failed because: Tests failed. The failure occurred in the test step."
```

### Example 5: Check Risk

```
You: "Any risky builds?"
App: "I found 2 deployments with high failure risk. The risky repositories are: legacy-api, flaky-service. You should review them before deploying."
```

## Advanced Queries

### Time-Based Filtering

```
"Show builds from today"
"Builds from this week"
"What happened yesterday?"
```

### Provider-Specific

```
"Show my Jenkins jobs"
"GitHub Actions workflows"
"Tell me about GitLab pipelines"
```

### Multi-Part Queries

```
"What's the failure rate for repository backend-api?"
"How long does the main branch take to build?"
"Who made the last commit to develop?"
```

## No Data Yet?

If you haven't added any CI/CD accounts:

```
You: "Show my builds"
App: "You don't have any builds configured yet. Add a CI/CD account to start monitoring your pipelines."
```

**To add an account:**

1. Go to **Settings** tab
2. Tap **Add Account**
3. Select your CI/CD provider (GitHub, GitLab, Jenkins, etc.)
4. Enter your credentials and API token
5. Save

## Troubleshooting

### Not Listening?

- Grant microphone permission when prompted
- Check that your device microphone is working
- Ensure you're not on mute

### No Response?

- Check internet connection
- Verify you have CI/CD accounts added
- Try rephrasing your query

### Response Too Generic?

- Be more specific (include names, numbers, time ranges)
- Use the suggestion chips for common queries
- Say "Help" to see all available commands

## Privacy & Security

- Voice processing uses Android's built-in speech recognition
- No voice data is sent to external servers
- All data queries stay within the app
- CI/CD credentials are encrypted locally

## Need More Help?

Say **"Help"** or **"What can you do?"** to get a comprehensive list of commands.

---

**Built with ❤️ for SecureOps CI/CD Monitoring**
