# 📱 Step-by-Step Guide: Creating ARM64 Emulator

## 🎯 Goal

Create an ARM64 emulator that can run the RunAnywhere SDK and AI models.

---

## 📋 Prerequisites

Before starting, make sure:

- ✅ Android Studio is open
- ✅ You have internet connection (to download system image)
- ✅ At least 10GB free disk space

---

## 🚀 Step-by-Step Instructions

### Step 1: Open Device Manager

**Option A: From Top Menu**

1. Click **Tools** in the menu bar
2. Click **Device Manager**

**Option B: From Right Sidebar**

1. Look for the **phone/tablet icon** on the right sidebar
2. Click it to open Device Manager

**Option C: From Top Toolbar**

1. Look for the device dropdown (near the Run button)
2. Click the **device selector dropdown**
3. Click **Device Manager** at the bottom

---

### Step 2: Create New Virtual Device

In the Device Manager window:

1. Click the **"+"** button or **"Create Device"** button
    - Located at the top-left or center of the window
    - Might say "Create Virtual Device" or just "+"

---

### Step 3: Select Hardware (Phone/Tablet)

You'll see a list of device definitions:

**Recommended Devices for ARM64:**

- ✅ **Pixel 7** (Good balance)
- ✅ **Pixel 7 Pro** (Larger screen)
- ✅ **Pixel 8** (Latest, if available)
- ✅ **Pixel 8 Pro** (Latest, if available)

**What to do:**

1. **Scroll through the list** under "Phone" category
2. **Select one of the recommended devices** (e.g., Pixel 7)
3. Click **"Next"** button at bottom-right

**Why these devices?**

- Modern devices have better ARM64 support
- Good screen sizes for testing
- Well-supported by Android Studio

---

### Step 4: Select System Image (Most Important!)

This is where you choose ARM64 architecture.

#### What You'll See:

You'll see tabs at the top:

- **Recommended** (may not have ARM64)
- **x86 Images** (don't use these!)
- **ARM64 Images** ← **SELECT THIS TAB!**
- **Other Images**

#### Action Steps:

1. **Click the "ARM64 Images" tab** (or "Other Images" tab)

2. **Look for images with these characteristics:**
    - **ABI Column**: Should say **"arm64-v8a"** or **"ARM 64"**
    - **API Level**: 34 (Android 14) or 35 (Android 15) recommended
    - **Target**: Android with Google APIs

3. **Recommended Image to Download:**
   ```
   Release Name: VanillaIceCream (or UpsideDownCake)
   API Level: 35 (or 34)
   ABI: arm64-v8a
   Target: Android with Google APIs
   ```

4. **If the image is not downloaded:**
    - You'll see a **download icon** next to it
    - Click the **download icon/link**
    - Accept license agreement
    - Wait for download (may take 5-10 minutes, ~1-2 GB)

5. **Select the ARM64 image** by clicking on it

6. Click **"Next"** button

#### ⚠️ Common Issue: Can't Find ARM64 Images Tab?

If you don't see "ARM64 Images" tab:

**Solution A: Look in "Other Images" Tab**

1. Click **"Other Images"** tab
2. Look at the **"ABI"** column
3. Find images with **"arm64-v8a"**

**Solution B: Show All Images**

1. Check **"Show all images"** checkbox (if available)
2. Look for **"arm64-v8a"** in the list

**Solution C: Update SDK**

1. Go to **Tools** → **SDK Manager**
2. Click **"SDK Tools"** tab
3. Check **"Android Emulator"** (latest version)
4. Click **"Apply"** to update
5. Restart Android Studio
6. Try again

---

### Step 5: Configure AVD (Android Virtual Device)

Now you'll configure the emulator settings:

#### Basic Configuration:

1. **AVD Name**:
    - Enter a descriptive name
    - Example: `"Pixel_7_ARM64_API35"`
    - Example: `"ARM64_Android14"`

2. **Startup Orientation**:
    - Leave as **Portrait** (default)

#### Advanced Settings (IMPORTANT!):

3. Click **"Show Advanced Settings"** button at the bottom

4. **Scroll down** to find these settings:

#### Memory Settings (Critical for AI Models!):

5. **RAM**:
    - **Minimum**: 4096 MB (4 GB)
    - **Recommended**: 6144 MB (6 GB) if your PC has 16GB+ RAM
    - **Maximum**: 8192 MB (8 GB) if your PC has 32GB+ RAM
    - ⚠️ **Don't set too high** - leave RAM for your host computer!

6. **VM Heap**:
    - Leave at default (256 MB or similar)

7. **Internal Storage**:
    - **Minimum**: 4096 MB (4 GB)
    - **Recommended**: 8192 MB (8 GB) for AI models
    - This is where models will be stored

8. **SD Card**:
    - Optional: 512 MB (or leave empty)

#### Performance Settings:

9. **Graphics**:
    - **Automatic** (recommended)
    - Or **Hardware - GLES 2.0**

10. **Boot Option**:
    - **Cold boot** (for testing)
    - Or **Quick boot** (for daily use)

11. **Multi-Core CPU**:
    - Set to **2** or **4** cores (if your PC has 4+ cores)

#### Network Settings:

12. Ensure these are enabled:
    - ✅ **Enable Device Frame** (optional, for visual)
    - ✅ **Enable Keyboard Input**

13. Click **"Finish"** button

---

### Step 6: Verify ARM64 Emulator Created

Back in Device Manager, you should see:

```
Your New Emulator:
Name: Pixel_7_ARM64_API35 (or your chosen name)
Type: arm64-v8a
API: 35 (or 34)
Status: Ready to launch
```

---

### Step 7: Launch ARM64 Emulator

1. In Device Manager, find your new ARM64 emulator
2. Click the **▶ (Play/Launch)** button
3. **Wait for emulator to boot** (first boot takes 2-5 minutes)

#### What to Expect:

- **First Launch**: 3-5 minutes (slow, system setup)
- **Subsequent Launches**: 30-60 seconds (faster)
- **Performance**: Slower than x86_64 (this is normal!)

---

### Step 8: Install Your App on ARM64 Emulator

Once the ARM64 emulator is running:

**Option A: From Android Studio**

1. Select the ARM64 emulator from device dropdown
2. Click **Run** (green play button)
3. App will install and launch

**Option B: From Terminal**

```powershell
cd "C:\Users\aravi\StudioProjects\Vibestate"
.\gradlew.bat installDebug
```

---

### Step 9: Verify Architecture

To confirm you're running on ARM64:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell getprop ro.product.cpu.abi
```

**Expected Output**: `arm64-v8a` ✅

---

### Step 10: Test AI Model Loading

1. **Open your app** on the ARM64 emulator
2. **Navigate to**: Settings → AI Models
3. **Model should already be downloaded** (if not, download again)
4. **Click "Load Model"**
5. **Wait 10-20 seconds**
6. **Should succeed!** ✅

---

## 🎯 Quick Configuration Summary

| Setting | Recommended Value | Why |
|---------|------------------|-----|
| **Device** | Pixel 7 or 8 | Modern, well-supported |
| **System Image** | API 34/35, arm64-v8a | Latest Android with ARM64 |
| **RAM** | 4096-6144 MB | Needed for AI models |
| **Storage** | 8192 MB | Space for models |
| **Graphics** | Automatic/Hardware | Better performance |
| **CPU Cores** | 2-4 | Faster emulation |

---

## 🐛 Troubleshooting

### Issue 1: "ARM64 Images" Tab Not Visible

**Solutions:**

**A. Update Android Emulator**

```
Tools → SDK Manager → SDK Tools → Android Emulator (latest)
Click Apply → OK
Restart Android Studio
```

**B. Check "Show all images"**

- Look for a checkbox in System Image selection
- Enable it to see all available images

**C. Manual SDK Download**

```
Tools → SDK Manager → SDK Platforms tab
Check "Show Package Details"
Expand Android 14.0 (API 34)
Check "ARM 64 v8a System Image"
Click Apply
```

---

### Issue 2: Image Download Fails

**Solutions:**

**A. Check Internet Connection**

- Ensure stable connection
- Try different network if possible

**B. Clear Download Cache**

```
Tools → SDK Manager
Click "SDK Update Sites" tab
Remove and re-add sites
Try download again
```

**C. Manual Download**

- Go to: https://developer.android.com/studio/emulator
- Download ARM64 system image manually
- Place in SDK folder

---

### Issue 3: Emulator Won't Start

**Solutions:**

**A. Check Virtualization Enabled**

- BIOS setting: Intel VT-x or AMD-V must be enabled
- Windows: Hyper-V or WHPX must be configured

**B. Update Graphics Drivers**

- Update your GPU drivers
- Try different Graphics option in AVD config

**C. Reduce RAM**

- If emulator fails to start, reduce RAM allocation
- Try 2048 MB or 3072 MB instead

---

### Issue 4: Emulator is Very Slow

**This is expected!** ARM64 emulation on x86_64 host is slower.

**To improve performance:**

**A. Reduce Resources**

- RAM: 4096 MB (not more)
- CPU Cores: 2 (not more)
- Graphics: Hardware - GLES 2.0

**B. Disable Animations**
In emulator:

```
Settings → Developer Options →
- Window animation scale: Off
- Transition animation scale: Off  
- Animator duration scale: Off
```

**C. Use Physical Device Instead**

- Much better performance
- See physical device setup guide

---

### Issue 5: Can't Find Device Manager

**Location Options:**

1. **Top Menu**: Tools → Device Manager
2. **Right Sidebar**: Phone icon
3. **Quick Search**: Ctrl+Shift+A → type "Device Manager"
4. **View Menu**: View → Tool Windows → Device Manager

---

## 📊 Performance Expectations

| Task | x86_64 Emulator | ARM64 Emulator | Physical Device |
|------|-----------------|----------------|-----------------|
| **Boot Time** | 20-30 sec | 2-5 min | 5-10 sec |
| **App Install** | 5-10 sec | 15-30 sec | 5-10 sec |
| **UI Navigation** | ⚡⚡⚡ Fast | ⚡ Slower | ⚡⚡⚡ Fast |
| **Model Load** | ❌ Fails | ⚡ 20-40 sec | ⚡⚡ 10-20 sec |
| **AI Inference** | ❌ N/A | ⚡ Slow | ⚡⚡ Fast |

---

## ✅ Success Checklist

After creating the ARM64 emulator, verify:

- [ ] Emulator shows in Device Manager
- [ ] Architecture is arm64-v8a (check with adb command)
- [ ] Emulator boots successfully
- [ ] Your app installs
- [ ] Settings → AI Models screen opens
- [ ] Model can be downloaded
- [ ] **Model loads successfully** (no "library not found" error)
- [ ] AI features work

---

## 💡 Pro Tips

### Tip 1: Keep Both Emulators

- Keep x86_64 for fast UI testing
- Use ARM64 only for AI feature testing
- Switch between them as needed

### Tip 2: Snapshot for Faster Boots

- After first successful boot
- Create a snapshot: "After First Boot"
- Speeds up subsequent launches

### Tip 3: Close Unnecessary Apps

- Close other resource-heavy apps
- Free up RAM for emulator
- Improves ARM64 emulation speed

### Tip 4: Consider Physical Device

- If ARM64 emulator is too slow
- Physical device gives best experience
- See physical device setup guide

---

## 📚 Additional Resources

- [Android Emulator Official Docs](https://developer.android.com/studio/run/emulator)
- [ARM64 System Images](https://developer.android.com/studio/run/managing-avds)
- [Emulator Performance Tips](https://developer.android.com/studio/run/emulator-acceleration)

---

## 🎉 Next Steps

Once your ARM64 emulator is ready:

1. ✅ Launch the ARM64 emulator
2. ✅ Install your app: `.\gradlew.bat installDebug`
3. ✅ Open app → Settings → AI Models
4. ✅ Load the model (should work now!)
5. ✅ Test AI features

---

## ❓ Still Having Issues?

If you're still stuck:

1. **Screenshot the issue** (especially the System Image selection screen)
2. **Check SDK Manager** (Tools → SDK Manager) for available downloads
3. **Try Physical Device** (recommended alternative)
4. **Check Device Manager** for the created emulator

**Your AI models will work once you're on ARM64!** 🚀
