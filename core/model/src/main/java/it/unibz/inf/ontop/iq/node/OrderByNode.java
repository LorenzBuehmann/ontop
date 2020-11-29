package it.unibz.inf.ontop.iq.node;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.injection.IntermediateQueryFactory;
import it.unibz.inf.ontop.model.term.ImmutableTerm;
import it.unibz.inf.ontop.model.term.NonGroundTerm;
import it.unibz.inf.ontop.substitution.ImmutableSubstitution;

import java.util.Optional;

/**
 * Follows a NULLS FIRST semantics (if ascending, NULLS LAST otherwise), similarly to SPARQL
 *
 * See https://www.w3.org/TR/sparql11-query/#modOrderBy
 *
 * See {@link IntermediateQueryFactory#createOrderByNode} for creating a new instance.
 */
public interface OrderByNode extends QueryModifierNode {

    ImmutableList<OrderComparator> getComparators();

    Optional<OrderByNode> applySubstitution(ImmutableSubstitution<? extends ImmutableTerm> substitution);

    @Override
    OrderByNode clone();


    interface OrderComparator {

        boolean isAscending();

        NonGroundTerm getTerm();
    }
}
