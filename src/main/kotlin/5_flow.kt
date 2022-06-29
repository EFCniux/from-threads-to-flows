import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

fun flow1() {
    println(
        "Thus far we have mostly written the code on an imperative and sequential way, " +
                "\n but flows work slightly different, instead of defining the code that will run as written, " +
                "\n we define the path of the data that the code generates, " +
                "\n if and only if the flow is consumed/observed."
    )
    println(
        "Flows are chains of transformations that define the series of instructions to generate a given data, " +
                "\n but they themselves do not contain nay data, only the instructions " +
                "\n to generate it (with some exceptions)"
    )
    println("In a way, yon can think of flows as suspendable sequences")

    Thread.sleep(1_000)
    println("\nTraditional imperative code:\n")

    run {
        fun createNumbers() = buildList {
            for (number in 1..20) {
                println("${Thread.currentThread().name}, generating number $number")
                Thread.sleep(100)
                add(number)
            }
        }

        fun removePairs(
            numbers: List<Int>
        ) = buildList {
            for (number in numbers) {
                println("${Thread.currentThread().name}, filtering number $number")
                Thread.sleep(100)
                if (number % 2 != 0)
                    add(number)
            }
        }

        fun sum(
            numbers: List<Int>
        ): Int {
            var sum = 0
            for (number in numbers) {
                println("${Thread.currentThread().name}, reducing number $number")
                Thread.sleep(100)
                sum += number
            }
            return sum
        }

        val numbers = createNumbers()
        println(numbers)

        val numbers2 = removePairs(numbers)
        println(numbers2)

        val sum = sum(numbers2)
        println(sum)
    }

    Thread.sleep(1_000)
    println("\nAs a sequence:\n")

    run {
        val sum = sequence {
            for (number in 1..20) {
                println("${Thread.currentThread().name}, generating number $number")
                Thread.sleep(100)
                yield(number)
            }
        }.filter {
            println("${Thread.currentThread().name}, filtering number $it")
            Thread.sleep(100)
            it % 2 != 0
        }.reduce { accumulator, value ->
            println("${Thread.currentThread().name}, reducing number $value")
            Thread.sleep(100)
            accumulator + value
        }

        println(sum)
    }

    Thread.sleep(1_000)
    println("\nAs a flow:\n")

    runBlocking(Dispatchers.IO) {
        val sum = flow {
            for (number in 1..20) {
                println("${Thread.currentThread().name}, generating number $number")
                delay(100)
                emit(number)
            }
        }.filter {
            println("${Thread.currentThread().name}, filtering number $it")
            delay(100)
            it % 2 != 0
        }.reduce { accumulator, value ->
            println("${Thread.currentThread().name}, reducing number $value")
            delay(100)
            accumulator + value
        }

        println(sum)
    }
}

fun flow2() {
    println(
        "Flows are specially useful to simplify complex calculations as data sequences, " +
                "\n allowing composing multiple flows to generate the desired result " +
                "\n without necessarily knowing the implementation details of each flow"
    )

    // Imagine this recovered information from a dog api
    fun dogFlow() = flowOf(
        "Affenpinscher",
        "Afghan Hound",
        "Africanis",
        "Aidi",
        "Airedale Terrier",
        "Akbash Dog",
        "Akita"
    ).onEach {
        println("${Thread.currentThread().name}, generating dog $it")
        delay(Random.nextLong(1_000, 2_000))
    }.flowOn(Dispatchers.IO)
    // Dispatchers.IO shares threads with Dispatchers.Default but is designed for blocking IO calls

    // Imagine this recovered information from a cat api
    fun catFlow() = flowOf(
        "Abyssinian cat",
        "Aegean cat",
        "American Bobtail",
        "American Curl",
        "American Shorthair",
        "American Wirehair",
        "Arabian Mau"
    ).onEach {
        println("${Thread.currentThread().name}, generating cat $it")
        delay(Random.nextLong(1_000, 2_000))
    }.flowOn(Dispatchers.IO)

    // Imagine this recovered information from a shotgun?¿?¿?¿ api
    fun shotgunFlow() = flowOf(
        "Akdal MKA 1919",
        "Armsel Striker",
        "Atchisson Assault Shotgun",
        "Baikal MP-153",
        "Bandayevsky RB-12",
        "Benelli M1",
        "Benelli M3"
    ).onEach {
        println("${Thread.currentThread().name}, generating shotgun $it")
        delay(Random.nextLong(1_000, 2_000))
    }.flowOn(Dispatchers.IO)

    // Wikipedia sure does love lists. Don't judge.

    val dogVsCatFlow = dogFlow().zip(catFlow()) { dog, cat ->
        println("${Thread.currentThread().name}, zipping $dog vs $cat")
        dog to cat
    }.zip(shotgunFlow()) { dogVsCat, shotgun ->
        val (dog, cat) = dogVsCat
        println("${Thread.currentThread().name}, zipping $dog vs $cat and $shotgun")

        Random
            .nextBoolean()
            .let { isDogOrCatUsingShotgun ->
                isDogOrCatUsingShotgun to (if (isDogOrCatUsingShotgun)
                    "$dog used $shotgun against $cat!"
                else
                    "$cat used $shotgun against $dog!")
            }
    }

    runBlocking(Dispatchers.Default) {
        var dogWinsNum = 0
        var catWinsNum = 0

        dogVsCatFlow
            .collect {
                val (isDogOrCatVictory, message) = it

                if (isDogOrCatVictory)
                    dogWinsNum += 1
                else
                    catWinsNum += 1

                println("${Thread.currentThread().name}, $message")
            }

        when {
            dogWinsNum > catWinsNum ->
                println("${Thread.currentThread().name}, The dogs won! (${dogWinsNum - catWinsNum} survivors)")
            catWinsNum > dogWinsNum ->
                println("${Thread.currentThread().name}, The cats won! (${catWinsNum - dogWinsNum} survivors)")
            else ->
                println("${Thread.currentThread().name}, War, war never changes")
        }
    }
}

fun flow3() {
    println("As hinted before, some flows are actually capable of containing data without being collected.")
    println(
        "This behavior is called a \"hot\" flow, given that on creation they already know what they are going to emit, " +
                "\n and normal flows are \"cold\" (lazy) flows, given that they need to be \"heated\" (collected) " +
                "\n in order to know what to produce whenever they have to emit."
    )

    println("This for example is a hot flow")
    val value = Unit
    val hotFlow = flowOf(value)

    runBlocking(Dispatchers.Default) {
        hotFlow.collect { println("Received value") }
    }

    println("This is a cold flow")
    val coldFlow = flow { emit(Unit) }

    runBlocking(Dispatchers.Default) {
        coldFlow.collect { println("Received value") }
    }

    println(
        "Both behave equally when collected, but its important to distinguish between them " +
                "\n as they could significantly change the expected data if it is time-sensitive"
    )
}
