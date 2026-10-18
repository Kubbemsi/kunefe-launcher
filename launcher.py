import customtkinter as ctk
import minecraft_launcher_lib
import subprocess, json, os, sys, hashlib, threading, uuid as uuid_lib, socket, time, zipfile
import psutil
import shutil
from PIL import Image, ImageDraw

# ══════════════════════════════════════════════════
#  KunefeSMP Client Launcher (Özel Kilitli Sürüm v4.5)
# ══════════════════════════════════════════════════

SERVER_NAME  = "KunefeSMP Resmi Sunucusu"
SERVER_IP    = "oyna.kunefesmp.com.tr"
SERVER_PORT  = "25565"
CLIENT_BRAND = "Kunefe Client"
LAUNCHER_SECRET = "kunefe-gizli-anahtar-degistir"
AUTH_PORT    = 7899

if sys.platform.startswith("linux") and os.environ.get("XDG_DATA_HOME"):
    MC_DIR = os.path.join(os.environ["XDG_DATA_HOME"], "kunefesmp")
elif sys.platform == "darwin":
    MC_DIR = os.path.join(os.path.expanduser("~"), "Library", "Application Support", "kunefesmp")
else:
    MC_DIR = os.path.join(os.path.expanduser("~"), ".kunefesmp")
USERS_FILE   = os.path.join(MC_DIR, "users.json")
CONFIG_FILE  = os.path.join(MC_DIR, "config.json")
PROFILES_FILE= os.path.join(MC_DIR, "profiles.json")
SERVERS_DAT  = os.path.join(MC_DIR, "servers.dat")
RP_DIR       = os.path.join(MC_DIR, "resourcepacks")
SKINS_DIR    = os.path.join(MC_DIR, "skins")
PACK_DIR     = os.path.join(RP_DIR, "KunefeClient")
ZIP_PACK     = os.path.join(RP_DIR, "KunefeClient.zip")
MODS_DIR     = os.path.join(MC_DIR, "mods")
MODS_CFG     = os.path.join(MC_DIR, "kunefe_mods.json")

# ─── MOD LİSTELERİ ───────────────────────────────────────────────────────────
# Zorunlu modlar — kullanıcı görmez, otomatik kurulur
REQUIRED_MODS = [
    {"slug": "appleskin",                "name": "AppleSkin"},
    {"slug": "sodium",                   "name": "Sodium"},
    {"slug": "fabric-api",               "name": "Fabric API"},
    {"slug": "libipn",                   "name": "libIPN"},
    {"slug": "cloth-config",             "name": "Cloth Config API"},
    {"slug": "fabric-language-kotlin",   "name": "Fabric Language Kotlin"},
    {"slug": "sound-physics-remastered", "name": "Sound Physics Remastered"},
    {"slug": "viafabric",                "name": "ViaFabric"},
    {"slug": "audio-player",             "name": "Audio Player"},
    {"slug": "modmenu",                  "name": "Mod Menu"},
    {"slug": "simple-voice-chat",        "name": "Simple Voice Chat"},
    {"slug": "crosshair-indicator",      "name": "Crosshair Indicator"},
    {"slug": "customskinloader",        "name": "CustomSkinLoader"},
]

# İsteğe bağlı modlar — kullanıcı seçer
OPTIONAL_MODS = {
    "⚡ FPS & Performans": [
        {"slug": "lithium",          "name": "Lithium",           "desc": "Oyun mantığı optimizasyonu",     "default": True},
        {"slug": "ferritecore",      "name": "FerriteCore",       "desc": "RAM kullanımını azaltır",         "default": True},
        {"slug": "entityculling",    "name": "Entity Culling",    "desc": "Görünmez entity render etmez",   "default": True},
        {"slug": "immediatelyfast",  "name": "ImmediatelyFast",   "desc": "Render hızını artırır",           "default": False},
        {"slug": "krypton",          "name": "Krypton",           "desc": "Ağ paketi optimizasyonu",         "default": False},
        {"slug": "moreculling",      "name": "More Culling",      "desc": "Ekstra render optimizasyonu",     "default": False},
        {"slug": "lazydfu",          "name": "LazyDFU",           "desc": "Oyun başlangıcını hızlandırır",   "default": False},
    ],
    "⚔️ PVP": [
        {"slug": "betterhitboxes",   "name": "Better Hitboxes",   "desc": "Düzeltilmiş hitbox görünümü",    "default": False},
        {"slug": "no-chat-reports",  "name": "No Chat Reports",   "desc": "Sohbet raporlamayı engeller",    "default": True},
        {"slug": "clear-skies",      "name": "Clear Skies",       "desc": "Net gökyüzü (sis efekti kaldır)","default": False},
        {"slug": "dynamic-crosshair","name": "Dynamic Crosshair", "desc": "Dinamik nişangah",               "default": False},
    ],
    "🎮 Kalite (QoL)": [
        {"slug": "shulkerboxtooltip","name": "Shulker Box Tooltip", "desc": "Sandık içeriğini önizler",    "default": False},
        {"slug": "jade",             "name": "Jade",              "desc": "Blok/Mob bilgisi gösterir",       "default": False},
        {"slug": "roughly-enough-items","name": "REI",            "desc": "Tarif & item görüntüleyici",      "default": False},
        {"slug": "continuity",       "name": "Continuity",        "desc": "Bağlı doku (CTM) desteği",        "default": False},
    ],
}

# Tema Renkleri
BG      = "#07071a"
SIDEBAR = "#0c0c22"
PANEL   = "#10102a"
CARD    = "#16163a"
ACCENT  = "#ff6200"
TEXT    = "#e8e8ff"
DIM     = "#5a5a8a"
SUCCESS = "#00e676"
ERROR   = "#ff3355"
INPUT   = "#0c0c22"
BORDER  = "#252550"
GOLD    = "#ffd700"

FPS_PRESETS = {
    "⚡ Performans": {
        "render": 6, "sim": 5, "fps": 120, "graphics": 0,
        "ao": 0, "shadows": "false", "particles": 2,
        "jvm": ["-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled",
                "-XX:MaxGCPauseMillis=200", "-XX:G1NewSizePercent=30"]
    },
    "⚖️ Dengeli": {
        "render": 10, "sim": 8, "fps": 60, "graphics": 1,
        "ao": 1, "shadows": "true", "particles": 1,
        "jvm": ["-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200"]
    },
    "🔥 Ultra": {
        "render": 16, "sim": 12, "fps": 260, "graphics": 1,
        "ao": 2, "shadows": "true", "particles": 0,
        "jvm": ["-XX:+UseG1GC", "-XX:MaxGCPauseMillis=50"]
    }
}

ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("dark-blue")

# ── Yardımcı Fonksiyonlar ─────────────────────────

def open_folder(path):
    """Platform'a uygun dosya gezgini açar."""
    os.makedirs(path, exist_ok=True)
    try:
        if sys.platform == "win32":
            os.startfile(path)
        elif sys.platform == "darwin":
            subprocess.Popen(["open", path])
        else:
            subprocess.Popen(["xdg-open", path])
    except Exception as e:
        print(f"Klasör açılamadı: {e}")

def ensure_dirs():
    os.makedirs(MC_DIR, exist_ok=True)
    os.makedirs(RP_DIR, exist_ok=True)
    os.makedirs(SKINS_DIR, exist_ok=True)

def hash_pw(pw):
    return hashlib.sha256(pw.encode()).hexdigest()

def load_users():
    if not os.path.exists(USERS_FILE): return {}
    with open(USERS_FILE, "r", encoding="utf-8") as f: return json.load(f)

def save_users(u):
    with open(USERS_FILE, "w", encoding="utf-8") as f: json.dump(u, f, indent=2)

def load_cfg():
    d = {"ram": "2", "last_user": "", "fps_preset": "⚖️ Dengeli", "skin": "default", "resource_pack": "KunefeClient.zip", "active_profile": "Varsayılan", "version": "", "remember_me": False, "auto_login_ingame": False}
    if not os.path.exists(CONFIG_FILE): return d
    with open(CONFIG_FILE, "r", encoding="utf-8") as f: return {**d, **json.load(f)}

def save_cfg(c):
    with open(CONFIG_FILE, "w", encoding="utf-8") as f: json.dump(c, f, indent=2)

def load_profiles():
    d = {
        "Varsayılan": {"ram": "2", "fps_preset": "⚖️ Dengeli", "skin": "default", "resource_pack": "KunefeClient.zip"}
    }
    if not os.path.exists(PROFILES_FILE): return d
    try:
        with open(PROFILES_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)
            return data if data else d
    except Exception:
        return d

def save_profiles(p):
    try:
        with open(PROFILES_FILE, "w", encoding="utf-8") as f:
            json.dump(p, f, indent=2, ensure_ascii=False)
    except Exception:
        pass

def get_available_skins():
    skins = []
    base_dir = getattr(sys, '_MEIPASS', os.path.dirname(os.path.abspath(__file__)))
    dirs = [SKINS_DIR, os.path.join(base_dir, "skins"), os.path.join(os.path.dirname(os.path.abspath(__file__)), "skins")]
    for d in dirs:
        if os.path.exists(d):
            try:
                for f in os.listdir(d):
                    if f.lower().endswith(".png") and f not in skins:
                        skins.append(f)
            except Exception:
                pass
    return skins or ["default"]

def get_available_resource_packs():
    packs = ["KunefeClient.zip"]
    base_dir = getattr(sys, '_MEIPASS', os.path.dirname(os.path.abspath(__file__)))
    dirs = [RP_DIR, os.path.join(base_dir, "resourcepacks"), os.path.join(os.path.dirname(os.path.abspath(__file__)), "resourcepacks")]
    for d in dirs:
        if os.path.exists(d):
            try:
                for f in os.listdir(d):
                    if f.lower().endswith(".zip") and f not in packs:
                        packs.append(f)
            except Exception:
                pass
    return packs

def download_skin_by_name(name):
    try:
        import urllib.request, base64
        url = f"https://api.mojang.com/users/profiles/minecraft/{name}"
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        res = json.loads(urllib.request.urlopen(req, timeout=6).read().decode("utf-8"))
        uuid = res.get("id")
        if not uuid: return False
        s_url = f"https://sessionserver.mojang.com/session/minecraft/profile/{uuid}"
        s_req = urllib.request.Request(s_url, headers={"User-Agent": "Mozilla/5.0"})
        s_res = json.loads(urllib.request.urlopen(s_req, timeout=6).read().decode("utf-8"))
        for prop in s_res.get("properties", []):
            if prop.get("name") == "textures":
                val = json.loads(base64.b64decode(prop["value"]).decode("utf-8"))
                skin_url = val.get("textures", {}).get("SKIN", {}).get("url")
                if skin_url:
                    img_data = urllib.request.urlopen(skin_url, timeout=6).read()
                    save_path = os.path.join(SKINS_DIR, f"{name}.png")
                    with open(save_path, "wb") as f:
                        f.write(img_data)
                    return True
    except Exception as e:
        print(f"Skin indirme hatası: {e}")
    return False

def upload_player_skin(username, skin_file_path):
    """Kullanıcının seçtiği skin dosyasını KunefeSMP sunucusuna yükler."""
    if not username or not skin_file_path or not os.path.exists(skin_file_path):
        return False
    try:
        import urllib.request
        with open(skin_file_path, "rb") as f:
            data = f.read()
        if len(data) < 100:
            return False
        req = urllib.request.Request(
            f"http://{SERVER_IP}:{AUTH_PORT}/kunefe-skin",
            data=data,
            method="POST"
        )
        req.add_header("X-Launcher-Secret", LAUNCHER_SECRET)
        req.add_header("X-Username", username)
        req.add_header("Content-Type", "image/png")
        with urllib.request.urlopen(req, timeout=5) as res:
            return res.status == 200
    except Exception as e:
        print(f"[KunefeSkin] Sunucuya skin yükleme hatası: {e}")
        return False

def get_active_skin_path(username, skin_name=None):
    """Aktif skin dosyasının tam yolunu döner."""
    if not skin_name:
        cfg = load_cfg()
        skin_name = cfg.get("skin", "default")

    if skin_name and skin_name != "default":
        base_dir = getattr(sys, '_MEIPASS', os.path.dirname(os.path.abspath(__file__)))
        for check_dir in [SKINS_DIR, os.path.join(base_dir, "skins"), os.path.join(os.path.dirname(os.path.abspath(__file__)), "skins")]:
            p = os.path.join(check_dir, skin_name)
            if os.path.exists(p):
                return p

    if username:
        user_skin = os.path.join(SKINS_DIR, f"{username}.png")
        if os.path.exists(user_skin):
            return user_skin

    return None

def setup_custom_skin_loader(username, skin_path=None):
    """CustomSkinLoader modunun konfigürasyonunu ve yerel skinleri hazırlar."""
    try:
        csl_dir = os.path.join(MC_DIR, "CustomSkinLoader")
        local_skins_dir = os.path.join(csl_dir, "LocalSkin", "skins")
        os.makedirs(local_skins_dir, exist_ok=True)

        # Eski önbelleği temizle (yeni yüklenen skinler hemen görünsün)
        cache_dir = os.path.join(csl_dir, "caches")
        if os.path.exists(cache_dir):
            try:
                shutil.rmtree(cache_dir, ignore_errors=True)
            except Exception:
                pass

        # Oyuncunun kendi skinini yerel klasöre de kopyala (çevrimdışı da anında gözüksün)
        if skin_path and os.path.exists(skin_path) and username:
            dest_local = os.path.join(local_skins_dir, f"{username}.png")
            try:
                shutil.copyfile(skin_path, dest_local)
            except Exception:
                pass

        csl_json_path = os.path.join(csl_dir, "CustomSkinLoader.json")
        csl_config = {
            "enable": True,
            "loadlist": [
                {
                    "name": "LocalSkin",
                    "type": "Legacy",
                    "checkPNG": False,
                    "skin": "LocalSkin/skins/{USERNAME}.png",
                    "model": "auto"
                },
                {
                    "name": "KunefeSMP",
                    "type": "Legacy",
                    "checkPNG": False,
                    "skin": f"http://{SERVER_IP}:{AUTH_PORT}/skins/{{USERNAME}}.png",
                    "model": "auto"
                },
                {
                    "name": "Mojang",
                    "type": "MojangAPI"
                },
                {
                    "name": "ElyBy",
                    "type": "ElyByAPI"
                }
            ]
        }
        with open(csl_json_path, "w", encoding="utf-8") as f:
            json.dump(csl_config, f, indent=2, ensure_ascii=False)
    except Exception as e:
        print(f"[CustomSkinLoader] Konfigürasyon oluşturma hatası: {e}")

def get_allowed_server_ips():
    ips = {"132.145.255.254"}
    try:
        _, _, ip_list = socket.gethostbyname_ex(SERVER_IP)
        ips.update(ip_list)
    except Exception:
        pass
    return ips

def lock_servers_dat():
    """Minecraft'ın servers.dat dosyasında sadece KunefeSMP'yi kilitler"""
    try:
        name_b = SERVER_NAME.encode("utf-8")
        ip_b = f"{SERVER_IP}:{SERVER_PORT}".encode("utf-8")
        
        data = bytearray()
        data.extend(b"\x0a\x00\x00")
        data.extend(b"\x09\x00\x07servers")
        data.extend(b"\x0a\x00\x00\x00\x01")
        
        data.extend(b"\x08\x00\x04name")
        data.extend(len(name_b).to_bytes(2, "big"))
        data.extend(name_b)
        
        data.extend(b"\x08\x00\x02ip")
        data.extend(len(ip_b).to_bytes(2, "big"))
        data.extend(ip_b)
        
        data.append(0x00)
        data.append(0x00)
        
        with open(SERVERS_DAT, "wb") as f:
            f.write(data)
    except Exception:
        pass

def clean_unauthorized_mods():
    """Harici hile modlarını temizler"""
    mods_dir = os.path.join(MC_DIR, "mods")
    if os.path.exists(mods_dir):
        for file in os.listdir(mods_dir):
            if file.endswith(".jar"):
                try:
                    os.remove(os.path.join(mods_dir, file))
                except Exception:
                    pass

def detect_version_pack_format(version_id):
    """
    Oyunun .jar dosyasını analiz edip Minecraft'ın beklediği
    kesin 'pack_format' değerini bulur (26.3 için 97, 1.21 için 46 vb.)
    """
    jar_path = os.path.join(MC_DIR, "versions", version_id, f"{version_id}.jar")
    if os.path.exists(jar_path):
        try:
            with zipfile.ZipFile(jar_path, "r") as z:
                if "version.json" in z.namelist():
                    v_data = json.loads(z.read("version.json"))
                    pack_ver = v_data.get("pack_version", {})
                    if "resource_major" in pack_ver:
                        return pack_ver["resource_major"]
        except Exception:
            pass
    return 97

def setup_kunefe_resource_pack(version_id="26.3"):
    """
    KunefeSMP Özel Resource Pack'ini oluşturur ve .zip olarak derler:
    - Minecraft sürümünün kesin formatına uygun mcmeta
    - Koyu obsidyen & turuncu parlak butonlar (button.png, widgets.png)
    - 9-slice ölçekleme tanımları (.mcmeta)
    - ESC Menüsü metin özelleştirmeleri (tr_tr ve en_us)
    - Kullanıcı kendi çizimini eklemişse korur, ezmez!
    """
    try:
        os.makedirs(PACK_DIR, exist_ok=True)
        pack_fmt = detect_version_pack_format(version_id)

        # 1. pack.mcmeta (Sürümün beklediği kesin formata göre ayarlanır)
        if pack_fmt >= 65:
            mcmeta = {
                "pack": {
                    "pack_format": pack_fmt,
                    "min_format": 65,
                    "max_format": 150,
                    "description": "§6§lKunefeSMP §r§7Özel Buton & Arayüz Paketi"
                }
            }
        else:
            mcmeta = {
                "pack": {
                    "pack_format": pack_fmt,
                    "description": "§6§lKunefeSMP §r§7Özel Buton & Arayüz Paketi"
                }
            }
        with open(os.path.join(PACK_DIR, "pack.mcmeta"), "w", encoding="utf-8") as f:
            json.dump(mcmeta, f, indent=2)

        # 2. Buton Dokuları (Koyu Tema & Turuncu Çerçeve)
        sprites_dir = os.path.join(PACK_DIR, "assets", "minecraft", "textures", "gui", "sprites", "widget")
        os.makedirs(sprites_dir, exist_ok=True)

        def make_button(bg_color, border_outer, border_inner):
            img = Image.new("RGBA", (200, 20), color=(0, 0, 0, 0))
            d = ImageDraw.Draw(img)
            d.rectangle([1, 1, 198, 18], fill=bg_color)
            d.rectangle([0, 0, 199, 19], outline=border_outer, width=1)
            if border_inner:
                d.rectangle([1, 1, 198, 18], outline=border_inner, width=1)
            return img

        btn_normal_path = os.path.join(sprites_dir, "button.png")
        btn_hover_path  = os.path.join(sprites_dir, "button_highlighted.png")
        btn_dis_path    = os.path.join(sprites_dir, "button_disabled.png")

        # Normal: Koyu obsidyen, turuncu ince çerçeve
        btn_normal = make_button((20, 20, 32, 240), (255, 119, 0, 160), (35, 35, 55, 255))
        btn_normal.save(btn_normal_path)

        # Hover & Focus: Koyu kehribar zemin, parlayan altın & turuncu çift çerçeve (odaklanınca kör etmez)
        btn_hover = make_button((50, 25, 10, 245), (255, 215, 0, 255), (255, 120, 0, 255))
        btn_hover.save(btn_hover_path)

        # Pasif: Sönük koyu
        btn_disabled = make_button((14, 14, 22, 180), (40, 40, 50, 255), None)
        btn_disabled.save(btn_dis_path)

        # 9-slice ölçekleme dosyaları
        mcmeta_slice = {
            "gui": {
                "scaling": {
                    "type": "nine_slice",
                    "width": 200,
                    "height": 20,
                    "border": 3
                }
            }
        }
        for name in ["button.png.mcmeta", "button_highlighted.png.mcmeta", "button_disabled.png.mcmeta"]:
            with open(os.path.join(sprites_dir, name), "w", encoding="utf-8") as f:
                json.dump(mcmeta_slice, f, indent=2)

        # Eski sürümler için klasik widgets.png
        gui_dir = os.path.join(PACK_DIR, "assets", "minecraft", "textures", "gui")
        os.makedirs(gui_dir, exist_ok=True)
        widgets = Image.new("RGBA", (256, 256), color=(0, 0, 0, 0))
        widgets.paste(btn_disabled, (0, 46))
        widgets.paste(btn_normal, (0, 66))
        widgets.paste(btn_hover, (0, 86))
        widgets.save(os.path.join(gui_dir, "widgets.png"))

        # 3. Dil Özelleştirmeleri (ESC Menüsü, Butonlar)
        lang_dir = os.path.join(PACK_DIR, "assets", "minecraft", "lang")
        os.makedirs(lang_dir, exist_ok=True)

        tr_lang = {
            "menu.game": "§6§lKunefeSMP §rOyun Menüsü",
            "menu.disconnect": "§c§l✖ KunefeSMP'den Çıkış Yap",
            "menu.returnToGame": "§a▶ Oyuna Devam Et",
            "menu.options": "§e⚙ Kunefe Ayarları",
            "menu.shareToLan": "§7Yerel Ağda Paylaş",
            "menu.sendFeedback": "§6💬 Kunefe Destek",
            "menu.reportBugs": "§e🐞 Hata Bildir",
            "gui.toTitle": "§c§l✖ KunefeSMP'yi Kapat",
            "connect.connecting": "§6KunefeSMP'ye bağlanılıyor...",
            "connect.authorizing": "§eHesap doğrulanıyor...",
            "connect.joining": "§aDünyaya giriş yapılıyor...",
            "multiplayer.title": "§6KunefeSMP Çok Oyunculu",
            "title.multiplayer.other": "§6KunefeSMP Resmi Sunucusu",
            "deathScreen.respawn": "§a⚔ Yeniden Doğ",
            "deathScreen.titleScreen": "§cKunefeSMP'den Çık"
        }
        with open(os.path.join(lang_dir, "tr_tr.json"), "w", encoding="utf-8") as f:
            json.dump(tr_lang, f, ensure_ascii=False, indent=2)

        en_lang = {
            "menu.game": "§6§lKunefeSMP §rGame Menu",
            "menu.disconnect": "§c§l✖ Exit KunefeSMP",
            "menu.returnToGame": "§a▶ Back to Game",
            "menu.options": "§e⚙ Kunefe Settings",
            "gui.toTitle": "§c§l✖ Exit KunefeSMP",
            "connect.connecting": "§6Connecting to KunefeSMP...",
            "connect.authorizing": "§eAuthorizing...",
            "connect.joining": "§aJoining KunefeSMP...",
            "deathScreen.respawn": "§a⚔ Respawn",
            "deathScreen.titleScreen": "§cExit KunefeSMP"
        }
        with open(os.path.join(lang_dir, "en_us.json"), "w", encoding="utf-8") as f:
            json.dump(en_lang, f, ensure_ascii=False, indent=2)

        # 4. pack.png Logosu (128x128)
        pack_png = os.path.join(PACK_DIR, "pack.png")
        if not os.path.exists(pack_png):
            img = Image.new("RGBA", (128, 128), color=(15, 15, 30, 255))
            d = ImageDraw.Draw(img)
            d.rectangle([2, 2, 125, 125], outline=(255, 119, 0, 255), width=3)
            d.rectangle([6, 6, 121, 121], outline=(255, 215, 0, 180), width=1)
            d.text((64, 40), "KUNEFE", fill=(255, 160, 0), anchor="mm")
            d.text((64, 65), "SMP", fill=(255, 255, 255), anchor="mm")
            d.text((64, 90), "OFFICIAL CLIENT", fill=(150, 150, 220), anchor="mm")
            img.save(pack_png)

        # 5. KunefeClient.zip Paketini Oluştur
        with zipfile.ZipFile(ZIP_PACK, "w", zipfile.ZIP_DEFLATED) as zipf:
            for root, dirs, files in os.walk(PACK_DIR):
                for file in files:
                    full_path = os.path.join(root, file)
                    rel_path = os.path.relpath(full_path, PACK_DIR)
                    zipf.write(full_path, rel_path)

    except Exception as e:
        print(f"Resource pack hatası: {e}")

def get_versions_1_21_plus():
    try:
        all_v = minecraft_launcher_lib.utils.get_version_list()
        result = []
        for v in all_v:
            if v["type"] != "release": continue
            try:
                parts = tuple(int(x) for x in v["id"].split("."))
                if parts >= (1, 21): result.append(v["id"])
            except Exception:
                continue
        return result or ["26.3", "26.2", "1.21.4"]
    except Exception:
        return ["26.3", "26.2", "1.21.4"]

def apply_options(preset_name):
    preset = FPS_PRESETS.get(preset_name, FPS_PRESETS["⚖️ Dengeli"])
    path = os.path.join(MC_DIR, "options.txt")
    settings = {}
    if os.path.exists(path):
        try:
            with open(path, "r", encoding="utf-8", errors="ignore") as f:
                for line in f:
                    if ":" in line:
                        k, v = line.strip().split(":", 1)
                        settings[k] = v
        except Exception:
            pass

    cfg = load_cfg()
    rp_name = cfg.get("resource_pack", "KunefeClient.zip")
    rp_entry = f"file/{rp_name}" if not rp_name.startswith("file/") and rp_name != "vanilla" else rp_name

    packs_list = [rp_entry, "vanilla"] if rp_entry != "vanilla" else ["vanilla"]
    settings.update({
        "renderDistance":     str(preset["render"]),
        "simulationDistance": str(preset["sim"]),
        "fpsLimit":           str(preset["fps"]),
        "graphics":           str(preset["graphics"]),
        "ao":                 str(preset["ao"]),
        "entityShadows":      preset["shadows"],
        "particles":          str(preset["particles"]),
        "resourcePacks":      json.dumps(packs_list, separators=(',', ':')),
        "incompatibleResourcePacks": json.dumps([])
    })

    try:
        with open(path, "w", encoding="utf-8") as f:
            for k, v in settings.items():
                f.write(f"{k}:{v}\n")
    except Exception:
        pass

def check_server(ip, port, timeout=3):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(timeout)
        result = s.connect_ex((ip, int(port)))
        s.close()
        return result == 0
    except Exception:
        return False

def load_mods_cfg():
    if not os.path.exists(MODS_CFG): return {}
    try:
        with open(MODS_CFG, "r", encoding="utf-8") as f: return json.load(f)
    except Exception: return {}

def save_mods_cfg(c):
    with open(MODS_CFG, "w", encoding="utf-8") as f: json.dump(c, f, indent=2, ensure_ascii=False)

def get_modrinth_download(slug, mc_version, loader="fabric"):
    """Modrinth'ten en güncel uyumlu versiyonun indirme URL ve dosya adını döner."""
    try:
        import urllib.request, urllib.parse
        params = urllib.parse.urlencode({
            "game_versions": f'["{mc_version}"]',
            "loaders": f'["{loader}"]'
        })
        url = f"https://api.modrinth.com/v2/project/{slug}/version?{params}"
        req = urllib.request.Request(url, headers={"User-Agent": "KunefeSMP-Launcher/4.5"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            versions = json.loads(resp.read().decode())
            if not versions:
                return None, None
            for f in versions[0].get("files", []):
                if f.get("primary", True):
                    return f["url"], f["filename"]
            return None, None
    except Exception as e:
        print(f"[Modrinth] {slug} ({mc_version}) için indirme URL alınamadı: {e}")
        return None, None

def install_mod(slug, mc_version, progress_cb=None):
    """Bir modu Modrinth'ten indirip MODS_DIR'e kurar. Zaten varsa tekrar indirmez."""
    os.makedirs(MODS_DIR, exist_ok=True)
    url, filename = get_modrinth_download(slug, mc_version)
    if not url:
        return False, None
    dest = os.path.join(MODS_DIR, filename)
    if os.path.exists(dest):
        return True, filename
    try:
        import urllib.request
        if progress_cb:
            progress_cb(f"📦 {filename} indiriliyor...")
        req = urllib.request.Request(url, headers={"User-Agent": "KunefeSMP-Launcher/4.5"})
        with urllib.request.urlopen(req, timeout=60) as resp:
            with open(dest, "wb") as f:
                f.write(resp.read())
        return True, filename
    except Exception as e:
        print(f"[Modrinth] {slug} indirme hatası: {e}")
        return False, None

def clean_unmanaged_mods(allowed_filenames):
    """Sadece kendi listemizdeki modları bırakır, diğerlerini siler."""
    if not os.path.exists(MODS_DIR):
        return
    for file in os.listdir(MODS_DIR):
        if file.endswith(".jar") and file not in allowed_filenames:
            try:
                os.remove(os.path.join(MODS_DIR, file))
            except Exception:
                pass


# ══════════════════════════════════════════════════
#  ANA PENCERE
# ══════════════════════════════════════════════════

class KunefeLauncher(ctk.CTk):
    def __init__(self):
        super().__init__()
        ensure_dirs()
        lock_servers_dat()
        setup_kunefe_resource_pack()
        self.cfg = load_cfg()
        self.profiles = load_profiles()
        self.current_user = None
        self.title("KunefeSMP Client Launcher")
        base_dir = getattr(sys, '_MEIPASS', os.path.dirname(os.path.abspath(__file__)))
        ico_file = os.path.join(base_dir, "kunefe.ico")
        if os.path.exists(ico_file):
            try:
                self.iconbitmap(ico_file)
            except Exception:
                pass
        self.geometry("960x600")
        self.resizable(False, False)
        self.configure(fg_color=BG)
        self.container = ctk.CTkFrame(self, fg_color=BG)
        self.container.pack(fill="both", expand=True)
        self.mc_proc = None  # Çalışan Minecraft process'i burada tutulur
        self.protocol("WM_DELETE_WINDOW", self._on_close)

        # Beni Hatırla kontrolü:
        last_u = self.cfg.get("last_user", "")
        if self.cfg.get("remember_me") and last_u:
            self.current_user = last_u
            self._show_main()
        else:
            self._show_login()
    def _clear(self):
        for w in self.container.winfo_children(): w.destroy()

    def _input(self, parent, ph, show=None, width=330):
        return ctk.CTkEntry(parent, placeholder_text=ph,
                            font=ctk.CTkFont("Segoe UI", 13),
                            fg_color=INPUT, border_color=BORDER, border_width=2,
                            text_color=TEXT, corner_radius=12,
                            height=48, width=width, show=show)

    # ── GİRİŞ / KAYIT EKRANI ──────────────────────

    def _show_login(self):
        self._clear()

        # Sol Dekoratif Panel
        sol = ctk.CTkFrame(self.container, fg_color=SIDEBAR, width=340)
        sol.pack(side="left", fill="y")
        sol.pack_propagate(False)
        ctk.CTkFrame(sol, fg_color=ACCENT, height=4, corner_radius=0).pack(fill="x")

        logo = ctk.CTkFrame(sol, fg_color="transparent")
        logo.pack(expand=True)
        ctk.CTkLabel(logo, text="☁", font=ctk.CTkFont("Segoe UI", 80),
                     text_color=ACCENT).pack(pady=(0, 5))
        ctk.CTkLabel(logo, text="KunefeSMP",
                     font=ctk.CTkFont("Segoe UI", 28, "bold"), text_color=TEXT).pack()
        ctk.CTkLabel(logo, text="ÖZEL KİLİTLİ CLIENT",
                     font=ctk.CTkFont("Segoe UI", 11, "bold"), text_color=ACCENT).pack(pady=(2, 0))
        ctk.CTkFrame(logo, fg_color=BORDER, height=1, width=200).pack(pady=20)
        ctk.CTkLabel(logo, text="🔒  Kilitli Sunucu",
                     font=ctk.CTkFont("Segoe UI", 11), text_color=DIM).pack()
        ctk.CTkLabel(logo, text=SERVER_IP,
                     font=ctk.CTkFont("Consolas", 12, "bold"), text_color=GOLD).pack(pady=(4, 0))

        ctk.CTkLabel(sol, text="v4.5 Dynamic UI Edition  ·  KunefeSMP",
                     font=ctk.CTkFont("Segoe UI", 10), text_color=DIM).pack(side="bottom", pady=20)

        # Sağ Giriş Alanı
        sag = ctk.CTkFrame(self.container, fg_color=PANEL)
        sag.pack(side="right", fill="both", expand=True)
        ctk.CTkFrame(sag, fg_color=CARD, height=4, corner_radius=0).pack(fill="x")

        form = ctk.CTkFrame(sag, fg_color="transparent")
        form.place(relx=0.5, rely=0.5, anchor="center")

        ctk.CTkLabel(form, text="Kunefe Hesabınla Giriş Yap",
                     font=ctk.CTkFont("Segoe UI", 22, "bold"), text_color=TEXT).pack(pady=(0, 4))
        ctk.CTkLabel(form, text="Orijinal Minecraft gerekmez (Korsan Uyumlu)",
                     font=ctk.CTkFont("Segoe UI", 11), text_color=DIM).pack(pady=(0, 20))

        tab = ctk.CTkFrame(form, fg_color=INPUT, corner_radius=14)
        tab.pack(fill="x", pady=(0, 18))

        self._btn_giris = ctk.CTkButton(tab, text="Giriş Yap",
                                         font=ctk.CTkFont("Segoe UI", 13, "bold"),
                                         fg_color=ACCENT, hover_color="#cc4d00",
                                         corner_radius=12, height=40, width=155,
                                         command=lambda: self._tab("giris"))
        self._btn_giris.pack(side="left", padx=4, pady=4)

        self._btn_kayit = ctk.CTkButton(tab, text="Kayıt Ol",
                                         font=ctk.CTkFont("Segoe UI", 13),
                                         fg_color="transparent", hover_color=CARD,
                                         corner_radius=12, height=40, width=155,
                                         command=lambda: self._tab("kayit"))
        self._btn_kayit.pack(side="right", padx=4, pady=4)

        self.form_box = ctk.CTkFrame(form, fg_color="transparent", width=330)
        self.form_box.pack()

        self.hata = ctk.CTkLabel(form, text="",
                                  font=ctk.CTkFont("Segoe UI", 12), text_color=ERROR)
        self.hata.pack(pady=(10, 0))

        self._tab("giris")

    def _tab(self, tab):
        for w in self.form_box.winfo_children(): w.destroy()
        self.hata.configure(text="")
        if tab == "giris":
            self._btn_giris.configure(fg_color=ACCENT)
            self._btn_kayit.configure(fg_color="transparent")
            self._form_giris()
        else:
            self._btn_giris.configure(fg_color="transparent")
            self._btn_kayit.configure(fg_color=ACCENT)
            self._form_kayit()

    def _form_giris(self):
        self.e_user = self._input(self.form_box, "👤  Kullanıcı Adı (Oyun İçi İsim)")
        self.e_user.pack(pady=5)
        if self.cfg.get("last_user"):
            self.e_user.insert(0, self.cfg["last_user"])
        self.e_pw = self._input(self.form_box, "🔒  Şifre", show="●")
        self.e_pw.pack(pady=5)

        self.remember_var = ctk.BooleanVar(value=self.cfg.get("remember_me", True))
        rem_box = ctk.CTkFrame(self.form_box, fg_color="transparent")
        rem_box.pack(fill="x", padx=4, pady=(2, 6))
        ctk.CTkCheckBox(rem_box, text="Beni Hatırla", variable=self.remember_var,
                        font=ctk.CTkFont("Segoe UI", 12), text_color=TEXT,
                        fg_color=ACCENT, hover_color="#cc4d00",
                        checkmark_color=TEXT).pack(side="left")

        ctk.CTkButton(self.form_box, text="Giriş Yap  ➤",
                       font=ctk.CTkFont("Segoe UI", 14, "bold"),
                       fg_color=ACCENT, hover_color="#cc4d00",
                       corner_radius=12, height=50, width=330,
                       command=self._giris).pack(pady=(10, 0))

    def _form_kayit(self):
        self.e_user2 = self._input(self.form_box, "👤  İstediğin Kullanıcı Adı")
        self.e_user2.pack(pady=5)
        self.e_pw2   = self._input(self.form_box, "🔒  Şifre", show="●")
        self.e_pw2.pack(pady=5)
        self.e_pw2re = self._input(self.form_box, "🔒  Şifre Tekrar", show="●")
        self.e_pw2re.pack(pady=5)
        ctk.CTkButton(self.form_box, text="Hesap Oluştur  ➤",
                       font=ctk.CTkFont("Segoe UI", 14, "bold"),
                       fg_color=ACCENT, hover_color="#cc4d00",
                       corner_radius=12, height=50, width=330,
                       command=self._kayit).pack(pady=(15, 0))

    def _api_request(self, endpoint, payload):
        try:
            import urllib.request, urllib.error
            url = f"http://{SERVER_IP}:{AUTH_PORT}{endpoint}"
            data = json.dumps(payload).encode("utf-8")
            req = urllib.request.Request(url, data=data, method="POST")
            req.add_header("X-Launcher-Secret", LAUNCHER_SECRET)
            req.add_header("Content-Type", "application/json; charset=utf-8")
            with urllib.request.urlopen(req, timeout=5) as response:
                res_body = response.read().decode("utf-8")
                return response.status, json.loads(res_body)
        except urllib.error.HTTPError as e:
            try:
                err_body = e.read().decode("utf-8")
                return e.code, json.loads(err_body)
            except Exception:
                return e.code, {"success": False, "error": f"Sunucu hatası ({e.code})"}
        except Exception:
            return 0, {"success": False, "error": "Sunucuya bağlanılamadı!"}

    def _giris(self):
        u, p = self.e_user.get().strip(), self.e_pw.get().strip()
        if not u or not p:
            self.hata.configure(text="❌ Tüm alanları doldurun!", text_color=ERROR)
            return

        self.hata.configure(text="⏳ Giriş doğrulanıyor...", text_color=DIM)
        self.update_idletasks()

        # 1. Sunucudan doğrudan doğrula
        code, res = self._api_request("/kunefe-login", {"username": u, "password": p})

        if code == 200 and res.get("success"):
            server_uuid = res.get("uuid", str(uuid_lib.uuid4()))
            users = load_users()
            users[u] = {"password": hash_pw(p), "uuid": server_uuid}
            save_users(users)

            self.current_user = u
            c = load_cfg()
            c["last_user"] = u
            c["remember_me"] = self.remember_var.get()
            save_cfg(c)

            self.hata.configure(text="✅ Giriş başarılı!", text_color=SUCCESS)
            self.after(500, self._show_main)
            return

        elif code in (400, 401, 403, 404, 409):
            err_msg = res.get("error", "Giriş başarısız!")
            self.hata.configure(text=f"❌ {err_msg}", text_color=ERROR)
            return

        else:
            # Sunucuya erişilemezse yerel yedek kontrolü
            users = load_users()
            if u in users and users[u].get("password") == hash_pw(p):
                self.current_user = u
                c = load_cfg()
                c["last_user"] = u
                c["remember_me"] = self.remember_var.get()
                save_cfg(c)
                self.hata.configure(text="⚠️ Sunucu kapalı, çevrimdışı girildi.", text_color=GOLD)
                self.after(600, self._show_main)
            else:
                self.hata.configure(text="❌ Sunucuya bağlanılamadı!", text_color=ERROR)

    def _kayit(self):
        u = self.e_user2.get().strip()
        p = self.e_pw2.get().strip()
        p2 = self.e_pw2re.get().strip()
        if not u or not p or not p2:
            self.hata.configure(text="❌ Tüm alanları doldurun!", text_color=ERROR)
            return
        if len(u) < 3 or len(u) > 16:
            self.hata.configure(text="❌ Kullanıcı adı 3-16 karakter olmalıdır!", text_color=ERROR)
            return
        if len(p) < 6:
            self.hata.configure(text="❌ Şifre en az 6 karakter olmalıdır!", text_color=ERROR)
            return
        if p != p2:
            self.hata.configure(text="❌ Şifreler eşleşmiyor!", text_color=ERROR)
            return

        self.hata.configure(text="⏳ Sunucuda hesap oluşturuluyor...", text_color=DIM)
        self.update_idletasks()

        # Sunucuya kaydet
        code, res = self._api_request("/kunefe-register", {"username": u, "password": p})

        if code == 200 and res.get("success"):
            server_uuid = res.get("uuid", str(uuid_lib.uuid4()))
            users = load_users()
            users[u] = {"password": hash_pw(p), "uuid": server_uuid}
            save_users(users)

            c = load_cfg()
            c["last_user"] = u
            save_cfg(c)

            self.hata.configure(text="✅ Kayıt tamamlandı! Hesabın sunucuya bağlandı.", text_color=SUCCESS)
            self.after(1200, lambda: self._tab("giris"))
        else:
            err_msg = res.get("error", "Kayıt işlemi başarısız!")
            self.hata.configure(text=f"❌ {err_msg}", text_color=ERROR)

    def _send_launcher_auth(self, username):
        try:
            import urllib.request
            data = username.encode('utf-8')
            req = urllib.request.Request(
                f"http://{SERVER_IP}:{AUTH_PORT}/kunefe-auth",
                data=data,
                method='POST'
            )
            req.add_header('X-Launcher-Secret', LAUNCHER_SECRET)
            req.add_header('Content-Type', 'text/plain; charset=utf-8')
            urllib.request.urlopen(req, timeout=3)
        except Exception:
            pass  # Bağlantı hatası olursa sessizce geç

    # ── ANA OYUN EKRANI ───────────────────────────

    def _show_main(self):
        self._clear()

        # Sol Panel
        sb = ctk.CTkFrame(self.container, fg_color=SIDEBAR, width=230)
        sb.pack(side="left", fill="y")
        sb.pack_propagate(False)
        ctk.CTkFrame(sb, fg_color=ACCENT, height=3, corner_radius=0).pack(fill="x")

        ctk.CTkLabel(sb, text="☁  KunefeSMP",
                     font=ctk.CTkFont("Segoe UI", 16, "bold"), text_color=TEXT).pack(pady=(18, 2))
        ctk.CTkLabel(sb, text="KİLİTLİ CLIENT",
                     font=ctk.CTkFont("Segoe UI", 9, "bold"), text_color=ACCENT).pack()
        ctk.CTkFrame(sb, fg_color=BORDER, height=1).pack(fill="x", padx=15, pady=12)

        # Oyuncu Kutusu
        uc = ctk.CTkFrame(sb, fg_color=CARD, corner_radius=12)
        uc.pack(padx=12, pady=4, fill="x")
        ctk.CTkLabel(uc, text="👤", font=ctk.CTkFont("Segoe UI", 24)).pack(pady=(12, 2))
        ctk.CTkLabel(uc, text=self.current_user,
                     font=ctk.CTkFont("Segoe UI", 14, "bold"), text_color=TEXT).pack()
        ctk.CTkLabel(uc, text="Doğrulanmış Oyuncu",
                     font=ctk.CTkFont("Segoe UI", 10), text_color=SUCCESS).pack(pady=(2, 12))

        ctk.CTkFrame(sb, fg_color=BORDER, height=1).pack(fill="x", padx=15, pady=10)

        # Sunucu Durum Kutusu
        ctk.CTkLabel(sb, text="HEDEF SUNUCU",
                     font=ctk.CTkFont("Segoe UI", 9, "bold"), text_color=DIM).pack(anchor="w", padx=15)
        sc = ctk.CTkFrame(sb, fg_color=CARD, corner_radius=10)
        sc.pack(padx=12, pady=5, fill="x")
        self.status_dot = ctk.CTkLabel(sc, text="●",
                                        font=ctk.CTkFont("Segoe UI", 14), text_color=DIM)
        self.status_dot.pack(side="left", padx=(12, 5), pady=10)
        self.status_lbl = ctk.CTkLabel(sc, text="Kontrol ediliyor...",
                                        font=ctk.CTkFont("Segoe UI", 11), text_color=DIM)
        self.status_lbl.pack(side="left")

        # Sol Alt
        bot = ctk.CTkFrame(sb, fg_color="transparent")
        bot.pack(side="bottom", fill="x", padx=12, pady=15)
        ctk.CTkButton(bot, text="🧩  Modlar",
                       font=ctk.CTkFont("Segoe UI", 12),
                       fg_color=ACCENT, hover_color="#cc4d00",
                       corner_radius=10, height=38,
                       command=self._modlar).pack(fill="x", pady=3)
        ctk.CTkButton(bot, text="⚙  Ayarlar",
                       font=ctk.CTkFont("Segoe UI", 12),
                       fg_color=CARD, hover_color=BORDER,
                       corner_radius=10, height=38,
                       command=self._ayarlar).pack(fill="x", pady=3)
        ctk.CTkButton(bot, text="📁  Doku Klasörü",
                       font=ctk.CTkFont("Segoe UI", 12),
                       fg_color=CARD, hover_color=BORDER,
                       corner_radius=10, height=38,
                       command=lambda: open_folder(PACK_DIR)).pack(fill="x", pady=3)
        def _logout():
            c = load_cfg()
            c["remember_me"] = False
            save_cfg(c)
            self._show_login()

        ctk.CTkButton(bot, text="⏏  Çıkış Yap",
                       font=ctk.CTkFont("Segoe UI", 12),
                       fg_color="transparent", hover_color="#2a0505",
                       text_color=DIM, corner_radius=10, height=38,
                       command=_logout).pack(fill="x", pady=3)

        # Sağ İçerik
        main = ctk.CTkFrame(self.container, fg_color=PANEL)
        main.pack(side="right", fill="both", expand=True)
        ctk.CTkFrame(main, fg_color=ACCENT, height=3, corner_radius=0).pack(fill="x")

        content = ctk.CTkFrame(main, fg_color="transparent")
        content.pack(fill="both", expand=True, padx=25, pady=20)

        # Bilgi Bandı
        banner = ctk.CTkFrame(content, fg_color=CARD, corner_radius=16,
                               border_width=1, border_color=BORDER)
        banner.pack(fill="x", pady=(0, 15))
        ctk.CTkLabel(banner, text="🎮  KunefeSMP Özel Dünyası",
                     font=ctk.CTkFont("Segoe UI", 18, "bold"), text_color=TEXT).pack(side="left", padx=20, pady=15)
        ctk.CTkLabel(banner, text=f"🔒 {SERVER_IP}",
                     font=ctk.CTkFont("Consolas", 12, "bold"), text_color=GOLD).pack(side="right", padx=20)

        # Seçenek Kartları
        mid = ctk.CTkFrame(content, fg_color="transparent")
        mid.pack(fill="x", pady=(0, 15))

        ver_card = ctk.CTkFrame(mid, fg_color=CARD, corner_radius=16,
                                 border_width=1, border_color=BORDER)
        ver_card.pack(side="left", fill="both", expand=True, padx=(0, 8))
        ctk.CTkLabel(ver_card, text="🗂  Minecraft Sürümü",
                     font=ctk.CTkFont("Segoe UI", 12, "bold"), text_color=DIM).pack(anchor="w", padx=15, pady=(15, 8))
        surumler = get_versions_1_21_plus()
        saved_ver = self.cfg.get("version", "")
        default_ver = saved_ver if saved_ver in surumler else surumler[0]
        self.surum_var = ctk.StringVar(value=default_ver)

        def on_surum_change(v):
            c = load_cfg()
            c["version"] = v
            save_cfg(c)

        ctk.CTkOptionMenu(ver_card, values=surumler, variable=self.surum_var,
                           command=on_surum_change,
                           fg_color=INPUT, button_color=ACCENT, button_hover_color="#cc4d00",
                           font=ctk.CTkFont("Segoe UI", 13),
                           width=190, height=38, corner_radius=10).pack(padx=15, pady=(0, 15))

        fps_card = ctk.CTkFrame(mid, fg_color=CARD, corner_radius=16,
                                 border_width=1, border_color=BORDER)
        fps_card.pack(side="right", fill="both", expand=True, padx=(8, 0))
        ctk.CTkLabel(fps_card, text="⚡  FPS Modu & Optimizasyon",
                     font=ctk.CTkFont("Segoe UI", 12, "bold"), text_color=DIM).pack(anchor="w", padx=15, pady=(15, 8))
        self.fps_var = ctk.StringVar(value=self.cfg.get("fps_preset", "⚖️ Dengeli"))
        ctk.CTkOptionMenu(fps_card, values=list(FPS_PRESETS.keys()), variable=self.fps_var,
                           fg_color=INPUT, button_color=ACCENT, button_hover_color="#cc4d00",
                           font=ctk.CTkFont("Segoe UI", 13),
                           width=190, height=38, corner_radius=10).pack(padx=15, pady=(0, 15))

        # Alt Başlat Çubuğu
        bot_bar = ctk.CTkFrame(content, fg_color=CARD, corner_radius=16,
                                border_width=1, border_color=BORDER)
        bot_bar.pack(fill="x", side="bottom")

        row = ctk.CTkFrame(bot_bar, fg_color="transparent")
        row.pack(fill="x", padx=20, pady=18)

        left = ctk.CTkFrame(row, fg_color="transparent")
        left.pack(side="left", fill="both", expand=True, padx=(0, 20))

        self.prog_lbl = ctk.CTkLabel(left, text="KunefeSMP'ye bağlanmaya hazır! 🎮",
                                      font=ctk.CTkFont("Segoe UI", 12), text_color=DIM)
        self.prog_lbl.pack(anchor="w", pady=(0, 8))

        self.progress = ctk.CTkProgressBar(left, height=8,
                                            fg_color=INPUT, progress_color=ACCENT,
                                            corner_radius=4)
        self.progress.set(0)
        self.progress.pack(fill="x")

        right_col = ctk.CTkFrame(row, fg_color="transparent")
        right_col.pack(side="right")

        self.auto_login_var = ctk.BooleanVar(value=self.cfg.get("auto_login_ingame", False))
        self.auto_login_cb = ctk.CTkCheckBox(
            right_col,
            text="⚡ Oyunda Otomatik Giriş",
            variable=self.auto_login_var,
            command=self._on_auto_login_toggle,
            font=ctk.CTkFont("Segoe UI", 11),
            text_color=TEXT,
            fg_color=ACCENT,
            hover_color="#cc4d00",
            checkmark_color=TEXT,
            height=20
        )
        self.auto_login_cb.pack(anchor="e", pady=(0, 6))

        self.oyna_btn = ctk.CTkButton(right_col, text="▶  KUNEFESMP'YE GİR",
                                       font=ctk.CTkFont("Segoe UI", 15, "bold"),
                                       fg_color=ACCENT, hover_color="#cc4d00",
                                       corner_radius=14, height=52, width=220,
                                       command=self._oyna)
        self.oyna_btn.pack()

        threading.Thread(target=self._check_server, daemon=True).start()

    def _check_server(self):
        online = check_server(SERVER_IP, SERVER_PORT)
        if online:
            self.after(0, lambda: (
                self.status_dot.configure(text_color=SUCCESS),
                self.status_lbl.configure(text="Aktif / Girişe Açık", text_color=SUCCESS)
            ))
        else:
            self.after(0, lambda: (
                self.status_dot.configure(text_color=ERROR),
                self.status_lbl.configure(text="Sunucu Çevrimdışı", text_color=ERROR)
            ))

    def _on_auto_login_toggle(self):
        is_checked = self.auto_login_var.get()
        if is_checked:
            # Kutucuk henüz onaylanmadı, sözleşme kabul edilene kadar kapalı tut
            self.auto_login_var.set(False)
            self._show_auto_login_disclaimer()
        else:
            c = load_cfg()
            c["auto_login_ingame"] = False
            save_cfg(c)

    def _show_auto_login_disclaimer(self):
        pop = ctk.CTkToplevel(self)
        pop.title("🔒 Güvenlik ve Sorumluluk Sözleşmesi")
        pop.geometry("640x640")
        pop.resizable(False, False)
        pop.configure(fg_color=PANEL)
        pop.grab_set()

        # Üst dekoratif çizgi
        ctk.CTkFrame(pop, fg_color=ACCENT, height=3, corner_radius=0).pack(fill="x")

        # Başlık alanı
        ctk.CTkLabel(pop, text="🔒  Otomatik Giriş Güvenlik Sözleşmesi",
                     font=ctk.CTkFont("Segoe UI", 18, "bold"), text_color=TEXT).pack(pady=(16, 4))
        ctk.CTkLabel(pop, text="Lütfen aşağıdaki maddeleri dikkatle okuyunuz.",
                     font=ctk.CTkFont("Segoe UI", 12), text_color=DIM).pack(pady=(0, 12))

        # Metin Kartı
        card = ctk.CTkFrame(pop, fg_color=CARD, corner_radius=14, border_width=1, border_color=BORDER)
        card.pack(fill="both", expand=True, padx=22, pady=(0, 16))

        scroll = ctk.CTkScrollableFrame(card, fg_color="transparent")
        scroll.pack(fill="both", expand=True, padx=8, pady=8)

        sozlesme_metni = (
            "KULLANICI GÜVENLİK VE RIZA BEYANI\n\n"
            "Bu kutucuğu işaretlediğinizde, KunefeSMP Client sunucuya bağlanırken hesap şifrenizi "
            "oyun içerisinde (/giris) sormadan doğrudan oturum açmanızı sağlayan tek kullanımlık "
            "güvenli yetkilendirme sinyali gönderecektir.\n\n"
            "⚠️ DİKKAT VE RİSK BİLDİRİMİ:\n\n"
            "1. Cihaz Güvenliği ve Üçüncü Şahıs Erişimi:\n"
            "Bu bilgisayarı ailenizle, arkadaşlarınızla veya ortak bir ortamda (internet kafe, yurt vb.) "
            "paylaşıyorsanız; bilgisayarınızı açan veya yetkisiz erişim sağlayan herhangi bir kişi, oyun içi şifrenizi "
            "bilmesine gerek kalmadan KunefeSMP hesabınıza doğrudan giriş yapabilir.\n\n"
            "2. Hesap Çalınması ve Kötüye Kullanım İhtimali:\n"
            "Bu seçeneğin aktif edilmesi durumunda, cihazınızın yetkisiz kullanımı sonucu hesabınızın çalınması, "
            "isteğiniz dışında başkaları tarafından kullanılması, oyun içi eşyalarınızın, yapılarınızın veya "
            "karakterinizin kalıcı olarak zarar görmesi riski bulunmaktadır.\n\n"
            "3. Sorumluluk ve Kabul Beyanı:\n"
            "Bu sözleşmeyi onaylayarak; yerel cihazınızın güvenliğinden yalnızca kendinizin sorumlu olduğunu, "
            "hesabınızın başkaları tarafından kullanılmasından doğabilecek tüm maddi, manevi ve oyun içi kayıpların "
            "bütün hukuki ve fiili sorumluluğunun tamamen size ait olduğunu anlamış ve kabul etmiş sayılırsınız.\n\n"
            "4. Yönetimin Muafiyeti:\n"
            "KunefeSMP Yönetimi ve yazılım geliştiricileri, yerel güvenlik ihmallerinden kaynaklanan hesap "
            "veya eşya kayıplarından hiçbir surette sorumlu tutulamaz.\n\n"
            "Yukarıdaki tüm koşulları ve riskleri anladığınızı teyit ediyorsanız 'Kabul Ediyorum' butonuna basınız."
        )

        ctk.CTkLabel(
            scroll,
            text=sozlesme_metni,
            font=ctk.CTkFont("Segoe UI", 12),
            text_color=TEXT,
            justify="left",
            wraplength=480
        ).pack(fill="x", padx=(25, 15), pady=12)

        # Butonlar
        btn_box = ctk.CTkFrame(pop, fg_color="transparent")
        btn_box.pack(fill="x", padx=22, pady=(0, 20))

        def on_accept():
            self.auto_login_var.set(True)
            c = load_cfg()
            c["auto_login_ingame"] = True
            save_cfg(c)
            pop.destroy()

        def on_reject():
            self.auto_login_var.set(False)
            c = load_cfg()
            c["auto_login_ingame"] = False
            save_cfg(c)
            pop.destroy()

        pop.protocol("WM_DELETE_WINDOW", on_reject)

        ctk.CTkButton(
            btn_box,
            text="❌ Vazgeç / Reddet",
            font=ctk.CTkFont("Segoe UI", 13, "bold"),
            fg_color=CARD,
            hover_color=BORDER,
            text_color=TEXT,
            corner_radius=12,
            height=44,
            command=on_reject
        ).pack(side="left", fill="x", expand=True, padx=(0, 8))

        ctk.CTkButton(
            btn_box,
            text="✅ Kabul Ediyorum",
            font=ctk.CTkFont("Segoe UI", 13, "bold"),
            fg_color=ACCENT,
            hover_color="#cc4d00",
            corner_radius=12,
            height=44,
            command=on_accept
        ).pack(side="right", fill="x", expand=True, padx=(8, 0))

    # ── AYARLAR ───────────────────────────────────

    # ── MODLAR ────────────────────────────────────────

    def _modlar(self):
        pop = ctk.CTkToplevel(self)
        pop.title("🧩 Mod Yöneticisi")
        pop.geometry("560x640")
        pop.resizable(False, False)
        pop.configure(fg_color=PANEL)
        pop.grab_set()
        ctk.CTkFrame(pop, fg_color=ACCENT, height=3, corner_radius=0).pack(fill="x")
        ctk.CTkLabel(pop, text="🧩  Mod Yöneticisi",
                     font=ctk.CTkFont("Segoe UI", 18, "bold"), text_color=TEXT).pack(pady=(14, 2))
        ctk.CTkLabel(pop, text="Zorunlu modlar otomatik kurulur. İsteğe bağlıları seçin.",
                     font=ctk.CTkFont("Segoe UI", 11), text_color=DIM).pack(pady=(0, 10))

        mods_cfg = load_mods_cfg()
        selected_slugs = set(mods_cfg.get("selected", []))

        # Varsayılan seçimleri uygula
        for cat_mods in OPTIONAL_MODS.values():
            for m in cat_mods:
                if m["default"] and m["slug"] not in mods_cfg:
                    selected_slugs.add(m["slug"])

        scroll = ctk.CTkScrollableFrame(pop, fg_color="transparent", height=420)
        scroll.pack(fill="both", expand=True, padx=20, pady=(0, 10))

        # Zorunlu modlar bölümü
        req_frame = ctk.CTkFrame(scroll, fg_color=CARD, corner_radius=12)
        req_frame.pack(fill="x", pady=(0, 10))
        ctk.CTkLabel(req_frame, text="✅  Zorunlu Modlar (Otomatik Kurulur)",
                     font=ctk.CTkFont("Segoe UI", 12, "bold"), text_color=SUCCESS).pack(anchor="w", padx=14, pady=(10, 6))
        for m in REQUIRED_MODS:
            row = ctk.CTkFrame(req_frame, fg_color="transparent")
            row.pack(fill="x", padx=14, pady=1)
            ctk.CTkLabel(row, text="  🔒 " + m["name"],
                         font=ctk.CTkFont("Segoe UI", 12), text_color=DIM).pack(side="left")

        # İsteğe bağlı modlar
        check_vars = {}
        for cat, mods in OPTIONAL_MODS.items():
            cat_frame = ctk.CTkFrame(scroll, fg_color=CARD, corner_radius=12)
            cat_frame.pack(fill="x", pady=(6, 0))
            ctk.CTkLabel(cat_frame, text=cat,
                         font=ctk.CTkFont("Segoe UI", 12, "bold"), text_color=TEXT).pack(anchor="w", padx=14, pady=(10, 4))
            for m in mods:
                var = ctk.BooleanVar(value=m["slug"] in selected_slugs)
                check_vars[m["slug"]] = var
                row = ctk.CTkFrame(cat_frame, fg_color="transparent")
                row.pack(fill="x", padx=10, pady=2)
                ctk.CTkCheckBox(row, text=m["name"],
                                variable=var,
                                font=ctk.CTkFont("Segoe UI", 12),
                                text_color=TEXT,
                                fg_color=ACCENT, hover_color="#cc4d00",
                                checkmark_color=TEXT).pack(side="left")
                ctk.CTkLabel(row, text=m["desc"],
                             font=ctk.CTkFont("Segoe UI", 10), text_color=DIM).pack(side="left", padx=(8, 0))

        # Alt bar
        status_lbl = ctk.CTkLabel(pop, text="", font=ctk.CTkFont("Segoe UI", 11), text_color=SUCCESS)
        status_lbl.pack(pady=(0, 4))

        def kaydet():
            new_selected = [slug for slug, var in check_vars.items() if var.get()]
            mods_cfg["selected"] = new_selected
            save_mods_cfg(mods_cfg)
            status_lbl.configure(text="✅ Mod tercihlerin kaydedildi! Bir sonraki oyunda uygulanır.")

        ctk.CTkButton(pop, text="💾  Tercihleri Kaydet",
                      font=ctk.CTkFont("Segoe UI", 13, "bold"),
                      fg_color=ACCENT, hover_color="#cc4d00",
                      corner_radius=10, height=44,
                      command=kaydet).pack(fill="x", padx=20, pady=(0, 14))

    # ── AYARLAR ───────────────────────────────────
    def _ayarlar(self):
        pop = ctk.CTkToplevel(self)
        pop.title("Client Ayarları")
        pop.geometry("480x670")
        pop.resizable(False, False)
        pop.configure(fg_color=PANEL)
        pop.grab_set()
        ctk.CTkFrame(pop, fg_color=ACCENT, height=3, corner_radius=0).pack(fill="x")
        ctk.CTkLabel(pop, text="⚙  Client & Profil Ayarları",
                     font=ctk.CTkFont("Segoe UI", 18, "bold"), text_color=TEXT).pack(pady=(14, 8))

        cfg = load_cfg()
        profiles = load_profiles()
        profile_names = list(profiles.keys())
        active_prof = cfg.get("active_profile", profile_names[0])
        if active_prof not in profiles:
            active_prof = profile_names[0]

        def card_row(label, widget_fn):
            f = ctk.CTkFrame(pop, fg_color=CARD, corner_radius=12)
            f.pack(padx=20, fill="x", pady=3)
            ctk.CTkLabel(f, text=label, font=ctk.CTkFont("Segoe UI", 13),
                         text_color=TEXT).pack(side="left", padx=15, pady=8)
            widget_fn(f).pack(side="right", padx=15, pady=6)

        prof_v = ctk.StringVar(value=active_prof)
        ram_v = ctk.StringVar(value=str(profiles[active_prof].get("ram", cfg.get("ram", "2"))))
        fps_v = ctk.StringVar(value=profiles[active_prof].get("fps_preset", cfg.get("fps_preset", "⚖️ Dengeli")))
        
        skin_files = get_available_skins()
        skin_v = ctk.StringVar(value=profiles[active_prof].get("skin", cfg.get("skin", "default")))
        if skin_v.get() not in skin_files:
            skin_files.append(skin_v.get())

        rp_files = get_available_resource_packs()
        rp_v = ctk.StringVar(value=profiles[active_prof].get("resource_pack", cfg.get("resource_pack", "KunefeClient.zip")))
        if rp_v.get() not in rp_files:
            rp_files.append(rp_v.get())

        def on_profile_change(new_prof):
            pdata = profiles.get(new_prof, {})
            ram_v.set(str(pdata.get("ram", "2")))
            fps_v.set(pdata.get("fps_preset", "⚖️ Dengeli"))
            skin_v.set(pdata.get("skin", "default"))
            rp_v.set(pdata.get("resource_pack", "KunefeClient.zip"))

        card_row("👤  Profil Seçimi", lambda p: ctk.CTkOptionMenu(p,
            values=profile_names, variable=prof_v, command=on_profile_change,
            fg_color=INPUT, button_color=ACCENT, button_hover_color="#cc4d00",
            font=ctk.CTkFont("Segoe UI", 12), width=180, height=34, corner_radius=8))

        card_row("🖥  Verilecek RAM (GB)", lambda p: ctk.CTkOptionMenu(p,
            values=["1","2","3","4","6","8","12","16"], variable=ram_v,
            fg_color=INPUT, button_color=ACCENT, button_hover_color="#cc4d00",
            font=ctk.CTkFont("Segoe UI", 12), width=120, height=34, corner_radius=8))

        card_row("⚡  FPS Profili", lambda p: ctk.CTkOptionMenu(p,
            values=list(FPS_PRESETS.keys()), variable=fps_v,
            fg_color=INPUT, button_color=ACCENT, button_hover_color="#cc4d00",
            font=ctk.CTkFont("Segoe UI", 12), width=160, height=34, corner_radius=8))

        # İsimle Otomatik Skin İndirme Kutusu
        dl_f = ctk.CTkFrame(pop, fg_color=CARD, corner_radius=12)
        dl_f.pack(padx=20, fill="x", pady=3)
        ctk.CTkLabel(dl_f, text="🔍  İsimle Skin Çek", font=ctk.CTkFont("Segoe UI", 13), text_color=TEXT).pack(side="left", padx=15, pady=8)
        
        dl_box = ctk.CTkFrame(dl_f, fg_color="transparent")
        dl_box.pack(side="right", padx=15, pady=6)
        
        name_e = ctk.CTkEntry(dl_box, placeholder_text="Örn: Adal, Dream...", width=140, height=32,
                              font=ctk.CTkFont("Segoe UI", 11), fg_color=INPUT, border_color=BORDER, text_color=TEXT)
        name_e.pack(side="left", padx=(0, 6))

        dl_status = ctk.CTkLabel(pop, text="", font=ctk.CTkFont("Segoe UI", 11), text_color=SUCCESS)
        dl_status.pack(pady=0)

        skin_menu_holder = []
        def on_skin_menu_created(menu):
            skin_menu_holder.append(menu)
            return menu

        def dosya_sec_click():
            from tkinter import filedialog
            dosya = filedialog.askopenfilename(
                title="Minecraft Skin Seç (.png)",
                filetypes=[("PNG Skin Dosyası", "*.png"), ("Tüm Dosyalar", "*.*")]
            )
            if not dosya:
                return
            try:
                fname = os.path.basename(dosya)
                dest = os.path.join(SKINS_DIR, fname)
                shutil.copy2(dosya, dest)
                dl_status.configure(text=f"✅ {fname} yüklendi ve seçildi!", text_color=SUCCESS)
                all_s = get_available_skins()
                if fname not in all_s:
                    all_s.append(fname)
                if skin_menu_holder:
                    skin_menu_holder[0].configure(values=all_s)
                skin_v.set(fname)
                if self.current_user:
                    threading.Thread(target=upload_player_skin, args=(self.current_user, dest), daemon=True).start()
            except Exception as e:
                dl_status.configure(text=f"❌ Skin yüklenemedi: {e}", text_color=ERROR)

        card_row("📁  Dosyadan Skin", lambda p: ctk.CTkButton(p,
            text="Bilgisayardan Seç...",
            fg_color=INPUT, hover_color=BORDER, border_color=ACCENT, border_width=1,
            text_color=TEXT, font=ctk.CTkFont("Segoe UI", 12),
            width=180, height=34, corner_radius=8,
            command=dosya_sec_click))

        card_row("🎨  Seçili Skin", lambda p: on_skin_menu_created(ctk.CTkOptionMenu(p,
            values=skin_files, variable=skin_v,
            fg_color=INPUT, button_color=ACCENT, button_hover_color="#cc4d00",
            font=ctk.CTkFont("Segoe UI", 12), width=180, height=34, corner_radius=8)))

        def indir_click():
            isim = name_e.get().strip()
            if not isim:
                dl_status.configure(text="❌ Bir oyuncu adı yazın!", text_color=ERROR)
                return
            dl_status.configure(text=f"⏳ {isim} skini çekiliyor...", text_color=DIM)
            def _th():
                ok = download_skin_by_name(isim)
                if ok:
                    new_f = f"{isim}.png"
                    def _ui():
                        dl_status.configure(text=f"✅ {isim} skini indirildi ve seçildi!", text_color=SUCCESS)
                        all_s = get_available_skins()
                        if new_f not in all_s:
                            all_s.append(new_f)
                        if skin_menu_holder:
                            skin_menu_holder[0].configure(values=all_s)
                        skin_v.set(new_f)
                        if self.current_user:
                            s_path = os.path.join(SKINS_DIR, new_f)
                            threading.Thread(target=upload_player_skin, args=(self.current_user, s_path), daemon=True).start()
                    pop.after(0, _ui)
                else:
                    pop.after(0, lambda: dl_status.configure(text="❌ Skin bulunamadı! İsmi kontrol edin.", text_color=ERROR))
            threading.Thread(target=_th, daemon=True).start()

        ctk.CTkButton(dl_box, text="⬇ Çek", width=60, height=32,
                       font=ctk.CTkFont("Segoe UI", 11, "bold"),
                       fg_color=ACCENT, hover_color="#cc4d00",
                       command=indir_click).pack(side="left")

        card_row("📦  Resource Pack", lambda p: ctk.CTkOptionMenu(p,
            values=rp_files, variable=rp_v,
            fg_color=INPUT, button_color=ACCENT, button_hover_color="#cc4d00",
            font=ctk.CTkFont("Segoe UI", 12), width=180, height=34, corner_radius=8))

        # Hızlı Klasör Erişim Butonları
        btn_box = ctk.CTkFrame(pop, fg_color="transparent")
        btn_box.pack(fill="x", padx=20, pady=5)
        ctk.CTkButton(btn_box, text="📁 Skin Klasörü",
                       font=ctk.CTkFont("Segoe UI", 11),
                       fg_color=CARD, hover_color=BORDER, height=32, corner_radius=8,
                       command=lambda: open_folder(SKINS_DIR)).pack(side="left", expand=True, fill="x", padx=(0, 5))
        ctk.CTkButton(btn_box, text="📁 Doku Klasörü",
                       font=ctk.CTkFont("Segoe UI", 11),
                       fg_color=CARD, hover_color=BORDER, height=32, corner_radius=8,
                       command=lambda: open_folder(RP_DIR)).pack(side="right", expand=True, fill="x", padx=(5, 0))

        preset_info = {
            "⚡ Performans": "Render: 6  |  FPS: 120  |  Grafik: Hızlı",
            "⚖️ Dengeli":    "Render: 10  |  FPS: 60   |  Grafik: Süslü",
            "🔥 Ultra":      "Render: 16  |  FPS: 260  |  Grafik: Süslü",
        }
        info = ctk.CTkFrame(pop, fg_color=CARD, corner_radius=10)
        info.pack(padx=20, fill="x", pady=4)
        info_lbl = ctk.CTkLabel(info, text=preset_info.get(fps_v.get(), ""),
                                 font=ctk.CTkFont("Consolas", 11), text_color=DIM)
        info_lbl.pack(padx=12, pady=8)
        fps_v.trace_add("write", lambda *_: info_lbl.configure(text=preset_info.get(fps_v.get(), "")))

        def kaydet():
            cur_prof = prof_v.get()
            profiles[cur_prof] = {
                "ram": ram_v.get(),
                "fps_preset": fps_v.get(),
                "skin": skin_v.get(),
                "resource_pack": rp_v.get()
            }
            save_profiles(profiles)
            cfg["active_profile"] = cur_prof
            cfg["ram"] = ram_v.get()
            cfg["fps_preset"] = fps_v.get()
            cfg["skin"] = skin_v.get()
            cfg["resource_pack"] = rp_v.get()
            save_cfg(cfg)
            try: self.fps_var.set(fps_v.get())
            except Exception: pass
            if self.current_user:
                chosen_skin = skin_v.get()
                s_path = get_active_skin_path(self.current_user, chosen_skin)
                if s_path:
                    threading.Thread(target=upload_player_skin, args=(self.current_user, s_path), daemon=True).start()
            pop.destroy()

        ctk.CTkButton(pop, text="💾  Ayarları Kaydet",
                       font=ctk.CTkFont("Segoe UI", 13, "bold"),
                       fg_color=ACCENT, hover_color="#cc4d00",
                       corner_radius=12, height=44, width=180,
                       command=kaydet).pack(pady=12)

    # ── CRAFTRISE KORUMASI VE OYUN İZLEME ─────────

    def _guard_watcher(self, process):
        allowed_ips = get_allowed_server_ips()
        has_joined = False

        disconnect_strikes = 0

        while process.poll() is None:
            try:
                lock_servers_dat()

                proc = psutil.Process(process.pid)
                conns = proc.net_connections(kind="inet")

                connected_to_kunefe = False

                for c in conns:
                    if c.raddr:
                        ip = c.raddr.ip
                        port = c.raddr.port

                        # Yerel ağ, loopback ve web portlarına izin ver
                        if ip.startswith("127.") or ip == "::1" or ip.startswith("192.168.") or ip.startswith("10."):
                            continue
                        if port in (80, 443, 53):
                            continue

                        # Sadece Minecraft sunucu portlarını (25565 vb.) denetle
                        if port in (25565, 25566, 25567):
                            if ip not in allowed_ips:
                                print(f"[KunefeGuard] Yabancı Minecraft sunucusuna bağlantı engellendi: {ip}:{port}")
                                proc.kill()
                                break
                            elif c.status == psutil.CONN_ESTABLISHED:
                                connected_to_kunefe = True

                if connected_to_kunefe:
                    has_joined = True
                    disconnect_strikes = 0
                elif has_joined and not connected_to_kunefe:
                    disconnect_strikes += 1
                    if disconnect_strikes >= 1:
                        print("[KunefeGuard] KunefeSMP bağlantısı kesildi! Kullanıcıya soruluyor...")
                        should_close = self._ask_close_minecraft(process.pid)
                        if should_close:
                            print("[KunefeGuard] Kullanıcı 'Evet' dedi. Minecraft kapatılıyor...")
                            proc.kill()
                            self.mc_proc = None
                        else:
                            print("[KunefeGuard] Kullanıcı 'Hayır' dedi. Minecraft açık kalacak ve launcher kapansa da etkilenmeyecek.")
                            self.mc_proc = None  # Launcher kapatılsa bile MC açık kalmaya devam etsin!
                        break

            except (psutil.NoSuchProcess, psutil.AccessDenied):
                break
            except Exception:
                pass

            time.sleep(0.1)

        self.after(0, self._on_game_closed)

    def _ask_close_minecraft(self, pid=None):
        """Sunucudan çıkınca oyun F11'de bile olsa kesinlikle görünecek şekilde soru sorar."""
        try:
            if sys.platform == "win32":
                import ctypes
                from ctypes import wintypes

                # Minecraft'ın penceresini bul
                mc_hwnd = None
                if pid:
                    hwnds = []
                    def enum_cb(hwnd, extra):
                        if ctypes.windll.user32.IsWindowVisible(hwnd):
                            window_pid = wintypes.DWORD()
                            ctypes.windll.user32.GetWindowThreadProcessId(hwnd, ctypes.byref(window_pid))
                            if window_pid.value == pid:
                                hwnds.append(hwnd)
                        return True
                    WNDENUMPROC = ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.c_int, ctypes.c_int)
                    ctypes.windll.user32.EnumWindows(WNDENUMPROC(enum_cb), 0)
                    if hwnds:
                        mc_hwnd = hwnds[0]

                # F11 tam ekranda sorunun arkada kalmasını önlemek için:
                # Minecraft penceresini geçici olarak simge durumuna küçült (SW_MINIMIZE = 6)
                if mc_hwnd:
                    ctypes.windll.user32.ShowWindow(mc_hwnd, 6)

                # MB_YESNO (4) | MB_ICONQUESTION (32) | MB_SYSTEMMODAL (4096) | MB_SETFOREGROUND (65536) | MB_TOPMOST (262144)
                flags = 0x00000004 | 0x00000020 | 0x00001000 | 0x00010000 | 0x00040000
                res = ctypes.windll.user32.MessageBoxW(
                    0,
                    "Minecraft Kapatılsın mı?",
                    "KunefeSMP Client",
                    flags
                )
                is_yes = (res == 6)  # 6 = IDYES (Evet)

                # Eğer kullanıcı 'Hayır' derse, Minecraft penceresini tekrar geri aç (SW_RESTORE = 9)
                if not is_yes and mc_hwnd:
                    ctypes.windll.user32.ShowWindow(mc_hwnd, 9)
                    ctypes.windll.user32.SetForegroundWindow(mc_hwnd)

                return is_yes
            else:
                # Linux & macOS: Tkinter messagebox ve wmctrl
                try:
                    if sys.platform.startswith("linux") and pid:
                        subprocess.run(["wmctrl", "-i", "-r", f"0x{pid:x}", "-b", "add,hidden"],
                                       capture_output=True, timeout=2)
                except Exception:
                    pass
                import tkinter.messagebox as mb
                return mb.askyesno("KunefeSMP Client", "Minecraft Kapatılsın mı?", icon="question")
        except Exception as e:
            print(f"[KunefeGuard] Soru diyalogu hatası: {e}")
            return True

    def _on_close(self):
        """Launcher kapatılınca Minecraft da kapatılır."""
        if self.mc_proc is not None:
            try:
                self.mc_proc.kill()
            except Exception:
                pass
        self.destroy()

    def _on_game_closed(self):
        self.deiconify()
        self._set_prog(0, "Oynamaya hazır! 🎮")
        self.oyna_btn.configure(state="normal", text="▶  KUNEFESMP'YE GİR")

    # ── OYUNU BAŞLATMA (ÖZEL KİLİTLİ BAĞLANTI) ────

    def _oyna(self):
        self.oyna_btn.configure(state="disabled", text="⏳  Hazırlanıyor...")
        cfg = load_cfg()
        cfg["fps_preset"] = self.fps_var.get()
        cfg["version"] = self.surum_var.get()
        save_cfg(cfg)
        # Eğer otomatik giriş seçeneği aktifse pre-auth tokenı gönderilir, kapalıysa oyunda şifre istenir
        if cfg.get("auto_login_ingame", False):
            threading.Thread(target=self._send_launcher_auth, args=(self.current_user,), daemon=True).start()
        threading.Thread(target=self._mc_thread, daemon=True).start()

    def _set_prog(self, v, t=""):
        self.progress.set(v)
        self.prog_lbl.configure(text=t)

    def _mc_thread(self):
        try:
            cfg    = load_cfg()
            users  = load_users()
            u_uuid = users.get(self.current_user, {}).get("uuid", str(uuid_lib.uuid4()))
            latest = self.surum_var.get()
            preset = self.fps_var.get()

            # 1. Güvenlik, Sunucu Kilidi ve Sürüme Özel Resource Pack Hazırlığı
            self.after(0, lambda: self._set_prog(0.03, "🎨 Özel Kunefe butonları ve korumalar yükleniyor..."))
            lock_servers_dat()
            setup_kunefe_resource_pack(latest)

            # 2. Fabric Loader Kurulumu
            self.after(0, lambda: self._set_prog(0.08, "🔧 Fabric Loader kontrol ediliyor..."))
            try:
                fabric_versions = minecraft_launcher_lib.fabric.get_all_loader_versions()
                if fabric_versions:
                    fabric_loader = fabric_versions[0]["version"]
                    fabric_version_id = f"fabric-loader-{fabric_loader}-{latest}"
                    fabric_installed = any(
                        v.get("id") == fabric_version_id
                        for v in minecraft_launcher_lib.utils.get_installed_versions(MC_DIR)
                    )
                    if not fabric_installed:
                        self.after(0, lambda: self._set_prog(0.12, "🔧 Fabric Loader kuruluyor..."))
                        minecraft_launcher_lib.fabric.install_fabric(latest, MC_DIR)
                    launch_version = fabric_version_id
                else:
                    launch_version = latest
            except Exception as e:
                print(f"[Fabric] Kurulum hatası: {e}")
                launch_version = latest

            # 3. Modları Kur
            mods_cfg = load_mods_cfg()
            selected_optional = set(mods_cfg.get("selected", []))

            # Varsayılan optional seçimleri uygula (ilk kez)
            if "selected" not in mods_cfg:
                for cat_mods in OPTIONAL_MODS.values():
                    for m in cat_mods:
                        if m["default"]:
                            selected_optional.add(m["slug"])

            all_slugs = [m["slug"] for m in REQUIRED_MODS] + list(selected_optional)
            installed_files = set()
            total = len(all_slugs)

            for i, slug in enumerate(all_slugs):
                prog = 0.15 + (i / total) * 0.70
                mod_name = slug
                # İsim bul
                for m in REQUIRED_MODS:
                    if m["slug"] == slug: mod_name = m["name"]
                for cat_mods in OPTIONAL_MODS.values():
                    for m in cat_mods:
                        if m["slug"] == slug: mod_name = m["name"]
                self.after(0, lambda p=prog, n=mod_name: self._set_prog(p, f"📦 {n} kontrol ediliyor..."))
                ok, fname = install_mod(slug, latest)
                if ok and fname:
                    installed_files.add(fname)

            # Yönetilmeyen modları temizle
            clean_unmanaged_mods(installed_files)

            # 4. Skin Hazırlığı ve Sunucuya Gönderme (CustomSkinLoader Entegrasyonu)
            self.after(0, lambda: self._set_prog(0.88, "🎨 Karakter skini hazırlanıyor..."))

            # Eski entity texture pack override'larını temizle (böylece tüm oyuncular aynı skine bürünmez)
            entity_tex_dir = os.path.join(PACK_DIR, "assets", "minecraft", "textures", "entity")
            if os.path.exists(entity_tex_dir):
                try:
                    shutil.rmtree(entity_tex_dir, ignore_errors=True)
                except Exception:
                    pass

            skin_name = cfg.get("skin", "default")
            active_skin = get_active_skin_path(self.current_user, skin_name)

            # Eğer skin dosyası yoksa oyuncunun adına ait Mojang skini var mı diye bir kez çekmeyi dene
            if not active_skin and self.current_user:
                try:
                    if download_skin_by_name(self.current_user):
                        active_skin = os.path.join(SKINS_DIR, f"{self.current_user}.png")
                except Exception:
                    pass

            # Sunucuya yükle (tüm diğer oyuncuların görmesi için)
            if active_skin:
                try:
                    upload_player_skin(self.current_user, active_skin)
                except Exception:
                    pass

            # CustomSkinLoader yapılandırmasını kur
            setup_custom_skin_loader(self.current_user, active_skin)

            # 3. Yeni resource pack zip'ini (kunefe_client) yeniden oluştur
            with zipfile.ZipFile(ZIP_PACK, "w", zipfile.ZIP_DEFLATED) as zipf:
                for root, dirs, files in os.walk(PACK_DIR):
                    for file in files:
                        full_path = os.path.join(root, file)
                        rel_path = os.path.relpath(full_path, PACK_DIR)
                        zipf.write(full_path, rel_path)

            # 4. SHA‑1 hesapla ve config'e kaydet
            try:
                with open(ZIP_PACK, "rb") as f:
                    sha1_val = hashlib.sha1(f.read()).hexdigest()
                cfg["resource_pack_sha1"] = sha1_val
                save_cfg(cfg)
            except Exception:
                pass

            # 3. FPS ve Optimizasyon Ayarları + Texture Pack'i Otomatik Aktif Etme
            self.after(0, lambda: self._set_prog(0.92, "⚡ Kunefe ayarları & Resource Pack uygulanıyor..."))
            apply_options(preset)

            ram = int(cfg.get("ram", "2"))
            jvm_extra = FPS_PRESETS.get(preset, {}).get("jvm", [])

            # Kunefe Client Özelleştirmeleri
            options = {
                "username":     self.current_user,
                "uuid":         u_uuid,
                "token":        "0",
                "versionType":  CLIENT_BRAND,
                "jvmArguments": [
                    f"-Xmx{ram}G", 
                    f"-Xms{ram}G",
                    f"-Dminecraft.launcher.brand={CLIENT_BRAND}",
                    f"-Dminecraft.launcher.version=4.5"
                ] + jvm_extra,
                "server":       SERVER_IP,
                "port":         SERVER_PORT,
            }

            self.after(0, lambda: self._set_prog(0.97, "🚀 KunefeSMP Başlatılıyor..."))
            cmd = minecraft_launcher_lib.command.get_minecraft_command(launch_version, MC_DIR, options)

            # CMD (konsol) penceresini engelle: Windows'ta javaw.exe kullan, Linux/macOS'ta java yeterli
            if sys.platform == "win32":
                if cmd and cmd[0].lower().endswith("java.exe"):
                    javaw_path = cmd[0][:-8] + "javaw.exe"
                    if os.path.exists(javaw_path):
                        cmd[0] = javaw_path
                    else:
                        cmd[0] = "javaw"
                elif cmd and cmd[0].lower() == "java":
                    cmd[0] = "javaw"

            # Doğrudan KunefeSMP'ye bağlama komutları
            if "--server" not in cmd:
                cmd += ["--server", SERVER_IP, "--port", SERVER_PORT]
            if "--quickPlayMultiplayer" not in cmd:
                cmd += ["--quickPlayMultiplayer", f"{SERVER_IP}:{SERVER_PORT}"]

            # Oyunu başlat (Windows'ta konsol/CMD penceresinin açılmasını tamamen engelle)
            creation_flags = subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0
            process = subprocess.Popen(cmd, creationflags=creation_flags)
            self.mc_proc = process  # Launcher kapanınca MC'yi de kapatmak için tut

            # CraftRise koruma bekçisini başlat
            threading.Thread(target=self._guard_watcher, args=(process,), daemon=True).start()

            self.after(0, lambda: self._set_prog(1.0, "✅ KunefeSMP Başarıyla Açıldı!"))
            self.after(2000, lambda: self.iconify())

        except Exception as e:
            self.after(0, lambda: self._set_prog(0, f"❌ Hata oluştu: {e}"))
            self.after(0, lambda: self.oyna_btn.configure(state="normal", text="▶  KUNEFESMP'YE GİR"))


if __name__ == "__main__":
    try:
        app = KunefeLauncher()
        app.mainloop()
    except KeyboardInterrupt:
        pass