package AutoMonitoring.AutoMonitoring.config;

import AutoMonitoring.AutoMonitoring.contract.checkMediaValid.CheckValidDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@RequiredArgsConstructor
@Configuration
@EnableRedisRepositories
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(cf);

        return redisTemplate;
    }

    @Bean
    public ReactiveRedisTemplate<String, CheckValidDTO> reactiveCheckValidDtoRedisTemplate(ReactiveRedisConnectionFactory cf) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        Jackson2JsonRedisSerializer<CheckValidDTO> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, CheckValidDTO.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, CheckValidDTO> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, CheckValidDTO> context = builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(cf, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate(ReactiveRedisConnectionFactory cf) {
        return new ReactiveRedisTemplate<>(cf, RedisSerializationContext.string());
    }
}