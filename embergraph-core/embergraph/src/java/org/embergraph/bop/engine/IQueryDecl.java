package org.embergraph.bop.engine;

import java.util.UUID;

import org.embergraph.bop.PipelineOp;

/**
 * A query declaration.
 */
public interface IQueryDecl {

    /**
     * The proxy for the query controller.
     */
    IQueryClient getQueryController();

    /**
     * The query identifier.
     */
    UUID getQueryId();

    /**
     * The query.
     */
    PipelineOp getQuery();

}
