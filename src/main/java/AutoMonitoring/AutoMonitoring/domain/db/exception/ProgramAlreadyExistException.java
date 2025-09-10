package AutoMonitoring.AutoMonitoring.domain.db.exception;

public class ProgramAlreadyExistException extends RuntimeException {
    public ProgramAlreadyExistException(String message) {
        super(message);
    }
}
