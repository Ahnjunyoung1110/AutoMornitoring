package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

public class DelayMonitoringWorker {
    // 메시지를 받아 처리하는 함수, 처리에 실패하면 다음 큐로 보낸다.
    void receiveMessage(){}

    // 실패하는 횟수가 threshold(4) 를 넘어간 url을 처리한다.
    void recordFailUrl(){}
}
