#!/bin/bash

FILE_OR_DIR=$1

if [ -f "/etc/default/embergraph" ] ; then
    . "/etc/default/embergraph"
else
    JETTY_PORT=9999
fi

LOAD_PROP_FILE=/tmp/$$.properties

[ -z "${NSS_PROPERTIES}" ] && export NSS_PROPERTIES=/usr/local/embergraph/conf/RWStore.properties

#Probably some unused properties below, but copied all to be safe.

cat <<EOT >> $LOAD_PROP_FILE
quiet=false
verbose=0
closure=false
durableQueues=true
#Needed for quads
#defaultGraph=
org.embergraph.rdf.store.DataLoader.flush=false
org.embergraph.rdf.store.DataLoader.bufferCapacity=100000
org.embergraph.rdf.store.DataLoader.queueCapacity=10
#Namespace to load
namespace=kb
#Files to load
fileOrDirs=$1
#Property file (if creating a new namespace)
propertyFile=$NSS_PROPERTIES
EOT

echo "Loading with properties..."

cat $LOAD_PROP_FILE

curl -X POST --data-binary @${LOAD_PROP_FILE} --header 'Content-Type:text/plain' http://localhost:${JETTY_PORT}/embergraph/dataloader

#Let the output go to STDOUT/ERR to allow script redirection

rm -f $LOAD_PROP_FILE
