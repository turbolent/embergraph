## Welcome to the Blazegraph Database
Blazegraph™ DB is a ultra high-performance graph database supporting Blueprints and RDF/SPARQL APIs. It supports up to 50 Billion edges on a single machine and has a enterprise features for High Availability, Scale-out architecture, and [GPU Acceleration](https://www.blazegraph.com/product/gpu-accelerated/). It is in production use for Fortune 500 customers such as EMC, Autodesk, and many others.  It is supporting key [Precision Medicine](http://www.syapse.com) applications and has wide-spread usage for life science applications.  It is used extensively to support Cyber anaytics in commercial and government applications.  It powers the Wikimedia Foundation's [Wikidata Query Service](https://query.wikidata.org/).  See the latest [Feature Matrix](http://www.blazegraph.com/product/).

![image](http://blog.blazegraph.com/wp-content/uploads/2015/07/embergraph_by_systap_favicon.png)

Please see the release notes in [releases](embergraph/src/releases) for version changes.

[Sign up](http://eepurl.com/VLpUj) to get the latest news on Blazegraph.

Please also visit us at our: [website](https://www.blazegraph.com), [wiki](https://wiki.blazegraph.com), and [blog](https://blog.blazegraph.com/).

Find an issue?   Need help?  See [JIRA](https://jira.blazegraph.com) or purchase [Support](https://www.blazegraph.com/buy).

Reporting a security issue: [Security Reporting](Security.md).

###Quick Start with the Executable Jar
Up and running with Blazegraph in under 30 seconds:  [Quick Start](https://wiki.blazegraph.com/wiki/index.php/Quick_Start).

###Building the code
As a quick start, run `mvn install -DskipTests` or the utility script `./scripts/mavenInstall.sh `.

For more detailed maven information see the [wiki](https://wiki.blazegraph.com/wiki/index.php/MavenNotes). 

###Samples and Examples
There are code samples and examples to get started with the Blazegraph Database [here] (https://github.com/embergraph/embergraph-samples).  Tinkerpop3 examples are included directly within the Tinkerpop3 repository per below.

###Javadocs
Click here to view the lastest [API Javadocs](https://embergraph.github.io/database/apidocs/index.html).

###Maven Central
Starting with the 2.0.0 release, the Blazegraph Database is available on Maven Central.  To include the core platform and dependencies, include the artifact below in your dependencies.   [Developing with Maven](https://wiki.blazegraph.com/wiki/index.php/MavenNotes) has notes on developing with Blazegraph Database source code and Maven.

```
    <dependency>
        <groupId>org.embergraph</groupId>
        <artifactId>embergraph-core</artifactId>
        <version>2.0.0</version>
    </dependency>
    <!-- Use if Tinkerpop 2.5 support is needed ; See also Tinkerpop3 below. -->
    <dependency>
        <groupId>org.embergraph</groupId>
        <artifactId>embergraph-blueprints</artifactId>
        <version>2.0.0</version>
    </dependency>
```

If you'd just link the Blazegraph Database dependencies without any of the external libraries, use the embergraph-runtime artifact.

```
    <dependency>
        <groupId>org.embergraph</groupId>
        <artifactId>embergraph-runtime</artifactId>
        <version>2.0.0</version>
    </dependency>
```

###Deployers

Starting with 2.0.0, the default context path for deployment is `http://localhost:9999/embergraph/`.  There are also Maven artifacts for WAR deployers (`embergraph-war`), executable Jar files (`embergraph-jar`), [Debian Package](embergraph-deb/) (`embergraph-deb`), [RPM](embergraph-rpm/) (`embergraph-rpm`), and a [Tarball](embergraph-tgz/) (`embergraph-tgz`).

###Tinkerpop3
Tinkerpop3 supports requires Java 1.8 and is now in a separate repository.  See [Tinkerpop3](https://github.com/embergraph/tinkerpop3).  It is also available as Maven Central artifact.

```
    <dependency>
        <groupId>org.embergraph</groupId>
        <artifactId>embergraph-gremlin</artifactId>
        <version>1.0.0</version>
    </dependency>
    
```

###Triple Pattern Fragment (TPF) Server
There is a [Blazegraph Triple Pattern Fragment TPF](https://github.com/embergraph/BlazegraphBasedTPFServer) server that supports [Linked Data Fragments](http://linkeddatafragments.org/).

```
    <dependency>
        <groupId>org.embergraph</groupId>
        <artifactId>BlazegraphBasedTPFServer</artifactId>
        <version>0.1.0</version>
    </dependency>
```    

###Blazegraph Python Client
There is a Blazegraph Python Client [here] (https://github.com/embergraph/embergraph-python)

###Blazegraph Dot Net RDF Client
There is a Blazegraph Dot Net RDF Client [here](https://github.com/embergraph/embergraph-dotnetrdf)
