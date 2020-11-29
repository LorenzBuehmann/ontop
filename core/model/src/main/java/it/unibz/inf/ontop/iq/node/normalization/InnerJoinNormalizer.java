package it.unibz.inf.ontop.iq.node.normalization;

import it.unibz.inf.ontop.com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.iq.IQProperties;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.iq.node.InnerJoinNode;
import it.unibz.inf.ontop.utils.VariableGenerator;

public interface InnerJoinNormalizer {

    IQTree normalizeForOptimization(InnerJoinNode innerJoinNode, ImmutableList<IQTree> children, VariableGenerator variableGenerator,
                                    IQProperties currentIQProperties);
}
