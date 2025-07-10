package com.example.techappnew

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max

@Suppress("DEPRECATION")
class SplashActivity : AppCompatActivity() {

    private var pulseCount = 0
    private val maxPulseCount = 3
    private var currentScale = 1.0f  // Начальный масштаб
    private lateinit var ball: ImageView
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        ball = findViewById(R.id.ball)
        container = findViewById(R.id.splashContainer)

        container.post {
            animateFallDown()
        }
    }

    private fun animateFallDown() {
        val targetY = container.height / 2f - ball.height / 2f
        ObjectAnimator.ofFloat(ball, "translationY", -60f, targetY).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startPulsing()
                }
            })
            start()
        }
    }

    private fun startPulsing() {
        // Пульсация: увеличение на 10% и возврат к текущему масштабу
        val pulseAnim = ObjectAnimator.ofPropertyValuesHolder(
            ball,
            PropertyValuesHolder.ofFloat("scaleX", currentScale, currentScale * 1.1f, currentScale),
            PropertyValuesHolder.ofFloat("scaleY", currentScale, currentScale * 1.1f, currentScale)
        ).apply {
            duration = 500
            interpolator = OvershootInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    pulseCount++
                    currentScale *= 1.5f  // Фиксируем увеличение на 10% после пульсации

                    if (pulseCount < maxPulseCount) {
                        startPulsing()  // Рекурсивный вызов для следующей пульсации
                    } else {
                        animateExpand()
                    }
                }
            })
        }
        pulseAnim.start()
    }

    private fun animateExpand() {
        val targetScale = max(
            container.width / ball.width.toFloat(),
            container.height / ball.height.toFloat()
        ) * 1.2f  // Масштаб для заполнения экрана

        ObjectAnimator.ofPropertyValuesHolder(
            ball,
            PropertyValuesHolder.ofFloat("scaleX", currentScale, targetScale),
            PropertyValuesHolder.ofFloat("scaleY", currentScale, targetScale)
        ).apply {
            duration = 1000
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                }
            })
            start()
        }
    }
}