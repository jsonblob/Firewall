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
	private MultiLock multiLock;

	public ParallelFilter() {
		total = 1 << 16;
		histogram = new int[total];
		multiLock = new MultiLock(total);
	}

	public void setBucket(int address) {
		multiLock.acquireWrite(address);
		histogram[address]++;
		multiLock.releaseWrite(address);
	}

	public int getBucket(int address) {
		multiLock.acquireRead(address);
		int val = histogram[address];
		multiLock.releaseRead(address);
		return val;
	}

	public void printHistogram() {
		System.out.println("--- Histogram ---");
		for (int i = 0; i < total; i++) {
			System.out.println("Bucket " + i + ": " + histogram[i]);
		}
	}
}