package com.bigdata.bop.fed;

import java.util.UUID;

/**
 * An allocation context which is shared by all operators running in the
 * same query.
 */
class QueryContext extends AllocationContextKey {

    private final UUID queryId;

    QueryContext(final UUID queryId) {
        
        if (queryId == null)
            throw new IllegalArgumentException();

        this.queryId = queryId;
        
    }

    public int hashCode() {
        return queryId.hashCode();
    }

    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof QueryContext))
            return false;
        if (!queryId.equals(((QueryContext) o).queryId))
            return false;
        return true;
    }

    @Override
    public boolean hasOperatorScope(int bopId) {
        return false;
    }

}