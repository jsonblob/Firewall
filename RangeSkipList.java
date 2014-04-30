import java.util.concurrent.ConcurrentSkipListMap;

class RangeSkipList {
	public ConcurrentSkipListMap<Integer, Integer> list = new ConcurrentSkipListMap<Integer, Integer>();
	public RangeSkipList() {
		list.put(Integer.MIN_VALUE, Integer.MIN_VALUE);
		list.put(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	public void put(int addressBegin, int addressEnd) {
		int k = findLow(addressBegin);
		int val = findHigh(addressEnd);
		removeBetween(k, val);
		list.put(k, val);
	}

	private int findLow(int addressBegin) {
		int lowK = list.floorKey(addressBegin);
		int lowVal = list.get(lowK);
		if (addressBegin <= lowVal)
			return lowK;
		return addressBegin;
	}

	private int findHigh(int addressEnd) {
        int lowK = list.floorKey(addressEnd);
        int lowVal = list.get(lowK);
        if (addressEnd < lowVal)
            return lowVal;
		int highK = list.ceilingKey(addressEnd);
		int highVal = list.get(highK);
		if (addressEnd <= highK)
			return addressEnd;
		return highVal;
	}

	private void removeBetween(int addressBegin, int addressEnd) {
		int k = list.ceilingKey(addressBegin);
		while (k >= addressBegin && k < addressEnd) {
            if (k == list.get(k)) {
                list.remove(k);
                k++;
            }
			int tmp = list.ceilingKey(list.get(k));
            list.remove(k);
			k = tmp;
		}
	}

	public void remove(int addressBegin, int addressEnd) {
        int k = findLow(addressBegin);
        int val = findHigh(addressEnd);
        removeBetween(k, val);
        if (k != addressBegin)
            list.put(k, addressBegin);
        if (addressEnd != val)
            list.put(addressEnd, val);
	}

	public boolean contains(int address) {
        int k = list.floorKey(address);
        while (address >= k) {
            if (address < list.get(k))
                return true;
            k = list.higherKey(list.get(k));
        }
        return false;
	}
}

class RangeSkipListTest {
  public static void main(String[] args) {  
    RangeSkipList list = new RangeSkipList();
    list.put(1, 2);
    System.out.println(list.list);
    list.put(3, 3);
    System.out.println(list.list);
    list.put(4, 5);
    System.out.println(list.list);
    list.put(1, 5);
    System.out.println(list.list);
    list.put(2, 7);
    System.out.println(list.list);
    list.remove(1, 5);
    System.out.println(list.list);
    list.put(1, 4);
    System.out.println(list.list);
    System.out.println(list.contains(0));
    System.out.println(list.contains(1));
    System.out.println(list.contains(2));
    System.out.println(list.contains(4));
    System.out.println(list.contains(5));
    System.out.println(list.contains(6));
    System.out.println(list.contains(7));
    System.out.println(list.contains(8));
  }
}