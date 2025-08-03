package io.chandler.morajai.tools;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSNumber;

public class JSSet implements Set<Integer> {

	@JSBody(params = { }, script = "return new Set();")
	private static native JSObject newSet();

	@JSBody(params = { "set", "value" }, script = "return set.add(value);")
	private static native boolean add(JSObject set, JSNumber value);

	@JSBody(params = { "set", "value" }, script = "return set.has(value);")
	private static native boolean has(JSObject set, JSNumber value);

	JSObject set;

	public JSSet() {
		set = newSet();
	}

	@Override public boolean add(Integer arg0) { return add(set, JSNumber.valueOf(arg0)); }
	@Override public boolean contains(Object o) { return has(set, JSNumber.valueOf((Integer) o)); }

	@Override public boolean addAll(Collection<? extends Integer> c) { throw new UnsupportedOperationException("Unimplemented method 'addAll'"); }
	@Override public void clear() { throw new UnsupportedOperationException("Unimplemented method 'clear'"); }
	@Override public boolean containsAll(Collection<?> c) { throw new UnsupportedOperationException("Unimplemented method 'containsAll'"); }
	@Override public boolean isEmpty() { throw new UnsupportedOperationException("Unimplemented method 'isEmpty'"); }
	@Override public Iterator<Integer> iterator() { throw new UnsupportedOperationException("Unimplemented method 'iterator'"); }
	@Override public boolean remove(Object o) { throw new UnsupportedOperationException("Unimplemented method 'remove'"); }
	@Override public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException("Unimplemented method 'removeAll'"); }
	@Override public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException("Unimplemented method 'retainAll'"); }
	@Override public int size() { throw new UnsupportedOperationException("Unimplemented method 'size'"); }
	@Override public Object[] toArray() { throw new UnsupportedOperationException("Unimplemented method 'toArray'"); }
	@Override public <T> T[] toArray(T[] arg0) { throw new UnsupportedOperationException("Unimplemented method 'toArray'"); }
}
