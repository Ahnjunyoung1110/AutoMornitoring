package AutoMonitoring.AutoMonitoring.exception;


import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Autowired
    RedisService redisService;

    @ExceptionHandler(IllegalArgumentException.class)
    public void illegalException(IllegalArgumentException e){
        String traceId = e.getMessage().split(" ")[0];
        redisService.setValues(traceId, "Error :%s".formatted(e.getMessage().split(" ")[1]));

    }

    @ExceptionHandler(IOException.class)
    public void ioException(IOException e){
        String traceId = e.getMessage().split(" ")[0];
        log.warn("%s 에서 문제가 발생했습니다. %s".formatted(traceId ,e.getMessage().split(" ")[1]));

    }

    @ExceptionHandler(RuntimeException.class)
    public void runtimeException(RuntimeException e){
        String traceId = e.getMessage().split(" ")[0];
        log.warn("%s 에서 문제가 발생했습니다. %s".formatted(traceId ,e.getMessage().split(" ")[1]));

    }
}
