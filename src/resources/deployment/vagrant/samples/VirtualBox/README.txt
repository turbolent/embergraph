This directory contains a collection of Vagrantfile samples that demonstrate how
to launch a VirtualBox instance (the vagrant default) and configure it with a
Embergraph server or MapGraph under several useful scenarios.

The Vagrantfiles are named with a descriptive extension, and must be copied to 
the generic "Vagrantfile" to be read by the vagrant program.


Relevant files:
---------------

Vagrantfile.tomcat - Creates aa VirtualBox instance (Ubuntu 12.04 by default) and installs
                     Tomcat 7 and deploys the Embergraph WAR file as a service.

Vagrantfile.tomcat.build-from-svn - Like Vagrantfile.tomcat but the Embergraph WAR
                     file will be built from a specified subversion repository branch.

Vagrantfile.nss - Creates an VirtualBox instance (Ubuntu 12.04 by default) and installs
                     and starts a Embergraph NanoSparqlServer (NSS) Jetty server instance.

Vagrantfile.nss.build-from-svn - Like Vagrantfile.nss but the Embergraph NSS server
                     will be built from a specified subversion repository branch.


Sample Session
--------------

% cp Vagrantfile.tomcat Vagrantfile
% vagrant up

# The embergraph server is now found at: http://33.33.33.10:9999/embergraph


# to login to the host:
% vagrant ssh

# to terminate the EC2 instance:
% vagrant destroy

% cp Vagrantfile.tomcat.build-from-svn Vagrantfile
# edit the Vagrantfile and set the :svn_branch variable as desired
% vagrant up
