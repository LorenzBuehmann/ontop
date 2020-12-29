package it.unibz.inf.ontop.spec.mapping.transformer.impl;

import it.unibz.inf.ontop.com.google.common.collect.*;
import com.google.inject.Inject;
import it.unibz.inf.ontop.dbschema.DBParameters;
import it.unibz.inf.ontop.injection.OntopMappingSettings;
import it.unibz.inf.ontop.injection.SpecificationFactory;
import it.unibz.inf.ontop.iq.IQ;
import it.unibz.inf.ontop.model.atom.RDFAtomPredicate;
import it.unibz.inf.ontop.model.term.TermFactory;
import it.unibz.inf.ontop.spec.mapping.*;
import it.unibz.inf.ontop.spec.mapping.impl.MappingImpl;
import it.unibz.inf.ontop.spec.ontology.*;
import it.unibz.inf.ontop.spec.OBDASpecification;
import it.unibz.inf.ontop.spec.mapping.transformer.*;
import it.unibz.inf.ontop.spec.ontology.impl.OntologyBuilderImpl;
import it.unibz.inf.ontop.utils.ImmutableCollectors;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;

import java.util.Optional;
import java.util.stream.Stream;

import static it.unibz.inf.ontop.injection.OntopModelSettings.CardinalityPreservationMode.LOOSE;

public class DefaultMappingTransformer implements MappingTransformer {

    private final MappingVariableNameNormalizer mappingNormalizer;
    private final MappingSaturator mappingSaturator;
    private final ABoxFactIntoMappingConverter factConverter;
    private final OntopMappingSettings settings;
    private final MappingSameAsInverseRewriter sameAsInverseRewriter;
    private final SpecificationFactory specificationFactory;
    private final RDF rdfFactory;

    private MappingDistinctTransformer mappingDistinctTransformer;
    private final TermFactory termFactory;

    @Inject
    private DefaultMappingTransformer(MappingVariableNameNormalizer mappingNormalizer,
                                      MappingSaturator mappingSaturator,
                                      ABoxFactIntoMappingConverter inserter,
                                      OntopMappingSettings settings,
                                      MappingSameAsInverseRewriter sameAsInverseRewriter,
                                      SpecificationFactory specificationFactory,
                                      RDF rdfFactory,
                                      MappingDistinctTransformer mappingDistinctTransformer,
                                      TermFactory termFactory) {
        this.mappingNormalizer = mappingNormalizer;
        this.mappingSaturator = mappingSaturator;
        this.factConverter = inserter;
        this.settings = settings;
        this.sameAsInverseRewriter = sameAsInverseRewriter;
        this.specificationFactory = specificationFactory;
        this.rdfFactory = rdfFactory;
        this.mappingDistinctTransformer = mappingDistinctTransformer;
        this.termFactory = termFactory;
    }

    @Override
    public OBDASpecification transform(ImmutableList<MappingAssertion> mapping, DBParameters dbParameters, Optional<Ontology> ontology) {
        if (ontology.isPresent()) {
            ImmutableList<MappingAssertion> factsAsMapping = factConverter.convert(extractAboxFacts(ontology.get()),
                    // Useless. To be removed (temporary)
                    true);

            ImmutableList<MappingAssertion> mappingWithFacts =
                    Stream.concat(mapping.stream(), factsAsMapping.stream()).collect(ImmutableCollectors.toList());

            return createSpecification(mappingWithFacts, dbParameters, ontology.get().tbox());
        }
        else {
            ClassifiedTBox emptyTBox = OntologyBuilderImpl.builder(rdfFactory, termFactory).build().tbox();
            return createSpecification(mapping, dbParameters, emptyTBox);
        }
    }

    /**
     * Temporary (before introducing FactExtractor)
     */
    protected ImmutableSet<RDFFact> extractAboxFacts(Ontology ontology) {
        if (settings.isOntologyAnnotationQueryingEnabled())
            return ontology.abox();

        OntologyVocabularyCategory<AnnotationProperty> annotationProperties = ontology.annotationProperties();
        return ontology.abox().stream()
                .filter(f -> !annotationProperties.contains(f.getProperty().getIRI()))
                .collect(ImmutableCollectors.toSet());
    }


    private OBDASpecification createSpecification(ImmutableList<MappingAssertion> mapping, DBParameters dbParameters, ClassifiedTBox tbox) {

        ImmutableList<MappingAssertion> sameAsOptimizedMapping = sameAsInverseRewriter.rewrite(mapping);
        ImmutableList<MappingAssertion> saturatedMapping = mappingSaturator.saturate(sameAsOptimizedMapping, tbox);
        ImmutableList<MappingAssertion> normalizedMapping = mappingNormalizer.normalize(saturatedMapping);

        // Don't insert the distinct if the cardinality preservation is set to LOOSE
        ImmutableList<MappingAssertion> finalMapping = settings.getCardinalityPreservationMode() == LOOSE
                ? normalizedMapping
                : mappingDistinctTransformer.addDistinct(normalizedMapping);

        return specificationFactory.createSpecification(getMapping(finalMapping), dbParameters, tbox);
    }

    private Mapping getMapping(ImmutableList<MappingAssertion> assertions) {
        ImmutableTable<RDFAtomPredicate, IRI, IQ> propertyDefinitions = assertions.stream()
                .filter(e -> !e.getIndex().isClass())
                .map(DefaultMappingTransformer::asCell)
                .collect(ImmutableCollectors.toTable());

        ImmutableTable<RDFAtomPredicate, IRI, IQ> classDefinitions = assertions.stream()
                .filter(e -> e.getIndex().isClass())
                .map(DefaultMappingTransformer::asCell)
                .collect(ImmutableCollectors.toTable());

        return new MappingImpl(propertyDefinitions, classDefinitions);
    }

    private static Table.Cell<RDFAtomPredicate, IRI, IQ> asCell(MappingAssertion assertion) {
        MappingAssertionIndex index = assertion.getIndex();
        return Tables.immutableCell(index.getPredicate(), index.getIri(), assertion.getQuery());
    }
}
