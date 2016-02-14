package com.jefferygriffith;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/*
 * PI/4 = 1/1 - 1/3 + 1/5 - 1/7 + 1/9 - ...
 */
public class CalcPi {
	
	public static CompletableFuture<Double> calcPiForTerms(long start, long numElements) {
		
		CompletableFuture<Double> f = future();
		
		go(() -> {
			double acc  = 0;
			
			for (long i = start; i < start + numElements; i++)
				acc += 4.0 * (double)(1 - (i % 2) * 2) / (double)(2 * i + 1);
			
			f.complete(acc);
		});
		
	    return f;
	}
	
	public static CompletableFuture<Double> calculatePi(long numWorkers, long elementsPerWorker) {
		
		List<CompletableFuture<Double>> futures = list();
		
		for (int i = 0; i < numWorkers; ++i) {
			CompletableFuture<Double> f = calcPiForTerms(i * elementsPerWorker, elementsPerWorker);
			futures.add(f);
		}
		
		return allOf(futures).thenApply(CalcPi::sum);
	}

	public static void main(String[] args) throws InterruptedException {

		long totalElements = 10000000000L;
		long numWorkers = 8;
		long elementsPerWorker = totalElements / numWorkers;
		
		printf("Calculating PI with %d workers and %d elements per worker\n", numWorkers, elementsPerWorker);

		Timer t = new Timer();
		Double pi = calculatePi(numWorkers, elementsPerWorker).join();
		t.stop();
		
		printf("Elapsed time: %dms\n", t.elapsed());
		printf("Pi is %.10f\n", pi);
	}
	
	///////////////// General stuff to make Java purdy.
	
    public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futures) {
    	// Nurkiewitz.com
        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        return allDoneFuture.thenApply(v -> futures.stream().map(future -> future.join()).collect(Collectors.<T> toList()));
    }
    
	public static Double sum(List<Double> terms) {
		double accum = 0.0;
		for (Double t : terms)
			accum += t;
		return accum;
	}

    @SafeVarargs
    public static <T> List<T> list(T... entries) {
        return Arrays.stream(entries).collect(Collectors.toList());
    }
    
    public static <T> CompletableFuture<T> future() {
    	return new CompletableFuture<T>();
    }
    
    public static void go(Runnable task) {
        ForkJoinPool.commonPool().execute(task);
    }
    
    public static class Latch {
    	private CountDownLatch cdl = new CountDownLatch(1);
    	public void done() { cdl.countDown(); }
    	public void await() throws InterruptedException { cdl.await(); }
    }
    
    public static class Timer {
    	long startTime = System.currentTimeMillis();
    	long elapsed;
    	public void stop() { elapsed = System.currentTimeMillis() - startTime; }
    	public long elapsed() { return elapsed; }
    }
    
    public static void printf(String format, Object... args) {
    	System.out.print(format(format, args));
    }
}
