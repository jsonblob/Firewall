import org.deuce.Atomic;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class SerialFilter {
	private final int total;
	private int[] histogram;

	public SerialFilter() {
		total = 1 << 16;
		histogram = new int[total];
	}

	public void setBucket(int address) {
		histogram[address]++;
	}

	public int getBucket(int address) {
		return histogram[address];
	}

	public void printHistogram() {
		System.out.println("--- Histogram ---");
		for (int i = 0; i < total; i++) {
			System.out.println("Bucket " + i + ": " + histogram[i]);
		}
	}
}


class ParallelSTMFilter {
	private final int total;
	private int[] histogram;

	public ParallelSTMFilter() {
		total = 1 << 16;
		histogram = new int[total];
	}

	@Atomic
	public void setBucket(int address) {
		histogram[address]++;
	}

	@Atomic
	public int getBucket(int address) {
		return histogram[address];
	}

	public void printHistogram() {
		System.out.println("--- Histogram ---");
		for (int i = 0; i < total; i++) {
			System.out.println("Bucket " + i + ": " + histogram[i]);
		}
	}
}

class ParallelFilter {
	private final int total;
	private int[] histogram;
	private ReentrantReadWriteLock[] locks;

	public ParallelFilter() {
		total = 1 << 16;
		histogram = new int[total];
		locks = new ReentrantReadWriteLock[total];
		for (int i = 0; i < total; i++) {
			locks[i] = new ReentrantReadWriteLock();
		}
	}

	public final void acquireRead(int address) {
		locks[address].readLock().lock();
	}

	public final void releaseRead(int address) {
		locks[address].readLock().unlock();
	}

	public final void acquireWrite(int address) {
		locks[address].writeLock().lock();
	}

	public final void releaseWrite(int address) {
		locks[address].writeLock().unlock();
	}

	public void setBucket(int address) {
		acquireWrite(address);
		histogram[address]++;
		releaseWrite(address);
	}

	public int getBucket(int address) {
		acquireRead(address);
		int val = histogram[address];
		releaseRead(address);
		return val;
	}

	public void printHistogram() {
		System.out.println("--- Histogram ---");
		for (int i = 0; i < total; i++) {
			System.out.println("Bucket " + i + ": " + histogram[i]);
		}
	}
}