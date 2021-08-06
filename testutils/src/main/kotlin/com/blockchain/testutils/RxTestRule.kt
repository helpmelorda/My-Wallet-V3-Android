package com.blockchain.testutils

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers

fun rxInit(block: RxInit.() -> Unit) =
    before {
        RxInit().also(block)
    } after {
        RxJavaPlugins.reset()
    }

open class RxInit {

    fun computation(scheduler: Scheduler) {
        RxJavaPlugins.setComputationSchedulerHandler { scheduler }
    }

    fun computationTrampoline() {
        computation(Schedulers.trampoline())
    }

    fun io(scheduler: Scheduler) {
        RxJavaPlugins.setIoSchedulerHandler { scheduler }
    }

    fun ioTrampoline() {
        io(Schedulers.trampoline())
    }

    fun newThread(scheduler: Scheduler) {
        RxJavaPlugins.setNewThreadSchedulerHandler { scheduler }
    }

    fun newThreadTrampoline() {
        newThread(Schedulers.trampoline())
    }

    fun single(scheduler: Scheduler) {
        RxJavaPlugins.setSingleSchedulerHandler { scheduler }
    }

    fun singleTrampoline() {
        single(Schedulers.trampoline())
    }
}
