package it.unibz.inf.ontop.injection;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableMap;
import it.unibz.inf.ontop.spec.mapping.parser.TargetQueryParser;

public interface TargetQueryParserFactory {

    TargetQueryParser createParser(ImmutableMap<String, String> prefixes);
}
