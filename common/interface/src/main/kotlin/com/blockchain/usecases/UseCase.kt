package com.blockchain.usecases

abstract class UseCase<in P, R> {
    open operator fun invoke(parameter: P): R {
        return execute(parameter)
    }

    protected abstract fun execute(parameter: P): R
}