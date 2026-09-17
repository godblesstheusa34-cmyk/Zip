package com.s24ultra.fluidlauncher.physics

import kotlin.math.abs

/** Allocation-free fourth-order Runge-Kutta integration of a damped spring. */
class SpringPhysics {
    var position = 0f
    var velocity = 0f
    var target = 0f

    fun step(dtSeconds: Float, stiffness: Float, damping: Float): Boolean {
        val dt = dtSeconds.coerceIn(0f, 1f / 30f)
        fun acceleration(x: Float, v: Float) = -stiffness * (x - target) - damping * v
        val x = position
        val v = velocity
        val k1x = v
        val k1v = acceleration(x, v)
        val k2x = v + k1v * dt * .5f
        val k2v = acceleration(x + k1x * dt * .5f, v + k1v * dt * .5f)
        val k3x = v + k2v * dt * .5f
        val k3v = acceleration(x + k2x * dt * .5f, v + k2v * dt * .5f)
        val k4x = v + k3v * dt
        val k4v = acceleration(x + k3x * dt, v + k3v * dt)
        position = x + dt * (k1x + 2f * k2x + 2f * k3x + k4x) / 6f
        velocity = v + dt * (k1v + 2f * k2v + 2f * k3v + k4v) / 6f
        if (abs(position - target) < .0001f && abs(velocity) < .0001f) {
            position = target
            velocity = 0f
            return false
        }
        return true
    }
}
