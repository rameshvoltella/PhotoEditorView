package com.tencent.zebra.doodle;


import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class ZoomableImageView extends ZoomableImageViewBase {

	static final float SCROLL_DELTA_THRESHOLD = 1.0f;
//	protected ScaleGestureDetector mScaleDetector;
	protected GestureDetector mGestureDetector;
	protected int mTouchSlop;
	protected float mScaleFactor;
	protected int mDoubleTapDirection;
	protected OnGestureListener mGestureListener;
//	protected OnScaleGestureListener mScaleListener;
	protected boolean mDoubleTapEnabled = true;
	protected boolean mScaleEnabled = true;
	protected boolean mScrollEnabled = true;
	private OnImageViewTouchDoubleTapListener mDoubleTapListener;
	private OnImageViewTouchSingleTapListener mSingleTapListener;
	
	private boolean mDidScaleHappen = false;
	
	public boolean didScaleHappen() {
		return mDidScaleHappen;
	}

	public ZoomableImageView ( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}
	
	@Override
	protected void init() {
		super.init();
		mTouchSlop = ViewConfiguration.get( getContext() ).getScaledTouchSlop();
		mGestureListener = getGestureListener();
//		mScaleListener = getScaleListener();

//		mScaleDetector = new ScaleGestureDetector( getContext(), mScaleListener );
		mGestureDetector = new GestureDetector( getContext(), mGestureListener, null);//, true );

		mDoubleTapDirection = 1;
	}

	public void setDoubleTapListener( OnImageViewTouchDoubleTapListener listener ) {
		mDoubleTapListener = listener;
	}

	public void setSingleTapListener( OnImageViewTouchSingleTapListener listener ) {
		mSingleTapListener = listener;
	}

	public void setDoubleTapEnabled( boolean value ) {
		mDoubleTapEnabled = value;
	}

	public void setScaleEnabled( boolean value ) {
		mScaleEnabled = value;
	}

	public void setScrollEnabled( boolean value ) {
		mScrollEnabled = value;
	}

	public boolean getDoubleTapEnabled() {
		return mDoubleTapEnabled;
	}

	protected OnGestureListener getGestureListener() {
		return new GestureListener();
	}

//	protected OnScaleGestureListener getScaleListener() {
//		return new ScaleListener();
//	}

	@Override
	protected void _setImageDrawable( final Drawable drawable, final Matrix initial_matrix, float min_zoom, float max_zoom ) {
		super._setImageDrawable( drawable, initial_matrix, min_zoom, max_zoom );
		mScaleFactor = getMaxScale() / 3;
	}

	private float moveBaseX;
	private float moveBaseY;
	private float baseDistance;
	private boolean isScale = false;

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
//		mScaleDetector.onTouchEvent( event );
//
//		if ( !mScaleDetector.isInProgress() ) {
//			mGestureDetector.onTouchEvent( event );
//		}
//		ZebraLog.e("aaaa", "event action =" + event.getAction());
		int action = event.getAction();
		switch ( action & MotionEvent.ACTION_MASK ) {
			case MotionEvent.ACTION_DOWN: 
//				ZebraLog.e("aaaa", "MotionEvent.ACTION_DOWN......");
				moveBaseX = event.getX();
            	moveBaseY = event.getY();
            	baseDistance = 0;
				break;
			case MotionEvent.ACTION_POINTER_1_DOWN: 
            case MotionEvent.ACTION_POINTER_2_DOWN:
            	mUserScaled = true;
            	moveBaseX = 0;
            	moveBaseY = 0;
            	break;
			case MotionEvent.ACTION_MOVE:
//				ZebraLog.e("aaaa", "MotionEvent.ACTION_MOVE......");
				if (event.getPointerCount() == 2) {
//					ZebraLog.e("aaaaaa", "event.getPointerCount() == 2......");
					float moveX = (event.getX(0) + event.getX(1)) / 2;
					float moveY = (event.getY(0) + event.getY(1)) / 2;
					float distanceX = event.getX(0) - event.getX(1);
					float distanceY = event.getY(0) - event.getY(1);
					float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);// 计算两点的距离
					
					
					if (baseDistance == 0) {
						baseDistance = distance;
					} else if (moveBaseX == 0) {
						moveBaseX = moveX;
						moveBaseY = moveY;
					} else {
						if (Math.abs(distance - baseDistance) >= 15) {
							// 当前两点间的距离除以手指落下时两点间的距离就是需要缩放的比例。
							isScale = true;
							float scale = distance / baseDistance;
							baseDistance = distance;
							float targetScale = getScale() * scale;
							targetScale = Math.min( getMaxScale(), Math.max( targetScale, 1.0f));//getMinScale() - 0.1f ) );
							mDidScaleHappen = true;
							zoomTo( targetScale, moveX, moveY );
							invalidate();
						} else {
							float deltaX = moveX - moveBaseX;
							float deltaY = moveY - moveBaseY;
							scrollBy(deltaX, deltaY);
							moveBaseX = moveX;
							moveBaseY = moveY;
							invalidate();
						}
					}
				}
				break;
			case MotionEvent.ACTION_UP:
//				ZebraLog.e("aaaa", "MotionEvent.ACTION_UP......");
				if(isScale) {
					finishZoom();
					isScale = false;
				}
			    mUserScaled = false;
//			    Log.e("super.OnTouchEvent ACTION_UP", "pointer count : " + String.valueOf(event.getPointerCount()));
//				if ( getScale() < getMinScale() ) {
//					zoomTo( getMinScale(), 50 );
//				}
				break;
		}
		return true;
	}
	
	@Override
	protected void onZoomAnimationCompleted( float scale ) {

		if( LOG_ENABLED ) {
//			Log.d( LOG_TAG, "onZoomAnimationCompleted. scale: " + scale + ", minZoom: " + getMinScale() );
		}

		if ( scale < getMinScale() ) {
			zoomTo( getMinScale(), 50 );
		}
	}

	protected float onDoubleTapPost( float scale, float maxZoom ) {
		if ( mDoubleTapDirection == 1 ) {
			if ( ( scale + ( mScaleFactor * 2 ) ) <= maxZoom ) {
				return scale + mScaleFactor;
			} else {
				mDoubleTapDirection = -1;
				return maxZoom;
			}
		} else {
			mDoubleTapDirection = 1;
			return 1f;
		}
	}

	public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
		if ( !mScrollEnabled ) return false;

		if ( e1 == null || e2 == null ) return false;
//		if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
//		 Log.e("onScroll", "e1 : " + String.valueOf(e1.getPointerCount()) + " e2 : " + String.valueOf(e2.getPointerCount()));
		if ( e1.getPointerCount() <= 1 && e2.getPointerCount() <= 1 ) return false;
//		if ( mScaleDetector.isInProgress() ) return false;
		if ( getScale() == 1f ) return false;

		mUserScaled = true;
		scrollBy( -distanceX, -distanceY );
		invalidate();
		return true;
	}

	public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
		if ( !mScrollEnabled ) return false;

//		if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
		if ( e1.getPointerCount() <= 1 && e2.getPointerCount() <= 1 ) return false;
//		if ( mScaleDetector.isInProgress() ) return false;
		if ( getScale() == 1f ) return false;

		float diffX = e2.getX() - e1.getX();
		float diffY = e2.getY() - e1.getY();

		if ( Math.abs( velocityX ) > 800 || Math.abs( velocityY ) > 800 ) {
			mUserScaled = true;
			scrollBy( diffX / 2, diffY / 2, 300 );
			invalidate();
			return true;
		}
		return false;
	}

	/**
	 * Determines whether this ImageViewTouch can be scrolled.
	 * 
	 * @param direction
	 *            - positive direction value means scroll from right to left,
	 *            negative value means scroll from left to right
	 * 
	 * @return true if there is some more place to scroll, false - otherwise.
	 */
	public boolean canScroll( int direction ) {
		RectF bitmapRect = getBitmapRect();
		updateRect( bitmapRect, mScrollRect );
		Rect imageViewRect = new Rect();
		getGlobalVisibleRect( imageViewRect );
		
		if( null == bitmapRect ) {
			return false;
		}

		if ( bitmapRect.right >= imageViewRect.right ) {
			if ( direction < 0 ) {
				return Math.abs( bitmapRect.right - imageViewRect.right ) > SCROLL_DELTA_THRESHOLD;
			}
		}

		double bitmapScrollRectDelta = Math.abs( bitmapRect.left - mScrollRect.left );
		return bitmapScrollRectDelta > SCROLL_DELTA_THRESHOLD;
	}

	public class GestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onSingleTapConfirmed( MotionEvent e ) {
//		    Log.e("GestureListener onSingleTapConfirmed", "pointer count : " + String.valueOf(e.getPointerCount()));
			if ( null != mSingleTapListener ) {
				mSingleTapListener.onSingleTapConfirmed();
			}

			return super.onSingleTapConfirmed( e );
		}

		@Override
		public boolean onDoubleTap( MotionEvent e ) {
//			Log.i( LOG_TAG, "onDoubleTap. double tap enabled? " + mDoubleTapEnabled );
//			Log.e("GestureListener onDoubleTap", "pointer count : " + String.valueOf(e.getPointerCount()));
			if ( mDoubleTapEnabled ) {
				mUserScaled = true;
				float scale = getScale();
				float targetScale = scale;
				targetScale = onDoubleTapPost( scale, getMaxScale() );
				targetScale = Math.min( getMaxScale(), Math.max( targetScale, getMinScale() ) );
				zoomTo( targetScale, e.getX(), e.getY(), DEFAULT_ANIMATION_DURATION );
				invalidate();
			}

			if ( null != mDoubleTapListener ) {
				mDoubleTapListener.onDoubleTap();
			}

			return super.onDoubleTap( e );
		}

		@Override
		public void onLongPress( MotionEvent e ) {
//			if ( isLongClickable() ) {
//				if ( !mScaleDetector.isInProgress() ) {
//					setPressed( true );
//					performLongClick();
//				}
//			}
		}

		@Override
		public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
//		    Log.e("GestureListener onScroll", "onScroll");
			return ZoomableImageView.this.onScroll( e1, e2, distanceX, distanceY );
		}

		@Override
		public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
//		    Log.e("GestureListener onFling", "onFling");
			return ZoomableImageView.this.onFling( e1, e2, velocityX, velocityY );
		}
		
	}

	
	
//	public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
//		
//		protected boolean mScaled = false;
//
//		@Override
//		public boolean onScale( ScaleGestureDetector detector ) {
////		    Log.e("ScaleListener onScale", "onScale");
//			float span = detector.getCurrentSpan() - detector.getPreviousSpan();
//			if(Math.abs(span) < 5)
//		    	return true;
//			
//			float targetScale = getScale() * detector.getScaleFactor();
//			
//			if ( mScaleEnabled ) {
//				if( mScaled && span != 0 ) {
//					mUserScaled = true;
//					targetScale = Math.min( getMaxScale(), Math.max( targetScale, getMinScale() - 0.1f ) );
//					zoomTo( targetScale, detector.getFocusX(), detector.getFocusY() );
//					mDoubleTapDirection = 1;
//					invalidate();
//					return true;
//				}
//				
//				// This is to prevent a glitch the first time 
//				// image is scaled.
//				if( !mScaled ) mScaled = true;
//			}
//			return true;
//		}
//	}
	
	public interface OnImageViewTouchDoubleTapListener {

		void onDoubleTap();
	}

	public interface OnImageViewTouchSingleTapListener {

		void onSingleTapConfirmed();
	}
	
	public void finishZoom() {
		
	}
	
	public float getBaseScale() {
		return getScale(mBaseMatrix);
	}
}
