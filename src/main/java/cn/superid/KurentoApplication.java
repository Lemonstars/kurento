package cn.superid;

import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * SpringBoot启动类，同时配置
 *
 * @author 刘兴
 * @date 2017-12-6
 * @version 1.0
 */
@SpringBootApplication
public class KurentoApplication{

    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create("ws://192.168.1.204:8888/kurento");
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(KurentoApplication.class, args);
    }

}
