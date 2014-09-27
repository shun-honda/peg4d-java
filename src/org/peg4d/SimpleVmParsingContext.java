package org.peg4d;

import java.io.UnsupportedEncodingException;

import org.peg4d.model.ParsingModel;
import org.peg4d.vm.Opcode;

public class SimpleVmParsingContext extends ParsingContext {
	
	public SimpleVmParsingContext(ParsingSource s, long pos, int stacksize, ParsingMemo memo) {
		super(s, pos, stacksize, memo);
	}
	
	public void opMatchCharset(byte[] bdata) {
		try {
			String text = new String(bdata, "UTF-8");
			ParsingCharset u = ParsingCharset.newParsingCharset(text);
			int consume = u.consume(this.source, pos);
			if(consume > 0) {
				this.consume(consume);
			}
			else {
				this.opFailure();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void opMatchText(Opcode op) {
		if(op.bdata != null) {
			if(this.source.match(this.pos, op.bdata)) {
				this.consume(op.bdata.length);
			}
			else {
				this.opFailure();
			}
		}
		else {
			if(this.source.byteAt(this.pos) == op.ndata) {
				this.consume(1);
			}
			else {
				this.opFailure();
			}
		}
	}
	
	public void opConnectObject(int index) {
		ParsingObject parent = this.opop();
		parent.setLength(parent.getLength() + this.left.getLength());
		parent.set(parent.size(), this.left);
		this.left = parent;
	}
	
	public void opNewObject(ParsingExpression p) {
		if(this.canTransCapture()) {
			ParsingModel model = new ParsingModel();
			this.left = new ParsingObject(model.get("#empty"), this.source, this.pos, p);
		}
	}
	
	public void opTagging(Opcode op) {
		if(this.canTransCapture()) {
			String tagName;
			try {
				tagName = new String(op.bdata, "UTF-8");
				ParsingTag tag = new ParsingTag(tagName);
				this.left.setTag(tag.tagging());
				this.left.setLength((int) (this.pos - this.lstack[this.lstacktop - 1]));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void opRememberSequencePosition() {
		lpush(this.pos);
		lpush(this.pos);
		opush(this.left);
	}
	
	public void opForgetFailurePosition() {
		lpop();
		this.fpos = this.lstack[this.lstacktop];
	}
	
	public void opBackTrackSequencePosition() {
		this.left = opop();
		lpop();
		lpop();
		this.rollback(this.lstack[this.lstacktop]);
	}
	
	public final void opStoreObject() {
		this.opush(this.left);
	}

	public final void opDropStoredObject() {
		this.opop();
	}

	public final void opRestoreObject() {
		this.left = this.opop();
	}
	
	public final void opRestoreNegativeObject() {
		if(this.isFailure()) {
			this.left = this.opop();
		}
		else {
			this.opop();
			this.opFailure();
		}
	}
	
	public final void opFailure() {
		if(this.pos >= fpos) {  // adding error location
			this.fpos = this.pos;
		}
		this.left = null;
	}
	
/*	public final void opMatchText(byte[] t) {
		if(this.source.match(this.pos, t)) {
			this.consume(t.length);
		}
		else {
			this.opFailure();
		}
	}*/
	
	public final void opMatchAnyChar() {
		if(this.source.charAt(this.pos) != -1) {
			int len = this.source.charLength(this.pos);
			this.consume(len);
		}
		else {
			this.opFailure();
		}
	}
}
