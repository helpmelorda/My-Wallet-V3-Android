package com.blockchain.testutils

import com.nhaarman.mockitokotlin2.mock
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

fun TestRule.runRule() {
    apply(mock(), mock()).evaluate()
}

fun TestRule.runRule(statement: () -> Unit) {
    apply(object : Statement() {
        override fun evaluate() {
            statement()
        }
    }, mock()).evaluate()
}
