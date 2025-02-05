# E-Commerce Gateway API MicroService for internal APIs

## Overview

This service acts as an API collection layer, consolidating requests to multiple backend services into a single request.  
It improves efficiency by batching and throttling API calls while ensuring timely responses.  

## Design & Development

- Uses **Spring WebFlux** for reactive and non-blocking API request handling
- Implements concurrency for efficient request processing.
- Requests are queued and sent in batches: max 5 requests per single API or 5s timeout.
- Ensures response delivery within 10 seconds for the 99th percentile.
- Includes automated tests and follows clean coding practices.

## Gateway API Documentation

The **Gateway API** consolidates multiple service requests into a single query.  
It allows fetching data from different internal APIs by passing their **unique 9-digit IDs**.

- The API parameters in the Gateway request are: `customer`, `product`, `inventory`, `order`, `shipment`.
- The API parameters in the Gateway request are **optional** and can be missing.
- All internal APIs as well as the Gateway API return a **JSON object**.
- Each internal API uses its respective unique ID: `customerId`, `productId`, `inventoryId`, `orderId`, `shipmentId`.
- When multiple IDs are passed to the request, the response will be **an array of arrays**.
- If an internal API fails (error or timeout), its field will still be included in the response, but the value will be **null**.

## Supported Internal API Requests
The following five request types can be aggregated in any order:

### **1. Customer**
Retrieves customer information.

**Response Format:**
```json
["customerName", "customerSurname", "customerAddress"]
```

### **2. Product**
Retrieves product details, including its inventory reference.

**Response Format:**
```json
["productName", "productPrice", "inventoryId"]
```

### **3. Inventory**
Checks product availability in stock.

**Response Format:**
```json
"IN STOCK" or "NOT AVAILABLE"
```

### **4. Order**
Retrieves order details, linking it to customer, product, and shipment information.

**Response Format:**
```json
["orderStatus", "customerId", "productId", "shipmentId"]

where orderStatus is "ACTIVE" or "COMPLETED"

```

### **5. Shipment**
Retrieves shipment details, including cost, courier, and status.

**Response Format:**
```json
["shipmentTime", "shipmentPrice", "shipmentCourier", "shipmentStatus"]

where shipmentStatus is "PENDING" or "IN TRANSIT" or "SHIPPED"

```

## Gateway API Request Handling

The Gateway API allows mixing the five internal request types in any order as query parameters.
For clarity, the examples below illustrate responses even if these batching and throttling condition are not met.
More on this in next section.

### Request Examples:

```sh
GET /gateway?product=100000000,123456789&inventory=222222220,200000001,234567890

GET /gateway?customer=3660234050,377889999,322229999&order=400000000,411011333&shipment=555555555,588888221

GET /gateway?customer=3660234050,377889999&order=400000000,411011333&shipment=555555555,588888221&product=100000000,123456789,123456700&inventory=222222220,200000001
```
(Usage of different numerical first digits in the ids is for simplicity purposes)


### Response Examples:
```json
{
  "products": {
    "100000000": ["Laptop 16-inch 64GB RAM", 4000.00, 222222220], 
    "123456789": ["Kitchen-Mix pots, pans and knives", 799.99, 200000001]
  },
  "inventory": {
    "222222220": "IN STOCK",
    "200000001": "NOT AVAILABLE",
    "234567890": null
  }
}
```

or
```json
{
  "customer": {
    "3660234050": ["Mark", "Fabbri", "Seychelles"],
    "377889999": ["Hugo", "Erikson", "Luxembourg"],
    "322229999": ["Elisabeth", "Cierra", "Mexico"]
  },
  "order": {
    "400000000": ["ACTIVE", 3660234050, 100000000, 555555555 ],
    "411011333":  ["COMPLETED", 322229999, 123456789, 588888221]
  },
  "shipments": {
    "555555555": ["3 Days", 15.50, "DHL", "IN TRANSIT"],
    "588888221": ["2 weeks", 40.99, "UPS", "NOT STARTED YET"]
  }
}
```

or a mix of them
```json
{
  "customer": {
    "3660234050": ["Mark", "Fabbri", "Seychelles"],
    "377889999": null,
    "322229999": ["Elisabeth", "Cierra", "Mexico"]
  },
  "order": {
    "400000000": null,
    "411011333":  ["COMPLETED", 322229999, 123456789, 588888221]
  },
  "shipments": {
    "555555555": ["3 Days", 15.50, "DHL", "IN TRANSIT"],
    "588888221": ["2 weeks", 40.99, "UPS", "PENDING"]
  },
  "products": {
    "100000000": null,
    "123456789": null
  },
  "inventory": {
    "222222220": "IN STOCK",
    "200000001": "NOT AVAILABLE",
    "234567890": null
  }
}
```

## Features & Implementation Steps

### 1. Single Network Call

Consolidates API requests for Pricing, Track, and Shipments into a single request.  
The service forwards each request to its respective API and returns a unified response once all results are available.
BUT:

### 2. Throttling and Batching

To optimize performance and prevent API overload:

- Requests are queued per API.
- A batched request is sent when the queue reaches 5 items.
- Responses are returned only after all relevant API calls complete.

### 3. Periodic Request Execution

If the queue for an API does not reach the batch threshold, pending requests are still processed within 5 seconds to ensure timely responses.  
The timer resets when a batch is sent before the timeout.

---

## Installation & Setup

### Prerequisites

- Java (latest stable version)
- Maven or Gradle
- Docker & Docker Compose (Install Guide)

### Running the Aggregation API

Step 1: Pull Backend Services Image

```bash
docker pull xyzassessment/backend-services
```

Step 2: Build the Aggregation API Docker Image

```bash
docker build -t aggregation-api .
```

Step 3: Start the Application with Docker Compose

```bash
docker-compose up
```

Now the Gateway API is available on localhost:8080.

## Notes


??? 
- The Dockerfile and docker-compose.yml can be adjusted for CI/CD integration.
- Ensure all services are running before starting the application.
- The backend service URLs can be configured via environment variables.