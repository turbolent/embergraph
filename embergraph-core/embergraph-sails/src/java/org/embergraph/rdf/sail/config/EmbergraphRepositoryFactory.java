package org.embergraph.rdf.sail.config;

import java.util.Properties;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSailRepository;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;

/*
 * A {@link RepositoryFactory} that creates {@link EmbergraphSailRepository}s based on RDF
 * configuration data.
 */
public class EmbergraphRepositoryFactory implements RepositoryFactory {

  /** The type of repositories that are created by this factory. */
  public static final String TYPE = "embergraph:EmbergraphRepository";

  public String getRepositoryType() {
    return TYPE;
  }

  public RepositoryImplConfig getConfig() {
    return new EmbergraphRepositoryConfig(TYPE);
  }

  public Repository getRepository(final RepositoryImplConfig config)
      throws RepositoryConfigException {

    if (!TYPE.equals(config.getType())) {
      throw new RepositoryConfigException("Invalid type: " + config.getType());
    }

    if (!(config instanceof EmbergraphRepositoryConfig)) {
      throw new RepositoryConfigException("Invalid type: " + config.getClass());
    }

    try {

      final EmbergraphRepositoryConfig embergraphConfig = (EmbergraphRepositoryConfig) config;
      final Properties properties = embergraphConfig.getProperties();
      final EmbergraphSail sail = new EmbergraphSail(properties);
      return new EmbergraphSailRepository(sail);

    } catch (Exception ex) {
      throw new RepositoryConfigException(ex);
    }
  }
}
