package com.wmods.wppenhacer.xposed.features.others;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.wmods.wppenhacer.views.dialog.SimpleColorPickerDialog;
import com.wmods.wppenhacer.xposed.core.Feature;
import com.wmods.wppenhacer.xposed.core.WppCore;
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator;
import com.wmods.wppenhacer.xposed.utils.ReflectionUtils;
import com.wmods.wppenhacer.xposed.utils.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class TextStatusComposer extends Feature {
    private static final ColorData colorData = new ColorData();

    public TextStatusComposer(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("statuscomposer", false)) return;

        Class<?> clazz = WppCore.getTextStatusComposerFragmentClass(classLoader);
        java.lang.reflect.Method methodOnCreate = ReflectionUtils.findMethodUsingFilter(clazz, method -> method.getParameterCount() == 2 && method.getParameterTypes()[0] == Bundle.class && method.getParameterTypes()[1] == View.class);
        XposedBridge.hookMethod(methodOnCreate,
                new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        logDebug("afterHookedMethod", "TextStatusComposer");
                        android.app.Activity activity = WppCore.getCurrentActivity();
                        View viewRoot = (View) param.args[1];
                        View pickerColor = viewRoot.findViewById(Utils.getID("color_picker_btn", "id"));
                        EditText entry = (EditText) viewRoot.findViewById(Utils.getID("entry", "id"));

                        pickerColor.setOnLongClickListener(v -> {
                            SimpleColorPickerDialog dialog = new SimpleColorPickerDialog(activity, color -> {
                                try {
                                    activity.getWindow().setBackgroundDrawable(new ColorDrawable(color));
                                    viewRoot.findViewById(Utils.getID("background","id")).setBackgroundColor(color);
                                    View controls = viewRoot.findViewById(Utils.getID("controls", "id"));
                                    controls.setBackgroundColor(color);
                                    colorData.backgroundColor = color;
                                } catch (Exception e) {
                                    log(e);
                                }
                            });
                            dialog.create().setCanceledOnTouchOutside(false);
                            dialog.show();
                            return true;
                        });

                        View textColor = viewRoot.findViewById(Utils.getID("font_picker_btn", "id"));
                        textColor.setOnLongClickListener(v -> {
                            SimpleColorPickerDialog dialog = new SimpleColorPickerDialog(activity, color -> {
                                colorData.textColor = color;
                                entry.setTextColor(color);
                            });
                            dialog.create().setCanceledOnTouchOutside(false);
                            dialog.show();
                            return true;
                        });
                    }
                });


        Object methodsObj = Unobfuscator.loadTextStatusData(classLoader);
        
        if (methodsObj instanceof java.lang.reflect.Method[]) {
            java.lang.reflect.Method[] methodsTextStatus = (java.lang.reflect.Method[]) methodsObj;
            for (java.lang.reflect.Method method : methodsTextStatus) {
                Class<?> textDataClass = classLoader.loadClass("com.whatsapp.TextData");
                logDebug("setColorTextComposer", Unobfuscator.getMethodDescriptor(method));
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object textData = ReflectionUtils.getArg(param.args, textDataClass, 0);
                        if (textData == null) return;
                        if (colorData.textColor != -1)
                            XposedHelpers.setObjectField(textData, "textColor", colorData.textColor);
                        if (colorData.backgroundColor != -1)
                            XposedHelpers.setObjectField(textData, "backgroundColor", colorData.backgroundColor);
                        colorData.textColor = -1;
                        colorData.backgroundColor = -1;
                    }
                });
            }
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Text Status Composer";
    }

    public static class ColorData {
        public int textColor = -1;
        public int backgroundColor = -1;
    }
}
