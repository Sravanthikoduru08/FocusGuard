package com.example.focusguard.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.focusguard.R
import java.util.*

class NeuralParticlesView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random()
    private var particleColor = context.getColor(R.color.cyan_glow)

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var alpha: Int
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        particles.clear()
        if (w > 0 && h > 0) {
            for (i in 0 until 40) {
                particles.add(createParticle(w, h))
            }
        }
    }

    private fun createParticle(w: Int, h: Int): Particle {
        return Particle(
            x = random.nextFloat() * w,
            y = random.nextFloat() * h,
            vx = (random.nextFloat() - 0.5f) * 2f,
            vy = (random.nextFloat() - 0.5f) * 2f,
            size = random.nextFloat() * 8f + 2f,
            alpha = random.nextInt(100) + 50
        )
    }

    fun setParticleColor(color: Int) {
        particleColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (particles.isEmpty()) return

        paint.color = particleColor
        
        for (p in particles) {
            // Move particle
            p.x += p.vx
            p.y += p.vy
            
            // Boundary checks
            if (p.x < 0 || p.x > width) p.vx *= -1
            if (p.y < 0 || p.y > height) p.vy *= -1
            
            // Draw particle
            paint.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.size, paint)
            
            // Draw connections between close particles (Neural link effect)
            for (other in particles) {
                if (p === other) continue
                val dx = p.x - other.x
                val dy = p.y - other.y
                val distSq = dx * dx + dy * dy
                
                if (distSq < 150f * 150f) {
                    val dist = Math.sqrt(distSq.toDouble()).toFloat()
                    paint.strokeWidth = 1f
                    paint.alpha = ((1f - dist / 150f) * 50).toInt()
                    canvas.drawLine(p.x, p.y, other.x, other.y, paint)
                }
            }
        }
        
        postInvalidateOnAnimation()
    }
}
