package org.sabda.family.utility

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import org.sabda.family.R

class YTPlayerUtil {

    fun showYoutubePopup(context: Context, videoUrl: String) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_youtube_player, null)
        dialog.setContentView(view)

        val youTubePlayerView = view.findViewById<YouTubePlayerView>(R.id.youtubePlayerView1)
        initializeYouTubePlayer(youTubePlayerView, videoUrl)

        dialog.show()
    }

    fun initializeYouTubePlayer(youTubePlayerView: YouTubePlayerView, videoUrl: String) {
        val videoId = extractVideoId(videoUrl)

        if (videoId != null) {
            youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.loadVideo(videoId, 0f)
                }
            })
        }
    }

    private fun extractVideoId(url: String): String? {
        return when {
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/")
            url.contains("youtube.com/watch?v=") -> url.substringAfter("v=").takeWhile { it != '&' }
            else -> null
        }
    }

}