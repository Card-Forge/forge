#!/usr/bin/env python3
"""
Forge MTG Card Animation Generator
----------------------------------
Automates the extraction, scaling, compositing, and deployment of animated card art loops
for both Forge Desktop and Forge Mobile/Adventure front-ends.

Usage:
  python generate_card_animation.py <SET> <CARD_NAME> <VIDEO_PATH> [<CARD_NUMBER>]
  python generate_card_animation.py <SET> <CARD_NAME> <CARD_NUMBER> <VIDEO_PATH>
  python generate_card_animation.py --set AFR --card "Improvised Weaponry" --video "clip.mp4"
  python generate_card_animation.py --set AFR --card "Acererak the Archlich" --number 372 --video "clip.mp4"

Examples:
  python generate_card_animation.py AFR "Improvised Weaponry" "minimax-h3_animate-this-scene.mp4"
  python generate_card_animation.py AFR "Acererak the Archlich" "clip.mp4" 372
  python generate_card_animation.py AFR "Acererak the Archlich" 372 "clip.mp4"
  python generate_card_animation.py FDN "Burst Lightning" "burst_lightning.mp4" --fps 24
"""

import argparse
import glob
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

try:
    from PIL import Image
    import requests
except ImportError as e:
    sys.exit(f"[ERROR] Missing required Python package: {e}. Please run: pip install Pillow requests")

# Standard MTG fullborder dimensions in Forge
STANDARD_CARD_WIDTH = 488
STANDARD_CARD_HEIGHT = 680

# Default modern card frame art window coordinates on a 488x680 card
DEFAULT_ART_X = 35
DEFAULT_ART_Y = 70
DEFAULT_ART_W = 420
DEFAULT_ART_H = 314

# Locations where animated cards are discovered by both Desktop and Mobile Forge
DEFAULT_TARGET_DIRS = [
    # Main project repository resource dir
    r"D:\projects\forge\res\animated_cards",
    # Runnable snapshot distribution dir
    r"D:\projects\forge\forge-installer\target\forge-installer-2.0.15-SNAPSHOT\res\animated_cards",
    # Local user cache pics dir
    os.path.expandvars(r"%LOCALAPPDATA%\Forge\Cache\pics\cards\animated_cards"),
    # Local user cache root
    os.path.expandvars(r"%LOCALAPPDATA%\Forge\Cache\animated_cards"),
]

# Local cache card picture search paths
CACHE_SEARCH_DIRS = [
    os.path.expandvars(r"%LOCALAPPDATA%\Forge\Cache\pics\cards"),
    r"D:\projects\forge\res\cardsfolder",
]

# Edition definitions search directories
EDITIONS_SEARCH_DIRS = [
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "res", "editions"),
    r"D:\projects\forge\res\editions",
]

# Edition sections that define cards with collector numbers
EDITION_SECTIONS = {
    "cards", "special slot", "precon product", "borderless", "etched", "showcase",
    "full art", "extended art", "alternate art", "retro frame", "buy a box", "promo",
    "prerelease promo", "bundle", "box topper", "jumpstart", "rebalanced", "eternal",
    "conjured", "scheme"
}


def resolve_art_info(set_code: str, card_name: str, card_number: str | None = None) -> tuple[int | None, int]:
    """
    Search Forge edition definitions for a card to map its collector number to its 1-based
    art index and find the total number of printings/arts in that edition.
    """
    set_code_upper = set_code.strip().upper()
    name_clean = card_name.strip().lower()
    matched_file = None

    for ed_dir in EDITIONS_SEARCH_DIRS:
        if not os.path.isdir(ed_dir):
            continue
        for f in glob.glob(os.path.join(ed_dir, "*.txt")):
            try:
                with open(f, "r", encoding="utf-8") as fp:
                    for line in fp:
                        line_s = line.strip()
                        if line_s.startswith("[metadata]"):
                            continue
                        if line_s.startswith("["):
                            break
                        if line_s.startswith("Code=") or line_s.startswith("ScryfallCode="):
                            val = line_s.split("=", 1)[1].strip().upper()
                            if val == set_code_upper:
                                matched_file = f
                                break
                if matched_file:
                    break
            except Exception:
                pass
        if matched_file:
            break

    if not matched_file:
        return None, 1

    entries = []
    try:
        with open(matched_file, "r", encoding="utf-8") as fp:
            current_section = None
            for line in fp:
                line_s = line.strip()
                if line_s.startswith("[") and line_s.endswith("]"):
                    current_section = line_s[1:-1].strip().lower()
                    continue
                if current_section in EDITION_SECTIONS and line_s and not line_s.startswith("#"):
                    parts = line_s.split()
                    if len(parts) >= 3:
                        c_num = parts[0]
                        rest = " ".join(parts[2:])
                        if "@" in rest:
                            c_name = rest.split("@")[0].strip()
                        else:
                            c_name = rest.strip()
                        if c_name.lower() == name_clean:
                            entries.append((c_num, c_name))
    except Exception as ex:
        print(f"[!] Warning reading edition file '{matched_file}': {ex}")

    total_arts = len(entries) if entries else 1
    if card_number:
        num_clean = str(card_number).strip().lower()
        for idx, (c_num, _) in enumerate(entries, 1):
            if c_num.lower() == num_clean:
                return idx, total_arts

    return None, total_arts


def find_local_card_image(
    set_code: str,
    card_name: str,
    card_number: str | None = None,
    art_index: int | None = None,
    art_count: int = 1,
) -> str | None:
    """Search Forge local cache for an existing base card image."""
    set_clean = set_code.strip()
    name_clean = card_name.strip()

    # Build prioritized candidate filename list
    candidates = []

    # 1. Art-indexed filenames if art_index is known and card has multiple arts
    if art_index is not None and (art_count > 1 or art_index > 1):
        candidates.extend([
            f"{name_clean}{art_index}.fullborder.jpg",
            f"{name_clean}{art_index}.fullborder.png",
            f"{name_clean}{art_index}.jpg",
            f"{name_clean}{art_index}.png",
        ])

    # 2. Collector number variations
    if card_number:
        num_clean = str(card_number).strip()
        candidates.extend([
            f"{name_clean}{num_clean}.fullborder.jpg",
            f"{name_clean}_{num_clean}.fullborder.jpg",
            f"{num_clean}_{name_clean}.fullborder.jpg",
            f"{name_clean}{num_clean}.jpg",
            f"{name_clean}_{num_clean}.jpg",
            f"{num_clean}_{name_clean}.jpg",
        ])

    # 3. Base un-indexed filenames
    candidates.extend([
        f"{name_clean}.fullborder.jpg",
        f"{name_clean}.fullborder.png",
        f"{name_clean}.jpg",
        f"{name_clean}.png",
    ])

    for base_dir in CACHE_SEARCH_DIRS:
        if not os.path.isdir(base_dir):
            continue

        # Look in set subfolder (e.g. %LOCALAPPDATA%\Forge\Cache\pics\cards\AFR)
        set_dirs = [
            os.path.join(base_dir, set_clean.upper()),
            os.path.join(base_dir, set_clean.lower()),
            os.path.join(base_dir, set_clean),
        ]

        for sdir in set_dirs:
            if not os.path.isdir(sdir):
                continue

            for c in candidates:
                cand_path = os.path.join(sdir, c)
                if os.path.isfile(cand_path):
                    return cand_path

            # If specific art index requested, look for file starting with name+art_index
            if art_index is not None and (art_count > 1 or art_index > 1):
                prefix_with_idx = f"{name_clean.lower()}{art_index}."
                for fname in os.listdir(sdir):
                    f_lower = fname.lower()
                    if f_lower.startswith(prefix_with_idx) and (f_lower.endswith(".jpg") or f_lower.endswith(".png")):
                        return os.path.join(sdir, fname)

            # If specific card number requested, look for collector number in filename
            if card_number:
                num_lower = str(card_number).strip().lower()
                n_lower = name_clean.lower()
                for fname in os.listdir(sdir):
                    f_lower = fname.lower()
                    if n_lower in f_lower and num_lower in f_lower and (f_lower.endswith(".jpg") or f_lower.endswith(".png")):
                        return os.path.join(sdir, fname)

            # If no art index / card number was specified or only 1 art exists, general name match
            if not card_number and art_count <= 1:
                n_lower = name_clean.lower()
                for fname in os.listdir(sdir):
                    f_lower = fname.lower()
                    if f_lower.startswith(n_lower) and (f_lower.endswith(".jpg") or f_lower.endswith(".png")):
                        return os.path.join(sdir, fname)

    return None


def _download_scryfall_image(data: dict, save_path: str, headers: dict, card_name: str) -> bool:
    """Helper to extract image URI and save card image from Scryfall card data object."""
    image_uris = data.get("image_uris")
    if not image_uris:
        card_faces = data.get("card_faces", [])
        if card_faces and "image_uris" in card_faces[0]:
            image_uris = card_faces[0]["image_uris"]

    if not image_uris:
        print(f"[!] No image URIs found in Scryfall response for '{card_name}'.")
        return False

    img_url = image_uris.get("large") or image_uris.get("normal") or image_uris.get("png")
    if not img_url:
        print("[!] No suitable image URL found in image_uris.")
        return False

    actual_name = data.get("name", card_name)
    actual_num = data.get("collector_number", "")
    print(f"[*] Downloading card scan from Scryfall ({actual_name} #{actual_num}): {img_url}")
    try:
        img_resp = requests.get(img_url, headers=headers, timeout=30)
        if img_resp.status_code == 200:
            os.makedirs(os.path.dirname(os.path.abspath(save_path)), exist_ok=True)
            with open(save_path, "wb") as f:
                f.write(img_resp.content)
            print(f"[+] Downloaded and cached card scan to: {save_path}")
            return True
        else:
            print(f"[!] Failed to download image from Scryfall, status: {img_resp.status_code}")
            return False
    except Exception as ex:
        print(f"[!] Error downloading image content: {ex}")
        return False


def download_card_image_from_scryfall(
    set_code: str,
    card_name: str,
    save_path: str,
    card_number: str | None = None,
) -> bool:
    """Fetch high-quality card scan from Scryfall API."""
    headers = {"User-Agent": "ForgeMTG-AnimationGenerator/1.0"}
    set_clean = set_code.strip().lower()

    if card_number:
        num_clean = str(card_number).strip().lower()
        print(f"[*] Querying Scryfall API for '{card_name}' #{card_number} (set: {set_code})...")
        # Direct lookup by set code and collector number: /cards/:code/:number
        url = f"https://api.scryfall.com/cards/{set_clean}/{num_clean}"
        try:
            resp = requests.get(url, headers=headers, timeout=15)
            if resp.status_code == 200:
                data = resp.json()
                return _download_scryfall_image(data, save_path, headers, card_name)
        except Exception as ex:
            print(f"[!] Error querying Scryfall by collector number: {ex}")

        # Fallback query using Scryfall search syntax
        search_url = "https://api.scryfall.com/cards/search"
        params = {"q": f's:{set_clean} cn:"{num_clean}"'}
        try:
            resp = requests.get(search_url, params=params, headers=headers, timeout=15)
            if resp.status_code == 200:
                data = resp.json()
                cards = data.get("data", [])
                if cards:
                    return _download_scryfall_image(cards[0], save_path, headers, card_name)
        except Exception:
            pass

    # Standard lookup by card name and set
    print(f"[*] Querying Scryfall API for '{card_name}' (set: {set_code})...")
    url = "https://api.scryfall.com/cards/named"
    params = {"exact": card_name, "set": set_clean}

    try:
        resp = requests.get(url, params=params, headers=headers, timeout=15)
        if resp.status_code != 200:
            # Try fuzzy search as fallback
            params = {"fuzzy": card_name, "set": set_clean}
            resp = requests.get(url, params=params, headers=headers, timeout=15)

        if resp.status_code != 200:
            print(f"[!] Scryfall API returned status {resp.status_code}: {resp.text}")
            return False

        data = resp.json()
        return _download_scryfall_image(data, save_path, headers, card_name)

    except Exception as ex:
        print(f"[!] Error fetching card from Scryfall: {ex}")
        return False


def fit_and_crop(frame_img: Image.Image, target_w: int, target_h: int) -> Image.Image:
    """Scale video frame to fill (target_w, target_h) preserving aspect ratio, then center-crop."""
    fw, fh = frame_img.size
    scale = max(target_w / fw, target_h / fh)
    new_w = int(round(fw * scale))
    new_h = int(round(fh * scale))

    resized = frame_img.resize((new_w, new_h), Image.Resampling.LANCZOS)
    left = max(0, (new_w - target_w) // 2)
    top = max(0, (new_h - target_h) // 2)
    return resized.crop((left, top, left + target_w, top + target_h))


def extract_video_frames(video_path: str, output_dir: str, fps: int = 24) -> list[str]:
    """Extract frames from MP4 video file using ffmpeg."""
    frame_pattern = os.path.join(output_dir, "raw_%04d.jpg")
    cmd = [
        "ffmpeg",
        "-y",
        "-i", video_path,
        "-vf", f"fps={fps}",
        "-q:v", "2",
        frame_pattern,
    ]
    print(f"[*] Extracting video frames at {fps} FPS with ffmpeg...")
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if res.returncode != 0:
        raise RuntimeError(f"ffmpeg frame extraction failed:\n{res.stderr}")

    extracted = sorted(glob.glob(os.path.join(output_dir, "raw_*.jpg")))
    print(f"[+] Extracted {len(extracted)} frames from video.")
    return extracted


def is_video_path(val: str | None) -> bool:
    """Check if a string argument represents a video file or path."""
    if not val:
        return False
    if os.path.isfile(val):
        return True
    lower = val.lower()
    video_exts = (".mp4", ".mkv", ".avi", ".mov", ".webm", ".flv", ".wmv", ".m4v", ".gif")
    if any(lower.endswith(ext) for ext in video_exts):
        return True
    if "/" in val or "\\" in val:
        return True
    return False


def process_card_animation(
    set_code: str,
    card_name: str,
    video_path: str,
    card_number: str | None = None,
    fps: int = 24,
    quality: int = 88,
    art_x: int = DEFAULT_ART_X,
    art_y: int = DEFAULT_ART_Y,
    art_w: int = DEFAULT_ART_W,
    art_h: int = DEFAULT_ART_H,
    custom_template: str | None = None,
    keep_temp: bool = False,
):
    video_path = os.path.abspath(video_path)
    if not os.path.isfile(video_path):
        sys.exit(f"[ERROR] Video file not found: {video_path}")

    # Resolve art index and total variants for this card in the set
    art_index, art_count = resolve_art_info(set_code, card_name, card_number)
    if card_number:
        if art_index:
            print(f"[+] Card: '{card_name}' #{card_number} (set: {set_code.upper()}) -> Art Variant {art_index}/{art_count}")
        else:
            print(f"[+] Card: '{card_name}' #{card_number} (set: {set_code.upper()})")
    else:
        print(f"[+] Card: '{card_name}' (set: {set_code.upper()})")

    # 1. Locate or download card base template
    template_path = None
    if custom_template and os.path.isfile(custom_template):
        template_path = os.path.abspath(custom_template)
        print(f"[+] Using custom card template: {template_path}")
    else:
        template_path = find_local_card_image(
            set_code=set_code,
            card_name=card_name,
            card_number=card_number,
            art_index=art_index,
            art_count=art_count,
        )
        if template_path:
            print(f"[+] Found local card template: {template_path}")
        else:
            # Download from Scryfall into user cache
            if art_index and (art_count > 1 or art_index > 1):
                cache_filename = f"{card_name}{art_index}.fullborder.jpg"
            else:
                cache_filename = f"{card_name}.fullborder.jpg"

            cache_target = os.path.expandvars(
                rf"%LOCALAPPDATA%\Forge\Cache\pics\cards\{set_code.upper()}\{cache_filename}"
            )
            if download_card_image_from_scryfall(set_code, card_name, cache_target, card_number=card_number):
                template_path = cache_target
                # If cached under an indexed name, also ensure a base unindexed copy exists as fallback
                unindexed_target = os.path.expandvars(
                    rf"%LOCALAPPDATA%\Forge\Cache\pics\cards\{set_code.upper()}\{card_name}.fullborder.jpg"
                )
                if not os.path.exists(unindexed_target):
                    try:
                        shutil.copy2(cache_target, unindexed_target)
                    except Exception:
                        pass
            else:
                num_msg = f" #{card_number}" if card_number else ""
                sys.exit(f"[ERROR] Could not find or download card template for '{card_name}'{num_msg} ({set_code}).")

    # Load and normalize base template to standard 488x680 full-border
    with Image.open(template_path) as orig_tmpl:
        orig_tmpl = orig_tmpl.convert("RGB")
        if orig_tmpl.size != (STANDARD_CARD_WIDTH, STANDARD_CARD_HEIGHT):
            print(f"[*] Resizing template from {orig_tmpl.size} to standard ({STANDARD_CARD_WIDTH}, {STANDARD_CARD_HEIGHT})...")
            card_template = orig_tmpl.resize((STANDARD_CARD_WIDTH, STANDARD_CARD_HEIGHT), Image.Resampling.LANCZOS)
        else:
            card_template = orig_tmpl.copy()

    # 2. Extract video frames to temporary directory
    temp_dir = tempfile.mkdtemp(prefix="forge_anim_")
    try:
        raw_frames = extract_video_frames(video_path, temp_dir, fps=fps)
        if not raw_frames:
            sys.exit(f"[ERROR] No frames were extracted from video: {video_path}")

        # 3. Composite each frame onto the card template
        print(f"[*] Compositing {len(raw_frames)} frames onto card frame (window: x={art_x}, y={art_y}, w={art_w}, h={art_h})...")
        composited_dir = os.path.join(temp_dir, "composited")
        os.makedirs(composited_dir, exist_ok=True)

        for idx, raw_frame_path in enumerate(raw_frames):
            with Image.open(raw_frame_path) as vf:
                vf = vf.convert("RGB")
                fitted_art = fit_and_crop(vf, art_w, art_h)

                comp = card_template.copy()
                comp.paste(fitted_art, (art_x, art_y))

                out_filename = f"frame_{idx:03d}.jpg"
                comp.save(os.path.join(composited_dir, out_filename), "JPEG", quality=quality, optimize=True)

            if (idx + 1) % 25 == 0 or (idx + 1) == len(raw_frames):
                print(f"    Composited {idx + 1}/{len(raw_frames)} frames...")

        # 4. Deploy to all active Forge destinations
        print("\n[*] Deploying animated frames to Forge directories:")
        target_subfolder = card_name.strip()
        deployed_count = 0

        for base_target in DEFAULT_TARGET_DIRS:
            dest_dir = os.path.join(base_target, target_subfolder)
            try:
                os.makedirs(dest_dir, exist_ok=True)
                for f in os.listdir(composited_dir):
                    shutil.copy2(os.path.join(composited_dir, f), os.path.join(dest_dir, f))
                print(f"  [OK] {dest_dir} ({len(raw_frames)} frames)")
                deployed_count += 1
            except Exception as ex:
                print(f"  [SKIP] Could not write to {dest_dir}: {ex}")

        num_msg = f" (collector #{card_number})" if card_number else ""
        print(f"\n[SUCCESS] Successfully generated and deployed animation for '{card_name}'{num_msg} ({len(raw_frames)} frames @ {fps} FPS)!")
        print(f"The animation is now live in:")
        print(f"  - Forge Desktop (Cards in Hand, Battlefield, Stack & Card Picture zoom)")
        print(f"  - Forge Mobile & Adventure (Classic Mode gameplay and zoom)")

    finally:
        if not keep_temp:
            shutil.rmtree(temp_dir, ignore_errors=True)
        else:
            print(f"[DEBUG] Kept temporary frames at: {temp_dir}")


def main():
    parser = argparse.ArgumentParser(
        description="Generate and deploy animated card art loops for Forge MTG (Desktop & Mobile/Adventure)."
    )

    # Allow positional arguments: supports both (<SET> <CARD> <VIDEO> [<NUM>]) and (<SET> <CARD> <NUM> <VIDEO>)
    parser.add_argument("pos_1", nargs="?", help="Card set code (e.g. AFR, MH2, FDN)")
    parser.add_argument("pos_2", nargs="?", help="Card name (e.g. 'Improvised Weaponry', 'Acererak the Archlich')")
    parser.add_argument("pos_3", nargs="?", help="Video path or card collector number")
    parser.add_argument("pos_4", nargs="?", help="Card collector number or video path")

    parser.add_argument("-s", "--set", dest="flag_set", help="Card set code (e.g. AFR, MH2, FDN)")
    parser.add_argument("-c", "--card", dest="flag_card", help="Card name (e.g. 'Improvised Weaponry')")
    parser.add_argument("-v", "--video", dest="flag_video", help="Path to input MP4 video clip")
    parser.add_argument("-n", "--number", "--num", "--card-number", dest="flag_number", help="Card collector number (e.g. 372, 87)")

    parser.add_argument("--fps", type=int, default=24, help="Target animation FPS (default: 24)")
    parser.add_argument("--quality", type=int, default=88, help="JPEG quality 1-100 (default: 88)")
    parser.add_argument("--template", help="Path to custom card template image (overrides auto-detection)")

    parser.add_argument("--x", type=int, default=DEFAULT_ART_X, help=f"Art window X position (default: {DEFAULT_ART_X})")
    parser.add_argument("--y", type=int, default=DEFAULT_ART_Y, help=f"Art window Y position (default: {DEFAULT_ART_Y})")
    parser.add_argument("--width", type=int, default=DEFAULT_ART_W, help=f"Art window width (default: {DEFAULT_ART_W})")
    parser.add_argument("--height", type=int, default=DEFAULT_ART_H, help=f"Art window height (default: {DEFAULT_ART_H})")

    parser.add_argument("--keep-temp", action="store_true", help="Keep temporary frame directory for debugging")

    args = parser.parse_args()

    set_code = args.flag_set or args.pos_1
    card_name = args.flag_card or args.pos_2
    video_path = args.flag_video
    card_number = args.flag_number

    pos_3 = args.pos_3
    pos_4 = args.pos_4

    if pos_3 and pos_4:
        # Both pos_3 and pos_4 provided
        if is_video_path(pos_3) and not is_video_path(pos_4):
            if not video_path:
                video_path = pos_3
            if not card_number:
                card_number = pos_4
        elif is_video_path(pos_4) and not is_video_path(pos_3):
            if not card_number:
                card_number = pos_3
            if not video_path:
                video_path = pos_4
        else:
            # Default positional order: pos_3 = video, pos_4 = number
            if not video_path:
                video_path = pos_3
            if not card_number:
                card_number = pos_4
    elif pos_3 and not pos_4:
        # Single extra positional argument
        if not video_path and not card_number:
            video_path = pos_3
        elif video_path and not card_number:
            card_number = pos_3
        elif card_number and not video_path:
            video_path = pos_3

    if not set_code or not card_name or not video_path:
        parser.print_help()
        sys.exit("\n[ERROR] Missing required arguments: SET, CARD_NAME, and VIDEO_PATH are required.")

    process_card_animation(
        set_code=set_code,
        card_name=card_name,
        video_path=video_path,
        card_number=card_number,
        fps=args.fps,
        quality=args.quality,
        art_x=args.x,
        art_y=args.y,
        art_w=args.width,
        art_h=args.height,
        custom_template=args.template,
        keep_temp=args.keep_temp,
    )


if __name__ == "__main__":
    main()
