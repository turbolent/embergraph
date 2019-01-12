#
# Cookbook Name:: embergraph
# Recipe:: nss
#
# Copyright 2014, Systap
#

#
# Only do the following for Embergraph NSS install
#
if node['embergraph'][:install_flavor] == "nss"

	include_recipe "java"

	#
	# Create the embergraph systm group:
	#
	group node['embergraph'][:group] do
		action	:create
		append	true
	end

	#
	# Create the embergraph systm user:
	#
	user node['embergraph'][:user] do
		gid	node['embergraph'][:group]
		supports :manage_home => true
		shell	"/bin/false"
		home	node['embergraph'][:home]
		system	true
		action	:create
	end


	if node['embergraph'][:build_from_svn]
		include_recipe	"ant"
		include_recipe	"subversion::client"

		#
		# Retrieve the Embergraph source from the specified subversion branch:
		#
		execute "checkout embergraph from svn repo" do
			user	'ubuntu'
		 	group	'ubuntu'
			cwd	"/home/ubuntu"
		        command	"svn checkout #{node['embergraph'][:svn_branch]} #{node['embergraph'][:source_dir]}"
		end

		#
		# Build the embergraph release package:
		#
		execute "build the nss tar ball" do
			user	'ubuntu'
		 	group	'ubuntu'
			cwd	node['embergraph'][:source_dir]
			command	"ant package-nss-brew"
		end

		#
		# Extract the just built release package, thus installing it in the Embergraph home directory:
		#
		execute "Extract and relocate the embergraph archive" do
			user	node['embergraph'][:user]
 			group	node['embergraph'][:group]
			cwd	"#{node['embergraph'][:home]}/.."
			command	"tar xvf #{node['embergraph'][:source_dir]}/REL-NSS.embergraph-1.*.tgz"
		end
	else
		#
		# Retrieve the package prepared for Brew:
		#
		remote_file "/tmp/bigdata.tgz" do
			owner	node['embergraph'][:user]
			group	node['embergraph'][:group]
			source	node['embergraph'][:url]
		end

		#
		# Extract the just retrieved release package, thus installing it in the Embergraph home directory:
		#
		execute "Extract and relocate the embergraph archive" do
			user	node['embergraph'][:user]
 			group	node['embergraph'][:group]
			cwd	"#{node['embergraph'][:home]}/.."
			command	"tar xvf /tmp/bigdata.tgz"
		end
	end

	#
	# Create a symbolic link of the bin/bigdataNSS script to /etc/init.d/bigdataNSS:
	#
	link "/etc/init.d/bigdataNSS" do
		to "#{node['embergraph'][:home]}/bin/bigdataNSS"
	end

	#
	# Set the install type in the bin/bigdataNSS script:
	#
	execute "set the INSTALL_TYPE in bin/bigdata" do
		cwd	"#{node['embergraph'][:home]}/bin"
		command	"sed -i 's|<%= INSTALL_TYPE %>|#{node['embergraph'][:install_flavor]}|' embergraphNSS"
	end

	#
	# Set the Embergraph home directory in the bin/bigdataNSS file:
	#
	execute "set the BD_HOME in bin/bigdata" do
		cwd	"#{node['embergraph'][:home]}/bin"
		command	"sed -i 's|<%= BD_HOME %>|#{node['embergraph'][:home]}|' embergraphNSS"
	end

	#
	# Set the absolute path to the embergraph.jnl file in RWStore.properties
	#
	execute "set the BD_HOME in RWStore.properties" do
		cwd	"#{node['embergraph'][:jetty_dir]}/WEB-INF"
		command	"sed -i 's|<%= BD_HOME %>|#{node['embergraph'][:home]}|' RWStore.properties"
	end

	#
	# Set the Embergraph home directory in the log4j.properties file to set the path for the log files:
	#
	execute "set the BD_HOME in log4j.properties" do
		cwd	"#{node['embergraph'][:jetty_dir]}/WEB-INF/classes"
		command	"sed -i 's|<%= BD_HOME %>|#{node['embergraph'][:home]}|' log4j.properties"
	end

	#
	# Setup the embergraphNSS script as a service:
	#
	service "embergraphNSS" do
		#
		# Reenable this when the bin/bigdata script is updated to return a "1" for a successful status:
		#
		#   See:  http://comments.gmane.org/gmane.comp.sysutils.chef.user/2723
		#
		# supports :status => true, :start => true, :stop => true, :restart => true
		supports :start => true, :stop => true, :restart => true
		action [ :enable, :start ]
	end
end
