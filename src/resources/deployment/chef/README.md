Embergraph Cookbook
================
The Embergraph cookbook provides the [embergraph v1.3.1](http://www.embergraph.org/) opensource triplestore/graph database.  The cookbook provides recipes to install the Embergraph server as a web application under Tomcat, with its own embedded Jetty server (NSS - the NanoSparqlServer).  The recipes will install pre-configured packages by node and optionally may build and install the server directly from source archive.

For more info on Embergraph please visit:

* Embergraph Homepage: [http://www.embergraph.org/](http://www.embergraph.org/)
* Embergraph SourceForge Page: [http://sourceforge.net/projects/bigdata/](http://sourceforge.net/projects/bigdata/)

Requirements
------------
Chef 11 or higher<br/>
Ruby 1.9 (preferably from the Chef full-stack installer)



Attributes
----------

### General Attributes

`node['embergraph'][:home]` - The root directory for embergraph contents (Default: `/var/lib/embergraph`)

`node['embergraph'][:url]` - Where to download the embergraph package file from. (Defaults: Tomcat: http://softlayer-dal.dl.sourceforge.net/project/embergraph/embergraph/1.3.1/embergraph.war / NSS: http://bigdata.com/deploy/embergraph-1.3.1.tgz)

`node['embergraph'][:data_dir]`
 - Where the embergraph.jnl resides. Discussed in <a href="http://sourceforge.net/apps/mediawiki/bigdata/index.php?title=NanoSparqlServer#Common_Startup_Problems">Common Startup Problmems</a>
 (Defaults: Tomcat: `node['embergraph'][:home]`/data / NSS: `node['embergraph'][:home]`/var/data)

`node['embergraph'][:log_dir]` - Where embergraph log files should reside (i.e. queryLog.csv, rules.log, queryRunStateLog.csv).  (Default: Tomcat: `node['embergraph'][:home]`/var/log / NSS: `node['embergraph'][:home]`/var/log)

`node['embergraph'][:properties]` - File path to the Embergraph properties file.  (Default: `node['embergraph'][:home]`/RWStore.properties)

`node['embergraph'][:svn_branch]` - The Subversion branch to retrieve source files from.  (Default: Tomcat: https://svn.code.sf.net/p/embergraph/code/branches/BIGDATA\_RELEASE\_1\_3\_0 / NSS: https://svn.code.sf.net/p/embergraph/code/branches/DEPLOYMENT\_BRANCH\_1\_3\_1)

`node['embergraph'][:source]` - The directory to retrieve Subversion contents into.  (Default: embergraph-code)

`node['embergraph']['journal.AbstractJournal.bufferMode']` - Journal Buffer Mode (Default: DiskRW)

`node['embergraph']['service.AbstractTransactionService.minReleaseAge']` - Minimum Release Age (Default: 1)

`node['embergraph']['btree.writeRetentionQueue.capacity']` - Writing retention queue length.  (Default: 4000)

`node['embergraph']['btree.BTree.branchingFactor']` - Branching factor for the journal's B-Tree.  (Default: 128)

`node['embergraph']['journal.AbstractJournal.initialExtent']` - Journal's initial extent (Default: 209715200)

`node['embergraph']['journal.AbstractJournal.maximumExtent']` - Journal's maximum extent (Default: 209715200)

`node['embergraph']['rdf.sail.truthMaintenance']` - Switch Truth Maintenance on/off.  (Default: false)

`node['embergraph']['rdf.store.AbstractTripleStore.quads']` - Switch Quads Mode on/off.  (Default: false)

`node['embergraph']['rdf.store.AbstractTripleStore.statementIdentifiers']` - Switch statement identifiers on/off.  (Default: false)

`node['embergraph']['rdf.store.AbstractTripleStore.textIndex']` - Switch text indexing on/off.  (Default: false)

`node['embergraph']['rdf.store.AbstractTripleStore.axiomsClass']` - The class to handle RDF axioms.  (Default: org.embergraph.rdf.axioms.NoAxioms)

`node['embergraph']['namespace.kb.lex.org.embergraph.btree.BTree.branchingFactor']` - Branching factor for the journal's Lexical B-Tree.  (Default:- 400)

`node['embergraph']['namespace.kb.spo.org.embergraph.btree.BTree.branchingFactor']` - Branching factor for the journal's SPO B-Tree.  (Default: 1024)

`node['embergraph']['rdf.sail.bufferCapacity']` - The number of statements to buffer before committing triples to the persistence layer.  (Default: 100000)

### Attributes for Tomcat Based Install

`node['embergraph'][:web_home]` - The web application root directory for embergraph.  (Default `node['tomcat'][:webapp_dir]`/embergraph)

`node['embergraph'][:log4j_properties]` - File path to the log4j properties file.  (Default `node['embergraph'][:web_home]`/WEB-INF/classes/log4j.properties)

### Attributes for NanoSparqlServer (NSS) Based Install

`node['embergraph'][:user]` - The user to install and run embergraph under.  (Default: `embergraph`)

`node['embergraph'][:group]` - The group to install and run embergraph under.  (Default: `embergraph`)

`node['embergraph'][:jetty_dir]` - The Jetty root directory.  (Default: `node['embergraph'][:home]`/var/jetty)

### Attributes for MapGraph

`node['mapgraph'][:svn_branch]` - The Subversion branch to retrieve source files from.  (Default: https://svn.code.sf.net/p/mpgraph/code/trunk)

`node['mapgraph'][:source]` - The directory to retrieve Subversion contents into.  (Default: mapgraph-code )


Recipes
-------

A node recipe is not provided by the Embergraph cookbook.  The user is given the option to install the Embergraph server under Tomcat or as a Jetty application.  Under both options, Embergraph may optinally be built directly from the a Subversion source code branch.

### tomcat

Installs the [Tomcat](http://tomcat.apache.org/) server and then embergraph as a web application. Embergraph will be configured according to the attributes. If no attributes are given, Embergraph will be installed with the systems nodes.

If the `build_from_svn` attribute is set to `true` Embergraph will be build from the Subversion repository given in the `svn_branch` attribute.

### nss

Installs the Embergraph server to run in the [NanoSparqlServer](http://wiki.blazegraph.com/wiki/index.php/NanoSparqlServer) (Jetty) mode.


If the `build_from_svn` attribute is set to `true` Embergraph will be build from the Subversion repository given in the `svn_branch` attribute.


### mapgraph

Retrieves the [MapGraph](http://sourceforge.net/projects/mpgraph/) project from its Subversion archive at SourceForget and builds it.
This recipe can only be used with GPU architecture and has only been validated against Amazon's  "NVIDIA GRID GPU Driver" AMI.


Usage
-----


### Vagrant Context

Sample Vagrant configurations are available in the Embergraph Subversion source tree under [embergraph/src/resources/deployment/vagrant](http://sourceforge.net/p/embergraph/code/HEAD/tree/branches/DEPLOYMENT_BRANCH_1_3_1/embergraph/src/resources/deployment/vagrant/). 

#### Tomcat Example


    chef.json = {
        :embergraph => {
                :install_flavor => "tomcat",
                :build_from_svn => true,
                :svn_branch => "https://svn.code.sf.net/p/embergraph/code/branches/BTREE_BUFFER_BRANCH/"
        },
        :java => {
                :install_flavor => "oracle",
                :jdk_version => "7",
                :oracle => { 'accept_oracle_download_terms' => true }
        },
        :tomcat => {
                :base_version => "7"
        }
    }

    chef.run_list = [
    	...
        "recipe[embergraph::tomcat]"
        ...
    ]



#### NSS Example


    chef.json = {
        :embergraph => {
                :install_flavor => "nss"
        },
        :java => {
                :install_flavor => "oracle",
                :jdk_version => "7",
                :oracle => { 'accept_oracle_download_terms' => true }
        }
    }

    chef.run_list = [
    	...
        "recipe[embergraph::nss]"
        ...
    ]


### Trouble Shooting

The Bigdta cookbook recipes have been tested thoroughly in the Vagrant context with VirtualBox and AWS providers using Ubuntu 12.04 and Oracle's JDK 7.

When errors occur in the Vagrant context, it is most typically during the installation process where a network timeout has occurred during the retrieval of a dependent resource. simply continue with:
	
	% vagrant provision

Which should get past any intermit ant network issues.  For assistance with installation and other issues, please visit the [Embergraph Support Forum](http://sourceforge.net/p/embergraph/discussion/676946).


License and Authors
-------------------
Author:: Daniel Mekonnen [daniel&lt;no-spam-at&gt;systap.com]


GNU GPLv2 - This pakcage may be resiributed under the same terms and conditions as the Embergraph project that it is a part of.

	http://www.gnu.org/licenses/gpl-2.0.html
