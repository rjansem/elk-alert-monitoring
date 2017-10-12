package com.neuflizeobc.api.monitoring.service;

import com.neuflizeobc.api.monitoring.domain.AlertDocument;
import com.neuflizeobc.api.monitoring.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service métier de monitoring, permettant l'interrogation d'elasticsearch et le traitement des données.
 *
 * @author rjansem
 */
@Service
public class MonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringService.class);

    private static final String TIMESTAMP_FIELD = "@timestamp";

    private final AlertRepository alertRepository;
    private final JavaMailSender javaMailSender;

    @Value("${monitoring.nbApplications}")
    private Integer nbApplications;

    @Value("${monitoring.email.subject}")
    private String emailSubject;

    @Value("${monitoring.email.from}")
    private String emailFrom;

    @Value("${monitoring.email.to}")
    private String emailTo;

    @Value("${monitoring.email.enabled}")
    private Boolean enabled;

    @Autowired
    public MonitoringService(AlertRepository alertRepository, JavaMailSender javaMailSender) {
        this.alertRepository = alertRepository;
        this.javaMailSender = javaMailSender;
    }

    /**
     * Interroge elasticsearch pour vérfier l'activité des différentes applications en vérifiant les alertes stockées dans ElasticSearch.
     */
    public void checkServersActivity() {
        // Pour s'assurer de remonter a minima 2 alertes par application.
        int nbAlertsToFetch = nbApplications * 3;
        Map<String, List<AlertDocument>> alertsByApp = alertRepository.findAll(PageRequest.of(0, nbAlertsToFetch, Sort.Direction.DESC, TIMESTAMP_FIELD))
                .stream()
                .collect(Collectors.groupingBy(AlertDocument::getUrl, LinkedHashMap::new, Collectors.toList()));
        LOGGER.debug("Alertes récupérées depuis ElasticSearch");

        alertsByApp.keySet().stream().map(k -> alertsByApp.get(k)).forEach(a -> {
            if (notifyForServerDown(a)) {
                LOGGER.debug("Envoi d'une alerte DOWN pour le serveur {}", a.get(0).getHost());
                notify(a, false);
            } else {
                LOGGER.debug("Pas d'envoi d'alerte DOWN pour le serveur {}", a.get(0).getHost());
            }
        });
        alertsByApp.keySet().stream().map(k -> alertsByApp.get(k)).forEach(a -> {
            if (notifyForServerUp(a)) {
                LOGGER.debug("Envoi d'une alerte UP pour le serveur {}", a.get(0).getHost());
                notify(a, true);
            } else {
                LOGGER.debug("Pas d'envoi d'alerte UP pour le serveur {}", a.get(0).getHost());
            }
        });

    }

    /**
     * Vérifie s'il faut envoyer une notification pour extinction d'application
     *
     * @param alerts : alertes enregistrées pour une application
     * @return true si la dernière alerte indique un serveur DOWN et que l'avant dernière indique un serveur UP.
     */
    private boolean notifyForServerDown(List<AlertDocument> alerts) {
        AlertDocument lastAlert = alerts.get(0);
        AlertDocument previousAlert = alerts.get(1);

        //return Boolean.FALSE.equals(lastAlert.getUp()) && Boolean.TRUE.equals(previousAlert.getUp());
        return Boolean.FALSE.equals(lastAlert.getUp()) ;
    }

    /**
     * Vérifie s'il faut envoyer une notification pour extinction remise en route d'application
     *
     * @param alerts : alertes enregistrées pour une application
     * @return true si la dernière alerte indique un serveur UP et que l'avant dernière indique un serveur DOWN.
     */
    private boolean notifyForServerUp(List<AlertDocument> alerts) {
        AlertDocument lastAlert = alerts.get(0);
        AlertDocument previousAlert = alerts.get(1);

        //return Boolean.TRUE.equals(lastAlert.getUp()) && Boolean.FALSE.equals(previousAlert.getUp());
        return Boolean.TRUE.equals(lastAlert.getUp());
    }

    /**
     * Envoi d'une notification par email
     *
     * @param alerts : liste des alertes d'une application
     * @param up     : true si l'application vient de redémarrer
     */
    private void notify(List<AlertDocument> alerts, boolean up) {
        String message;
        if (up) {
            message = "L'application répondant à l'URL " + alerts.get(0).getUrl() + " a redémarré";
        } else {
            message = "L'application répondant à l'URL " + alerts.get(0).getUrl() + " s'est éteinte";
        }
        sendMail(message);
    }

    /**
     * Envoi d'un email générique
     *
     * @param message
     */
    private void sendMail(String message) {
        LOGGER.warn("Envoi de l'email : {}", message);
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom(emailFrom);
            messageHelper.setTo(emailTo);
            messageHelper.setSubject(emailSubject);
            messageHelper.setText(message);
            LOGGER.info("le mail a été envoyé à l'adresse {}", emailTo);
        };
        try {
            if (Boolean.TRUE.equals(this.enabled)) {
                javaMailSender.send(messagePreparator);
            } else {
                LOGGER.warn("Envoi d'email désactivé");
            }
        } catch (MailException e) {
            throw new RuntimeException("Erreur lors de l'envoi du mail", e);
        }

    }
}
