package org.peg4d.pegcode;

import java.util.ArrayList;
import java.util.List;

public class Function {
	Module module;
	String funcName;
	List<BasicBlock> bbList;
	public Function(Module m, String funcName) {
		this.module = m;
		this.module.append(this);
		this.funcName = funcName;
		this.bbList = new ArrayList<BasicBlock>(); 
	}
	
	public BasicBlock get(int index) {
		return this.bbList.get(index);
	}
	
	public Function append(BasicBlock bb) {
		this.bbList.add(bb);
		return this;
	}
	
	public int size() {
		return this.bbList.size();
	}
	
	public int indexOf(BasicBlock bb) {
		return this.bbList.indexOf(bb);
	}
	
	public void stringfy(StringBuilder sb) {
		sb.append(this.funcName + ":\n");
		for(int i = 0; i < this.size(); i++) {
			sb.append("bb" + i + " {\n");
			this.get(i).stringfy(sb);
			sb.append("}\n");
		}
		sb.append("\n");
	}
}