from setuptools import setup, find_packages

setup(
    name="forge-rl",
    version="0.1.0",
    description="Gymnasium environment for MTG via Forge game engine",
    packages=find_packages(),
    python_requires=">=3.10",
    install_requires=[
        "gymnasium>=0.29.0",
        "numpy>=1.24.0",
        "grpcio>=1.62.0",
        "grpcio-tools>=1.62.0",
        "protobuf>=4.25.0",
    ],
)
