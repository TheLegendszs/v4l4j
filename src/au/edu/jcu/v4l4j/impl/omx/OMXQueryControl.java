package au.edu.jcu.v4l4j.impl.omx;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import au.edu.jcu.v4l4j.api.control.CompositeControl;
import au.edu.jcu.v4l4j.api.control.Control;
import au.edu.jcu.v4l4j.api.control.ControlType;
import au.edu.jcu.v4l4j.impl.jni.MemoryUtils;
import au.edu.jcu.v4l4j.impl.jni.StructMap;
import au.edu.jcu.v4l4j.impl.jni.StructPrototype;

/**
 * A root composite control that when push or pull is called, executes a 
 * @author mailmindlin
 */
public class OMXQueryControl implements CompositeControl {
	protected final OMXComponent component;
	protected final Set<Control<?>> children = new HashSet<>();
	protected transient Map<String, Control<?>> childMap;
	protected final String rootName;
	protected final int queryId;
	protected final StructPrototype struct;
	
	protected OMXQueryControl(OMXComponent component, String rootName, int queryId, StructPrototype struct) {
		this.component = component;
		this.rootName = rootName;
		this.queryId = queryId;
		this.struct = struct;
		
	}
	
	@Override
	public String getName() {
		return this.rootName;
	}

	@Override
	public Set<Control<?>> getChildren() {
		return this.children;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Control<?> getChildByName(String name) {
		if (childMap == null) {
			for (Control<?> child : this.children)
				addToMap("", child);
		}
		
		return childMap.get(name);
	}
	
	protected void addToMap(String prefix, Control<?> child) {
		String name = prefix + (prefix.isEmpty() ? "" : ".") + child.getName();
		this.childMap.put(name, child);
		if (child.getType() == ControlType.COMPOSITE)
			for (Control<?> c : ((CompositeControl)child).getChildren())
				addToMap(name, c);
	}

	@Override
	public CompositeControlGetter<Map<String, Object>, Map<String, Object>> get() {
		return new OMXQueryControlGetter<>(null, null, null);
	}

	@Override
	public CompositeControlAccessor<Map<String, Object>, Void> access() {
		return new OMXQueryControlAccessor<>(null, null);
	}
	
	public class OMXQueryControlAccessor<R> implements CompositeControlAccessor<Map<String, Object>, R> {
		protected final OMXQueryControlAccessor<?> parent;
		protected final Duration timeout;
		
		protected OMXQueryControlAccessor(OMXQueryControlAccessor<?> parent, Duration timeout) {
			this.parent = parent;
			this.timeout = timeout;
		}
		
		@Override
		public OMXQueryControlAccessor<R> setTimeout(Duration timeout) {
			//We can pass our parent ref to the child because we have the same state
			return new OMXQueryControlAccessor<>(doGetChildParent(), timeout);
		}

		@Override
		public OMXQueryControlGetter<Map<String, Object>> get() {
			return new OMXQueryControlGetter<>(doGetChildParent(), this.timeout, state->OMXQueryControl.this.component.accessConfig(false, true, OMXQueryControl.this.queryId, state.valueMap.getBuffer()));
		}
		
		/**
		 * Get the parent reference that should be used for child objects
		 * generated by this object.
		 * @return
		 */
		protected OMXQueryControlAccessor<?> doGetChildParent() {
			return this.parent;
		}
		
		/**
		 * Method that actually does stuff when invoked.
		 * @return Pointer to native memory, or 0 if none is allocated
		 * @throws Exception
		 */
		protected R doCall(OMXQueryControlAccessorState state) throws Exception {
			if (this.parent == null)
				return null;
			return (R) this.parent.doCall(state);
		}

		@Override
		public R call() throws Exception {
			R result;
			try (OMXQueryControlAccessorState state = new OMXQueryControlAccessorState()) {
				result = doCall(state);
			}
			return result;
		}
		
	}
	
	public class OMXQueryControlGetter<R> extends OMXQueryControlAccessor<R> implements CompositeControlGetter<Map<String, Object>, R> {
		protected final Consumer<OMXQueryControlAccessorState> reader;
		
		protected OMXQueryControlGetter(OMXQueryControlAccessor<?> parent, Duration timeout, Consumer<OMXQueryControlAccessorState> reader) {
			super(parent, timeout);
			this.reader = reader;
		}
		
		@Override
		public OMXQueryControlGetter<R> setTimeout(Duration timeout) {
			return new OMXQueryControlGetter<>(doGetChildParent(), timeout, null);
		}
		
		@Override
		protected OMXQueryControlAccessor<?> doGetChildParent() {
			if (this.reader == null)
				return this.parent;
			return this;
		}

		@Override
		public OMXQueryControlGetter<R> read(Consumer<Map<String, Object>> handler) {
			return new OMXQueryControlGetter<R>(doGetChildParent(), timeout, state->handler.accept(state.valueMap));
		}

		@Override
		public <E> OMXQueryControlGetter<R> read(String name, Consumer<E> handler) {
			return read(map->handler.accept((E)map.get(name)));
		}

		@Override
		public OMXQueryControlUpdater<R> write(Map<String, Object> value) {
			return new OMXQueryControlUpdater<>(doGetChildParent(), timeout, state->state.valueMap.putAll(value));
		}

		@Override
		public OMXQueryControlUpdater<R> write(Supplier<Map<String, Object>> supplier) {
			return new OMXQueryControlUpdater<>(doGetChildParent(), timeout, state->state.valueMap.putAll(supplier.get()));
		}

		@Override
		public <E> OMXQueryControlUpdater<R> write(String name, Supplier<E> supplier) {
			return new OMXQueryControlUpdater<>(doGetChildParent(), timeout, state->state.valueMap.put(name, supplier.get()));
		}

		@Override
		public <E> OMXQueryControlUpdater<R> write(String name, E value) {
			return new OMXQueryControlUpdater<>(doGetChildParent(), timeout, state->state.valueMap.put(name, value));
		}

		@Override
		public OMXQueryControlUpdater<R> update(Function<Map<String, Object>, Map<String, Object>> mappingFunction) {
			//I'm thinking some kind of copy-mark changed-merge scheme, if I ever get around to it...
			throw new UnsupportedOperationException("This one is hard to implement");
		}

		@Override
		@SuppressWarnings("unchecked")
		public <E extends Object> OMXQueryControlUpdater<R> update(String name, BiFunction<String, E, E> mappingFunction) {
			return new OMXQueryControlUpdater<>(doGetChildParent(), timeout, state->state.valueMap.compute(name, (BiFunction<String, Object, Object>)mappingFunction));
		}
		
	}
	
	public class OMXQueryControlUpdater<R> extends OMXQueryControlGetter<R> implements CompositeControlUpdater<Map<String, Object>, R> {

		protected OMXQueryControlUpdater(OMXQueryControlAccessor<?> parent, Duration timeout, Consumer<OMXQueryControlAccessorState> handler) {
			super(parent, timeout, handler);
		}

		@Override
		public OMXQueryControlUpdater<R> setTimeout(Duration timeout) {
			return new OMXQueryControlUpdater<>(doGetChildParent(), timeout, null);
		}

		@Override
		public OMXQueryControlAccessor<R> set() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public OMXQueryControlGetter<R> setAndGet() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	protected static class OMXQueryControlAccessorState implements AutoCloseable {
		StructMap valueMap;
		Set<ByteBuffer> unmanagedRefs = new HashSet<>();
		
		@Override
		public void close() throws Exception {
			valueMap.close();
		}
	}
}
