package com.bigdata.relation.accesspath;

import java.util.concurrent.Callable;

import com.bigdata.relation.rule.eval.ISolution;

/**
 * Task invokes {@link IBuffer#flush()} and returns its return value.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class FlushBufferTask implements Callable<Long> {
    
    final private IBuffer<ISolution[]> buffer;
    
    public FlushBufferTask(IBuffer<ISolution[]> buffer) {

        if (buffer == null)
            throw new IllegalArgumentException();

        this.buffer = buffer;
        
    }
    
    /**
     * @return The mutation count from {@link IBuffer#flush()}.
     */
    public Long call() {
        
        return buffer.flush();
        
    }
    
}