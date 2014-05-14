import org.deuce.Atomic;
import java.util.HashMap;

public interface AccessControl {
  public void setSendPerm(int address, boolean perm);
  public void setAcceptPerm(int address, int addressBegin, int addressEnd, boolean perm);
  public boolean getSendPerm(int address);
  public boolean getAcceptPerm(int address, int checkAdd);
} 

@SuppressWarnings("unchecked")
class SerialAccessControl implements AccessControl {
	private final int total;
	private Boolean[] PNG;
	public RangeSkipList[] R;

	public SerialAccessControl(int numAddressesLog) {
		total = (1 << numAddressesLog);
		PNG = new Boolean[total];
		R = new RangeSkipList[total];

		for (int i = 0; i < total; i++) {
			PNG[i] = true;
			R[i] = new RangeSkipList();
		}
	}

	public void setSendPerm(int address, boolean perm) {
		PNG[address] = perm;
	}

	public void setAcceptPerm(int address, int addressBegin, int addressEnd, boolean perm) {
		if (!perm)
			R[address].put(addressBegin, addressEnd);
		else
			R[address].remove(addressBegin, addressEnd);
	}

	public boolean getSendPerm(int address) {
		boolean val = PNG[address];
		return val;
	}

	public boolean getAcceptPerm(int address, int checkAdd) {
		boolean val = false;
		val = !R[address].contains(checkAdd);
		return val;
	}

	public void printSendPerms() {
		System.out.println("--- PNG ---");
		for (int i = 0; i < total; i++) {
			System.out.println("PNG " + i + ": " + PNG[i]);
		}
	}
}


@SuppressWarnings("unchecked")
class ParallelSTMAccessControl implements AccessControl {
	private final int total;
	private Boolean[] PNG;
	private HashMap[] R;

	public ParallelSTMAccessControl(int numAddressesLog) {
		total = (1 << numAddressesLog);
		PNG = new Boolean[total];
		R = new HashMap[total];

		for (int i = 0; i < total; i++) {
			PNG[i] = true;
			R[i] = new HashMap<Integer, Boolean>();
		}
	}

	@Atomic
	public void setSendPerm(int address, boolean perm) {
		PNG[address] = perm;
	}

	@Atomic
	public void setAcceptPerm(int address, int addressBegin, int addressEnd, boolean perm) {
		for (int i = addressBegin; i < addressEnd; i++) {
			if (!perm)
				R[address].put(i, null);
			else
				R[address].remove(i);
		}
	}

	@Atomic
	public boolean getSendPerm(int address) {
		return PNG[address];
	}

	@Atomic
	public boolean getAcceptPerm(int address, int checkAdd) {
		return !R[address].containsKey(checkAdd);
	}

	public void printSendPerms() {
		System.out.println("--- PNG ---");
		for (int i = 0; i < total; i++) {
			System.out.println("PNG " + i + ": " + PNG[i]);
		}
	}
}

class ParallelAccessControl implements AccessControl {
	private final int total;
	private Boolean[] PNG;
	private Cache cache;
	public RangeSkipList[] R;
	private MultiLock multiLock;

	public ParallelAccessControl(int numAddressesLog) {
		total = (1 << numAddressesLog);
		PNG = new Boolean[total];
		R = new RangeSkipList[total];
		cache = new Cache(numAddressesLog);

		for (int i = 0; i < total; i++) {
			PNG[i] = true;
			R[i] = new RangeSkipList();
		}
		multiLock = new MultiLock(total);
	}

	public void setSendPerm(int address, boolean perm) {
		multiLock.acquireWrite(address);
		PNG[address] = perm;
		multiLock.releaseWrite(address);
	}

	public void setAcceptPermInit(int address, int addressBegin, int addressEnd, boolean perm) {
		multiLock.acquireWrite(address);
		if (!perm)
			R[address].put(addressBegin, addressEnd);
		else
			R[address].remove(addressBegin, addressEnd);
		multiLock.releaseWrite(address);
	}

	public void setAcceptPerm(int address, int addressBegin, int addressEnd, boolean perm) {
		multiLock.acquireWrite(address);
		if (!perm)
			R[address].put(addressBegin, addressEnd);
		else
			R[address].remove(addressBegin, addressEnd);

		for (int i = addressBegin; i < addressEnd; i++) {
			cache.remove(address + "-" + i);
		}
		multiLock.releaseWrite(address);
	}

	public boolean getSendPerm(int address) {
		multiLock.acquireRead(address);
		boolean res = PNG[address];
		multiLock.releaseRead(address);
		return res;
	}

	public boolean getAcceptPerm(int address, int checkAdd) {
		String key = address + "-" + checkAdd;
		Boolean val = cache.get(key);
		if (val == null) {
			multiLock.acquireRead(address);
			val = !R[address].contains(Integer.valueOf(checkAdd));
			cache.put(key, val);
			multiLock.releaseRead(address);
		}
		return val;
	}

	public boolean isAllowed(Header header) {
		boolean res = !getSendPerm(header.source) && getAcceptPerm(header.dest, header.source);
		return res;
	}

	public void printSendPerms() {
		System.out.println("--- PNG ---");
		for (int i = 0; i < total; i++) {
			System.out.println("PNG " + i + ": " + PNG[i]);
		}
	}
}