package com.s24ultra.fluidlauncher.config

data class TransitionConfig(
    var waveAmplitude: Float = 65f,
    var waveWidth: Float = 190f,
    var fingerVerticalSpread: Float = 280f,
    var horizontalDisplacement: Float = 48f,
    var zElevationMax: Float = 130f,
    var curvatureStrength: Float = 1.4f,
    var springStiffness: Float = 280f,
    var settlingDamping: Float = 26f,
    var appDrawerCurvature: Float = .45f,
    var debugSlowMotion: Boolean = false,
)
