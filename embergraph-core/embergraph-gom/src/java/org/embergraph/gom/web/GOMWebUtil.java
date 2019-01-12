package org.embergraph.gom.web;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.embergraph.gom.om.ObjectManager;
import org.embergraph.journal.ITx;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSailRepository;
import org.embergraph.rdf.sail.webapp.EmbergraphRDFContext;
import org.embergraph.rdf.sail.webapp.EmbergraphRDFServlet;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.repository.RepositoryException;

/*
* A simple utility class that can be initialized with a ServletRequest and used to simplify dynamic
 * web pages.
 *
 * @author Martyn Cutcher
 */
public class GOMWebUtil {

  /*
   * The name of the request attribute to which we attach the {@link ObjectManager} scoped to that
   * request.
   */
  private static final String ATTRIBUTE_OM = ObjectManager.class.getName();

  /*
   * Return an {@link ITx#UNISOLATED} {@link ObjectManager} instance that is scoped to the <code>
   * request</code>. The same {@link ObjectManager} instance will be returned each time this method
   * is invoked for the same <code>request</code>. A distinct {@link ObjectManager} instance is
   * returned for each distinct <code>request</code>.
   *
   * @param request The request.
   * @return The {@link ObjectManager}.
   * @throws Exception
   */
  public static ObjectManager getObjectManager(final HttpServletRequest request) throws Exception {

    return newObjectManager(request, ITx.UNISOLATED);
  }

  /*
   * Return an {@link ObjectManager} instance that is scoped to the <code>request</code>. The same
   * {@link ObjectManager} instance will be returned each time this method is invoked for the same
   * <code>request</code>. A distinct {@link ObjectManager} instance is returned for each distinct
   * <code>request</code>.
   *
   * @param request The request.
   * @param timestamp The timestamp of the view.
   * @return The {@link ObjectManager}.
   * @throws Exception
   */
  public static ObjectManager newObjectManager(
      final HttpServletRequest request, final long timestamp) throws RepositoryException {

    ObjectManager om = (ObjectManager) request.getAttribute(ATTRIBUTE_OM);

    if (om != null) {

      return om;
    }

    final ServletContext servletContext = request.getSession().getServletContext();

    final EmbergraphRDFContext rdfContext = getEmbergraphRDFContext(servletContext);

    // The SPARQL endpoint.
    final String endpoint = request.getRequestURL().toString();

    final String namespace = rdfContext.getConfig().namespace;

    final AbstractTripleStore tripleStore =
        (AbstractTripleStore)
            rdfContext.getIndexManager().getResourceLocator().locate(namespace, timestamp);

    if (tripleStore == null) {

      throw new RuntimeException("Not found: namespace=" + namespace);
    }

    // Wrap with SAIL.
    final EmbergraphSail sail = new EmbergraphSail(tripleStore);

    final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);

    repo.initialize();

    om = new ObjectManager(endpoint, repo);

    request.setAttribute(ATTRIBUTE_OM, om);

    return om;
  }

  protected static final EmbergraphRDFContext getEmbergraphRDFContext(
      final ServletContext servletContext) {

    return getRequiredServletContextAttribute(
        servletContext, EmbergraphRDFServlet.ATTRIBUTE_RDF_CONTEXT);
  }

  protected static <T> T getRequiredServletContextAttribute(
      final ServletContext servletContext, final String name) {

    @SuppressWarnings("unchecked")
    final T v = (T) servletContext.getAttribute(name);

    if (v == null) throw new RuntimeException("Not set: " + name);

    return v;
  }
}
