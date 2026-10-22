# Ai-boy AI server

Render Web Service: Root Directory `server`, Build Command `npm install`, Start Command `npm start`.

Set secret environment variables `OPENAI_API_KEY` and `AI_BOY_ACCESS_TOKEN` (long random string). Optional `OPENAI_MODEL`.

Health: GET /health. Chat: POST /chat with JSON `{"message":"안녕하세요"}` and `Authorization: Bearer <AI_BOY_ACCESS_TOKEN>`. Response: `{"reply":"..."}`.

Never commit keys to GitHub. Free Render may sleep when idle; app timeout may need adjustment.
