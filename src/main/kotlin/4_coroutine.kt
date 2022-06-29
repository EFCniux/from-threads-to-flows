import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/** A super simplified fake coroutine implementation to give a vague idea of its inner workings */
abstract class FakeCoroutine<T> {
    sealed class Result<T> {
        data class Yield<T>(
            val at: Int
        ) : Result<T>()

        data class Complete<T>(
            val value: T
        ) : Result<T>()
    }

    protected fun yield(at: Int) = Result.Yield<T>(at)
    protected fun complete(value: T) = Result.Complete(value)

    abstract fun run(yielded: Result.Yield<T>? = null): Result<T>
}

fun coroutine1() {
    println(
        "A coroutine is a routine that cooperates for access to the thread pool its running on " +
                "\n by pausing its work and letting other routines do theirs"
    )

    val coroutineQueue = LinkedBlockingQueue<FakeCoroutine<Unit>>()

    val coroutineRunner = Runnable {
        val pendingCoroutines = LinkedHashMap<FakeCoroutine<Unit>, FakeCoroutine.Result.Yield<Unit>>()

        val readResult = { coroutine: FakeCoroutine<Unit>, result: FakeCoroutine.Result<Unit> ->
            when (result) {
                is FakeCoroutine.Result.Yield ->
                    pendingCoroutines[coroutine] = result
                is FakeCoroutine.Result.Complete ->
                    Unit
            }
        }

        // Do forever
        while (true) {
            when {
                // If there are coroutines to be started
                coroutineQueue.isNotEmpty() -> {
                    val coroutine = coroutineQueue.take()
                    val result = coroutine.run()
                    readResult(coroutine, result)
                }
                // If there are coroutines to be resumed
                pendingCoroutines.isNotEmpty() -> {
                    val coroutine = pendingCoroutines.keys.first()
                    val olrResult = pendingCoroutines.remove(coroutine)
                    val newResult = coroutine.run(olrResult)
                    readResult(coroutine, newResult)
                }
                else -> Unit
            }
        }
    }

    val threadCoroutineRunner = Thread(coroutineRunner)

    threadCoroutineRunner.start()

    val createCoroutine: (Int) -> FakeCoroutine<Unit> = { coroutineNumber ->
        object : FakeCoroutine<Unit>() {
            private val pretendToWorkForMillis = Random.nextLong(2_000, 4_000)

            override fun run(
                yielded: Result.Yield<Unit>?
            ): Result<Unit> {
                if (yielded == null)
                    println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Will work for $pretendToWorkForMillis millis")

                var workingOn = yielded?.at ?: 0

                while (true) {
                    workingOn += 1
                    Thread.sleep(pretendToWorkForMillis / 4)
                    println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Step $workingOn completed, worked for ${pretendToWorkForMillis / 4} millis")

                    return if (workingOn < 4)
                        yield(workingOn)
                    else {
                        println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Finished working for $pretendToWorkForMillis millis")
                        complete(Unit)
                    }
                }
            }
        }
    }

    for (number in 1..10) {
        val coroutine = createCoroutine(number)
        coroutineQueue.put(coroutine)
    }
}

fun coroutine2() {
    println(
        "Same implementation, but this time using proper coroutines and a single thread, " +
                "\n or in other words sequentially"
    )

    // This is a coroutine. All it needs is to be marked as suspend-able.
    suspend fun doWork(coroutineNumber: Int) {
        val pretendToWorkForMillis = Random.nextLong(2_000, 4_000)

        println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Will work for $pretendToWorkForMillis millis")

        var workingOn = 0

        while (true) {
            workingOn += 1
            // Unlike Thread.sleep(), delay does not block the current thread,
            // instead it pauses the coroutine and schedules it to be resumed after the given time.
            delay(pretendToWorkForMillis / 4)
            println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Step $workingOn completed, worked for ${pretendToWorkForMillis / 4} millis")

            if (workingOn < 4)
            // Similar to delay except without a time, so it will be resumed as soon as possible.
                yield()
            else {
                println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Finished working for $pretendToWorkForMillis millis")
                return
            }
        }
    }

    // The CoroutineDispatcher we are going to run the coroutines on.
    // It fits the same role as a thread pool executor would do.
    val threadPool: CoroutineDispatcher = Dispatchers.Default

    // runBlocking creates a coroutine context (coroutine scope to be more precise).
    // For more info on scope vs context see https://elizarov.medium.com/coroutine-context-and-scope-c8b255d59055
    //
    // A coroutine context defines the lifetime of its running coroutines,
    // and allows coroutines to be started inside it.
    //
    // Contexts can inherit form others in witch they themselves run on.
    //
    // This one in particular blocks the current thread until its coroutines are completed (successfully or not),
    // witch is useful for bridging non-blocking (suspending) code with traditional blocking code
    val results = runBlocking(threadPool) {
        // Execute a list of coroutines sequentially on the coroutineDispatcher and capture its results
        buildList {
            for (number in 1..10) {
                val result = doWork(number)
                add(result)
            }
        }
    }

    println("Finished all ${results.size} coroutines!")
}

fun coroutine3() {
    println(
        "Same implementation, but this time using multiple threads, " +
                "\n and as separate jobs, or in other words concurrently-ish"
    )

    suspend fun doWork(coroutineNumber: Int) {
        val pretendToWorkForMillis = Random.nextLong(2_000, 4_000)

        println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Will work for $pretendToWorkForMillis millis")

        var workingOn = 0

        while (true) {
            workingOn += 1
            delay(pretendToWorkForMillis / 4)
            println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Step $workingOn completed, worked for ${pretendToWorkForMillis / 4} millis")

            if (workingOn < 4)
                yield()
            else {
                println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Finished working for $pretendToWorkForMillis millis")
                return
            }
        }
    }

    val threadPool: CoroutineDispatcher = Dispatchers.Default

    runBlocking(threadPool) {
        // Execute a list of coroutines in different jobs.
        // Note that since they are concurrent we cannot capture their results
        println("Start coroutines in a separate job")
        val jobList = buildList {
            for (number in 1..10)
                add(launch { doWork(number) })
        }

        delay(1_000)

        // Wait for the list of executed jobs to complete
        println("Wait all coroutine jobs to complete")
        jobList
            .joinAll()
    }

    println("Finished all coroutines!")
    println("Please, do note how its much quicker because its running on multiple threads and not following a strict execution order!")
}

fun coroutine4() {
    println(
        "Same implementation, but this time using multiple threads, " +
                "\n and in the same job, or in other words in parallel-ish"
    )

    suspend fun doWork(coroutineNumber: Int) {
        val pretendToWorkForMillis = Random.nextLong(2_000, 4_000)

        println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Will work for $pretendToWorkForMillis millis")

        var workingOn = 0

        while (true) {
            workingOn += 1
            delay(pretendToWorkForMillis / 4)
            println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Step $workingOn completed, worked for ${pretendToWorkForMillis / 4} millis")

            if (workingOn < 4)
                yield()
            else {
                println("${Thread.currentThread().name}, coroutine-$coroutineNumber: Finished working for $pretendToWorkForMillis millis")
                return
            }
        }
    }

    val threadPool: CoroutineDispatcher = Dispatchers.Default

    val results = runBlocking(threadPool) {
        // Create (and possibly execute) a list of coroutines as part of the current job.
        println("Deferred (async) coroutines are executed as part of the same job that initiated them")
        val deferredList = buildList {
            for (number in 1..10)
                add(async(
                    // cold use different starting logic, like starting lazily
                    start = CoroutineStart.DEFAULT
                ) { doWork(number) })
        }

        delay(1_000)

        // Wait for the list of coroutines to return a completion value and capture their results
        println("Wait all deferred coroutines to complete")
        deferredList
            .awaitAll()
    }

    println("Finished all ${results.size} coroutines!")
}

fun coroutine5() {
    suspend fun explode(inSeconds: Int) {
        println("Will explode in $inSeconds second!")
        delay(inSeconds.seconds)
        throw RuntimeException("Something exploded!")
    }

    println("As long as coroutine code does not change jobs, it can be treated as if it is running sequentially")

    runBlocking(Dispatchers.Default) {
        try {
            explode(1)
        } catch (e: Throwable) {
            println("Successfully contained bomb: $e")
        }
    }
}

fun coroutine6() {
    suspend fun explode(inSeconds: Int) {
        println("Will explode in $inSeconds second!")
        delay(inSeconds.seconds)
        throw RuntimeException("Something exploded!")
    }

    suspend fun times() {
        for (number in 1..Int.MAX_VALUE) {
            println("Having a fun time! x$number")
            delay(100)
        }
    }

    println("However once it changes jobs, code outside the job cannot interact with code in the other job")
    println("Notice how also if a job throws an error, the parent job that owns the child job is also cancelled.")

    runBlocking(Dispatchers.Default) {
        launch {
            try {
                explode(1)
            } catch (e: Throwable) {
                println("Successfully contained bomb: $e")
            }
        }

        try {
            launch {
                explode(2)
            }
        } catch (e: Throwable) {
            // Will never be called
            println("Successfully contained bomb: $e")
        }

        times()
    }
}

fun coroutine7() {
    suspend fun explode(inSeconds: Int) {
        println("Will explode in $inSeconds second!")
        delay(inSeconds.seconds)
        throw RuntimeException("Something exploded!")
    }

    suspend fun times() {
        for (number in 1..20) {
            println("Having a fun time! x$number")
            delay(100)
        }
    }

    println("If the parent job is a SupervisorJob, it is capable of surviving a child's job failure")
    println(
        "This is important because sometimes, just because one of the child jobs fails, " +
                "\n all the others should not be cancelled, like for example in an android ViewModel " +
                "\n or any view component"
    )

    runBlocking(Dispatchers.Default) {
        println("\nWill survive, inside a supervisorJob")
        with(this + SupervisorJob()) {
            launch {
                explode(1)
            }

            times()
        }

        delay(1_000)

        println("\nWill survive, inside a supervisorScope")
        supervisorScope {
            launch {
                explode(1)
            }

            times()
        }
    }
}
