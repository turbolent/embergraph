package org.embergraph.service.ndx.pipeline;

/*
 * An operation which can be mapped across subtasks.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <S>
 */
public interface SubtaskOp<S extends AbstractSubtask> {

  void call(S s);
}
