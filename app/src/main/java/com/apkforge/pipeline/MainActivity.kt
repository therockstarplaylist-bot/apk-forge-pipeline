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
import java.math.RoundingMode

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
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        display = findViewById(R.id.display)
        preview = findViewById(R.id.preview)

        val keys = listOf(
            R.id.btn_c, R.id.btn_sign, R.id.btn_pct, R.id.btn_div,
            R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_mul,
            R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_sub,
            R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_add,
            R.id.btn_0, R.id.btn_dot, R.id.btn_back, R.id.btn_eq
        )

        keys.forEach { id ->
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
                if (current.startsWith("-")) current = current.drop(1) else if (current != "0") current = "-$current"
            }
            "%" -> {
                current = applyPercent(current)
            }
            "⌫" -> {
                current = if (current.length > 1) current.dropLast(1) else "0"
            }
            "=" -> {
                val full = if (expression.isNotEmpty()) expression + current else current
                current = evaluate(full)
                expression = ""
                lastWasEquals = true
            }
            in listOf("+", "-", "×", "÷") -> {
                if (lastWasEquals) {
                    expression = current
                    lastWasEquals = false
                } else {
                    expression = if (expression.isEmpty()) current else expression + current
                }
                expression += label
                current = "0"
            }
            else -> {
                if (lastWasEquals) {
                    current = "0"
                    lastWasEquals = false
                }
                current = if (current == "0" && label != ".") label else current + label
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
            BigDecimal(value).divide(BigDecimal(100), MathContext.DECIMAL64).stripTrailingZeros().toPlainString()
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun evaluate(expr: String): String {
        return try {
            val tokens = tokenize(expr)
            val result = shuntingYard(tokens)
            result.stripTrailingZeros().toPlainString()
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr .isDigit() || expr == '.')) i++
                    tokens.add(expr.substring(start, i))
                    continue
                }
                c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "×", "÷", "(")) -> {
                    val start = i
                    i++
                    while (i < expr.length && (expr .isDigit() || expr == '.')) i++
                    tokens.add(expr.substring(start, i))
                    continue
                }
                else -> {
                    tokens.add(c.toString())
                    i++
                }
            }
        }
        return tokens
    }

    private fun shuntingYard(tokens: List<String>): BigDecimal {
        val output = mutableListOf<String>()
        val ops = mutableListOf<String>()
        val precedence = mapOf("×" to 2, "÷" to 2, "+" to 1, "-" to 1)

        for (token in tokens) {
            when (token) {
                in precedence -> {
                    while (ops.isNotEmpty() && precedence ?: 0 >= precedence !!) {
                        output.add(ops.removeLast())
                    }
                    ops.add(token)
                }
                else -> output.add(token)
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
                "×" -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    stack.add(a.multiply(b))
                }
                "÷" -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    if (b.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("div0")
                    stack.add(a.divide(b, MathContext.DECIMAL64))
                }
                else -> stack.add(BigDecimal(token))
            }
        }
        return stack.last()
    }
}
