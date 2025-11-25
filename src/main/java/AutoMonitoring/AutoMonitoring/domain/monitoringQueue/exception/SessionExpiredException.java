package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.exception;

import lombok.Getter;

@Getter
public class SessionExpiredException extends RuntimeException {
    private static final String DEFAULT_MESSAGE =
            "Session has expired. Please log in again.";

    private final String traceId;

    public SessionExpiredException(String traceId) {
        super(DEFAULT_MESSAGE);
        this.traceId = traceId;

    }

    public SessionExpiredException(String message, String traceId) {
        super(message);
        this.traceId = traceId;
    }
}
