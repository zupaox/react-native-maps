package com.airbnb.android.react.maps;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

public class AirMapMarkerManager extends ViewGroupManager<AirMapMarker> {

  private static final int SHOW_INFO_WINDOW = 1;
  private static final int HIDE_INFO_WINDOW = 2;
  private static final int ANIMATE_MARKER_TO_COORDINATE = 3;

  public static class AirMapMarkerSharedIcon {
    private String imageUri;
    private BitmapDescriptor iconBitmapDescriptor;
    private Bitmap bitmap;
    private List<WeakReference<AirMapMarker>> markers;
    private boolean loadImageStarted;

    public AirMapMarkerSharedIcon(String uri){
      this.markers = new ArrayList<>();
      this.imageUri = uri;
      this.loadImageStarted = false;
    }

    /**
     * subscribe icon update for given marker.
     *
     * The marker is wrapped in weakReference, so no need to remove it explicitly.
     *
     * When some other markers has already start loading the image, this method will return false
     * so this marker do not need to load again.
     *
     * When this is the first marker added for this icon, the method will return true. So the marker
     * Need to load the image.
     *
     * @param marker
     * @return true when marker should try to load the image, false otherwise
     */
    public synchronized boolean addMarker(AirMapMarker marker) {
      boolean loadStarted = this.loadImageStarted;
      this.loadImageStarted = true;
      this.markers.add(new WeakReference<AirMapMarker>(marker));
      if(this.iconBitmapDescriptor != null) {
        marker.setFinalIconBitmapDescriptor(this.iconBitmapDescriptor);
      }
      return !loadStarted;
    }

    public synchronized void updateIcon(BitmapDescriptor bitmapDescriptor, Bitmap bitmap) {
      this.iconBitmapDescriptor = bitmapDescriptor;
      this.bitmap = bitmap;
      List<WeakReference<AirMapMarker>> newArray = new ArrayList<>();
      for(WeakReference<AirMapMarker> weakMarker: markers) {
        AirMapMarker marker = weakMarker.get();
        if(marker != null) {
          marker.setFinalIconBitmapDescriptor(bitmapDescriptor);
          newArray.add(weakMarker);
        }
      }
      this.markers = newArray;
    }
  }

  private Map<String, AirMapMarkerSharedIcon> sharedIcons = new ConcurrentHashMap<>();

  /**
   * get the shared icon object, if not existed, create a new one and store it.
   *
   * @param uri
   * @return the icon object for the given uri.
   */
  public AirMapMarkerSharedIcon getSharedIcon(String uri) {
    AirMapMarkerSharedIcon icon = this.sharedIcons.get(uri);
    if(icon == null) {
      icon = new AirMapMarkerSharedIcon(uri);
      this.sharedIcons.put(uri, icon);
    }
    return icon;
  }

  public AirMapMarkerManager() {
  }

  @Override
  public String getName() {
    return "AIRMapMarker";
  }

  @Override
  public AirMapMarker createViewInstance(ThemedReactContext context) {
    return new AirMapMarker(context, this);
  }

  @ReactProp(name = "coordinate")
  public void setCoordinate(AirMapMarker view, ReadableMap map) {
    view.setCoordinate(map);
  }

  @ReactProp(name = "title")
  public void setTitle(AirMapMarker view, String title) {
    view.setTitle(title);
  }

  @ReactProp(name = "identifier")
  public void setIdentifier(AirMapMarker view, String identifier) {
    view.setIdentifier(identifier);
  }

  @ReactProp(name = "description")
  public void setDescription(AirMapMarker view, String description) {
    view.setSnippet(description);
  }

  // NOTE(lmr):
  // android uses normalized coordinate systems for this, and is provided through the
  // `anchor` property  and `calloutAnchor` instead.  Perhaps some work could be done
  // to normalize iOS and android to use just one of the systems.
//    @ReactProp(name = "centerOffset")
//    public void setCenterOffset(AirMapMarker view, ReadableMap map) {
//
//    }
//
//    @ReactProp(name = "calloutOffset")
//    public void setCalloutOffset(AirMapMarker view, ReadableMap map) {
//
//    }

  @ReactProp(name = "anchor")
  public void setAnchor(AirMapMarker view, ReadableMap map) {
    // should default to (0.5, 1) (bottom middle)
    double x = map != null && map.hasKey("x") ? map.getDouble("x") : 0.5;
    double y = map != null && map.hasKey("y") ? map.getDouble("y") : 1.0;
    view.setAnchor(x, y);
  }

  @ReactProp(name = "calloutAnchor")
  public void setCalloutAnchor(AirMapMarker view, ReadableMap map) {
    // should default to (0.5, 0) (top middle)
    double x = map != null && map.hasKey("x") ? map.getDouble("x") : 0.5;
    double y = map != null && map.hasKey("y") ? map.getDouble("y") : 0.0;
    view.setCalloutAnchor(x, y);
  }

  @ReactProp(name = "image")
  public void setImage(AirMapMarker view, @Nullable String source) {
    view.setImage(source);
  }
//    public void setImage(AirMapMarker view, ReadableMap image) {
//        view.setImage(image);
//    }

  @ReactProp(name = "pinColor", defaultInt = Color.RED, customType = "Color")
  public void setPinColor(AirMapMarker view, int pinColor) {
    float[] hsv = new float[3];
    Color.colorToHSV(pinColor, hsv);
    // NOTE: android only supports a hue
    view.setMarkerHue(hsv[0]);
  }

  @ReactProp(name = "rotation", defaultFloat = 0.0f)
  public void setMarkerRotation(AirMapMarker view, float rotation) {
    view.setRotation(rotation);
  }

  @ReactProp(name = "flat", defaultBoolean = false)
  public void setFlat(AirMapMarker view, boolean flat) {
    view.setFlat(flat);
  }

  @ReactProp(name = "draggable", defaultBoolean = false)
  public void setDraggable(AirMapMarker view, boolean draggable) {
    view.setDraggable(draggable);
  }

  @Override
  @ReactProp(name = "zIndex", defaultFloat = 0.0f)
  public void setZIndex(AirMapMarker view, float zIndex) {
    super.setZIndex(view, zIndex);
    int integerZIndex = Math.round(zIndex);
    view.setZIndex(integerZIndex);
  }

  @Override
  @ReactProp(name = "opacity", defaultFloat = 1.0f)
  public void setOpacity(AirMapMarker view, float opacity) {
    super.setOpacity(view, opacity);
    view.setOpacity(opacity);
  }

  @Override
  public void addView(AirMapMarker parent, View child, int index) {
    // if an <Callout /> component is a child, then it is a callout view, NOT part of the
    // marker.
    if (child instanceof AirMapCallout) {
      parent.setCalloutView((AirMapCallout) child);
    } else {
      super.addView(parent, child, index);
      parent.update();
    }
  }

  @Override
  public void removeViewAt(AirMapMarker parent, int index) {
    super.removeViewAt(parent, index);
    parent.update();
  }

  @Override
  @Nullable
  public Map<String, Integer> getCommandsMap() {
    return MapBuilder.of(
        "showCallout", SHOW_INFO_WINDOW,
        "hideCallout", HIDE_INFO_WINDOW,
        "animateMarkerToCoordinate",  ANIMATE_MARKER_TO_COORDINATE
    );
  }

  @Override
  public void receiveCommand(AirMapMarker view, int commandId, @Nullable ReadableArray args) {
    Integer duration;
    Double lat;
    Double lng;
    ReadableMap region;

    switch (commandId) {
      case SHOW_INFO_WINDOW:
        ((Marker) view.getFeature()).showInfoWindow();
        break;

      case HIDE_INFO_WINDOW:
        ((Marker) view.getFeature()).hideInfoWindow();
        break;
      
      case ANIMATE_MARKER_TO_COORDINATE:
        region = args.getMap(0);
        duration = args.getInt(1);

        lng = region.getDouble("longitude");
        lat = region.getDouble("latitude");
        view.animateToCoodinate(new LatLng(lat, lng), duration);
        break;
    }
  }

  @Override
  @Nullable
  public Map getExportedCustomDirectEventTypeConstants() {
    Map<String, Map<String, String>> map = MapBuilder.of(
        "onPress", MapBuilder.of("registrationName", "onPress"),
        "onCalloutPress", MapBuilder.of("registrationName", "onCalloutPress"),
        "onDragStart", MapBuilder.of("registrationName", "onDragStart"),
        "onDrag", MapBuilder.of("registrationName", "onDrag"),
        "onDragEnd", MapBuilder.of("registrationName", "onDragEnd")
    );

    map.putAll(MapBuilder.of(
        "onDragStart", MapBuilder.of("registrationName", "onDragStart"),
        "onDrag", MapBuilder.of("registrationName", "onDrag"),
        "onDragEnd", MapBuilder.of("registrationName", "onDragEnd")
    ));

    return map;
  }

  @Override
  public LayoutShadowNode createShadowNodeInstance() {
    // we use a custom shadow node that emits the width/height of the view
    // after layout with the updateExtraData method. Without this, we can't generate
    // a bitmap of the appropriate width/height of the rendered view.
    return new SizeReportingShadowNode();
  }

  @Override
  public void updateExtraData(AirMapMarker view, Object extraData) {
    // This method is called from the shadow node with the width/height of the rendered
    // marker view.
    HashMap<String, Float> data = (HashMap<String, Float>) extraData;
    float width = data.get("width");
    float height = data.get("height");
    view.update((int) width, (int) height);
  }
}
