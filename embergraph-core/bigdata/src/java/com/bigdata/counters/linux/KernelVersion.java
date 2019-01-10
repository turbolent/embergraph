package com.bigdata.counters.linux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.system.SystemUtil;

/**
 * Reports on the kernel version for a linux (or OSX) host.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan
 *         Thompson</a>
 * @version $Id$
 * 
 * @todo there is more information lurking in the kernel version (under linux).
 * 
 * @see http://en.wikipedia.org/wiki/Linux_kernel for a summary of linux
 *      kernel versioning.
 */
public class KernelVersion {
    
    static private final Logger log = Logger.getLogger(KernelVersion.class);

    public final int version;
    public final int major;
    public final int minor;

    private static Pattern p = Pattern.compile("^([0-9]+).([0-9]+).([0-9]+).*");
    
    public KernelVersion(final String val) {

        if (val == null)
            throw new IllegalArgumentException();
        
        final Matcher m = p.matcher(val);
        
        if(!m.matches())
            throw new IllegalArgumentException("Not a kernel version? [" + val
                    + "]");
        
        version = Integer.parseInt(m.group(1));
        major   = Integer.parseInt(m.group(2));
        minor   = Integer.parseInt(m.group(3));
        
    }

    public String toString() {

		return "{version=" + version + ", major=" + major + ", minor=" + minor
				+ "}";

    }
    
    /**
     * Return the version of the Linux kernel as reported by
     * <code>uname</code>
     * 
     * @return The {@link KernelVersion}
     * 
     * @throws RuntimeException
     *             if anything goes wrong.
     */
    static public KernelVersion get() {

        final List<String> commands = new LinkedList<String>();

        final Process pr;
        
        try {
        
			if (SystemUtil.isLinux()) {

				commands.add("/bin/uname");

			} else if (SystemUtil.isOSX()) {

				commands.add("/usr/bin/uname");

        	} else {

				/*
				 * Attempt uname -r at with this path, but this is an unknown
				 * OS.
				 */
				commands.add("/bin/uname");
        		
        	}
            
            commands.add("-r");
            
            final ProcessBuilder pb = new ProcessBuilder(commands);
            
            pr = pb.start();
            
            pr.waitFor();
        
        } catch (Exception ex) {
        
            throw new RuntimeException("Problem running command: ["
                    + commands + "]", ex);
            
        }
        
        if (pr.exitValue() == 0) {
        
            final BufferedReader outReader = new BufferedReader(
                    new InputStreamReader(pr.getInputStream()));
            
            final String val;
            try {
            
                val = outReader.readLine();
                
            } catch (IOException ex) {
                
                throw new RuntimeException(ex);
                
            } finally {
                
                try {
                    outReader.close();
                } catch (IOException ex) {
                    log.error(ex, ex);
                }
                
            }

            if (val == null)
                throw new RuntimeException("Nothing returned.");
            
            if(log.isInfoEnabled())
                log.info("read: [" + val + "]");

            return new KernelVersion(val.trim());
            
        } else {
            
			throw new RuntimeException(
					"Could not get kernal version: exitValue=" + pr.exitValue());
            
        }

    }

	/**
	 * Prints the data reported by {@link KernelVersion#get()}.
	 * 
	 * @param args
	 *            ignored.
	 */
    public static void main(final String[] args) {
    	
    	final KernelVersion kver = KernelVersion.get();

    	System.out.println("kernel version: "+kver);
    	
    }
    
}