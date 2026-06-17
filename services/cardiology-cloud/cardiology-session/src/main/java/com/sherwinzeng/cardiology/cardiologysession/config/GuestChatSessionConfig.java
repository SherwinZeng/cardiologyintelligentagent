package com.sherwinzeng.cardiology.cardiologysession.config;

import com.sherwinzeng.cardiology.cardiologysession.properties.GuestChatSessionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
@EnableConfigurationProperties(GuestChatSessionProperties.class)
public class GuestChatSessionConfig {

    @Bean
    public DefaultRedisScript<Long> guestCreateSessionScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/guest_create_session.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> guestAppendUserMessageScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/guest_append_user_message.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
