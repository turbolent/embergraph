package org.embergraph.rdf.load;

/** A factory for {@link Runnable} tasks. */
public interface ITaskFactory<T extends Runnable> {

  T newTask(String file);
}
