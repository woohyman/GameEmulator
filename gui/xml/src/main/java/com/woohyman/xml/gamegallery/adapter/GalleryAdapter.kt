package com.woohyman.xml.gamegallery.adapter

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.woohyman.keyboard.rom.IRomLauncher
import com.woohyman.xml.gamegallery.base.BaseGalleryAdapter
import kotlinx.coroutines.launch

class GalleryAdapter(
    private val activity: AppCompatActivity,
    private val romLauncher: IRomLauncher
) : BaseGalleryAdapter(activity) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val (game) = filterGames[position]
        if(game == null){
            return view
        }
        view.setOnClickListener {
            activity.lifecycleScope.launch {
                romLauncher.LauncherRom(game)
            }
        }

        return view
    }

}