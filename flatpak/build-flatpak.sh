#!/bin/bash
# ══════════════════════════════════════════════════════════════
#  KunefeSMP Launcher — Flatpak Otomatik Derleme & Paketleme
#  Kullanım: bash flatpak/build-flatpak.sh
# ══════════════════════════════════════════════════════════════

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=== KunefeSMP Flatpak Derleme Aracı ===${NC}"

# flatpak ve flatpak-builder kontrolü
for cmd in flatpak flatpak-builder; do
    if ! command -v $cmd &>/dev/null; then
        echo -e "${RED}[Hata] '$cmd' bulunamadı!${NC}"
        echo "Lütfen kurun:"
        echo "  Ubuntu/Debian: sudo apt install flatpak flatpak-builder"
        echo "  Arch Linux:    sudo pacman -S flatpak flatpak-builder"
        echo "  Fedora:        sudo dnf install flatpak flatpak-builder"
        exit 1
    fi
done

# Flathub deposunu ekle
echo -e "${YELLOW}[1/4] Flathub ve Freedesktop SDK runtime kontrol ediliyor...${NC}"
flatpak remote-add --if-not-exists --user flathub https://flathub.org/repo/flathub.flatpakrepo

# Gerekli SDK ve Platform sürümlerini kur
flatpak install --user -y --noninteractive flathub org.freedesktop.Platform//25.08 org.freedesktop.Sdk//25.08 2>/dev/null || true
flatpak install --user -y --noninteractive flathub org.freedesktop.Sdk.Extension.openjdk21//25.08 2>/dev/null || true

# Proje ana dizinini tespit et
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

# Derleme (yerel test manifesti: kaynak kodu dogrudan proje dizininden alir)
echo -e "${YELLOW}[2/4] Flatpak paketi derleniyor...${NC}"
flatpak-builder --force-clean --user --install-deps-from=flathub --repo=flatpak-repo build-dir flatpak/com.kunefesmp.Launcher.test.yml

# Tek dosyalık dağıtılabilir .flatpak bundle oluştur
echo -e "${YELLOW}[3/4] Dağıtılabilir 'KunefeSMP-Launcher.flatpak' oluşturuluyor...${NC}"
flatpak build-bundle flatpak-repo KunefeSMP-Launcher.flatpak com.kunefesmp.Launcher

echo -e "${GREEN}[4/4] İşlem başarıyla tamamlandı! 🎉${NC}"
echo ""
echo -e "${GREEN}Oluşturulan Dosya: ${ROOT_DIR}/KunefeSMP-Launcher.flatpak${NC}"
echo ""
echo "Bu .flatpak dosyasını herhangi bir Linux / Steam Deck bilgisayara aktarıp şu komutla kurabilirsiniz:"
echo "  flatpak install KunefeSMP-Launcher.flatpak"
echo ""
echo "Başlatmak için:"
echo "  flatpak run com.kunefesmp.Launcher"
