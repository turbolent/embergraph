/*

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*/
/*
 * Created on Apr 6, 2009
 */

package org.embergraph.counters.query;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import org.embergraph.Banner;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.IHostCounters;
import org.embergraph.counters.IRequiredHostCounters;
import org.embergraph.counters.PeriodEnum;
import org.embergraph.counters.XMLUtility;
import org.embergraph.counters.httpd.DummyEventReportingService;
import org.embergraph.counters.render.IRenderer;
import org.embergraph.counters.render.RendererFactory;
import org.embergraph.service.Event;
import org.embergraph.util.httpd.NanoHTTPD;

/**
 * Utility to extract a batch of performance counters from a collection of
 * logged XML counter set files. This utility accepts file(s) giving the URLs
 * which would be used to demand the corresponding performance counters against
 * the live bigdata federation. The URLs listed in that file are parsed. The
 * host and port information are ignored, but the URL query parameters are
 * extracted and used to configured a set of {@link URLQueryModel}s.
 * <p>
 * A single pass is made through the specified XML counter set files. Each file
 * is read into memory by itself, and each query implied by a listed URL is run
 * against the in-memory {@link CounterSet} hierarchy. The results are collected
 * in independent {@link CounterSet} provisioned for the specified reporting
 * units, etc. Once the last XML counter set file has been processed, the
 * various reports requested by the listed URLs are generated.
 * <p>
 * For each generated report, the name of the file on which the report will be
 * written is taken from the name of the counter whose value was extracted for
 * that report. This filename may be overridden by including the URL query
 * parameter {@value URLQueryModel#FILE}, which specifies the file on which to
 * write the report for that query.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @see IHostCounters
 * @see IRequiredHostCounters
 * 
 * @todo When rendering HTML output using flot, the flot resources need to be
 *       available in order to view the graphs. They should be written once into
 *       the output directory and the links in the (X)HTML output should resolve
 *       them there.
 * 
 * @todo Permit nsamples to be specified in units of minutes, hours, days. E.g.,
 *       3d would be 3 days.
 * 
 * @todo Performance for long runs could be improved if we use more efficient
 *       classes for mutable strings in {@link XMLUtility} and perhaps
 *       {@link CounterSet}. E.g., mg4j mutable string or
 *       http://javolution.org/.
 */
public class CounterSetQuery {

    static private final Logger log = Logger.getLogger(CounterSetQuery.class);

    /**
     * Reads a list of {@link URL}s from a file. Blank lines and comment lines
     * are ignored.
     * 
     * @param file
     *            A file containing URLs, blank lines, and comment lines (which
     *            start with '#').
     * 
     * @return A list of the URLs read from the file.
     * 
     * @throws IOException
     */
    private static Collection<URL> readURLsFromFile(final File file) throws IOException {

        if(log.isInfoEnabled())
            log.info("Reading queries: "+file);
        
        final List<URL> tmp = new LinkedList<URL>();

        final BufferedReader r = new BufferedReader(new FileReader(file));

        try {

            String s;
            while ((s = r.readLine()) != null) {

                s = s.trim();

                if (s.isEmpty())
                    continue;

                if (s.startsWith("#"))
                    continue;

                tmp.add(new URL(s));

            }

        } finally {

            r.close();

        }

        return tmp;

    }

    /**
     * Reads URLs from a file or all files (recursively) in a directory.
     * 
     * @param file
     *            The file or directory.
     *            
     * @return The URLs read from the file(s).
     * 
     * @throws IOException 
     */
    static private Collection<URL> readURLs(final File file) throws IOException {

        /*
         * note: duplicates are not filtered out but this preserves the
         * evaluation order.
         */
        final Collection<URL> urls = new LinkedList<URL>();
        
        if (file.isDirectory()) {

            final File[] files = file.listFiles();
            
            for(File f : files) {
               
                if (f.isHidden())
                    continue;
                
                urls.addAll(readURLsFromFile(f));
                               
            }
            
        } else {

            urls.addAll(readURLsFromFile(file));

        }

        return urls;
        
    }

    private static void readFiles(final Collection<File> counterSetFiles,
            final CounterSet counterSet, final int nsamples,
            final PeriodEnum period, final Pattern regex) throws IOException,
            SAXException, ParserConfigurationException, InterruptedException,
            ExecutionException {

        // flatten directories in the list of files.
        final Collection<File> flatFileList = QueryUtil.collectFiles(counterSetFiles,
                new FileFilter() {

                    public boolean accept(File pathname) {

                        return !pathname.isHidden()
                                && pathname.getName().endsWith(".xml");

                    }

                });

        if (log.isInfoEnabled())
            log.info("Reading performance counters from "
                    + flatFileList.size() + " sources.");
        
        // read the files
        if (false/* sequential */) {

            // process the files one at a time.
            readFilesSequential(flatFileList, counterSet, nsamples, period,
                    regex);

        } else {

            // process the files in parallel.
            readFilesParallel(flatFileList, counterSet, nsamples, period,
                    regex);
            
        }

    }

    private static void readFilesSequential(
            final Collection<File> counterSetFiles,
            final CounterSet counterSet, final int nsamples,
            final PeriodEnum period, final Pattern regex) throws IOException,
            SAXException, ParserConfigurationException {

        for (File file : counterSetFiles) {

                if(log.isInfoEnabled())
                    log.info("Reading file: " + file);

                QueryUtil.readCountersFromFile(file, counterSet, regex, nsamples,
                        period);

        }

    }

    private static void readFilesParallel(
            final Collection<File> counterSetFiles,
            final CounterSet counterSet, final int nsamples,
            final PeriodEnum period, final Pattern regex) throws IOException,
            SAXException, ParserConfigurationException, InterruptedException, ExecutionException {

        final int nfiles = counterSetFiles.size();

        final List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(nfiles);

        for (File file : counterSetFiles) {

            tasks.add(new QueryUtil.ReadCounterSetXMLFileTask(file, counterSet,
                    nsamples, period, regex));
            
        }
        
        final ExecutorService service = Executors.newFixedThreadPool(nfiles);
        
        final List<Future<Void>> futures;
        try {
        
            // run all tasks.
            futures = service.invokeAll(tasks);

        } finally {
            
            service.shutdownNow();
            
        }

        int i = 0;
        int nerrors = 0;
        for(Future<Void> future : futures) {
            
            // look for errors in the tasks.
            try {
                future.get();
            } catch(ExecutionException ex) {
                if(ex.getCause() instanceof SAXException) {
                    /*
                     * Sometimes you can get a partial XML file if the LBS was
                     * in the process of generating the file when it was copied.
                     * This shows up as a SAXException.  Rather than dying, this
                     * just logs a warning and continues.
                     */
                    log.warn("Could not parse file (ignored): " + tasks.get(i), ex);
                    nerrors++;
                    continue;
                }
            }
            
            i++;
            
        }
        
        if (nerrors != 0)
            log.error("There were " + nerrors + " errors.");

    }

    /**
     * Utility class for running extracting data from performance counter dumps
     * and running various kinds of reports on those data.
     * <p>
     * Usage:
     * <dl>
     * <dt>-outputDir</dt>
     * <dd>The output directory (default is the current working directory).</dd>
     * <dt>-mimeType</dt>
     * <dd>The default MIME type for the rendered reports. The default is
     * <code>text/plain</code>, but can be overridden on a query by query basis
     * using {@link URLQueryModel#MIMETYPE}.</dd>
     * <dt>-nsamples</dt>
     * <dd>Override for the default #of history samples to be retained. It is an
     * error if there are more distinct samples in the processed XML counter set
     * files (that is, if the #of time periods sampled exceeds this many
     * samples). If there are fewer, then some internal arrays will be
     * dimensioned larger than is otherwise necessary.</dd>
     * <dt>-events &lt;file&gt;</dt>
     * <dd>A file containing tab-delimited {@link Event}s. The {@link Event}s
     * are not required for simple performance counter views.</dd>
     * <dt>-queries &lt;file&gt;</dt>
     * <dd>A file, or directory of files, containing a list of URLs, each of
     * which is interpreted as a {@link URLQueryModel}.</dd>
     * <dt>&lt;file&gt;(s)</dt>
     * <dd>One or more XML counter set files or directories containing such
     * files. All such files will be processed before the reports are generated.
     * </dd>
     * </dl>
     * 
     * @param args
     *            Command line arguments.
     * 
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void main(final String[] args) throws IOException,
            SAXException, ParserConfigurationException, InterruptedException,
            ExecutionException {

        Banner.banner();

        if (args.length == 0) {

            System.err.println("See javadoc for usage.");

            System.exit(1);
            
        }
        
        /*
         * The events read from the file(s).
         */
        final DummyEventReportingService service = new DummyEventReportingService();

        // The default output format (text, html, etc.)
        String defaultMimeType = NanoHTTPD.MIME_TEXT_PLAIN;

        /*
         * The #of slots to allocate (one slot per period of data to be read).
         * 
         * Note: The default is 7 days of data if period is minutes.
         */
        int nsamples = 60 * 24 * 7;

        // The output directory defaults to the current working directory.
        File outputDir = new File(".");
        
        // the set of queries to be processed.
        final List<URLQueryModel> queries = new LinkedList<URLQueryModel>();
        
        // the set of counter set XML files to be processed.
        final List<File> counterSetFiles = new LinkedList<File>();
        
        for (int i = 0; i < args.length; i++) {

            final String arg = args[i];

            if (arg.startsWith("-")) {

                if (arg.equals("-outputDir")) {

                    outputDir = new File(args[++i]);

                    if (log.isInfoEnabled()) {

                        log.info("outputDir: " + outputDir);

                    }
                    
                    if(!outputDir.exists()) {
                        
                        outputDir.mkdirs();
                        
                    }
                    
                } else if (arg.equals("-mimeType")) {

                    defaultMimeType = args[++i];

                } else if (arg.equals("-nsamples")) {

                    nsamples = Integer.valueOf(args[++i]);

                    if (nsamples <= 0)
                        throw new IllegalArgumentException(
                                "nslots must be positive.");

                } else if (arg.equals("-events")) {

                    // @todo read list of event files once all args are parsed.
                    QueryUtil.readEvents(service, new File(args[++i]));

                } else if (arg.equals("-queries")) {

                    final File file = new File(args[++i]);

                    final Collection<URL> urls = readURLs(file);
                    
                    for (URL url : urls) {
                        
                        queries.add(URLQueryModel.getInstance(url));

                    }

                } else {

                    System.err.println("Unknown option: " + arg);

                    System.exit(1);

                }

            } else {

                final File file = new File(arg);

                if (!file.exists())
                    throw new FileNotFoundException(file.toString());

                counterSetFiles.add(file);

            }

        }

        if (queries.isEmpty()) {

            throw new RuntimeException("No queries were specified.");

        }

        if (counterSetFiles.isEmpty()) {

            throw new RuntimeException("No counter set files were specified.");

        }

        /*
         * Compute a regular expression which will match anything which would
         * have been matched by the individual URLs. E.g., the OR of the
         * individual regular expressions entailed by each URL when interpreted
         * as a query.
         */
        final Pattern regex;
        {
            final List<Pattern> tmp = new LinkedList<Pattern>();

            for (URLQueryModel model : queries) {

                if (model.pattern != null) {

                    tmp.add(model.pattern);

                }

            }

            regex = QueryUtil.getPattern(tmp);

        }

        /*
         * Read counters accepted by the optional filter into the counter set to
         * be served.
         * 
         * @todo this does not support reading at different periods for each
         * query.
         */

        // The performance counters read from the file(s).
        final CounterSet counterSet = new CounterSet();

        readFiles(counterSetFiles, counterSet, nsamples, PeriodEnum.Minutes,
                regex);

        /*
         * Run each query in turn against the filtered pre-loaded counter set.
         */
        if (log.isInfoEnabled())
            log.info("Evaluating " + queries.size() + " queries.");

        for (URLQueryModel model : queries) {

            try {

                final IRenderer renderer = RendererFactory.get(model,
                        new CounterSetSelector(counterSet), defaultMimeType);

                /*
                 * Render on a file. The file can be specified by a URL query
                 * parameter.
                 * 
                 * @todo Use the munged counter path / counter name (when one
                 * can be identified) as the default filename.
                 */
                File file;

                if (model.file == null) {

                    file = File.createTempFile("query", ".out", outputDir);

                } else {

                    if (!model.file.isAbsolute()) {

                        file = new File(outputDir, model.file.toString());

                    } else {

                        file = model.file;

                    }

                }

                if (file.getParentFile() != null
                        && !file.getParentFile().exists()) {

                    if (log.isInfoEnabled()) {

                        log.info("Creating directory: " + file.getParentFile());

                    }

                    // make sure the parent directory exists.
                    file.getParentFile().mkdirs();

                }

                if (log.isInfoEnabled()) {

                    log.info("Writing file: " + file + " for query: "
                            + model.uri);

                }

                final Writer w = new BufferedWriter(
                        new FileWriter(file, false/* append */));

                try {

                    renderer.render(w);

                    w.flush();

                } finally {

                    w.close();

                }

            } catch (Throwable t) {

                log.error("Could not run query: " + model.uri, t);

            }
            
        }

    }

}
