package it.unibz.inf.ontop.spec.mapping.pp.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import it.unibz.inf.ontop.dbschema.*;
import it.unibz.inf.ontop.dbschema.impl.RawQuotedIDFactory;
import it.unibz.inf.ontop.exception.InvalidMappingSourceQueriesException;
import it.unibz.inf.ontop.exception.MinorOntopInternalBugException;
import it.unibz.inf.ontop.injection.CoreSingletons;
import it.unibz.inf.ontop.injection.IntermediateQueryFactory;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.model.atom.*;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.model.type.DBTypeFactory;
import it.unibz.inf.ontop.spec.mapping.MappingAssertion;
import it.unibz.inf.ontop.spec.mapping.TargetAtom;
import it.unibz.inf.ontop.spec.mapping.sqlparser.*;
import it.unibz.inf.ontop.spec.mapping.sqlparser.exception.InvalidSelectQueryException;
import it.unibz.inf.ontop.spec.mapping.sqlparser.exception.UnsupportedSelectQueryException;
import it.unibz.inf.ontop.spec.mapping.pp.PPMappingAssertionProvenance;
import it.unibz.inf.ontop.spec.mapping.pp.SQLPPMappingConverter;
import it.unibz.inf.ontop.spec.mapping.pp.SQLPPTriplesMap;
import it.unibz.inf.ontop.spec.mapping.transformer.impl.IQ2CQ;
import it.unibz.inf.ontop.substitution.ImmutableSubstitution;
import it.unibz.inf.ontop.substitution.SubstitutionFactory;
import it.unibz.inf.ontop.utils.ImmutableCollectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * SQLPPMapping -> MappingAssertion
 */
public class SQLPPMappingConverterImpl implements SQLPPMappingConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLPPMappingConverterImpl.class);

    private final TermFactory termFactory;
    private final IntermediateQueryFactory iqFactory;
    private final AtomFactory atomFactory;
    private final SubstitutionFactory substitutionFactory;
    private final CoreSingletons coreSingletons;
    private final DBTypeFactory dbTypeFactory;

    @Inject
    private SQLPPMappingConverterImpl(CoreSingletons coreSingletons) {
        this.termFactory = coreSingletons.getTermFactory();
        this.iqFactory = coreSingletons.getIQFactory();
        this.atomFactory = coreSingletons.getAtomFactory();
        this.substitutionFactory = coreSingletons.getSubstitutionFactory();
        this.coreSingletons = coreSingletons;
        this.dbTypeFactory = coreSingletons.getTypeFactory().getDBTypeFactory();
    }

    @Override
    public ImmutableList<MappingAssertion> convert(ImmutableList<SQLPPTriplesMap> mapping, MetadataLookup metadataLookup) throws InvalidMappingSourceQueriesException {

        ImmutableList.Builder<MappingAssertion> builder = ImmutableList.builder();
        for (SQLPPTriplesMap assertion : mapping) {
            RAExpression re = getRAExpression(assertion, metadataLookup);
            IQTree tree = IQ2CQ.toIQTree(
                    re.getDataAtoms().stream()
                            .collect(ImmutableCollectors.toList()),
                    termFactory.getConjunction(re.getFilterAtoms().stream()),
                    iqFactory);

            ImmutableMap<QuotedID, ImmutableTerm> lookupTable =  re.getAttributes().entrySet().stream()
                    .filter(e -> e.getKey().getRelation() == null)
                    .collect(ImmutableCollectors.toMap(e -> e.getKey().getAttribute(), Map.Entry::getValue));
            Function<Variable, Optional<ImmutableTerm>> lookup = placeholderLookup(assertion, metadataLookup.getQuotedIDFactory(), lookupTable);

            for (TargetAtom target : assertion.getTargetAtoms()) {
                PPMappingAssertionProvenance provenance = assertion.getMappingAssertionProvenance(target);
                builder.add(convert(target, lookup, provenance, tree));
            }
        }

        ImmutableList<MappingAssertion> result = builder.build();
        LOGGER.debug("Original mapping size: {}", result.size());
        return result;
    }


    private static <T> Function<Variable, Optional<T>> placeholderLookup(SQLPPTriplesMap mappingAssertion, QuotedIDFactory idFactory, ImmutableMap<QuotedID, T> lookup) {
        Function<Variable, Optional<T>> standard =
                v -> Optional.ofNullable(lookup.get(idFactory.createAttributeID(v.getName())));

        if (mappingAssertion instanceof OntopNativeSQLPPTriplesMap) {
            QuotedIDFactory rawIdFactory = new RawQuotedIDFactory(idFactory);
            return v -> Optional.ofNullable(standard.apply(v)
                            .orElse(lookup.get(rawIdFactory.createAttributeID(v.getName()))));
        }
        else
            return standard;
    }


    private MappingAssertion convert(TargetAtom target, Function<Variable, Optional<ImmutableTerm>> lookup, PPMappingAssertionProvenance provenance, IQTree tree) throws InvalidMappingSourceQueriesException {

        ImmutableMap<Variable, Optional<ImmutableTerm>> targetPreMap = target.getProjectionAtom().getArguments().stream()
                    .map(v -> target.getSubstitution().apply(v))
                    .flatMap(ImmutableTerm::getVariableStream)
                    .distinct()
                    .collect(ImmutableCollectors.toMap(Function.identity(), lookup));

        if (targetPreMap.values().stream().anyMatch(t -> !t.isPresent()))
            throw new InvalidMappingSourceQueriesException(targetPreMap.entrySet().stream()
                    .filter(e -> !e.getValue().isPresent())
                    .map(Map.Entry::getKey)
                    .map(Variable::getName)
                    .collect(Collectors.joining(", ",
                            "The placeholder(s) ",
                            " in the target do(es) not occur in source query of the mapping assertion\n["
                                    + provenance.getProvenanceInfo() + "]")));

        ImmutableMap<Variable, ImmutableTerm> targetMap = targetPreMap.entrySet().stream()
                .collect(ImmutableCollectors.toMap(Map.Entry::getKey, e -> e.getValue().orElseThrow(() -> new MinorOntopInternalBugException("Impossible"))));

        ImmutableSubstitution<ImmutableTerm> targetRenamingPart = substitutionFactory.getSubstitution(targetMap.entrySet().stream()
                .filter(e -> e.getValue() instanceof Variable)
                .filter(e -> !e.getValue().equals(e.getKey()))
                .collect(ImmutableCollectors.toMap()));

        ImmutableSubstitution<ImmutableTerm> spoSubstitution = substitutionFactory.getSubstitution(target.getSubstitution().getImmutableMap().entrySet().stream()
                        .collect(ImmutableCollectors.toMap(Map.Entry::getKey,
                                e -> targetRenamingPart.apply(e.getValue()))));

        ImmutableSubstitution<ImmutableTerm> selectSubstitution = substitutionFactory.getSubstitution(
                targetMap.entrySet().stream()
                        .filter(e -> !(e.getValue() instanceof Variable))
                        .collect(ImmutableCollectors.toMap()));

        IQTree selectTree = iqFactory.createUnaryIQTree(
                iqFactory.createConstructionNode(spoSubstitution.getImmutableMap().values().stream()
                        .flatMap(ImmutableTerm::getVariableStream).collect(ImmutableCollectors.toSet()), selectSubstitution),
                tree);

        IQTree mappingTree = iqFactory.createUnaryIQTree(iqFactory.createConstructionNode(
                target.getProjectionAtom().getVariables(), spoSubstitution), selectTree);

        return new MappingAssertion(iqFactory.createIQ(target.getProjectionAtom(), mappingTree), provenance);
    }

    private RAExpression getRAExpression(SQLPPTriplesMap mappingAssertion, MetadataLookup metadataLookup) throws InvalidMappingSourceQueriesException {
        String sourceQuery = mappingAssertion.getSourceQuery().getSQL();
        SelectQueryParser sqp = new SelectQueryParser(metadataLookup, coreSingletons);
        try {
            try {
                return sqp.parse(sourceQuery);
            }
            catch (UnsupportedSelectQueryException e) {
                ImmutableList<QuotedID> attributes;
                try {
                    DefaultSelectQueryAttributeExtractor sqae = new DefaultSelectQueryAttributeExtractor(metadataLookup, termFactory);
                    ImmutableMap<QuotedID, ImmutableTerm> attrs = sqae.getRAExpressionAttributes(sourceQuery).getUnqualifiedAttributes();
                    attributes = ImmutableList.copyOf(attrs.keySet());
                }
                catch (InvalidSelectQueryException | UnsupportedSelectQueryException e2) {
                    ApproximateSelectQueryAttributeExtractor sqae = new ApproximateSelectQueryAttributeExtractor(metadataLookup.getQuotedIDFactory());
                    attributes = sqae.getAttributes(sourceQuery);
                }
                ParserViewDefinition view = new ParserViewDefinition(attributes, sourceQuery, dbTypeFactory);
                return sqp.createAtom(view, ImmutableSet.of());
            }
        }
        catch (InvalidSelectQueryException e) {
            throw new InvalidMappingSourceQueriesException("Error: " + e.getMessage()
                    + " \nProblem location: source query of triplesMap \n["
                    +  mappingAssertion.getTriplesMapProvenance().getProvenanceInfo() + "]");
        }
    }
}
