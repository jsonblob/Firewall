import org.deuce.Atomic;
import java.util.HashMap;

@SuppressWarnings("unchecked")
class SerialAccessControl {
	private final int total;
	private Boolean[] PNG;
	private RangeSkipList[] R;

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
		if (perm)
			R[address].put(addressBegin, addressEnd);
		else
			R[address].remove(addressBegin, addressEnd);
	}

	public boolean getSendPerm(int address) {
		return PNG[address];
	}

	public boolean getAcceptPerm(int address, int checkAdd) {
		return R[address].contains(Integer.valueOf(checkAdd));
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