package org.peg4d.vm;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public final class MachineContext {
	ParsingObject left;
    ParsingSource source;
    long pos;
    long fpos = 0;
    long[]   lstack = new long[4096];
    long[]   bstack = new long[4096];
    int      lstacktop = 1;
    ParsingObject[] ostack = new ParsingObject[512];
    int      ostacktop = 0;
    int      bstacktop;
    public final ParsingObject failureResult = new ParsingObject(null, this.source, 0);
    public MachineContext(ParsingObject left, ParsingSource s, long pos) {
    	this.left = left;
    	this.source = s;
    	this.pos = pos;
    	this.lstack[0] = -1;
    }
    
    public final void showPosition(String msg, long pos) {
		System.out.println(this.source.formatErrorMessage("debug", pos, msg));
	}
    
    public final long getPosition() {
    	return this.pos;
    }
    
    public final boolean hasUnconsumedCharacter() {
		return this.source.byteAt(this.pos) != ParsingSource.EOF;
	}
}