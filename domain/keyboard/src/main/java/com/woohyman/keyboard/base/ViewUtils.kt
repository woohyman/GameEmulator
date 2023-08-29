package com.woohyman.keyboard.base

import android.content.Context
import com.woohyman.keyboard.data.entity.GfxProfile
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.EmuUtils.emulator
import com.woohyman.keyboard.utils.PreferenceUtil

object ViewUtils {
    fun computeViewPort(
        emulator: Emulator?, screenWidth: Int,
        screenHeight: Int, paddingLeft: Int, paddingTop: Int
    ): ViewPort {
        var gfx: GfxProfile? = null
        gfx = if (emulator != null) {
            emulator.activeGfxProfile
        } else {
            EmuUtils.emulator.info.defaultGfxProfile
        }
        return computeViewPort(
            gfx, screenWidth, screenHeight, paddingLeft,
            paddingTop
        )
    }

    @JvmStatic
    fun computeInitViewPort(
        context: Context?, w: Int, h: Int,
        paddingLeft: Int, paddingTop: Int
    ): ViewPort {
        val gfx = emulator.info.defaultGfxProfile
        return computeViewPort(gfx, w, h, paddingLeft, paddingTop)
    }

    @JvmStatic
    fun computeAllInitViewPorts(
        context: Context?, w: Int, h: Int, paddingLeft: Int, paddingTop: Int
    ): HashMap<String, ViewPort> {
        val res = HashMap<String, ViewPort>()
        for (profile in emulator.info.availableGfxProfiles!!) {
            val vp = computeViewPort(
                profile, w, h, paddingLeft,
                paddingTop
            )
            res[profile!!.name] = vp
        }
        return res
    }

    @JvmStatic
    fun loadOrComputeAllViewPorts(
        context: Context?, w: Int, h: Int, paddingLeft: Int, paddingTop: Int
    ): HashMap<String, ViewPort> {
        val res = computeAllInitViewPorts(
            context, w, h,
            paddingLeft, paddingTop
        )
        for (profile in emulator.info.availableGfxProfiles!!) {
            val vp = loadViewPort(context, w, h, profile)
            if (vp != null) {
                res[profile!!.name!!] = vp
            }
        }
        return res
    }

    @JvmStatic
    fun loadOrComputeViewPort(
        context: Context?, emulator: Emulator?,
        w: Int, h: Int, paddingLeft: Int, paddingTop: Int,
        ignoreFullscreenSettings: Boolean
    ): ViewPort? {
        var vp: ViewPort? = null
        var profile: GfxProfile? = null
        profile = if (emulator != null) {
            emulator.activeGfxProfile
        } else {
            EmuUtils.emulator.info.defaultGfxProfile
        }
        if (!ignoreFullscreenSettings
            && PreferenceUtil.isFullScreenEnabled(context)
        ) {
            vp = ViewPort()
            vp.height = h
            vp.width = w
            vp.x = 0
            vp.y = 0
        } else if (loadViewPort(context, w, h, profile) != null) {
            vp = loadViewPort(context, w, h, profile)
        } else {
            vp = computeViewPort(
                profile, w, h, paddingLeft,
                paddingTop
            )
        }
        return vp
    }

    private fun loadViewPort(
        context: Context?, w: Int, h: Int,
        profile: GfxProfile?
    ): ViewPort? {
        val vp = PreferenceUtil.getViewPort(context, w, h)
        val defaultProfile = emulator.info.defaultGfxProfile
        if (vp != null && profile !== defaultProfile) {
            var vpw = vp.width
            var vph = vp.height
            val ow = vpw
            val oh = vph
            val ratio = profile!!.originalScreenHeight.toFloat() / profile.originalScreenWidth
            if (w < h) {
                vpw = vp.width
                vph = (vpw * ratio).toInt()
            } else {
                vph = vp.height
                vpw = (vph / ratio).toInt()
                vp.x += (ow - vpw) / 2
            }
            vp.width = vpw
            vp.height = vph
        }
        return vp
    }

    fun computeViewPort(
        gfx: GfxProfile?, screenWidth: Int,
        screenHeight: Int, paddingLeft: Int, paddingTop: Int
    ): ViewPort {
        var gfx = gfx
        if (gfx == null) {
            gfx = emulator.info.defaultGfxProfile
        }
        val w = screenWidth - paddingLeft
        val h = screenHeight - paddingTop
        val vpw: Int
        val vph: Int
        val ratio = gfx!!.originalScreenHeight.toFloat() / gfx.originalScreenWidth
        if (w < h) {
            vpw = w
            vph = (vpw * ratio).toInt()
        } else {
            vph = h
            vpw = (vph / ratio).toInt()
        }
        val result = ViewPort()
        result.x = (w - vpw) / 2 + paddingLeft
        result.y = paddingTop
        result.height = vph
        result.width = vpw
        return result
    }
}