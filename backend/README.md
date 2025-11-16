# AskNow Backend API

FastAPI backend for the AskNow real-time Q&A tutoring system.

## Features

- User authentication (JWT tokens)
- RESTful API endpoints for questions and messages
- WebSocket for real-time communication
- Image upload support
- SQLite database with SQLAlchemy ORM

## Installation

1. Install Python 3.8 or higher

2. Install dependencies:
```bash
pip install -r requirements.txt
```

Or using virtual environment:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

## Running the Server

```bash
python main.py
```

Or using uvicorn directly:
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

The server will start at `http://0.0.0.0:8000`

## API Documentation

Once the server is running, visit:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## API Endpoints

### Authentication
- `POST /api/register` - Register new user
- `POST /api/login` - Login user

### Questions
- `GET /api/questions` - Get questions (with optional status filter)
- `POST /api/questions` - Create new question
- `POST /api/questions/accept` - Accept a question (tutor)
- `POST /api/questions/close` - Close a question

### Messages
- `GET /api/messages?questionId={id}` - Get messages for a question
- `POST /api/messages` - Send a message

### Upload
- `POST /api/upload` - Upload image
- `GET /uploads/{user_id}/{filename}` - Get uploaded image

### WebSocket
- `WS /ws/{user_id}` - WebSocket connection for real-time updates

## WebSocket Message Format

```json
{
  "type": "NEW_QUESTION" | "CHAT_MESSAGE" | "QUESTION_UPDATED" | "ACK",
  "data": {
    "questionId": 123,
    "content": "Message content",
    "senderId": 456,
    "messageType": "text",
    "imagePath": "/uploads/..."
  },
  "timestamp": "1234567890",
  "messageId": "uuid-string"
}
```

## Configuration

To change the database location or other settings, modify:
- `database.py` - DATABASE_URL
- `auth.py` - SECRET_KEY (important for production!)

## Production Deployment

For production, consider:
1. Change SECRET_KEY in `auth.py`
2. Use PostgreSQL instead of SQLite
3. Use a proper WSGI server (gunicorn)
4. Set up HTTPS/TLS
5. Configure proper CORS origins
6. Use environment variables for sensitive data

## Testing with Android App

When testing with the Android emulator, use:
- `http://10.0.2.2:8000` as the base URL in the Android app

When testing with a physical device:
- Find your computer's local IP address
- Use `http://{YOUR_IP}:8000` as the base URL
- Make sure both devices are on the same network

