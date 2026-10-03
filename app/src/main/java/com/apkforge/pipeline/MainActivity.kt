package com.apkforge.pipeline

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.math.BigDecimal
import java.math.MathContext

class MainActivity : ComponentActivity() {
    private lateinit var display: TextView
    private lateinit var preview: TextView
    private var current = "0"
    private var expression = ""
    private var lastWasEquals = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        display = findViewById(R.id.display)
        preview = findViewById(R.id.preview)
        val ids = intArrayOf(
            R.id.btn_c, R.id.btn_sign, R.id.btn_pct, R.id.btn_div,
            R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_mul,
            R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_sub,
            R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_add,
            R.id.btn_0, R.id.btn_dot, R.id.btn_back, R.id.btn_eq
        )
        for (id in ids) {
            findViewById<Button>(id).setOnClickListener { onKey(it as Button) }
        }
        updateDisplay()
    }

    private fun onKey(btn: Button) {
        val label = btn.text.toString()
        when (label) {
            "C" -> {
                current = "0"
                expression = ""
                lastWasEquals = false
            }
            "+/-" -> {
                current = when {
                    current == "0" || current == "Error" -> current
                    current.startsWith("-") -> current.drop(1)
                    else -> "-" + current
                }
            }
            "%" -> current = applyPercent(current)
            "\u232b" -> {
                current = if (current.length > 1 && current != "Error") current.dropLast(1) else "0"
            }
            "=" -> {
                val full = if (expression.isNotEmpty()) expression + current else current
                current = evaluate(full)
                expression = ""
                lastWasEquals = true
            }
            "+", "-", "\u00d7", "\u00f7" -> {
                if (current == "Error") return
                expression = if (lastWasEquals) {
                    lastWasEquals = false
                    current
                } else if (expression.isEmpty()) {
                    current
                } else {
                    evaluate(expression + current)
                }
                expression += label
                current = "0"
            }
            else -> {
                if (lastWasEquals || current == "Error") {
                    current = "0"
                    lastWasEquals = false
                }
                current = if (current == "0" && label != ".") label else current + label
                if (current.count { it == '.' } > 1) current = current.dropLast(1)
            }
        }
        updateDisplay()
    }

    private fun updateDisplay() {
        display.text = current
        preview.text = expression
    }

    private fun applyPercent(value: String): String {
        return try {
            BigDecimal(value)
                .divide(BigDecimal(100), MathContext.DECIMAL64)
                .stripTrailingZeros()
                .toPlainString()
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun evaluate(expr: String): String {
        return try {
            shuntingYard(tokenize(expr)).stripTrailingZeros().toPlainString()
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        val ops = setOf("+", "-", "\u00d7", "\u00f7")
        var i = 0
        while (i < expr.length) {
            val c = expr.get(i)
            if (c.isDigit() || c == '.') {
                val start = i
                while (i < expr.length && (expr.get(i).isDigit() || expr.get(i) == '.')) i++
                tokens.add(expr.substring(start, i))
            } else if (c == '-' && (tokens.isEmpty() || tokens.last() in ops)) {
                val start = i
                i++
                while (i < expr.length && (expr.get(i).isDigit() || expr.get(i) == '.')) i++
                if (i == start + 1) tokens.add("-") else tokens.add(expr.substring(start, i))
            } else {
                tokens.add(c.toString())
                i++
            }
        }
        return tokens
    }

    private fun shuntingYard(tokens: List<String>): BigDecimal {
        val output = mutableListOf<String>()
        val ops = mutableListOf<String>()
        val precedence = mapOf("\u00d7" to 2, "\u00f7" to 2, "+" to 1, "-" to 1)
        for (token in tokens) {
            if (token in precedence) {
                while (ops.isNotEmpty() && (precedence.get(ops.last()) ?: 0) >= (precedence.get(token) ?: 0)) {
                    output.add(ops.removeLast())
                }
                ops.add(token)
            } else {
                output.add(token)
            }
        }
        while (ops.isNotEmpty()) output.add(ops.removeLast())
        val stack = mutableListOf<BigDecimal>()
        for (token in output) {
            when (token) {
                "+" -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    stack.add(a.add(b))
                }
                "-" -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    stack.add(a.subtract(b))
                }
                "\u00d7" -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    stack.add(a.multiply(b))
                }
                "\u00f7" -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    if (b.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("div0")
                    stack.add(a.divide(b, MathContext.DECIMAL64))
                }
                else -> stack.add(BigDecimal(token))
            }
        }
        if (stack.size != 1) throw IllegalStateException("bad expr")
        return stack.last()
    }
}
