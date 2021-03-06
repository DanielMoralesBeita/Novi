package io.anuke.novi.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import io.anuke.novi.Novi;

public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setMaximized(true);
		config.setTitle("Novi");
		config.useVsync(false); // set to true to cap FPS
		config.setWindowIcon("icon.png");
		new Lwjgl3Application(new Novi(), config);
	}
}
