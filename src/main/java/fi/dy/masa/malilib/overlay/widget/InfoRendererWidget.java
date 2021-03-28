package fi.dy.masa.malilib.overlay.widget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.gui.position.ScreenLocation;
import fi.dy.masa.malilib.gui.widget.BaseWidget;
import fi.dy.masa.malilib.listener.EventListener;
import fi.dy.masa.malilib.overlay.InfoWidgetManager;
import fi.dy.masa.malilib.render.ShapeRenderUtils;
import fi.dy.masa.malilib.render.text.StyledTextLine;
import fi.dy.masa.malilib.util.JsonUtils;

public abstract class InfoRendererWidget extends BaseWidget
{
    protected final List<Consumer<ScreenLocation>> locationChangeListeners = new ArrayList<>();
    protected final Set<UUID> markers = new HashSet<>();
    protected ScreenLocation location = ScreenLocation.TOP_LEFT;
    protected String name = "?";
    @Nullable protected EventListener geometryChangeListener;
    @Nullable protected EventListener enabledChangeListener;
    @Nullable protected StyledTextLine styledName;
    protected boolean enabled = true;
    protected boolean delaydGeometryUpdate;
    protected boolean forceNotifyGeometryChangeListener;
    protected boolean isOverlay;
    protected boolean needsReLayout;
    protected boolean oddEvenBackground;
    protected boolean renderBackground;
    protected boolean renderName;
    protected boolean shouldSerialize;
    protected long previousGeometryUpdateTime = -1;
    protected long geometryShrinkDelay = (long) (5 * 1E9); // 5 seconds
    protected int backgroundColor = 0x30A0A0A0;
    protected int backgroundColorOdd = 0x40A0A0A0;
    protected int containerWidth;
    protected int containerHeight;
    protected int geometryShrinkThresholdX = 40;
    protected int geometryShrinkThresholdY = 10;
    protected int previousUpdatedWidth;
    protected int previousUpdatedHeight;
    protected int sortIndex = 100;

    public InfoRendererWidget()
    {
        super(0, 0, 0, 0);

        this.margin.setChangeListener(this::requestUnconditionalReLayout);
        this.padding.setChangeListener(this::requestUnconditionalReLayout);
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    /**
     * A widget that says it's an overlay will not get moved on the y direction
     * by other widgets, but instead it will sit on top of the other widgets
     * at the base location of the InfoArea.
     */
    public boolean isOverlay()
    {
        return this.isOverlay;
    }

    /**
     * Returns whether or not this widget should get saved and loaded
     * automatically. This should generally only return true for
     * widgets that are created by the user via some configuration menu,
     * and are thus handled via the InfoWidgetManager.
     */
    public boolean getShouldSerialize()
    {
        return this.shouldSerialize;
    }

    public int getSortIndex()
    {
        return this.sortIndex;
    }

    public boolean isBackgroundEnabled()
    {
        return this.renderBackground;
    }

    public boolean isOddEvenBackgroundEnabled()
    {
        return this.oddEvenBackground;
    }

    public boolean getRenderName()
    {
        return this.renderName;
    }

    public String getName()
    {
        return this.name != null ? this.name : this.location.getDisplayName();
    }

    public ScreenLocation getScreenLocation()
    {
        return this.location;
    }

    public int getBackgroundColor()
    {
        return this.backgroundColor;
    }

    public int getOddBackgroundColor()
    {
        return this.backgroundColorOdd;
    }

    public void toggleEnabled()
    {
        this.setEnabled(! this.isEnabled());
    }

    public void setEnabled(boolean enabled)
    {
        boolean wasEnabled = this.enabled;
        this.enabled = enabled;

        if (enabled != wasEnabled && this.enabledChangeListener != null)
        {
            this.enabledChangeListener.onEvent();
        }
    }

    public void toggleBackgroundEnabled()
    {
        this.renderBackground = ! this.renderBackground;
    }

    public void toggleOddEvenBackgroundEnabled()
    {
        this.oddEvenBackground = ! this.oddEvenBackground;
    }

    public void toggleRenderName()
    {
        this.renderName = ! this.renderName;
        this.requestUnconditionalReLayout();
    }

    /**
     * Sets the sort index of this widget. Lower values come first (higher up).
     */
    public void setSortIndex(int index)
    {
        this.sortIndex = index;
    }

    public void setBackgroundColor(int color)
    {
        this.backgroundColor = color;
    }

    public void setOddBackgroundColor(int color)
    {
        this.backgroundColorOdd = color;
    }

    /**
     * Sets a listener that should be notified if the dimensions of this widget get changed,
     * such as the widget height or width changing due to changes in the displayed contents.
     */
    public void setGeometryChangeListener(@Nullable EventListener listener)
    {
        this.geometryChangeListener = listener;
    }

    /**
     * Sets a listener that should be notified if the dimensions of this widget get changed,
     * such as the widget height or width changing due to changes in the displayed contents.
     */
    public void setEnabledChangeListener(@Nullable EventListener listener)
    {
        this.enabledChangeListener = listener;
    }

    /**
     * Adds a listener that gets notified when the ScreenLocation of this widget gets changed.
     */
    public void addLocationChangeListener(Consumer<ScreenLocation> listener)
    {
        if (this.locationChangeListeners.contains(listener) == false)
        {
            this.locationChangeListeners.add(listener);
        }
    }

    public void removeLocationChangeListener(Consumer<ScreenLocation> listener)
    {
        this.locationChangeListeners.remove(listener);
    }

    /**
     * Adds a marker that a mod can use to recognize which of the possibly several
     * info widgets of the same type in the same InfoArea/location it has been using.
     * This is mostly useful after game restarts or world re-logs, when the
     * InfoWidgetManager reloads the saved widgets, and a mod wants to re-attach to the
     * "same" widget it was using before, instead of creating new ones every time.
     */
    public void addMarker(UUID marker)
    {
        this.markers.add(marker);
    }

    public void removeMarker(UUID marker)
    {
        this.markers.remove(marker);
    }

    public boolean hasMarker(UUID marker)
    {
        return this.markers.contains(marker);
    }

    public void setContainerDimensions(int width, int height)
    {
        this.containerWidth = width;
        this.containerHeight = height;
    }

    public void setLocation(ScreenLocation location)
    {
        this.location = location;

        if (StringUtils.isBlank(this.name))
        {
            this.setName(location.getDisplayName());
        }

        for (Consumer<ScreenLocation> listener : this.locationChangeListeners)
        {
            listener.accept(location);
        }
    }

    public void setName(String name)
    {
        this.name = name;
        this.styledName = StyledTextLine.of(name);
    }

    protected void requestConditionalReLayout()
    {
        this.needsReLayout = true;
    }

    protected void requestUnconditionalReLayout()
    {
        this.needsReLayout = true;
        this.forceNotifyGeometryChangeListener = true;
    }

    protected void reLayoutWidgets(boolean forceNotify)
    {
        this.updateSize();
        this.updateSubWidgetPositions();
        this.notifyContainerOfChanges(forceNotify);

        this.needsReLayout = false;
        this.forceNotifyGeometryChangeListener = false;
    }

    @Override
    protected void onPositionChanged(int oldX, int oldY)
    {
        this.updateSubWidgetPositions();
    }

    public void updateSubWidgetPositions()
    {
    }

    /**
     * Requests the container to re-layout all the info widgets due to
     * this widget's dimensions changing.
     */
    protected void notifyContainerOfChanges(boolean forceNotify)
    {
        if (this.geometryChangeListener != null && (forceNotify || this.needsGeometryUpdate()))
        {
            this.geometryChangeListener.onEvent();
            this.previousUpdatedWidth = this.getWidth();
            this.previousUpdatedHeight = this.getHeight();
            this.previousGeometryUpdateTime = System.nanoTime();
            this.delaydGeometryUpdate = false;
        }
    }

    protected boolean needsGeometryUpdate()
    {
        int height = this.getHeight();
        int width = this.getWidth();

        if (width > this.previousUpdatedWidth || height > this.previousUpdatedHeight)
        {
            return true;
        }

        if (width < (this.previousUpdatedWidth - this.geometryShrinkThresholdX) ||
            height < (this.previousUpdatedHeight - this.geometryShrinkThresholdY))
        {
            this.delaydGeometryUpdate = true;
            return System.nanoTime() - this.previousGeometryUpdateTime > this.geometryShrinkDelay;
        }

        return false;
    }

    /**
     * 
     * Called to allow the widget to update its state before all the enabled widgets are rendered.
     */
    public void updateState()
    {
        if (this.needsReLayout)
        {
            this.reLayoutWidgets(this.forceNotifyGeometryChangeListener);
        }

        // Keep checking for geometry updates until the delay time runs out,
        // if the contents are set to shrink after a delay
        if (this.delaydGeometryUpdate)
        {
            this.notifyContainerOfChanges(false);
        }
    }

    public void render()
    {
        if (this.isEnabled())
        {
            int x = this.getX();
            int y = this.getY();
            this.renderAt(x, y, this.getZLevel());

            if (MaLiLibConfigs.Debug.INFO_OVERLAY_DEBUG.getBooleanValue())
            {
                this.renderDebug(0, 0, false, true, MaLiLibConfigs.Debug.GUI_DEBUG_INFO_ALWAYS.getBooleanValue());
            }
        }
    }

    public void renderAt(int x, int y, float z)
    {
        this.renderBackground(x, y, z);

        if (this.renderName && this.styledName != null)
        {
            y += this.padding.getTop();
            this.renderTextLine(x + this.padding.getLeft(), y, z, 0xFFFFFFFF, true, this.styledName);
            y += this.lineHeight;
        }

        this.renderContents(x, y, z);
    }

    protected void renderBackground(int x, int y, float z)
    {
        if (this.renderBackground)
        {
            if (this.oddEvenBackground)
            {
                this.renderOddEvenLineBackgrounds(x, y, z);
            }
            else
            {
                int width = this.getWidth();
                int height = this.getHeight();
                ShapeRenderUtils.renderRectangle(x, y, z, width, height, this.backgroundColor);
            }
        }
    }

    protected void renderOddEvenLineBackgrounds(int x, int y, float z)
    {
    }

    protected void renderContents(int x, int y, float z)
    {
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.addProperty("type", this.getClass().getName());
        obj.addProperty("name", this.getName());
        obj.addProperty("enabled", this.isEnabled());
        obj.addProperty("screen_location", this.getScreenLocation().getName());
        obj.addProperty("render_name", this.renderName);
        obj.addProperty("bg_enabled", this.renderBackground);
        obj.addProperty("bg_odd_even", this.oddEvenBackground);
        obj.addProperty("bg_color", this.backgroundColor);
        obj.addProperty("bg_color_odd", this.backgroundColorOdd);
        obj.addProperty("sort_index", this.getSortIndex());
        obj.add("padding", this.padding.toJson());
        obj.add("margin", this.margin.toJson());

        if (this.markers.isEmpty() == false)
        {
            JsonArray arr = new JsonArray();

            for (UUID marker : this.markers)
            {
                arr.add(marker.toString());
            }

            obj.add("markers", arr);
        }

        return obj;
    }

    public void fromJson(JsonObject obj)
    {
        this.enabled = JsonUtils.getBooleanOrDefault(obj, "enabled", true);
        this.renderBackground = JsonUtils.getBooleanOrDefault(obj, "bg_enabled", false);
        this.oddEvenBackground = JsonUtils.getBooleanOrDefault(obj, "bg_odd_even", false);
        this.backgroundColor = JsonUtils.getIntegerOrDefault(obj, "bg_color", 0x30A0A0A0);
        this.backgroundColorOdd = JsonUtils.getIntegerOrDefault(obj, "bg_color_odd", 0x40A0A0A0);
        this.renderName = JsonUtils.getBooleanOrDefault(obj, "render_name", false);
        this.setName(JsonUtils.getStringOrDefault(obj, "name", this.name));
        this.setSortIndex(JsonUtils.getIntegerOrDefault(obj, "sort_index", 100));

        if (JsonUtils.hasString(obj, "screen_location"))
        {
            ScreenLocation location = ScreenLocation.findValueByName(obj.get("screen_location").getAsString(), ScreenLocation.VALUES);
            this.setLocation(location);
        }

        if (JsonUtils.hasArray(obj, "padding"))
        {
            this.padding.fromJson(obj.get("padding").getAsJsonArray());
        }

        if (JsonUtils.hasArray(obj, "margin"))
        {
            this.margin.fromJson(obj.get("margin").getAsJsonArray());
        }

        this.markers.clear();

        if (JsonUtils.hasArray(obj, "markers"))
        {
            JsonArray arr = obj.get("markers").getAsJsonArray();
            int size = arr.size();

            for (int i = 0; i < size; ++i)
            {
                try
                {
                    UUID marker = UUID.fromString(arr.get(i).getAsString());
                    this.markers.add(marker);
                }
                catch (IllegalArgumentException ignore) {}
            }
        }
    }

    @Nullable
    public static InfoRendererWidget createFromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "type"))
        {
            String type = obj.get("type").getAsString();
            InfoWidgetManager.InfoWidgetFactory factory = InfoWidgetManager.getWidgetFactory(type);

            if (factory != null)
            {
                InfoRendererWidget widget = factory.create();
                widget.fromJson(obj);
                return widget;
            }
        }

        return null;
    }
}
