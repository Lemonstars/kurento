package cn.superid;

import cn.superid.handler.HelloHandler;
import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
@EnableWebSocket
public class KurentoApplication implements WebSocketConfigurer{

	@Bean
	public KurentoClient kurentoClient() {
		return KurentoClient.create("ws://192.168.1.184:8888/kurento");
	}

    @Bean
    public HelloHandler helloHandler() {
        return new HelloHandler();
    }

    public static void main(String[] args) {
		SpringApplication.run(KurentoApplication.class, args);
	}

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(helloHandler(), "/hello");
    }
}
