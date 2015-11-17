package com.android.systemui.statusbar.qscontroll;

import android.graphics.drawable.Drawable;

/**
 * 
 * Notification toggle view interface.
 * This is use by {@link NotificationToggleController} to update the UI show.
 * 
 * </br>
 * 
 * @author hmm@dw.gdbbk.com
 *
 */
public interface INotificationToggle {
	
    /**
     * Set status icon.
     * We may need dynamic change the icon some times.
     *  
     * @param icon {@link Drawable} of icon.
     */
    public void setIcon(Drawable icon);

    /**
     * Set toggle label.
     * We may need dynamic change the label some times.
     * 
     * @param label {@link String} of the label
     */
    public void setLabel(String label);

    /**
     * Set status icon level.
     * If you use this, you should set the {@link LevelListDrawable} to toggle view icon.
     *  
     * @param iconLevel icon status(level)
     */
    public void setIconLevel(int iconLevel);

    /**
     * Set toggle to selected status(not the icon, but only whole toggle).
     * 
     * @param selected True selected, false un-selected
     */
    public void makeSelected(boolean selected);

}
