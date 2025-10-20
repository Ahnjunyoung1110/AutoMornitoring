package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.adapter;

import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.dto.CheckValidDTO;
import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.vo.ValidationResult;

public interface ValidateCheckService {
    ValidationResult checkValidation(CheckValidDTO dto);
}
