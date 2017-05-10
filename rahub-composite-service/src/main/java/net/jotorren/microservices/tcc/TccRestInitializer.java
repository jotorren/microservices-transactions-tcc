package net.jotorren.microservices.tcc;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.atomikos.icatch.config.Configuration;

@Component
public class TccRestInitializer implements ApplicationListener<ContextRefreshedEvent>{

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
    	Configuration.init();
    }
}
