package AutoMonitoring.AutoMonitoring.domain.db.exception;

public class ProgramNotFoundException extends RuntimeException{
    public ProgramNotFoundException(String id){
        super("프로그램을 찾을 수 없습니다, id=" +id);
    }
}
