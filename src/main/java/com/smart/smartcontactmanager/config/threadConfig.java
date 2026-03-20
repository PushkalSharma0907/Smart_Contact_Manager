package com.smart.smartcontactmanager.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class threadConfig {
	
	@Bean(name = "io")
	public ExecutorService ioBoundThreadPool() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}
	
	@Bean(name = "cpu")
	public ExecutorService cpuBoundThreadPool() {
		int cpuCores = Runtime.getRuntime().availableProcessors();
		return Executors.newFixedThreadPool(cpuCores);
	}

}
