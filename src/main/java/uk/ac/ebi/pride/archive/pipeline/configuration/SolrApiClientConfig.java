package uk.ac.ebi.pride.archive.pipeline.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.ac.ebi.pride.solr.api.client.SolrApiClientFactory;
import uk.ac.ebi.pride.solr.api.client.SolrProjectClient;


@Configuration
public class SolrApiClientConfig {

    private final SolrApiClientFactory solrApiClientFactory;

    public SolrApiClientConfig(@Value("${solr.api.hx.baseUrl}") String apiBaseUrl,
                               @Value("${solr.api.hx.keyName}") String apiKeyName,
                               @Value("${solr.api.hx.keyValue}") String apiKeyValue,
                               @Value("${spring.application.name}") String appName) {

        this.solrApiClientFactory = new SolrApiClientFactory(apiBaseUrl, apiKeyName, apiKeyValue, appName);
    }

    @Bean
    @Primary
    @Qualifier("solrProjectClientHX")
    public SolrProjectClient solrProjectClientHX() {
        return this.solrApiClientFactory.getSolrProjectClient();
    }
}
