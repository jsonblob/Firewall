
class AccessControl {
	private final int total;
	private Boolean[] PNG;
	private Boolean[][] R;

	public AccessControl(int numAddressesLog) {
		total = (1 << numAddressesLog);
		PNG = new Boolean[total];
		R = new Boolean[total][total];

		for (int i = 0; i < total; i++) {
			PNG[i] = true;
			for (int j = 0; j < total; j++) {
				R[i][j] = false;
			}
		}
	}

	public void setSendPerm(int address, boolean perm) {
		PNG[address] = perm;
	}

	public void setAcceptPerm(int address, int addressBegin, int addressEnd, boolean perm) {
		for (int i = addressBegin; i < addressEnd; i++) {
			R[address][i] = perm;
		}
	}

	public boolean getSendPerm(int address) {
		return PNG[address];
	}

	public boolean getAcceptPerm(int address, int checkAdd) {
		return R[address][checkAdd];
	}

	public void printSendPerms() {
		System.out.println("--- PNG ---");
		for (int i = 0; i < total; i++) {
			System.out.println("PNG " + i + ": " + PNG[i]);
		}
	}
}