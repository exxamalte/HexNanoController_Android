package com.hexairbot.hexmini.ui;

import javax.microedition.khronos.opengles.GL10;

import com.hexairbot.hexmini.ui.gl.GLSprite;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

public class Indicator extends Sprite {
  private GLSprite[] indicatorStates;
  private int value;
  private boolean initialized;

  public Indicator(Resources resources, int[] drawableIds, Align alignment) {
    super(alignment);

    indicatorStates = new GLSprite[drawableIds.length];

    for (int i = 0; i < drawableIds.length; ++i) {
      GLSprite sprite = new GLSprite(resources, drawableIds[i]);
      indicatorStates[i] = sprite;
    }
  }

  @Override
  public void init(GL10 gl, int program) {

    for (GLSprite sprite : indicatorStates) {
      sprite.init(gl, program);
    }

    initialized = true;
  }

  @Override
  public void draw(GL10 gl) {
    GLSprite sprite = indicatorStates[value];
    sprite.onDraw(gl, bounds.left, surfaceHeight - bounds.top - sprite.height);
  }

  @Override
  public void draw(Canvas canvas) {
    GLSprite sprite = indicatorStates[value];
    sprite.onDraw(canvas, bounds.left, surfaceHeight - bounds.top - sprite.height);
  }

  @Override
  public boolean onTouchEvent(View v, MotionEvent event) {
    return false;
  }

  @Override
  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public void setViewAndProjectionMatrices(float[] vMatrix, float[] projMatrix) {
    for (GLSprite indicatorState : indicatorStates) {
      indicatorState.setViewAndProjectionMatrices(vMatrix, projMatrix);
    }
  }

  @Override
  public int getWidth() {
    return indicatorStates[value].width;
  }

  @Override
  public int getHeight() {
    return indicatorStates[value].height;
  }

  public void setValue(int value) {
    if (value < 0 || value >= indicatorStates.length) {
      throw new IllegalArgumentException("Value " + value + " is out of bounds");
    }

    this.value = value;
  }

  @Override
  public void freeResources() {
    for (GLSprite indicatorState : indicatorStates) {
      indicatorState.freeResources();
    }
  }

  @Override
  public void setNeedsUpdate() {
    initialized = false;
  }
}
