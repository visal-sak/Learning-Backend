package co.istad.visal.e_learning.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Async (emails sent in the background) and @Scheduled (keep-alive for live notification streams). */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
