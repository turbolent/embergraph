package org.embergraph.btree.raba;

import cutthecrap.utils.striterators.EmptyIterator;
import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.Iterator;

/*
 * An immutable, empty {@link IRaba}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class EmptyRaba implements IRaba, Externalizable {

  /** An empty, immutable B+Tree keys {@link IRaba} instance. */
  public static transient IRaba KEYS = new EmptyKeysRaba();

  /** An empty, immutable B+Tree values {@link IRaba} instance. */
  public static transient IRaba VALUES = new EmptyValuesRaba();

  /*
   * An empty, immutable B+Tree keys {@link IRaba}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  public static class EmptyKeysRaba extends EmptyRaba {

    /** */
    private static final long serialVersionUID = -1171667811365413307L;

    /** De-serialization ctor. */
    public EmptyKeysRaba() {}

    @Override
    public final boolean isKeys() {

      return true;
    }
  }

  /*
   * An empty, immutable B+Tree values {@link IRaba}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  public static class EmptyValuesRaba extends EmptyRaba {

    /** */
    private static final long serialVersionUID = 858342963304055608L;

    /** De-serialization ctor. */
    public EmptyValuesRaba() {}

    @Override
    public final boolean isKeys() {

      return false;
    }
  }

  /** De-serialization ctor. */
  public EmptyRaba() {}

  @Override
  public final int capacity() {
    return 0;
  }

  @Override
  public final boolean isEmpty() {
    return true;
  }

  @Override
  public final boolean isFull() {
    return true;
  }

  @Override
  public final int size() {
    return 0;
  }

  @Override
  public final boolean isReadOnly() {
    return true;
  }

  @Override
  public final boolean isNull(int index) {
    throw new IndexOutOfBoundsException();
  }

  @Override
  public final int length(int index) {
    throw new IndexOutOfBoundsException();
  }

  @Override
  public final byte[] get(int index) {
    throw new IndexOutOfBoundsException();
  }

  @Override
  public final int copy(int index, OutputStream os) {
    throw new IndexOutOfBoundsException();
  }

  @Override
  @SuppressWarnings("unchecked")
  public final Iterator<byte[]> iterator() {
    return EmptyIterator.DEFAULT;
  }

  @Override
  public final int search(byte[] searchKey) {
    if (isKeys()) return -1;
    throw new UnsupportedOperationException();
  }

  @Override
  public final void set(int index, byte[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final int add(byte[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final int add(byte[] value, int off, int len) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final int add(DataInput in, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    // NOP
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    // NOP
  }
}
