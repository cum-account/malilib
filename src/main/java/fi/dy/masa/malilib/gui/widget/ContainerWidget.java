package fi.dy.masa.malilib.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.util.ScreenContext;

public abstract class ContainerWidget extends InteractableWidget
{
    protected final List<InteractableWidget> subWidgets = new ArrayList<>(1);
    @Nullable protected MenuWidget activeContextMenuWidget;

    public ContainerWidget(int width, int height)
    {
        super(width, height);
    }

    public ContainerWidget(int x, int y, int width, int height)
    {
        super(x, y, width, height);
    }

    /**
     * This method should be overridden to add any widgets from
     * (final) fields to the ContainerWidget lists.
     * This should be called whenever the collection of (active)
     * sub widgets should change for whatever reason (such as a widget
     * becoming active or inactive and maybe covering other widgets).
     */
    public void reAddSubWidgets()
    {
        this.clearWidgets();
    }

    public void closeCurrentContextMenu()
    {
        if (this.activeContextMenuWidget != null)
        {
            this.removeWidget(this.activeContextMenuWidget);
        }
    }

    public void openContextMenu(MenuWidget widget)
    {
        this.closeCurrentContextMenu();
        this.activeContextMenuWidget = widget;
        this.addWidget(widget);
    }

    @Override
    protected void onPositionChanged(int oldX, int oldY)
    {
        int diffX = this.getX() - oldX;
        int diffY = this.getY() - oldY;

        if (diffX != 0 || diffY != 0)
        {
            this.moveSubWidgets(diffX, diffY);
        }
    }

    @Override
    protected void onSizeChanged()
    {
        this.updateSubWidgetsToGeometryChanges();
    }

    @Override
    protected void onPositionOrSizeChanged(int oldX, int oldY)
    {
        int diffX = this.getX() - oldX;
        int diffY = this.getY() - oldY;

        if (diffX != 0 || diffY != 0)
        {
            this.moveSubWidgets(diffX, diffY);
        }
        else
        {
            this.updateSubWidgetsToGeometryChanges();
        }
    }

    /**
     * Moves all the sub widgets by the specified amount.
     * Used for example when the window is resized or maybe some
     * widgets are dragged around.
     * @param diffX
     * @param diffY
     */
    public void moveSubWidgets(int diffX, int diffY)
    {
        for (InteractableWidget widget : this.subWidgets)
        {
            widget.setPosition(widget.getX() + diffX, widget.getY() + diffY);
        }
    }

    /**
     *
     * This method should be overridden to update any sub widget
     * positions to the current position of this container widget,
     * or to any other changes such as width or height changes.
     */
    public void updateSubWidgetsToGeometryChanges()
    {
        for (InteractableWidget widget : this.subWidgets)
        {
            widget.onContainerGeometryChanged();
        }
    }

    public <T extends InteractableWidget> T addWidgetIfNotNull(@Nullable T widget)
    {
        if (widget != null)
        {
            this.addWidget(widget);
        }

        return widget;
    }

    public <T extends InteractableWidget> T addWidget(T widget)
    {
        this.subWidgets.add(widget);
        this.onSubWidgetAdded(widget);

        return widget;
    }

    public void removeWidget(InteractableWidget widget)
    {
        this.subWidgets.remove(widget);
    }

    public void clearWidgets()
    {
        this.subWidgets.clear();
    }

    public void onSubWidgetAdded(InteractableWidget widget)
    {
        widget.setTaskQueue(this.taskQueue);
        widget.onWidgetAdded(this.getZ());
    }

    @Override
    public void setTaskQueue(@Nullable Consumer<Runnable> taskQueue)
    {
        super.setTaskQueue(taskQueue);

        for (InteractableWidget widget : this.subWidgets)
        {
            widget.setTaskQueue(taskQueue);
        }
    }

    @Override
    public void onWidgetAdded(float parentZLevel)
    {
        super.onWidgetAdded(parentZLevel);
        this.reAddSubWidgets();
        this.updateSubWidgetsToGeometryChanges();
    }

    @Override
    public void setZ(float z)
    {
        for (InteractableWidget widget : this.subWidgets)
        {
            widget.setZLevelBasedOnParent(z);
        }

        super.setZ(z);
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY)
    {
        if (super.isMouseOver(mouseX, mouseY))
        {
            return true;
        }

        // Let the sub widgets check if the mouse is over them,
        // in case they extend beyond the bounds of this container widget.
        for (InteractableWidget widget : this.subWidgets)
        {
            if (widget.isMouseOver(mouseX, mouseY))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.subWidgets.isEmpty() == false)
        {
            for (InteractableWidget widget : this.subWidgets)
            {
                if (widget.tryMouseClick(mouseX, mouseY, mouseButton))
                {
                    // Don't call super if the button press got handled
                    return true;
                }
            }
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onMouseReleased(int mouseX, int mouseY, int mouseButton)
    {
        if (this.subWidgets.isEmpty() == false)
        {
            for (InteractableWidget widget : this.subWidgets)
            {
                widget.onMouseReleased(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    protected boolean onMouseScrolled(int mouseX, int mouseY, double mouseWheelDelta)
    {
        if (this.subWidgets.isEmpty() == false)
        {
            for (InteractableWidget widget : this.subWidgets)
            {
                if (widget.tryMouseScroll(mouseX, mouseY, mouseWheelDelta))
                {
                    return true;
                }
            }
        }

        return super.onMouseScrolled(mouseX, mouseY, mouseWheelDelta);
    }

    @Override
    public boolean onMouseMoved(int mouseX, int mouseY)
    {
        for (InteractableWidget widget : this.subWidgets)
        {
            if (widget.onMouseMoved(mouseX, mouseY))
            {
                return true;
            }
        }

        return super.onMouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        if (this.subWidgets.isEmpty() == false)
        {
            for (InteractableWidget widget : this.subWidgets)
            {
                if (widget.onKeyTyped(keyCode, scanCode, modifiers))
                {
                    // Don't call super if the key press got handled
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onCharTyped(char charIn, int modifiers)
    {
        if (this.subWidgets.isEmpty() == false)
        {
            for (InteractableWidget widget : this.subWidgets)
            {
                if (widget.onCharTyped(charIn, modifiers))
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    @Nullable
    public InteractableWidget getTopHoveredWidget(int mouseX, int mouseY, @Nullable InteractableWidget highestFoundWidget)
    {
        highestFoundWidget = super.getTopHoveredWidget(mouseX, mouseY, highestFoundWidget);
        return InteractableWidget.getTopHoveredWidgetFromList(this.subWidgets, mouseX, mouseY, highestFoundWidget);
    }

    @Override
    public List<BaseTextFieldWidget> getAllTextFields()
    {
        List<BaseTextFieldWidget> textFields = new ArrayList<>();

        if (this.subWidgets.isEmpty() == false)
        {
            for (InteractableWidget widget : this.subWidgets)
            {
                textFields.addAll(widget.getAllTextFields());
            }
        }

        return textFields;
    }

    @Override
    public void renderAt(int x, int y, float z, ScreenContext ctx)
    {
        super.renderAt(x, y, z, ctx);

        this.renderSubWidgets(x, y, z, ctx);
    }

    protected void renderSubWidgets(int x, int y, float z, ScreenContext ctx)
    {
        if (this.subWidgets.isEmpty() == false)
        {
            int diffX = x - this.getX();
            int diffY = y - this.getY();
            float diffZ = z - this.getZ();

            for (InteractableWidget widget : this.subWidgets)
            {
                int wx = widget.getX() + diffX;
                int wy = widget.getY() + diffY;
                float wz = widget.getZ() + diffZ;
                widget.renderAt(wx, wy, wz, ctx);
            }
        }
    }

    @Override
    public void renderDebug(boolean hovered, ScreenContext ctx)
    {
        super.renderDebug(hovered, ctx);

        BaseScreen.renderWidgetDebug(this.subWidgets, ctx);
    }
}
