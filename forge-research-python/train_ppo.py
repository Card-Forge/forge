"""CleanRL-style PPO training script for Forge RL with action masking."""

import atexit
import os
import random
import socket
import subprocess
import time
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import gymnasium as gym
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from torch.distributions import Categorical
from torch.utils.tensorboard import SummaryWriter

from forge_rl.env import ForgeRlEnv


# ---------------------------------------------------------------------------
# Hyperparameters
# ---------------------------------------------------------------------------

@dataclass
class Args:
    # Environment
    num_envs: int = 8
    num_steps: int = 128
    total_timesteps: int = 5_000_000
    deck_a: str = "src/main/resources/decks/mono_red_pingers.dck"
    deck_b: str = "src/main/resources/decks/caw_gates.dck"

    # Server
    server_base_port: int = 50051
    server_jar_dir: str = str(Path(__file__).resolve().parent.parent / "forge-research")

    # PPO
    learning_rate: float = 2.5e-4
    gamma: float = 0.999
    gae_lambda: float = 0.95
    num_minibatches: int = 4
    update_epochs: int = 4
    clip_coef: float = 0.2
    ent_coef: float = 0.05
    vf_coef: float = 0.5
    max_grad_norm: float = 0.5

    # Misc
    seed: int = 1
    track: bool = False
    run_name: str = ""
    anneal_lr: bool = True

    # Dual RL (train against frozen opponent)
    opponent_model: str = ""
    swap_decks: bool = False
    no_reward_shaping: bool = False

    @property
    def batch_size(self) -> int:
        return self.num_envs * self.num_steps

    @property
    def minibatch_size(self) -> int:
        return self.batch_size // self.num_minibatches


# ---------------------------------------------------------------------------
# Masked Categorical
# ---------------------------------------------------------------------------

class CategoricalMasked(Categorical):
    """Categorical distribution with invalid-action masking."""

    def __init__(self, logits: torch.Tensor, mask: torch.Tensor):
        self.mask = mask.bool()
        masked_logits = logits.clone()
        masked_logits[~self.mask] = -1e8
        super().__init__(logits=masked_logits)

    def entropy(self):
        # Compute entropy only over valid actions, normalized to [0, 1]
        p_log_p = self.logits * self.probs
        p_log_p[~self.mask] = 0.0
        raw = -p_log_p.sum(dim=-1)
        max_ent = torch.log(self.mask.sum(dim=-1).clamp(min=2).float())
        return raw / max_ent


# ---------------------------------------------------------------------------
# Agent (Policy + Value Network)
# ---------------------------------------------------------------------------

CARD_ZONES = [
    "agent_hand", "agent_battlefield", "opponent_battlefield",
    "agent_graveyard", "opponent_graveyard",
    "agent_exile", "opponent_exile",
]
CARD_FEATURES = 19
STACK_FEATURES = 8


def layer_init(layer, std=np.sqrt(2), bias_const=0.0):
    nn.init.orthogonal_(layer.weight, std)
    nn.init.constant_(layer.bias, bias_const)
    return layer


class Agent(nn.Module):
    def __init__(self):
        super().__init__()

        # Scalar encoder: game_info(6) + agent_scalars(11) + opponent_scalars(11) + decision_type(15) = 43
        self.scalar_encoder = nn.Sequential(
            layer_init(nn.Linear(43, 64)),
            nn.ReLU(),
        )

        # Card encoder (shared across all 7 zones)
        self.card_encoder = nn.Sequential(
            layer_init(nn.Linear(CARD_FEATURES, 32)),
            nn.ReLU(),
        )

        # Stack encoder
        self.stack_encoder = nn.Sequential(
            layer_init(nn.Linear(STACK_FEATURES, 16)),
            nn.ReLU(),
        )

        # Trunk: 64 (scalars) + 7*32 (cards) + 16 (stack) = 304
        trunk_input = 64 + 7 * 32 + 16  # 304
        self.trunk = nn.Sequential(
            layer_init(nn.Linear(trunk_input, 256)),
            nn.ReLU(),
            layer_init(nn.Linear(256, 128)),
            nn.ReLU(),
        )

        # Action encoder: per-action features → embedding
        self.action_encoder = nn.Sequential(
            layer_init(nn.Linear(7, 32)),
            nn.ReLU(),
        )
        # Project trunk to action embedding space for dot-product scoring
        self.trunk_to_action = layer_init(nn.Linear(128, 32), std=0.01)

        # Critic head
        self.critic = layer_init(nn.Linear(128, 1), std=1.0)

    def _encode_cards(self, card_matrix: torch.Tensor) -> torch.Tensor:
        """Encode a (batch, max_cards, features) matrix via shared card encoder with mean pooling."""
        # Mask: cards with all -1 are padding
        # Use first feature (name_id) >= 0 as indicator of real card
        valid = (card_matrix[:, :, 0] >= 0).float().unsqueeze(-1)  # (batch, max_cards, 1)
        encoded = self.card_encoder(card_matrix.float())  # (batch, max_cards, 32)
        encoded = encoded * valid
        count = valid.sum(dim=1).clamp(min=1)  # (batch, 1)
        pooled = encoded.sum(dim=1) / count  # (batch, 32)
        return pooled

    def _encode_stack(self, stack_matrix: torch.Tensor) -> torch.Tensor:
        """Encode stack entries with mean pooling."""
        valid = (stack_matrix[:, :, 0] >= 0).float().unsqueeze(-1)
        encoded = self.stack_encoder(stack_matrix.float())
        encoded = encoded * valid
        count = valid.sum(dim=1).clamp(min=1)
        pooled = encoded.sum(dim=1) / count
        return pooled

    def _get_trunk(self, obs: dict[str, torch.Tensor]) -> torch.Tensor:
        # Scalars
        scalars = torch.cat([
            obs["game_info"].float(),
            obs["agent_scalars"].float(),
            obs["opponent_scalars"].float(),
            obs["decision_type"].float(),
        ], dim=-1)  # (batch, 41)
        scalar_enc = self.scalar_encoder(scalars)  # (batch, 64)

        # Cards (7 zones, shared encoder)
        card_encs = []
        for zone in CARD_ZONES:
            card_encs.append(self._encode_cards(obs[zone]))  # each (batch, 32)
        card_enc = torch.cat(card_encs, dim=-1)  # (batch, 224)

        # Stack
        stack_enc = self._encode_stack(obs["stack"])  # (batch, 16)

        # Trunk
        combined = torch.cat([scalar_enc, card_enc, stack_enc], dim=-1)  # (batch, 304)
        return self.trunk(combined)

    def get_value(self, obs: dict[str, torch.Tensor]) -> torch.Tensor:
        return self.critic(self._get_trunk(obs))

    def get_action_and_value(
        self,
        obs: dict[str, torch.Tensor],
        action: Optional[torch.Tensor] = None,
    ):
        trunk = self._get_trunk(obs)

        # Attention-style actor: dot product of trunk projection and action embeddings
        action_enc = self.action_encoder(obs["action_features"].float())  # (B, 256, 32)
        trunk_proj = self.trunk_to_action(trunk)                           # (B, 32)
        logits = torch.einsum('bd,bnd->bn', trunk_proj, action_enc)        # (B, 256)

        mask = obs["action_mask"]
        dist = CategoricalMasked(logits, mask)

        if action is None:
            action = dist.sample()

        return action, dist.log_prob(action), dist.entropy(), self.critic(trunk)


# ---------------------------------------------------------------------------
# Observation utilities
# ---------------------------------------------------------------------------

OBS_KEYS = [
    "game_info", "agent_scalars", "opponent_scalars",
    "agent_hand", "agent_battlefield", "opponent_battlefield",
    "agent_graveyard", "opponent_graveyard",
    "agent_exile", "opponent_exile",
    "stack", "action_mask",
    "decision_type", "action_features",
]


DECISION_TYPE_NAMES = [
    "choose_spell_ability", "declare_attackers", "declare_blockers",
    "choose_targets", "mulligan", "choose_cards", "confirm_action",
    "choose_entity", "choose_color", "choose_number", "choose_type",
    "choose_pile", "choose_mode", "order_cards", "ai_fallback",
]
DT_MULLIGAN = 4

# Card feature indices (matching env.py _CARD_FIELDS order)
CARD_IDX_NAME_ID = 0
CARD_IDX_CMC = 3
CARD_IDX_TYPE_BITMASK = 16
TYPE_BIT_LAND = 1 << 1


def obs_to_tensor(obs: dict[str, np.ndarray], device: torch.device) -> dict[str, torch.Tensor]:
    """Convert a dict of numpy arrays to a dict of torch tensors."""
    return {k: torch.tensor(obs[k], device=device) for k in OBS_KEYS}


def obs_to_tensor_float(obs: dict[str, np.ndarray], device: torch.device) -> dict[str, torch.Tensor]:
    """Convert obs dict to tensors (used for single env step, adds batch dim)."""
    return {k: torch.tensor(obs[k], device=device) for k in OBS_KEYS}


# ---------------------------------------------------------------------------
# Server management
# ---------------------------------------------------------------------------

def _port_is_open(port: int, host: str = "localhost", timeout: float = 0.5) -> bool:
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except (ConnectionRefusedError, OSError):
        return False


def _build_classpath(server_dir: str) -> str:
    """Build the Java classpath from Maven output."""
    cp_file = os.path.join(server_dir, "target", "classpath.txt")
    if not os.path.exists(cp_file):
        raise FileNotFoundError(
            f"Classpath file not found: {cp_file}\n"
            "Run 'mvn dependency:build-classpath -Dmdep.outputFile=target/classpath.txt' in forge-research/"
        )
    with open(cp_file, "r") as f:
        deps_cp = f.read().strip()

    # Add module target/classes dirs
    module_dirs = [
        os.path.join(server_dir, "target", "classes"),
    ]
    # Also add sibling module classes that might be needed
    forge_root = os.path.dirname(server_dir)
    for module in ["forge-core", "forge-game", "forge-ai", "forge-gui", "forge-gui-desktop"]:
        classes_dir = os.path.join(forge_root, module, "target", "classes")
        if os.path.isdir(classes_dir):
            module_dirs.append(classes_dir)

    return os.pathsep.join(module_dirs) + os.pathsep + deps_cp


def start_servers(
    num_servers: int,
    base_port: int,
    server_dir: str,
    deck_paths: list[str] | None = None,
    log_dir: str | None = None,
) -> list[subprocess.Popen]:
    """Spawn N ForgeResearchServer Java processes sequentially.

    When deck_paths are provided, enables lazy card loading — each server
    only loads the cards from those decks (~30-50MB vs ~400MB full DB).
    When log_dir is provided, server stdout/stderr is written to per-server log files.
    """
    classpath = _build_classpath(server_dir)
    procs = []

    # Resolve deck paths to absolute so they work from server_dir cwd
    abs_deck_paths = []
    if deck_paths:
        for dp in deck_paths:
            abs_deck_paths.append(str(Path(dp).resolve()))

    # Set up log directory
    log_files = []
    if log_dir:
        Path(log_dir).mkdir(parents=True, exist_ok=True)

    for i in range(num_servers):
        port = base_port + i
        if _port_is_open(port):
            raise RuntimeError(f"Port {port} is already in use")

        cmd = ["java", "-Xmx128m", "-cp", classpath,
               "forge.research.ForgeResearchServer", str(port)]
        cmd.extend(abs_deck_paths)

        if log_dir:
            log_path = Path(log_dir) / f"server_{port}.log"
            log_file = open(log_path, "w")
            log_files.append(log_file)
            stdout_dest = log_file
            stderr_dest = log_file
        else:
            stdout_dest = subprocess.DEVNULL
            stderr_dest = subprocess.DEVNULL

        proc = subprocess.Popen(
            cmd,
            cwd=server_dir,
            stdout=stdout_dest,
            stderr=stderr_dest,
        )
        procs.append(proc)
        print(f"  Starting server {i+1}/{num_servers} on port {port} (PID={proc.pid})...")

        # Wait for this server before launching the next
        for attempt in range(120):  # up to 60 seconds
            if proc.poll() is not None:
                raise RuntimeError(
                    f"Server on port {port} exited with code {proc.returncode}"
                )
            if _port_is_open(port):
                print(f"  Server on port {port} is ready")
                break
            time.sleep(0.5)
        else:
            raise RuntimeError(f"Server on port {port} did not start within 60 seconds")

    return procs


def stop_servers(procs: list[subprocess.Popen]):
    """Terminate all server processes."""
    for proc in procs:
        if proc.poll() is None:
            proc.terminate()
    for proc in procs:
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()
    print("All servers stopped.")


# ---------------------------------------------------------------------------
# Reward shaping wrapper
# ---------------------------------------------------------------------------

class RewardShaping(gym.Wrapper):
    """Add intermediate rewards to guide learning.

    - Opponent life change: +0.05 per point of damage dealt, -0.05 per point gained.
      Applied per decision step (damage events happen at specific moments).
    - Board creature presence: +0.01 per creature per turn (not per decision step).
      Only counts creatures (cards with power >= 0, since lands/artifacts have power = -1).
    - Mulligan penalty: -0.5 for keeping a hand with 0 lands or 5+ lands.
    - Terminal +1/-1 from the base env is preserved unchanged.
    """

    LIFE_SCALE = 0.05    # per opponent life point lost
    BOARD_SCALE = 0.01   # per creature per turn (applied once when turn changes)
    MULL_PENALTY = -0.5  # penalty for keeping unplayable hands

    def __init__(self, env: gym.Env):
        super().__init__(env)
        self._prev_opp_life = 20
        self._prev_turn = 0
        self._pending_mull_obs = None  # hand obs when facing a mulligan decision

    def reset(self, **kwargs):
        obs, info = self.env.reset(**kwargs)
        self._prev_opp_life = int(obs["opponent_scalars"][0])
        self._prev_turn = int(obs["game_info"][0])
        # Check if first decision is a mulligan
        if info.get("decision_type") == DT_MULLIGAN:
            self._pending_mull_obs = obs
        else:
            self._pending_mull_obs = None
        return obs, info

    def step(self, action):
        # Check if we're resolving a mulligan decision
        mull_penalty = 0.0
        if self._pending_mull_obs is not None:
            if action == 0:  # Keep
                hand = self._pending_mull_obs["agent_hand"]
                occupied = hand[:, CARD_IDX_NAME_ID] > 0
                is_land = occupied & ((hand[:, CARD_IDX_TYPE_BITMASK] & TYPE_BIT_LAND) != 0)
                land_count = int(is_land.sum())
                if land_count == 0 or land_count >= 5:
                    mull_penalty = self.MULL_PENALTY
            self._pending_mull_obs = None

        obs, reward, terminated, truncated, info = self.env.step(action)
        reward += mull_penalty

        if not terminated:
            opp_life = int(obs["opponent_scalars"][0])

            # Reward for damage dealt, penalty for opponent lifegain
            opp_life_lost = self._prev_opp_life - opp_life
            reward += self.LIFE_SCALE * opp_life_lost
            self._prev_opp_life = opp_life

            # Board creature presence: only on turn change to avoid per-decision accumulation
            current_turn = int(obs["game_info"][0])
            if current_turn != self._prev_turn:
                agent_bf = obs["agent_battlefield"]
                # Count creatures: power >= 0 distinguishes creatures from lands/artifacts
                num_creatures = int((agent_bf[:, 1] >= 0).sum())
                reward += self.BOARD_SCALE * num_creatures
                self._prev_turn = current_turn

            # Track if next decision is a mulligan (for next step)
            if info.get("decision_type") == DT_MULLIGAN:
                self._pending_mull_obs = obs
            else:
                self._pending_mull_obs = None

        return obs, reward, terminated, truncated, info



# ---------------------------------------------------------------------------
# Environment factory
# ---------------------------------------------------------------------------

def make_env(port: int, deck_a: str, deck_b: str, seed: int,
             agent_player_index: int = 0, opponent_model_path: str = "",
             no_reward_shaping: bool = False):
    """Create an env factory function for the given port."""
    def thunk():
        env = ForgeRlEnv(
            deck_path_a=deck_a,
            deck_path_b=deck_b,
            agent_player_index=agent_player_index,
            port=port,
            opponent_model_path=opponent_model_path if opponent_model_path else None,
        )
        if not no_reward_shaping:
            env = RewardShaping(env)
        env = gym.wrappers.RecordEpisodeStatistics(env)
        return env
    return thunk


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def parse_args() -> Args:
    import argparse
    parser = argparse.ArgumentParser(description="PPO training for Forge RL")
    args_obj = Args()

    parser.add_argument("--num-envs", type=int, default=args_obj.num_envs)
    parser.add_argument("--num-steps", type=int, default=args_obj.num_steps)
    parser.add_argument("--total-timesteps", type=int, default=args_obj.total_timesteps)
    parser.add_argument("--deck-a", type=str, default=args_obj.deck_a)
    parser.add_argument("--deck-b", type=str, default=args_obj.deck_b)
    parser.add_argument("--server-base-port", type=int, default=args_obj.server_base_port)
    parser.add_argument("--server-jar-dir", type=str, default=args_obj.server_jar_dir)
    parser.add_argument("--learning-rate", type=float, default=args_obj.learning_rate)
    parser.add_argument("--gamma", type=float, default=args_obj.gamma)
    parser.add_argument("--gae-lambda", type=float, default=args_obj.gae_lambda)
    parser.add_argument("--num-minibatches", type=int, default=args_obj.num_minibatches)
    parser.add_argument("--update-epochs", type=int, default=args_obj.update_epochs)
    parser.add_argument("--clip-coef", type=float, default=args_obj.clip_coef)
    parser.add_argument("--ent-coef", type=float, default=args_obj.ent_coef)
    parser.add_argument("--vf-coef", type=float, default=args_obj.vf_coef)
    parser.add_argument("--max-grad-norm", type=float, default=args_obj.max_grad_norm)
    parser.add_argument("--seed", type=int, default=args_obj.seed)
    parser.add_argument("--track", action="store_true", default=args_obj.track)
    parser.add_argument("--run-name", type=str, default=args_obj.run_name)
    parser.add_argument("--anneal-lr", type=lambda x: x.lower() == "true", default=args_obj.anneal_lr)
    parser.add_argument("--opponent-model", type=str, default=args_obj.opponent_model,
                        help="Path to frozen .onnx model for opponent")
    parser.add_argument("--swap-decks", action="store_true", default=args_obj.swap_decks,
                        help="Agent plays deck B instead of A (player index 1)")
    parser.add_argument("--no-reward-shaping", action="store_true", default=False,
                        help="Disable intermediate reward shaping (pure win/loss)")
    parsed = parser.parse_args()
    return Args(**vars(parsed))


def main():
    args = parse_args()

    if not args.run_name:
        runs_dir = Path("runs")
        existing = sorted(runs_dir.glob("run_*")) if runs_dir.exists() else []
        next_num = 1
        for p in existing:
            try:
                n = int(p.name.split("_")[1])
                next_num = max(next_num, n + 1)
            except (IndexError, ValueError):
                pass
        args.run_name = f"run_{next_num:03d}"

    num_updates = args.total_timesteps // args.batch_size

    # Seeding
    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)
    torch.backends.cudnn.deterministic = True

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Device: {device}")

    # Logging
    writer = SummaryWriter(f"runs/{args.run_name}")
    writer.add_text("hyperparameters", "|param|value|\n|-|-|\n" + "\n".join(
        f"|{k}|{v}|" for k, v in vars(args).items()
    ))

    if args.track:
        import wandb
        wandb.init(project="forge-rl", name=args.run_name, config=vars(args), save_code=True)

    # Opponent model path (ONNX) — passed to Java server for local inference
    opponent_model_path = ""
    agent_player_index = 0
    if args.opponent_model:
        opponent_model_path = str(Path(args.opponent_model).resolve())
        print(f"Using ONNX opponent model: {opponent_model_path}")
        if args.swap_decks:
            agent_player_index = 1
            print("Agent plays deck B (player index 1)")

    # Start servers (pass deck paths for lazy card loading)
    log_dir = f"runs/{args.run_name}/server_logs"
    print(f"Starting {args.num_envs} game servers (logs: {log_dir})...")
    server_procs = start_servers(args.num_envs, args.server_base_port, args.server_jar_dir,
                                 deck_paths=[args.deck_a, args.deck_b], log_dir=log_dir)
    atexit.register(stop_servers, server_procs)

    # Create vectorized environment
    envs = gym.vector.SyncVectorEnv([
        make_env(args.server_base_port + i, args.deck_a, args.deck_b, args.seed + i,
                 agent_player_index=agent_player_index, opponent_model_path=opponent_model_path,
                 no_reward_shaping=args.no_reward_shaping)
        for i in range(args.num_envs)
    ])

    # Agent
    agent = Agent().to(device)
    optimizer = optim.Adam(agent.parameters(), lr=args.learning_rate, eps=1e-5)
    print(f"Agent parameters: {sum(p.numel() for p in agent.parameters()):,}")

    # Rollout storage (numpy dicts for obs, tensors for the rest)
    obs_buf = {
        key: np.zeros((args.num_steps, args.num_envs) + envs.single_observation_space[key].shape,
                       dtype=envs.single_observation_space[key].dtype)
        for key in OBS_KEYS
    }
    actions = torch.zeros((args.num_steps, args.num_envs), dtype=torch.long, device=device)
    logprobs = torch.zeros((args.num_steps, args.num_envs), device=device)
    rewards = torch.zeros((args.num_steps, args.num_envs), device=device)
    dones = torch.zeros((args.num_steps, args.num_envs), device=device)
    values = torch.zeros((args.num_steps, args.num_envs), device=device)

    # Initial reset
    next_obs, info = envs.reset(seed=args.seed)
    next_done = torch.zeros(args.num_envs, device=device)

    # Tracking
    global_step = 0
    episode_count = 0
    win_count = 0
    recent_returns = []

    # Decision type & mulligan tracking (reset each update)
    decision_type_counts = Counter()
    mulligan_events = []  # [(land_count, action_taken, mulligan_count), ...]

    print(f"Starting training: {num_updates} updates, {args.batch_size} batch size")
    start_time = time.time()

    for update in range(1, num_updates + 1):
        # Anneal learning rate
        if args.anneal_lr:
            frac = 1.0 - (update - 1) / num_updates
            lr = frac * args.learning_rate
            optimizer.param_groups[0]["lr"] = lr

        # --- Rollout phase ---
        for step in range(args.num_steps):
            global_step += args.num_envs

            # Store obs and done
            for key in OBS_KEYS:
                obs_buf[key][step] = next_obs[key]
            dones[step] = next_done

            # Get action from policy
            with torch.no_grad():
                obs_t = obs_to_tensor(next_obs, device)
                action, logprob, _, value = agent.get_action_and_value(obs_t)
                values[step] = value.flatten()

            actions[step] = action
            logprobs[step] = logprob

            # Track decision types and mulligan quality
            for env_i in range(args.num_envs):
                dt = int(next_obs["decision_type"][env_i].argmax())
                decision_type_counts[dt] += 1

                if dt == DT_MULLIGAN:
                    hand = next_obs["agent_hand"][env_i]  # (MAX_HAND, CARD_FEATURES)
                    occupied = hand[:, CARD_IDX_NAME_ID] > 0
                    is_land = occupied & ((hand[:, CARD_IDX_TYPE_BITMASK] & TYPE_BIT_LAND) != 0)
                    land_count = int(is_land.sum())
                    mulligan_count = int(next_obs["game_info"][env_i][4])
                    action_taken = int(action[env_i].item())
                    mulligan_events.append((land_count, action_taken, mulligan_count))

            # Step environment
            next_obs, reward, terminated, truncated, infos = envs.step(action.cpu().numpy())
            done = np.logical_or(terminated, truncated)
            rewards[step] = torch.tensor(reward, device=device)
            next_done = torch.tensor(done, dtype=torch.float32, device=device)

            # Log completed episodes (Gymnasium v1.x uses "episode"/"_episode" keys)
            if "episode" in infos and "_episode" in infos:
                # _episode is a boolean mask indicating which envs finished
                finished_mask = infos["_episode"]
                for i, finished in enumerate(finished_mask):
                    if finished:
                        ep_return = float(infos["episode"]["r"][i])
                        ep_length = int(infos["episode"]["l"][i])
                        episode_count += 1
                        recent_returns.append(ep_return)
                        if ep_return > 0:
                            win_count += 1

                        writer.add_scalar("charts/episodic_return", ep_return, global_step)
                        writer.add_scalar("charts/episodic_length", ep_length, global_step)

        # --- GAE ---
        with torch.no_grad():
            obs_t = obs_to_tensor(next_obs, device)
            next_value = agent.get_value(obs_t).flatten()

            advantages = torch.zeros_like(rewards)
            lastgaelam = 0
            for t in reversed(range(args.num_steps)):
                if t == args.num_steps - 1:
                    nextnonterminal = 1.0 - next_done
                    nextvalues = next_value
                else:
                    nextnonterminal = 1.0 - dones[t + 1]
                    nextvalues = values[t + 1]
                delta = rewards[t] + args.gamma * nextvalues * nextnonterminal - values[t]
                advantages[t] = lastgaelam = delta + args.gamma * args.gae_lambda * nextnonterminal * lastgaelam
            returns = advantages + values

        # --- Optimization phase ---
        # Flatten batch
        b_obs = {key: torch.tensor(obs_buf[key].reshape((-1,) + obs_buf[key].shape[2:]), device=device)
                 for key in OBS_KEYS}
        b_actions = actions.reshape(-1)
        b_logprobs = logprobs.reshape(-1)
        b_advantages = advantages.reshape(-1)
        b_returns = returns.reshape(-1)
        b_values = values.reshape(-1)

        b_inds = np.arange(args.batch_size)
        clipfracs = []

        for epoch in range(args.update_epochs):
            np.random.shuffle(b_inds)
            for start in range(0, args.batch_size, args.minibatch_size):
                end = start + args.minibatch_size
                mb_inds = b_inds[start:end]

                mb_obs = {key: b_obs[key][mb_inds] for key in OBS_KEYS}
                _, newlogprob, entropy, newvalue = agent.get_action_and_value(
                    mb_obs, b_actions[mb_inds]
                )

                logratio = newlogprob - b_logprobs[mb_inds]
                ratio = logratio.exp()

                with torch.no_grad():
                    approx_kl = ((ratio - 1) - logratio).mean()
                    clipfracs.append(((ratio - 1.0).abs() > args.clip_coef).float().mean().item())

                mb_advantages = b_advantages[mb_inds]
                mb_advantages = (mb_advantages - mb_advantages.mean()) / (mb_advantages.std() + 1e-8)

                # Policy loss
                pg_loss1 = -mb_advantages * ratio
                pg_loss2 = -mb_advantages * torch.clamp(ratio, 1 - args.clip_coef, 1 + args.clip_coef)
                pg_loss = torch.max(pg_loss1, pg_loss2).mean()

                # Value loss
                v_loss = 0.5 * ((newvalue.flatten() - b_returns[mb_inds]) ** 2).mean()

                # Entropy loss
                entropy_loss = entropy.mean()

                loss = pg_loss - args.ent_coef * entropy_loss + args.vf_coef * v_loss

                optimizer.zero_grad()
                loss.backward()
                nn.utils.clip_grad_norm_(agent.parameters(), args.max_grad_norm)
                optimizer.step()

        # --- Logging ---
        y_pred = b_values.cpu().numpy()
        y_true = b_returns.cpu().numpy()
        var_y = np.var(y_true)
        explained_var = np.nan if var_y == 0 else 1 - np.var(y_true - y_pred) / var_y

        sps = int(global_step / (time.time() - start_time))
        win_rate = win_count / max(episode_count, 1)

        writer.add_scalar("charts/learning_rate", optimizer.param_groups[0]["lr"], global_step)
        writer.add_scalar("charts/win_rate", win_rate, global_step)
        writer.add_scalar("charts/episodes", episode_count, global_step)
        writer.add_scalar("charts/SPS", sps, global_step)
        writer.add_scalar("losses/policy_loss", pg_loss.item(), global_step)
        writer.add_scalar("losses/value_loss", v_loss.item(), global_step)
        writer.add_scalar("losses/entropy", entropy_loss.item(), global_step)
        writer.add_scalar("losses/approx_kl", approx_kl.item(), global_step)
        writer.add_scalar("losses/clipfrac", np.mean(clipfracs), global_step)
        writer.add_scalar("losses/explained_variance", explained_var, global_step)

        # Decision type frequency
        total_decisions = sum(decision_type_counts.values()) or 1
        for dt_idx, name in enumerate(DECISION_TYPE_NAMES):
            count = decision_type_counts.get(dt_idx, 0)
            writer.add_scalar(f"decisions/count_{name}", count, global_step)
            writer.add_scalar(f"decisions/frac_{name}", count / total_decisions, global_step)

        # Mulligan quality metrics
        if mulligan_events:
            actions_taken = [a for _, a, _ in mulligan_events]
            mull_rate = np.mean(actions_taken)
            writer.add_scalar("mulligan/overall_mull_rate", mull_rate, global_step)
            writer.add_scalar("mulligan/num_decisions", len(mulligan_events), global_step)

            # Keep stats
            keeps = [l for l, a, _ in mulligan_events if a == 0]
            if keeps:
                writer.add_scalar("mulligan/avg_lands_when_keeping", np.mean(keeps), global_step)

            # Mulligan rate by land count bucket
            for lc in range(8):
                events = [a for l, a, _ in mulligan_events if l == lc]
                if events:
                    writer.add_scalar(f"mulligan/mull_rate_{lc}_lands", np.mean(events), global_step)

            # Key buckets: 0 lands and 6+ lands (should both be ~1.0)
            zero_land = [a for l, a, _ in mulligan_events if l == 0]
            if zero_land:
                writer.add_scalar("mulligan/mull_rate_0_lands_highlight", np.mean(zero_land), global_step)
            six_plus = [a for l, a, _ in mulligan_events if l >= 6]
            if six_plus:
                writer.add_scalar("mulligan/mull_rate_6plus_lands_highlight", np.mean(six_plus), global_step)

        decision_type_counts.clear()
        mulligan_events.clear()

        if update % 10 == 0 or update == 1:
            avg_return = np.mean(recent_returns[-100:]) if recent_returns else 0.0
            print(
                f"Update {update}/{num_updates} | "
                f"Step {global_step:,} | "
                f"SPS {sps} | "
                f"Episodes {episode_count} | "
                f"Win rate {win_rate:.3f} | "
                f"Avg return {avg_return:.3f} | "
                f"PG loss {pg_loss.item():.4f} | "
                f"V loss {v_loss.item():.4f} | "
                f"Entropy {entropy_loss.item():.4f}"
            )

        # Periodic checkpoint every 50 updates
        if update % 50 == 0:
            ckpt_path = f"runs/{args.run_name}/agent.pt"
            torch.save(agent.state_dict(), ckpt_path)
            print(f"  Checkpoint saved to {ckpt_path} (update {update})")

    # Save final model
    model_path = f"runs/{args.run_name}/agent.pt"
    torch.save(agent.state_dict(), model_path)
    print(f"Model saved to {model_path}")

    envs.close()
    writer.close()
    if args.track:
        wandb.finish()


if __name__ == "__main__":
    main()
