# Local Ollama chat

Run `./Start-LocalAi.ps1` with Docker Desktop running. The script preserves database volumes. Use `-Model <name>` to select another Ollama model.

Always merge compose.yaml, compose.ai.yaml, compose.local.yaml, and compose.ollama.yaml in that order when recreating the stack. Using only compose.yaml removes the local AI timeout configuration.

The default is Ollama 0.13.5 with qwen2.5:1.5b, a compact CPU model. Model files persist in the ollama-models volume. The host API is bound to 127.0.0.1:11434. Inference uses the private Docker network.

Timeouts: inference 90 seconds, Java AI service 95 seconds, gateway 100 seconds, frontend 120 seconds. Cold model loading can be slow. Model answers are not guaranteed to be correct.

Java authorizes tool use and retrieves current business data. Private results are not sent to Ollama. The model cannot bypass permissions or automatically confirm changes.

Check free disk space before pulling images and models. For failures, inspect logs for api-gateway, ai-service, ai-inference and ollama. Never delete database volumes to repair startup failures.
