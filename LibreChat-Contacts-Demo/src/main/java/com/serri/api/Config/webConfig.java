package com.serri.api.Config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class webConfig implements WebMvcConfigurer {

   @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")          
                .allowedOrigins(
                    "http://localhost:3080",  
                    "http://localhost:8080"   
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
      @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
