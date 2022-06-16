package framework.thread.kotlin

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * https://www.kotlincn.net/docs/reference/coroutines/basics.html
 * https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-global-scope/index.html
 *
 * 觀念建立：Java Daemon Thread -> Kotlin GlobalScope 原始型態（依照背景子執行緒的壽命限制）
 * https://openhome.cc/Gossip/JavaGossip-V2/DaemonThread.htm
 *
 * 測試 kotlin 語言層級實現協程，此為 JDK 19 之前的折衷方法，
 * 因 kt 協程效率仍高於傳統 thread 很多所以採用
 *
 * --- required jar file
 * kotlin-stdlib-1.6.21.jar
 * https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib/1.6.21
 *
 * kotlinx-coroutines-core-jvm-1.6.2.jar
 * https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.6.2
 */
open class VirtualThread {

    /**
     * Kotlin Coroutines - 協程
     * 需使用 GlobalScope 才符合高效率協程，而不是 blocking 狀態
     * 但需要注意原生技術為 Java Daemon Thread，所以高耗時的運算過程要確保主執行緒的存活
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun execute( runnable: Runnable ) = GlobalScope.launch {
        runnable.run()
    }

}
