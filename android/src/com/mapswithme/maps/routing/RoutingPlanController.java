package com.mapswithme.maps.routing;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.mapswithme.maps.Framework;
import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.R;
import com.mapswithme.maps.uber.Uber;
import com.mapswithme.maps.uber.UberInfo;
import com.mapswithme.maps.widget.RotateDrawable;
import com.mapswithme.maps.widget.RoutingToolbarButton;
import com.mapswithme.maps.widget.ToolbarController;
import com.mapswithme.maps.widget.WheelProgressView;
import com.mapswithme.util.UiUtils;
import com.mapswithme.util.statistics.AlohaHelper;
import com.mapswithme.util.statistics.Statistics;

public class RoutingPlanController extends ToolbarController implements SlotFrame.SlotClickListener
{
  static final int ANIM_TOGGLE = MwmApplication.get().getResources().getInteger(R.integer.anim_slots_toggle);

  protected final View mFrame;
  private final ImageView mToggle;
  private final SlotFrame mSlotFrame;
  private final RadioGroup mRouterTypes;
  private final WheelProgressView mProgressVehicle;
  private final WheelProgressView mProgressPedestrian;
  private final WheelProgressView mProgressBicycle;
  private final WheelProgressView mProgressTaxi;

  @NonNull
  private final RoutingBottomMenuController mRoutingBottomMenuController;

  private final RotateDrawable mToggleImage = new RotateDrawable(R.drawable.ic_down);
  int mFrameHeight;
  private int mToolbarHeight;
  private boolean mOpen;

  @Nullable
  private OnToggleListener mToggleListener;

  @Nullable
  private  SearchPoiTransitionListener mPoiTransitionListener;

  public interface OnToggleListener
  {
    void onToggle(boolean state);
  }

  public interface SearchPoiTransitionListener
  {
    void animateSearchPoiTransition(@NonNull final Rect startRect,
                                    @Nullable final Runnable runnable);
  }

  private RadioButton setupRouterButton(@IdRes int buttonId, final @DrawableRes int iconRes, View.OnClickListener clickListener)
  {
    CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener()
    {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
      {
        RoutingToolbarButton button = (RoutingToolbarButton) buttonView;
        button.setIcon(iconRes);
        if (isChecked)
          button.activate();
        else
          button.deactivate();
      }
    };

    RoutingToolbarButton rb = (RoutingToolbarButton) mRouterTypes.findViewById(buttonId);
    listener.onCheckedChanged(rb, false);
    rb.setOnCheckedChangeListener(listener);
    rb.setOnClickListener(clickListener);
    return rb;
  }

  RoutingPlanController(View root, Activity activity)
  {
    super(root, activity);
    mFrame = root;

    mToggle = (ImageView) mToolbar.findViewById(R.id.toggle);
    mSlotFrame = (SlotFrame) root.findViewById(R.id.slots);
    mSlotFrame.setSlotClickListener(this);
    mRouterTypes = (RadioGroup) mToolbar.findViewById(R.id.route_type);

    setupRouterButton(R.id.vehicle, R.drawable.ic_car, new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        AlohaHelper.logClick(AlohaHelper.ROUTING_VEHICLE_SET);
        Statistics.INSTANCE.trackEvent(Statistics.EventName.ROUTING_VEHICLE_SET);
        RoutingController.get().setRouterType(Framework.ROUTER_TYPE_VEHICLE);
      }
    });

    setupRouterButton(R.id.pedestrian, R.drawable.ic_pedestrian, new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        AlohaHelper.logClick(AlohaHelper.ROUTING_PEDESTRIAN_SET);
        Statistics.INSTANCE.trackEvent(Statistics.EventName.ROUTING_PEDESTRIAN_SET);
        RoutingController.get().setRouterType(Framework.ROUTER_TYPE_PEDESTRIAN);
      }
    });

    setupRouterButton(R.id.bicycle, R.drawable.ic_bike, new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        AlohaHelper.logClick(AlohaHelper.ROUTING_BICYCLE_SET);
        Statistics.INSTANCE.trackEvent(Statistics.EventName.ROUTING_BICYCLE_SET);
        RoutingController.get().setRouterType(Framework.ROUTER_TYPE_BICYCLE);
      }
    });

    setupRouterButton(R.id.taxi, R.drawable.ic_taxi, new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        AlohaHelper.logClick(AlohaHelper.ROUTING_TAXI_SET);
        Statistics.INSTANCE.trackEvent(Statistics.EventName.ROUTING_TAXI_SET);
        RoutingController.get().setRouterType(Framework.ROUTER_TYPE_TAXI);
      }
    });

    View progressFrame = mToolbar.findViewById(R.id.progress_frame);
    mProgressVehicle = (WheelProgressView) progressFrame.findViewById(R.id.progress_vehicle);
    mProgressPedestrian = (WheelProgressView) progressFrame.findViewById(R.id.progress_pedestrian);
    mProgressBicycle = (WheelProgressView) progressFrame.findViewById(R.id.progress_bicycle);
    mProgressTaxi = (WheelProgressView) progressFrame.findViewById(R.id.progress_taxi);

    mRoutingBottomMenuController = RoutingBottomMenuController.newInstance(mActivity, mFrame);

    mToggle.setImageDrawable(mToggleImage);
    mToggle.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        toggleSlots();
      }
    });
  }

  @Override
  public void onUpClick()
  {
    AlohaHelper.logClick(AlohaHelper.ROUTING_CANCEL);
    Statistics.INSTANCE.trackEvent(Statistics.EventName.ROUTING_CANCEL);
    RoutingController.get().cancel();
  }

  @Override
  public void onSlotClicked(final int order, @NonNull Rect rect)
  {
    if (mPoiTransitionListener != null)
    {
      mPoiTransitionListener.animateSearchPoiTransition(rect, new Runnable()
      {
        @Override
        public void run()
        {
          RoutingController.get().searchPoi();
        }
      });
    }
    else
    {
      RoutingController.get().searchPoi();
    }
  }

  public void setPoiTransitionListener(@Nullable SearchPoiTransitionListener poiTransitionListener)
  {
    mPoiTransitionListener = poiTransitionListener;
  }

  boolean checkFrameHeight()
  {
    if (mFrameHeight > 0)
      return true;

    mFrameHeight = mSlotFrame.getHeight();
    mToolbarHeight = mToolbar.getHeight();
    return (mFrameHeight > 0);
  }

  private void animateSlotFrame(int offset)
  {
    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mSlotFrame.getLayoutParams();
    lp.topMargin = (mToolbarHeight - offset);
    mSlotFrame.setLayoutParams(lp);
  }

  public void updatePoints()
  {
    mSlotFrame.update();
  }

  private void updateProgressLabels()
  {
    RoutingController.BuildState buildState = RoutingController.get().getBuildState();

    boolean ready = (buildState == RoutingController.BuildState.BUILT);

    if (!ready) 
    {
      mRoutingBottomMenuController.hideAltitudeChartAndRoutingDetails();
      return;
    }

    if (!isTaxiRouterType())
    {
      mRoutingBottomMenuController.setStartButton();
      mRoutingBottomMenuController.showAltitudeChartAndRoutingDetails();
    }
  }

  public void updateBuildProgress(int progress, @Framework.RouterType int router)
  {
    UiUtils.invisible(mProgressVehicle, mProgressPedestrian, mProgressBicycle, mProgressTaxi);
    WheelProgressView progressView;
    if (router == Framework.ROUTER_TYPE_VEHICLE)
    {
      mRouterTypes.check(R.id.vehicle);
      progressView = mProgressVehicle;
    }
    else if (router == Framework.ROUTER_TYPE_PEDESTRIAN)
    {
      mRouterTypes.check(R.id.pedestrian);
      progressView = mProgressPedestrian;
    }
    else if (router == Framework.ROUTER_TYPE_TAXI)
    {
      mRouterTypes.check(R.id.taxi);
      progressView = mProgressTaxi;
    }
    else
    {
      mRouterTypes.check(R.id.bicycle);
      progressView = mProgressBicycle;
    }

    RoutingToolbarButton button = (RoutingToolbarButton)mRouterTypes
        .findViewById(mRouterTypes.getCheckedRadioButtonId());
    button.progress();

    updateProgressLabels();

    if (RoutingController.get().isUberRequestHandled())
    {
      if (!RoutingController.get().isInternetConnected())
      {
        showNoInternetError();
        return;
      }
      button.complete();
      return;
    }

    if (!RoutingController.get().isBuilding() && !RoutingController.get().isUberPlanning())
    {
      button.complete();
      return;
    }

    UiUtils.show(progressView);
    progressView.setPending(progress == 0);
    if (progress != 0)
      progressView.setProgress(progress);
  }

  private void toggleSlots()
  {
    AlohaHelper.logClick(AlohaHelper.ROUTING_TOGGLE);
    Statistics.INSTANCE.trackEvent(Statistics.EventName.ROUTING_TOGGLE);
    showSlots(!mOpen, true);
  }

  void showSlots(final boolean show, final boolean animate)
  {
    if (!checkFrameHeight())
    {
      mFrame.post(new Runnable()
      {
        @Override
        public void run()
        {
          showSlots(show, animate);
        }
      });
      return;
    }

    mOpen = show;

    if (animate)
    {
      ValueAnimator animator = ValueAnimator.ofFloat(mOpen ? 1.0f : 0, mOpen ? 0 : 1.0f);
      animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
      {
        @Override
        public void onAnimationUpdate(ValueAnimator animation)
        {
          float fraction = (float)animation.getAnimatedValue();
          animateSlotFrame((int)(fraction * mFrameHeight));
          mToggleImage.setAngle((1.0f - fraction) * 180.0f);
        }
      });
      animator.addListener(new UiUtils.SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation)
        {
          if (mToggleListener != null)
            mToggleListener.onToggle(mOpen);
        }
      });

      animator.setDuration(ANIM_TOGGLE);
      animator.start();
      mSlotFrame.fadeSlots(!mOpen);
    }
    else
    {
      animateSlotFrame(mOpen ? 0 : mFrameHeight);
      mToggleImage.setAngle(mOpen ? 180.0f : 0.0f);
      mSlotFrame.unfadeSlots();
      mSlotFrame.post(new Runnable()
      {
        @Override
        public void run()
        {
          if (mToggleListener != null)
            mToggleListener.onToggle(mOpen);
        }
      });
    }
  }

  private boolean isTaxiRouterType()
  {
    return RoutingController.get().isTaxiRouterType();
  }

  void disableToggle()
  {
    UiUtils.hide(mToggle);
    showSlots(true, false);
  }

  public boolean isOpen()
  {
    return mOpen;
  }

  public void showUberInfo(@NonNull UberInfo info)
  {
    mRoutingBottomMenuController.showUberInfo(info);
  }

  public void showUberError(@NonNull Uber.ErrorCode code)
  {
    switch (code)
    {
      case NoProducts:
        showError(R.string.taxi_not_found);
        break;
      case RemoteError:
        showError(R.string.dialog_taxi_error);
        break;
      default:
        throw new AssertionError("Unsupported uber error: " + code);
    }
  }

  private void showNoInternetError()
  {
    @IdRes
    int checkedId = mRouterTypes.getCheckedRadioButtonId();
    RoutingToolbarButton rb = (RoutingToolbarButton) mRouterTypes.findViewById(checkedId);
    rb.error();
    showError(R.string.dialog_taxi_offline);
  }

  private void showError(@StringRes int message)
  {
    mRoutingBottomMenuController.showError(message);
  }

  void showStartButton(boolean show)
  {
    mRoutingBottomMenuController.showStartButton(show);
  }

  void saveRoutingPanelState(@NonNull Bundle outState)
  {
    mRoutingBottomMenuController.saveRoutingPanelState(outState);
  }

  void restoreRoutingPanelState(@NonNull Bundle state)
  {
    mRoutingBottomMenuController.restoreRoutingPanelState(state);
  }

  public int getHeight()
  {
    return mFrame.getHeight();
  }

  public void setOnToggleListener(@Nullable OnToggleListener listener)
  {
    mToggleListener = listener;
  }

  public void showAddStartFrame()
  {
    mRoutingBottomMenuController.showAddStartFrame();
  }

  public void showAddFinishFrame()
  {
    mRoutingBottomMenuController.showAddFinishFrame();
  }

  public void hideActionFrame()
  {
    mRoutingBottomMenuController.hideActionFrame();
  }
}
