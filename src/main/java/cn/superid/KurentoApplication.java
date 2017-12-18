package cn.superid;

import cn.superid.handler.CallHandler;
import cn.superid.manager.RoomManagerInterface;
import cn.superid.manager.UserManagerInterface;
import cn.superid.manager.impl.RoomManagerImpl;
import cn.superid.manager.impl.UserManagerImpl;
import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * SpringBoot启动类，同时配置
 *
 * @author 刘兴
 * @date 2017-12-6
 * @version 1.0
 */
@SpringBootApplication
@EnableWebSocket
public class KurentoApplication implements WebSocketConfigurer {

    @Bean
    public UserManagerInterface registry() {
        return new UserManagerImpl();
    }

    @Bean
    public RoomManagerInterface roomManager() {
        return new RoomManagerImpl();
    }

    @Bean
    public CallHandler groupCallHandler() {
        return new CallHandler();
    }

    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create("ws://192.168.1.184:8888/kurento");
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(KurentoApplication.class, args);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(groupCallHandler(), "/groupcall");
    }

}
