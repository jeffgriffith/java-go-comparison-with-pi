/*
 * Compare to my Scala version using the actor model
 * https://prezi.com/4asyld78xkm6/actor-systems-with-akka/
 *
 * PI/4 = 1/1 - 1/3 + 1/5 - 1/7 + 1/9 - ...
 */
package main
import "fmt"
import "time"

func calcPiForTerms(start int, numElements int, out chan float64) {
    accum := 0.0

    for i := start; i < start + numElements; i++ {
        accum += 4.0 * float64(1 - (i % 2) * 2) / float64(2 * i + 1)
    }

    out <- accum
}

func calculatePi(numWorkers int, elementsPerWorker int) chan float64 {
    result := make(chan float64) // Single async result

    go func() {
        out := make(chan float64)

        for i := 0; i < numWorkers; i++ {
            go calcPiForTerms(i * elementsPerWorker, elementsPerWorker, out)
        }
        result <- sum(out, numWorkers)
        close(out)
        close(result)
    }()

    return result
}

func main() {

    totalElements := 10000000000
    numWorkers := 8
    elementsPerWorker := totalElements / numWorkers

    fmt.Printf("Calculating PI with %d workers and %d elements per worker\n", numWorkers, elementsPerWorker);

    start := time.Now()

    pi := <-calculatePi(numWorkers, elementsPerWorker)

    fmt.Printf("Elapsed time: %s\n", time.Since(start))
    fmt.Printf("Pi is %.10f\n", pi)
}

////////////////// Generic utilities

func sum(ch chan float64, numWorkers int) float64 {
    sum := 0.0
    for i := 0; i < numWorkers; i++ {
        sum += <-ch
    }
    return sum
}
