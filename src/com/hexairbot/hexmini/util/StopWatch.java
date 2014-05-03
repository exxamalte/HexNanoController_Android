package com.hexairbot.hexmini.util;

/**
 * Simple stop watch.
 *
 * @author Malte Franken
 */
public class StopWatch {

  private Long startingTime;
  private long measure = 0l;

  public void start() {
    if (startingTime == null) {
      // never started before
      measure = 0l;
    }
    startingTime = System.currentTimeMillis();
  }

  public void pause() {
    if (startingTime == null) {
      // pause called before start
      return;
    }
    if (startingTime > 0) {
      long newMeasure = System.currentTimeMillis() - startingTime;
      measure = measure + newMeasure;
    }
    startingTime = 0l;
  }

  public long measure() {
    long newMeasure = 0l;
    if ((startingTime != null) && (startingTime > 0)) {
      newMeasure = System.currentTimeMillis() - startingTime;
    }
    return measure + newMeasure;
  }

  public void reset() {
    startingTime = null;
    measure = 0l;
  }

}
