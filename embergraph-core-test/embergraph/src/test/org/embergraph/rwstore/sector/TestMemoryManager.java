package org.embergraph.rwstore.sector;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase2;
import org.embergraph.io.DirectBufferPool;
import org.embergraph.rawstore.IAllocationContext;
import org.embergraph.rawstore.IPSOutputStream;
import org.embergraph.util.DaemonThreadFactory;
import org.embergraph.util.InnerCause;

public class TestMemoryManager extends TestCase2 {

  final int sectorSize = 10 * 1024 * 1024; // 10M

  private MemoryManager manager;

  private char[] c_testData;

  private Random r;

  protected void setUp() throws Exception {
    r = new Random();
    manager = new MemoryManager(DirectBufferPool.INSTANCE, 10);
    c_testData = genTestData();
  }

  protected void tearDown() throws Exception {
    manager.clear();
    r = null;
    manager = null;
    c_testData = null;
    super.tearDown();
  }

  private char[] genTestData() {
    String src = "The quick brown fox jumped over the lazy dog";

    StringBuffer buf = new StringBuffer();
    while (buf.length() < (20 * 1024)) buf.append(src);

    return buf.toString().toCharArray();
  }

  private String genString(int min, int i) {
    return new String(c_testData, 0, min + r.nextInt(i - min));
    // return new String(c_testData, 0, i - 20);
  }

  public void testSimpleAllocations() {

    String helloWorld = "Hello World";

    final long saddr = allocate(manager, helloWorld);

    String retstr = getString(saddr);

    assertTrue(helloWorld.equals(retstr));

    manager.free(saddr);

    try {
      getString(saddr);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      // expected
    }

    final long saddr2 = allocate(manager, helloWorld);

    assertTrue(saddr == saddr2);
  }

  /*
   * The address mappings are between the integer allocation address and the sector index (16 signed
   * bits) and offset (16 unsigned bits).
   */
  public void testAddressMappings() {
    int i = 10000000;
    while (--i > 0) {
      // final int sector = r.nextInt(12);
      final int sector = r.nextInt(32 * 1024);
      final int bit = r.nextInt(32 * 1024);
      final int rwaddr = SectorAllocator.makeAddr(sector, bit);
      final int rsector = SectorAllocator.getSectorIndex(rwaddr);
      final int rbit = SectorAllocator.getSectorOffset(rwaddr);
      assertTrue(
          "Address Error "
              + i
              + " , sector: "
              + sector
              + " != "
              + rsector
              + " or "
              + bit
              + " != "
              + rbit,
          (sector == rsector) && (bit == rbit));
    }
  }

  public void testStressAllocations() {

    for (int i = 0; i < 20; i++) {

      doStressAllocations(manager, true, 50000, 5 + r.nextInt(5000));
    }
  }

  public void testSimpleBlob() {

    final String blob = new String(c_testData, 0, 11000);

    final long slotBytesBefore = manager.getSlotBytes();

    final long saddr = allocate(manager, blob);

    final long slotBytesAfterAlloc = manager.getSlotBytes();

    final String retstr = getString(saddr);

    assertTrue(blob.equals(retstr));

    manager.free(saddr);

    final long slotBytesAfterFree = manager.getSlotBytes();

    assertEquals("slotBytes", slotBytesBefore, slotBytesAfterFree);

    // verify that the memory was immediately recycled.
    assertEquals("saddr", saddr, allocate(manager, blob));
  }

  public void testAllocationContexts() {

    final IMemoryManager context = manager.createAllocationContext();

    for (int i = 0; i < 500; i++) {

      doStressAllocations(context, false, 5000, 5 + r.nextInt(3000));

      context.clear();
    }
  }

  /** Rather than creating contexts directly, instead associate with externally created context. */
  public void testNestedAllocationContexts() {
    final IAllocationContext context = manager.newAllocationContext(true);

    final String test = "Hello World";

    final long addr1 = allocate(manager, context, test);
    final long addr2 = allocate(manager, context, test);

    assertTrue(addr1 != addr2);

    final String res = getString(addr1);

    assertTrue(res.equals(test));

    manager.free(addr1, context);

    // check recycling of original allocation
    final long addr3 = allocate(manager, context, test);

    // should not be recycled because of session protection
    assertTrue(addr1 != addr3);

    manager.detachContext(context);

    // now check recycling with new context
    final IAllocationContext context2 = manager.newAllocationContext(true);
    final long addr4 = allocate(manager, context2, test);

    // confirm that addr1 is now available for use
    assertTrue(addr1 == addr4);
  }

  public void testAllocationContextsStreams() throws IOException {

    final IMemoryManager context = manager.createAllocationContext();

    final String ss = "Just a simple string object";
    for (int i = 0; i < 500; i++) {

      IPSOutputStream pout = context.getOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(pout);

      for (int t = 0; t < 100; t++) {
        out.writeObject(ss);
      }

      out.close();

      final long addr = pout.getAddr();
      InputStream pin = context.getInputStream(addr);
      ObjectInputStream in = new ObjectInputStream(pin);
      int reads = 0;
      try {
        while (true) {
          assertTrue(ss.equals(in.readObject()));
          reads++;
        }
      } catch (EOFException eof) {
        // expected
      } catch (ClassNotFoundException e) {
        fail("Unexpected ClassNotFoundException");
      }

      assertTrue(reads == 100);

      context.clear();

      assertTrue(manager.getSlotBytes() == 0);
    }
  }

  public void testStressConcurrent() throws InterruptedException {

    final int nclients = 20;

    final ExecutorService executorService =
        Executors.newFixedThreadPool(nclients, DaemonThreadFactory.defaultThreadFactory());

    final Collection<Callable<Long>> tasks = new HashSet<Callable<Long>>();
    for (int i = 0; i < nclients; i++) {
      tasks.add(
          new Callable<Long>() {
            public Long call() throws Exception {
              try {
                doStressAllocations(manager, false, 50000, 5 + r.nextInt(600));
              } catch (Throwable t) {
                t.printStackTrace();
              }
              return null;
            }
          });
    }

    executorService.invokeAll(tasks);
    executorService.awaitTermination(5, TimeUnit.SECONDS);
  }

  public void doStressAllocations(
      final IMemoryManager mm, final boolean clear, final int tests, final int maxString) {
    if (clear) mm.clear();

    int allocs = 0;
    int frees = 0;
    final ArrayList<Long> addrs = new ArrayList<Long>();
    try {
      for (int i = 0; i < tests; i++) {
        final long addr1 = allocate(mm, genString(1, maxString));
        allocs++;
        if ((i % 2) == 0) {
          addrs.add(Long.valueOf(addr1));
        } else if (i > 1000) {
          final int f = r.nextInt(addrs.size());
          final long faddr = addrs.remove(f).longValue();
          mm.free(faddr);
          frees++;
        }
      }
    } catch (MemoryManagerOutOfMemory err) {
      /*
       * all okay - some of the tests are designed to exercise the maximum
       * capacity granted to the memory manager.
       */
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + err);
    }
  }

  /** Allocate a blob the size of a single sector. */
  public void test_largeBlobAllocation() {

    final int sectorSize = manager.getSectorSize();

    assertTrue(manager.getSlotBytes() == 0L);

    final long addr = manager.allocate(sectorSize, false /* blocks */);

    if (log.isInfoEnabled())
      log.info(
          "Manager addr="
              + addr
              + ", sizeof(addr)="
              + manager.allocationSize(addr)
              + ", slotBytes: "
              + manager.getSlotBytes());

    assertEquals("allocationSize", sectorSize, manager.allocationSize(addr));

    manager.free(addr);

    assertTrue(manager.getSlotBytes() == 0L);
  }

  /*
   * Unit test in which we verify that a thread will block awaiting an allocation until another
   * thread releases an allocation, thereby making enough memory available for the thread to
   * continue.
   *
   * @throws InterruptedException
   * @throws TimeoutException
   * @throws ExecutionException
   *     <p>TODO Version of test with lt blob size allocations, with blob size allocations, and with
   *     sector (or more) sized allocations.
   */
  public void test_blockingAllocation()
      throws InterruptedException, ExecutionException, TimeoutException {

    final int sectorSize = manager.getSectorSize(); // allow for blob

    // grab all the memory (for this size of allocation).
    final List<Long> addrs = new LinkedList<Long>();
    while (true) {
      try {
        final long addr = manager.allocate(sectorSize, false /* blocks */);
        addrs.add(addr);
        if (log.isInfoEnabled())
          log.info(
              "Manager addr="
                  + addr
                  + ", sizeof(addr)="
                  + manager.allocationSize(addr)
                  + ", slotBytes: "
                  + manager.getSlotBytes());
      } catch (MemoryManagerResourceError err) {
        if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + err);
        break;
      }
    }

    // Must have allocated something.
    assertFalse(addrs.isEmpty());

    if (log.isInfoEnabled()) log.info("#addrs=" + addrs.size() + ", addrs: " + addrs);

    //		// The last address allocated.
    //		final long lastAddr = addrs.get(addrs.size());
    //		final long lastAddr2 = addrs.size() >= 2 ? addrs.get(addrs.size() - 1)
    //				: 0L;

    final FutureTask<Long> ft =
        new FutureTask<Long>(
            new Callable<Long>() {
              public Long call() throws Exception {
                try {
                  if (log.isInfoEnabled())
                    log.info(
                        "Attempting blocking allocation: slotBytes: " + manager.getSlotBytes());
                  /*
                   * blocking allocation of the same size that was
                   * just refused.
                   */
                  return manager.allocate(sectorSize);
                } catch (Throwable t) {
                  if (InnerCause.isInnerCause(t, InterruptedException.class)) {
                    fail("Not expecting interrupt");
                  }
                  throw new RuntimeException(t);
                }
              }
            });

    final ExecutorService service = Executors.newSingleThreadExecutor();

    try {

      if (log.isInfoEnabled()) log.info("Manager slotBytes: " + manager.getSlotBytes());

      service.execute(ft);

      while (!addrs.isEmpty()) {
        final long addr = addrs.remove(0);

        log.info(
            "freeing: addr="
                + addr
                + ", sizeof(addr)="
                + manager.allocationSize(addr)
                + ", slotBytes="
                + manager.getSlotBytes());

        manager.free(addr);
        //				Thread.sleep(10/*ms*/);
      }

      //			{
      //				final long addr = addrs.remove(0);
      //
      //				log.info("freeing: addr=" + addr + ", sizeof(addr)="
      //						+ manager.allocationSize(addr) + ", slotBytes="
      //						+ manager.getSlotBytes());
      //
      //				manager.free(addr);
      //			}

      final long addr = ft.get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

      if (log.isInfoEnabled())
        log.info("Released allocation: slotBytes: " + manager.getSlotBytes());

      manager.free(addr);

      log.info(
          "freeing: addr="
              + addr
              + ", sizeof(addr)="
              + manager.allocationSize(addr)
              + ", slotBytes="
              + manager.getSlotBytes());

      assertTrue(manager.getSlotBytes() == 0);

    } finally {

      service.shutdownNow();
    }

    //		// Now release everything else.
    //		for (Long addr : addrs) {
    //
    //			log.info("freeing: addr=" + addr + ", sizeof(addr)="
    //					+ manager.allocationSize(addr) + ", slotBytes="
    //					+ manager.getSlotBytes());
    //
    //			manager.free(addr);
    //
    //		}

    if (log.isInfoEnabled()) log.info("Manager slotBytes: " + manager.getSlotBytes());
  }

  public void test_stressBlockingAllocation()
      throws InterruptedException, ExecutionException, TimeoutException {
    for (int i = 0; i < 50; i++) {
      test_blockingAllocation();
    }
  }

  /** Unit test for reading a copy of the data from the {@link IMemoryManager}. */
  public void test_read() {

    final byte[] expected = new byte[1000];
    r.nextBytes(expected);

    final long addr = manager.allocate(ByteBuffer.wrap(expected));

    final byte[] actual = manager.read(addr);

    assertEquals(expected, actual);
  }

  private String getString(final long saddr) {

    final StringBuffer sb = new StringBuffer();

    final ByteBuffer[] bufs = manager.get(saddr);

    for (int i = 0; i < bufs.length; i++) {
      final byte[] data;
      if (bufs[i].isDirect()) {
        final ByteBuffer indbuf = ByteBuffer.allocate(bufs[i].remaining());
        data = indbuf.array();
        indbuf.put(bufs[i]);
        indbuf.flip();
      } else {
        data = bufs[i].array();
      }

      sb.append(new String(data));
    }

    return sb.toString();
  }

  private long allocate(final IMemoryManager mm, String val) {

    final ByteBuffer bb = ByteBuffer.wrap(val.getBytes());

    return mm.allocate(bb, false /* blocks */);
  }

  private long allocate(final IMemoryManager mm, final IAllocationContext context, String val) {

    final ByteBuffer bb = ByteBuffer.wrap(val.getBytes());

    return mm.allocate(bb, context);
  }
}
