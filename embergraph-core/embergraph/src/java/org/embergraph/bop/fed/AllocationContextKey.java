package org.embergraph.bop.fed;

/*
 * The allocation context key groups together allocations onto the same direct {@link ByteBuffer}s.
 * There are different implementations depending on how it makes sense to group data data for a
 * given query.
 *
 * @see IAllocationContext
 */
public abstract class AllocationContextKey {

  /*
   * Must be overridden. The queryId must be a part of each hashCode() in order to ensure that the
   * hash codes are well distributed across different queries on the same node.
   */
  @Override
  public abstract int hashCode();

  /** Must be overridden. */
  @Override
  public abstract boolean equals(Object o);

  /*
   * Return <code>true</code> if the allocation context is scoped to the specified operator.
   *
   * @param bopId The {@link BOp} identifier.
   */
  public abstract boolean hasOperatorScope(int bopId);
}
