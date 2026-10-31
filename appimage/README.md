# KunefeSMP Launcher AppImage

This package is built on Linux from the launcher source. The script targets x86_64 and bundles Python dependencies, the launcher resources, and the Java 21 runtime found on the build machine.

## Build on Ubuntu/Debian

Install the Linux build prerequisites:

```bash
sudo apt update
sudo apt install python3 python3-venv python3-tk openjdk-21-jre
```

Install the official `appimagetool` for x86_64 from the [AppImageKit releases](https://github.com/AppImage/AppImageKit/releases). Make the downloaded file executable and place it on `PATH` under the name `appimagetool`.

From the project root, run:

```bash
bash appimage/build-appimage.sh
```

The result is written to:

```text
dist/KunefeSMP-Launcher-4.6-x86_64.AppImage
```

To build a later launcher version, set `APP_VERSION` when running the script, for example `APP_VERSION=4.7 bash appimage/build-appimage.sh`.

## Test and share

Run the generated AppImage on the Linux computer before sharing it. If it fails, launch it from a terminal and copy the error output. AppImage is a direct-download Linux format; it does not publish the app to Flathub. Upload the finished file to the project's website or GitHub Releases.
