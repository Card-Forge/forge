#!/usr/bin/env python3
"""Build, sign, and locally host a Forge Android APK for phone download.

The hosted page also exposes one-tap buttons to rebuild the desktop app and
sidecar, restart the sidecar, and import every format's MTGGoldfish meta decks
into a downloadable ZIP (organized as per-format deck folders).
"""

from __future__ import annotations

import argparse
import html
import http.server
import os
import shutil
import socket
import subprocess
import sys
import threading
import time
import urllib.parse
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
ANDROID_PROJECT = REPO_ROOT / "forge-gui-android"
TARGET_DIR = ANDROID_PROJECT / "target"
SERVE_DIR = TARGET_DIR / "apk-download"
LATEST_APK = "forge-android-latest.apk"
REBUILD_LOG = "rebuild.log"
DEFAULT_ANDROID_HOME = Path.home() / "Android" / "Sdk"
DEFAULT_BUILD_TOOLS = "35.0.0"

# Meta-deck import: scrape every tracked format into <format>/<slug>.dck, then
# zip the tree (folder structure preserved) into the serve dir so the phone can
# download it and unzip into Forge's decks folder.
DECKS_STAGE_DIR = TARGET_DIR / "meta-decks"
DECKS_ZIP = "forge-meta-decks.zip"
META_FORMATS = ("standard", "pioneer", "modern", "legacy", "vintage", "pauper", "commander")


class BuildState:
    def __init__(self) -> None:
        self.lock = threading.Lock()
        self.running = False
        self.status = "Idle"
        self.started_at: float | None = None
        self.finished_at: float | None = None
        self.error: str | None = None


STATE = BuildState()
ACTION_STATES: dict[str, BuildState] = {
    "apk": STATE,
    "desktop": BuildState(),
    "sidecar": BuildState(),
    "sidecar-restart": BuildState(),
    "import-decks": BuildState(),
}
ACTION_LABELS = {
    "apk": "Android APK",
    "desktop": "Desktop App",
    "sidecar": "Sidecar",
    "sidecar-restart": "Sidecar App",
    "import-decks": "Meta Decks",
}
ACTION_LOGS = {
    "apk": REBUILD_LOG,
    "desktop": "desktop-build.log",
    "sidecar": "sidecar-build.log",
    "sidecar-restart": "sidecar-restart.log",
    "import-decks": "import-decks.log",
}
ACTION_VERBS = {
    "apk": "Rebuild",
    "desktop": "Rebuild",
    "sidecar": "Rebuild",
    "sidecar-restart": "Restart",
    "import-decks": "Import",
}
ACTION_ORDER = ("apk", "desktop", "sidecar", "sidecar-restart", "import-decks")


def run(
    cmd: list[str],
    *,
    cwd: Path = REPO_ROOT,
    env: dict[str, str] | None = None,
    output=None,
) -> None:
    line = "+ " + " ".join(cmd)
    if output:
        output.write(line + "\n")
        output.flush()
    else:
        print(line, flush=True)
    subprocess.run(
        cmd,
        cwd=cwd,
        env=env,
        check=True,
        stdout=output if output else None,
        stderr=subprocess.STDOUT if output else None,
    )


def android_home_from_args(value: str | None) -> Path:
    if value:
        return Path(value).expanduser()
    if os.environ.get("ANDROID_HOME"):
        return Path(os.environ["ANDROID_HOME"]).expanduser()
    return DEFAULT_ANDROID_HOME


def build_tools_dir(android_home: Path, build_tools_version: str) -> Path:
    build_tools = android_home / "build-tools" / build_tools_version
    if not build_tools.exists():
        raise SystemExit(f"Android build tools not found: {build_tools}")
    return build_tools


def build_apk(android_home: Path, *, output=None) -> None:
    env = os.environ.copy()
    env.setdefault("ANDROID_HOME", str(android_home))
    env.setdefault("_JAVA_OPTIONS", "-Xmx2g")
    env["MAVEN_OPTS"] = " ".join(
        part
        for part in [
            env.get("MAVEN_OPTS", ""),
            "--add-exports=java.base/sun.security.pkcs=ALL-UNNAMED",
            "--add-exports=java.base/sun.security.x509=ALL-UNNAMED",
        ]
        if part
    )
    run(["mvn", "-pl", "forge-gui-android", "-am", "-P", "android-debug", "verify"], env=env, output=output)


def build_desktop(*, output=None) -> None:
    run(["mvn", "-pl", "forge-gui-desktop", "-am", "package"], output=output)


def build_sidecar(*, output=None) -> None:
    sidecar_dir = REPO_ROOT / "forge-llm-sidecar"
    venv_python = sidecar_dir / ".venv" / "bin" / "python"
    python = str(venv_python) if venv_python.exists() else sys.executable
    run([python, "-m", "pip", "install", "-e", "."], cwd=sidecar_dir, output=output)


def build_meta_decks(*, output=None) -> Path:
    """Scrape every tracked format's meta decks and zip them for phone download.

    Writes ``<stage>/<format>/<slug>.dck`` (the importer's native layout, which
    Forge shows as per-format deck folders), then archives the tree into the
    serve dir as ``forge-meta-decks.zip``. Best-effort per format: a format whose
    scrape fails (network blip, empty page) is logged and skipped, not fatal.
    """
    sidecar_dir = REPO_ROOT / "forge-llm-sidecar"
    venv_python = sidecar_dir / ".venv" / "bin" / "python"
    python = str(venv_python) if venv_python.exists() else sys.executable

    if DECKS_STAGE_DIR.exists():
        shutil.rmtree(DECKS_STAGE_DIR)
    DECKS_STAGE_DIR.mkdir(parents=True, exist_ok=True)

    imported = 0
    for fmt in META_FORMATS:
        cmd = [python, "-m", "scripts.import_goldfish_decks", fmt, "--out", str(DECKS_STAGE_DIR), "--force"]
        line = "+ " + " ".join(cmd)
        if output:
            output.write(line + "\n")
            output.flush()
        # check=False: the importer exits non-zero when a format yields no decks;
        # that shouldn't abort the other formats.
        result = subprocess.run(
            cmd,
            cwd=sidecar_dir,
            stdout=output if output else None,
            stderr=subprocess.STDOUT if output else None,
        )
        if result.returncode == 0:
            imported += 1
        elif output:
            output.write(f"WARN: import returned {result.returncode} for {fmt}; continuing\n")
            output.flush()

    dck_count = sum(1 for _ in DECKS_STAGE_DIR.rglob("*.dck"))
    if dck_count == 0:
        raise RuntimeError("No decks were imported for any format (network down or markup changed?)")

    SERVE_DIR.mkdir(parents=True, exist_ok=True)
    archive = shutil.make_archive(
        str(SERVE_DIR / "forge-meta-decks"), "zip", root_dir=DECKS_STAGE_DIR
    )
    if output:
        output.write(
            f"Zipped {dck_count} decks from {imported}/{len(META_FORMATS)} formats -> {archive}\n"
        )
        output.flush()
    return Path(archive)


def sidecar_uvicorn_command() -> list[str]:
    sidecar_dir = REPO_ROOT / "forge-llm-sidecar"
    venv_uvicorn = sidecar_dir / ".venv" / "bin" / "uvicorn"
    if venv_uvicorn.exists():
        return [str(venv_uvicorn), "app.main:app", "--host", "0.0.0.0", "--port", "18970"]
    venv_python = sidecar_dir / ".venv" / "bin" / "python"
    python = str(venv_python) if venv_python.exists() else sys.executable
    return [python, "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "18970"]


def sidecar_pids() -> list[int]:
    result = subprocess.run(["ps", "-eo", "pid=,args="], check=True, capture_output=True, text=True)
    pids: list[int] = []
    for line in result.stdout.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        pid_text, _, args = stripped.partition(" ")
        if (
            "uvicorn" in args
            and "app.main:app" in args
            and "--port 18970" in args
            and int(pid_text) != os.getpid()
        ):
            pids.append(int(pid_text))
    return pids


def restart_sidecar(*, output=None) -> None:
    sidecar_dir = REPO_ROOT / "forge-llm-sidecar"
    runtime_log = SERVE_DIR / "sidecar-runtime.log"
    pids = sidecar_pids()
    if pids:
        if output:
            output.write(f"Stopping sidecar pid(s): {', '.join(str(pid) for pid in pids)}\n")
            output.flush()
        for pid in pids:
            os.kill(pid, 15)
        deadline = time.time() + 10
        while time.time() < deadline:
            if not any(pid in sidecar_pids() for pid in pids):
                break
            time.sleep(0.25)
        remaining = [pid for pid in pids if pid in sidecar_pids()]
        for pid in remaining:
            if output:
                output.write(f"Force stopping sidecar pid: {pid}\n")
                output.flush()
            os.kill(pid, 9)

    cmd = sidecar_uvicorn_command()
    env = os.environ.copy()
    env.setdefault("HOST", "0.0.0.0")
    env.setdefault("PORT", "18970")
    env.setdefault("LLM_BASE_URL", "http://localhost:8080/v1")
    if output:
        output.write("+ " + " ".join(cmd) + f" > {runtime_log}\n")
        output.flush()
    runtime = runtime_log.open("ab")
    process = subprocess.Popen(
        cmd,
        cwd=sidecar_dir,
        env=env,
        stdout=runtime,
        stderr=subprocess.STDOUT,
        start_new_session=True,
    )
    time.sleep(1)
    if process.poll() is not None:
        raise RuntimeError(f"Sidecar exited immediately with code {process.returncode}; see {runtime_log}")
    if output:
        output.write(f"Started sidecar pid: {process.pid}\n")
        output.flush()


def newest_raw_apk() -> Path:
    candidates = [
        apk
        for apk in TARGET_DIR.glob("*.apk")
        if not any(
            suffix in apk.name
            for suffix in ("-aligned.apk", "-debug-signed.apk", "-signed.apk", "-signed-aligned.apk")
        )
    ]
    if not candidates:
        raise SystemExit(f"No raw APK found in {TARGET_DIR}; run with build enabled first.")
    return max(candidates, key=lambda p: p.stat().st_mtime)


def sign_apk(raw_apk: Path, build_tools: Path, *, output=None) -> Path:
    debug_keystore = Path.home() / ".android" / "debug.keystore"
    if not debug_keystore.exists():
        raise SystemExit(
            f"Debug keystore not found: {debug_keystore}\n"
            "Create it with Android Studio/SDK tooling, then rerun this script."
        )

    aligned = raw_apk.with_name(raw_apk.stem + "-aligned.apk")
    signed = raw_apk.with_name(raw_apk.stem + "-debug-signed.apk")
    zipalign = build_tools / "zipalign"
    apksigner = build_tools / "apksigner"
    run([str(zipalign), "-f", "-p", "4", str(raw_apk), str(aligned)], output=output)
    run(
        [
            str(apksigner),
            "sign",
            "--ks",
            str(debug_keystore),
            "--ks-key-alias",
            "androiddebugkey",
            "--ks-pass",
            "pass:android",
            "--key-pass",
            "pass:android",
            "--out",
            str(signed),
            str(aligned),
        ],
        output=output,
    )
    run([str(apksigner), "verify", "--verbose", str(signed)], output=output)
    return signed


def local_ipv4_addresses() -> list[str]:
    addresses: list[str] = []
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.connect(("8.8.8.8", 80))
            addresses.append(sock.getsockname()[0])
    except OSError:
        pass

    try:
        hostname = socket.gethostname()
        for info in socket.getaddrinfo(hostname, None, socket.AF_INET):
            addr = info[4][0]
            if not addr.startswith("127.") and addr not in addresses:
                addresses.append(addr)
    except OSError:
        pass
    return addresses or ["127.0.0.1"]


def tailscale_ipv4_addresses() -> list[str]:
    tailscale = shutil.which("tailscale")
    if not tailscale:
        return []
    try:
        result = subprocess.run(
            [tailscale, "ip", "-4"],
            check=True,
            capture_output=True,
            text=True,
        )
    except subprocess.CalledProcessError:
        return []
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def format_timestamp(value: float | None) -> str:
    if value is None:
        return "Never"
    return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(value))


def render_action_card(target: str) -> str:
    state = ACTION_STATES[target]
    label = ACTION_LABELS[target]
    log_name = ACTION_LOGS[target]
    verb = ACTION_VERBS[target]
    with state.lock:
        running = state.running
        status = state.status
        started_at = state.started_at
        finished_at = state.finished_at
        error = state.error
    button_label = f"{verb} {label}" if not running else f"{verb}ing {label}..."
    button_disabled = " disabled" if running else ""
    status_class = "error" if error else "ok"
    error_html = f"<p class=\"error\">{html.escape(error)}</p>" if error else ""
    return f"""
    <section>
      <h2>{html.escape(label)}</h2>
      <form method="post" action="/build?target={html.escape(target)}">
        <p><button type="submit"{button_disabled}>{html.escape(button_label)}</button></p>
      </form>
      <p class="{status_class}">{html.escape(status)}</p>
      <p>Started: {format_timestamp(started_at)}</p>
      <p>Finished: {format_timestamp(finished_at)}</p>
      {error_html}
      <p><a href="/{html.escape(log_name)}">View log</a></p>
    </section>
"""


def render_index(apk_path: Path, base_urls: list[str]) -> str:
    stat = apk_path.stat()
    links = "\n".join(
        f'<li><a href="{html.escape(url)}/{LATEST_APK}">{html.escape(url)}/{LATEST_APK}</a></li>'
        for url in base_urls
    )
    action_cards = "\n".join(render_action_card(target) for target in ACTION_ORDER)
    decks_zip = SERVE_DIR / DECKS_ZIP
    if decks_zip.exists():
        zip_stat = decks_zip.stat()
        decks_download = (
            f'<p><a class="button" href="/{DECKS_ZIP}">Download meta decks (ZIP)</a> '
            f"&middot; {zip_stat.st_size / 1024 / 1024:.1f} MB &middot; built "
            f'{time.strftime("%Y-%m-%d %H:%M", time.localtime(zip_stat.st_mtime))}</p>'
            "<p>Unzip into Forge's <code>decks/constructed</code> folder on your phone; "
            "each format becomes its own deck folder.</p>"
        )
    else:
        decks_download = (
            '<p>Use the <b>Import Meta Decks</b> button below to fetch every format’s '
            "meta decks, then a download link appears here.</p>"
        )
    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Forge Android APK</title>
  <style>
    body {{ font-family: system-ui, sans-serif; margin: 2rem; line-height: 1.45; }}
    main {{ max-width: 720px; }}
    a.button {{ display: inline-block; padding: 0.85rem 1rem; background: #1f6feb; color: white; text-decoration: none; border-radius: 6px; }}
    button {{ padding: 0.85rem 1rem; background: #1f883d; color: white; border: 0; border-radius: 6px; font: inherit; }}
    button:disabled {{ background: #8c959f; }}
    code {{ background: #f1f3f5; padding: 0.15rem 0.3rem; border-radius: 4px; }}
    .ok {{ color: #1f883d; }}
    .error {{ color: #cf222e; }}
  </style>
</head>
<body>
  <main>
    <h1>Forge Android APK</h1>
    <p><a class="button" href="/{LATEST_APK}">Download latest APK</a></p>
    <h2>Meta decks</h2>
    {decks_download}
    {action_cards}
    <p>Built: {time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(stat.st_mtime))}</p>
    <p>Size: {stat.st_size / 1024 / 1024:.1f} MB</p>
    <p>If Android blocks the install, allow installs from this browser and retry the download.</p>
    <h2>Direct URLs</h2>
    <ul>{links}</ul>
  </main>
</body>
</html>
"""


def write_index(apk_path: Path, base_urls: list[str]) -> None:
    (SERVE_DIR / "index.html").write_text(render_index(apk_path, base_urls), encoding="utf-8")


def publish(signed_apk: Path, port: int, host_url: str | None) -> list[str]:
    SERVE_DIR.mkdir(parents=True, exist_ok=True)
    hosted_apk = SERVE_DIR / LATEST_APK
    shutil.copy2(signed_apk, hosted_apk)
    if host_url:
        urls = [host_url.rstrip("/")]
    else:
        addresses = tailscale_ipv4_addresses() + local_ipv4_addresses()
        deduped = list(dict.fromkeys(addresses))
        urls = [f"http://{ip}:{port}" for ip in deduped]
    write_index(hosted_apk, urls)
    return urls


def run_target_build(target: str, server: "ApkServer") -> None:
    state = ACTION_STATES[target]
    log_path = SERVE_DIR / ACTION_LOGS[target]
    with state.lock:
        state.status = f"Building {ACTION_LABELS[target]}"
        state.started_at = time.time()
        state.finished_at = None
        state.error = None
    try:
        with log_path.open("w", encoding="utf-8") as log:
            log.write(f"Started: {format_timestamp(time.time())}\n")
            log.flush()
            if target == "apk":
                build_apk(server.android_home, output=log)
                with state.lock:
                    state.status = "Signing Android APK"
                raw_apk = newest_raw_apk()
                signed_apk = sign_apk(raw_apk, server.build_tools, output=log)
                with state.lock:
                    state.status = "Publishing Android APK"
                server.base_urls = publish(signed_apk, server.server_port, server.host_url)
            elif target == "desktop":
                build_desktop(output=log)
            elif target == "sidecar":
                build_sidecar(output=log)
            elif target == "sidecar-restart":
                restart_sidecar(output=log)
            elif target == "import-decks":
                build_meta_decks(output=log)
            else:
                raise ValueError(f"Unknown build target: {target}")
            log.write(f"Finished: {format_timestamp(time.time())}\n")
            log.flush()
        with state.lock:
            state.status = "Build complete"
            state.finished_at = time.time()
    except Exception as exc:  # noqa: BLE001
        with state.lock:
            state.status = "Build failed"
            state.error = str(exc)
            state.finished_at = time.time()
            error_text = state.error
        with log_path.open("a", encoding="utf-8") as log:
            log.write(f"\nFAILED: {error_text}\n")
    finally:
        with state.lock:
            state.running = False


class ApkRequestHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self) -> None:  # noqa: N802
        path = urllib.parse.urlparse(self.path).path
        if path in ("/", "/index.html"):
            apk_path = SERVE_DIR / LATEST_APK
            page = render_index(apk_path, self.server.base_urls).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(page)))
            self.end_headers()
            self.wfile.write(page)
            return
        super().do_GET()

    def do_POST(self) -> None:  # noqa: N802
        path = urllib.parse.urlparse(self.path).path
        query = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
        if path == "/rebuild":
            target = "apk"
        elif path == "/build":
            target = query.get("target", [""])[0]
        else:
            self.send_error(404)
            return
        if target not in ACTION_STATES:
            self.send_error(400, f"Unknown build target: {target}")
            return
        state = ACTION_STATES[target]
        with state.lock:
            if not state.running:
                state.running = True
                thread = threading.Thread(target=run_target_build, args=(target, self.server), daemon=True)
                state.status = f"Queued {ACTION_LABELS[target]} build"
                thread.start()
        self.send_response(303)
        self.send_header("Location", "/")
        self.end_headers()


class ApkServer(http.server.ThreadingHTTPServer):
    allow_reuse_address = True

    def __init__(
        self,
        server_address: tuple[str, int],
        handler_class: type[http.server.BaseHTTPRequestHandler],
        *,
        android_home: Path,
        build_tools: Path,
        host_url: str | None,
        base_urls: list[str],
    ) -> None:
        super().__init__(server_address, handler_class)
        self.android_home = android_home
        self.build_tools = build_tools
        self.host_url = host_url
        self.base_urls = base_urls


def serve(port: int, android_home: Path, build_tools: Path, host_url: str | None, base_urls: list[str]) -> None:
    os.chdir(SERVE_DIR)
    server = ApkServer(
        ("0.0.0.0", port),
        ApkRequestHandler,
        android_home=android_home,
        build_tools=build_tools,
        host_url=host_url,
        base_urls=base_urls,
    )
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped APK server.")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build, debug-sign, and host Forge's Android APK for local phone download."
    )
    parser.add_argument("--android-home", help=f"Android SDK path. Default: $ANDROID_HOME or {DEFAULT_ANDROID_HOME}")
    parser.add_argument("--build-tools", default=DEFAULT_BUILD_TOOLS, help=f"Android build-tools version. Default: {DEFAULT_BUILD_TOOLS}")
    parser.add_argument("--port", type=int, default=8090, help="HTTP port to serve on. Default: 8090")
    parser.add_argument("--host-url", help="Public URL to show on the page, for example a Tailscale Funnel URL.")
    parser.add_argument("--no-build", action="store_true", help="Skip Maven and host/sign the newest existing raw APK.")
    parser.add_argument("--no-serve", action="store_true", help="Build/sign/publish but do not start the HTTP server.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    android_home = android_home_from_args(args.android_home)
    build_tools = build_tools_dir(android_home, args.build_tools)
    if not args.no_build:
        build_apk(android_home)
    raw_apk = newest_raw_apk()
    signed_apk = sign_apk(raw_apk, build_tools)
    urls = publish(signed_apk, args.port, args.host_url)

    print("\nAPK ready:")
    print(f"  Source: {signed_apk}")
    print(f"  Hosted: {SERVE_DIR / LATEST_APK}")
    for url in urls:
        print(f"  Open on phone: {url}/")

    if not args.no_serve:
        print("\nServing until you press Ctrl+C.")
        serve(args.port, android_home, build_tools, args.host_url, urls)
    return 0


if __name__ == "__main__":
    sys.exit(main())
