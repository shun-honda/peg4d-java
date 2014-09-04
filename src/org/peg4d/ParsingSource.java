package org.peg4d;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ParsingSource {
	public final static int EOF = 256; 
	Grammar   peg;
	private String    fileName;
	protected long    startLineNum = 1;
	Stat      stat = null;
	
	public ParsingSource(Grammar peg, String fileName, long linenum) {
		this.peg = peg;
		this.fileName = fileName;
		this.startLineNum = linenum;
	}

	public abstract int     byteAt(long pos);
	public abstract long    length();

	public int charAt(long pos) {
		int c = byteAt(pos);
		int len = ParsingCharset.lengthOfUtf8(c);
		switch(len) {
		case 0:
			return -1;
		case 1:
			return c;
		case 2:
			throw new RuntimeException("TODO");
			// TODO;
		}
		return 0;
	}

	public int charLength(long pos) {
		int c = byteAt(pos);
		return ParsingCharset.lengthOfUtf8(c);
	}

	
	
	public abstract boolean match(long pos, byte[] text);
	public abstract boolean consume(long pos, byte[] charset);
	public abstract String  substring(long startIndex, long endIndex);
	public abstract long    linenum(long pos);
	
	public final String getResourceName() {
		return fileName;
	}

//	public long skipNot(long pos, long end, ParsingCharset cset) {
//		while(pos < end) {
//			int ch = byteAt(pos);
//			if(cset.match(ch)) {
//				return pos;
//			}
//		}
//		return this.length();
//	}
	
	
	
	public final long getLineStartPosition(long fromPostion) {
		long startIndex = fromPostion;
		if(!(startIndex < this.length())) {
			startIndex = this.length() - 1;
		}
		if(startIndex < 0) {
			startIndex = 0;
		}
		while(startIndex > 0) {
			int ch = byteAt(startIndex);
			if(ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}

	public final String getIndentText(long fromPosition) {
		long startPosition = this.getLineStartPosition(fromPosition);
		long i = startPosition;
		for(; i < fromPosition; i++) {
			int ch = this.byteAt(i);
			if(ch != ' ' && ch != '\t') {
				break;
			}
		}
		return this.substring(startPosition, i);
	}

	public final String getLineTextAt(long pos) {
		long startIndex = this.getLineStartPosition(pos);
		long endIndex = startIndex;
		while(endIndex < this.length()) {
			int ch = byteAt(endIndex);
			if(ch == '\n') {
				break;
			}
			endIndex = endIndex + 1;
		}
		return this.substring(startIndex, endIndex);
	}

	public final String getMarker(long pos) {
		long startIndex = this.getLineStartPosition(pos);
		String markerLine = "";
		long i = startIndex;
		while(i < pos) {
			int ch = byteAt(i);
			if(ch == '\n') {
				break;
			}
			if(ch == '\t') {
				markerLine = markerLine + "\t";
			}
			else {
				markerLine = markerLine + " ";
			}
			i = i + 1;
		}
		return markerLine + "^";
	}

	public final String formatErrorHeader(String error, long pos, String message) {
		return "(" + this.getResourceName() + ":" + this.linenum(pos) + ") [" + error +"] " + message;
	}

	public final String formatErrorMessage(String errorType, long pos, String msg) {
		String line = this.getLineTextAt(pos);
		String delim = "\n\t";
		if(line.startsWith("\t") || line.startsWith(" ")) {
			delim = "\n";
		}
		String header = this.formatErrorHeader(errorType, pos, msg);
		String marker = this.getMarker(pos);
		msg = header + delim + line + delim + marker;
		return msg;
	}

	public final String getFilePath(String fileName) {
		int loc = this.getResourceName().lastIndexOf("/");
		if(loc > 0) {
			return this.getResourceName().substring(0, loc+1) + fileName; 
		}
		return fileName;
	}

}

class StringSource extends ParsingSource {
	private byte[] utf8;
	private boolean[] asciiBitMap = new boolean[ParsingSource.EOF+1];
	long textLength;
	
	StringSource(Grammar peg, String sourceText) {
		super(peg, "(string)", 1);
		this.utf8 = ParsingCharset.toUtf8(sourceText);
		this.textLength = utf8.length;
	}
	
	StringSource(Grammar peg, String fileName, long linenum, String sourceText) {
		super(peg, fileName, linenum);
		this.utf8 = ParsingCharset.toUtf8(sourceText);
		this.textLength = utf8.length;
	}
	
	@Override
	public final long length() {
		return this.textLength;
	}

	@Override
	public final int byteAt(long pos) {
		if(pos < this.textLength) {
			return this.utf8[(int)pos] & 0xff;
		}
		return ParsingSource.EOF;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		if(pos + text.length > this.textLength) {
			return false;
		}
		for(int i = 0; i < text.length; i++) {
			if(text[i] != this.utf8[(int)pos + i]) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public final boolean consume(long pos, byte[] charset) {
		try {
			String string = new String(charset, "UTF-8");
			//System.out.println();
		} catch (UnsupportedEncodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		if(pos + 1 > this.textLength) {
			return false;
		}
		byte c1, c2;
		int i = 1;
		while(i < charset.length - 1) {
			c1 = charset[i];
			i++;
			if(c1 == 92) {
				do {
					c1 = this.getMetaByte(charset[i]);
					i++;
				}
				while(c1 == 0);
			}
			if(charset[i] == 45) {
				i++;
				c2 = charset[i];
				i++;
				if(c2 == 92) {
					do {
						c2 = this.getMetaByte(charset[i]);
						i++;
					}
					while(c2 == 0);
				}
				for(int c = c1; c <= c2; c++) {
					asciiBitMap[c] = true;
				}
			}
			else {
				asciiBitMap[c1] = true;
			}
		}
		if(!asciiBitMap[this.utf8[(int) pos]]) {
			return false;
		}
		return true;
	}
	
	private final byte getMetaByte(byte c) {
		switch(c) {
		case 116:
			return 9;
		case 110:
			return 10;
		case 114:
			return 13;
		}
		return 0;
	}

	@Override
	public final String substring(long startIndex, long endIndex) {
		try {
			return new String(this.utf8, (int)(startIndex), (int)(endIndex - startIndex), ParsingCharset.DefaultEncoding);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	@Override
	public final long linenum(long pos) {
		long count = this.startLineNum;
		for(int i = 0; i <= (int)pos; i++) {
			if(this.utf8[i] == '\n') {
				count++;
			}
		}
		return count;
	}
}

class FileSource extends ParsingSource {
	public final static int PageSize = 4096;
	
	private RandomAccessFile file;
	private long fileLength = 0;
	private long buffer_offset;
	private byte[] buffer;
	private long lines[];
	
	private final int FifoSize = 8; 
	private LinkedHashMap<Long, byte[]> fifoMap = null;
	
	FileSource(Grammar peg, String fileName) throws IOException {
		super(peg, fileName, 1);
		this.file = new RandomAccessFile(fileName, "r");
		this.fileLength = this.file.length();
		this.buffer_offset = 0;
		lines = new long[((int)this.fileLength / PageSize) + 1];
		lines[0] = 1;
		if(this.FifoSize > 0) {
			this.fifoMap = new LinkedHashMap<Long, byte[]>(FifoSize) {  //FIFO
				private static final long serialVersionUID = 6725894996600788028L;
				@Override
				protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest)  {
					if(this.size() > FifoSize) {
						return true;			
					}
					return false;
				}
			};
			this.buffer = null;
		}
		else {
			this.fifoMap = null;
			this.buffer = new byte[PageSize];
		}
		this.readMainBuffer(this.buffer_offset);
		
	}
	@Override
	public final long length() {
		return this.fileLength;
	}

	private long buffer_alignment(long pos) {
		return (pos / PageSize) * PageSize;
	}

	@Override
	public final int byteAt(long pos) {
		if(pos < this.fileLength) {
			return byteAt_(pos);
		}
		return ParsingSource.EOF;
	}

	public final int byteAt_(long pos) {
		int buffer_pos = (int)(pos - this.buffer_offset);
		if(!(buffer_pos >= 0 && buffer_pos < PageSize)) {
			this.buffer_offset = buffer_alignment(pos);
			this.readMainBuffer(this.buffer_offset);
			buffer_pos = (int)(pos - this.buffer_offset);
		}
		return this.buffer[buffer_pos] & 0xff;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		int offset = (int)(pos - this.buffer_offset);
		if(offset >= 0 && offset + text.length <= PageSize) {
			switch(text.length) {
			case 0:
				break;
			case 1:
				if(text[0] == this.buffer[offset]) {
					return true;
				}
				return false;
			case 2:
				if(text[0] == this.buffer[offset] && text[1] == this.buffer[offset+1]) {
					return true;
				}
				return false;
			case 3:
				if(text[0] == this.buffer[offset] && text[1] == this.buffer[offset+1] && text[2] == this.buffer[offset+2]) {
					return true;
				}
				return false;
			case 4:
				if(text[0] == this.buffer[offset] && text[1] == this.buffer[offset+1] && text[2] == this.buffer[offset+2] && text[3] == this.buffer[offset+3]) {
					return true;
				}
				return false;
			default:
				for(int i = 0; i < text.length; i++) {
					if(text[i] != this.buffer[offset+i]) {
						return false;
					}
				}
			}
			return true;
		}
		for(int i = 0; i < text.length; i++) {
			if((text[i] & 0xff) != this.byteAt(pos + i)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public final boolean consume(long pos, byte[] charset) {
		return false;
	}
		
	@Override
	public final String substring(long startIndex, long endIndex) {
		if(endIndex > startIndex) {
			try {
				long off_s = buffer_alignment(startIndex);
				long off_e = buffer_alignment(endIndex);
				if(off_s == off_e) {
					if(this.buffer_offset != off_s) {
						this.buffer_offset = off_s;
						this.readMainBuffer(this.buffer_offset);
					}
					return new String(this.buffer, (int)(startIndex - this.buffer_offset), (int)(endIndex - startIndex), ParsingCharset.DefaultEncoding);
				}
				else {
					byte[] b = new byte[(int)(endIndex - startIndex)];
					this.readStringBuffer(startIndex, b);
					return new String(b, ParsingCharset.DefaultEncoding);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	
	private int lineIndex(long pos) {
		return (int)(pos / this.PageSize);
	}

	private long startLineNum(long pos) {
		int index = lineIndex(pos);
		return this.lines[index];
	}
	
	@Override
	public final long linenum(long pos) {
		long count = startLineNum(pos);
		byteAt(pos); // restore buffer at pos
		int offset = (int)(pos - this.buffer_offset);
		for(int i = 0; i < offset; i++) {
			if(this.buffer[i] == '\n') {
				count++;
			}
		}
		return count;
	}
	
	private void readMainBuffer(long pos) {
		int index = lineIndex(pos);
		if(this.lines[index] == 0) {
			long count = this.lines[index-1];
			for(int i = 0; i < this.buffer.length; i++) {
				if(this.buffer[i] == '\n') {
					count++;
				}
			}
			this.lines[index] = count;
		}
		if(this.fifoMap != null) {
			Long key = pos;
			byte[] buf = this.fifoMap.get(key);
			if(buf == null) {
				buf = new byte[PageSize];
				this.readBuffer(pos, buf);
				this.fifoMap.put(key, buf);
				this.buffer = buf;
			}
			else {
				this.buffer = buf;
			}
		}
		else {
			this.readBuffer(pos, this.buffer);
		}
	}
	
	private void readBuffer(long pos, byte[] b) {
		try {
			this.file.seek(pos);
			int readsize = this.file.read(b);
			for(int i = readsize; i < b.length; i++) {
				b[i] = 0;
			}
			if(this.stat != null) {
				stat.readFile(b.length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readStringBuffer(long pos, byte[] buf) {
		if(this.fifoMap != null) {
			int copied = 0;
			long start = pos;
			long end = pos + buf.length;
			while(start < end) {
				long offset = this.buffer_alignment(start);
				if(this.buffer_offset != offset) {
					this.buffer_offset = offset;
					this.readMainBuffer(offset);
				}
				int start_off = (int)(start - offset);
				int end_off = (int)(end - offset);				
				if(end_off <= PageSize) {
					int len = end_off - start_off;
					System.arraycopy(this.buffer, start_off, buf, copied, len);
					copied += len;
					assert(copied == buf.length);
					return;
				}
				else {
					int len = PageSize - start_off;
					System.arraycopy(this.buffer, start_off, buf, copied, len);
					copied += len;
					start += len;
				}
			}
		}
		else {
			this.readBuffer(pos, buf);
		}
	}
	
//	public final String substringDebug(long startIndex, long endIndex) {
//		String s= this.substring(startIndex, endIndex);
//		if(this.debug != null) {
//			String s2= this.debug.substring(startIndex, endIndex);
//			if(!s.equals(s2)) {
//				System.out.println("s1: " + s);
//				System.out.println("s2: " + s2);
//				Main._Exit(1, "different " + this.fileName + " pos=" + startIndex + " end=" + endIndex);
//			}
//		}
//		return s;
//	}
//	
//	public final int charAtDebug(long n) {
//		//assert(n < this.fileLength);
//		int c = this.charAt(n);
//		if(this.debug != null) {
//			int c2 = this.debug.charAt(n);
//			if(c != c2) {
//				Main._Exit(1, "different " + this.fileName + " pos=" + n + "c='"+c+"', c2='"+c2+"'");
//			}
//		}
//		return c;
//	}

}

class InputStreamSource extends ParsingSource {
	public InputStreamSource(Grammar peg, String fileName, InputStream inStream) {
		super(peg, fileName, 1);
	}

	@Override
	public long length() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int byteAt(long n) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean match(long pos, byte[] text) {
		return false;
	}
	
	@Override
	public boolean consume(long pos, byte[] charset) {
		return false;
	}

	@Override
	public String substring(long startIndex, long endIndex) {
		return null;
	}

	@Override
	public long linenum(long pos) {
		// TODO Auto-generated method stub
		return 0;
	}

}
