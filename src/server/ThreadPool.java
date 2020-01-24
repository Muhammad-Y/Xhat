package server;

import java.util.LinkedList;

public class ThreadPool {
	private Buffer<Runnable> buffer = new Buffer<Runnable>();
	private LinkedList<Worker> workers;
	private int nbrOfThreads;
	
	public ThreadPool(int nbrOfThreads) {
		this.nbrOfThreads = nbrOfThreads;
	}
	
	public synchronized void start() {
		Worker worker;
		if(workers==null) {
			workers = new LinkedList<Worker>();	
			for(int i=0; i<nbrOfThreads; i++) {
				worker = new Worker();
				worker.start();
				workers.add(worker);
			}
		}
	}
	
	public synchronized void execute(Runnable runnable) {
		buffer.put(runnable);
	}
	
	public synchronized void stop() {
		if (workers != null) {
			for (int i = 0; i < nbrOfThreads; i++) {
				execute(new StopWorker());
			}
			workers.clear();
			workers = null;
		}
	}

	private class StopWorker implements Runnable {
		public void run() {
			Thread.currentThread().interrupt();
		}

		public String toString() {
			return Thread.currentThread() + " StopWorker";
		}
	}
	
	private class Worker extends Thread {
		public void run() {
			while(!Thread.interrupted()) {
				try {
				    buffer.get().run();
				} catch(InterruptedException e) {
					System.out.println(e); // Utskrift av java.lang.InterruptedException
					break;
				}
			}
		}
	}
	
//	public static void main(String[] args) {
//		ThreadPool pool = new ThreadPool(20);
//		pool.start();
//		for(int i=0; i<100; i++) {
//			pool.execute(new Task(i));
//		}
//		pool.stop();
//	}
}