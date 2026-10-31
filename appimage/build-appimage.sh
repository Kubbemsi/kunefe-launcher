#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_VERSION="${APP_VERSION:-4.6}"
APP_ID="com.kunefesmp.Launcher"
APP_NAME="KunefeSMP-Launcher"
OUTPUT="$ROOT_DIR/dist/${APP_NAME}-${APP_VERSION}-x86_64.AppImage"

if [[ "$(uname -m)" != "x86_64" ]]; then
    echo "Bu betik x86_64 Linux için hazırlandı. Algılanan mimari: $(uname -m)" >&2
    exit 1
fi

for cmd in python3 appimagetool java; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "Gerekli komut bulunamadı: $cmd" >&2
        echo "Kurulum adımları için appimage/README.md dosyasına bak." >&2
        exit 1
    fi
done

if ! python3 -c 'import tkinter' >/dev/null 2>&1; then
    echo "Python Tk desteği bulunamadı. Dağıtımının python3-tk paketini kurup yeniden dene." >&2
    exit 1
fi

JAVA_BIN="$(readlink -f "$(command -v java)")"
JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
JAVA_VERSION="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1)"
if [[ "$JAVA_VERSION" != "21" ]]; then
    echo "Minecraft istemcisi için Java 21 gerekiyor. Bulunan Java sürümü: ${JAVA_VERSION:-bilinmiyor}" >&2
    echo "Linux'ta Java 21 kurup JAVA_HOME/PATH ayarını yaptıktan sonra tekrar çalıştır." >&2
    exit 1
fi

if ! command -v appimagetool >/dev/null 2>&1; then
    echo "appimagetool bulunamadı. Resmî AppImageKit sürümünü kur; ayrıntı README'de." >&2
    exit 1
fi

for path in "$ROOT_DIR/launcher.py" "$ROOT_DIR/kunefe.ico" "$ROOT_DIR/kunefe.png" "$ROOT_DIR/skins" "$ROOT_DIR/resourcepacks" "$ROOT_DIR/flatpak/com.kunefesmp.Launcher.desktop"; do
    if [[ ! -e "$path" ]]; then
        echo "Proje dosyası eksik: $path" >&2
        exit 1
    fi
done

if [[ -e "$OUTPUT" ]]; then
    echo "Eski çıktı siliniyor: $OUTPUT"
    rm -f "$OUTPUT"
fi

mkdir -p "$ROOT_DIR/dist"
BUILD_DIR="$(mktemp -d "${TMPDIR:-/tmp}/kunefesmp-appimage.XXXXXX")"
cleanup() {
    result=$?
    if [[ $result -eq 0 ]]; then
        rm -rf -- "$BUILD_DIR"
    else
        echo "Derleme başarısız oldu. Geçici dosyalar burada bırakıldı: $BUILD_DIR" >&2
    fi
}
trap cleanup EXIT

VENV="$BUILD_DIR/venv"
python3 -m venv "$VENV"
"$VENV/bin/python" -m pip install --upgrade pip
"$VENV/bin/python" -m pip install -r "$ROOT_DIR/appimage/requirements-linux.txt"

"$VENV/bin/pyinstaller" \
    --noconfirm \
    --clean \
    --onedir \
    --windowed \
    --name "$APP_NAME" \
    --distpath "$BUILD_DIR/dist" \
    --workpath "$BUILD_DIR/work" \
    --specpath "$BUILD_DIR/spec" \
    --icon "$ROOT_DIR/kunefe.ico" \
    --add-data "$ROOT_DIR/kunefe.ico:." \
    --add-data "$ROOT_DIR/skins:skins" \
    --add-data "$ROOT_DIR/resourcepacks:resourcepacks" \
    --collect-all customtkinter \
    --collect-all minecraft_launcher_lib \
    --collect-all PIL \
    --collect-all psutil \
    --hidden-import darkdetect \
    "$ROOT_DIR/launcher.py"

APPDIR="$BUILD_DIR/AppDir"
mkdir -p "$APPDIR/usr/bin" "$APPDIR/usr/lib/jvm/java-21" "$APPDIR/usr/share/icons/hicolor/256x256/apps"
cp -a "$BUILD_DIR/dist/$APP_NAME/." "$APPDIR/usr/bin/"
cp -a "$JAVA_HOME/." "$APPDIR/usr/lib/jvm/java-21/"
cp "$ROOT_DIR/kunefe.png" "$APPDIR/$APP_ID.png"
cp "$ROOT_DIR/kunefe.png" "$APPDIR/usr/share/icons/hicolor/256x256/apps/$APP_ID.png"

cat > "$APPDIR/$APP_ID.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=KunefeSMP Launcher
Name[tr]=KunefeSMP Launcher
Comment=Custom Minecraft client launcher for KunefeSMP
Comment[tr]=KunefeSMP icin ozel Minecraft istemci baslaticisi
Exec=AppRun
Icon=$APP_ID
Categories=Game;
Keywords=minecraft;kunefe;smp;launcher;
Terminal=false
EOF

cat > "$APPDIR/AppRun" <<'EOF'
#!/usr/bin/env bash
set -e
APPDIR="${APPDIR:-$(dirname "$(readlink -f "$0")")}"
export JAVA_HOME="$APPDIR/usr/lib/jvm/java-21"
export PATH="$JAVA_HOME/bin:$APPDIR/usr/bin:$PATH"
exec "$APPDIR/usr/bin/KunefeSMP-Launcher" "$@"
EOF
chmod +x "$APPDIR/AppRun" "$APPDIR/usr/bin/$APP_NAME"
ARCH=x86_64 appimagetool --no-appstream "$APPDIR" "$OUTPUT"
chmod +x "$OUTPUT"
echo "AppImage hazır: $OUTPUT"
