"""gRPC client wrapper for Forge RL service."""

import grpc

from forge_rl.proto import forge_rl_pb2
from forge_rl.proto import forge_rl_pb2_grpc


class ForgeRlClient:
    """Thin wrapper around the gRPC stub for ForgeRlService."""

    def __init__(self, host: str = "localhost", port: int = 50051):
        self.channel = grpc.insecure_channel(f"{host}:{port}")
        self.stub = forge_rl_pb2_grpc.ForgeRlServiceStub(self.channel)

    def reset(
        self,
        deck_path_a: str,
        deck_path_b: str,
        agent_player_index: int = 0,
        seed: int = 0,
        dual_rl: bool = False,
        opponent_model_path: str = "",
    ):
        """Reset the game and return the initial observation."""
        request = forge_rl_pb2.ResetRequest(
            deck_path_a=deck_path_a,
            deck_path_b=deck_path_b,
            agent_player_index=agent_player_index,
            seed=seed,
            dual_rl=dual_rl,
            opponent_model_path=opponent_model_path,
        )
        return self.stub.Reset(request)

    def step(self, action_index: int):
        """Take an action and return the next observation."""
        request = forge_rl_pb2.StepRequest(action_index=action_index)
        return self.stub.Step(request)

    def close(self):
        """Close the gRPC channel."""
        self.channel.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
