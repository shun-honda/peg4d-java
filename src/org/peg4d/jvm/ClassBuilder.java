package org.peg4d.jvm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * wrapper class of ClassWriter
 * @author skgchxngsxyz-osx
 *
 */
public class ClassBuilder extends ClassWriter implements Opcodes {
	private final String internalName;

	/**
	 * equivalent to ClassBuilder(ACC_PUBLIC | ACC_FINAL, fullyQualifiedClassName, sourceName, superClass, interfaces)
	 * @param fullyQualifiedClassName
	 * @param sourceName
	 * @param superClass
	 * @param interfaces
	 */
	public ClassBuilder(String fullyQualifiedClassName, 
			String sourceName, Class<?> superClass, Class<?>[] interfaces) {
		this(ACC_PUBLIC | ACC_FINAL, fullyQualifiedClassName, sourceName, superClass, interfaces);
	}

	/**
	 * generate new class builder
	 * @param accessFlag
	 * represent for java access flag (public, private, static ... )
	 * @param fullyQualifiedClassName
	 * ex. org/peg4d/generated/Parser
	 * @param sourceName
	 * source file name, may be null
	 * @param superClass
	 * if null, super class is java/lang/Object
	 * @param interfaces
	 * may be null, if has no interface
	 */
	public ClassBuilder(int accessFlag, String fullyQualifiedClassName, 
			String sourceName, Class<?> superClass, Class<?>[] interfaces) {
		super(ClassWriter.COMPUTE_FRAMES);
		this.internalName = fullyQualifiedClassName;
		String[] interfaceNames = null;

		if(superClass == null) {
			superClass = Object.class;
		}
		if(interfaces != null) {
			final int size = interfaces.length;
			interfaceNames = new String[size];
			for(int i = 0; i < size; i++) {
				interfaceNames[i] = Type.getInternalName(interfaces[i]);
			}
		}
		this.visit(V1_7, accessFlag, this.internalName, null, Type.getInternalName(superClass), interfaceNames);
		this.visitSource(sourceName, null);
	}

	/**
	 * get fully qualified class name. for UserDefinedClassLoader#definedAndLoadClass()
	 * @return
	 */
	public String getInternalName() {
		return this.internalName;
	}

	@Override
	public String toString() {
		return this.getInternalName();
	}
}
