/*
Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Jul 26, 2011
 */

package org.embergraph.rdf.sail;

import info.aduna.iteration.CloseableIteration;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import org.apache.log4j.Logger;
import org.embergraph.Banner;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.Journal;
import org.embergraph.rawstore.IRawStore;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.relation.RelationSchema;
import org.embergraph.relation.locator.ILocatableResource;
import org.embergraph.sparse.ITPS;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.sail.SailException;

/*
 * Utility class for exporting the configuration properties and data associated with one or more KBs
 * on a {@link Journal}.
 *
 * @see <a href="https://sourceforge.net/apps/mediawiki/bigdata/index.php?title=DataMigration">Data
 *     Migration</a>.
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class ExportKB {

  private static final Logger log = Logger.getLogger(ExportKB.class);

  /** The connection that will be used to export the data. */
  private final EmbergraphSailConnection conn;

  //    /*
  //     * The KB to be exported.
  //     */
  //    private final AbstractTripleStore kb;

  /** The namespace associated with that KB. */
  private final String namespace;

  /** The directory into which the KB properties and data will be written. */
  private final File kbdir;

  /** The {@link RDFFormat} which will be used when the data are exported. */
  private final RDFFormat format;

  /*
   * When <code>true</code> inferences and axioms will also be exported. Otherwise just the
   * explicitly given (aka told) triples/quads will be exported.
   */
  private final boolean includeInferred;

  /*
   * @param conn The connection.
   * @param kbdir The directory into which the exported properties and RDF data will be written.
   * @param format The {@link RDFFormat} to use when exporting the data.
   * @param includeInferred When <code>true</code> inferences and axioms will also be exported.
   *     Otherwise just the explicitly given (aka told) triples/quads will be exported.
   */
  public ExportKB(
      final EmbergraphSailConnection conn,
      final File kbdir,
      final RDFFormat format,
      final boolean includeInferred) {

    if (conn == null) throw new IllegalArgumentException("KB not specified.");

    if (kbdir == null) throw new IllegalArgumentException("Output directory not specified.");

    if (format == null) throw new IllegalArgumentException("RDFFormat not specified.");

    final AbstractTripleStore kb = conn.getTripleStore();

    if (kb.isStatementIdentifiers() && !RDFFormat.RDFXML.equals(format))
      throw new IllegalArgumentException("SIDs mode requires RDF/XML interchange.");

    if (kb.isQuads() && !format.supportsContexts())
      throw new IllegalArgumentException("RDFFormat does not support quads: " + format);

    this.conn = conn;

    //        this.kb = kb;

    this.namespace = kb.getNamespace();

    this.kbdir = kbdir;

    this.format = format;

    this.includeInferred = includeInferred;
  }

  /*
   * Munge a name index so that it is suitable for use in a filesystem. In particular, any non-word
   * characters are converted to an underscore character ("_"). This gets rid of all punctuation
   * characters and whitespace in the index name itself, but will not translate unicode characters.
   *
   * @param s The name of the scale-out index.
   * @return A string suitable for inclusion in a filename.
   */
  private static String munge(final String s) {

    return s.replaceAll("[\\W]", "_");
  }

  /*
   * Export the properties and data for the KB.
   *
   * @throws IOException
   * @throws SailException
   * @throws RDFHandlerException
   */
  public void export() throws IOException, SailException, RDFHandlerException {

    System.out.println("Effective output directory: " + kbdir);

    prepare();

    exportProperties();

    exportData();
  }

  public void prepare() throws IOException {
    if (!kbdir.exists()) {
      if (!kbdir.mkdirs()) throw new IOException("Could not create directory: " + kbdir);
    }
  }

  /*
   * Export the configuration properties for the kb.
   *
   * @throws IOException
   */
  public void exportProperties() throws IOException {
    prepare();
    final AbstractTripleStore kb = conn.getTripleStore();
    // Prepare a comment block for the properties file.
    final StringBuilder comments = new StringBuilder("Configuration properties.\n");
    if (kb.getIndexManager() instanceof IRawStore) {
      comments.append("source=").append(((IRawStore) kb.getIndexManager()).getFile()).append("\n");
      comments.append("namespace=").append(namespace).append("\n");
      // The timestamp of the KB view.
      comments.append("timestamp=").append(kb.getTimestamp()).append("\n");
      // The date and time when the KB export began. (Automatically added by Java).
      //            comments.append("exportDate=" + new Date() + "\n");
      // The approximate #of statements (includes axioms, inferences, and
      // deleted statements).
      comments.append("fastStatementCount=").append(kb.getStatementCount(false /* exact */))
          .append("\n");
      // The #of URIs in the lexicon indices.
      comments.append("uriCount=").append(kb.getURICount()).append("\n");
      // The #of Literals in the lexicon indices.
      comments.append("literalCount=").append(kb.getLiteralCount()).append("\n");
      // The #of blank nodes in the lexicon indices.
      comments.append("bnodeCount=").append(kb.getBNodeCount()).append("\n");
    }
    // Flatten the properties so inherited defaults will also be written
    // out.
    final Properties properties = flatCopy(kb.getProperties());
    // Write the properties file.
    final File file = new File(kbdir, "kb.properties");
    System.out.println("Writing " + file);
    final OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
    try {
      properties.store(os, comments.toString());
    } finally {
      os.close();
    }
  }

  /*
   * Exports all told statements associated with the last commit point for the KB.
   *
   * @throws IOException
   * @throws SailException
   * @throws RDFHandlerException
   */
  public void exportData() throws IOException, SailException, RDFHandlerException {
    prepare();
    //        final EmbergraphSail sail = new EmbergraphSail(kb);
    //        try {
    //            sail.initialize();
    //            final SailConnection conn = sail.getReadOnlyConnection();
    //            try {
    final CloseableIteration<? extends Statement, SailException> itr =
        conn.getStatements(
            null /* s */, null /* p */, null /* o */, includeInferred
            /* contexts */ );
    try {
      final File file = new File(kbdir, "data." + format.getDefaultFileExtension() + ".gz");
      System.out.println("Writing " + file);
      final OutputStream os = new GZIPOutputStream(new FileOutputStream(file));
      try {
        final RDFWriter writer = RDFWriterRegistry.getInstance().get(format).getWriter(os);
        writer.startRDF();
        while (itr.hasNext()) {
          final Statement stmt = itr.next();
          writer.handleStatement(stmt);
        }
        writer.endRDF();
      } finally {
        os.close();
      }
    } finally {
      itr.close();
    }
    //            } finally {
    //                conn.close();
    //            }
    //        } finally {
    //            sail.shutDown();
    //        }

  }

  /*
   * Return a list of the namespaces for the {@link AbstractTripleStore}s registered against the
   * embergraph instance.
   */
  static List<String> getNamespaces(final IIndexManager indexManager) {

    // the triple store namespaces.
    final List<String> namespaces = new LinkedList<>();

    // scan the relation schema in the global row store.
    @SuppressWarnings("unchecked")
    final Iterator<ITPS> itr =
        (Iterator<ITPS>) indexManager.getGlobalRowStore().rangeIterator(RelationSchema.INSTANCE);

    while (itr.hasNext()) {

      // A timestamped property value set is a logical row with
      // timestamped property values.
      final ITPS tps = itr.next();

      // If you want to see what is in the TPS, uncomment this.
      //          System.err.println(tps.toString());

      // The namespace is the primary key of the logical row for the
      // relation schema.
      final String namespace = (String) tps.getPrimaryKey();

      // Get the name of the implementation class
      // (AbstractTripleStore, SPORelation, LexiconRelation, etc.)
      final String className = (String) tps.get(RelationSchema.CLASS).getValue();

      try {
        final Class<?> cls = Class.forName(className);
        if (AbstractTripleStore.class.isAssignableFrom(cls)) {
          // this is a triple store (vs something else).
          namespaces.add(namespace);
        }
      } catch (ClassNotFoundException e) {
        log.error(e, e);
      }
    }

    return namespaces;
  }

  /*
   * Load a {@link Properties} object from a file.
   *
   * @param file The property file.
   * @return The {@link Properties}.
   * @throws IOException
   */
  private static Properties loadProperties(final File file) throws IOException {

    final Properties p = new Properties();

    final InputStream is = new BufferedInputStream(new FileInputStream(file));

    try {

      p.load(is);

    } finally {

      is.close();
    }

    return p;
  }

  public static Properties flatCopy(final Properties props) {

    final Properties tmp = new Properties();

    tmp.putAll(flatten(props));

    return tmp;
  }

  private static Map<String, String> flatten(final Properties properties) {

    if (properties == null) {

      throw new IllegalArgumentException();
    }

    final Map<String, String> out = new LinkedHashMap<>();

    final Enumeration<?> e = properties.propertyNames();

    while (e.hasMoreElements()) {

      final String property = (String) e.nextElement();

      final String propertyValue = properties.getProperty(property);

      if (propertyValue != null) out.put(property, propertyValue);
    }

    return out;
  }

  /*
   * Export one or more KBs from a Journal. The only required argument is the name of the properties
   * file for the Journal. By default all KB instances found on the journal will be exported into
   * the current working directory. Each KB will be written into a subdirectory based on the
   * namespace of the KB.
   *
   * @param args <code>[options] propertyFile namespace*</code> where <i>options</i> is any of:
   *     <dl>
   *       <dt>-outdir
   *       <dd>The output directory (default is the current working directory)
   *       <dt>-format
   *       <dd>The {@link RDFFormat} which will be used to export the data. If not specified then an
   *           appropriate format will be selected based on the KB configuration. The default for
   *           triples or SIDs is {@link RDFFormat#RDFXML}. The default for quads is {@link
   *           RDFFormat#TRIX}.
   *       <dt>-includeInferred
   *       <dd>Normally only the told triples/quads will be exported. This option may be given to
   *           export the axioms and inferences as well as the told triples/quads.
   *       <dt>-n
   *       <dd>Do nothing, but show the KBs which would be exported.
   *       <dt>-help
   *       <dd>Display the usage message and exit.
   *     </dl>
   *     where <i>propertyFile</i> is the properties file for the Journal.<br>
   *     where <i>namespace</i> is zero or more namespaces of KBs to export from the Journal. If no
   *     namespace is given, then all KBs on the Journal will be exported.
   * @throws Exception
   */
  public static void main(final String[] args) throws Exception {

    Banner.banner();

    /*
     * Defaults for options.
     */
    boolean nothing = false;
    boolean includeInferred = false;
    RDFFormat format = null;
    File propertyFile = null;
    File outdir = new File(".");
    final List<String> namespaces = new LinkedList<>();

    // Parse options.
    int i = 0;
    for (; i < args.length; ) {
      final String s = args[i];
      if (!s.startsWith("-")) {
        // end of options.
        break;
      }
      i++;
      if (s.equals("-n")) {
        nothing = true;
      } else if (s.equals("-help")) {
        usage();
        System.exit(0);
      } else if (s.equals("-format")) {
        format = RDFFormat.valueOf(args[i++]);
      } else if (s.equals("-includeInferred")) {
        includeInferred = true;
      } else if (s.equals("-outdir")) {
        outdir = new File(args[i++]);
      } else {
        System.err.println("Unknown option: " + s);
        usage();
        System.exit(1);
      }
    }

    // properties file.
    if (i == args.length) {
      usage();
      System.exit(1);
    } else {
      propertyFile = new File(args[i++]);
      if (!propertyFile.exists()) {
        System.err.println("No such file: " + propertyFile);
        System.exit(1);
      }
    }

    // Load the properties from the file.
    final Properties properties = loadProperties(propertyFile);

    /*
     * Allow override of select options.
     */
    {
      final String[] overrides =
          new String[] {
            // Journal options.
            org.embergraph.journal.Options.FILE,
          };
      for (String s : overrides) {
        if (System.getProperty(s) != null) {
          // Override/set from the environment.
          final String v = System.getProperty(s);
          System.out.println("Using: " + s + "=" + v);
          properties.setProperty(s, v);
        }
      }
    }

    // Open the journal.
    final Journal indexManager = new Journal(properties);
    try {

      // The last commit time on the store.
      final long commitTime = indexManager.getLastCommitTime();

      if (i == args.length) {
        // Use all namespaces.
        namespaces.addAll(getNamespaces(indexManager));
      } else {
        // use just the given namespace(s).
        for (; i < args.length; ) {
          final String namespace = args[i++];
          // Verify that the KB exists.
          final ILocatableResource<?> kb =
              indexManager.getResourceLocator().locate(namespace, commitTime);
          if (kb == null) {
            throw new RuntimeException("No such namespace: " + namespace);
          }
          if (!(kb instanceof AbstractTripleStore)) {
            throw new RuntimeException("Not a KB: " + namespace);
          }
          namespaces.add(namespace);
        }
      }

      for (String namespace : namespaces) {

        //                // Get KB view.
        //                final AbstractTripleStore kb = (AbstractTripleStore) indexManager
        //                        .getResourceLocator().locate(namespace, commitTime);

        final EmbergraphSail sail = new EmbergraphSail(namespace, indexManager);
        try {

          sail.initialize();

          final EmbergraphSailConnection conn = sail.getReadOnlyConnection(commitTime);

          try {

            // The name of the subdirectory on which the properties and RDF
            // data will be written.
            final File kbdir = new File(outdir, munge(namespace));

            // Choose an appropriate RDFFormat.
            RDFFormat fmt = format;
            if (fmt == null) {
              // Choose an appropriate format.
              if (conn.getTripleStore().isStatementIdentifiers()) {
                fmt = RDFFormat.RDFXML;
              } else if (conn.isQuads()) {
                fmt = RDFFormat.TRIX;
              } else {
                fmt = RDFFormat.RDFXML;
              }
            }
            System.out.println("Exporting " + namespace + " as " + fmt.getName() + " on " + kbdir);
            if (!nothing) {
              // Export KB.
              new ExportKB(conn, kbdir, fmt, includeInferred).export();
            }

          } finally {

            conn.close();
          }
        } finally {
          sail.shutDown();
        }
      }

      // Success.
      System.out.println("Done");

    } finally {
      indexManager.close();
    }
  }

  private static void usage() {

    System.err.println("usage: [options] propertyFile namespace*");
  }
}
