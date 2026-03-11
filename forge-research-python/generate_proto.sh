#!/bin/bash
# Generate Python protobuf stubs from forge_rl.proto
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROTO_DIR="$SCRIPT_DIR/../forge-research/src/main/proto"
OUT_DIR="$SCRIPT_DIR/forge_rl/proto"

python3 -m grpc_tools.protoc \
    --proto_path="$PROTO_DIR" \
    --python_out="$OUT_DIR" \
    --grpc_python_out="$OUT_DIR" \
    "$PROTO_DIR/forge_rl.proto"

# Fix grpc stub import to use package-relative path
sed -i '' 's/^import forge_rl_pb2 as/from forge_rl.proto import forge_rl_pb2 as/' "$OUT_DIR/forge_rl_pb2_grpc.py"

echo "Python protobuf stubs generated in $OUT_DIR"
