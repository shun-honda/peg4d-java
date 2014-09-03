package org.peg4d.vm;

import java.io.UnsupportedEncodingException;

import org.peg4d.Grammar;
import org.peg4d.PExpression;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;
import org.peg4d.ParsingTag;
import org.peg4d.model.ParsingModel;

public class Machine {

	private static ParsingObject failure(MachineContext c) {
		return c.failureResult;
	}
	
	private static void link(ParsingObject parent, ParsingObject child) {
		parent.setLength(parent.getLength() + child.getLength());
		parent.set(parent.size(), child);
	}

	public final static void PUSH_POS(MachineContext c) {
	    c.lstack[c.lstacktop] = c.pos;
	    c.lstacktop++;
	}

	public final static void POP_POS(MachineContext c) {
	    c.lstacktop--;
	}

	public final static void POP_POS_BACK(MachineContext c) {
	    c.lstacktop--;
	    c.pos = c.lstack[c.lstacktop];
	}

	public final static void PUSH_BUFPOS(MachineContext c) {
	    c.lstack[c.lstacktop] = c.bstacktop;
	    c.lstacktop++;
	}

	public final static void POP_BUFPOS(MachineContext c) {
	    c.lstacktop--;
	}

	public final static void POP_BUFPOS_BACK(MachineContext c) {
	    c.lstacktop--;
	    c.bstacktop = (int)c.lstack[c.lstacktop];
	}

	public final static void PUSH_FPOS(MachineContext c) {
	    c.bstack[c.bstacktop] = c.fpos;
	    c.bstacktop++;
	}

	public final static void POP_FPOS(MachineContext c) {
	    c.bstacktop--;
	}

	public final static void POP_FPOS_FORGET(MachineContext c) {
	    c.bstacktop--;
	    c.fpos = c.bstack[c.bstacktop];
	}
	
	public final static void PUSH_LEFT(MachineContext c) {
	    c.ostack[c.ostacktop] = c.left;
	    c.ostacktop++;
	}

	public final static void POP_LEFT(MachineContext c)
	{
	    c.ostacktop--;
	    c.left = c.ostack[c.ostacktop];
	}

	public final static void POP_LEFT_IFFAIL(MachineContext c)
	{
	    c.ostacktop--;
	    if(c.left == null) {
	        c.left = c.ostack[c.ostacktop];
	    }
	}

	public final static void POP_LEFT_NOT(MachineContext c)
	{
	    c.ostacktop--;
	    if(c.left == null) {
	        c.left = c.ostack[c.ostacktop];
	    }
	    else {
	        c.left = failure(c);
	    }
	}

	public final static void POP_LEFT_CONNECT(MachineContext c, Opcode op)
	{
	    c.ostacktop--;
	    if(c.left != c.failureResult) {
	        ParsingObject left = c.ostack[c.ostacktop];
	        if(c.left != left) {
	            link(left, c.left);
	            c.left = left;
	        }
	    }
	}

	
	public final static void TMATCH(MachineContext c, Opcode op)
	{
		if(c.source.match(c.pos, op.bdata)) {
			c.pos += op.bdata.length;
		}
		else {
			c.left = failure(c);
		}
	}
	
	public final static void AMATCH(MachineContext c, Opcode op)
	{
		if(c.source.consume(c.pos, op.bdata)) {
			c.pos ++;
		}
		else {
			c.left = failure(c);
		}
	}
	
	public final static void UMATCH(MachineContext c, Opcode op)
	{
		if(c.pos < c.source.length()) {
			c.pos++;
		}
		else {
			c.left = failure(c);
		}
	}
	
	public final static void NEW(MachineContext c, Opcode op)
	{
		ParsingModel model = new ParsingModel();
		ParsingTag emptyTag = model.get("#empty");
		c.left = new ParsingObject(emptyTag, c.source, c.left.getLength() << 16);
		c.left.setLength((int) (c.pos - c.lstack[c.lstacktop - 1]));
	}
	
	public final static void TAG(MachineContext c, Opcode op)
	{
		String tagName;
		try {
			tagName = new String(op.bdata, "UTF-8");
			ParsingTag tag = new ParsingTag(tagName);
			c.left.setTag(tag.tagging());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
		
	public ParsingObject run(MachineContext c, int pc, Opcode[] code) {
		Opcode op = code[pc];
		while(true) {
			op = code[pc];
			System.out.println(op);
			switch(op.opcode) {
			case EXIT:
				return c.left;
			case JUMP:
				pc = op.ndata;
				break;
			case CALL:	
				c.lstack[c.lstacktop] = pc;
				c.lstacktop++;
				pc = op.ndata;
				break;
			case RET:
			    c.lstacktop--;
			    pc = (int)c.lstack[c.lstacktop];
				break;
			case IFSUCC:
				if(c.left != c.failureResult) {
					pc = op.ndata;
				}
				break;
			case IFFAIL:
				if(c.left == c.failureResult) {
					pc = op.ndata;
				}
				break;
			case PUSH_POS:
				PUSH_POS(c);
				break;
			case POP_POS:
				POP_POS(c);
				break;
			case POP_POS_BACK:
				POP_POS_BACK(c);
				break;
			case PUSH_BUFPOS:
				PUSH_BUFPOS(c);
				break;
			case POP_BUFPOS:
				POP_BUFPOS(c);
				break;
			case POP_BUFPOS_BACK:
				POP_BUFPOS_BACK(c);
				break;
			case PUSH_FPOS:
				PUSH_FPOS(c);
				break;
			case POP_FPOS:
				POP_FPOS(c);
				break;
			case POP_FPOS_FORGET:
				POP_FPOS_FORGET(c);
				break;
			case PUSH_LEFT:
				PUSH_LEFT(c);
				break;
			case POP_LEFT:
				POP_LEFT(c);
				break;
			case POP_LEFT_IFFAIL:
				POP_LEFT_IFFAIL(c);
				break;
			case POP_LEFT_NOT:
				POP_LEFT_NOT(c);
				break;
			case POP_LEFT_CONNECT:
				POP_LEFT_CONNECT(c, op);
				break;
			case TMATCH:
				TMATCH(c, op);
				break;
			case UMATCH:
				UMATCH(c, op);
				break;
			case AMATCH:
				AMATCH(c, op);
				break;
			case NEW:
				NEW(c, op);
				break;
			case TAG:
				TAG(c, op);
				break;
			default:
				throw new RuntimeException("unimplemented opcode: " + op);
			}	
			pc = pc + 1;
		}
	}
	
}







