package AutoMonitoring.AutoMonitoring.domain.program.exception;

public class ProgramAlreadyExistException extends RuntimeException {
    public ProgramAlreadyExistException(String message) {
        super(message);
    }
}
