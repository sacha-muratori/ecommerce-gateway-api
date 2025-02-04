API Aggregation Service

Overview

This service acts as an API aggregation layer, consolidating requests to multiple backend services into a single request. It improves efficiency by batching and throttling API calls while ensuring timely responses. The implementation uses Spring WebFlux for reactive and non-blocking request handling.

Design & Development

Uses Spring WebFlux for non-blocking API calls.

Implements concurrency for efficient request processing.

Requests are queued and sent in batches (max 5 requests per API or 5s timeout).

Ensures response delivery within 10 seconds for the 99th percentile.

Includes automated tests and follows clean coding practices.

API Contract

Request Example:

GET /aggregation?pricing=NL,CN&track=109347263,123456891&shipments=109347263,123456891

Response Example:

{
"pricing": {
"NL": 14.24,
"CN": 20.50
},
"track": {
"109347263": null,
"123456891": "COLLECTING"
},
"shipments": {
"109347263": ["box", "box", "pallet"],
"123456891": null
}
}

Features & Implementation Steps

1. Single Network Call

Consolidates API requests for Pricing, Track, and Shipments into a single request. The service forwards each request to its respective API and returns a unified response once all results are available.

2. Throttling and Batching

To optimize performance and prevent API overload:

Requests are queued per API.

A batched request is sent when the queue reaches 5 items.

Responses are returned only after all relevant API calls complete.

3. Periodic Request Execution

If the queue for an API does not reach the batch threshold, pending requests are still processed within 5 seconds to ensure timely responses. The timer resets when a batch is sent before the timeout.

Installation & Setup

Prerequisites

Java (latest stable version)

Docker

Maven or Gradle

Docker Compose (Install Guide)

Running the Aggregation API

Step 1: Pull Backend Services Image

docker pull xyzassessment/backend-services

Step 2: Build the Aggregation API Docker Image

docker build -t aggregation-api .

Step 3: Start the Application with Docker Compose

docker-compose up

Now the Aggregation API is available on localhost:8080.

Notes

The Dockerfile and docker-compose.yml can be adjusted for CI/CD integration.

Ensure all services are running before starting the application.

The backend service URLs can be configured via environment variables.

