import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun mutable1() {
    println("A thread can read variables on another thread without issues")

    var number = 1

    val thread = Thread {
        println("${Thread.currentThread().name}: The number is $number")

        Thread.sleep(550)

        println("${Thread.currentThread().name}: The number is $number")

        Thread.sleep(1_500)

        println("${Thread.currentThread().name}: The number is $number")
    }

    println("${Thread.currentThread().name}: The number is $number")
    thread.start()

    Thread.sleep(1_000)

    number += 1
    println("${Thread.currentThread().name}: The number is $number")

    Thread.sleep(1_000)

    number += 1
    println("${Thread.currentThread().name}: The number is $number")
}

fun mutable2() {
    println("A thread CANNOT write variables on another thread without issues")

    var number = 1

    val thread = Thread {
        println("${Thread.currentThread().name}: Expected 1, is $number")

        Thread.sleep(550)

        number += 1
        println("${Thread.currentThread().name}: Expected 2, is $number")

        Thread.sleep(1_500)

        number += 1
        println("${Thread.currentThread().name}: Expected 3, is $number")
    }

    println("${Thread.currentThread().name}: Expected 1, is $number")
    thread.start()

    Thread.sleep(1_000)

    number += 1
    println("${Thread.currentThread().name}: Expected 2, is $number")

    Thread.sleep(1_000)

    number += 1
    println("${Thread.currentThread().name}: Expected 3, is $number")
}

fun mutable3() {
    println(
        "If a thread expects a value to have not changed, but another thread has modified it," +
                "\n crashes can happen, or worse, undefined behaviour"
    )

    val mutList = (1..10).toMutableList()
    val thread = Thread {
        var index = 0
        val iterator = mutList.iterator()
        while (iterator.hasNext()) {
            val number = iterator.next()

            println("${Thread.currentThread().name}: Expected ${index + 1}, is $number")

            iterator.remove()
            index += 1

            println("${Thread.currentThread().name}: Removed $number")

            Thread.sleep(1_000)
        }
    }

    thread.start()

    var index = 0
    try {
        for (number in mutList) {
            println("${Thread.currentThread().name}: Expected ${index + 1}, is $number")
            Thread.sleep(1_000)
            index += 1
        }
    } catch (e: ConcurrentModificationException) {
        println("${Thread.currentThread().name}: Expected ${index + 1}, crashed because: $e")
    }
}

fun mutable4() {
    println(
        "Special constructs exist to allow multiple threads to read/write the same value, " +
                "\nlike locks, atomic values, or synchronized code"
    )

    println(
        "However, most of the solutions share the same problem, they fix the issue by forcing " +
                "\n code running in different threads to run sequentially, or in other words, " +
                "\n like if they were on the same thread"
    )

    val lock = ReentrantLock()

    val mutList = (1..10).toMutableList()
    val thread = Thread {
        var index = 0
        val iterator = mutList.iterator()

        var isLockAcquired = false
        try {
            while (!isLockAcquired) {
                isLockAcquired = lock.tryLock()

                println("${Thread.currentThread().name}: Is waiting: ${!isLockAcquired}")

                if (!isLockAcquired)
                    Thread.sleep(1_000)
            }

            println("${Thread.currentThread().name}: Loop Start")

            while (iterator.hasNext()) {
                val number = iterator.next()

                println("${Thread.currentThread().name}: Expected ${index + 1}, is $number")

                iterator.remove()
                index += 1

                println("${Thread.currentThread().name}: Removed $number")

                Thread.sleep(1_000)
            }

            println("${Thread.currentThread().name}: Loop End")
        } finally {
            if (isLockAcquired)
                lock.unlock()
        }
    }

    thread.start()

    var index = 0

    lock.withLock {
        println("${Thread.currentThread().name}: Loop Start")

        for (number in mutList) {
            println("${Thread.currentThread().name}: Expected ${index + 1}, is $number")
            Thread.sleep(1_000)
            index += 1
        }

        println("${Thread.currentThread().name}: Loop End")
    }
}

fun mutable5() {
    println(
        "Another good practice to avoid issues is to never mutate a given value, " +
                "\n but instead make a copy of it, mutate the copy, and then reassign it, " +
                "\n only needing to lock when the value is copied or reassigned, " +
                "\n since the copy does not affect the original value."
    )

    println(
        "That said, while this would be faster, it would consume more memory, " +
                "\n and the threads would only use the value as it was when they read it, " +
                "\n unable to know if someone else was changing it at that very moment"
    )

    val lock = ReentrantLock()

    var state = (1..10).toList()
    val thread = Thread {
        val mutList = lock.withLock {
            state.toMutableList() // copy it as a mutable list
        }

        var index = 0
        val iterator = mutList.iterator()
        while (iterator.hasNext()) {
            val number = iterator.next()

            println("${Thread.currentThread().name}: Expected ${index + 1}, is $number")

            iterator.remove()
            index += 1

            println("${Thread.currentThread().name}: Removed $number")

            Thread.sleep(1_000)
        }

        lock.withLock {
            state = mutList.toList() // reassign our mutable list as an immutable list
        }
    }

    thread.start()

    val list = lock.withLock {
        state.toList() // copy it as an immutable list
    }

    var index = 0
    for (number in list) {
        println("${Thread.currentThread().name}: Expected ${index + 1}, is $number")
        Thread.sleep(1_000)
        index += 1
    }
}
