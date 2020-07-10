package it.unibz.inf.ontop.endpoint.beans;

import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.rdf4j.repository.OntopRepository;
import it.unibz.inf.ontop.rdf4j.repository.impl.OntopVirtualRepository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class OntopVirtualRepositoryBean {

    @Bean
    private OntopVirtualRepository setupVirtualRepository(@Value("${mapping}") String mappings,
                                                          @Value("${ontology:#{null}}") String ontology,
                                                          @Value("${xml-catalog:#{null}}") String xmlCatalog,
                                                          @Value("${properties}") String properties,
                                                          @Value("${constraint:#{null}}") String constraint,
                                                          @Value("${lazy:false}") boolean lazy) throws RepositoryException {
        OntopSQLOWLAPIConfiguration.Builder<? extends OntopSQLOWLAPIConfiguration.Builder> builder = OntopSQLOWLAPIConfiguration.defaultBuilder()
                .propertyFile(properties);

        if (mappings.endsWith(".obda"))
            builder.nativeOntopMappingFile(mappings);
        else
            builder.r2rmlMappingFile(mappings);

        if ((ontology != null) && (!ontology.isEmpty()))
            builder.ontologyFile(ontology);

        if ((xmlCatalog != null) && (!xmlCatalog.isEmpty()))
            builder.xmlCatalogFile(xmlCatalog);

        if (constraint !=null && !constraint.isEmpty())
            builder.basicImplicitConstraintFile(constraint);

        OntopSQLOWLAPIConfiguration configuration = builder.build();
        OntopVirtualRepository repository = OntopRepository.defaultRepository(configuration);

        if (!lazy) {
            repository.init();
        }

        return repository;
    }
}
