package app.auspice.preferences.standard

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import app.auspice.R
import app.auspice.activities.MainActivity
import app.auspice.databinding.AuthorPreferenceRowBinding
import app.auspice.utilities.getThemeColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import androidx.core.net.toUri

class CustomAuthorPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs), View.OnClickListener {
    private val activityScope = CoroutineScope(Dispatchers.Main)

    // Easter egg stuff, why not
    private var easterEggCounter = 0
    private lateinit var confetti: KonfettiView

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val binding = AuthorPreferenceRowBinding.bind(holder.itemView)

        // Manage the shimmer
        val shimmer = binding.settingsShimmer
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        if (shimmerEnabled) shimmer.startShimmer()

        // Manage confetti
        confetti = binding.confettiEasterEggView

        // Make the icons clickable
        val logo = binding.imageLogo
        val github = binding.linkGithub
        val birday = binding.linkBirday

        // Start the logo animation, if the drawable has one
        activityScope.launch {
            delay(300)
            (logo.drawable as? Animatable)?.start()
        }
        logo.setOnClickListener(this)
        github.setOnClickListener(this)
        birday.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        // Vibrate using the common method in MainActivity
        val act = context as MainActivity
        val uri: Uri
        when (v.id) {
            R.id.imageLogo -> if (easterEggCounter == 5) {
                easterEggCounter = 0
                // Trigger a snackbar and confetti
                confetti.build()
                    .addColors(
                        getThemeColor(R.attr.colorTertiary, act),
                        getThemeColor(R.attr.colorSecondary, act),
                        getThemeColor(R.attr.colorPrimary, act),
                        getThemeColor(R.attr.colorOnSurface, act),
                    )
                    .setDirection(0.0, 359.0)
                    .setSpeed(0.5f, 4f)
                    .setRotationEnabled(true)
                    .setFadeOutEnabled(true)
                    .setTimeToLive(2000L)
                    .addShapes(
                        Shape.DrawableShape(
                            ContextCompat.getDrawable(
                                act,
                                R.drawable.ic_triangle_24dp
                            )!!
                        ),
                        Shape.DrawableShape(
                            ContextCompat.getDrawable(
                                act,
                                R.drawable.ic_favorites_24dp
                            )!!
                        ),
                        Shape.DrawableShape(
                            ContextCompat.getDrawable(
                                act,
                                R.drawable.ic_star_24dp
                            )!!
                        ),
                        Shape.DrawableShape(
                            ContextCompat.getDrawable(
                                act,
                                R.drawable.ic_octagram_24dp
                            )!!
                        )
                    )
                    .addSizes(Size(8), Size(12), Size(16))
                    // It should approximately start from the logo
                    .setPosition(confetti.x + confetti.width / 2, confetti.y + confetti.height / 3)
                    .burst(300)
                act.showSnackbar(context.getString(R.string.easter_egg))
            } else easterEggCounter++
            R.id.linkGithub -> {
                act.vibrate()
                uri = context.getString(R.string.dev_github).toUri()
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            R.id.linkBirday -> {
                act.vibrate()
                uri = context.getString(R.string.upstream_birday).toUri()
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
    }
}