import java.util.concurrent.atomic.*;
import java.util.concurrent.ConcurrentHashMap;

class Cache {
	private ConcurrentHashMap<String,AtomicMarkableReference<Boolean>> entries;
	private AtomicInteger invCount;
	private AtomicInteger count;
	private final int MAXINVCOUNT;
	private final int MAXCOUNT;

	public Cache(int log) {
		MAXINVCOUNT = 1<<(log/2);
		MAXCOUNT = 1<<log;
		invCount = new AtomicInteger(0);
		count = new AtomicInteger(0);
		entries = new ConcurrentHashMap<String,AtomicMarkableReference<Boolean>>();
	}

	public void put(String key, boolean val) {
		shouldPrune();
		entries.put(key, new AtomicMarkableReference<Boolean>(val, false));
		count.getAndIncrement();
	}

	public void remove(String key) {
		if (!entries.containsKey(key))
			return;
		Boolean val = entries.get(key).getReference();
		entries.get(key).attemptMark(val, true);
		invCount.getAndIncrement();
		shouldPrune();
	}

	public Boolean get(String key) {
		if (!entries.containsKey(key))
			return null;
		AtomicMarkableReference<Boolean> val = entries.get(key);
		if (val.isMarked())
			return val.getReference();
		return null;
	}

	public void shouldPrune() {
		if (count.get() > MAXCOUNT && false) {
			entries = new ConcurrentHashMap<String,AtomicMarkableReference<Boolean>>();
			count.set(0);
			invCount.set(0);
		}
	}
}