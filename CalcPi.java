package com.jefferygriffith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.DoubleAdder;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.LongStream.rangeClosed;


/*
 * PI/4 = 1/1 - 1/3 + 1/5 - 1/7 + 1/9 - ...
 */
public class CalcPi
{
    public static CompletableFuture<Double> calcPiForTerms(long start, long numElements)
    {
        return supplyAsync(() -> {
            double acc = 0;

            for (long i = start; i < start + numElements; i++) {
                acc += 4.0 * (double) (1 - (i % 2) * 2) / (double) (2 * i + 1);
            }

            return acc;
        });
    }


    public static double calculatePi(long numWorkers, long elementsPerWorker)
    {
        var sum = new DoubleAdder();
        allOf(rangeClosed(0, numWorkers - 1).boxed()
                      .map(i -> calcPiForTerms(i * elementsPerWorker, elementsPerWorker)
                              .thenAccept(d -> sum.add(d)))
                      .toArray(CompletableFuture[]::new))
                .join();
        return sum.doubleValue();
    }


    public static void main(String[] args) throws InterruptedException
    {
        long totalElements = 10000000000L;
        long numWorkers = 8;
        long elementsPerWorker = totalElements / numWorkers;

        System.out.printf("Calculating PI with %d workers and %d elements per worker\n", numWorkers, elementsPerWorker);

        long startTime = System.currentTimeMillis();
        double pi = calculatePi(numWorkers, elementsPerWorker);
        long elapsed = System.currentTimeMillis() - startTime;

        System.out.printf("Elapsed time: %dms\n", elapsed);
        System.out.printf("Pi is %.10f\n", pi);
    }
}
