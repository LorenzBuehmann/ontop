package it.unibz.inf.ontop.model.type.impl;

import it.unibz.inf.ontop.model.Predicate;
import it.unibz.inf.ontop.model.type.LanguageTag;
import it.unibz.inf.ontop.model.type.TermType;

import java.util.Optional;

import static it.unibz.inf.ontop.model.Predicate.COL_TYPE.LITERAL_LANG;

/**
 * TODO: integrate into a factory
 */
public class TermTypeImpl implements TermType {

    private final Predicate.COL_TYPE colType;
    private final Optional<LanguageTag> optionalLangTag;

    /**
     * Only for langString WHEN the languageTag is known.
     *
     * It may indeed appear that the languageTag is not known
     * at query reformulation time because this information
     * is stored in a DB column.
     *
     */
    public TermTypeImpl(LanguageTag languageTag) {
        this.colType = LITERAL_LANG;
        this.optionalLangTag = Optional.of(languageTag);
    }

    /**
     * If you know the language tag, use the other constructor.
     */
    public TermTypeImpl(Predicate.COL_TYPE colType) {
        this.colType = colType;
        this.optionalLangTag = Optional.empty();
    }

    @Override
    public Predicate.COL_TYPE getColType() {
        return colType;
    }

    @Override
    public Optional<LanguageTag> getLanguageTag() {
        return optionalLangTag;
    }

    @Override
    public boolean isCompatibleWith(Predicate.COL_TYPE moreGeneralType) {
        return TermTypeInferenceTools.getCommonDenominatorType(colType, moreGeneralType)
                .map(t -> t == moreGeneralType)
                .orElse(false);
    }

    @Override
    public Optional<TermType> getCommonDenominator(TermType otherTermType) {

        /**
         * TODO: explain
         */
        if (colType == LITERAL_LANG && otherTermType.getColType() == LITERAL_LANG) {
            Optional<LanguageTag> newOptionalLangTag = optionalLangTag
                    .flatMap(tag1 -> otherTermType.getLanguageTag()
                            .flatMap(tag1::getCommonDenominator));

            return Optional.of(newOptionalLangTag
                    .map(TermTypeImpl::new)
                    .orElseGet(()-> new TermTypeImpl(LITERAL_LANG)));
        }
        else {
            return TermTypeInferenceTools.getCommonDenominatorType(colType, otherTermType.getColType())
                    .map(TermTypeImpl::new);
        }
    }

    /**
     * As defined in https://www.w3.org/TR/sparql11-query/#operandDataTypes
     */
    @Override
    public boolean isNumeric() {
        switch (colType) {
            case INTEGER:
            case DECIMAL:
            case DOUBLE:
            case LONG:
            case FLOAT:
            case NEGATIVE_INTEGER:
            case NON_NEGATIVE_INTEGER:
            case POSITIVE_INTEGER:
            case NON_POSITIVE_INTEGER:
            case INT:
            case UNSIGNED_INT:
                return true;
            default:
                return false;
        }
    }

    @Override
    public int hashCode() {
        return colType.hashCode() + optionalLangTag.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return Optional.ofNullable(other)
                .filter(o -> (o instanceof TermType))
                .map(o -> (TermType) o)
                .filter(o -> colType == o.getColType())
                .filter(o -> optionalLangTag
                        .map(tag1 -> o.getLanguageTag()
                                .map(tag1::equals)
                                .orElse(false))
                        .orElseGet(() -> !o.getLanguageTag().isPresent()))
                .isPresent();
    }

    @Override
    public String toString() {
        // TODO: Should we print the IRI of the datatypes instead (when possible)?
        return colType + optionalLangTag.map(tag -> "@" + tag).orElse("");
    }
}
