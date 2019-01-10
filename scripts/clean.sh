#!/bin/bash
BASE_DIR=`dirname $0`

mvn -f "${BASE_DIR}"/../pom.xml clean
mvn -f "${BASE_DIR}"/../embergraph-artifacts/pom.xml clean
rm -rf "${BAZE_DIR}"/artifacts

