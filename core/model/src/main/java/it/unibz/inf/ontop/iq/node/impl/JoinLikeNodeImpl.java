package it.unibz.inf.ontop.iq.node.impl;

import it.unibz.inf.ontop.com.google.common.collect.*;
import it.unibz.inf.ontop.evaluator.TermNullabilityEvaluator;
import it.unibz.inf.ontop.injection.IntermediateQueryFactory;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.iq.exception.InvalidIntermediateQueryException;
import it.unibz.inf.ontop.iq.node.VariableNullability;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.iq.IntermediateQuery;
import it.unibz.inf.ontop.iq.node.JoinLikeNode;
import it.unibz.inf.ontop.model.type.TypeFactory;
import it.unibz.inf.ontop.substitution.SubstitutionFactory;
import it.unibz.inf.ontop.substitution.impl.ImmutableSubstitutionTools;
import it.unibz.inf.ontop.substitution.impl.ImmutableUnificationTools;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;


@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class JoinLikeNodeImpl extends JoinOrFilterNodeImpl implements JoinLikeNode {

    protected JoinLikeNodeImpl(Optional<ImmutableExpression> optionalJoinCondition,
                               TermNullabilityEvaluator nullabilityEvaluator,
                               TermFactory termFactory, IntermediateQueryFactory iqFactory,
                               TypeFactory typeFactory, SubstitutionFactory substitutionFactory,
                               ImmutableUnificationTools unificationTools, ImmutableSubstitutionTools substitutionTools) {
        super(optionalJoinCondition, nullabilityEvaluator, termFactory, iqFactory, typeFactory,
                substitutionFactory, unificationTools, substitutionTools);
    }

    @Override
    public ImmutableSet<Variable> getRequiredVariables(IntermediateQuery query) {
        ImmutableMultiset<Variable> childrenVariableBag = query.getChildren(this).stream()
                .flatMap(c -> query.getVariables(c).stream())
                .collect(ImmutableCollectors.toMultiset());

        Stream<Variable> cooccuringVariableStream = childrenVariableBag.entrySet().stream()
                .filter(e -> e.getCount() > 1)
                .map(Multiset.Entry::getElement);

        return Stream.concat(cooccuringVariableStream, getLocallyRequiredVariables().stream())
                .collect(ImmutableCollectors.toSet());
    }


    /**
     * Checks that non-projected variables are not shared among children
     */
    protected void checkNonProjectedVariables(ImmutableList<IQTree> children) throws InvalidIntermediateQueryException {

        Set<Variable> allVariables = new HashSet<>();
        // Variables projected by the children
        children.forEach(c -> allVariables.addAll(c.getVariables()));

        for (IQTree child : children) {
            ImmutableSet<Variable> childProjectedVariables = child.getVariables();


            ImmutableSet<Variable> childNonProjectedVariables = child.getKnownVariables().stream()
                    .filter(v -> !childProjectedVariables.contains(v))
                    .collect(ImmutableCollectors.toSet());

            ImmutableSet<Variable> conflictingVariables = childNonProjectedVariables.stream()
                    .filter(allVariables::contains)
                    .collect(ImmutableCollectors.toSet());


            if (!conflictingVariables.isEmpty()) {
                throw new InvalidIntermediateQueryException("The following non-projected variables "
                        + conflictingVariables + " are appearing in different children of "+ this + ": \n"
                        + children.stream()
                            .filter(c -> c.getKnownVariables().stream()
                                    .anyMatch(conflictingVariables::contains))
                            .map(c -> "\n" + c.toString())
                            .collect(ImmutableCollectors.toList()));
            }
            allVariables.addAll(childNonProjectedVariables);
        }
    }
}
