package AutoMonitoring.AutoMonitoring.domain.program.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.NOT_FOUND;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProgramNotFoundException.class)
    public ProblemDetail handleProgramNotFound(
            ProgramNotFoundException ex,
            HttpServletRequest request
    ){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(NOT_FOUND, ex.getMessage());
        pd.setTitle("Program Not Found");
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "데이터 무결성 위반");
        pd.setTitle("Data Integrity Violation");
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("cause", NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
        return pd;
    }

    @ExceptionHandler(ProgramAlreadyExistException.class)
    public ProblemDetail handelProgramAlreadyExist(
            ProgramAlreadyExistException ex,
            HttpServletRequest request
    ){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(NOT_FOUND, ex.getMessage());
        pd.setTitle("Program Already Exist");
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }
}
