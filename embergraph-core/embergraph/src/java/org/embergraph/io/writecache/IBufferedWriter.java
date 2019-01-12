package org.embergraph.io.writecache;

import org.embergraph.rwstore.RWStore;

public interface IBufferedWriter {

  int getSlotSize(int data_len);

  RWStore.StoreCounters<?> getStoreCounters();
}
