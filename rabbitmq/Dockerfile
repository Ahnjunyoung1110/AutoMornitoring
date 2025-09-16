FROM rabbitmq:3.13-management

# 플러그인 다운로드
ADD https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v3.13.0/rabbitmq_delayed_message_exchange-3.13.0.ez /opt/rabbitmq/plugins/

# 파일 권한 수정 (이부분 추가)
RUN chmod 644 /opt/rabbitmq/plugins/rabbitmq_delayed_message_exchange-3.13.0.ez

# 플러그인 활성화
RUN rabbitmq-plugins enable rabbitmq_delayed_message_exchange

# RabbitMQ 포트 노출
EXPOSE 5672 15672

# RabbitMQ 서버 실행
CMD ["rabbitmq-server"]
