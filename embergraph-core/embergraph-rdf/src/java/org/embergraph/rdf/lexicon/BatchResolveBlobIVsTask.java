package org.embergraph.rdf.lexicon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.impl.BlobIV;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;

/*
 * Batch resolve {@link BlobIV}s to RDF {@link Value}s.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
class BatchResolveBlobIVsTask implements Callable<Void> {

  //    static private final transient Logger log = Logger
  //            .getLogger(BatchResolveBlobIVsTask.class);

  private final ExecutorService service;
  private final IIndex ndx;
  private final Collection<BlobIV<?>> ivs;
  private final ConcurrentHashMap<IV<?, ?> /* iv */, EmbergraphValue /* term */> ret;
  private final ITermCache<IV<?, ?>, EmbergraphValue> termCache;
  private final EmbergraphValueFactory valueFactory;
  private final int MAX_CHUNK;

  public BatchResolveBlobIVsTask(
      final ExecutorService service,
      final IIndex ndx,
      final Collection<BlobIV<?>> ivs,
      final ConcurrentHashMap<IV<?, ?> /* iv */, EmbergraphValue /* term */> ret,
      final ITermCache<IV<?, ?>, EmbergraphValue> termCache,
      final EmbergraphValueFactory valueFactory,
      final int chunkSize) {

    this.service = service;

    this.ndx = ndx;

    this.ivs = ivs;

    this.ret = ret;

    this.termCache = termCache;

    this.valueFactory = valueFactory;

    this.MAX_CHUNK = chunkSize;
  }

  public Void call() {

    final int numNotFound = ivs.size();

    // An array of IVs that to be resolved against the index.
    final BlobIV<?>[] notFound = ivs.toArray(new BlobIV[numNotFound]);

    // Sort IVs into index order.
    Arrays.sort(notFound, 0, numNotFound);

    // Encode IVs as keys for the index.
    final byte[][] keys = new byte[numNotFound][];
    {
      final IKeyBuilder keyBuilder = KeyBuilder.newInstance();

      for (int i = 0; i < numNotFound; i++) {

        keys[i] = notFound[i].encode(keyBuilder.reset()).getKey();
      }
    }

    if (numNotFound < MAX_CHUNK) {

      /*
       * Resolve everything in one go.
       */

      new ResolveBlobsTask(
              ndx,
              0 /* fromIndex */,
              numNotFound /* toIndex */,
              keys,
              notFound,
              ret,
              termCache,
              valueFactory)
          .call();

    } else {

      /*
       * Break it down into multiple chunks and resolve those chunks
       * in parallel.
       */

      // #of elements.
      final int N = numNotFound;
      // target maximum #of elements per chunk.
      final int M = MAX_CHUNK;
      // #of chunks
      final int nchunks = (int) Math.ceil((double) N / M);
      // #of elements per chunk, with any remainder in the last chunk.
      final int perChunk = N / nchunks;

      // System.err.println("N="+N+", M="+M+", nchunks="+nchunks+", perChunk="+perChunk);

      final List<Callable<Void>> tasks = new ArrayList<>(nchunks);

      int fromIndex = 0;
      int remaining = numNotFound;

      for (int i = 0; i < nchunks; i++) {

        final boolean lastChunk = i + 1 == nchunks;

        final int chunkSize = lastChunk ? remaining : perChunk;

        final int toIndex = fromIndex + chunkSize;

        remaining -= chunkSize;

        // System.err.println("chunkSize=" + chunkSize
        // + ", fromIndex=" + fromIndex + ", toIndex="
        // + toIndex + ", remaining=" + remaining);

        tasks.add(
            new ResolveBlobsTask(
                ndx, fromIndex, toIndex, keys, notFound, ret, termCache, valueFactory));

        fromIndex = toIndex;
      }

      try {

        // Run tasks.
        final List<Future<Void>> futures = service.invokeAll(tasks);

        // Check futures.
        for (Future<?> f : futures) f.get();

      } catch (Exception e) {

        throw new RuntimeException(e);
      }
    }

    //          final long elapsed = System.currentTimeMillis() - begin;
    //
    //          if (log.isInfoEnabled())
    //              log.info("resolved " + numNotFound + " terms in "
    //                      + tasks.size() + " chunks and " + elapsed + "ms");

    // Done.
    return null;
  }
}
