"""Gymnasium environment wrapping the Forge RL gRPC service."""

from __future__ import annotations

from typing import Any, Optional

import gymnasium as gym
import numpy as np
from gymnasium import spaces

from forge_rl.client import ForgeRlClient


# Observation dimension constants
MAX_HAND = 10
MAX_BATTLEFIELD = 20
MAX_GRAVEYARD = 15
MAX_EXILE = 10
MAX_STACK = 10
CARD_FEATURES = 16
STACK_FEATURES = 8
MAX_ACTIONS = 256
NUM_DECISION_TYPES = 15
ACTION_FEATURES = 7

# Card feature indices (matching proto order)
_CARD_FIELDS = [
    "name_id", "power", "toughness", "cmc",
    "tapped", "summoning_sick", "colors_bitmask", "damage",
    "loyalty", "controller_index", "attacking", "blocking",
    "counter_count", "keyword_count", "card_id", "owner_index",
]


class ForgeRlEnv(gym.Env):
    """
    Gymnasium environment for Magic: The Gathering via Forge.

    Communicates with a running ForgeResearchServer over gRPC.
    Observations are flat numpy arrays; actions are discrete with masking.

    When opponent_model_path is set, Java handles opponent inference via ONNX
    on the game thread — the Python side sees a simple single-player flow.
    """

    metadata = {"render_modes": []}

    def __init__(
        self,
        deck_path_a: str,
        deck_path_b: str,
        agent_player_index: int = 0,
        host: str = "localhost",
        port: int = 50051,
        opponent_model_path: Optional[str] = None,
    ):
        super().__init__()

        self.deck_path_a = deck_path_a
        self.deck_path_b = deck_path_b
        self.agent_player_index = agent_player_index
        self.opponent_model_path = opponent_model_path or ""

        self.client = ForgeRlClient(host=host, port=port)

        # Action space: discrete with masking
        self.action_space = spaces.Discrete(MAX_ACTIONS)

        # Observation space: dictionary of numpy arrays
        self.observation_space = spaces.Dict({
            "game_info": spaces.Box(low=-1, high=1000, shape=(6,), dtype=np.int32),
            "agent_scalars": spaces.Box(low=-100, high=1000, shape=(11,), dtype=np.int32),
            "opponent_scalars": spaces.Box(low=-100, high=1000, shape=(11,), dtype=np.int32),
            "agent_hand": spaces.Box(low=-1, high=100000, shape=(MAX_HAND, CARD_FEATURES), dtype=np.int32),
            "agent_battlefield": spaces.Box(low=-1, high=100000, shape=(MAX_BATTLEFIELD, CARD_FEATURES), dtype=np.int32),
            "opponent_battlefield": spaces.Box(low=-1, high=100000, shape=(MAX_BATTLEFIELD, CARD_FEATURES), dtype=np.int32),
            "agent_graveyard": spaces.Box(low=-1, high=100000, shape=(MAX_GRAVEYARD, CARD_FEATURES), dtype=np.int32),
            "opponent_graveyard": spaces.Box(low=-1, high=100000, shape=(MAX_GRAVEYARD, CARD_FEATURES), dtype=np.int32),
            "agent_exile": spaces.Box(low=-1, high=100000, shape=(MAX_EXILE, CARD_FEATURES), dtype=np.int32),
            "opponent_exile": spaces.Box(low=-1, high=100000, shape=(MAX_EXILE, CARD_FEATURES), dtype=np.int32),
            "stack": spaces.Box(low=-1, high=100000, shape=(MAX_STACK, STACK_FEATURES), dtype=np.int32),
            "action_mask": spaces.Box(low=0, high=1, shape=(MAX_ACTIONS,), dtype=np.int8),
            "decision_type": spaces.Box(low=0, high=1, shape=(NUM_DECISION_TYPES,), dtype=np.int8),
            "action_features": spaces.Box(low=-1, high=100, shape=(MAX_ACTIONS, ACTION_FEATURES), dtype=np.float32),
        })

        self._last_decision_point = None

    def reset(
        self,
        *,
        seed: Optional[int] = None,
        options: Optional[dict] = None,
    ) -> tuple[dict, dict]:
        super().reset(seed=seed)

        response = self.client.reset(
            deck_path_a=self.deck_path_a,
            deck_path_b=self.deck_path_b,
            agent_player_index=self.agent_player_index,
            seed=seed if seed is not None else 0,
            dual_rl=bool(self.opponent_model_path),
            opponent_model_path=self.opponent_model_path,
        )

        self._last_decision_point = response.decision_point
        obs = self._build_observation(response.observation, response.decision_point)
        info = self._build_info(response.decision_point)
        return obs, info

    def step(self, action: int) -> tuple[dict, float, bool, bool, dict]:
        response = self.client.step(action_index=action)

        terminated = response.terminated
        self._last_decision_point = response.decision_point
        obs = self._build_observation(response.observation, response.decision_point)
        reward = float(response.reward)
        truncated = False
        info = self._build_info(response.decision_point)

        if response.HasField("game_result"):
            info["game_result"] = {
                "winner_index": response.game_result.winner_index,
                "is_draw": response.game_result.is_draw,
                "turns_played": response.game_result.turns_played,
                "win_condition": response.game_result.win_condition,
            }

        return obs, reward, terminated, truncated, info

    def close(self):
        self.client.close()

    def _build_observation(self, obs_proto, decision_point) -> dict:
        """Convert protobuf Observation + DecisionPoint into numpy dict."""
        result = {}

        # Game info
        gi = obs_proto.game_info
        result["game_info"] = np.array([
            gi.turn, gi.phase, gi.active_player_index, gi.priority_player_index,
            gi.agent_mulligan_count, gi.opponent_mulligan_count,
        ], dtype=np.int32)

        # Player scalars
        result["agent_scalars"] = self._player_scalars(obs_proto.agent_player)
        result["opponent_scalars"] = self._player_scalars(obs_proto.opponent_player)

        # Card zones
        result["agent_hand"] = self._card_matrix(obs_proto.agent_player.hand, MAX_HAND)
        result["agent_battlefield"] = self._card_matrix(obs_proto.agent_player.battlefield, MAX_BATTLEFIELD)
        result["opponent_battlefield"] = self._card_matrix(obs_proto.opponent_player.battlefield, MAX_BATTLEFIELD)
        result["agent_graveyard"] = self._card_matrix(obs_proto.agent_player.graveyard, MAX_GRAVEYARD)
        result["opponent_graveyard"] = self._card_matrix(obs_proto.opponent_player.graveyard, MAX_GRAVEYARD)
        result["agent_exile"] = self._card_matrix(obs_proto.agent_player.exile, MAX_EXILE)
        result["opponent_exile"] = self._card_matrix(obs_proto.opponent_player.exile, MAX_EXILE)

        # Stack
        result["stack"] = self._stack_matrix(obs_proto.stack)

        # Action mask
        action_mask = np.zeros(MAX_ACTIONS, dtype=np.int8)
        if decision_point and decision_point.legal_actions:
            for action in decision_point.legal_actions:
                if 0 <= action.index < MAX_ACTIONS:
                    action_mask[action.index] = 1
        result["action_mask"] = action_mask

        # Decision type one-hot
        decision_type = np.zeros(NUM_DECISION_TYPES, dtype=np.int8)
        if decision_point:
            dt = decision_point.type
            if 0 <= dt < NUM_DECISION_TYPES:
                decision_type[dt] = 1
        result["decision_type"] = decision_type

        # Per-action features: [log_name_id, log_card_id, is_pass,
        #   target_is_player, log_target_name_id, log_target_card_id, target_is_own]
        action_features = np.full((MAX_ACTIONS, ACTION_FEATURES), -1, dtype=np.float32)
        if decision_point and decision_point.legal_actions:
            for action in decision_point.legal_actions:
                idx = action.index
                if 0 <= idx < MAX_ACTIONS:
                    src_name_id = action.source_name_id
                    src_card_id = action.source_card_id
                    is_pass = 1.0 if (src_name_id == 0 and src_card_id == 0) else 0.0
                    action_features[idx] = [
                        np.log1p(src_name_id),
                        np.log1p(src_card_id),
                        is_pass,
                        1.0 if action.target_is_player else 0.0,
                        np.log1p(action.target_name_id),
                        np.log1p(action.target_card_id),
                        1.0 if action.target_is_own else 0.0,
                    ]
        result["action_features"] = action_features

        return result

    def _player_scalars(self, player_state) -> np.ndarray:
        mp = player_state.mana_pool
        return np.array([
            player_state.life,
            player_state.hand_size,
            player_state.library_size,
            player_state.lands_played,
            player_state.max_lands,
            mp.white,
            mp.blue,
            mp.black,
            mp.red,
            mp.green,
            mp.colorless,
        ], dtype=np.int32)

    def _card_matrix(self, cards, max_cards: int) -> np.ndarray:
        mat = np.full((max_cards, CARD_FEATURES), -1, dtype=np.int32)
        for i, card in enumerate(cards):
            if i >= max_cards:
                break
            mat[i] = [
                card.name_id,
                card.power,
                card.toughness,
                card.cmc,
                int(card.tapped),
                int(card.summoning_sick),
                card.colors_bitmask,
                card.damage,
                card.loyalty,
                card.controller_index,
                int(card.attacking),
                int(card.blocking),
                card.counter_count,
                card.keyword_count,
                card.card_id,
                card.owner_index,
            ]
        return mat

    def _stack_matrix(self, stack_entries) -> np.ndarray:
        mat = np.full((MAX_STACK, STACK_FEATURES), -1, dtype=np.int32)
        for i, entry in enumerate(stack_entries):
            if i >= MAX_STACK:
                break
            mat[i] = [
                entry.source_card_id,
                entry.controller_index,
                0, 0, 0, 0, 0, 0,  # reserved for future features
            ]
        return mat

    def _build_info(self, decision_point) -> dict:
        info = {}
        if decision_point:
            info["decision_type"] = decision_point.type
            info["prompt"] = decision_point.prompt
            info["num_legal_actions"] = len(decision_point.legal_actions)
            info["legal_actions"] = [
                {"index": a.index, "description": a.description}
                for a in decision_point.legal_actions
            ]
        return info
