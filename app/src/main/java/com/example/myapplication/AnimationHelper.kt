package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView

object AnimationHelper {

    private var currentToast: ViewGroup? = null
    private var currentXpStack = 0
    private var fadeOutRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun vibrateMini(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }

    fun showXpToast(context: Context, amount: Int) {
        val activity = context as? Activity ?: return
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)

        if (currentToast != null && currentToast!!.isAttachedToWindow) {
            updateExistingToast(amount)
        } else {
            createNewToast(context, rootView, amount)
        }
    }

    private fun createNewToast(context: Context, rootView: ViewGroup, amount: Int) {
        val inflater = LayoutInflater.from(context)
        val toastView = inflater.inflate(R.layout.toast_animated, rootView, false) as ViewGroup

        currentToast = toastView
        currentXpStack = amount

        // === KOLOROWANIE TOASTA ===
        val themeColor = GameManager.appThemeColor

        // 1. Tło dymku
        toastView.backgroundTintList = ColorStateList.valueOf(themeColor)

        // 2. Ikona gwiazdki (dla kontrastu, np. biała albo lekko jaśniejsza od tła)
        // Możemy zostawić złotą (jak jest w XML) albo zmienić na białą, bo tło jest teraz kolorowe.
        // Zmieńmy na białą dla lepszej czytelności na kolorowym tle:
        val icon = toastView.findViewById<ImageView>(R.id.toast_icon)
        icon.imageTintList = ColorStateList.valueOf(android.graphics.Color.WHITE)
        // ==========================

        val textWrapper = toastView.findViewById<View>(R.id.text_wrapper)
        val text = toastView.findViewById<TextView>(R.id.tv_toast_message)

        text.text = "+$currentXpStack XP"

        toastView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val targetWidth = textWrapper.measuredWidth

        val params = textWrapper.layoutParams
        params.width = 0
        textWrapper.layoutParams = params

        rootView.addView(toastView)

        val iconScaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 0f, 1f)
        val iconScaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 0f, 1f)
        val popAnim = AnimatorSet().apply {
            playTogether(iconScaleX, iconScaleY)
            duration = 400
            interpolator = OvershootInterpolator(1.2f)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    vibrateMini(context)
                }
            })
        }

        val widthAnimator = ValueAnimator.ofInt(0, targetWidth + 50).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val layoutParams = textWrapper.layoutParams
                layoutParams.width = animation.animatedValue as Int
                textWrapper.layoutParams = layoutParams
            }
        }
        val textFade = ObjectAnimator.ofFloat(text, View.ALPHA, 0f, 1f).apply { duration = 300 }

        val expandAnim = AnimatorSet().apply {
            playTogether(widthAnimator, textFade)
            startDelay = 150
        }

        AnimatorSet().apply {
            playSequentially(popAnim, expandAnim)
            start()
        }

        scheduleFadeOut(toastView, rootView)
    }

    private fun updateExistingToast(amount: Int) {
        val toast = currentToast ?: return
        val text = toast.findViewById<TextView>(R.id.tv_toast_message)

        if (fadeOutRunnable != null) handler.removeCallbacks(fadeOutRunnable!!)
        toast.alpha = 1f

        currentXpStack += amount
        text.text = "+$currentXpStack XP"

        val scaleX = ObjectAnimator.ofFloat(text, View.SCALE_X, 1f, 1.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(text, View.SCALE_Y, 1f, 1.3f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 200
            start()
        }

        vibrateMini(toast.context)
        val rootView = toast.parent as? ViewGroup
        if (rootView != null) scheduleFadeOut(toast, rootView)
    }

    private fun scheduleFadeOut(toastView: ViewGroup, rootView: ViewGroup) {
        fadeOutRunnable = Runnable {
            toastView.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    rootView.removeView(toastView)
                    if (currentToast == toastView) {
                        currentToast = null
                        currentXpStack = 0
                    }
                }
                .start()
        }
        handler.postDelayed(fadeOutRunnable!!, 1500)
    }

    fun showLevelUpDialog(context: Context, newLevel: Int) {
        val activity = context as? Activity ?: return
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val inflater = LayoutInflater.from(context)

        // Zakładam, że używasz "overlay_level_up" (jak w poprzednim kodzie),
        // ale wkleiłeś mi XML z CardView (Dialog lvl up).
        // Żeby to zadziałało z Twoim nowym CardView XMLem, musimy go tu użyć.
        // Jeśli jednak chcesz animowany overlay, zostaw stary layout.
        // Poniżej kod dla twojego nowego XMLa z CardView:

        val dialogView = inflater.inflate(R.layout.dialog_level_up, rootView, false) as ViewGroup // Upewnij się że plik nazywa się dialog_level_up.xml

        // === KOLOROWANIE LEVEL UP ===
        val themeColor = GameManager.appThemeColor

        // 1. Tytuł LEVEL UP
        dialogView.findViewById<TextView>(R.id.tv_level_up_title)?.setTextColor(themeColor)

        // 2. Gwiazda
        dialogView.findViewById<ImageView>(R.id.iv_level_up_star)?.imageTintList = ColorStateList.valueOf(themeColor)

        // 3. Przycisk OK
        dialogView.findViewById<Button>(R.id.btn_level_up_ok)?.backgroundTintList = ColorStateList.valueOf(themeColor)
        // ============================

        val message = dialogView.findViewById<TextView>(R.id.tv_level_up_message)
        message.text = "Poziom $newLevel osiągnięty!"

        rootView.addView(dialogView)

        // Prosta animacja wejścia dla CardView
        dialogView.alpha = 0f
        dialogView.scaleX = 0.8f
        dialogView.scaleY = 0.8f

        dialogView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator())
            .start()

        // Wibracja
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }

        dialogView.findViewById<Button>(R.id.btn_level_up_ok).setOnClickListener {
            dialogView.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(300)
                .withEndAction { rootView.removeView(dialogView) }
                .start()
        }
    }
}