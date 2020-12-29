package it.unibz.inf.ontop.substitution.impl;

import it.unibz.inf.ontop.com.google.common.base.Joiner;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableMap;
import it.unibz.inf.ontop.com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.model.atom.AtomFactory;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.substitution.ImmutableSubstitution;
import it.unibz.inf.ontop.substitution.SubstitutionFactory;
import it.unibz.inf.ontop.substitution.Var2VarSubstitution;

import java.util.Map;

/**
 * Immutable {@code  Variable --> Variable } substitution.
 */
public class Var2VarSubstitutionImpl extends AbstractImmutableSubstitutionImpl<Variable> implements Var2VarSubstitution {

    private final ImmutableMap<Variable, Variable> map;

    /**
     * Regular constructor
     */
    protected Var2VarSubstitutionImpl(ImmutableMap<Variable, ? extends Variable> substitutionMap, AtomFactory atomFactory,
                                      TermFactory termFactory, SubstitutionFactory substitutionFactory) {
        super(atomFactory, termFactory, substitutionFactory);
        this.map = (ImmutableMap)substitutionMap;
    }

    @Override
    public Variable applyToVariable(Variable variable) {
        Variable r = map.get(variable);
        return r == null ? variable : r;
    }

    @Override
    public <T extends ImmutableTerm> ImmutableSubstitution<T> applyToTarget(ImmutableSubstitution<T> otherSubstitution) {
        ImmutableMap.Builder<Variable, T> mapBuilder = ImmutableMap.builder();

        for (Map.Entry<Variable, T> otherEntry : otherSubstitution.getImmutableMap().entrySet()) {
            T newValue = applyToTerm(otherEntry.getValue());
            if (!otherEntry.getKey().equals(newValue))
                mapBuilder.put(otherEntry.getKey(), newValue);
        }
        return substitutionFactory.getSubstitution(mapBuilder.build());
    }


    @Override
    public Variable get(Variable var) {
        return map.get(var);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public String toString() {
        return Joiner.on(", ").withKeyValueSeparator("/").join(map);
    }

    @Override
    public ImmutableMap<Variable, Variable> getImmutableMap() {
        return map;
    }

    @Override
    public boolean isDefining(Variable variable) {
        return map.containsKey(variable);
    }

    @Override
    public ImmutableSet<Variable> getDomain() {
        return map.keySet();
    }

    @Override
    protected ImmutableSubstitution<Variable> constructNewSubstitution(ImmutableMap<Variable, Variable> map) {
        return substitutionFactory.getVar2VarSubstitution(map);
    }

    @Override
    public NonGroundTerm applyToNonGroundTerm(NonGroundTerm term) {
        if (term instanceof Variable)
            return applyToVariable((Variable)term);
        else
            return (NonGroundTerm) applyToFunctionalTerm((ImmutableFunctionalTerm) term);
    }

    @Override
    public <T extends ImmutableTerm> T applyToTerm(T term) {
        return (T) super.apply(term);
    }

}
