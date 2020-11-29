package it.unibz.inf.ontop.model.term.functionsymbol.db.impl;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.iq.node.VariableNullability;
import it.unibz.inf.ontop.model.term.*;
import it.unibz.inf.ontop.model.term.functionsymbol.db.DBFunctionSymbolSerializer;
import it.unibz.inf.ontop.model.type.DBTermType;
import it.unibz.inf.ontop.utils.VariableGenerator;

import java.util.Optional;

public class NullRejectingDBConcatFunctionSymbol extends AbstractDBConcatFunctionSymbol {


    protected NullRejectingDBConcatFunctionSymbol(String nameInDialect, int arity, DBTermType dbStringType,
                                                  DBTermType rootDBTermType, boolean isOperator) {
        super(nameInDialect, arity, dbStringType, rootDBTermType,
                isOperator
                        ? Serializers.getOperatorSerializer(nameInDialect)
                        : Serializers.getRegularSerializer(nameInDialect));
    }

    protected NullRejectingDBConcatFunctionSymbol(String nameInDialect, int arity, DBTermType dbStringType,
                                                  DBTermType rootDBTermType, DBFunctionSymbolSerializer serializer) {
        super(nameInDialect, arity, dbStringType, rootDBTermType, serializer);
    }

    @Override
    public boolean isAlwaysInjectiveInTheAbsenceOfNonInjectiveFunctionalTerms() {
        return false;
    }

    /**
     * TODO: allow post-processing
     */
    @Override
    public boolean canBePostProcessed(ImmutableList<? extends ImmutableTerm> arguments) {
        return false;
    }
}
