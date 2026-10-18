# 🐧 KunefeSMP Launcher — Linux Kurulum & Paketleme Kılavuzu

KunefeSMP Client Launcher artık **Windows**, **Linux** ve **macOS** üzerinde tam uyumlu olarak çalışabilmektedir.

---

## 📦 1. Yöntem: Flatpak Paketi Oluşturma (Tüm Dağıtımlar & Steam Deck)

Flatpak, uygulamanın gerekli Java 21, Python ortamı ve tüm kütüphaneler ile birlikte tek bir izole (.flatpak) paket haline getirilmesini sağlar. Ubuntu, Debian, Fedora, Arch Linux ve Steam Deck üzerinde sorunsuz çalışır.

### Flatpak Paketini Derleme Adımları:

1. **Gerekli araçları kurun (Derleme yapacak makinede bir defaya mahsus):**
   ```bash
   # Ubuntu / Debian / Linux Mint:
   sudo apt install flatpak flatpak-builder

   # Fedora:
   sudo dnf install flatpak flatpak-builder

   # Arch Linux / Manjaro:
   sudo pacman -S flatpak flatpak-builder
   ```

2. **Otomatik derleme scriptini çalıştırın:**
   ```bash
   bash flatpak/build-flatpak.sh
   ```
   *Bu script Freedesktop SDK 24.08 ve OpenJDK 21 eklentilerini kurar, launcher'ı derler ve tek parça `KunefeSMP-Launcher.flatpak` dosyasını üretir.*

3. **Oluşan `.flatpak` Dosyasını Oyuncular Nasıl Kurar?**
   Oyuncular indirdikleri `.flatpak` dosyasını terminalden veya grafik arayüzle tek komutla kurabilir:
   ```bash
   flatpak install KunefeSMP-Launcher.flatpak
   ```
   Ve başlatmak için:
   ```bash
   flatpak run com.kunefesmp.Launcher
   ```
   *(Ayrıca sistemin uygulama menüsüne "KunefeSMP Launcher" olarak otomatik eklenir).*

---

## ⚡ 2. Yöntem: Doğrudan Python ile Çalıştırma (Hızlı / Geliştirici)

Flatpak derlemesi beklemeden doğrudan çalıştırmak için:

1. **Gereksinimleri kurun:**
   ```bash
   # Ubuntu / Debian:
   sudo apt install python3 python3-pip python3-tk

   # Fedora:
   sudo dnf install python3 python3-pip python3-tkinter

   # Arch Linux:
   sudo pacman -S python python-pip tk
   ```

2. **Hızlı başlatıcıyı çalıştırın:**
   ```bash
   bash flatpak/install-dev.sh
   ```
   veya doğrudan:
   ```bash
   python3 launcher.py
   ```

---

## 📁 Linux'ta Dosya Konumları

- **Flatpak Kurulumunda:**  
  `~/.var/app/com.kunefesmp.Launcher/data/kunefesmp/`
- **Doğrudan Python Kurulumunda:**  
  `~/.kunefesmp/` (XDG_DATA_HOME tanımlıysa `~/.local/share/kunefesmp/`)

Resource pack, skinler ve Fabric modları bu dizinde saklanır.
