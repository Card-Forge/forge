# Container image for the Forge LLM sidecar.
# The local LLM (Ollama) and Scryfall are reached over the network at runtime;
# this image only contains the FastAPI service.
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# Install the package and its dependencies. The committed metagame data and
# curated archetype files are bundled as package data (see pyproject.toml).
COPY pyproject.toml ./
COPY app ./app
RUN pip install --no-cache-dir .

# Run as an unprivileged user.
RUN useradd --create-home --uid 1000 sidecar
USER sidecar

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" \
    || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
