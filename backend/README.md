# Svara_X Local Testing Backend

A lightweight Python FastAPI backend for the Svara_X SIH prototype.

### Setup and Running on your laptop:

1. Create a virtual environment and install requirements:
```bash
cd backend
python -m venv venv
source venv/bin/activate   # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

2. Run the server:
```bash
python server.py
# Server runs on http://0.0.0.0:8000
```

3. Test with curl:
```bash
curl -X POST "http://localhost:8000/analyze/transcript" \
     -H "Content-Type: application/json" \
     -d '{"transcript": "Hello, I am calling from your bank. Your account will be blocked. Share your OTP immediately."}'
```
