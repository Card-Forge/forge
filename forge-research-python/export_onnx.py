"""Export a trained Agent checkpoint to ONNX format.

The ONNX model takes 13 positional tensor inputs (all OBS_KEYS except action_mask)
and returns logits of shape (1, 256). action_mask is excluded — it's applied in
Java post-inference.

Usage:
    python export_onnx.py --checkpoint runs/run_002/agent.pt --output runs/run_002/agent.onnx
"""

import argparse
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn

# Import the Agent class from the training script
from train_ppo import Agent


# The 13 observation keys fed to the ONNX model (action_mask excluded)
ONNX_INPUT_KEYS = [
    "game_info",           # (1, 6)
    "agent_scalars",       # (1, 11)
    "opponent_scalars",    # (1, 11)
    "agent_hand",          # (1, 10, 16)
    "agent_battlefield",   # (1, 20, 16)
    "opponent_battlefield",# (1, 20, 16)
    "agent_graveyard",     # (1, 15, 16)
    "opponent_graveyard",  # (1, 15, 16)
    "agent_exile",         # (1, 10, 16)
    "opponent_exile",      # (1, 10, 16)
    "stack",               # (1, 10, 8)
    "decision_type",       # (1, 15)
    "action_features",     # (1, 256, 7)
]


class AgentOnnxWrapper(nn.Module):
    """Wrapper that takes 13 positional tensor inputs and returns logits (1, 256).

    All int inputs are cast to float internally (ONNX traces as float).
    action_mask is NOT included — it's applied in Java post-inference.
    """

    def __init__(self, agent: Agent):
        super().__init__()
        self.agent = agent

    def forward(
        self,
        game_info: torch.Tensor,
        agent_scalars: torch.Tensor,
        opponent_scalars: torch.Tensor,
        agent_hand: torch.Tensor,
        agent_battlefield: torch.Tensor,
        opponent_battlefield: torch.Tensor,
        agent_graveyard: torch.Tensor,
        opponent_graveyard: torch.Tensor,
        agent_exile: torch.Tensor,
        opponent_exile: torch.Tensor,
        stack: torch.Tensor,
        decision_type: torch.Tensor,
        action_features: torch.Tensor,
    ) -> torch.Tensor:
        # Build obs dict with float tensors
        obs = {
            "game_info": game_info.float(),
            "agent_scalars": agent_scalars.float(),
            "opponent_scalars": opponent_scalars.float(),
            "agent_hand": agent_hand.float(),
            "agent_battlefield": agent_battlefield.float(),
            "opponent_battlefield": opponent_battlefield.float(),
            "agent_graveyard": agent_graveyard.float(),
            "opponent_graveyard": opponent_graveyard.float(),
            "agent_exile": agent_exile.float(),
            "opponent_exile": opponent_exile.float(),
            "stack": stack.float(),
            "decision_type": decision_type.float(),
            "action_features": action_features.float(),
        }

        trunk = self.agent._get_trunk(obs)

        # Compute logits (same as get_action_and_value but without masking/sampling)
        action_enc = self.agent._encode_actions(obs["action_features"])  # (B, 256, 32)
        trunk_proj = self.agent.trunk_to_action(trunk)                   # (B, 32)
        logits = torch.einsum('bd,bnd->bn', trunk_proj, action_enc)      # (B, 256)

        return logits


def make_dummy_inputs(device: torch.device = torch.device("cpu")):
    """Create dummy inputs matching observation shapes (batch=1)."""
    from train_ppo import CARD_FEATURES, STACK_FEATURES
    return (
        torch.zeros(1, 6, device=device),       # game_info
        torch.zeros(1, 11, device=device),      # agent_scalars
        torch.zeros(1, 11, device=device),      # opponent_scalars
        torch.full((1, 10, CARD_FEATURES), -1, dtype=torch.float32, device=device),  # agent_hand
        torch.full((1, 20, CARD_FEATURES), -1, dtype=torch.float32, device=device),  # agent_battlefield
        torch.full((1, 20, CARD_FEATURES), -1, dtype=torch.float32, device=device),  # opponent_battlefield
        torch.full((1, 15, CARD_FEATURES), -1, dtype=torch.float32, device=device),  # agent_graveyard
        torch.full((1, 15, CARD_FEATURES), -1, dtype=torch.float32, device=device),  # opponent_graveyard
        torch.full((1, 10, CARD_FEATURES), -1, dtype=torch.float32, device=device),  # agent_exile
        torch.full((1, 10, CARD_FEATURES), -1, dtype=torch.float32, device=device),  # opponent_exile
        torch.full((1, 10, STACK_FEATURES), -1, dtype=torch.float32, device=device), # stack
        torch.zeros(1, 15, device=device),      # decision_type
        torch.full((1, 256, 7), -1, dtype=torch.float32, device=device),  # action_features
    )


def main():
    parser = argparse.ArgumentParser(description="Export Agent to ONNX")
    parser.add_argument("--checkpoint", required=True, help="Path to agent.pt checkpoint")
    parser.add_argument("--output", required=True, help="Path for output .onnx file")
    args = parser.parse_args()

    # Load model
    agent = Agent()
    agent.load_state_dict(torch.load(args.checkpoint, map_location="cpu", weights_only=True))
    agent.eval()

    wrapper = AgentOnnxWrapper(agent)
    wrapper.eval()

    dummy_inputs = make_dummy_inputs()

    # Ensure output directory exists
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)

    # Export
    torch.onnx.export(
        wrapper,
        dummy_inputs,
        args.output,
        input_names=ONNX_INPUT_KEYS,
        output_names=["logits"],
        dynamic_axes={name: {0: "batch"} for name in ONNX_INPUT_KEYS + ["logits"]},
        opset_version=17,
    )

    print(f"Exported ONNX model to {args.output}")

    # Verify
    import onnxruntime as ort
    session = ort.InferenceSession(args.output)
    feeds = {name: inp.numpy() for name, inp in zip(ONNX_INPUT_KEYS, dummy_inputs)}
    outputs = session.run(None, feeds)
    print(f"Verification: output shape = {outputs[0].shape}")


if __name__ == "__main__":
    main()
