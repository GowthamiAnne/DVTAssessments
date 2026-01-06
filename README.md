✈️ **Loyalty Points Quote Service**

A Vert.x microservice that calculates airline loyalty points with real-time FX conversion, tier bonuses, and promotional offers.

🏗 **Architecture**

Clean Architecture with clear separation of concerns:

HTTP Layer (Handlers / Verticles)
        ↓
Service Layer (Business Logic)
        ↓
External Clients (FX, Promo)


📦 **Tech Stack**

Java 17

Vert.x

Maven

WireMock (external service stubs)

REST Assured (HTTP testing)

Micrometer + Prometheus

JUnit 5

📡 **API**
Calculate Points

POST /v1/points/quote

Request

{
  "fareAmount": 1234.50,
  "currency": "USD",
  "cabinClass": "ECONOMY",
  "customerTier": "SILVER",
  "promoCode": "SUMMER25"
}


Response

{
  "basePoints": 1234,
  "tierBonus": 185,
  "promoBonus": 308,
  "totalPoints": 1727,
  "effectiveFxRate": 1.0,
  "warnings": []
}

❤️ **Health & Metrics**
GET /health – Service health

GET /metrics – Prometheus metrics

🧪 **Testing**

Full component & integration automation testing

Real HTTP server + WireMock stubs

Coverage enforced via JaCoCo
COPY target/points-quote-service-*.jar /app.jar
EXPOSE 8080 9
