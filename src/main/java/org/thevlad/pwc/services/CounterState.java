package org.thevlad.pwc.services;

public class CounterState {

	private int count = 0;;
	private boolean finished = false;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

}
