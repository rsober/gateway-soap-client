package com.anypresence.wsclient;

import java.util.Enumeration;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUtils {

	private static final String CLASS_EXT = ".class";
	
	public static String toClassName(String jarEntryName) {
		if (!jarEntryName.endsWith(CLASS_EXT)) {
			throw new IllegalArgumentException("The provided jar entry with name " + jarEntryName + " is not a valid java class file (does not end with '.class')");
		}
		jarEntryName = jarEntryName.replaceAll("/", ".");
		jarEntryName = jarEntryName.substring(0,  jarEntryName.length() - CLASS_EXT.length());
		return jarEntryName;
	}
	
	public static void loadClassesFromJar(ClassLoader loader, JarFile jar) {
		findClassInJar(loader, jar, null);
	}
	
	public static Class<?> findClassInJar(ClassLoader loader, JarFile jar, Function<Class<?>, Boolean> onLoad) {
		Enumeration<JarEntry> enumerator = jar.entries();
		while (enumerator.hasMoreElements()) {
			JarEntry entry = enumerator.nextElement();
			String entryName = entry.getName();
			if (!entryName.endsWith(CLASS_EXT)) {
				continue;
			}
			String className = toClassName(entryName);
			
			Class<?> clazzToLoad = null;
			try {
				clazzToLoad = loader.loadClass(className);
			} catch (ClassNotFoundException e) {
				// shouldn't happen
				throw new RuntimeException("Unable to load class for class name " + className + " due to ClassNotFoundException", e);
			}
			
			if (onLoad != null) {
				if (onLoad.apply(clazzToLoad)) {
					return clazzToLoad;
				}
			}
		}
		
		return null;
	}
	
}
