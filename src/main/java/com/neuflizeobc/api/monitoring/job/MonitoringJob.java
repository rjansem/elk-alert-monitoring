package com.neuflizeobc.api.monitoring.job;

import com.neuflizeobc.api.monitoring.service.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.TemporalField;

/**
 * Job permettant de monitorer l'activité des services
 *
 * @author rjansem
 */
@Component
public class MonitoringJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringJob.class);

    private MonitoringService monitoringService;

    @Autowired
    public MonitoringJob(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Scheduled(fixedRateString = "${monitoring.jobExecutionEvery}")
    public void reportCurrentTime() {
        LOGGER.info("Exécution du job de monitoring de l'activité des applications");
        Instant start = Instant.now();
        monitoringService.checkServersActivity();
        LOGGER.info("Fin de l'exécution du job de monitoring de l'activité des applications en {} ms", Instant.now().toEpochMilli() - start.toEpochMilli());
    }
}
