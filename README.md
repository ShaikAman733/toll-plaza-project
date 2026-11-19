# Toll Plaza Finder

Toll Plaza Finder is a Spring Boot REST API that helps you find toll plazas along a driving route between two Indian pincodes.

Simply provide a source and destination pincode, and the API calculates the driving route, identifies toll plazas along the way, and returns their names, coordinates, and distance from the starting point.

## Tech Stack

- Java 17 and Spring Boot 3.5
- Maven
- OpenRouteService (ORS) for route calculation
- OpenStreetMap Nominatim for pincode geocoding
- H2 for local development and PostgreSQL for production
- Caffeine for caching API responses
- Swagger UI for API documentation

## API Endpoint

**`POST /api/v1/toll-plazas`**

Send the source and destination pincodes in the request body:

```json
{
  "sourcePincode": "560064",
  "destinationPincode": "411045"
}
```

### Sample Response

The API returns the total route distance along with the toll plazas found on the route.

```json
{
  "route": {
    "sourcePincode": "560064",
    "destinationPincode": "411045",
    "distanceInKm": 842.31
  },
  "tollPlazas": [
    {
      "name": "Kaggalipura Toll Plaza",
      "latitude": 12.7425,
      "longitude": 77.4528,
      "distanceFromSource": 31.52
    }
  ]
}
```

*The toll plaza details above are illustrative and not actual API results.*

If a route has no toll plazas, the API still returns a successful response with an empty `tollPlazas` array.

## Error Handling

The API handles common errors and returns appropriate HTTP status codes.

| Scenario | Status Code | Response |
|---|---:|---|
| Invalid or unrecognized pincode | 400 | Invalid source or destination pincode |
| Source and destination are the same | 400 | Source and destination pincodes cannot be the same |
| Missing or invalid request body | 400 | Request body must contain valid JSON and both pincodes |
| No drivable route found | 422 | No drivable route found between the given pincodes |
| API rate limit exceeded | 429 | Rate limit reached, please try again later |
| External API failure | 502 | External service failed or rejected the API key |
| External API timeout | 504 | Request timed out, please try again |
| External service unavailable | 503 | Service is currently unreachable |
| Unexpected server error | 500 | Something went wrong, please try again later |

## Getting Started

### Prerequisites

Before running the project, make sure you have:

- JDK 17 or later
- An OpenRouteService API key

You can get a free API key from [OpenRouteService](https://openrouteservice.org).

### Configuration

Create a `.env` file in the project root and add your API key:

```env
ORS_API_KEY=your-key
```

You can also set it directly in your terminal.

**Windows PowerShell:**

```powershell
$env:ORS_API_KEY="your-key"
```

**Run the application:**

```powershell
mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

Once the application starts, Swagger UI is available at:

`http://localhost:8080/swagger-ui.html`

### Test the API

You can test the endpoint using Postman or cURL.

```bash
curl -X POST http://localhost:8080/api/v1/toll-plazas \
  -H "Content-Type: application/json" \
  -d '{"sourcePincode":"560064","destinationPincode":"411045"}'
```

The application uses an in-memory H2 database locally and automatically loads toll plaza data from `toll_plaza_india.csv` during startup. No additional database setup is required.

## Running with Docker

Build the Docker image:

```bash
docker build -t toll-plaza-finder .
```

Run the container:

```bash
docker run -p 8080:8080 -e ORS_API_KEY=your-key toll-plaza-finder
```

## Running Tests

Run the test suite using Maven:

```bash
mvnw.cmd test
```

The tests don't depend on real external API calls. OpenRouteService and geocoding requests are mocked, allowing the tests to run without an API key or external network access.

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `ORS_API_KEY` | Yes | API key used for route calculation |
| `PORT` | No | Application port (defaults to 8080) |
| `SPRING_PROFILES_ACTIVE` | For deployment | Set to `prod` to enable PostgreSQL |
| `DB_URL` | Production | PostgreSQL connection URL |
| `DB_USERNAME` | Production | Database username |
| `DB_PASSWORD` | Production | Database password |
| `TOLL_MATCH_TOLERANCEMETERS` | No | Maximum distance from the route to match a toll plaza (default: 300 m) |
| `TOLL_MATCH_MERGEDISTANCEMETERS` | No | Distance used to merge duplicate toll plaza entries (default: 1000 m) |
| `GEOCODING_PROVIDER` | No | Geocoding provider: `nominatim` or `ors` (default: `nominatim`) |

The ORS routing and geocoding base URLs are configured in `application.yml`. See `.env.example` for the available configuration placeholders.

## Deployment

The application can be deployed using Render and Neon PostgreSQL.

### Deployment Steps

1. Create a PostgreSQL database on Neon.
2. Create a Web Service on Render using this GitHub repository and select Docker as the runtime.
3. Add the required environment variables to Render: `ORS_API_KEY`, `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
4. Deploy the application.

Once deployed, Swagger UI will be available at:

`https://your-service.onrender.com/swagger-ui.html`

The root URL (`/`) redirects to Swagger UI, and the `/health` endpoint returns:

```json
{
  "status": "UP"
}
```

This health endpoint can be used to check whether the service is running.

Render's free tier may spin down the application after around 15 minutes of inactivity. The first request after the service wakes up may take 30–60 seconds because the application needs to start and reload its toll plaza data. The cache is also empty after a restart, so the first few requests may take longer.

## How It Works

Here's what happens when a request is received:

1. The API validates both pincodes to make sure they're valid and different.
2. The geocoding service converts the pincodes into geographical coordinates. Coordinates are cached to avoid repeated lookups.
3. OpenRouteService calculates the driving route and returns its geometry and total distance.
4. The route's bounding box is used to find nearby toll plazas in the database without checking every location.
5. The toll matching logic identifies plazas close to the route, calculates their distance along the route, sorts them in order, and merges duplicate entries.
6. The final result is cached for future requests using the source and destination pincodes.

## Design Decisions

- **External services are replaceable:** Geocoding and routing are handled through interfaces, making it easier to change providers and test the application without relying on live APIs.
- **Coordinates identify toll plazas:** Toll plaza names aren't always reliable because the dataset contains duplicate names, missing names, and multiple entries for the same location. Latitude and longitude are used to identify unique locations.
- **Distance follows the route:** The distance from the source is calculated along the driving route, not as a straight-line distance.
- **Duplicate plazas are merged:** Entries representing the same toll plaza, including those on different carriageways, are merged when they have the same name and are within the configured distance.
- **Separate caching for geocoding and routes:** Pincode coordinates and route results are cached independently. Routes are directional, so A → B and B → A are stored separately.
- **Consistent error responses:** A centralized exception handler ensures errors follow the same response format throughout the API.
- **Flexible geocoding:** Nominatim is the default provider because it doesn't require an API key. ORS geocoding is also supported, although Indian postal-code coverage hasn't been fully verified.

Both caches expire after 24 hours and can store up to 1,000 entries.

## Known Limitations

- **Dataset inconsistencies:** Some toll plaza locations have multiple names. Currently, only the first name is retained, while additional names are logged as warnings.
- **Matching accuracy:** Toll detection depends on the accuracy of the coordinates in the CSV file. Plazas located too far from the road may be missed, while plazas near parallel roads may be incorrectly included.
- **Incomplete coverage:** The current dataset doesn't include toll plazas for the last approximately 330 km of the Bengaluru (560064) to Pune (411045) route. As a result, the API returns 11 toll plazas, all within the first 520 km. Increasing the matching tolerance from 300 m to 1,500 m only added one plaza, indicating that the missing locations are primarily a data coverage issue.
- **External API limits:** OpenRouteService and Nominatim have request limits. ORS allows approximately 2,000 routing requests and 1,000 geocoding requests per day, while public Nominatim usage is limited to one request per second. Caching helps reduce unnecessary requests.
- **Cache persistence:** The caches are stored in memory and are cleared whenever the application restarts.

## Further Documentation

For a more detailed explanation of the project and its implementation, see [`docs/EXPLAINER.md`](docs/EXPLAINER.md).
