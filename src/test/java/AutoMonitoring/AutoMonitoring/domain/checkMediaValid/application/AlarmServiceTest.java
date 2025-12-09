//package AutoMonitoring.AutoMonitoring.domain.checkMediaValid.application;
//
//import AutoMonitoring.AutoMonitoring.domain.checkMediaValid.util.CheckValidConfigHolder;
//import AutoMonitoring.AutoMonitoring.util.redis.adapter.RedisService;
//import AutoMonitoring.AutoMonitoring.util.redis.keys.RedisKeys;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class AlarmServiceTest {
//
//    @Mock
//    private WebClient webClient;
//    @Mock
//    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
//    @Mock
//    private WebClient.RequestBodySpec requestBodySpec;
//    @Mock
//    private WebClient.ResponseSpec responseSpec;
//
//    @Mock
//    private CheckValidConfigHolder configHolder;
//    @Mock
//    private RedisService redisService;
//
//    @InjectMocks
//    private AlarmService alarmService;
//
//    private final String TRACE_ID = "testTraceId";
//    private final String RESOLUTION = "1080p";
//    private final String VALIDATION_RESULT = "ERROR_TEST";
//    private final String SLACK_WEBHOOK_URL = "http://mock.slack.webhook";
//
//
//    @BeforeEach
//    void setUp() {
//        // Mock WebClient behavior for successful response
//        when(webClient.post()).thenReturn(requestBodyUriSpec);
//        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
//        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
//        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
//        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
//        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("ok"));
//
//        // Set webhook URL value
//        alarmService.url = SLACK_WEBHOOK_URL;
//    }
//
//    @Test
//    void publishAlarm_disabled_doesNotSendAlarm() {
//        // Arrange
//        when(configHolder.isAlarmEnabled()).thenReturn(false);
//
//        // Act
//        alarmService.publishAlarm(VALIDATION_RESULT, RESOLUTION, TRACE_ID);
//
//        // Assert
//        verify(webClient, never()).post();
//    }
//
//    @Test
//    void publishAlarm_cooldownActive_doesNotSendAlarm() {
//        // Arrange
//        when(configHolder.isAlarmEnabled()).thenReturn(true);
//        when(redisService.getValues(RedisKeys.alarmCooldownKey(TRACE_ID, RESOLUTION))).thenReturn("1"); // Cooldown active
//
//        // Act
//        alarmService.publishAlarm(VALIDATION_RESULT, RESOLUTION, TRACE_ID);
//
//        // Assert
//        verify(webClient, never()).post();
//        verify(redisService, times(1)).getValues(RedisKeys.alarmCooldownKey(TRACE_ID, RESOLUTION));
//        verify(redisService, never()).increment(anyString(), anyLong());
//    }
//
//    @Test
//    void publishAlarm_thresholdExceeded_doesNotSendAlarm() {
//        // Arrange
//        when(configHolder.isAlarmEnabled()).thenReturn(true);
//        when(redisService.getValues(RedisKeys.alarmCooldownKey(TRACE_ID, RESOLUTION))).thenReturn("false"); // No cooldown
//        when(configHolder.getThreshold()).thenReturn(5); // Threshold
//        when(redisService.increment(RedisKeys.alarmCountKey(TRACE_ID, RESOLUTION), 1L)).thenReturn(6L); // Exceeds threshold
//
//        // Act
//        alarmService.publishAlarm(VALIDATION_RESULT, RESOLUTION, TRACE_ID);
//
//        // Assert
//        verify(webClient, never()).post();
//        verify(redisService, times(1)).getValues(RedisKeys.alarmCooldownKey(TRACE_ID, RESOLUTION));
//        verify(redisService, times(1)).increment(eq(RedisKeys.alarmCountKey(TRACE_ID, RESOLUTION)), eq(1L));
//        verify(redisService, times(1)).expire(eq(RedisKeys.alarmCountKey(TRACE_ID, RESOLUTION)), any(Duration.class));
//        verify(redisService, never()).setValues(anyString(), anyString(), any(Duration.class)); // Cooldown not set
//    }
//
//    @Test
//    void publishAlarm_maxPerChannelExceeded_doesNotSendAlarm() {
//        // Arrange
//        when(configHolder.isAlarmEnabled()).thenReturn(true);
//        when(redisService.getValues(RedisKeys.alarmCooldownKey(TRACE_ID, RESOLUTION))).thenReturn("false"); // No cooldown
//        when(configHolder.getThreshold()).thenReturn(5); // Threshold
//        when(redisService.increment(RedisKeys.alarmCountKey(TRACE_ID, RESOLUTION), 1L)).thenReturn(3L); // Below threshold
//        when(configHolder.getMaxAlarmsPerChannelPerHour()).thenReturn(10); // Max per channel
//        when(redisService.increment(RedisKeys.channelAlarmCountKey(RESOLUTION), 1L)).thenReturn(11L); // Exceeds max per channel
//
//        // Act
//        alarmService.publishAlarm(VALIDATION_RESULT, RESOLUTION, TRACE_ID);
//
//        // Assert
//        verify(webClient, never()).post();
//        verify(redisService, times(1)).getValues(RedisKeys.alarmCooldownKey(TRACE_ID, RESOLUTION));
//        verify(redisService, times(1)).increment(eq(RedisKeys.alarmCountKey(TRACE_ID, RESOLUTION)), eq(1L));
//        verify(redisService, times(1)).expire(eq(RedisKeys.alarmCountKey(TRACE_ID, RESOLUTION)), any(Duration.class));
//        verify(redisService, times(1)).increment(eq(RedisKeys.channelAlarmCountKey(RESOLUTION)), eq(1L));
//        verify(redisService, times(1)).expire(eq(RedisKeys.channelAlarmCountKey(RESOLUTION)), any(Duration.class));
//        verify(redisService, never()).setValues(anyString(), anyString(), any(Duration.class)); // Cooldown not set
//    }
//
//    @Test
//    void publishAlarm_sendsAlarmAndSetsCooldown() {
//        // Arrange
//        when(configHolder.isAlarmEnabled()).thenReturn(true);
//        when(redisService.hasKey(RedisKeys.alarmCooldownKey(TRACE_ID, RESOLUTION))).thenReturn(false);
//        when(configHolder.getThreshold()).thenReturn(5);
//        when(redisService.increment(RedisKeys.alarmCountKey(TRACE_ID, RESOLUTION), 1L)).thenReturn(1L); // Below threshold
//        when(configHolder.getMaxAlarmsPerChannelPerHour()).thenReturn(10);
//        when(redisService.increment(RedisKeys.channelAlarmCountKey(RESOLUTION), 1L)).thenReturn(1L); // Below max per channel
//        when(configHolder.getAlarmCooldownSeconds()).thenReturn(60);
//
//        // Act
//        alarmService.publishAlarm(VALIDATION_RESULT, RESOLUTION, TRACE_ID);
//
//        // Assert
//        verify(webClient, times(1)).post(); // WebClient post called
//        verify(redisService, times(1)).setValues(eq(RedisKeys.alarmCooldownKey(TRACE_ID, RESOLUTION)), eq("1"), eq(Duration.ofSeconds(60)));
//        verify(redisService, times(1)).expire(eq(RedisKeys.alarmCountKey(TRACE_ID, RESOLUTION)), any(Duration.class));
//        verify(redisService, times(1)).expire(eq(RedisKeys.channelAlarmCountKey(RESOLUTION)), any(Duration.class));
//    }
//}
