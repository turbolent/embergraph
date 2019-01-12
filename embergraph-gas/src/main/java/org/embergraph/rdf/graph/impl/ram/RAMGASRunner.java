package org.embergraph.rdf.graph.impl.ram;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.embergraph.rdf.graph.IGASEngine;
import org.embergraph.rdf.graph.IGraphAccessor;
import org.embergraph.rdf.graph.impl.ram.RAMGASEngine.RAMGraph;
import org.embergraph.rdf.graph.impl.ram.RAMGASEngine.RAMGraphAccessor;
import org.embergraph.rdf.graph.impl.util.GASRunnerBase;

/**
 * Class for running GAS performance tests against the SAIL.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class RAMGASRunner<VS, ES, ST> extends GASRunnerBase<VS, ES, ST> {

  private static final Logger log = Logger.getLogger(RAMGASRunner.class);

  public RAMGASRunner(String[] args) throws ClassNotFoundException {
    super(args);
  }

  protected class RAMOptionData extends GASRunnerBase<VS, ES, ST>.OptionData {

    private final RAMGraph g = new RAMGraph();

    public RAMGraph getGraph() {
      synchronized (g) {
        /*
         * Note: Synchronization pattern is intended to provide
         * visibility for graph traversal following a load of data into
         * the graph.
         */
        return g;
      }
    }

    @Override
    public void init() throws Exception {

      super.init();
    }

    @Override
    public void shutdown() {}

    @Override
    public boolean handleArg(final AtomicInteger i, final String[] args) {
      return super.handleArg(i, args);
      //            final String arg = args[i.get()];
      //            if (arg.equals("-bufferMode")) {
      //                final String s = args[i.incrementAndGet()];
      //                bufferModeOverride = BufferMode.valueOf(s);
      //            } else if (arg.equals("-namespace")) {
      //                final String s = args[i.incrementAndGet()];
      //                namespaceOverride = s;
      //            } else {
      //                return false;
      //            }
    }

    @Override
    public void report(final StringBuilder sb) {
      // NOP
    }
  } // class SAILOptionData

  @Override
  protected RAMOptionData newOptionData() {

    return new RAMOptionData();
  }

  @Override
  protected IGASEngine newGASEngine() {

    return new RAMGASEngine(getOptionData().nthreads);
  }

  @Override
  protected void loadFiles() throws Exception {

    final RAMOptionData opt = getOptionData();

    final String[] resources = opt.loadSet.toArray(new String[0]);

    new RAMGraphLoader(opt.getGraph()).loadGraph(null /* fallback */, resources);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected RAMOptionData getOptionData() {

    return (RAMOptionData) super.getOptionData();
  }

  @Override
  protected IGraphAccessor newGraphAccessor() {

    return new RAMGraphAccessor(getOptionData().g);
  }

  /**
   * Performance testing harness.
   *
   * @see #GASRunner(String[])
   */
  @SuppressWarnings("rawtypes")
  public static void main(final String[] args) throws Exception {

    new RAMGASRunner(args).call();
  }
}
