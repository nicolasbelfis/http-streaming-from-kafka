[![Java CI with Maven](https://github.com/nicolasbelfis/http-streaming-from-kafka/actions/workflows/maven.yml/badge.svg)](https://github.com/nicolasbelfis/http-streaming-from-kafka/actions/workflows/maven.yml)
# Streaming

## simple streaming service

### reactor Flux
    
subscribe `get localhost:8080/flux/stream`

post message : `post localhost:8080/flux/message {"message": "your message"}`

### kotlin Flow

subscribe `get localhost:8080/flow/stream`

post message : `post localhost:8080/flow/message {"message": "your message"}`


## stream twitter directly

### pre-requisites
- have a twitter developer account and replace the bearer token value in file `stream-api/src/main/resources/application.yaml`
- have maven and java installed

run `stream-api/src/main/kotlin/simple/Application.kt` with spring profile `direct-twitter`

start streaming event from twitter `curl --location --request GET 'http://localhost:8080/stream/tweets/sse'`

### simple twitter kafka streaming

start kafka cluster : `docker-compose up`

start TwitterWorkerApplication.kt