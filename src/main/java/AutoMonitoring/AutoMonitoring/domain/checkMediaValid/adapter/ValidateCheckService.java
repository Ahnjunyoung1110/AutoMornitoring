package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.ValidationResult;

public interface ValidateCheckService {
    ValidationResult checkValidation(CheckValidDTO dto);
}
