fun thread1() {
    println("This is a thread.")

    println("This thread is named: ${Thread.currentThread().name}")
}

fun thread2() {
    println("This is also a thread.")

    val thread = Thread()

    println("This thread is named: ${thread.name}")
}

fun thread3() {
    println("Code always runs on a thread, and all code on it is executed sequentially")

    val runnable = Runnable {
        for (num in 0..10) {
            println("$num")
            Thread.sleep(1_000) // Pretend to do work for 1 second
        }
    }

    val thread = Thread(runnable)

    thread.start()
}

fun thread4() {
    println("However, code on different threads, can run concurrently, although still sequentially inside of its onw thread")

    println("${Thread.currentThread().name}: Start")

    val thread1 = Thread {
        println("${Thread.currentThread().name}: Start")
        for (num in 0..10) {
            println("${Thread.currentThread().name}: $num")
            Thread.sleep(1_000)
        }
        println("${Thread.currentThread().name}: Stop")
    }

    val thread2 = Thread {
        println("${Thread.currentThread().name}: Start")
        for (num in 0..10) {
            println("${Thread.currentThread().name}: $num")
            Thread.sleep(500)
        }
        println("${Thread.currentThread().name}: Stop")
    }

    thread1.start()
    thread2.start()

    println("${Thread.currentThread().name}: Stop")
}
