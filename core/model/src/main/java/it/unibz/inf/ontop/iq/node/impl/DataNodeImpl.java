package it.unibz.inf.ontop.iq.node.impl;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.injection.IntermediateQueryFactory;
import it.unibz.inf.ontop.iq.impl.IQTreeTools;
import it.unibz.inf.ontop.model.atom.AtomPredicate;
import it.unibz.inf.ontop.model.atom.DataAtom;
import it.unibz.inf.ontop.model.term.Variable;
import it.unibz.inf.ontop.iq.IntermediateQuery;
import it.unibz.inf.ontop.utils.ImmutableCollectors;

import javax.annotation.Nullable;

/**
 *
 */
public abstract class DataNodeImpl<P extends AtomPredicate> extends LeafIQTreeImpl {

    private DataAtom<P> atom;

    // LAZY
    @Nullable
    private ImmutableSet<Variable> variables;

    protected DataNodeImpl(DataAtom<P> atom, IQTreeTools iqTreeTools, IntermediateQueryFactory iqFactory) {
        super(iqTreeTools, iqFactory);
        this.atom = atom;
        this.variables = null;
    }

    public DataAtom<P> getProjectionAtom() {
        return atom;
    }


    @Override
    public ImmutableSet<Variable> getVariables() {
        return getLocalVariables();
    }

    @Override
    public synchronized ImmutableSet<Variable> getLocalVariables() {
        if (variables == null) {
            variables = atom.getArguments()
                    .stream()
                    .filter(Variable.class::isInstance)
                    .map(Variable.class::cast)
                    .collect(ImmutableCollectors.toSet());
        }
        return variables;
    }

    @Override
    public ImmutableSet<Variable> getLocallyRequiredVariables() {
        return ImmutableSet.of();
    }

    @Override
    public ImmutableSet<Variable> getLocallyDefinedVariables() {
        return getLocalVariables();
    }

    @Override
    public ImmutableSet<Variable> getRequiredVariables(IntermediateQuery query) {
        return getLocallyRequiredVariables();
    }

    @Override
    public ImmutableSet<Variable> getKnownVariables() {
        return getLocalVariables();
    }

    @Override
    public boolean isDeclaredAsEmpty() {
        return false;
    }
}
