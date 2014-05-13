import java.util.concurrent.locks.ReentrantReadWriteLock;

class MultiLock {
	private ReentrantReadWriteLock[] locks;
	public MultiLock(int cap) {
		locks = new ReentrantReadWriteLock[cap];
		for (int i = 0; i < cap; i++) {
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
}