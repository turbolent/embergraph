package org.embergraph.gom;

import java.io.IOException;
import java.net.URL;
import org.embergraph.gom.om.IObjectManager;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

public interface IGOMProxy {

  IObjectManager getObjectManager();

  ValueFactory getValueFactory();

  void proxySetup() throws Exception;

  void proxyTearDown() throws Exception;

  void load(URL n3, RDFFormat n32) throws IOException, RDFParseException, RepositoryException;
}
