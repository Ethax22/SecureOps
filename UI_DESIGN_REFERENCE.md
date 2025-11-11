# SecureOps UI Design Reference

## Design Philosophy

The new SecureOps UI is inspired by modern AI assistant applications with:

- **Glassmorphism**: Frosted glass effects with transparency
- **Purple Gradient Theme**: Sophisticated purple/violet color palette
- **Neon Accents**: Glowing highlights for interactive elements
- **Depth & Layers**: Multiple layers with subtle shadows
- **Clean Typography**: Bold headlines, clear hierarchy

---

## Color Reference Card

### 🎨 Primary Palette

```
Purple        ████ #8B5CF6  - Primary brand color
Purple Dark   ████ #7C3AED  - Hover/active states
Purple Light  ████ #A78BFA  - Disabled states
```

### 🌈 Accent Colors

```
Violet        ████ #9333EA  - Secondary actions
Pink          ████ #EC4899  - Call-to-action
Cyan          ████ #06B6D4  - Info elements
Green         ████ #10B981  - Success states
```

### ⚠️ Status Colors

```
Success       ████ #22C55E  - Passed builds
Warning       ████ #F59E0B  - Pending/queued
Error         ████ #EF4444  - Failed builds
Info          ████ #3B82F6  - Running builds
```

### 🌙 Dark Theme

```
Background    ████ #0F0F1E  - Main background
Surface       ████ #1E1E30  - Card surfaces
Elevated      ████ #252540  - Raised elements
Border        ████ #30A78BFA - Glass borders (30% opacity)
```

---

## Component Showcase

### 1. Glass Card

**Visual Structure:**

```
┌─────────────────────────────────────────┐ ← Purple border (30% opacity)
│  ╔═══════════════════════════════════╗  │
│  ║  Transparent glass background     ║  │ ← #40252540 (40% opacity)
│  ║  with 20dp rounded corners        ║  │
│  ║                                   ║  │
│  ║  Content with full opacity        ║  │
│  ╚═══════════════════════════════════╝  │
└─────────────────────────────────────────┘
```

**Usage:**

- Pipeline cards on Dashboard
- Message bubbles in Voice screen
- Settings items
- Any content container

### 2. Status Badge

**Visual Structure:**

```
╔════════════════════╗
║ ● Success         ║ ← Colored dot + text
╚════════════════════╝
   └─ Colored background (15% opacity)
```

**Variants:**

- **Success**: Green dot + green background
- **Failed**: Red dot + red background
- **Running**: Blue dot + blue background
- **Queued**: Amber dot + amber background

### 3. Neon Chip

**Visual Structure:**

```
╔═══════════════╗ ← Colored border (50% opacity)
║ 🌿 main       ║ ← Optional icon + text
╚═══════════════╝
  └─ Colored background (20% opacity)
```

**Usage:**

- Branch names (green)
- Provider badges (cyan)
- Tags and labels
- Category indicators

### 4. Neon Button

**Visual Structure:**

```
╔════════════════════════════════╗ ← Neon glow border
║  Purple → Pink gradient        ║
║  ┌──────────────────────────┐  ║
║  │  Icon   Button Text      │  ║ ← White text
║  └──────────────────────────┘  ║
╚════════════════════════════════╝
```

**States:**

- **Normal**: Purple-to-pink gradient
- **Hover**: Brighter gradient
- **Disabled**: Gray gradient

---

## Screen Layout Patterns

### Dashboard Screen Layout

```
╔════════════════════════════════════════════╗
║  ╔════════════════════════════════════╗    ║
║  ║ SecureOps Dashboard          🔄    ║ TopBar (transparent)
║  ╚════════════════════════════════════╝    ║
║                                            ║
║  Repository Name                           ║ Section header
║                                            ║
║  ╔═══════════════════════════════════════╗ ║
║  ║ ✓ Success  #123          ⚠️ 85%      ║ ║ Glass card
║  ║                                       ║ ║
║  ║ 🌿 main  Jenkins                      ║ ║ Chips
║  ║                                       ║ ║
║  ║ Fix: Update dependencies              ║ ║ Commit message
║  ║ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  ║ ║ Gradient divider
║  ║ 👤 John Doe        ⏱ 5m 23s          ║ ║ Footer
║  ╚═══════════════════════════════════════╝ ║
║                                            ║
╚════════════════════════════════════════════╝
```

### Voice Screen Layout

```
╔════════════════════════════════════════════╗
║  ╔════════════════════════════════════╗    ║
║  ║ Voice Assistant                     ║    ║ TopBar
║  ╚════════════════════════════════════╝    ║
║                                            ║
║      ╔═════════════════════════════╗       ║ User message
║      ║ 👤 You                      ║       ║ (right-aligned)
║      ║ Check build status          ║       ║
║      ╚═════════════════════════════╝       ║
║                                            ║
║  ╔═════════════════════════════════════╗   ║ AI message
║  ║ 🤖 Assistant                        ║   ║ (left-aligned)
║  ║ Your latest build #123 succeeded!   ║   ║
║  ╚═════════════════════════════════════╝   ║
║                                            ║
║  ╔═══════════════════════════════════════╗ ║ Listening indicator
║  ║    ◉ Listening...                     ║ ║
║  ╚═══════════════════════════════════════╝ ║
║                                            ║
║  💼 Check status    ⚠️ Risky builds?      ║ Suggestion chips
║                                            ║
║                                     ╔═══╗  ║
║                                     ║ 🎤 ║  ║ FAB (gradient)
║                                     ╚═══╝  ║
╚════════════════════════════════════════════╝
```

### Settings Screen Layout

```
╔════════════════════════════════════════════╗
║  ╔════════════════════════════════════╗    ║
║  ║ Settings                            ║    ║ TopBar
║  ╚════════════════════════════════════╝    ║
║                                            ║
║  AI & Models                               ║ Section header
║                                            ║
║  ╔═══════════════════════════════════════╗ ║
║  ║  ╭───╮                                ║ ║ Glass card
║  ║  │ 🧠│  AI Models            →       ║ ║
║  ║  ╰───╯  Download and manage          ║ ║
║  ╚═══════════════════════════════════════╝ ║
║                                            ║
║  Preferences                               ║
║                                            ║
║  ╔═══════════════════════════════════════╗ ║
║  ║  ╭───╮                                ║ ║
║  ║  │ 🌙│  Dark Mode            ●—○     ║ ║ Switch item
║  ║  ╰───╯  Toggle dark theme            ║ ║
║  ╚═══════════════════════════════════════╝ ║
║                                            ║
╚════════════════════════════════════════════╝
```

---

## Gradient Patterns

### Background Gradient (Vertical)

```
Top    #1E1E30 ████████████████
       #0F0F1E ████████████████
Bottom #1A1A2E ████████████████
```

### Button Gradient (Horizontal)

```
Left   #8B5CF6 ████████████████
       #A855F7 ████████████████
Right  #EC4899 ████████████████
```

### Icon Background (Linear)

```
Start  Purple @ 30% opacity
End    Purple @ 10% opacity
```

---

## Icon Treatment Guide

### Standard Icon Sizes

- **Large icons**: 48dp container, 24dp icon (Settings)
- **Medium icons**: 32dp container, 20dp icon (Status)
- **Small icons**: 24dp container, 16dp icon (Inline)
- **Micro icons**: 16dp (Decorative)

### Icon Container Pattern

```
╔════════════════╗
║  ╭──────────╮  ║ ← Rounded container (12dp radius)
║  │          │  ║
║  │   Icon   │  ║ ← Gradient background
║  │          │  ║
║  ╰──────────╯  ║
╚════════════════╝
```

### Color Coding by Category

- **AI/ML**: Pink gradient (#EC4899)
- **Accounts**: Cyan gradient (#06B6D4)
- **Settings**: Purple gradient (#8B5CF6)
- **Analytics**: Green gradient (#10B981)
- **Notifications**: Amber gradient (#F59E0B)

---

## Typography Scale

```
Headline Large   32sp  Bold    SecureOps Dashboard
Headline Medium  28sp  Bold    Voice Assistant
Headline Small   24sp  Bold    #123

Title Large      22sp  Bold    Repository Name
Title Medium     16sp  Bold    Section Headers
Title Small      14sp  Medium  Card Titles

Body Large       16sp  Regular Long content
Body Medium      14sp  Regular Default text
Body Small       12sp  Regular Metadata

Label Large      14sp  Medium  Badges
Label Medium     12sp  Medium  Chips
Label Small      11sp  Regular Timestamps
```

---

## Spacing System

### Standard Padding

- **Screen edges**: 16dp
- **Card padding**: 20dp
- **Between cards**: 16dp
- **Icon + text**: 8-12dp
- **Sections**: 24dp

### Component Spacing

```
Card margins:        16dp
Card padding:        20dp
Icon + text:         12dp
Text lines:          4-8dp
Sections:           24dp
```

---

## Animation Timing

### Suggested Durations (Not yet implemented)

- **Quick**: 150ms - Chip selection, button press
- **Normal**: 300ms - Card appearance, navigation
- **Slow**: 500ms - Complex transitions, gradient shifts

### Easing Functions

- **Standard**: Cubic bezier for most animations
- **Emphasized**: Sharp entrance, gradual exit
- **Decelerate**: Smooth stop for scrolling

---

## Accessibility Features

### Color Contrast Ratios

- **Primary text on glass**: 7:1 (AAA rated)
- **Secondary text on glass**: 4.5:1 (AA rated)
- **Icons on gradient**: 7:1 minimum

### Touch Targets

- **Minimum size**: 48x48dp
- **Cards**: Full width, minimum 72dp height
- **Chips**: Minimum 32dp height
- **Buttons**: Minimum 48dp height

### Screen Reader Support

- All icons have contentDescription
- Status indicated by text + color
- Glass backgrounds don't interfere with text

---

## Implementation Checklist

### ✅ Completed

- [x] Purple gradient color palette
- [x] Glass card component
- [x] Neon button component
- [x] Status badge component
- [x] Neon chip component
- [x] Gradient background
- [x] Dashboard screen redesign
- [x] Voice screen redesign
- [x] Settings screen redesign
- [x] Theme configuration
- [x] Color scheme

### 🚧 In Progress

- [ ] Analytics screen redesign
- [ ] Build details screen redesign
- [ ] Add account screen redesign
- [ ] Manage accounts screen redesign

### 📋 Planned

- [ ] Animated gradients
- [ ] Glow animations
- [ ] Particle effects
- [ ] Custom icon set
- [ ] Splash screen redesign
- [ ] Loading states
- [ ] Error states
- [ ] Empty states

---

## Design Tips

### Do's ✓

- Use glass cards for all content containers
- Apply gradient backgrounds to all screens
- Use colored icon backgrounds consistently
- Maintain proper text contrast
- Use neon chips for tags/labels
- Add gradient dividers between sections

### Don'ts ✗

- Don't use solid backgrounds on cards
- Don't mix old and new components
- Don't use colors outside the palette
- Don't reduce opacity below readability
- Don't forget to wrap screens in GradientBackground
- Don't use standard Material buttons

---

## Quick Reference

### Color Variables

```kotlin
// Primary
PrimaryPurple, PrimaryPurpleDark, PrimaryPurpleLight

// Accents
AccentViolet, AccentPink, AccentCyan, AccentGreen

// Status
SuccessGreen, WarningAmber, ErrorRed, InfoBlue

// Backgrounds
BackgroundDark, SurfaceDark, GlassSurfaceDark

// Effects
NeonPurple, NeonPink, GlassBorderDark
```

### Common Patterns

```kotlin
// Screen wrapper
GradientBackground(modifier = Modifier.fillMaxSize()) {
    Scaffold(containerColor = Color.Transparent) { /* ... */ }
}

// Content card
GlassCard(onClick = { /* ... */ }) {
    // Card content
}

// Status indicator
StatusBadge(status = "Success", color = SuccessGreen)

// Tag/label
NeonChip(text = "main", icon = "🌿", color = AccentGreen)

// Primary action
NeonButton(
    text = "Get Started",
    onClick = { /* ... */ },
    icon = Icons.Default.ArrowForward
)
```

---

## Support & Questions

For questions about implementing this design system or suggestions for improvements, refer to:

- `UI_REDESIGN_SUMMARY.md` - Detailed implementation guide
- `GlassComponents.kt` - Component source code
- `Color.kt` - Complete color definitions
- `Theme.kt` - Theme configuration

---

**Last Updated**: December 2024  
**Design System Version**: 1.0  
**App Version**: 1.0.0
