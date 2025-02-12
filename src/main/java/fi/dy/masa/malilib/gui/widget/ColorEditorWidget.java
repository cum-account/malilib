package fi.dy.masa.malilib.gui.widget;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import fi.dy.masa.malilib.gui.util.EdgeInt;
import fi.dy.masa.malilib.util.data.Color4f;

public class ColorEditorWidget extends ContainerWidget
{
    protected final IntSupplier colorInput;
    protected final IntConsumer colorOutput;
    protected final BaseTextFieldWidget textField;
    protected final ColorIndicatorWidget colorIndicator;
    protected final int originalColor;

    public ColorEditorWidget(int width, int height, EdgeInt colorStorage)
    {
        this(width, height, colorStorage::getTop, colorStorage::setAll);
    }

    public ColorEditorWidget(int width, int height, IntSupplier colorInput, IntConsumer colorOutput)
    {
        super(width, height);

        this.colorInput = colorInput;
        this.colorOutput = colorOutput;
        this.originalColor = colorInput.getAsInt();

        int w = width - height - 4;
        this.textField = new BaseTextFieldWidget(w, 16, String.format("#%08X", this.originalColor));
        this.textField.setTextValidator(BaseTextFieldWidget.VALIDATOR_HEX_COLOR_8_6_4_3);
        this.textField.setListener(this::setColorFromString);

        int size = height;
        this.colorIndicator = new ColorIndicatorWidget(size, size, colorInput, this::setColorFromEditor);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.textField);
        this.addWidget(this.colorIndicator);
    }

    @Override
    public void updateSubWidgetsToGeometryChanges()
    {
        super.updateSubWidgetsToGeometryChanges();

        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();

        this.textField.setPosition(x, y + height / 2 - 8);
        this.textField.setWidth(width - height - 4);

        this.colorIndicator.setPosition(x + width - height, y);
    }

    protected void setColorFromEditor(int color)
    {
        this.textField.setText(String.format("#%08X", color));
        this.colorOutput.accept(color);
    }

    protected void setColorFromString(String str)
    {
        this.colorOutput.accept(Color4f.getColorFromString(str, this.originalColor));
    }
}
