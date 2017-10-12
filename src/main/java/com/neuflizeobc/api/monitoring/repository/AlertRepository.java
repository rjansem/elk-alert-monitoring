package com.neuflizeobc.api.monitoring.repository;

import com.neuflizeobc.api.monitoring.domain.AlertDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.stream.Stream;

/**
 * Repository de recherche de documents de type Alert dans ElasticSearch
 *
 * @author rjansem
 */
public interface AlertRepository extends ElasticsearchRepository<AlertDocument, String> {

}
