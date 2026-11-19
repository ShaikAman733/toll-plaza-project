# Explainer (plain language)

## 1. Request flow, end to end

```
POST /api/v1/toll-plazas  {"sourcePincode":"560064","destinationPincode":"411045"}
  -> TollPlazaController      validates the body (6 digits). Bad input -> 400.
  -> TollPlazaService         [cache "tollRoutes": hit -> return immediately]
       same pincodes?         -> 400 "cannot be the same"
       PincodeLocator x2      [cache "pincodes"] -> GeocodingClient (Nominatim) -> lat/lon
       RoutingClient (ORS)    -> route line (thousands of points) + total distance
       BoundingBox            -> rectangle around the route
       TollPlazaRepository    -> plazas inside the rectangle (one SQL query)
       TollMatcher            -> keep those near the line, add distance along route, sort, merge
  -> TollResponse (JSON)
Any exception -> GlobalExceptionHandler -> {"error": "..."} with the right status.
```

## 2. Packages and classes

| Package | Class | Job |
|---|---|---|
| controller | `TollPlazaController` | The single endpoint, plus the Swagger descriptions. No logic. |
| service | `TollPlazaService` | Orchestrates the steps above. Cached. |
| service | `PincodeLocator` | Pincode to coordinate, cached. A separate bean because Spring caching only works across beans, not on a method calling its own class. |
| service | `TollMatcher` | The core: which plazas are on the route and where. Pure code, no I/O, easy to unit test. |
| service | `BoundingBox`, `GeoMath`, `TollMatch` | Helpers: the rectangle, distance maths, one matched plaza. |
| service | `TollPlazaCsvParser`, `TollPlazaImporter` | Read the CSV; upsert rows into the database. |
| client | `GeocodingClient`, `RoutingClient` | Interfaces the service depends on. |
| client | `NominatimGeocodingClient`, `OrsGeocodingClient`, `OrsRoutingClient` | Real HTTP implementations. `ExternalCalls` turns HTTP failures (429, 5xx, timeout) into one `ExternalServiceException`. |
| repository | `TollPlazaRepository` | Spring Data JPA, plus the bounding-box query. |
| model | `TollPlaza` (entity), `Coordinate`, `RouteGeometry`, `TollRequest`, `TollResponse`, `ErrorResponse` | Data. Records for anything immutable. |
| exception | `GlobalExceptionHandler` and 4 exception classes | One place that maps errors to HTTP responses. |
| config | `CacheConfig`, `HttpClientConfig`, `*Properties`, `TollDataLoader`, `OpenApiConfig` | Wiring: caching, HTTP timeouts, typed settings, startup CSV load, Swagger info. |

## 3. Design patterns, and why

- **Strategy (via interfaces):** `GeocodingClient` and `RoutingClient` have interchangeable implementations. Nominatim vs ORS geocoding is chosen by one property. Tests use mocks.
- **Facade:** `TollPlazaService` gives the controller one simple call and hides geocoding, routing, database and matching behind it.
- **Repository:** Spring Data hides SQL behind an interface.
- **Cache-aside (Spring Cache):** the annotation checks the cache first, calls the method only on a miss.
- **Centralised exception handling:** one `@RestControllerAdvice` instead of try/catch in every method.
- **Upsert on a natural key:** insert if the location is new, update if it exists.

Not used on purpose: factories, builders and abstract base classes, because nothing needed them.

## 4. How the matching works

1. **Bounding box.** Take the smallest rectangle around all route points and grow it by the tolerance. One database query returns only plazas inside it, so we never compare against all 1,500. (Longitude margin is widened because a degree of longitude is shorter further from the equator.)
2. **Tolerance (default 300 m).** A plaza counts as "on the route" if its distance to the route line is at most this. The route is a chain of short segments; for each plaza we find the closest point on each segment (a flat-earth projection, accurate to well under a metre at this scale) and keep the smallest distance. A cheap rectangle check skips segments that are obviously too far.
3. **Along-route distance.** We add up the length of the segments before the closest point, plus the part of the final segment. So a plaza after a bend is reported by the road distance, not "as the crow flies".
4. **Sort** by that distance.
5. **Merge.** The data often lists the two directions of one plaza. If two matches have the same name (ignoring case and spaces) and are within 1 km of each other along the route, only the first is kept. Different names are never merged.

## 5. Cache keys

- `pincodes`: key = the pincode. Value = coordinate. Reused by every route that touches that pincode.
- `tollRoutes`: key = `"<source>-<destination>"`. `560064-411045` and `411045-560064` are separate because the road route and the distances differ.
- Caffeine, max 1000 entries, expire 24 h after write. Failed lookups throw, and Spring does not cache exceptions, so an invalid pincode or a provider outage is retried next time.

## 6. The CSV and the database

- 2,385 rows, 849 exact duplicates, 1,513 unique locations.
- The name cannot be the key (the same name appears in different places), so the unique key is (latitude, longitude).
- Missing names are the literal text `nan` in the file. They become `Unnamed Toll Plaza (<State>)`.
- 20 locations have two or three different names: the first name wins, the others are logged.
- The loader reads everything, removes duplicates in memory, fetches existing rows once, then saves only new or changed rows. Run it twice and the second run changes nothing.

## 7. Likely interview questions

1. **Why is the name not the key?** It repeats at different places and can be missing, so I used the location (a unique constraint on latitude and longitude).
2. **How is upsert done, and is it efficient?** The importer loads existing rows once, compares by location, and does one `saveAll` for inserts and updates. It is idempotent. A native `ON CONFLICT` would be faster but ties the code to Postgres, and 1.5k rows do not need it.
3. **How do you decide a toll is "on" the route?** Within a configurable tolerance (300 m) of the route line, after narrowing with a bounding box. The tolerance is the tuning knob against real tolls.
4. **Why along-route distance, not a straight line?** Roads curve. The requirement is distance from the source, and a drive of 420 km can be 350 km in a straight line.
5. **How does caching work, and what are the keys?** Two Caffeine caches: pincode to coordinate, and source-destination to final result. Direction matters. A second identical call touches neither the geocoder nor ORS, which a test proves.
6. **What happens when ORS is down, rate limited or slow?** Every client call has a timeout. Failures become one exception type mapped to 429, 502, 503 or 504 with a clear message, never a stack trace. The API key is never in messages or logs.
7. **Why interfaces for the clients?** To swap providers (I already have Nominatim and ORS geocoders) and to test the logic with mocks, so tests never hit the network.
8. **Why did you use Nominatim for geocoding?** It resolves Indian postal codes reliably and needs no key. I could not verify ORS's postal-code coverage for India, so it is available but opt-in.
9. **How would this scale?** The service is stateless apart from the cache. I would move to a shared cache (Redis) for several instances, use a spatial index (PostGIS) instead of a bounding-box range query, and batch the geocoding.
10. **What are the limitations?** Matching quality depends on the CSV coordinates, parallel roads can produce false matches, and free-tier quotas and Render's idle sleep affect latency.
11. **How did you test it?** Unit tests for the matcher (written before the code), mocked-HTTP tests for the clients, Mockito for the service, MockMvc for the controller, a cache test that counts client calls, and a loader test against the real CSV.
12. **Why a bounding box before the distance calculation?** It turns "check all plazas against thousands of segments" into "check a few hundred", with a single indexed query.
