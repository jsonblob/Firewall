import org.deuce.Atomic;

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