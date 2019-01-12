package org.embergraph.rdf.sail.config;

import java.util.Properties;

import org.embergraph.rdf.sail.EmbergraphSail;
import org.openrdf.sail.Sail;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailFactory;
import org.openrdf.sail.config.SailImplConfig;

/**
 * A {@link SailFactory} that creates {@link EmbergraphSail}s based on RDF
 * configuration data.
 */
public class EmbergraphSailFactory implements SailFactory {

	/**
	 * The type of sails that are created by this factory.
	 */
	public static final String TYPE = "embergraph:EmbergraphSail";

	public String getSailType() {
		return TYPE;
	}

	public SailImplConfig getConfig() {
		return new EmbergraphSailConfig(TYPE);
	}

	public Sail getSail(final SailImplConfig config)
		throws SailConfigException {
	
		if (!TYPE.equals(config.getType())) {
			throw new SailConfigException(
                    "Invalid type: " + config.getType());
		}

		if (!(config instanceof EmbergraphSailConfig)) {
			throw new SailConfigException(
                    "Invalid type: " + config.getClass());
		}
		
        try {
            
			final EmbergraphSailConfig bigdataConfig = (EmbergraphSailConfig)config;
			final Properties properties = bigdataConfig.getProperties();
    		return new EmbergraphSail(properties);
            
        } catch (Exception ex) {
            throw new SailConfigException(ex);
        }
        
	}
}
