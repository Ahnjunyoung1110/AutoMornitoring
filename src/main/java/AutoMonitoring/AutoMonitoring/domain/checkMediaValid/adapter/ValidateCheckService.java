package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.ValidationResult;
import reactor.core.publisher.Mono;

public interface ValidateCheckService {
    Mono<ValidationResult> checkValidation(CheckValidDTO dto);
}