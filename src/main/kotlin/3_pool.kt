import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import kotlin.random.Random

fun pool1() {
    println(
        "Threads are not free. A computer can only run a limited number of them concurrently, " +
                "\n plus depending on the implementation, creating threads can be costly."
    )

    println(
        "For example, this code/computer can run concurrently ${Runtime.getRuntime().availableProcessors()} threads, " +
                "\n any more than that, and some threads will have to wait until another one finishes before doing any work."
    )

    println(
        "Given this, to avoid creating a thread every time some work needs to be done, " +
                "\n we can recycle the previously created threads and make then do a new task."
    )

    val routineQueue = LinkedBlockingQueue<Runnable>()

    val routineRunner = Runnable {
        // Do forever
        while (true) {
            // Block the thread this runnable is on until the queue has a runnable, remove it, and run it
            routineQueue.take().run()
        }
    }

    val threadRoutineRunner1 = Thread(routineRunner)
    val threadRoutineRunner2 = Thread(routineRunner)

    threadRoutineRunner1.start()
    threadRoutineRunner2.start()

    val createRoutine: (Int) -> Runnable = { routineNumber ->
        Runnable {
            val pretendToWorkForMillis = Random.nextLong(500, 2_000)
            println("${Thread.currentThread().name}, routine-$routineNumber: Will work for $pretendToWorkForMillis millis")
            Thread.sleep(pretendToWorkForMillis)
            println("${Thread.currentThread().name}, routine-$routineNumber: Finished working for $pretendToWorkForMillis millis")
        }
    }

    for (number in 1..20) {
        val routine = createRoutine(number)
        routineQueue.put(routine)
    }
}

fun pool2() {
    println(
        "The previous technique can be simplified/improved upon by using thread pools, " +
                "\n but the underlying logic is the same"
    )

    val threadPool = Executors.newFixedThreadPool(10)

    val createRoutine: (Int) -> Runnable = { routineNumber ->
        Runnable {
            val pretendToWorkForMillis = Random.nextLong(500, 2_000)
            println("${Thread.currentThread().name}, routine-$routineNumber: Will work for $pretendToWorkForMillis millis")
            Thread.sleep(pretendToWorkForMillis)
            println("${Thread.currentThread().name}, routine-$routineNumber: Finished working for $pretendToWorkForMillis millis")
        }
    }

    for (number in 1..20) {
        val routine = createRoutine(number)
        threadPool.execute(routine)
    }
}
