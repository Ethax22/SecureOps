# 🔐 OAuth Authentication Flow - User Guide

## What is OAuth?

OAuth (Open Authorization) allows you to securely connect your CI/CD accounts without sharing your
password. Instead of manually creating and managing API tokens, you can authenticate directly
through your provider's website.

---

## 🎯 Supported Providers

| Provider | OAuth Support | Manual Token |
|----------|--------------|--------------|
| GitHub Actions | ✅ Supported | ✅ Supported |
| GitLab CI | ✅ Supported | ✅ Supported |
| Azure DevOps | ✅ Supported | ✅ Supported |
| Jenkins | ❌ Not Available | ✅ Supported |
| CircleCI | ❌ Not Available | ✅ Supported |

---

## 📱 How to Add an Account Using OAuth

### Step 1: Select Provider

1. Open **Settings** from the navigation drawer
2. Tap **"Add Account"**
3. Select your CI/CD provider (e.g., GitHub Actions)

### Step 2: Choose OAuth

You'll see two options:

- **"Sign in with OAuth"** button (recommended)
- **OR** manual token entry

Tap the **"Sign in with OAuth"** button.

### Step 3: Authenticate

- A Chrome Custom Tab will open
- You'll see your provider's login page
- Sign in with your credentials
- Review the permissions requested
- Tap **"Authorize"** or **"Allow"**

### Step 4: Grant Permissions

#### GitHub

- Read repository data
- Access workflow information
- Read organization details

#### GitLab

- Read API endpoints
- Access repository information

#### Azure DevOps

- Read build information

### Step 5: Complete Setup

- The app automatically redirects back
- Your token is auto-filled
- Enter a friendly name for the account (e.g., "My GitHub")
- Tap **"Add Account"**
- ✅ Done! Your account is connected.

---

## 🔒 Security & Privacy

### What OAuth Does

✅ No need to share your password  
✅ Tokens can be revoked anytime  
✅ Limited scope (only what's needed)  
✅ Secure token storage on device  
✅ Standard OAuth 2.0 protocol

### What OAuth Doesn't Do

❌ We never see your password  
❌ We can't access unrelated data  
❌ We can't modify your account  
❌ We can't perform actions outside granted scopes

---

## 🆚 OAuth vs Manual Token

| Feature | OAuth | Manual Token |
|---------|-------|--------------|
| Security | More secure | Secure |
| Setup time | 30 seconds | 2-3 minutes |
| Revocation | Easy via provider | Manual |
| Expiration | Handled automatically | Manual refresh |
| User experience | Seamless | More steps |

---

## 🛠️ Troubleshooting

### "OAuth button not showing"

- Only GitHub, GitLab, and Azure support OAuth
- Jenkins and CircleCI require manual tokens
- Use the manual entry option below the OAuth button

### "Chrome Custom Tab won't open"

- Ensure Chrome is installed on your device
- Check internet connectivity
- Try manual token entry instead

### "Authentication failed"

- Check your credentials on the provider's website
- Ensure you granted all required permissions
- Try again or use manual token entry

### "Token auto-fill not working"

- The token should appear automatically after OAuth
- If not, you can still paste it manually
- Contact support if issue persists

---

## 🔄 Revoking OAuth Access

If you want to revoke the app's access:

### GitHub

1. Go to GitHub Settings
2. Navigate to **Applications** → **Authorized OAuth Apps**
3. Find "SecureOps"
4. Click **"Revoke"**

### GitLab

1. Go to GitLab Settings
2. Navigate to **Applications**
3. Find "SecureOps"
4. Click **"Revoke"**

### Azure DevOps

1. Go to Azure Portal
2. Navigate to **App Registrations**
3. Find "SecureOps"
4. Revoke permissions

After revoking, you'll need to re-authenticate to continue using the account in the app.

---

## 💡 Tips

**Use OAuth when:**

- You want the easiest setup
- You prefer not to create tokens manually
- You want automatic token management
- Your provider supports it

**Use manual token when:**

- Your provider doesn't support OAuth (Jenkins, CircleCI)
- You need more control over permissions
- You prefer traditional API token approach
- OAuth is temporarily unavailable

---

## 📞 Support

If you encounter issues with OAuth authentication:

1. **Try manual token entry** - Always available as fallback
2. **Check provider status** - Ensure provider's OAuth service is operational
3. **Review permissions** - Ensure you granted all required scopes
4. **Update the app** - OAuth flow may be improved in newer versions

---

## 🎉 Why OAuth is Great

- **One-Click Setup**: No need to navigate provider settings
- **Secure**: Industry-standard security protocol
- **Convenient**: No copying/pasting long tokens
- **Revocable**: Easy to revoke access anytime
- **Modern**: Best practice for app authentication

Happy CI/CD monitoring! 🚀
