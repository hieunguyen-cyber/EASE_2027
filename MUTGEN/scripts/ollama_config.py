"""Shared Ollama configuration for the standalone MUTGEN helper scripts."""

import os


OLLAMA_SERVER_URL = os.getenv(
    "OLLAMA_SERVER_URL", "http://10.148.182.141:11434"
).rstrip("/")
OLLAMA_OPENAI_BASE_URL = f"{OLLAMA_SERVER_URL}/v1"
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5-coder:32b")
OLLAMA_API_KEY = os.getenv("OLLAMA_API_KEY", "ollama")
