package it.unibz.inf.ontop.materialization.impl;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.com.google.common.collect.ImmutableMap;
import it.unibz.inf.ontop.com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.com.google.common.collect.UnmodifiableIterator;
import it.unibz.inf.ontop.answering.OntopQueryEngine;
import it.unibz.inf.ontop.answering.connection.OntopConnection;
import it.unibz.inf.ontop.answering.connection.OntopStatement;
import it.unibz.inf.ontop.answering.reformulation.input.InputQueryFactory;
import it.unibz.inf.ontop.answering.reformulation.input.SelectQuery;
import it.unibz.inf.ontop.answering.resultset.MaterializedGraphResultSet;
import it.unibz.inf.ontop.answering.resultset.OntopBindingSet;
import it.unibz.inf.ontop.answering.resultset.SimpleGraphResultSet;
import it.unibz.inf.ontop.answering.resultset.TupleResultSet;
import it.unibz.inf.ontop.exception.*;
import it.unibz.inf.ontop.materialization.MaterializationParams;
import it.unibz.inf.ontop.model.term.IRIConstant;
import it.unibz.inf.ontop.model.term.ObjectConstant;
import it.unibz.inf.ontop.model.term.RDFConstant;
import it.unibz.inf.ontop.model.term.TermFactory;
import it.unibz.inf.ontop.model.vocabulary.RDF;
import it.unibz.inf.ontop.spec.ontology.ABoxAssertionSupplier;
import it.unibz.inf.ontop.spec.ontology.Assertion;
import it.unibz.inf.ontop.spec.ontology.NamedAssertion;
import it.unibz.inf.ontop.spec.ontology.impl.OntologyBuilderImpl;
import org.apache.commons.rdf.api.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class DefaultMaterializedGraphResultSet implements MaterializedGraphResultSet {

    private final TermFactory termFactory;
    private final ImmutableMap<IRI, VocabularyEntry> vocabulary;
    private final InputQueryFactory inputQueryFactory;
    private final boolean canBeIncomplete;

    private final OntopQueryEngine queryEngine;
    private final UnmodifiableIterator<VocabularyEntry> vocabularyIterator;
    private final ABoxAssertionSupplier builder;


    private int counter;
    @Nullable
    private OntopConnection ontopConnection;
    @Nullable
    private OntopStatement tmpStatement;
    @Nullable
    private TupleResultSet tmpContextResultSet;

    private Logger LOGGER = LoggerFactory.getLogger(DefaultMaterializedGraphResultSet.class);
    private final List<IRI> possiblyIncompleteClassesAndProperties;
    private VocabularyEntry lastSeenPredicate;
    private IRIConstant lastSeenPredicateIRI;

    private final IRIConstant rdfTypeIRI;


    DefaultMaterializedGraphResultSet(ImmutableMap<IRI, VocabularyEntry> vocabulary, MaterializationParams params,
                                      OntopQueryEngine queryEngine,
                                      InputQueryFactory inputQueryFactory,
                                      TermFactory termFactory,
                                      org.apache.commons.rdf.api.RDF rdfFactory) {

        this.termFactory = termFactory;
        this.builder = OntologyBuilderImpl.assertionSupplier(rdfFactory);
        this.vocabulary = vocabulary;
        this.vocabularyIterator = vocabulary.values().iterator();

        this.queryEngine = queryEngine;
        this.canBeIncomplete = params.canMaterializationBeIncomplete();
        this.inputQueryFactory = inputQueryFactory;
        this.possiblyIncompleteClassesAndProperties = new ArrayList<>();

        counter = 0;

        rdfTypeIRI = termFactory.getConstantIRI(RDF.TYPE.getIRIString());

        // Lately initiated
        ontopConnection = null;
        tmpStatement = null;
        tmpContextResultSet = null;
    }

    @Override
    public ImmutableSet<IRI> getSelectedVocabulary() {
        return vocabulary.keySet();
    }

    @Override
    public boolean hasNext() throws OntopQueryAnsweringException, OntopConnectionException {
        // Initialization
        if (ontopConnection == null)
            ontopConnection = queryEngine.getConnection();

        if ((tmpContextResultSet != null) && tmpContextResultSet.hasNext()) {
            return true;
        }

        // Davide> If there is no next, we need to go to the next vocabulary predicate

        while (vocabularyIterator.hasNext()) {
            /*
             * Closes the previous result set and statement (if open)
             */
            if (tmpContextResultSet != null) {
                try {
                    tmpContextResultSet.close();
                } catch (OntopConnectionException e) {
                    LOGGER.warn("Non-critical exception while closing the graph result set: " + e);
                    // Not critical, continue
                }
            }
            if (tmpStatement != null) {
                try {
                    tmpStatement.close();
                } catch (OntopConnectionException e) {
                    LOGGER.warn("Non-critical exception while closing the statement: " + e);
                    // Not critical, continue
                }
            }

            /*
             * New query for the next RDF property/class
             */
            VocabularyEntry predicate = vocabularyIterator.next();

            SelectQuery query = inputQueryFactory.createSelectQuery(predicate.getSelectQuery());

            try {
                tmpStatement = ontopConnection.createStatement();
                tmpContextResultSet = tmpStatement.execute(query);

                if (tmpContextResultSet.hasNext()) {
                    lastSeenPredicate = predicate;
                    lastSeenPredicateIRI = termFactory.getConstantIRI(lastSeenPredicate.getIRIString());

                    return true;
                }
            } catch (OntopQueryAnsweringException | OntopConnectionException e) {
                if (canBeIncomplete) {
                    LOGGER.warn("Possibly incomplete class/property " + predicate + " (materialization problem).\n"
                            + "Details: " + e);
                    possiblyIncompleteClassesAndProperties.add(predicate.name);
                } else {
                    LOGGER.error("Problem materializing the class/property " + predicate);
                    throw e;
                }
            }
        }

        return false;
    }

    /**
     * Builds (named) assertions out of (quad) results
     */
    private Assertion toAssertion(OntopBindingSet tuple) throws OntopResultConversionException {
        ObjectConstant s = (ObjectConstant) tuple.getConstant("s");
        ObjectConstant p = lastSeenPredicate.isClass() ? rdfTypeIRI : lastSeenPredicateIRI;
        RDFConstant o = lastSeenPredicate.isClass() ? lastSeenPredicateIRI : tuple.getConstant("o");
        ObjectConstant g = (ObjectConstant)tuple.getConstant("g");
        Assertion a = SimpleGraphResultSet.getAssertion(builder, s, p, o);
        if (g != null) {
            a = NamedAssertion.of(a, g);
        }
        return a;
    }

    @Override
    public Assertion next() throws OntopQueryAnsweringException {
        counter++;

        OntopBindingSet resultTuple;
        try {
            resultTuple = tmpContextResultSet.next();
            return toAssertion(resultTuple);
        } catch (OntopConnectionException e) {
            try {
                tmpContextResultSet.close();
            } catch (OntopConnectionException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Releases all the connection resources
     */
    public void close() throws OntopConnectionException {
        if (tmpStatement != null) {
            tmpStatement.close();
        }
        if (ontopConnection != null) {
            ontopConnection.close();
        }
    }

    public long getTripleCountSoFar() {
        return counter;
    }

    public ImmutableList<IRI> getPossiblyIncompleteRDFPropertiesAndClassesSoFar() {
        return ImmutableList.copyOf(possiblyIncompleteClassesAndProperties);
    }
}
