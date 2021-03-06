package org.embergraph.rdf.internal;

/*
 * Exception thrown by {@link IV#getValue()} if the {@link IV} has not first been cached using
 * {@link IV#asValue(LexiconRelation)}.
 *
 * @author thompsonbry
 * @see IV#getValue()
 */
public class NotMaterializedException extends RuntimeException {

  /** */
  private static final long serialVersionUID = 1L;

  public NotMaterializedException() {}

  public NotMaterializedException(String message) {
    super(message);
  }

  public NotMaterializedException(Throwable cause) {
    super(cause);
  }

  public NotMaterializedException(String message, Throwable cause) {
    super(message, cause);
  }
}
