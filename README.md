# Streaming

## simple streaming service

### reactor Flux
    
subscribe `get localhost:8080/flux/stream`

post message : `post localhost:8080/flux/message {"message": "your message"}`

### kotlin Flow

subscribe `get localhost:8080/flow/stream`

post message : `post localhost:8080/flow/message {"message": "your message"}`
