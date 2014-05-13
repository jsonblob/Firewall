import org.deuce.Atomic;
import java.util.HashMap;

@SuppressWarnings("unchecked")
class SerialAccessControl {
	private final int total;
	private Boolean[] PNG;
	public RangeSkipList[] R;

	public SerialAccessControl(int numAddressesLog) {
		total = (1 << numAddressesLog);
		PNG = new Boolean[total];
		R = new RangeSkipList[total];

		for (int i = 0; i < total; i++) {
			PNG[i] = true;
			for (int j = 0; j < total; j++) {
				R[i] = new RangeSkipList();
			}
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
		return PNG[address];
	}

	public boolean getAcceptPerm(int address, int checkAdd) {
		return !R[address].contains(Integer.valueOf(checkAdd));
	}

	public void printSendPerms() {
		System.out.println("--- PNG ---");
		for (int i = 0; i < total; i++) {
			System.out.println("PNG " + i + ": " + PNG[i]);
		}
	}
}


@SuppressWarnings("unchecked")
class ParallelSTMAccessControl {
	private final int total;
	private Boolean[] PNG;
	private HashMap[] R;

	public ParallelSTMAccessControl(int numAddressesLog) {
		total = (1 << numAddressesLog);
		PNG = new Boolean[total];
		R = new HashMap[total];

		for (int i = 0; i < total; i++) {
			PNG[i] = true;
			for (int j = 0; j < total; j++) {
				R[i] = new HashMap<Integer, Boolean>();
			}
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

class ParallelAccessControl {
	private final int total;
	private Boolean[] PNG;
	public RangeSkipList[] R;
	private MultiLock multiLock;

	public ParallelAccessControl(int numAddressesLog) {
		total = (1 << numAddressesLog);
		PNG = new Boolean[total];
		R = new RangeSkipList[total];

		for (int i = 0; i < total; i++) {
			PNG[i] = true;
			for (int j = 0; j < total; j++) {
				R[i] = new RangeSkipList();
			}
		}
		multiLock = new MultiLock(total);
	}

	public void setSendPerm(int address, boolean perm) {
		multiLock.acquireWrite(address);
		PNG[address] = perm;
		multiLock.releaseWrite(address);
	}

	public void setAcceptPerm(int address, int addressBegin, int addressEnd, boolean perm) {
		multiLock.acquireWrite(address);
		if (!perm)
			R[address].put(addressBegin, addressEnd);
		else
			R[address].remove(addressBegin, addressEnd);
		multiLock.releaseWrite(address);
	}

	public boolean getSendPerm(int address) {
		multiLock.acquireRead(address);
		boolean res = PNG[address];
		multiLock.releaseRead(address);
		return res;
	}

	public boolean getAcceptPerm(int address, int checkAdd) {
		multiLock.acquireRead(address);
		boolean res = !R[address].contains(Integer.valueOf(checkAdd));
		multiLock.releaseRead(address);
		return res;
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