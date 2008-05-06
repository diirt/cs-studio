/*
 * Copyright (c) 2001-2007 OFFIS, Tammo Freese.
 * This program is made available under the terms of the MIT License.
 */
package org.easymock;

/**
 * A comparison function that is used to match arguments.
 * 
 * @see MockControl#setDefaultMatcher
 * @see MockControl#setMatcher
 * @see MockControl#EQUALS_MATCHER
 * @see MockControl#ARRAY_MATCHER
 * @see MockControl#ALWAYS_MATCHER
 * 
 * @deprecated Since EasyMock 2.0, <code>ArgumentsMatcher</code>s are only
 *             supported for the legacy <code>MockControl</code>. For mock
 *             objects generated by the methods on <code>EasyMock</code>,
 *             there are per-argument matchers available. For more information,
 *             see the EasyMock documentation.
 */
@Deprecated
public interface ArgumentsMatcher {

	/**
	 * Matches two arrays of arguments.
	 * 
	 * @param expected
	 *            the expected arguments.
	 * @param actual
	 *            the actual arguments.
	 * @return true if the arguments match, false otherwise.
	 */
	boolean matches(Object[] expected, Object[] actual);

	/**
	 * Returns a string representation of the arguments.
	 * 
	 * @param arguments
	 *            the arguments to be used in the string representation.
	 * @return a string representation of the arguments.
	 */
	String toString(Object[] arguments);
}
