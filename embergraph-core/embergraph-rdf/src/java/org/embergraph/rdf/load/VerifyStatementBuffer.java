package org.embergraph.rdf.load;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.rio.StatementBuffer;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.striterator.IChunkedOrderedIterator;

/*
 * Statements inserted into the buffer are verified against the database. No new {@link Value}s or
 * {@link Statement}s will be written on the database by this class. The #of {@link URI}, {@link
 * Literal}, and told triples not found in the database are reported by various counters.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @todo The counters are being updated on each incremental write rather than tracked on a per-task
 *     basis and then updated iff the task as a whole succeeds. This causes double-counting of both
 *     found and not found totals when a task errors and then retries. The counters need to be
 *     attached to the task and the task logic extended to capture them rather than to the statement
 *     buffer (a bit of a mess).
 */
public class VerifyStatementBuffer extends StatementBuffer {

  private static final Logger log = Logger.getLogger(VerifyStatementBuffer.class);

  final AtomicLong nterms, ntermsNotFound, ntriples, ntriplesNotFound;

  /*
   * @param database
   * @param capacity
   */
  public VerifyStatementBuffer(
      AbstractTripleStore database,
      int capacity,
      AtomicLong nterms,
      AtomicLong ntermsNotFound,
      AtomicLong ntriples,
      AtomicLong ntriplesNotFound) {

    super(database, capacity);

    this.nterms = nterms;

    this.ntermsNotFound = ntermsNotFound;

    this.ntriples = ntriples;

    this.ntriplesNotFound = ntriplesNotFound;
  }

  /*
   * Overridden to batch verify the terms and statements in the buffer.
   *
   * <p>FIXME Verify that {@link StatementBuffer#flush()} is doing the right thing for this case
   * (esp, how it handles bnodes when appearing as {s,p,o} or when appearing as the statement
   * identifier).
   */
  @Override
  protected void incrementalWrite() {

    if (log.isInfoEnabled()) {
      log.info("numValues=" + numValues + ", numStmts=" + numStmts);
    }

    // Verify terms (batch operation).
    if (numValues > 0) {

      database.getLexiconRelation().addTerms(values, numValues, true /* readOnly */);
    }

    for (int i = 0; i < numValues; i++) {

      final EmbergraphValue v = values[i];

      nterms.incrementAndGet();

      if (v.getIV() == null) {

        log.warn("Unknown term: " + v);

        ntermsNotFound.incrementAndGet();
      }
    }

    // Verify statements (batch operation).
    if (numStmts > 0) {

      final SPO[] a = new SPO[numStmts];
      final EmbergraphStatement[] b = new EmbergraphStatement[numStmts];

      // #of SPOs generated for testing.
      int n = 0;

      for (int i = 0; i < numStmts; i++) {

        final EmbergraphStatement stmt = stmts[i];

        ntriples.incrementAndGet();

        if (!stmt.isFullyBound()) {

          log.warn("Unknown statement (one or more unknown terms) " + stmt);

          ntriplesNotFound.incrementAndGet();

          continue;
        }

        a[n] = new SPO(stmt);

        b[n] = stmt;

        n++;
      }

      final IChunkedOrderedIterator<ISPO> itr = database.bulkCompleteStatements(a, n);

      try {

        while (itr.hasNext()) {

          itr.next();
        }

      } finally {

        itr.close();
      }

      for (int i = 0; i < n; i++) {

        final ISPO spo = a[i];

        if (!spo.hasStatementType()) {

          ntriplesNotFound.incrementAndGet();

          log.warn("Statement not in database: " + b[i] + " (" + spo + ")");

          continue;
        }

        if (spo.getStatementType() != StatementEnum.Explicit) {

          ntriplesNotFound.incrementAndGet();

          log.warn(
              "Statement not explicit database: "
                  + b[i]
                  + " is marked as "
                  + spo.getStatementType());

        }
      }
    }

    // Reset the state of the buffer (but not the bnodes nor deferred stmts).
    _clear();
  }
}
