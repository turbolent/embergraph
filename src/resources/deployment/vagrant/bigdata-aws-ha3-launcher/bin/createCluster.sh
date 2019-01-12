#! /bin/sh

export PYTHONPATH=/usr/local/lib/python2.7/site-packages

source aws.rc
python ./bin/createSecurityGroup.py
source .aws_security_group
rm .aws_security_group
vagrant up
#
# Occassionally, usually during svn based builds, AWS has timeout issues.  If this occurs, launch the cluster instances individually:
#
# vagrant up embergraphA
# echo "\nembergraphA is up\n"
# vagrant up embergraphB
# echo "\nembergraphB is up\n"
# vagrant up embergraphC
# echo "\nembergraphC is up\n"
echo "Vagrant up completed. Setting host names..."
python ./bin/setHosts.py
