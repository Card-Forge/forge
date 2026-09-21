#!/usr/bin/env python3
"""
Forge MTG Card Animation Generator
----------------------------------
Automates the extraction, scaling, compositing, and deployment of animated card art loops
for both Forge Desktop and Forge Mobile/Adventure front-ends.

Usage:
  python generate_card_animation.py <SET> <CARD_NAME> <VIDEO_PATH>
  python generate_card_animation.py --set AFR --card "Improvised Weaponry" --video "clip.mp4"

Examples:
  python generate_card_animation.py AFR "Improvised Weaponry" "minimax-h3_animate-this-scene.mp4"
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


def find_local_card_image(set_code: str, card_name: str) -> str | None:
    """Search Forge local cache for an existing base card image."""
    set_clean = set_code.strip()
    name_clean = card_name.strip()

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

            candidates = [
                os.path.join(sdir, f"{name_clean}.fullborder.jpg"),
                os.path.join(sdir, f"{name_clean}.fullborder.png"),
                os.path.join(sdir, f"{name_clean}.jpg"),
                os.path.join(sdir, f"{name_clean}.png"),
            ]
            for c in candidates:
                if os.path.isfile(c):
                    return c

            # Try case-insensitive matching in folder
            for fname in os.listdir(sdir):
                f_lower = fname.lower()
                n_lower = name_clean.lower()
                if f_lower.startswith(n_lower) and (f_lower.endswith(".jpg") or f_lower.endswith(".png")):
                    return os.path.join(sdir, fname)

    return None


def download_card_image_from_scryfall(set_code: str, card_name: str, save_path: str) -> bool:
    """Fetch high-quality card scan from Scryfall API."""
    print(f"[*] Querying Scryfall API for '{card_name}' (set: {set_code})...")
    url = "https://api.scryfall.com/cards/named"
    params = {"exact": card_name, "set": set_code.lower()}
    headers = {"User-Agent": "ForgeMTG-AnimationGenerator/1.0"}

    try:
        resp = requests.get(url, params=params, headers=headers, timeout=15)
        if resp.status_code != 200:
            # Try fuzzy search as fallback
            params = {"fuzzy": card_name, "set": set_code.lower()}
            resp = requests.get(url, params=params, headers=headers, timeout=15)

        if resp.status_code != 200:
            print(f"[!] Scryfall API returned status {resp.status_code}: {resp.text}")
            return False

        data = resp.json()
        image_uris = data.get("image_uris")
        if not image_uris:
            # Could be a double-faced card; take front face
            card_faces = data.get("card_faces", [])
            if card_faces and "image_uris" in card_faces[0]:
                image_uris = card_faces[0]["image_uris"]

        if not image_uris:
            print(f"[!] No image URIs found in Scryfall response for '{card_name}'.")
            return False

        # Prefer 'large' or 'normal'
        img_url = image_uris.get("large") or image_uris.get("normal") or image_uris.get("png")
        if not img_url:
            print(f"[!] No suitable image URL found in image_uris.")
            return False

        print(f"[*] Downloading card image from Scryfall: {img_url}")
        img_resp = requests.get(img_url, headers=headers, timeout=30)
        if img_resp.status_code == 200:
            os.makedirs(os.path.dirname(os.path.abspath(save_path)), exist_ok=True)
            with open(save_path, "wb") as f:
                f.write(img_resp.content)
            print(f"[+] Downloaded and cached card image to: {save_path}")
            return True
        else:
            print(f"[!] Failed to download image from Scryfall, status: {img_resp.status_code}")
            return False

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


def process_card_animation(
    set_code: str,
    card_name: str,
    video_path: str,
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

    # 1. Locate or download card base template
    template_path = None
    if custom_template and os.path.isfile(custom_template):
        template_path = os.path.abspath(custom_template)
        print(f"[+] Using custom card template: {template_path}")
    else:
        template_path = find_local_card_image(set_code, card_name)
        if template_path:
            print(f"[+] Found local card template: {template_path}")
        else:
            # Download from Scryfall into user cache
            cache_target = os.path.expandvars(
                rf"%LOCALAPPDATA%\Forge\Cache\pics\cards\{set_code.upper()}\{card_name}.fullborder.jpg"
            )
            if download_card_image_from_scryfall(set_code, card_name, cache_target):
                template_path = cache_target
            else:
                sys.exit(f"[ERROR] Could not find or download card template for '{card_name}' ({set_code}).")

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

        print(f"\n[SUCCESS] Successfully generated and deployed animation for '{card_name}' ({len(raw_frames)} frames @ {fps} FPS)!")
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

    # Allow positional arguments or named flags
    parser.add_argument("pos_set", nargs="?", help="Card set code (e.g. AFR, MH2, FDN)")
    parser.add_argument("pos_card", nargs="?", help="Card name (e.g. 'Improvised Weaponry')")
    parser.add_argument("pos_video", nargs="?", help="Path to input 5-second MP4 video clip")

    parser.add_argument("-s", "--set", dest="flag_set", help="Card set code (e.g. AFR, MH2, FDN)")
    parser.add_argument("-c", "--card", dest="flag_card", help="Card name (e.g. 'Improvised Weaponry')")
    parser.add_argument("-v", "--video", dest="flag_video", help="Path to input MP4 video clip")

    parser.add_argument("--fps", type=int, default=24, help="Target animation FPS (default: 24)")
    parser.add_argument("--quality", type=int, default=88, help="JPEG quality 1-100 (default: 88)")
    parser.add_argument("--template", help="Path to custom card template image (overrides auto-detection)")

    parser.add_argument("--x", type=int, default=DEFAULT_ART_X, help=f"Art window X position (default: {DEFAULT_ART_X})")
    parser.add_argument("--y", type=int, default=DEFAULT_ART_Y, help=f"Art window Y position (default: {DEFAULT_ART_Y})")
    parser.add_argument("--width", type=int, default=DEFAULT_ART_W, help=f"Art window width (default: {DEFAULT_ART_W})")
    parser.add_argument("--height", type=int, default=DEFAULT_ART_H, help=f"Art window height (default: {DEFAULT_ART_H})")

    parser.add_argument("--keep-temp", action="store_true", help="Keep temporary frame directory for debugging")

    args = parser.parse_args()

    set_code = args.flag_set or args.pos_set
    card_name = args.flag_card or args.pos_card
    video_path = args.flag_video or args.pos_video

    if not set_code or not card_name or not video_path:
        parser.print_help()
        sys.exit("\n[ERROR] Missing required arguments: SET, CARD_NAME, and VIDEO_PATH are required.")

    process_card_animation(
        set_code=set_code,
        card_name=card_name,
        video_path=video_path,
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
