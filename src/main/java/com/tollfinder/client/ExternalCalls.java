package com.tollfinder.client;

import com.tollfinder.exception.ExternalServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.io.InterruptedIOException;
import java.net.http.HttpTimeoutException;
import java.util.function.Supplier;

/** Turns low-level HTTP failures into one exception type the controller advice understands. */
final class ExternalCalls {

    private ExternalCalls() {
    }

    static <T> T call(String provider, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw fromStatus(provider, e);
        } catch (ResourceAccessException e) {
            boolean timedOut = e.getCause() instanceof InterruptedIOException
                    || e.getCause() instanceof HttpTimeoutException;
            throw timedOut
                    ? new ExternalServiceException(HttpStatus.GATEWAY_TIMEOUT, provider + " timed out, please try again")
                    : new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE, provider + " is unreachable, please try again later");
        }
    }

    // never include request details here: they carry the API key
    static ExternalServiceException fromStatus(String provider, RestClientResponseException e) {
        if (e.getStatusCode().value() == 429) {
            return new ExternalServiceException(HttpStatus.TOO_MANY_REQUESTS,
                    provider + " rate limit reached, please try again later");
        }
        if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
            return new ExternalServiceException(HttpStatus.BAD_GATEWAY,
                    provider + " rejected our credentials, check that ORS_API_KEY is set and valid");
        }
        return new ExternalServiceException(HttpStatus.BAD_GATEWAY,
                provider + " returned an error (HTTP " + e.getStatusCode().value() + ")");
    }
}
