[![Java CI with Maven](https://github.com/nicolasbelfis/http-streaming-from-kafka/actions/workflows/maven.yml/badge.svg)](https://github.com/nicolasbelfis/http-streaming-from-kafka/actions/workflows/maven.yml)
# Streaming twitter with kafka streams and reactor

prerequisites
- a twitter developer account and replace the bearer token value in file `stream-api/src/main/resources/application.yaml`
- docker, npm, maven and java installed

## backend

### stream twitter directly
run `stream-api/src/main/kotlin/simple/Application.kt` with spring profile `direct-twitter`

start streaming event from twitter `curl --location --request GET 'http://localhost:8080/stream/sseTweets'`

### simple twitter kafka streaming

start kafka cluster : `docker-compose up`

start TwitterWorkerApplication.kt


## start frontend
cd into `cd frontend-app/ui` , run `npm run start`

the app displays only the last 4 tweets received
