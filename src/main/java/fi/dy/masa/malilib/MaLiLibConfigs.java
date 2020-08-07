package fi.dy.masa.malilib;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.ModConfig;
import fi.dy.masa.malilib.config.option.BooleanConfig;
import fi.dy.masa.malilib.config.option.ConfigOption;
import fi.dy.masa.malilib.config.option.HotkeyConfig;
import fi.dy.masa.malilib.config.option.IntegerConfig;
import fi.dy.masa.malilib.config.option.OptionListConfig;
import fi.dy.masa.malilib.config.value.HudAlignment;
import fi.dy.masa.malilib.config.value.KeybindDisplayMode;
import fi.dy.masa.malilib.input.KeyAction;
import fi.dy.masa.malilib.input.KeyBindSettings;

public class MaLiLibConfigs implements ModConfig
{
    public static class Generic
    {
        public static final OptionListConfig<KeybindDisplayMode> KEYBIND_DISPLAY        = new OptionListConfig<>("keybindDisplay", KeybindDisplayMode.NONE);
        public static final OptionListConfig<HudAlignment> KEYBIND_DISPLAY_ALIGNMENT    = new OptionListConfig<>("keybindDisplayAlignment", HudAlignment.BOTTOM_RIGHT);

        public static final HotkeyConfig IGNORED_KEYS                   = new HotkeyConfig("ignoredKeys", "");
        public static final BooleanConfig KEYBIND_DISPLAY_CALLBACK_ONLY = new BooleanConfig("keybindDisplayCallbackOnly", true);
        public static final BooleanConfig KEYBIND_DISPLAY_CANCEL_ONLY   = new BooleanConfig("keybindDisplayCancelOnly", true);
        public static final IntegerConfig KEYBIND_DISPLAY_DURATION      = new IntegerConfig("keybindDisplayDuration", 5000, 0, 120000);
        public static final HotkeyConfig OPEN_GUI_CONFIGS               = new HotkeyConfig("openGuiConfigs", "A,C");

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                IGNORED_KEYS,
                KEYBIND_DISPLAY,
                KEYBIND_DISPLAY_ALIGNMENT,
                KEYBIND_DISPLAY_CALLBACK_ONLY,
                KEYBIND_DISPLAY_CANCEL_ONLY,
                KEYBIND_DISPLAY_DURATION,
                OPEN_GUI_CONFIGS
        );
    }

    public static class Debug
    {
        public static final KeyBindSettings DBG_KS = KeyBindSettings.create(KeyBindSettings.Context.GUI, KeyAction.PRESS, true, false, false, false, true);

        public static final BooleanConfig GUI_DEBUG                 = new BooleanConfig("guiDebug", false);
        public static final BooleanConfig GUI_DEBUG_ALL             = new BooleanConfig("guiDebugAll", true);
        public static final BooleanConfig GUI_DEBUG_INFO_ALWAYS     = new BooleanConfig("guiDebugInfoAlways", false);
        public static final HotkeyConfig GUI_DEBUG_KEY              = new HotkeyConfig("guiDebugKey", "LMENU", DBG_KS);
        public static final BooleanConfig KEYBIND_DEBUG             = new BooleanConfig("keybindDebugging", false);
        public static final BooleanConfig KEYBIND_DEBUG_ACTIONBAR   = new BooleanConfig("keybindDebuggingIngame", true);

        public static final ImmutableList<ConfigOption<?>> OPTIONS = ImmutableList.of(
                GUI_DEBUG,
                GUI_DEBUG_ALL,
                GUI_DEBUG_INFO_ALWAYS,
                GUI_DEBUG_KEY,
                KEYBIND_DEBUG,
                KEYBIND_DEBUG_ACTIONBAR
        );
    }

    @Override
    public String getModId()
    {
        return MaLiLibReference.MOD_ID;
    }

    @Override
    public String getModName()
    {
        return MaLiLibReference.MOD_NAME;
    }

    @Override
    public String getConfigFileName()
    {
        return MaLiLibReference.MOD_ID + ".json";
    }

    @Override
    public Map<String, List<? extends ConfigOption<?>>> getConfigsPerCategories()
    {
        Map<String, List<? extends ConfigOption<?>>> map = new LinkedHashMap<>();

        map.put("Generic",  Generic.OPTIONS);
        map.put("Debug",    Debug.OPTIONS);

        return map;
    }

    @Override
    public boolean shouldSaveCategoryToFile(String category)
    {
        return category.equals("Debug") == false;
    }
}
