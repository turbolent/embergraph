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
 * Created on Aug 22, 2006
 */

package org.embergraph.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase2;

/*
 * Low level IO performance tests in support of embergraph design options.
 *
 * @todo Develop test to compare efficiency of an ordered write to random writes of the same pages.
 *     Use a random sequence of pages. Selection without replacement is an option, but not required.
 *     The store size should be larger than the #of pages to be written, and the #of pages to be
 *     written should be at least 5x the disk cache (8-32M).
 * @todo Installation reads are when you need to read a page from the database so that you can
 *     update some rows on that page from a journal. Write tests to determine if installation reads
 *     might be optimized by ordered reads from a <em>region</em> of the database to get the pages
 *     into memory followed by updating those pages from the journal and then an ordered write to
 *     install the dirty pages back onto the database. Unfortunately you can not use nio to directly
 *     communicate an ordered write, e.g., with an array of buffers together with their target file
 *     offsets. That is a situation then where not writing through the disk cache and letting the
 *     drive optimize a series of queued write operations would be advantageous.
 *     <p>Explore the possibility of asynchronous IO vs synchronous IO for the database. I think
 *     that you would have to use the FileChannel directly from multiple threads in order to make
 *     asynchronous requests to multiple offsets and get some queue depth for the read/write
 *     operations (FileChannel does not support asynchronous operations from a single thread).
 *     Otherwise the ordered reads (and ordered writes) will involve yielding to permit each IO to
 *     be synchronous. That could still be efficient if you could interleave a bunch of IOs onto the
 *     same track of the disk, reading pages, updating them from the most recent committed state of
 *     the objects in the journal for that page, and then writing pages once they had been updated.
 *     That would minimize head movement since you are just waiting for the right part of the disk
 *     to come around again. If I can identify AIO support for FileChannel then refactor appropriate
 *     tests into a TestAIO class.
 * @see Detailed information about storage hardware is available under windows using <code>
 *     Programs > Accessories > System Tools > System Information</code>. See the <code>
 *      Components > Storage > Disks </code> view.
 * @see http://www.jroller.com/page/cpurdy/20040907
 * @see http://alphaworks.ibm.com/tech/aio4j
 * @see http://coconut.codehaus.org/
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestIO extends TestCase2 {

  /** */
  public TestIO() {
    this(null);
  }

  /** @param arg0 */
  public TestIO(String arg0) {
    super(arg0);
    /*
     * Set up formatting for integers.
     */

    nf = NumberFormat.getIntegerInstance();

    // grouping is disabled since we write comma-separated-value files.
    nf.setGroupingUsed(true);

    fpf = NumberFormat.getNumberInstance();

    fpf.setGroupingUsed(false);

    fpf.setMaximumFractionDigits(2);
  }

  /*
   * Formatting useful for integers and floating point values that need to be rounded to integers.
   * If the value is in milliseconds, and you want to write it in seconds then first divide by 1000.
   * If the value is units per millisecond and you want to write units per second, then compute and
   * format <code>units/milliseconds*1000</code>.
   */
  final NumberFormat nf;

  /** Formatting useful for floating point values with at most two digits after the decimal. */
  final NumberFormat fpf;

  /*
   * Computes units/second given units and nanoseconds.
   *
   * @param units The units, e.g., the #of triples loaded.
   * @param nanos The nanoseconds.
   * @return Units/seconds, e.g., the #of pages written per second. If <i>nanos</i> is zero(0) then
   *     this method returns zero.
   */
  private double getUnitsPerSecond(long units, long nanos) {

    if (nanos == 0) return 0d;

    return (((double) units) / nanos) * 1000000000L;
  }

  public void test_formats() {

    System.err.println("\nnf: ");
    System.err.println("12.1 : " + nf.format(12.1f));
    System.err.println("12.5 : " + nf.format(12.5f));
    System.err.println("12.6 : " + nf.format(12.6f));
    System.err.println("112.5 : " + nf.format(112.5f));
    System.err.println("1125.5 : " + nf.format(1125.5f));

    /*
     * Example of formatting for a units/sec value. Note the cast to
     * floating point before dividing the units by the milliseconds and
     * _then_ multiple through by 1000 to get units/sec.
     */
    System.err.println("400/855*1000 : " + nf.format(400. / 855 * 1000));

    System.err.println("\nfpf: ");
    System.err.println("12.1 : " + fpf.format(12.1f));
    System.err.println("12.5 : " + fpf.format(12.5f));
    System.err.println("12.6 : " + fpf.format(12.6f));
    System.err.println("112.5 : " + fpf.format(112.5f));
    /*
     * Example of formatting for a units/sec value. Note the cast to
     * floating point before dividing the units by the milliseconds and
     * _then_ multiple through by 1000 to get units/sec.
     */
    System.err.println("400/855*1000 : " + fpf.format(400. / 855 * 1000));
  }

  public void test_units() {

    System.err.println("One second is " + nf.format(TimeUnit.SECONDS.toNanos(1)) + " nanoseconds");
    System.err.println(
        "One second is " + nf.format(TimeUnit.SECONDS.toMicros(1)) + " microseconds");
    System.err.println(
        "One second is " + nf.format(TimeUnit.SECONDS.toMillis(1)) + " milliseconds");

    System.err.println("One kilobyte is " + nf.format(KiloByte));
    System.err.println("One megabyte is " + nf.format(MegaByte));
    System.err.println("One gigabyte is " + nf.format(GigaByte));
    System.err.println("One terabyte is " + nf.format(TeraByte));
    System.err.println("One petabyte is " + nf.format(PetaByte));
    System.err.println("One exabyte  is " + nf.format(ExaByte));
    //        System.exit(1);

  }

  private static final int KiloByte = 1024;
  private static final int MegaByte = 1024 * 1024;
  private static final int GigaByte = 1024 * 1024 * 1024;
  private static final int TeraByte = 1024 * 1024 * 1024 * 1024;
  private static final int PetaByte = 1024 * 1024 * 1024 * 1024 * 1024;
  private static final int ExaByte = 1024 * 1024 * 1024 * 1024 * 1024 * 1024;

  /*
   * Test of raw IO performance for random writes.
   *
   * @throws IOException
   */
  public void test_001() throws IOException {

    // page size.
    final int pageSize = 8 * KiloByte;
    System.err.println("pageSize=" + nf.format(pageSize) + " bytes");

    // assumption about the disk cache size.
    final int diskCacheSize = 8 * MegaByte;
    System.err.println("diskCacheSize=" + nf.format(diskCacheSize) + " bytes");

    final int pagesInDiskCache = diskCacheSize / pageSize;
    System.err.println("pagesInDiskCache=" + nf.format(pagesInDiskCache));

    /*
     * #of pages to write. This value effects the utility of the disk cache.
     * If all pages fit in the cache, then the writes can be absorbed
     * directly by the cache without pausing for disk IO. As the #of pages
     * written begins to exceed the cache, the cache becomes less effective.
     * Since this benchmark is meant to indicate sustained load, this value
     * should be at least 5x.
     */
    final int pagesToWrite = 10 * pagesInDiskCache;
    System.err.println("pagesToWrite=" + nf.format(pagesToWrite));

    /*
     * maximum file length (in pages). This value effects the amount that
     * the head must move when seeking within the file. In order to be
     * effective, this value should be choosen based on knowledge of the
     * format of the disk, including the sector size and the #of sectors per
     * track. When IOs are located within the same track, the head stays
     * still and waits for the right sector to come around. When IOs cover
     * multiple tracks, the head must seek among those tracks. The maximum
     * value is limited by the free space on the drive. The drive should be
     * defragmented before running this test so that the allocated file will
     * be a contiguous extent to the greatest extent possible.
     */
    final int maxPages = pagesToWrite * 10;
    System.err.println(
        "maxPages="
            + nf.format(maxPages)
            + ", maxLength="
            + nf.format(((long) maxPages * pageSize)));

    // when true, the data will write through to disk with each IO.
    final boolean writeThrough = true;

    // when true, the data will be forced to disk after it is all written.
    final boolean synchAfterTest = true;

    final Random r = new Random();

    /*
     * Create a temporary file for the test. You can specify the directory
     * using an optional argument as a means of choosing which disk drive or
     * partition to use for the test.
     */
    final File file =
        File.createTempFile(
            "test", ".dbCache"
            //                , new File("D:/")
            );

    System.err.println("file=" + file);

    final RandomAccessFile raf = new RandomAccessFile(file, (writeThrough ? "rwd" : "rw"));

    try {

      final FileChannel fileChannel = raf.getChannel();

      /*
       * Allocate direct buffer.
       */
      final ByteBuffer buf = ByteBuffer.allocateDirect(pageSize);

      /*
       * Extend the file to its maximum size. We set the limit to one
       * before extending the file so that we only write the very last
       * byte of the extent. We then restore the limit to the capacity of
       * the buffer, since that is its initial condition and the
       * assumption through the rest of this code.
       */
      assert buf.limit() == buf.capacity();
      long maxOffset = (long) maxPages * pageSize - 1;
      buf.limit(1);
      fileChannel.write(buf, maxOffset);
      buf.limit(buf.capacity());

      long startNanos = System.nanoTime();

      for (int i = 0; i < pagesToWrite; i++) {

        // offset of page.
        final long pos = r.nextInt(maxPages) * (long) pageSize;
        assert pos <= maxOffset;

        /*
         * Reset position so that the next write will transfer the
         * entire buffer contents. If you don't do this then it will
         * only write the buffer the first time through since the
         * position defaults to zero and the limit defaults to the
         * capacity.
         */
        // buf.limit(pageSize);
        buf.position(0);

        final int nwritten;
        try {
          nwritten = fileChannel.write(buf, pos);
        } catch (IllegalArgumentException ex) {
          System.err.println("pos=" + pos);
          throw ex;
        }

        assertEquals("iteration=" + i + ", nwritten", pageSize, nwritten);
      }

      if (synchAfterTest) {
        // force data, but do not force metadata.
        fileChannel.force(false);
      }

      final long endNanos = System.nanoTime();

      final long elapsedNanos = endNanos - startNanos;

      // System.err.println("startNanos="+nf.format(startNanos));
      // System.err.println("endNanos="+nf.format(endNanos));
      System.err.println(
          "Wrote "
              + nf.format(pagesToWrite)
              + " pages of "
              + pageSize
              + " bytes in "
              + TimeUnit.NANOSECONDS.toSeconds(elapsedNanos)
              + " secs");
      System.err.println(
          "" + nf.format(getUnitsPerSecond(pagesToWrite, elapsedNanos)) + " pages per second");
      final long megabytesWritten = pagesToWrite * pageSize / MegaByte;
      System.err.println("" + fpf.format(megabytesWritten) + " megabytes written");
      System.err.println(
          ""
              + fpf.format(getUnitsPerSecond(megabytesWritten, elapsedNanos))
              + " megabytes per second");
      System.err.println(
          ""
              + fpf.format(getUnitsPerSecond(megabytesWritten * 8, elapsedNanos))
              + " megabits per second");
      System.err.println("writeThroughIOs=" + writeThrough);
      System.err.println("synchAfterTest=" + synchAfterTest);

      System.err.println("bytes on disk: " + nf.format(raf.length()));

    } finally {

      raf.close();

      if (!file.delete()) {
        throw new RuntimeException("Could not delete file: " + file);
      }

      if (log.isInfoEnabled()) log.info("deleted: " + file);
    }
  }

  /*
   * Low level test of the ability to modify a file a sync it to the underlying storage media. This
   * test is interested in the absolute maximum syncs per second that the hardware can support.
   *
   * @throws IOException
   */
  public void test_commitsPerSec() throws IOException {

    // page size.
    final int pageSize = 512; // * KiloByte;
    if (log.isInfoEnabled()) log.info("pageSize=" + nf.format(pageSize) + " bytes");

    /*
     * The #of commits. We update the same page for each commit and then
     * sync the FileChannel to the disk.
     */
    final int numCommits = 10000;
    if (log.isInfoEnabled()) log.info("numCommits=" + nf.format(numCommits) + " commits");

    /*
     * Create a temporary file for the test. You can specify the directory
     * using an optional argument as a means of choosing which disk drive or
     * partition to use for the test.
     */
    final File file = File.createTempFile("test", ".dbCache");

    final RandomAccessFile raf = new RandomAccessFile(file, "rw");

    try {

      final FileChannel fileChannel = raf.getChannel();

      /*
       * Allocate direct buffer.
       */
      final ByteBuffer buf = ByteBuffer.allocateDirect(pageSize);

      /*
       * Extend the file to its maximum size. We set the limit to one
       * before extending the file so that we only write the very last
       * byte of the extent. We then restore the limit to the capacity of
       * the buffer, since that is its initial condition and the
       * assumption through the rest of this code.
       */
      assert buf.limit() == buf.capacity();
      FileChannelUtility.writeAll(fileChannel, buf, 0L /* pos */);

      long startNanos = System.nanoTime();

      // Always update the first byte.
      final long pos = 0L;

      for (int i = 0; i < numCommits; i++) {

        buf.position(0);
        buf.limit(pageSize);

        // overwrite the buffer.
        final byte v = (byte) (i % 256);
        for (int off = 0; off < pageSize; off++) {
          buf.put(off /* index */, v /* newValue */);
        }

        // write on page.
        FileChannelUtility.writeAll(fileChannel, buf, pos);

        // Sync the channel to the disk.
        fileChannel.force(false /* metadata */);
      }

      final long endNanos = System.nanoTime();

      final long elapsedNanos = endNanos - startNanos;

      System.err.println(
          "Did "
              + nf.format(numCommits)
              + " commits in "
              + TimeUnit.NANOSECONDS.toSeconds(elapsedNanos)
              + " secs");
      System.err.println(
          "" + nf.format(getUnitsPerSecond(numCommits, elapsedNanos)) + " pages per second");

    } finally {

      raf.close();

      if (!file.delete()) {
        throw new RuntimeException("Could not delete file: " + file);
      }
    }
  }
}
