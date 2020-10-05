package press.widgets.popup

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AnimationUtils
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.ViewFlipper
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import me.saket.press.R

/**
 * A [ViewFlipper] that smoothly changes its height for the currently displayed child.
 */
class HeightAnimatableViewFlipper(context: Context) : BaseHeightClippableFlipper(context) {

  fun show(view: View, forward: Boolean) {
    enqueueAnimation {
      val index = if (forward) childCount else 0
      val params = view.layoutParams ?: LayoutParams(MATCH_PARENT, WRAP_CONTENT)
      super.addView(view, index, params)

      if (childCount == 1) {
        return@enqueueAnimation
      }

      setupFlipAnimation(forward)
      val prevView = displayedChildView!!
      displayedChildView = view

      doOnLayout {
        animateHeight(
            from = prevView.height,
            to = view.height,
            onEnd = {
              // ViewFlipper plays animation if the view
              // count goes down, which isn't wanted here.
              inAnimation = null
              outAnimation = null
              removeView(prevView)
            }
        )
      }
    }
  }

  fun goForward(child: View) =
    show(child, forward = true)

  fun goBack(child: View) =
    show(child, forward = false)

  override fun addView(child: View, index: Int, params: android.view.ViewGroup.LayoutParams) =
    throw error("Use show() / goForward() / goBack() instead")

  private fun enqueueAnimation(action: () -> Unit) {
    if (!animator.isRunning) action()
    else animator.doOnEnd { action() }
  }

  private fun setupFlipAnimation(goingForward: Boolean) {
    val inflate = { animRes: Int ->
      AnimationUtils.loadAnimation(context, animRes).also {
        it.duration = animationDuration
        it.interpolator = animationInterpolator
      }
    }

    if (goingForward) {
      inAnimation = inflate(R.anim.cascademenu_submenu_enter)
      outAnimation = inflate(R.anim.cascademenu_mainmenu_exit)
    } else {
      inAnimation = inflate(R.anim.cascademenu_mainmenu_enter)
      outAnimation = inflate(R.anim.cascademenu_submenu_exit)
    }
  }
}

@Suppress("LeakingThis")
abstract class BaseHeightClippableFlipper(context: Context) : ViewFlipper(context) {
  protected var animationDuration = 350L
  protected var animationInterpolator = FastOutSlowInInterpolator()

  private var clipBounds2: Rect? = null // Because View#clipBounds creates a new Rect on every call
  protected var animator: ValueAnimator = ObjectAnimator()

  init {
    setWillNotDraw(false)
    outlineProvider = ViewOutlineProvider.BACKGROUND
  }

  protected fun animateHeight(from: Int, to: Int, onEnd: () -> Unit) {
    animator.cancel()
    animator = ObjectAnimator.ofFloat(0f, 1f).apply {
      duration = animationDuration
      interpolator = FastOutSlowInInterpolator()

      addUpdateListener {
        val scale = it.animatedValue as Float
        val newHeight = ((to - from) * scale + from).toInt()
        setClippedHeight(newHeight)
      }
      doOnEnd { onEnd() }
      start()
    }
  }

  private fun setClippedHeight(newHeight: Int) {
    clipBounds2 = (clipBounds2 ?: Rect()).also {
      it.set(left, top, right, newHeight)
      clipBounds = it
    }
    invalidate()
  }

  override fun onDetachedFromWindow() {
    animator.cancel()
    super.onDetachedFromWindow()
  }

  @Suppress("DEPRECATION")
  override fun setBackgroundDrawable(background: Drawable?) {
    super.setBackgroundDrawable(background?.let(::DrawSuppressibleDrawable))
  }

  override fun draw(canvas: Canvas) {
    // Draw the background manually with clipped bounds, because
    // super.draw() will always reset it to View's bounds.
    background?.let {
      it.setBounds(left, top, right, clipBounds2?.height() ?: bottom)
      it.draw(canvas)
    }

    (background as DrawSuppressibleDrawable?).withDrawSuppressed {
      super.draw(canvas)
    }
  }

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    return if (clipBounds2 != null && !clipBounds2!!.contains(ev)) false
    else super.dispatchTouchEvent(ev)
  }
}

private var ViewFlipper.displayedChildView: View?
  get() = getChildAt(displayedChild)
  set(value) {
    displayedChild = indexOfChild(value)
  }

private fun Rect.contains(ev: MotionEvent): Boolean {
  return contains(ev.x.toInt(), ev.y.toInt())
}