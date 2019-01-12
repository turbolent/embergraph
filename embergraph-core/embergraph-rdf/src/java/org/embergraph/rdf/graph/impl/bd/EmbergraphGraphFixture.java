package org.embergraph.rdf.graph.impl.bd;

import java.util.Properties;
import org.embergraph.rdf.graph.IGraphAccessor;
import org.embergraph.rdf.graph.impl.bd.EmbergraphGASEngine.EmbergraphGraphAccessor;
import org.embergraph.rdf.graph.util.AbstractGraphFixture;
import org.embergraph.rdf.graph.util.SailGraphLoader;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.ValueFactory;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

public class EmbergraphGraphFixture extends AbstractGraphFixture {

  private final EmbergraphSail sail;

  public EmbergraphGraphFixture(final Properties properties) throws SailException {

    sail = new EmbergraphSail(properties);

    sail.initialize();
  }

  public EmbergraphGraphFixture(final AbstractTripleStore kb) throws SailException {

    sail = new EmbergraphSail(kb);

    sail.initialize();
  }

  @Override
  public EmbergraphSail getSail() {

    return sail;
  }

  @Override
  public void destroy() throws Exception {

    if (sail.isOpen()) {

      sail.shutDown();
    }

    if (sail instanceof EmbergraphSail) {

      ((EmbergraphSail) sail).__tearDownUnitTest();
    }
  }

  @Override
  protected SailGraphLoader newSailGraphLoader(SailConnection cxn) {

    return new EmbergraphSailGraphLoader(cxn);
  }

  @Override
  public EmbergraphGASEngine newGASEngine(final int nthreads) {

    return new EmbergraphGASEngine(sail, nthreads);
  }

  @Override
  public IGraphAccessor newGraphAccessor(final SailConnection ignored) {

    return new EmbergraphGraphAccessor(sail.getIndexManager());
  }

  public static class EmbergraphSailGraphLoader extends SailGraphLoader {

    private final ValueFactory valueFactory;

    public EmbergraphSailGraphLoader(SailConnection cxn) {

      super(cxn);

      // Note: Needed for RDR.
      this.valueFactory = ((EmbergraphSailConnection) cxn).getEmbergraphSail().getValueFactory();
    }

    @Override
    protected ValueFactory getValueFactory() {

      return valueFactory;
    }
  }
}
