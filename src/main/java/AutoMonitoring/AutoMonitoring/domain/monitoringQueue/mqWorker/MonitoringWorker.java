package AutoMonitoring.AutoMonitoring.domain.monitoringQueue.mqWorker;

import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.dto.CheckMediaManifestCmd;

public class MonitoringWorker {


    // 메시지를 받아 처리하는 함수, 처리에 실패하면 딜레이 큐로 보낸다.
    void receiveMessage(CheckMediaManifestCmd cmd){}

    // redis에 이전 이이전 미디어 메니페스트를 저장
    void addToRedis(String url){}
}