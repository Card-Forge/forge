"""Play against a trained ONNX model interactively via CLI.

Usage:
    python play_vs_model.py --model runs/run_002/agent.onnx \
        --your-deck src/main/resources/decks/mono_red_pingers.dck \
        --ai-deck src/main/resources/decks/caw_gates.dck
"""

import atexit
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from forge_rl.client import ForgeRlClient
from train_ppo import start_servers, stop_servers

PHASE_NAMES = {
    0: "Untap", 1: "Upkeep", 2: "Draw",
    3: "Main 1", 4: "Begin Combat", 5: "Declare Attackers",
    6: "Declare Blockers", 7: "Combat Damage", 8: "End Combat",
    9: "Main 2", 10: "End", 11: "Cleanup",
}

TYPE_NAMES = [
    "SPELL/ABILITY", "ATTACKERS", "BLOCKERS", "TARGETS", "MULLIGAN",
    "CARDS", "CONFIRM", "ENTITY", "COLOR", "NUMBER", "TYPE",
    "PILE", "MODE", "ORDER", "AI_FALLBACK",
]


def show_game_state(obs):
    """Display the current game state."""
    gi = obs.game_info
    agent = obs.agent_player
    opp = obs.opponent_player

    phase = PHASE_NAMES.get(gi.phase, f"Phase {gi.phase}")
    print(f"\n{'='*60}")
    print(f"  Turn {gi.turn} | {phase}")
    print(f"{'='*60}")

    # Opponent info (top)
    print(f"\n  OPPONENT: {opp.life} life | Hand: {opp.hand_size} | Library: {opp.library_size}")
    if opp.battlefield:
        bf_cards = [f"  {c.name}" + (" (T)" if c.tapped else "") +
                    (f" {c.power}/{c.toughness}" if c.power > 0 or c.toughness > 0 else "")
                    for c in opp.battlefield]
        print(f"  Battlefield: {', '.join(bf_cards)}")
    if opp.graveyard:
        gy_names = [c.name for c in opp.graveyard]
        print(f"  Graveyard: {', '.join(gy_names)}")

    print(f"\n  {'─'*56}")

    # Your info (bottom)
    print(f"\n  YOU: {agent.life} life | Library: {agent.library_size}")
    mp = agent.mana_pool
    mana_parts = []
    if mp.white: mana_parts.append(f"{mp.white}W")
    if mp.blue: mana_parts.append(f"{mp.blue}U")
    if mp.black: mana_parts.append(f"{mp.black}B")
    if mp.red: mana_parts.append(f"{mp.red}R")
    if mp.green: mana_parts.append(f"{mp.green}G")
    if mp.colorless: mana_parts.append(f"{mp.colorless}C")
    if mana_parts:
        print(f"  Mana pool: {' '.join(mana_parts)}")

    if agent.battlefield:
        bf_cards = [f"  {c.name}" + (" (T)" if c.tapped else "") +
                    (f" {c.power}/{c.toughness}" if c.power > 0 or c.toughness > 0 else "")
                    for c in agent.battlefield]
        print(f"  Battlefield: {', '.join(bf_cards)}")

    if agent.hand:
        hand_names = [c.name for c in agent.hand]
        print(f"  Hand: {', '.join(hand_names)}")

    if agent.graveyard:
        gy_names = [c.name for c in agent.graveyard]
        print(f"  Graveyard: {', '.join(gy_names)}")


def show_decision(dp):
    """Display the decision prompt and legal actions."""
    dt = dp.type
    type_name = TYPE_NAMES[dt] if 0 <= dt < len(TYPE_NAMES) else f"TYPE_{dt}"
    print(f"\n  [{type_name}] {dp.prompt}")
    print()

    for action in dp.legal_actions:
        print(f"    {action.index:>3}) {action.description}")


def get_action(dp):
    """Prompt the user to pick a legal action."""
    legal_indices = {a.index for a in dp.legal_actions}

    while True:
        try:
            raw = input("\n  Your choice > ").strip()
            if raw.lower() in ('q', 'quit', 'exit'):
                return None
            choice = int(raw)
            if choice in legal_indices:
                return choice
            print(f"  Invalid. Choose from: {sorted(legal_indices)}")
        except ValueError:
            print("  Enter a number (or 'q' to quit).")
        except (EOFError, KeyboardInterrupt):
            return None


def main():
    import argparse
    parser = argparse.ArgumentParser(description="Play against a trained ONNX model")
    parser.add_argument("--model", required=True, help="Path to .onnx model file")
    parser.add_argument("--your-deck", required=True, help="Path to your deck file")
    parser.add_argument("--ai-deck", required=True, help="Path to AI's deck file")
    parser.add_argument("--port", type=int, default=50061, help="Server port")
    parser.add_argument("--server-dir", type=str,
                        default=str(Path(__file__).resolve().parent.parent / "forge-research"))
    args = parser.parse_args()

    model_path = str(Path(args.model).resolve())
    print(f"AI model: {model_path}")
    print(f"Your deck: {args.your_deck}")
    print(f"AI deck: {args.ai_deck}")

    # Start server
    print("\nStarting game server...")
    procs = start_servers(1, args.port, args.server_dir)
    atexit.register(stop_servers, procs)

    client = ForgeRlClient(port=args.port)

    # You are player 0 (deck A), AI is player 1 (deck B) with ONNX
    # agent_player_index=0 means player 0 sends decisions via gRPC (you)
    # The opponent (player 1) uses ONNX
    response = client.reset(
        deck_path_a=args.your_deck,
        deck_path_b=args.ai_deck,
        agent_player_index=0,
        dual_rl=True,
        opponent_model_path=model_path,
    )

    print("\nGame started! Type 'q' to quit.\n")

    step = 0
    while True:
        show_game_state(response.observation)
        show_decision(response.decision_point)

        action = get_action(response.decision_point)
        if action is None:
            print("\nQuitting.")
            break

        response = client.step(action_index=action)
        step += 1

        if response.terminated:
            show_game_state(response.observation)
            gr = response.game_result
            if gr.is_draw:
                print("\n  DRAW!")
            elif gr.winner_index == 0:
                print("\n  YOU WIN!")
            else:
                print("\n  YOU LOSE!")
            print(f"  Turns played: {gr.turns_played}")
            break

    client.close()
    stop_servers(procs)


if __name__ == "__main__":
    main()
