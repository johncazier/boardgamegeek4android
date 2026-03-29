package com.boardgamegeek.ui.image

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.boardgamegeek.ui.MainActivity
import com.boardgamegeek.ui.navigation.ImageRoute
import com.boardgamegeek.ui.theme.AppTheme
import com.boardgamegeek.util.PaletteTransformation
import com.github.chrisbanes.photoview.PhotoView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import timber.log.Timber

object ImageActivity {
    fun start(context: Context, imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) {
            Timber.w("Missing the required image URL.")
            return
        }
        context.startActivity(
            MainActivity.createIntent(
                context = context,
                route = ImageRoute(imageUrl = imageUrl),
            ),
        )
    }
}

@Composable
fun ImageRouteScreen(route: ImageRoute) {
    val context = LocalContext.current
    val firebaseAnalytics = remember(context) { FirebaseAnalytics.getInstance(context) }

    LaunchedEffect(route.imageUrl) {
        if (route.imageUrl.isBlank()) {
            Timber.w("Received an empty imageUrl")
        } else {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "Image")
                param(FirebaseAnalytics.Param.ITEM_ID, route.imageUrl)
            }
        }
    }

    AppTheme {
        var isLoading by remember { mutableStateOf(true) }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { viewContext ->
                    PhotoView(viewContext).apply {
                        contentDescription = viewContext.getString(R.string.image)
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { view ->
                    Picasso.get()
                        .load(route.imageUrl.ensureHttpsScheme())
                        .error(R.drawable.thumbnail_image_empty)
                        .fit()
                        .centerInside()
                        .transform(PaletteTransformation.instance())
                        .into(view, object : Callback.EmptyCallback() {
                            override fun onSuccess() {
                                setBackgroundColor(view)
                                isLoading = false
                            }

                            override fun onError(e: Exception?) {
                                isLoading = false
                            }
                        })
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

private fun setBackgroundColor(view: PhotoView) {
    val bitmap = (view.drawable as? BitmapDrawable)?.bitmap ?: return
    val palette = PaletteTransformation.getPalette(bitmap)
    val swatch = palette?.darkMutedSwatch ?: palette?.darkVibrantSwatch ?: palette?.mutedSwatch
    view.setBackgroundColor(swatch?.rgb ?: Color.BLACK)
}
