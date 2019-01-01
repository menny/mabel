package examples.kotlin.program

import com.github.salomonbrys.kotson.*
import com.google.gson.*

fun main(args: Array<String>) {

    val obj: JsonObject = jsonObject(
            "output" to "Hello world!"
    )

    System.out.println("$obj")
}
