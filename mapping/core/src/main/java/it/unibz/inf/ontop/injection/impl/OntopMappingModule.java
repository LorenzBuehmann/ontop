package it.unibz.inf.ontop.injection.impl;


import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import it.unibz.inf.ontop.injection.OntopMappingConfiguration;
import it.unibz.inf.ontop.injection.OntopMappingSettings;
import it.unibz.inf.ontop.injection.ProvenanceMappingFactory;
import it.unibz.inf.ontop.spec.mapping.MappingWithProvenance;
import it.unibz.inf.ontop.spec.ontology.MappingVocabularyExtractor;
import it.unibz.inf.ontop.spec.mapping.validation.MappingOntologyComplianceValidator;
import it.unibz.inf.ontop.spec.mapping.transformer.*;
import it.unibz.inf.ontop.datalog.Datalog2QueryMappingConverter;
import it.unibz.inf.ontop.datalog.Mapping2DatalogConverter;
import it.unibz.inf.ontop.spec.mapping.TMappingExclusionConfig;
import it.unibz.inf.ontop.spec.mapping.transformer.MappingTransformer;


public class OntopMappingModule extends OntopAbstractModule {

    private final OntopMappingConfiguration configuration;

    OntopMappingModule(OntopMappingConfiguration configuration) {
        super(configuration.getSettings());
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bindTMappingExclusionConfig();
        bind(OntopMappingSettings.class).toInstance(configuration.getSettings());
        bindFromPreferences(MappingNormalizer.class);
        bindFromPreferences(MappingSaturator.class);
        bindFromPreferences(MappingCanonicalRewriter.class);
        bindFromPreferences(Datalog2QueryMappingConverter.class);
        bindFromPreferences(Mapping2DatalogConverter.class);
        bindFromPreferences(MappingVocabularyExtractor.class);
        bindFromPreferences(ABoxFactIntoMappingConverter.class);
        bindFromPreferences(MappingDatatypeFiller.class);
        bindFromPreferences(MappingMerger.class);
        bindFromPreferences(MappingTransformer.class);
        bindFromPreferences(MappingOntologyComplianceValidator.class);
        bindFromPreferences(MappingSameAsInverseRewriter.class);
        bindFromPreferences(MappingEquivalenceFreeRewriter.class);

        Module factoryModule = buildFactory(ImmutableList.of(MappingWithProvenance.class),
                ProvenanceMappingFactory.class);
        install(factoryModule);

    }

    private void bindTMappingExclusionConfig() {
        TMappingExclusionConfig tMappingExclusionConfig = configuration.getTmappingExclusions()
                .orElseGet(TMappingExclusionConfig::empty);

        bind(TMappingExclusionConfig.class).toInstance(tMappingExclusionConfig);
    }
}