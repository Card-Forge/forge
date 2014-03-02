package com.forge;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;

import forge.Forge;

public class Main extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(new Forge(), false);
    }
}
