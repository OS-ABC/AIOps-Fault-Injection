package com.pcxg.fitools;

import com.pcxg.fitools.entity.SSHConf;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({SSHConf.class})
@Configuration
@SpringBootApplication
public class FitoolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitoolsApplication.class, args);
    }

}
