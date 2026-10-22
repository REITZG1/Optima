<div align="center">

# Optima

**An Android system-performance tuner for rooted devices**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-23%2B-green.svg)](app/build.gradle)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](app/build.gradle)

</div>

---

Optima is a lightweight tuning utility that gives you direct control over the
parts of Android that are normally hidden behind vendor overlays: CPU governors
and frequency limits, thermal zone temperatures, per-app freezing, display
refresh rate, and a set of shell shortcuts for diagnostics.

Everything is written in plain Java against the public Android framework plus
the Linux `sysfs` / `procfs` interfaces, so there is no native code, no
obfuscation and no analytics.

---

## Table of contents

- [Features](#features)
- [Screens](#screens)
- [Requirements](#requirements)
- [Installation](#installation)
- [Building from source](#building-from-source)
- [Granting root access](#granting-root-access)
- [Project layout](#project-layout)
- [How it works](#how-it-works)
- [Compatibility notes](#compatibility-notes)
- [License](#license)
- [Credits](#credits)

---

## Features

| Area | What you can do |
|---|---|
| **CPU control** | Per-cluster governor switching, min/max frequency limits, online-core management, MTK `sugov_ext` ramp limits |
| **Thermal** | List every thermal zone with its live temperature and policy, inspect trip points |
| **App freezer** | Disable/enable any installed package in bulk, see which packages are currently frozen |
| **Process list** | Live per-process CPU and RSS usage, thread counts, UID |
| **Power stats** | Battery level, current, voltage, power draw, health, cycle count, charge counter |
| **Charge control** | Charge limit, current limit, charge on/off — when the kernel exposes the nodes |
| **Refresh rate** | List supported display modes and pin one, or hand control back to the system |
| **System info** | SoC, core topology, cluster layout, RAM, root status |
| **Shell tools** | Custom command runner, text editor for system files, dmesg/getprop/mount shortcuts, Magisk module list |
| **Integration** | Quick Settings tile, boot receiver, home-screen widget, notification listener, accessibility service |

The app deliberately tells you when a control is **not available** on your
device instead of failing silently — for example when a vendor marks the
frequency nodes read-only.

---

## Screens

<details>
<summary><b>Overview</b></summary>

Live battery state, per-core CPU load, RAM usage and a root-access card with a
one-tap re-check.

</details>

<details>
<summary><b>CPU control</b></summary>

Clusters are detected from `related_cpus`, so a big.LITTLE phone shows the same
groups the kernel actually uses. The screen shows the governor, the current
frequency, the hardware limits and lets you pick a new governor and frequency
range. Nodes that the kernel locks are shown greyed out with an explanation.

</details>

<details>
<summary><b>Thermal editor</b></summary>

All thermal zones of the device with their type, policy and live temperature.

</details>

<details>
<summary><b>App freezer</b></summary>

Every installed package, with the frozen ones flagged. Tapping a package
toggles it. The frozen set is read in a single batch instead of one shell call
per app.

</details>

---

## Requirements

- Android 6.0 (API 23) or newer
- **Root access** for most features — Magisk, KernelSU or any other solution
  that exposes an `su` binary
- A device that exposes the standard `cpufreq`, `thermal` and `power_supply`
  sysfs trees (true for virtually every modern Android phone)

The app is fully usable without root — it just tells you which features need
it.

---

## Installation

Download the APK from the [Releases](../../releases) page, or build it yourself
(see below), then install it:

```bash
adb install -r Optima-1.0.0-debug.apk
```

or copy the APK to the device and open it (you will need to allow installation
from unknown sources).

---

## Building from source

You need **JDK 17+** and the **Android SDK** (platform 34, build-tools 34+).

```bash
git clone https://github.com/REITZG1/Optima.git
cd Optima

# point the build at your SDK
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To build a release-signed variant, add a `keystore.properties` file and point
`app/build.gradle` at it — the project intentionally ships with the debug
signing config so that a fresh clone builds out of the box.

---

## Granting root access

Root solutions decide per app whether a `su` call is allowed, and the app
cannot grant that permission itself.

1. Open your root manager (Magisk / KernelSU / …).
2. Find **Optima** in the superuser list.
3. Grant it **root** access and make sure it is set to *allowed* rather than
   *ask* if you do not want a prompt on every launch.
4. Reopen Optima. The overview screen shows a **Root: Granted** card together
   with the exact `su` invocation that worked, or a description of why it
   failed.

Optima looks for `su` in `/system/bin`, `/system/xbin`, `/sbin`, `/vendor/bin`,
`/system/sbin` and the usual Magisk locations, and supports both the
`su -c <cmd>` and `su 0 <cmd>` calling conventions.

---

## Project layout

```
Optima/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/optima/tuner/
│       │   ├── Optima.java              # Application entry point + preferences
│       │   ├── activities/              # 59 screens
│       │   ├── core/                    # the engine
│       │   │   ├── Background.java      # off-main-thread helper
│       │   │   ├── BatteryManager.java  # power_supply reads and charge control
│       │   │   ├── CpuManager.java      # cpufreq policies, governors, usage
│       │   │   ├── ProcessManager.java  # /proc process + thread statistics
│       │   │   ├── RefreshRateManager.java
│       │   │   ├── Shell.java           # root shell layer
│       │   │   ├── SystemInfo.java
│       │   │   ├── ThermalManager.java
│       │   │   └── ...
│       │   ├── receivers/               # boot receiver, app widget
│       │   └── services/                # tile, accessibility, notifications
│       └── res/                         # layouts, drawables, strings (en, zh-rCN)
├── design/                              # launcher icon sources (SVG)
└── gradle/
```

---

## How it works

All privileged work goes through `core/Shell.java`, which owns a single root
shell. Two execution paths exist:

- **One-shot** — `su -c <command>` for infrequent calls.
- **Persistent** — one long-lived `su` process for polling loops, so a refresh
  cycle does not spawn a process per sample.

A few design rules the code follows:

- **Never block the main thread.** Every screen loads its data on a worker and
  posts the result back. The device you test on may have 50+ thermal zones;
  reading them one shell process at a time would take seconds.
- **Batch aggressively.** The cpufreq tree is read with a single `for` loop
  instead of one `cat` per file, and the frozen-app list is read with one
  `pm list packages -d` instead of one query per package.
- **Report failures honestly.** Writes are read back and compared, so the app
  can tell you that the kernel refused a value instead of pretending it worked.

---

## Compatibility notes

Sysfs layouts differ a lot between vendors, so the app probes instead of
assuming:

- `cpufreq` is read from both the `cpu<N>/cpufreq/` and the `cpufreq/policy<N>/`
  layouts.
- Clusters are grouped by `related_cpus`, falling back to the policy path.
- Write permissions are detected with `stat`, so a read-only `scaling_min_freq`
  is greyed out rather than failing on apply.
- Charge limit and current limit are probed across the usual vendor node
  names; devices without them get a clear "not supported" message.

---

## License

Released under the [MIT License](LICENSE).

---

## Credits

Optima is an **independent, original implementation**. All source code in this
repository was written from scratch for this project; no third-party source
code has been copied, vendored or decompiled into it, and the icon and all
resources are original work.

The feature set reflects functionality that is common across the Android
rooting and tuning community. Optima is not affiliated with, endorsed by, or
derived from any other application or project.

Thanks to everyone who reports bugs and sends device-specific findings — the
sysfs probing logic is much better because of it.

---

<div align="center">

Made with ☕ and a lot of `adb shell` sessions.

</div>
