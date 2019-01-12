#
# Where embergraph resource files will be installed:
#
default['embergraph'][:home] = "/var/lib/bigdata"

#
# Who runs embergraph? This is applicable to NSS and HA installs only:
#
default['embergraph'][:user]  = "embergraph"
default['embergraph'][:group] = "embergraph"
default['embergraph'][:base_version] = "1.3.1"

#
# When "build_from_svn" is "true", code retrieved from subversion will be downloaded to the "source_dir" directory:
#
default['embergraph'][:source_dir] = "/home/ubuntu/bigdata-code"

# Where the RWStore.properties file can be found:
default['embergraph'][:properties] = node['embergraph'][:home] + "/RWStore.properties"

case node['embergraph'][:install_flavor]
when "nss"
	# The URL to the embergraph-nss bundle.  The following is the same bundle used by the Embergraph Brew installer:
	default['embergraph'][:url] = "http://bigdata.com/deploy/bigdata-#{node['embergraph'][:base_version]}.tgz"

	# Where the jetty resourceBase is defined:
	default['embergraph'][:jetty_dir]  = node['embergraph'][:home] + "/var/jetty"

	# Where the log files will live:
	default['embergraph'][:log_dir]  = node['embergraph'][:home] + "/var/log"

	# Where the embergraph-ha.jnl file will live:
	default['embergraph'][:data_dir]  = node['embergraph'][:home] + "/var/data"

	# The subversion branch to use when building from source:
	if node['embergraph'][:build_from_svn]
		default['embergraph'][:svn_branch] = "https://svn.code.sf.net/p/bigdata/code/branches/DEPLOYMENT_BRANCH_1_3_1"
	end
when "tomcat"
	# The Tomcat version to install.  The Embergraph Chef cookbook has only been tested with Version 7:
	default['tomcat'][:base_version] = 7

	# JRE options options to set for Tomcat, the following is strongly recommended:
	default['tomcat'][:java_options] = "-Djava.awt.headless=true -server -Xmx4G -XX:+UseG1GC"

	# A SourceForge URL to use for downloading the embergraph.war file:
	default['embergraph'][:url]  = "http://hivelocity.dl.sourceforge.net/project/bigdata/bigdata/#{node['embergraph'][:base_version]}/bigdata.war"

	# Where the embergraph contents reside under Tomcat:
	default['embergraph'][:web_home] = node['tomcat'][:webapp_dir] + "/bigdata"

	# Where the log4j.properites file can be found:
	default['embergraph'][:log4j_properties] = default['embergraph'][:web_home] + "/WEB-INF/classes/log4j.properties"

	# Where the embergraph-ha.jnl file will live:
	default['embergraph'][:data_dir]  = node['embergraph'][:home] + "/data"

	# Where the log files will live:
	default['embergraph'][:log_dir]  = node['embergraph'][:home] + "/log"

	# The subversion branch to use when building from source:
	if node['embergraph'][:build_from_svn]
		default['embergraph'][:svn_branch] = "https://svn.code.sf.net/p/bigdata/code/branches/BIGDATA_RELEASE_1_3_0"
	end
when "ha"
	# The URL to the embergraphHA release bundle.
	default['embergraph'][:url] = "http://softlayer-dal.dl.sourceforge.net/project/bigdata/bigdata/#{node['embergraph'][:base_version]}/REL.embergraph-#{node['embergraph'][:base_version]}.tgz"

	# The subversion branch to use when building from source:
	if node['embergraph'][:build_from_svn]
		# default['embergraph'][:svn_branch] = "https://svn.code.sf.net/p/bigdata/code/branches/BIGDATA_RELEASE_1_3_0"
		default['embergraph'][:svn_branch] = "https://svn.code.sf.net/p/bigdata/code/branches/DEPLOYMENT_BRANCH_1_3_1"
	end

	# Where the embergraph-ha.jnl file will live:
	default['embergraph'][:data_dir] = node['embergraph'][:home] + "/data"

	# Where the log files will live:
	default['embergraph'][:log_dir] = node['embergraph'][:home] + "/log"

	# Where the jetty resourceBase is defined:
	default['embergraph'][:jetty_dir] = node['embergraph'][:home] + "/var/jetty"

	# Where the RWStore.properties file can be found:
	default['embergraph'][:properties] = node['embergraph'][:jetty_dir] + "/WEB-INF/RWStore.properties"

	# Name of the federation of services (controls the Apache River GROUPS).
	default['embergraph'][:fedname] = 'my-cluster-1'

	# Name of the replication cluster to which this HAJournalServer will belong.
	default['embergraph'][:logical_service_id] = 'HA-Replication-Cluster-1'

	# Set the REPLICATION_FACTOR.  1 = HA1, 3 = HA3, etc
	default['embergraph'][:replication_factor] = 3

	# Where to find the Apache River service registrars (can also use multicast).
	default['embergraph'][:river_locator1] = '33.33.33.10'
	default['embergraph'][:river_locator2] = '33.33.33.11'
	default['embergraph'][:river_locator3] = '33.33.33.12'

	# Where to find the Apache Zookeeper ensemble.
	default['embergraph'][:zk_server1] = 'embergraphA'
	default['embergraph'][:zk_server2] = 'embergraphB'
	default['embergraph'][:zk_server3] = 'embergraphC'

	# set the JVM_OPTS as used by startHAService
	default['embergraph'][:java_options] = "-server -Xmx4G -XX:MaxDirectMemorySize=3000m"
	# default['embergraph'][:java_options] = "-server -Xmx4G -XX:MaxDirectMemorySize=3000m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1046"
end


###################################################################################
#
#  Set the RWStore.properties attributes that apply for all installation scenarios.
#
###################################################################################


default['embergraph']['journal.AbstractJournal.bufferMode'] = "DiskRW"

# Setup for the RWStore recycler rather than session protection.
default['embergraph']['service.AbstractTransactionService.minReleaseAge']= "1"

default['embergraph']['btree.writeRetentionQueue.capacity'] = "4000"
default['embergraph']['btree.BTree.branchingFactor'] = "128"

# 200M initial extent.
default['embergraph']['journal.AbstractJournal.initialExtent'] = "209715200"
default['embergraph']['journal.AbstractJournal.maximumExtent'] = "209715200"

# Setup for QUADS mode without the full text index.
default['embergraph']['rdf.sail.truthMaintenance'] = "false"
default['embergraph']['rdf.store.AbstractTripleStore.quads'] = "false"
default['embergraph']['rdf.store.AbstractTripleStore.statementIdentifiers'] = "false"
default['embergraph']['rdf.store.AbstractTripleStore.textIndex'] = "false"
default['embergraph']['rdf.store.AbstractTripleStore.axiomsClass'] = "org.embergraph.rdf.axioms.NoAxioms"

# Bump up the branching factor for the lexicon indices on the default kb.
default['embergraph']['namespace.kb.lex.org.embergraph.btree.BTree.branchingFactor'] = "400"

# Bump up the branching factor for the statement indices on the default kb.
default['embergraph']['namespace.kb.spo.org.embergraph.btree.BTree.branchingFactor'] = "1024"
default['embergraph']['rdf.sail.bufferCapacity'] = "100000"

#
# Embergraph supports over a hundred properties and only the most commonly configured
# are set here as Chef attributes.  Any number of additional properties may be
# configured by Chef. To do so, add the desired property in this (attributes/default.rb)
# file as well as in the templates/default/RWStore.properties.erb file.  The
# "vocabularyClass" property (below) for inline URIs is used as example additional
# entry:
#
# default['embergraph']['rdf.store.AbstractTripleStore.vocabularyClass'] = "com.my.VocabularyClass"


#################################################################
#
#  The following attributes are defaults for the MapGraph recipe.
#
#################################################################

# The subversion branch to use when building from source:
default['mapgraph'][:svn_branch] = "https://svn.code.sf.net/p/mpgraph/code/trunk"

# MapGraph code retrieved from subversion will be downloaded to the "source_dir" directory:
default['mapgraph'][:source_dir] = "/home/ec2-user/mapgraph-code"
