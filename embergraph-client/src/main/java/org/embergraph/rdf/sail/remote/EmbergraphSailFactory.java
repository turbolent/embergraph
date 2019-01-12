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
package org.embergraph.rdf.sail.remote;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.embergraph.EmbergraphStatics;
import org.embergraph.rdf.sail.webapp.client.RemoteRepositoryManager;
import org.embergraph.util.httpd.Config;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;

/**
 * Helper class to create a embergraph instance.
 *
 * @author mikepersonick
 * @see <a href="http://trac.bigdata.com/ticket/1152" > EmbergraphSailFactory must be moved to the
 *     client package </a>
 */
public class EmbergraphSailFactory {

  /** The default embergraph SAIL_PROVIDER. */
  public static final String EMBERGRAPH_SAIL_INSTANCE = "org.embergraph.rdf.sail.EmbergraphSail";

  /**
   * The name of the property to set with the class that will provide the Sail.
   *
   * <p>It must have a constructor that takes a single properties file as the parameter.
   */
  public static final String SAIL_PROVIDER = "org.embergraph.rdf.sail.remote.Provider";

  /**
   * A handy list of common Options you might want to specify when creating your embergraph
   * instance.
   *
   * @author mikepersonick
   */
  public enum Option {

    /** Inference on or off. Off by default. */
    Inference,

    /** Quads on or off. Off by default. */
    Quads,

    /** RDR (statement identifiers) on or off. Off by default. */
    RDR,

    /** Text index on or off. Off by default. */
    TextIndex,

    //        /**
    //         * Create an in-memory instance.
    //         */
    //        InMemory,
    //
    //        /**
    //         * Create a persistent instance backed by a file.  You must specify
    //         * the file.
    //         */
    //        Persistent

  }

  /**
   * Connect to a remote embergraph instance.
   *
   * <p>FIXME This does not parameterize the value of the ContextPath. See {@link
   * EmbergraphStatics#getContextPath()}.
   */
  public static EmbergraphSailRemoteRepository connect(final String host, final int port) {
    return connect("http://" + host + ":" + port + "/" + Config.PATH);
  }

  /**
   * Connect to a remote embergraph instance.
   *
   * @param sparqlEndpointURL The URL of the SPARQL end point.
   *     <p>FIXME This does not support the HA load balancer pattern. See #1148.
   *     <p>FIXME This does not parameterize the value of the ContextPath. See {@link
   *     EmbergraphStatics#getContextPath()}.
   *     <p>FIXME This MIGHT leak HttpClient or Executor resources.
   */
  public static EmbergraphSailRemoteRepository connect(final String sparqlEndpointURL) {

    return new RemoteRepositoryManager()
        .getRepositoryForURL(sparqlEndpointURL)
        .getEmbergraphSailRemoteRepository();
  }

  /**
   * Convenience method to allow the testing of the URL normalization functionality.
   *
   * @see <a href="http://trac.blazegraph.com/ticket/1139">EmbergraphSailFactory.connect() </a>
   */
  @Deprecated // We are getting rid of this, right?
  public static String testServiceEndpointUrl(final String serviceEndpoint) {
    return normalizeEndpoint(serviceEndpoint);
  }

  /** Massage the service endpoint to ensure that it ends with </code>/embergraph</code> */
  @Deprecated // We are getting rid of this, right?
  private static String normalizeEndpoint(final String serviceEndpoint) {

    if (serviceEndpoint.endsWith("/sparql")) {

      return serviceEndpoint.substring(0, serviceEndpoint.length() - "/sparql".length());
    }
    if (serviceEndpoint.endsWith("/sparql/")) {

      return serviceEndpoint.substring(0, serviceEndpoint.length() - "/sparql/".length());

    } else if (serviceEndpoint.endsWith("/" + Config.PATH + "/")) {

      return serviceEndpoint.substring(0, serviceEndpoint.length() - 1);

    } else if (serviceEndpoint.endsWith("/" + Config.PATH)) {

      return serviceEndpoint;

    } else if (serviceEndpoint.contains("/" + Config.PATH) && serviceEndpoint.endsWith("/")) {
      // This is the case of /embergraph/namespace/NAMESPACE/

      return serviceEndpoint.substring(0, serviceEndpoint.length() - 1);

    } else if (serviceEndpoint.contains("/" + Config.PATH)) {
      // This is the case of /embergraph/namespace/NAMESPACE

      return serviceEndpoint;

    } else if (serviceEndpoint.endsWith("/")) {

      return serviceEndpoint + Config.PATH;

    } else {

      return serviceEndpoint + "/" + Config.PATH;
    }
  }

  /**
   * Open an existing persistent embergraph instance. If a journal does not exist at the specified
   * location then an exception will be thrown.
   */
  public static SailRepository openRepository(final String file) {

    return new SailRepository(openSail(file, false));
  }

  /**
   * Open an existing persistent embergraph instance. If a journal does not exist at the specified
   * location and the boolean create flag is true a journal will be created at that location with
   * the default set of options.
   */
  public static SailRepository openRepository(final String file, final boolean create) {

    return new SailRepository(openSail(file, create));
  }

  /**
   * Open an existing persistent embergraph instance. If a journal does not exist at the specified
   * location then an exception will be thrown.
   */
  public static Sail openSail(final String file) {

    return openSail(file, false);
  }

  /**
   * Open an existing persistent embergraph instance. If a journal does not exist at the specified
   * location and the boolean create flag is true a journal will be created at that location with
   * the default set of options.
   */
  public static Sail openSail(final String file, final boolean create) {

    if (!new File(file).exists()) {

      if (!create) {
        throw new IllegalArgumentException("journal does not exist at specified location");
      } else {
        return createSail(file);
      }

    } else {

      final Properties props = new Properties();
      props.setProperty("org.embergraph.journal.AbstractJournal.file", file);

      final Sail sail = getSailProviderInstance(props);

      return sail;
    }
  }

  /**
   * Create a new embergraph instance using the specified options. Since no journal file is
   * specified this must be an in-memory instance.
   */
  public static SailRepository createRepository(final Option... args) {

    return createRepository(new Properties(), null, args);
  }

  /**
   * Create a new embergraph instance using the specified options. Since no journal file is
   * specified this must be an in-memory instance.
   */
  public static SailRepository createRepository(final Properties props, final Option... args) {

    return createRepository(props, null, args);
  }

  /** Create a new embergraph instance using the specified options. */
  public static SailRepository createRepository(final String file, final Option... args) {

    return createRepository(new Properties(), file, args);
  }

  /**
   * Create a new embergraph instance using the specified options. Since no journal file is
   * specified this must be an in-memory instance.
   */
  public static SailRepository createRepository(
      final Properties props, final String file, final Option... args) {

    return new SailRepository(createSail(props, file, args));
  }

  /**
   * Create a new embergraph instance using the specified options. Since no journal file is
   * specified this must be an in-memory instance.
   */
  public static Sail createSail(final Option... args) {

    return createSail(new Properties(), null, args);
  }

  /** Create a new embergraph instance using the specified options and filename. */
  public static Sail createSail(final String file, final Option... args) {

    // Ticket #1185: EmbergraphGraphFactory create not working.

    return createSail(new Properties(), file, args);
  }

  /** Create a new embergraph instance using the specified options. */
  public static Sail createSail(final Properties props, final String file, final Option... args) {

    final List<Option> options = args != null ? Arrays.asList(args) : new LinkedList<Option>();

    checkArgs(file, options);

    //        final Properties props = new Properties();
    // FIXME:  Changed these to String Values to remove package / artifact dependency
    /*
    if (file != null) {
        props.setProperty(EmbergraphSail.Options.FILE, file);
        props.setProperty(Journal.Options.BUFFER_MODE, BufferMode.DiskRW.toString());
    } else {
        props.setProperty(Journal.Options.BUFFER_MODE, BufferMode.MemStore.toString());
    }

    if (options.contains(Option.Inference)) {
        props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, OwlAxioms.class.getName());
        props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "true");
        props.setProperty(EmbergraphSail.Options.JUSTIFY, "true");
    } else {
        props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
        props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
    }

    props.setProperty(EmbergraphSail.Options.TEXT_INDEX,
            String.valueOf(options.contains(Option.TextIndex)));

    props.setProperty(EmbergraphSail.Options.STATEMENT_IDENTIFIERS,
            String.valueOf(options.contains(Option.RDR)));

    props.setProperty(EmbergraphSail.Options.QUADS,
            String.valueOf(options.contains(Option.Quads)));
    */

    if (file != null) {
      props.setProperty("org.embergraph.journal.AbstractJournal.file", file);
      props.setProperty("org.embergraph.journal.AbstractJournal.bufferMode", "DiskRW");
    } else {
      props.setProperty("org.embergraph.journal.AbstractJournal.bufferMode", "MemStore");
    }
    if (options.contains(Option.Inference)) {
      props.setProperty(
          "org.embergraph.rdf.store.AbstractTripleStore.axiomsClass",
          "org.embergraph.rdf.axioms.OwlAxioms");
      props.setProperty("org.embergraph.rdf.sail.truthMaintenance", "true");
      props.setProperty("org.embergraph.rdf.store.AbstractTripleStore.justify", "true");
    } else {
      props.setProperty(
          "org.embergraph.rdf.store.AbstractTripleStore.axiomsClass",
          "org.embergraph.rdf.axioms.NoAxioms");
      props.setProperty("org.embergraph.rdf.sail.truthMaintenance", "false");
      props.setProperty("org.embergraph.rdf.store.AbstractTripleStore.justify", "false");
    }

    props.setProperty(
        "org.embergraph.rdf.store.AbstractTripleStore.textIndex",
        String.valueOf(options.contains(Option.TextIndex)));

    props.setProperty(
        "org.embergraph.rdf.store.AbstractTripleStore.statementIdentifiers",
        String.valueOf(options.contains(Option.RDR)));

    props.setProperty(
        "org.embergraph.rdf.store.AbstractTripleStore.quads",
        String.valueOf(options.contains(Option.Quads)));

    // Setup for the RWStore recycler rather than session protection.
    props.setProperty("org.embergraph.service.AbstractTransactionService.minReleaseAge", "1");
    props.setProperty("org.embergraph.btree.writeRetentionQueue.capacity", "4000");
    props.setProperty("org.embergraph.btree.BTree.branchingFactor", "128");
    // Bump up the branching factor for the lexicon indices on the default kb.
    props.setProperty(
        "org.embergraph.namespace.kb.lex.org.embergraph.btree.BTree.branchingFactor", "400");
    // Bump up the branching factor for the statement indices on the default kb.
    props.setProperty(
        "org.embergraph.namespace.kb.spo.org.embergraph.btree.BTree.branchingFactor", "1024");

    final Sail sail = getSailProviderInstance(props);

    return sail;
  }

  protected static Sail getSailProviderInstance(Properties props) {

    final String providerClass = System.getProperty(SAIL_PROVIDER, EMBERGRAPH_SAIL_INSTANCE);

    try {
      final Class<?> c = Class.forName(providerClass);
      final Constructor<?> cons = c.getConstructor(Properties.class);
      final Object object = cons.newInstance(props);
      final Sail proxy = (Sail) object;
      return proxy;
    } catch (ClassNotFoundException
        | NoSuchMethodException
        | SecurityException
        | InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException e) {

      throw new RuntimeException(e);
    }
  }

  protected static void checkArgs(final String file, final List<Option> options) {

    if (options.contains(Option.Inference) && options.contains(Option.Quads)) {
      throw new IllegalArgumentException();
    }

    if (options.contains(Option.RDR) && options.contains(Option.Quads)) {
      throw new IllegalArgumentException();
    }

    //        if (options.contains(Option.InMemory) && options.contains(Option.Persistent)) {
    //            throw new IllegalArgumentException();
    //        }
    //
    //        if (options.contains(Option.InMemory) && file != null) {
    //            throw new IllegalArgumentException();
    //        }
    //
    //        if (options.contains(Option.Persistent) && file == null) {
    //            throw new IllegalArgumentException();
    //        }

  }
}
