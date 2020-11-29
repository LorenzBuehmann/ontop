package it.unibz.inf.ontop.model.term.functionsymbol.impl;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.model.term.ImmutableTerm;
import it.unibz.inf.ontop.model.term.TermFactory;
import it.unibz.inf.ontop.model.type.RDFDatatype;
import it.unibz.inf.ontop.model.vocabulary.XPathFunction;


public class StrBeforeSPARQLFunctionSymbolImpl extends AbstractStrBeforeOrAfterSPARQLFunctionSymbol {
    protected StrBeforeSPARQLFunctionSymbolImpl(RDFDatatype xsdStringType) {
        super("SP_STRBEFORE", XPathFunction.SUBSTRING_BEFORE, xsdStringType);
    }

    @Override
    protected ImmutableTerm computeLexicalTerm(ImmutableList<ImmutableTerm> subLexicalTerms,
                                               ImmutableList<ImmutableTerm> typeTerms, TermFactory termFactory, ImmutableTerm returnedTypeTerm) {
        return termFactory.getIfThenElse(
                termFactory.getDBIsStringEmpty(subLexicalTerms.get(1)),
                termFactory.getDBStringConstant(""),
                computeLexicalTermWhenSecondArgIsNotEmpty(subLexicalTerms, termFactory));
    }

    @Override
    protected ImmutableTerm computeLexicalTermWhenSecondArgIsNotEmpty(ImmutableList<ImmutableTerm> subLexicalTerms,
                                                                      TermFactory termFactory) {
        return termFactory.getDBStrBefore(subLexicalTerms.get(0), subLexicalTerms.get(1));
    }
}
