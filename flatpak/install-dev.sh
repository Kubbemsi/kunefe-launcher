#!/bin/bash
# ══════════════════════════════════════════════════════════════
#  KunefeSMP Launcher — Linux Hızlı Başlatıcı (Geliştirici Modu)
#  Kullanım: bash flatpak/install-dev.sh
# ══════════════════════════════════════════════════════════════

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo -e "${GREEN}=== KunefeSMP Launcher — Linux Doğrudan Çalıştırma ===${NC}"

# Python3 kontrolü
if ! command -v python3 &>/dev/null; then
    echo -e "${RED}[Hata] Python 3 bulunamadı!${NC}"
    echo "Ubuntu/Debian için: sudo apt install python3 python3-pip python3-tk"
    exit 1
fi

# Tkinter kontrolü (GUI arayüzü için zorunludur)
if ! python3 -c "import tkinter" &>/dev/null; then
    echo -e "${YELLOW}[!] Tkinter modülü eksik, kuruluyor...${NC}"
    if command -v apt-get &>/dev/null; then
        sudo apt-get update && sudo apt-get install -y python3-tk
    elif command -v pacman &>/dev/null; then
        sudo pacman -S --noconfirm tk
    elif command -v dnf &>/dev/null; then
        sudo dnf install -y python3-tkinter
    fi
fi

# Gerekli Python paketleri
echo -e "${YELLOW}Gerekli Python kütüphaneleri kontrol ediliyor...${NC}"
pip3 install --user --upgrade customtkinter minecraft-launcher-lib psutil pillow requests 2>/dev/null || \
pip install --user --upgrade customtkinter minecraft-launcher-lib psutil pillow requests

# Launcher'ı başlat
echo -e "${GREEN}KunefeSMP Launcher Başlatılıyor... 🚀${NC}"
cd "$ROOT_DIR"
exec python3 launcher.py "$@"
