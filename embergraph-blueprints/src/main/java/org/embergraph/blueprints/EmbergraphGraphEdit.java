package org.embergraph.blueprints;


public class EmbergraphGraphEdit {
    
    public enum Action {
        ADD, REMOVE
    }

    private final Action action;

    private final EmbergraphGraphAtom atom;
    
    private final long timestamp;
    
    public EmbergraphGraphEdit(final Action action, final EmbergraphGraphAtom atom) {
        this(action, atom, 0L);
    }

    public EmbergraphGraphEdit(final Action action, final EmbergraphGraphAtom atom,
            final long timestamp) {
        this.action = action;
        this.atom = atom;
        this.timestamp = timestamp;
    }

    public Action getAction() {
        return action;
    }

    public EmbergraphGraphAtom getAtom() {
        return atom;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getId() {
        return atom.getId();
    }

    @Override
    public String toString() {
        return "EmbergraphGraphEdit [action=" + action + ", atom=" + atom
                + ", timestamp=" + timestamp + "]";
    }

}

