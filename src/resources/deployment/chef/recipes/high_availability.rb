#
# Cookbook Name:: embergraph
# Recipe:: high_availability
#
# Copyright 2014, Systap
#

#
# Only do the following for Embergraph HA install
#
if node['embergraph'][:install_flavor] == "ha"

	include_recipe "java"
	include_recipe "sysstat"
	include_recipe "hadoop::zookeeper_server"

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

	#
	# Make sure the Embergraph home directory is owned by the embergraph user and group:
	#
	execute "change the ownership of the embergraph home directory to embergraph, which strangely is not" do
 		user	"root"
 		group	"root"
		cwd	node['embergraph'][:home]
 		command	"chown -R #{node['embergraph'][:user]}:#{node['embergraph'][:group]} ."
	end

	if node['embergraph'][:build_from_svn]
		include_recipe "ant"
		include_recipe "subversion::client"
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
		execute "ant deploy-artifact" do
			user	'ubuntu'
 			group	'ubuntu'
			cwd	node['embergraph'][:source_dir]
			command	"ant deploy-artifact"
		end

		#
		# Extract the just built release package, thus installing it in the Embergraph home directory:
		#
		execute "deflate REL tar" do
			user	node['embergraph'][:user]
 			group	node['embergraph'][:group]
			cwd	"#{node['embergraph'][:home]}/.."
			command	"tar xvf #{node['embergraph'][:source_dir]}/REL.embergraph-1.*.tgz"
		end

	else
		#
		# Retrieve the package prepared for Brew:
		#
		remote_file "/tmp/embergraph.tgz" do
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
			command	"tar xvf /tmp/embergraph.tgz"
		end

		#
		# The following are assumed fixed in releases after 1.3.1 and in the current subversion branch:
		#
		if node['embergraph'][:base_version].gsub(/\./, '').to_i == 131
			execute "Divert standard and error output into /dev/null" do
				user	'root'
				group	'root'
				cwd	"#{node['embergraph'][:home]}/etc/init.d"
				command	"sed -i 's|startHAServices\"|startHAServices > /dev/null 2>\\&1\"|' embergraphHA"
			end

			execute "Change SystemProperty to Property in the 'host' attribute of jetty.xml" do
				user	'root'
				group	'root'
				cwd	node['embergraph'][:jetty_dir]
			        command "sed -i 's|<Set name=\"host\"><SystemProperty|<Set name=\"host\"><Property|' jetty.xml"
			end

			execute "Change SystemProperty to Property in the 'port' attribute of jetty.xml" do
				user	'root'
				group	'root'
				cwd	node['embergraph'][:jetty_dir]
			        command "sed -i 's|<Set name=\"port\"><SystemProperty|<Set name=\"port\"><Property|' jetty.xml"
			end

			execute "Change SystemProperty to Property in the 'idleTimeout' attribute of jetty.xml" do
				user	'root'
				group	'root'
				cwd	node['embergraph'][:jetty_dir]
			        command "sed -i 's|<Set name=\"idleTimeout\"><SystemProperty|<Set name=\"idleTimeout\"><Property|' jetty.xml"
			end
		end
	end

	#
	# Install hte embergraphHA service file:
	#
	execute "copy over the /etc/init.d/embergraphHA file" do
		user	'root'
		group	'root'
		cwd	"#{node['embergraph'][:home]}/etc/init.d"
		command	"cp embergraphHA /etc/init.d/embergraphHA; chmod 00755 /etc/init.d/embergraphHA"
	end

	#
	# Create the log directory for embergraph:
	#
	directory node['embergraph'][:log_dir] do
		owner	node['embergraph'][:user]
		group	node['embergraph'][:group]
		mode	00755
		action	:create
	end

	#
	# Install the log4jHA.properties file:
	#
	template "#{node['embergraph'][:home]}/var/config/logging/log4jHA.properties" do
		source	"log4jHA.properties.erb"
		owner	node['embergraph'][:user]
		group	node['embergraph'][:group]
		mode	00644
	end

	#
	# Set the absolute path to the RWStore.properties file
	#
	execute "set absolute path to RWStore.properties" do
		cwd	"#{node['embergraph'][:jetty_dir]}/WEB-INF"
		command	"sed -i 's|<param-value>WEB-INF/RWStore.properties|<param-value>#{node['embergraph'][:properties]}|' web.xml"
	end

	#
	# Install the RWStore.properties file:
	#
	template node['embergraph'][:properties] do
		source	"RWStore.properties.erb"
		owner	node['embergraph'][:user]
		group	node['embergraph'][:group]
		mode	00644
	end

	#
	# Copy the /etc/default/embergraphHA template:
	#
	template "/etc/default/embergraphHA" do
		source	"etc/default/embergraphHA.erb"
 		user	'root'
 		group	'root'
		mode	00644
	end

	#
	# Setup the embergraphHA script as a service:
	#
	service "embergraphHA" do
		supports :restart => true, :status => true
		action [ :enable, :start ]
	end

	#
	# Install the zoo.cfg file:
	#
	template "/etc/zookeeper/conf/zoo.cfg" do
		source	"zoo.cfg.erb"
		owner	'root'
		group	'root'
		mode	00644
	end

	#
	# The hadoop cookbook overlooks the log4j.properties file presently, but a future version may get this right:
	#
	execute "copy the distribution log4j.properties file" do
		user	'root'
 		group	'root'
		cwd	"/etc/zookeeper/conf.chef"
		command	"cp ../conf.dist/log4j.properties ."
	end
end
