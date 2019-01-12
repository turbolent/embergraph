#!/bin/bash

##
# Script applies properties read from the command line to the specified
# resource (e.g., a triple store).
#

# Setup the environment.
source `dirname $0`/embergraphenv

if [ -z "$1" ]; then
    echo "usage: $0 namespace"
    echo "   where 'namespace' is the namespace of the KB."
    exit 1;
fi

namespace=$1

java ${JAVA_OPTS} -Xmx400m \
	 -cp ${CLASSPATH} \
	 org.embergraph.rdf.sail.EmbergraphSailHelper \
	 ${BIGDATA_CONFIG} JDS $namespace
