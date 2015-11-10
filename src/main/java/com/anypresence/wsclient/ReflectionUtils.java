package com.anypresence.wsclient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * ReflectionUtils contains utility functions related to reflection.=
 * 
 * @author Rick Snyder
 */
public class ReflectionUtils {

	/**
	 * Finds all methods on a class that are annotated with a specific annotation, and must satisfy the provided predicate 
	 * 
	 * @param c The class on which to search for methods
	 * @param annotationClass The annotation for which we will search on each method of the class
	 * @param predicate A predicate that the annotation declaration must satisfy
	 * @return The methods that satisfy the provided predicate.  If none are found, an empty array is returned.
	 */
	public static <T extends Annotation> Method[] findMethodsAnnotatedWith(Class<?> c, Class<T> annotationClass, BiPredicate<Method, T> predicate) {
		Method[] methods = c.getMethods();
		
		List<Method> matches = new ArrayList<Method>();
		for (Method m : methods) {
			T t = m.getAnnotation(annotationClass);
			if (t != null && predicate.test(m, t)) {
				matches.add(m);
			}
		}
		
		return matches.toArray(new Method[matches.size()]);
	}
	
	/**
	 * Finds a single method on a class that are annotated with a specific annotation, which must satisfy the provided predicate
	 * 
	 * @param c The class on which to search for methods
	 * @param annotationClass The annotation for which we will search on each method of the class
	 * @param predicate A predicate that the annotation declaration must satisfy
	 * @return If no methods are found satisfying the criteria, null is returned.  If a single method is found, it is returned.  Otherwise,
	 * if more than one method is found that satisfy these conditions, a <code>ReflectionException</code> is thrown.
	 */
	public static <T extends Annotation> Method findMethodAnnotatedWith(Class<?> c, Class<T> annotationClass, BiPredicate<Method, T> predicate) {
		Method[] methods = findMethodsAnnotatedWith(c, annotationClass, predicate);
		if (methods.length == 0) {
			return null;
		} else if (methods.length == 1) {
			return methods[0];
		} else {
			throw new ReflectionException("Found more than one method matching the specified predicate!");
		}
	}
	
	/**
	 * Finds an annotation on a class, provided that the predicate is satisfied
	 * 
	 * @param c The class on which to search for the annotation
	 * @param annotationClass The annotation to search for
	 * @param predicate A predicate that the given annotation declaration must satisfy, if present
	 * @return The annotation declaration that matches the criteria, or null if it doesn't exist
	 */
	public static <T extends Annotation> T findAnnotationOnClass(Class<?> c, Class<T> annotationClass, Predicate<T> predicate) {
		T t = c.getDeclaredAnnotation(annotationClass);
		if (t == null) {
			return null;
		}
		
		if (predicate.test(t)) {
			return t;
		}
		
		return null;
	}
	
	/**
	 * Returns the interface implemented by c, that is annotated with annotationClass
	 * 
	 * @param c The target class for which to find the interface
	 * @param annotationClass The annotation to search for
	 * @return The class representing the interface that is the subject of our search, or null if it doesn't exist
	 */
	public static <T extends Annotation> Class<?> findInterfaceWithAnnotation(Class<?> c, Class<T> annotationClass) {
		for (Class<?> intFace : c.getInterfaces()) {
			T t = intFace.getAnnotation(annotationClass);
			if (t != null) {
				return intFace;
			}
		}
		
		return null;
	}
}
