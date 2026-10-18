# config_manager.py – Kunefe Launcher profile handling
"""Utility module for loading, saving and listing user profiles.

Profiles are stored as JSON files under ``~/.kunefesmp/profiles/``. Each file is named
``<profile_name>.json`` and contains the same keys used by the launcher
(`ram`, `fps_preset`, `skin`, `resource_pack`, `resource_pack_sha1`, `jvm_extra`, ...).

The functions are deliberately lightweight – they perform no UI work and can be
imported from any part of the code base (e.g., the login screen or the settings
window).
"""
import os
import json
from typing import Dict, List

# Base directory for all Kunefe client data (already used elsewhere)
MC_DIR = os.path.join(os.path.expanduser("~"), ".kunefesmp")
PROFILES_DIR = os.path.join(MC_DIR, "profiles")


def _ensure_profiles_dir() -> None:
    """Make sure the profiles directory exists."""
    os.makedirs(PROFILES_DIR, exist_ok=True)


def list_profiles() -> List[str]:
    """Return a list of available profile names (without the .json suffix)."""
    _ensure_profiles_dir()
    files = [f for f in os.listdir(PROFILES_DIR) if f.endswith('.json')]
    return [os.path.splitext(f)[0] for f in files]


def load_profile(name: str) -> Dict:
    """Load a profile by *name*.

    If the profile does not exist, a default profile is created and returned.
    The returned dictionary can be safely modified and later saved with
    ``save_profile(name, data)``.
    """
    _ensure_profiles_dir()
    profile_path = os.path.join(PROFILES_DIR, f"{name}.json")
    if not os.path.exists(profile_path):
        # Default profile – mirrors the defaults used in ``load_cfg``
        default = {"ram": "2", "fps_preset": "⚖️ Dengeli"}
        save_profile(name, default)
        return default
    with open(profile_path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_profile(name: str, data: Dict) -> None:
    """Persist *data* under the given profile *name*.

    The function overwrites any existing file with the same name.
    """
    _ensure_profiles_dir()
    profile_path = os.path.join(PROFILES_DIR, f"{name}.json")
    with open(profile_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

# Convenience wrappers used by the existing launcher code (which still calls
# ``load_cfg``/``save_cfg``). Keeping them here maintains backward compatibility.
def load_cfg() -> Dict:
    """Load the *last used* profile.

    The ``config.json`` file stores the name of the last selected profile under the
    key ``last_profile``. If it is missing we fall back to a profile called
    ``default``.
    """
    cfg_path = os.path.join(MC_DIR, "config.json")
    if not os.path.exists(cfg_path):
        # Include defaults for new fields
        return {"ram": "2", "fps_preset": "⚖️ Dengeli", "last_profile": "default", "skin": "default", "resource_pack": "KunefeClient.zip", "resource_pack_sha1": ""}
    with open(cfg_path, "r", encoding="utf-8") as f:
        cfg = json.load(f)
    profile_name = cfg.get("last_profile", "default")
    return load_profile(profile_name)



def save_cfg(cfg: Dict) -> None:
    """Save the current configuration and remember the active profile.

    The caller should ensure ``cfg`` contains the key ``last_profile``. If it does
    not, we store ``default``.
    """
    cfg_path = os.path.join(MC_DIR, "config.json")
    # Ensure the ``last_profile`` key exists – this is the link between the
    # generic ``load_cfg``/``save_cfg`` helpers and the per‑profile storage.
    cfg.setdefault("last_profile", cfg.get("last_profile", "default"))
    with open(cfg_path, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)

# Backwards‑compatibility aliases (the original code imports these directly)
load_cfg = load_cfg
save_cfg = save_cfg
