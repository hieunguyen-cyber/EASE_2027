"""Backend selection and local Ollama model preparation."""

from __future__ import annotations

import json
import shutil
import subprocess
import urllib.error
import urllib.request


OLLAMA_MODEL = "llama3.3:70b"
OLLAMA_URL = "http://127.0.0.1:11434"


def configure_backend() -> tuple[str, str]:
    """Return the fixed local Ollama model and endpoint used by Mutahunter."""
    ensure_ollama_model(OLLAMA_URL, OLLAMA_MODEL)
    return f"ollama/{OLLAMA_MODEL}", OLLAMA_URL


def ensure_ollama_model(server_url: str, model: str) -> None:
    """Verify Ollama is reachable and pull ``model`` when it is not installed."""
    model = model.removeprefix("ollama/")
    try:
        with urllib.request.urlopen(f"{server_url}/api/tags", timeout=10) as response:
            payload = json.load(response)
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        raise RuntimeError(
            f"Không kết nối được Ollama tại {server_url}. Hãy khởi động Ollama trước."
        ) from error

    available = {item.get("name") for item in payload.get("models", [])}
    if model in available:
        return
    ollama = shutil.which("ollama")
    if not ollama:
        raise RuntimeError(f"Model Ollama '{model}' chưa có và không tìm thấy lệnh ollama để pull")
    result = subprocess.run([ollama, "pull", model], check=False)
    if result.returncode != 0:
        raise RuntimeError(f"Ollama pull thất bại cho model '{model}' (exit {result.returncode})")
