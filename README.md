# Toll Plaza Finder

A Spring Boot REST API. Give it a source and a destination Indian pincode and it returns the toll plazas on the
driving route between them: name, latitude, longitude and the distance from the source along the route.

- Java 17, Spring Boot 3.5, Maven
- Routing from [OpenRouteService](https://openrouteservice.org) (ORS), geocoding of pincodes from OpenStreetMap Nominatim (or ORS, see below)
- Toll plazas loaded from a CSV into a database (H2 locally, Postgres when deployed)
- Responses cached with Caffeine
- Swagger UI at `/swagger-ui.html`

## The endpoint

`POST /api/v1/toll-plazas`

```json
{ "sourcePincode": "560064", "destinationPincode": "411045" }
```

Success (distances are in km):

```json
{
  "route": { "sourcePincode": "560064", "destinationPincode": "411045", "distanceInKm": 842.31 },
  "tollPlazas": [
    { "name": "Kaggalipura Toll Plaza", "latitude": 12.7425, "longitude": 77.4528, "distanceFromSource": 31.52 }
  ]
}
```

(The plaza values above are illustrative.) A route with no plazas is a normal `200` with `"tollPlazas": []`.

| Case | Status | Body |
|---|---|---|
| Pincode is not 6 digits, or cannot be found | 400 | `{"error": "Invalid source or destination pincode"}` |
| Same source and destination | 400 | `{"error": "Source and destination pincodes cannot be the same"}` |
| Malformed or missing JSON body | 400 | `{"error": "Request body must be JSON with sourcePincode and destinationPincode"}` |
| No drivable route | 422 | `{"error": "No drivable route found between the given pincodes"}` |
| ORS / geocoder rate limit reached | 429 | `{"error": "... rate limit reached, please try again later"}` |
| ORS / geocoder failed or rejected the key | 502 | `{"error": "..."}` |
| ORS / geocoder timed out | 504 | `{"error": "... timed out, please try again"}` |
| ORS / geocoder unreachable | 503 | `{"error": "... is unreachable, please try again later"}` |
| Anything unexpected | 500 | `{"error": "Something went wrong, please try again later"}` (details only in the server log) |

## Run locally

You need a JDK 17+ and an OpenRouteService API key (free at openrouteservice.org).

```bash
export ORS_API_KEY=your-key            # PowerShell: $env:ORS_API_KEY="your-key"
./mvnw spring-boot:run                 # Windows: mvnw.cmd spring-boot:run
```

Then open http://localhost:8080/swagger-ui.html or:

```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{"sourcePincode":"560064","destinationPincode":"411045"}'
```

Locally the app uses an in-memory H2 database and loads `src/main/resources/toll_plaza_india.csv` at startup.
No database setup is needed. In Postman: POST, body type raw / JSON, same payload.

## Run with Docker

```bash
docker build -t toll-plaza-finder .
docker run -p 8080:8080 -e ORS_API_KEY=your-key toll-plaza-finder
```

## Run the tests

```bash
./mvnw test
```

No test calls a real external API: ORS and the geocoder are mocked (`MockRestServiceServer`, Mockito, `@MockitoBean`).

## Configuration

| Variable | Needed | Purpose |
|---|---|---|
| `ORS_API_KEY` | yes (for routing) | OpenRouteService key, sent in the `Authorization` header. Never logged or committed. |
| `PORT` | no (default 8080) | HTTP port. Render sets this automatically. |
| `SPRING_PROFILES_ACTIVE` | for deployment | Set to `prod` to use Postgres. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | with `prod` | JDBC URL and credentials, e.g. `jdbc:postgresql://<neon-host>/<db>?sslmode=require` |
| `TOLL_MATCH_TOLERANCEMETERS` | no (default 300) | How close (metres) a plaza must be to the route line. |
| `TOLL_MATCH_MERGEDISTANCEMETERS` | no (default 1000) | Same-name plazas closer than this along the route count as one. |
| `GEOCODING_PROVIDER` | no (default `nominatim`) | `nominatim` or `ors` |

The ORS base URLs (`ors.directions-base-url`, `ors.geocoding-base-url`) are properties in `application.yml`, no trailing slash.
See `.env.example` for placeholders.

## Deploying (Render + Neon)

1. Create a Neon Postgres database and copy its host, database name, user and password.
2. On Render create a **Web Service** from this GitHub repo, runtime **Docker**.
3. Set the environment variables: `ORS_API_KEY`, `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
4. Deploy. Swagger is at `https://<your-service>.onrender.com/swagger-ui.html`.

The Render free tier **sleeps after about 15 minutes idle**, so the first request after a pause can take 30-60 seconds
while the app starts and reloads the CSV (a quick upsert). The in-memory cache is also empty after a wake-up.

## How it works

1. The request is validated (6 digits, not starting with 0). Same source and destination is rejected.
2. Each pincode is turned into coordinates by a `GeocodingClient` (cached per pincode).
3. A `RoutingClient` (ORS) returns the route geometry and its total distance.
4. A bounding box around the route narrows the candidate plazas with one database query.
5. `TollMatcher` keeps plazas within the tolerance of the route line, computes each one's distance along the route,
   sorts by it and merges duplicates.
6. The final result is cached per `source-destination` pair.

More detail, in plain language, is in [docs/EXPLAINER.md](docs/EXPLAINER.md).

## Design decisions

- **Interfaces for external services** (`GeocodingClient`, `RoutingClient`): the provider can be swapped and the
  business logic is tested with mocks.
- **Plaza location is the key, not the name.** The CSV has repeated names at different places, missing names and
  many exact duplicate rows. The table has a unique constraint on (latitude, longitude) and the loader upserts, so
  it is safe to run on every start.
- **Along-route distance.** `distanceFromSource` is measured along the route to the nearest point, not in a straight line.
- **Merging.** The data often lists both carriageways of a plaza. Same name and within 1 km along the route are reported once.
- **Two caches.** Pincode to coordinate (`pincodes`) and final result per pair (`tollRoutes`). Directions are not
  symmetric, so A to B and B to A are cached separately. Entries expire after 24 h, max 1000 each.
- **One error handler** (`@RestControllerAdvice`) so every error has the same `{"error": ...}` shape.
- **Geocoding provider.** Nominatim resolves Indian pincodes reliably and needs no key, so it is the default. An ORS
  (Pelias) implementation exists behind `GEOCODING_PROVIDER=ors`, but its postal-code coverage for India was not verified.

## Known limitations

- **Data:** the CSV has 20 locations that carry two or three different names. Only the first name in the file is kept
  (the others are logged as warnings at startup).
- **Accuracy:** matching depends on plaza coordinates in the CSV being close to the road. If real tolls are missed, raise the
  tolerance; if unrelated plazas on parallel roads appear, lower it.
- **Coverage:** the CSV has no plazas near the last ~330 km of the Bengaluru (560064) to Pune (411045) route, so the
  API returns 11 plazas (all in the first 520 km) even with a larger tolerance. Raising the tolerance from 300 m to
  1500 m only added one plaza, so the gap is in the data, not the matching.
- **Free-tier limits:** ORS allows about 2000 directions and 1000 geocoding requests per day, and the public Nominatim
  server asks for at most one request per second. The caches keep repeat calls from using that quota.
- The cache is in memory, per instance, and is lost on restart.
