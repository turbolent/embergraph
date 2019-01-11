Embergraph RPM Deployer
-----------------

This must be run on a platform with the rpm tools installed.

```
yum -y install rpm-build
```

```
mvn package
rpm --install target/rpm/embergraph-rpm/RPMS/noarch/embergraph-rpm-1.6.0-SNAPSHOT.noarch.rpm
service embergraph start
```

This will start a Embergraph instance running on port 9999 on localhost host.

You may then navigate http://localhost:9999/bigdata/ to access Embergraph.


Changing the configuration
-----------------

The embergraph configuration is stored in `/etc/embergraph/`

