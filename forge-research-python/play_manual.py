"""Play a game manually from the CLI, controlling both sides.

Logs every decision point and chosen action to a JSON file for analysis.
Uses dual_rl mode so both players' decisions come through gRPC.

Usage:
    python play_manual.py [--deck-a PATH] [--deck-b PATH] [--port PORT]

The log file is written to plays/game_<timestamp>.json
"""

import argparse
import json
import os
import subprocess
import socket
import sys
import time
from datetime import datetime
from pathlib import Path

from forge_rl.client import ForgeRlClient


def port_is_open(port: int, host: str = "localhost", timeout: float = 0.5) -> bool:
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except (ConnectionRefusedError, OSError):
        return False


def build_classpath(server_dir: str) -> str:
    cp_file = os.path.join(server_dir, "target", "classpath.txt")
    if not os.path.exists(cp_file):
        raise FileNotFoundError(f"Classpath file not found: {cp_file}")
    with open(cp_file, "r") as f:
        deps_cp = f.read().strip()
    module_dirs = [os.path.join(server_dir, "target", "classes")]
    forge_root = os.path.dirname(server_dir)
    for module in ["forge-core", "forge-game", "forge-ai", "forge-gui", "forge-gui-desktop"]:
        classes_dir = os.path.join(forge_root, module, "target", "classes")
        if os.path.isdir(classes_dir):
            module_dirs.append(classes_dir)
    return os.pathsep.join(module_dirs) + os.pathsep + deps_cp


DECISION_TYPE_NAMES = {
    0: "CHOOSE_SPELL_ABILITY",
    1: "DECLARE_ATTACKERS",
    2: "DECLARE_BLOCKERS",
    3: "CHOOSE_TARGETS",
    4: "MULLIGAN",
    5: "CHOOSE_CARDS",
    6: "CONFIRM_ACTION",
    7: "CHOOSE_ENTITY",
    8: "CHOOSE_COLOR",
    9: "CHOOSE_NUMBER",
    10: "CHOOSE_TYPE",
    11: "CHOOSE_PILE",
    12: "CHOOSE_MODE",
    13: "ORDER_CARDS",
    14: "AI_FALLBACK",
}


def format_game_state(obs):
    """Print a compact game state summary."""
    gi = obs.game_info
    agent = obs.agent_player
    opp = obs.opponent_player

    print(f"\n  Turn {gi.turn} | Phase {gi.phase} | "
          f"Active: P{gi.active_player_index} | Priority: P{gi.priority_player_index}")
    print(f"  P0 (deck A): Life={agent.life} | Hand={agent.hand_size} | "
          f"Library={agent.library_size} | Battlefield={len(agent.battlefield)}")
    print(f"  P1 (deck B): Life={opp.life} | Hand={opp.hand_size} | "
          f"Library={opp.library_size} | Battlefield={len(opp.battlefield)}")

    if agent.battlefield:
        cards = [f"{c.name}{'(T)' if c.tapped else ''}" for c in agent.battlefield]
        print(f"  P0 board: {', '.join(cards)}")
    if opp.battlefield:
        cards = [f"{c.name}{'(T)' if c.tapped else ''}" for c in opp.battlefield]
        print(f"  P1 board: {', '.join(cards)}")
    if agent.exile:
        cards = [c.name for c in agent.exile]
        print(f"  P0 exile: {', '.join(cards)}")
    if opp.exile:
        cards = [c.name for c in opp.exile]
        print(f"  P1 exile: {', '.join(cards)}")
    if obs.stack:
        entries = [f"{s.source_card_name}" for s in obs.stack]
        print(f"  Stack: {', '.join(entries)}")


def format_hand(player_state):
    """Show the hand for context."""
    if player_state.hand:
        cards = [f"{c.name} (cmc={c.cmc})" for c in player_state.hand]
        print(f"  Hand: {', '.join(cards)}")


def play_game(client, deck_a, deck_b, log):
    """Play one full game, logging all decisions."""
    print("\n" + "=" * 60)
    print("Starting new game")
    print(f"  Deck A (P0): {os.path.basename(deck_a)}")
    print(f"  Deck B (P1): {os.path.basename(deck_b)}")
    print("=" * 60)

    response = client.reset(
        deck_path_a=deck_a,
        deck_path_b=deck_b,
        agent_player_index=0,
        dual_rl=True,
    )

    decision_num = 0
    game_over = False

    while not game_over:
        obs = response.observation
        dp = response.decision_point

        if not dp or not dp.legal_actions:
            # Check if this is from a step response
            if hasattr(response, 'terminated') and response.terminated:
                game_over = True
                break
            print("\nNo decision point received. Game may have ended.")
            break

        decision_num += 1
        player_idx = dp.player_index
        dt_name = DECISION_TYPE_NAMES.get(dp.type, f"UNKNOWN({dp.type})")

        # Show game state
        format_game_state(obs)

        # Show hand for the deciding player
        if player_idx == 0:
            format_hand(obs.agent_player)
        else:
            format_hand(obs.opponent_player)

        # Show decision
        print(f"\n  [Decision #{decision_num}] Player {player_idx} | {dt_name}")
        print(f"  Prompt: {dp.prompt}")
        print(f"  Legal actions ({len(dp.legal_actions)}):")

        actions_data = []
        for action in dp.legal_actions:
            extra = ""
            if action.source_card_name:
                extra += f" [src: {action.source_card_name}]"
            if action.target_card_id or action.target_is_player:
                tgt = "player" if action.target_is_player else f"card(id={action.target_card_id})"
                extra += f" [tgt: {tgt}, own={action.target_is_own}]"
            print(f"    {action.index}: {action.description}{extra}")
            actions_data.append({
                "index": action.index,
                "description": action.description,
                "source_card_id": action.source_card_id,
                "source_card_name": action.source_card_name,
                "source_name_id": action.source_name_id,
                "target_card_id": action.target_card_id,
                "target_name_id": action.target_name_id,
                "target_is_own": action.target_is_own,
                "target_is_player": action.target_is_player,
            })

        # Get user input
        while True:
            try:
                raw = input(f"\n  Choose action index (P{player_idx}): ").strip()
                if raw.lower() in ('q', 'quit'):
                    print("Quitting.")
                    return
                choice = int(raw)
                valid_indices = [a.index for a in dp.legal_actions]
                if choice in valid_indices:
                    break
                print(f"  Invalid index. Valid: {valid_indices}")
            except (ValueError, EOFError):
                print("  Enter a valid integer.")

        # Log the decision
        log_entry = {
            "decision_num": decision_num,
            "player_index": player_idx,
            "decision_type": dt_name,
            "decision_type_id": dp.type,
            "prompt": dp.prompt,
            "turn": obs.game_info.turn,
            "phase": obs.game_info.phase,
            "agent_life": obs.agent_player.life,
            "opponent_life": obs.opponent_player.life,
            "legal_actions": actions_data,
            "chosen_action": choice,
            "chosen_description": next(
                (a.description for a in dp.legal_actions if a.index == choice), ""),
        }
        log.append(log_entry)

        # Step
        response = client.step(action_index=choice)

        if response.terminated:
            game_over = True
            if response.HasField("game_result"):
                gr = response.game_result
                print("\n" + "=" * 60)
                if gr.is_draw:
                    print("GAME OVER: Draw")
                else:
                    print(f"GAME OVER: Player {gr.winner_index} wins!")
                print(f"  Turns played: {gr.turns_played}")
                print(f"  Win condition: {gr.win_condition}")
                print("=" * 60)

                log.append({
                    "event": "game_over",
                    "winner_index": gr.winner_index,
                    "is_draw": gr.is_draw,
                    "turns_played": gr.turns_played,
                    "win_condition": gr.win_condition,
                    "total_decisions": decision_num,
                })


def main():
    parser = argparse.ArgumentParser(description="Play a manual game with full logging")
    parser.add_argument("--deck-a", default="src/main/resources/decks/mono_red_pingers.dck")
    parser.add_argument("--deck-b", default="src/main/resources/decks/caw_gates.dck")
    parser.add_argument("--port", type=int, default=50051)
    parser.add_argument("--server-jar-dir",
                        default=str(Path(__file__).resolve().parent.parent / "forge-research"))
    parser.add_argument("--no-server", action="store_true",
                        help="Don't start a server (assume one is already running)")
    args = parser.parse_args()

    # Start server if needed
    server_proc = None
    if not args.no_server:
        if port_is_open(args.port):
            print(f"Server already running on port {args.port}")
        else:
            print(f"Starting server on port {args.port}...")
            classpath = build_classpath(args.server_jar_dir)
            server_proc = subprocess.Popen(
                ["java", "-cp", classpath,
                 "forge.research.ForgeResearchServer", str(args.port)],
                cwd=args.server_jar_dir,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            for _ in range(120):
                if server_proc.poll() is not None:
                    print(f"Server exited with code {server_proc.returncode}")
                    sys.exit(1)
                if port_is_open(args.port):
                    print("Server ready.")
                    break
                time.sleep(0.5)
            else:
                print("Server failed to start.")
                sys.exit(1)

    try:
        client = ForgeRlClient(port=args.port)
        log = []

        log.append({
            "event": "game_start",
            "deck_a": args.deck_a,
            "deck_b": args.deck_b,
            "timestamp": datetime.now().isoformat(),
        })

        play_game(client, args.deck_a, args.deck_b, log)

        # Save log
        plays_dir = Path("plays")
        plays_dir.mkdir(exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        log_path = plays_dir / f"game_{timestamp}.json"
        with open(log_path, "w") as f:
            json.dump(log, f, indent=2)
        print(f"\nGame log saved to {log_path}")

        # Print decision type summary
        type_counts = {}
        for entry in log:
            if "decision_type" in entry:
                dt = entry["decision_type"]
                type_counts[dt] = type_counts.get(dt, 0) + 1
        if type_counts:
            print("\nDecision type summary:")
            for dt, count in sorted(type_counts.items(), key=lambda x: -x[1]):
                print(f"  {dt}: {count}")

        client.close()
    finally:
        if server_proc and server_proc.poll() is None:
            server_proc.terminate()
            server_proc.wait(timeout=5)
            print("Server stopped.")


if __name__ == "__main__":
    main()
