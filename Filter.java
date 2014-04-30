
class Filter {
	private final int total;
	private int[] histogram;

	public Filter() {
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