#!/bin/bash
# KunefeSMP Launcher başlatıcı scripti (Flatpak sandbox içinden çalışır)
set -e

# Java 21 yolunu ayarla
export JAVA_HOME=/app/jre
export PATH=$JAVA_HOME/bin:$PATH

# Flatpak veri dizini kontrolü (XDG_DATA_HOME)
KUNEFE_DATA="${XDG_DATA_HOME:-$HOME/.local/share}/kunefesmp"
mkdir -p "$KUNEFE_DATA"

# Flatpak içindeki paylaşılan kaynakları veri dizinine hazırla (eğer yoksa)
if [ -d "/app/share/kunefesmp/resourcepacks" ] && [ ! -d "$KUNEFE_DATA/resourcepacks" ]; then
    mkdir -p "$KUNEFE_DATA/resourcepacks"
    cp -rn /app/share/kunefesmp/resourcepacks/* "$KUNEFE_DATA/resourcepacks/" 2>/dev/null || true
fi

if [ -d "/app/share/kunefesmp/skins" ] && [ ! -d "$KUNEFE_DATA/skins" ]; then
    mkdir -p "$KUNEFE_DATA/skins"
    cp -rn /app/share/kunefesmp/skins/* "$KUNEFE_DATA/skins/" 2>/dev/null || true
fi

# Launcher'ı Python ile çalıştır
exec python3 /app/bin/launcher.py "$@"
